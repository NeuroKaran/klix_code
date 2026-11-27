package com.klix.code

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.data.sandbox.SystemTarPlatformOps
import com.klix.code.data.sandbox.TarGzExtractor
import com.klix.code.data.sandbox.TarPlatformOps
import com.klix.code.data.sandbox.modeToPosixPermissions
import com.klix.code.ui.components.sandbox.SandboxSetupUiHelper
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission

class SandboxManagerTest {

    private lateinit var mockContext: Context
    private lateinit var tempDir: File
    private lateinit var sandboxManager: SandboxManager

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        tempDir = Files.createTempDirectory("klix_test_sandbox_").toFile()
        `when`(mockContext.filesDir).thenReturn(tempDir)

        sandboxManager = SandboxManager(mockContext)
    }

    @After
    fun tearDown() {
        if (tempDir.exists()) {
            tempDir.walkTopDown().forEach {
                it.setWritable(true)
            }
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testArchitectureDetectionAndToolchainEnv() {
        val arch = sandboxManager.detectArch()
        assertNotNull(arch)
        assertTrue(arch in listOf("aarch64", "x86_64"))

        val env = sandboxManager.getToolchainEnv(sandboxManager.getSandboxDir())
        assertTrue(!env["PATH"].isNullOrBlank())
        assertTrue(!env["PYTHONHOME"].isNullOrBlank())
        assertTrue(!env["HOME"].isNullOrBlank())
        assertTrue(!env["TMPDIR"].isNullOrBlank())
    }

    @Test
    fun testDirectoryStructures() {
        val sandboxDir = sandboxManager.getSandboxDir()
        val rootfsDir = sandboxManager.getRootfsDir()
        val binDir = sandboxManager.getBinDir()

        assertEquals(File(tempDir, "sandbox").absolutePath, sandboxDir.absolutePath)
        assertEquals(File(sandboxDir, "rootfs").absolutePath, rootfsDir.absolutePath)
        assertEquals(File(sandboxDir, "bin").absolutePath, binDir.absolutePath)
    }

    @Test
    fun testInitialStatusAndReset() {
        assertEquals(SandboxStatus.NOT_INSTALLED, sandboxManager.status.value)

        val resetSuccess = sandboxManager.resetSandbox()
        assertTrue(resetSuccess)
        assertEquals(SandboxStatus.NOT_INSTALLED, sandboxManager.status.value)
    }

    @Test
    fun testSandboxSetupUiHelperActionMapping() {
        // When READY: should trigger reinstall with reload icon
        assertTrue(SandboxSetupUiHelper.isReinstall(SandboxStatus.READY))
        assertEquals("Reinstall RootFS", SandboxSetupUiHelper.getActionLabel(SandboxStatus.READY))
        assertEquals(Icons.Default.Refresh, SandboxSetupUiHelper.getActionIcon(SandboxStatus.READY))

        // When NOT_INSTALLED, DOWNLOADING, EXTRACTING, ERROR: should trigger install with download icon
        val installStatuses = listOf(
            SandboxStatus.NOT_INSTALLED,
            SandboxStatus.DOWNLOADING,
            SandboxStatus.EXTRACTING,
            SandboxStatus.ERROR
        )

        for (status in installStatuses) {
            assertFalse("Expected status $status to not be reinstall", SandboxSetupUiHelper.isReinstall(status))
            assertEquals("Expected status $status to show 'Install RootFS'", "Install RootFS", SandboxSetupUiHelper.getActionLabel(status))
            assertEquals("Expected status $status to show download icon", Icons.Default.Download, SandboxSetupUiHelper.getActionIcon(status))
        }
    }

    @Test
    fun testInstallSandboxRejectsConcurrentExecution() = runBlocking {
        // Mock status to DOWNLOADING
        val statusField = SandboxManager::class.java.getDeclaredField("_status")
        statusField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val statusFlow = statusField.get(sandboxManager) as kotlinx.coroutines.flow.MutableStateFlow<SandboxStatus>
        statusFlow.value = SandboxStatus.DOWNLOADING

        val result = sandboxManager.installSandbox()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("RootFS installation is already in progress", result.exceptionOrNull()?.message)
    }

    @Test
    fun testToolchainActionMapping() {
        // Not installed: download icon and Tap to Install
        assertEquals("Tap to Install", SandboxSetupUiHelper.getToolActionLabel(isInstalled = false))
        assertEquals(Icons.Default.Download, SandboxSetupUiHelper.getToolActionIcon(isInstalled = false))

        // Installed: refresh/buffer icon and Reinstall
        assertEquals("Reinstall", SandboxSetupUiHelper.getToolActionLabel(isInstalled = true))
        assertEquals(Icons.Default.Refresh, SandboxSetupUiHelper.getToolActionIcon(isInstalled = true))

        // Installing in progress:
        assertEquals("Installing...", SandboxSetupUiHelper.getToolActionLabel(isInstalled = false, isInstalling = true))
        assertEquals("Installing...", SandboxSetupUiHelper.getToolActionLabel(isInstalled = true, isInstalling = true))
    }

    @Test
    fun testToolInstallationDetectionAndStateFlow() {
        // On clean sandbox (no rootfs), none of the tools are installed
        assertEquals(false, sandboxManager.isToolInstalled("nodejs"))
        assertEquals(false, sandboxManager.isToolInstalled("python3"))
        assertEquals(false, sandboxManager.isToolInstalled("git"))
        assertEquals(false, sandboxManager.isToolInstalled("c/c++"))

        val toolsMap = sandboxManager.installedTools.value
        assertEquals(false, toolsMap["nodejs"])
        assertEquals(false, toolsMap["python3"])
        assertEquals(false, toolsMap["git"])
        assertEquals(false, toolsMap["c/c++"])

        // Simulate installing nodejs binary in rootfs
        val rootfs = sandboxManager.getRootfsDir()
        val nodeBin = File(rootfs, "usr/bin/node")
        nodeBin.parentFile?.mkdirs()
        nodeBin.writeText("#!/bin/sh\necho v20.0.0")

        // Simulate installing python3 stdlib dir in rootfs
        val pyStdlib = File(rootfs, "usr/lib/python3.12")
        pyStdlib.mkdirs()

        sandboxManager.refreshInstalledTools()

        assertTrue(sandboxManager.isToolInstalled("nodejs"))
        assertTrue(sandboxManager.isToolInstalled("python3"))
        assertFalse(sandboxManager.isToolInstalled("git"))
        assertFalse(sandboxManager.isToolInstalled("c/c++"))

        val updatedMap = sandboxManager.installedTools.value
        assertEquals(true, updatedMap["nodejs"])
        assertEquals(true, updatedMap["python3"])
        assertEquals(false, updatedMap["git"])
        assertEquals(false, updatedMap["c/c++"])
    }

    @Test
    fun testToolInstallingTracking() {
        assertFalse(sandboxManager.installingTools.value.contains("nodejs"))

        sandboxManager.setToolInstalling("nodejs", true)
        assertTrue(sandboxManager.installingTools.value.contains("nodejs"))

        sandboxManager.setToolInstalling("nodejs", false)
        assertFalse(sandboxManager.installingTools.value.contains("nodejs"))
    }

    @Test
    fun testInstallAllRejectsConcurrentExecution() = runBlocking {
        val installingAllField = SandboxManager::class.java.getDeclaredField("_isInstallingAll")
        installingAllField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val installingAllFlow = installingAllField.get(sandboxManager) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
        installingAllFlow.value = true

        val result = sandboxManager.installAll()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Installation is already in progress", result.exceptionOrNull()?.message)
    }

    @Test
    fun testInstallAllSucceedsImmediatelyWhenEverythingReady() = runBlocking {
        val rootfs = sandboxManager.getRootfsDir()
        File(rootfs, "bin").mkdirs()
        File(rootfs, "bin/sh").writeText("#!/bin/sh\n")
        File(rootfs, "usr/bin").mkdirs()
        File(rootfs, "usr/bin/node").writeText("")
        File(rootfs, "usr/bin/python3").writeText("")
        File(rootfs, "usr/bin/git").writeText("")
        File(rootfs, "usr/bin/clang").writeText("")

        val statusField = SandboxManager::class.java.getDeclaredField("_status")
        statusField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val statusFlow = statusField.get(sandboxManager) as kotlinx.coroutines.flow.MutableStateFlow<SandboxStatus>
        statusFlow.value = SandboxStatus.READY

        val result = sandboxManager.installAll()
        assertTrue(result.isSuccess)
        assertFalse(sandboxManager.isInstallingAll.value)
        assertEquals(1.0f, sandboxManager.installAllProgress.value)
    }

    @Test
    fun testIsToolInstalledRecognizesCAlias() {
        val rootfs = sandboxManager.getRootfsDir()
        val clang = File(rootfs, "usr/bin/clang")
        clang.parentFile?.mkdirs()
        clang.writeText("#!/bin/sh\n")

        assertTrue(sandboxManager.isToolInstalled("c"))
        assertTrue(sandboxManager.isToolInstalled("cpp"))
        assertTrue(sandboxManager.isToolInstalled("c/c++"))
    }

    @Test
    fun testSetupBasicEnvironmentGeneratesPosixValidKlixEnv() {
        val rootfs = sandboxManager.getRootfsDir().apply { mkdirs() }
        sandboxManager.setupBasicEnvironment(rootfs)

        val envFile = File(rootfs, "etc/klix_env.sh")
        assertTrue("Expected klix_env.sh to be generated", envFile.exists())
        val content = envFile.readText()
        assertFalse("POSIX dash forbids '+' in function names", content.contains("clang++()"))
        assertTrue("Expected POSIX-valid clangxx function", content.contains("clangxx()"))
        assertTrue("Expected clang++ alias to clangxx", content.contains("alias clang++='clangxx'"))
    }

    @Test
    fun testExtractTarGzHandlesLongPathsViaPaxGnuAndUstarPrefix() = runBlocking {
        // 1. PAX long path (>100 chars) + long symlink target (>100 chars)
        val paxLongPath = "a".repeat(55) + "/" + "b".repeat(55) + "/" + "c".repeat(45) + "/pax_file.txt"
        val paxLongSymlinkTarget = "../" + "target_segment_".repeat(8) + "real.txt"
        assertTrue(paxLongPath.length > 150)
        assertTrue(paxLongSymlinkTarget.length > 110)

        val paxArchive = File(tempDir, "pax.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(paxArchive))).use { tos ->
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            val payload = "pax-content".toByteArray(Charsets.UTF_8)
            val fileEntry = TarArchiveEntry(paxLongPath).apply {
                size = payload.size.toLong()
                mode = 0b110_100_100
            }
            tos.putArchiveEntry(fileEntry)
            tos.write(payload)
            tos.closeArchiveEntry()

            val linkEntry = TarArchiveEntry("links/pax_symlink", TarConstants.LF_SYMLINK).apply {
                linkName = paxLongSymlinkTarget
            }
            tos.putArchiveEntry(linkEntry)
            tos.closeArchiveEntry()
        }

        val recordingOps = RecordingTarPlatformOps(SystemTarPlatformOps)
        val extractor = TarGzExtractor(recordingOps)
        val paxDest = File(tempDir, "pax_out")
        val paxResult = extractor.extract(paxArchive, paxDest)
        assertTrue("PAX extraction failed: ${paxResult.exceptionOrNull()}", paxResult.isSuccess)
        assertEquals("pax-content", File(paxDest, paxLongPath).readText())
        assertTrue(
            recordingOps.symlinks.any {
                it.first == paxDest.toPath().toAbsolutePath().normalize().resolve("links/pax_symlink") &&
                    it.second == paxLongSymlinkTarget
            }
        )

        // 2. GNU long path (>100 chars) + long symlink target (>100 chars)
        val gnuLongPath = "gnu_dir_".repeat(15) + "deep/gnu_file.txt"
        val gnuLongTarget = "gnu_target_".repeat(11) + "bin"
        assertTrue(gnuLongPath.length > 130)
        assertTrue(gnuLongTarget.length > 110)

        val gnuArchive = File(tempDir, "gnu.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(gnuArchive))).use { tos ->
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            val payload = "gnu-content".toByteArray(Charsets.UTF_8)
            val fileEntry = TarArchiveEntry(gnuLongPath).apply {
                size = payload.size.toLong()
                mode = 0b110_100_100
            }
            tos.putArchiveEntry(fileEntry)
            tos.write(payload)
            tos.closeArchiveEntry()

            val linkEntry = TarArchiveEntry("gnu_sym", TarConstants.LF_SYMLINK).apply {
                linkName = gnuLongTarget
            }
            tos.putArchiveEntry(linkEntry)
            tos.closeArchiveEntry()
        }

        val gnuDest = File(tempDir, "gnu_out")
        val gnuResult = extractor.extract(gnuArchive, gnuDest)
        assertTrue("GNU extraction failed: ${gnuResult.exceptionOrNull()}", gnuResult.isSuccess)
        assertEquals("gnu-content", File(gnuDest, gnuLongPath).readText())
        assertTrue(
            recordingOps.symlinks.any {
                it.first == gnuDest.toPath().toAbsolutePath().normalize().resolve("gnu_sym") &&
                    it.second == gnuLongTarget
            }
        )

        // 3. Raw POSIX USTAR offset-345 prefix (>100 chars combined) via SandboxManager.extractTarGz
        val ustarPrefix = "ustar_prefix_seg_1/ustar_prefix_seg_2/ustar_prefix_seg_3/ustar_prefix_seg_4/ustar_prefix_seg_5/ustar_prefix_seg_6"
        val ustarName = "ustar_prefixed_file.txt"
        assertTrue(ustarPrefix.length in 101..155)
        assertTrue((ustarPrefix.length + 1 + ustarName.length) > 120)

        val ustarArchive = File(tempDir, "ustar.tar.gz")
        writeRawUstarArchive(ustarArchive, ustarPrefix, ustarName, "ustar-prefix-ok".toByteArray(Charsets.UTF_8))

        val ustarDest = File(tempDir, "ustar_out")
        val ustarResult = sandboxManager.extractTarGz(ustarArchive, ustarDest)
        assertTrue("USTAR prefix extraction failed: ${ustarResult.exceptionOrNull()}", ustarResult.isSuccess)
        assertEquals("ustar-prefix-ok", File(ustarDest, "$ustarPrefix/$ustarName").readText())
    }

    @Test
    fun testExtractTarGzResolvesForwardAndChainedHardlinksAndFailsOnMissingTarget() = runBlocking {
        val archive = File(tempDir, "hardlinks.tar.gz")
        val payload = "hello-hardlink-binary".toByteArray(Charsets.UTF_8)
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(archive))).use { tos ->
            // Chained forward hardlink: alias2 -> alias1 -> real_tool
            val alias2 = TarArchiveEntry("bin/alias2", TarConstants.LF_LINK).apply {
                linkName = "bin/alias1"
            }
            tos.putArchiveEntry(alias2)
            tos.closeArchiveEntry()

            val alias1 = TarArchiveEntry("bin/alias1", TarConstants.LF_LINK).apply {
                linkName = "bin/real_tool"
            }
            tos.putArchiveEntry(alias1)
            tos.closeArchiveEntry()

            val realTool = TarArchiveEntry("bin/real_tool").apply {
                size = payload.size.toLong()
                mode = 0b111_101_101
            }
            tos.putArchiveEntry(realTool)
            tos.write(payload)
            tos.closeArchiveEntry()
        }

        val dest = File(tempDir, "hardlinks_out")
        val result = sandboxManager.extractTarGz(archive, dest)
        assertTrue("Forward/chained hardlink extraction failed: ${result.exceptionOrNull()}", result.isSuccess)

        val realPath = File(dest, "bin/real_tool").toPath()
        val alias1Path = File(dest, "bin/alias1").toPath()
        val alias2Path = File(dest, "bin/alias2").toPath()

        assertArrayEquals(payload, Files.readAllBytes(alias1Path))
        assertArrayEquals(payload, Files.readAllBytes(alias2Path))
        assertTrue(Files.isSameFile(realPath, alias1Path))
        assertTrue(Files.isSameFile(realPath, alias2Path))

        // Missing hardlink target must fail loudly
        val brokenArchive = File(tempDir, "broken_hardlink.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(brokenArchive))).use { tos ->
            val orphan = TarArchiveEntry("bin/orphan", TarConstants.LF_LINK).apply {
                linkName = "bin/does_not_exist"
            }
            tos.putArchiveEntry(orphan)
            tos.closeArchiveEntry()
        }
        val brokenResult = sandboxManager.extractTarGz(brokenArchive, File(tempDir, "broken_out"))
        assertTrue(brokenResult.isFailure)
    }

    @Test
    fun testExtractTarGzPreservesPosixModesAndDefersReadOnlyDirectoryModes() {
        val archive = File(tempDir, "modes.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(archive))).use { tos ->
            // Parent directory with read-only 0555 mode BEFORE child files
            val roDir = TarArchiveEntry("ro_dir/").apply {
                mode = 0b101_101_101 // 0555
            }
            tos.putArchiveEntry(roDir)
            tos.closeArchiveEntry()

            val execData = "#!/bin/sh\necho hi\n".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("ro_dir/run.sh").apply {
                size = execData.size.toLong()
                mode = 0b111_101_101 // 0755
            })
            tos.write(execData)
            tos.closeArchiveEntry()

            val secretData = "secret".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("ro_dir/secret.key").apply {
                size = secretData.size.toLong()
                mode = 0b110_000_000 // 0600
            })
            tos.write(secretData)
            tos.closeArchiveEntry()

            val normalData = "normal".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("readme.txt").apply {
                size = normalData.size.toLong()
                mode = 0b110_100_100 // 0644
            })
            tos.write(normalData)
            tos.closeArchiveEntry()

            // Symlink must never receive chmod
            tos.putArchiveEntry(TarArchiveEntry("ro_dir/link.sh", TarConstants.LF_SYMLINK).apply {
                linkName = "run.sh"
                mode = 0b111_111_111
            })
            tos.closeArchiveEntry()
        }

        val recordingOps = RecordingTarPlatformOps(SystemTarPlatformOps)
        val extractor = TarGzExtractor(recordingOps)
        val dest = File(tempDir, "modes_out")
        val result = extractor.extract(archive, dest)
        assertTrue("Mode extraction failed: ${result.exceptionOrNull()}", result.isSuccess)

        val destRoot = dest.toPath().toAbsolutePath().normalize()
        val roDirPath = destRoot.resolve("ro_dir")
        val runPath = destRoot.resolve("ro_dir/run.sh")
        val secretPath = destRoot.resolve("ro_dir/secret.key")
        val readmePath = destRoot.resolve("readme.txt")
        val linkPath = destRoot.resolve("ro_dir/link.sh")

        assertEquals(0b111_101_101, recordingOps.modes[runPath])
        assertEquals(0b110_000_000, recordingOps.modes[secretPath])
        assertEquals(0b110_100_100, recordingOps.modes[readmePath])
        assertEquals(0b101_101_101, recordingOps.modes[roDirPath])
        assertFalse("chmod must never be called on symlinks", recordingOps.modes.containsKey(linkPath))

        // Verify directory mode 0555 was applied AFTER child file modes
        val runIdx = recordingOps.modeOrder.indexOf(runPath)
        val secretIdx = recordingOps.modeOrder.indexOf(secretPath)
        val roDirIdx = recordingOps.modeOrder.indexOf(roDirPath)
        assertTrue(roDirIdx > runIdx && roDirIdx > secretIdx)

        // Direct modeToPosixPermissions mapping check
        assertEquals(
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
            ),
            modeToPosixPermissions(0b111_101_101)
        )
        assertEquals(
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            modeToPosixPermissions(0b110_000_000)
        )

        // When running on a POSIX host, verify actual filesystem permissions
        val posixView = Files.getFileAttributeView(runPath, PosixFileAttributeView::class.java)
        if (posixView != null) {
            assertEquals(modeToPosixPermissions(0b111_101_101), Files.getPosixFilePermissions(runPath))
            assertEquals(modeToPosixPermissions(0b110_000_000), Files.getPosixFilePermissions(secretPath))
            assertEquals(modeToPosixPermissions(0b101_101_101), Files.getPosixFilePermissions(roDirPath))
        }
    }

    @Test
    fun testExtractTarGzRejectsPathTraversalAndLinkEscapes() = runBlocking {
        val outsideTarget = File(tempDir, "outside_secret.txt").apply { writeText("do-not-touch") }
        val outsideEvil = File(tempDir, "escape.txt")

        // 1. Relative '..' traversal
        val relTraversal = File(tempDir, "rel_traversal.tar.gz")
        createSingleFileTarGz(relTraversal, "../escape.txt", "pwned")
        val dest1 = File(tempDir, "dest1")
        assertTrue(sandboxManager.extractTarGz(relTraversal, dest1).isFailure)
        assertFalse(outsideEvil.exists())

        // 2. Nested '..' traversal
        val nestedTraversal = File(tempDir, "nested_traversal.tar.gz")
        createSingleFileTarGz(nestedTraversal, "sub/../../escape.txt", "pwned")
        assertTrue(sandboxManager.extractTarGz(nestedTraversal, File(tempDir, "dest2")).isFailure)
        assertFalse(outsideEvil.exists())

        // 3. Absolute entry path
        val absEntry = File(tempDir, "abs_entry.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(absEntry))).use { tos ->
            val data = "pwned".toByteArray()
            val entry = TarArchiveEntry("/abs_escape.txt", true).apply { size = data.size.toLong() }
            tos.putArchiveEntry(entry)
            tos.write(data)
            tos.closeArchiveEntry()
        }
        assertTrue(sandboxManager.extractTarGz(absEntry, File(tempDir, "dest3")).isFailure)

        // 4. Relative escaping symlink
        val relSymlinkEscape = File(tempDir, "rel_symlink_escape.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(relSymlinkEscape))).use { tos ->
            val entry = TarArchiveEntry("evil_link", TarConstants.LF_SYMLINK).apply {
                linkName = "../../outside_secret.txt"
            }
            tos.putArchiveEntry(entry)
            tos.closeArchiveEntry()
        }
        assertTrue(sandboxManager.extractTarGz(relSymlinkEscape, File(tempDir, "dest4")).isFailure)

        // 5. Absolute symlink rejected when allowAbsoluteSymlinks=false, allowed when true
        val absSymlink = File(tempDir, "abs_symlink.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(absSymlink))).use { tos ->
            val entry = TarArchiveEntry("bin/sh", TarConstants.LF_SYMLINK).apply {
                linkName = "/bin/busybox"
            }
            tos.putArchiveEntry(entry)
            tos.closeArchiveEntry()
        }
        assertTrue(sandboxManager.extractTarGz(absSymlink, File(tempDir, "dest5_default")).isFailure)
        val recordingOps = RecordingTarPlatformOps(SystemTarPlatformOps)
        assertTrue(
            TarGzExtractor(recordingOps).extract(
                absSymlink,
                File(tempDir, "dest5_chroot"),
                allowAbsoluteSymlinks = true
            ).isSuccess
        )

        // 6. Hardlink target escape
        val hardlinkEscape = File(tempDir, "hardlink_escape.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(hardlinkEscape))).use { tos ->
            val entry = TarArchiveEntry("evil_hardlink", TarConstants.LF_LINK).apply {
                linkName = "../outside_secret.txt"
            }
            tos.putArchiveEntry(entry)
            tos.closeArchiveEntry()
        }
        assertTrue(sandboxManager.extractTarGz(hardlinkEscape, File(tempDir, "dest6")).isFailure)
        assertEquals("do-not-touch", outsideTarget.readText())

        // 7. Traversal through an intermediate symlink directory
        val symlinkDirTraversal = File(tempDir, "symlink_dir_traversal.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(symlinkDirTraversal))).use { tos ->
            tos.putArchiveEntry(TarArchiveEntry("sub/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()
            tos.putArchiveEntry(TarArchiveEntry("dir_link", TarConstants.LF_SYMLINK).apply {
                linkName = "sub"
            })
            tos.closeArchiveEntry()
            val data = "via-symlink".toByteArray()
            tos.putArchiveEntry(TarArchiveEntry("dir_link/pwned.txt").apply {
                size = data.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(data)
            tos.closeArchiveEntry()
        }
        assertTrue(sandboxManager.extractTarGz(symlinkDirTraversal, File(tempDir, "dest7")).isFailure)
    }

    @Test
    fun testExtractTarGzTreatsTrailingSlashSymlinkAsSymlinkNotDirectory() {
        // Valid trailing-slash symlink must create a symlink, never a real directory
        val validArchive = File(tempDir, "trailing_slash_symlink_valid.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(validArchive))).use { tos ->
            tos.putArchiveEntry(TarArchiveEntry("sub/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()
            tos.putArchiveEntry(TarArchiveEntry("dir_link/", TarConstants.LF_SYMLINK).apply {
                linkName = "sub"
            })
            tos.closeArchiveEntry()
        }
        val validDest = File(tempDir, "trailing_valid_out")
        val validOps = RecordingTarPlatformOps(SystemTarPlatformOps)
        val validResult = TarGzExtractor(validOps).extract(validArchive, validDest)
        assertTrue("Valid trailing-slash symlink failed: ${validResult.exceptionOrNull()}", validResult.isSuccess)
        val validLinkPath = validDest.toPath().toAbsolutePath().normalize().resolve("dir_link")
        assertFalse(Files.isDirectory(validLinkPath, LinkOption.NOFOLLOW_LINKS))
        assertTrue(validOps.symlinks.any { it.first == validLinkPath && it.second == "sub" })

        // Trailing-slash symlink followed by entry inside dir_link/ must be rejected
        val traversalArchive = File(tempDir, "trailing_slash_symlink_traversal.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(traversalArchive))).use { tos ->
            tos.putArchiveEntry(TarArchiveEntry("sub/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()
            tos.putArchiveEntry(TarArchiveEntry("dir_link/", TarConstants.LF_SYMLINK).apply {
                linkName = "sub"
            })
            tos.closeArchiveEntry()
            val data = "pwned".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("dir_link/pwned.txt").apply {
                size = data.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(data)
            tos.closeArchiveEntry()
        }
        val traversalDest = File(tempDir, "trailing_traversal_out")
        val traversalOps = RecordingTarPlatformOps(SystemTarPlatformOps)
        val traversalResult = TarGzExtractor(traversalOps).extract(traversalArchive, traversalDest)
        assertTrue(traversalResult.isFailure)
        assertTrue(traversalResult.exceptionOrNull() is SecurityException)
        assertFalse(File(traversalDest, "sub/pwned.txt").exists())
        assertFalse(File(traversalDest, "dir_link/pwned.txt").exists())
    }

    @Test
    fun testExtractTarGzHandlesSelfReferentialHardlink() = runBlocking {
        // Existing regular file followed by self-referential hardlink a -> a preserves file
        val selfValidArchive = File(tempDir, "self_hardlink_valid.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(selfValidArchive))).use { tos ->
            val data = "important-data".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("a").apply {
                size = data.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(data)
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("a", TarConstants.LF_LINK).apply {
                linkName = "a"
            })
            tos.closeArchiveEntry()
        }

        val validDest = File(tempDir, "self_hl_valid_out")
        val validResult = sandboxManager.extractTarGz(selfValidArchive, validDest)
        assertTrue("Self-referential hardlink failed: ${validResult.exceptionOrNull()}", validResult.isSuccess)
        val aFile = File(validDest, "a")
        assertTrue(aFile.exists())
        assertEquals("important-data", aFile.readText())

        // Missing target self-referential hardlink a -> a must still fail loudly
        val selfMissingArchive = File(tempDir, "self_hardlink_missing.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(selfMissingArchive))).use { tos ->
            tos.putArchiveEntry(TarArchiveEntry("a", TarConstants.LF_LINK).apply {
                linkName = "a"
            })
            tos.closeArchiveEntry()
        }
        val missingResult = sandboxManager.extractTarGz(selfMissingArchive, File(tempDir, "self_hl_missing_out"))
        assertTrue(missingResult.isFailure)
    }

    @Test
    fun testExtractTarGzExtractsAllMembersOfConcatenatedMultiMemberTarGz() = runBlocking {
        val part1 = File(tempDir, "part1.tar.gz")
        val part2 = File(tempDir, "part2.tar.gz")
        createSingleFileTarGz(part1, "first.txt", "member-one")
        createSingleFileTarGz(part2, "second.txt", "member-two")

        val concatArchive = File(tempDir, "concat.tar.gz")
        concatArchive.writeBytes(part1.readBytes() + part2.readBytes())

        val concatDest = File(tempDir, "concat_out")
        val concatResult = sandboxManager.extractTarGz(concatArchive, concatDest)
        assertTrue("Concatenated tar.gz failed: ${concatResult.exceptionOrNull()}", concatResult.isSuccess)
        assertEquals("member-one", File(concatDest, "first.txt").readText())
        assertEquals("member-two", File(concatDest, "second.txt").readText())

        // Also test concatenated tar.gz where member 1 has GNU 10KB zero padding
        val paddedPart1 = File(tempDir, "padded_part1.tar.gz")
        val tarBytes = ByteArrayOutputStream().apply {
            TarArchiveOutputStream(this).use { tos ->
                val data = "padded-one".toByteArray(Charsets.UTF_8)
                tos.putArchiveEntry(TarArchiveEntry("padded1.txt").apply {
                    size = data.size.toLong()
                    mode = 0b110_100_100
                })
                tos.write(data)
                tos.closeArchiveEntry()
            }
            val remainder = size() % 10240
            if (remainder != 0) {
                write(ByteArray(10240 - remainder))
            }
        }.toByteArray()
        GzipCompressorOutputStream(FileOutputStream(paddedPart1)).use { it.write(tarBytes) }

        val paddedConcat = File(tempDir, "padded_concat.tar.gz")
        paddedConcat.writeBytes(paddedPart1.readBytes() + part2.readBytes())
        val paddedDest = File(tempDir, "padded_concat_out")
        val paddedResult = sandboxManager.extractTarGz(paddedConcat, paddedDest)
        assertTrue("Padded concatenated tar.gz failed: ${paddedResult.exceptionOrNull()}", paddedResult.isSuccess)
        assertEquals("padded-one", File(paddedDest, "padded1.txt").readText())
        assertEquals("member-two", File(paddedDest, "second.txt").readText())

        // Empty valid tar.gz succeeds cleanly
        val emptyArchive = File(tempDir, "empty.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(emptyArchive))).use { }
        assertTrue(sandboxManager.extractTarGz(emptyArchive, File(tempDir, "empty_out")).isSuccess)
    }

    @Test
    fun testExtractTarGzHandlesSymlinkAndHardlinkOverwriteOrdering() {
        // 1. Symlink s -> real, overwritten by hardlink s -> real, then hardlink hl2 -> s
        val symThenHlArchive = File(tempDir, "sym_then_hl.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(symThenHlArchive))).use { tos ->
            val realData = "real-data".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("real").apply {
                size = realData.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(realData)
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("s", TarConstants.LF_SYMLINK).apply {
                linkName = "real"
            })
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("s", TarConstants.LF_LINK).apply {
                linkName = "real"
            })
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("hl2", TarConstants.LF_LINK).apply {
                linkName = "s"
            })
            tos.closeArchiveEntry()
        }

        val dest1 = File(tempDir, "sym_then_hl_out")
        val result1 = TarGzExtractor(SystemTarPlatformOps).extract(symThenHlArchive, dest1)
        assertTrue("Hardlink overwriting symlink failed: ${result1.exceptionOrNull()}", result1.isSuccess)
        val realPath1 = File(dest1, "real").toPath()
        val sPath1 = File(dest1, "s").toPath()
        val hl2Path1 = File(dest1, "hl2").toPath()
        assertTrue(Files.isRegularFile(sPath1, LinkOption.NOFOLLOW_LINKS))
        assertTrue(Files.isSameFile(realPath1, sPath1))
        assertTrue(Files.isSameFile(realPath1, hl2Path1))
        assertEquals("real-data", hl2Path1.toFile().readText())

        // 2. Hardlink s -> real, superseded by later symlink s -> sub
        val hlThenSymArchive = File(tempDir, "hl_then_sym.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(hlThenSymArchive))).use { tos ->
            val realData = "real-data".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("real").apply {
                size = realData.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(realData)
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("sub/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("s", TarConstants.LF_LINK).apply {
                linkName = "real"
            })
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("s", TarConstants.LF_SYMLINK).apply {
                linkName = "sub"
            })
            tos.closeArchiveEntry()
        }

        val dest2 = File(tempDir, "hl_then_sym_out")
        val ops2 = RecordingTarPlatformOps(SystemTarPlatformOps)
        val result2 = TarGzExtractor(ops2).extract(hlThenSymArchive, dest2)
        assertTrue("Symlink superseding hardlink failed: ${result2.exceptionOrNull()}", result2.isSuccess)
        val sPath2 = dest2.toPath().toAbsolutePath().normalize().resolve("s")
        assertTrue(ops2.symlinks.any { it.first == sPath2 && it.second == "sub" })
        assertFalse("Superseded hardlink must not clobber later symlink", ops2.hardLinks.any { it.first == sPath2 })
        assertFalse(Files.isRegularFile(sPath2, LinkOption.NOFOLLOW_LINKS))
    }

    @Test
    fun testExtractTarGzOverwritesReadOnlyRegularFile() = runBlocking {
        val archive = File(tempDir, "overwrite_ro.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(archive))).use { tos ->
            val v1 = "version-1".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("config.txt").apply {
                size = v1.size.toLong()
                mode = 0b100_000_000 // 0400
            })
            tos.write(v1)
            tos.closeArchiveEntry()

            val v2 = "version-2-updated".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("config.txt").apply {
                size = v2.size.toLong()
                mode = 0b100_000_000 // 0400
            })
            tos.write(v2)
            tos.closeArchiveEntry()
        }

        val dest = File(tempDir, "overwrite_ro_out")
        val result = sandboxManager.extractTarGz(archive, dest)
        assertTrue("Overwriting 0400 read-only file failed: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals("version-2-updated", File(dest, "config.txt").readText())
    }

    @Test
    fun testExtractTarGzRejectsTruncatedSub512PayloadsAndTrailingGarbage() = runBlocking {
        // 1. 0-byte file
        val zeroByteFile = File(tempDir, "zero_bytes.tar.gz").apply { writeBytes(byteArrayOf()) }
        val rZeroFile = sandboxManager.extractTarGz(zeroByteFile, File(tempDir, "zero_file_out"))
        assertTrue(rZeroFile.isFailure)
        assertTrue(rZeroFile.exceptionOrNull() is IOException)

        // 2. Valid .gz wrapping 0 uncompressed bytes
        val emptyGz = File(tempDir, "empty_stream.tar.gz")
        GzipCompressorOutputStream(FileOutputStream(emptyGz)).use { }
        val rEmptyGz = sandboxManager.extractTarGz(emptyGz, File(tempDir, "empty_gz_out"))
        assertTrue(rEmptyGz.isFailure)
        assertTrue(rEmptyGz.exceptionOrNull() is IOException)

        // 3. Valid .gz wrapping 32-byte non-TAR payload (N15)
        val sub512Invalid = File(tempDir, "sub512_invalid.tar.gz")
        GzipCompressorOutputStream(FileOutputStream(sub512Invalid)).use { gzos ->
            gzos.write("NOT_A_VALID_TAR_HEADER_TRUNCATED".toByteArray(Charsets.US_ASCII))
        }
        val rSub512 = sandboxManager.extractTarGz(sub512Invalid, File(tempDir, "sub512_out"))
        assertTrue("Expected <512B invalid TAR to fail", rSub512.isFailure)
        assertTrue(rSub512.exceptionOrNull() is IOException)

        // 4. Valid TAR followed by 100 bytes of 0x42 inside the same .gz stream (N8)
        val validTarBytes = ByteArrayOutputStream().apply {
            TarArchiveOutputStream(this).use { tos ->
                val data = "hello".toByteArray(Charsets.UTF_8)
                tos.putArchiveEntry(TarArchiveEntry("hello.txt").apply {
                    size = data.size.toLong()
                    mode = 0b110_100_100
                })
                tos.write(data)
                tos.closeArchiveEntry()
            }
        }.toByteArray()

        val trailing100Gz = File(tempDir, "trailing_100.tar.gz")
        GzipCompressorOutputStream(FileOutputStream(trailing100Gz)).use { gzos ->
            gzos.write(validTarBytes)
            gzos.write(ByteArray(100) { 0x42 })
        }
        val rTrailing100 = sandboxManager.extractTarGz(trailing100Gz, File(tempDir, "trailing_100_out"))
        assertTrue("Expected <512B trailing garbage to fail", rTrailing100.isFailure)
        assertTrue(rTrailing100.exceptionOrNull() is IOException)

        // 5. Valid TAR + single 512-byte EOF block + 100 bytes of 0x42 inside .gz
        val singleEofPlus100 = File(tempDir, "single_eof_trailing_100.tar.gz")
        GzipCompressorOutputStream(FileOutputStream(singleEofPlus100)).use { gzos ->
            // Strip second 512-byte EOF block so only 1 EOF block precedes 100-byte garbage
            gzos.write(validTarBytes, 0, validTarBytes.size - 512)
            gzos.write(ByteArray(100) { 0x42 })
        }
        val rSingleEof = sandboxManager.extractTarGz(singleEofPlus100, File(tempDir, "single_eof_out"))
        assertTrue("Expected 1-EOF + 100B garbage to fail", rSingleEof.isFailure)
        assertTrue(rSingleEof.exceptionOrNull() is IOException)

        // 6. Valid TAR followed by 512 bytes of 0x42 inside .gz
        val trailing512Gz = File(tempDir, "trailing_512.tar.gz")
        GzipCompressorOutputStream(FileOutputStream(trailing512Gz)).use { gzos ->
            gzos.write(validTarBytes)
            gzos.write(ByteArray(512) { 0x42 })
        }
        val rTrailing512 = sandboxManager.extractTarGz(trailing512Gz, File(tempDir, "trailing_512_out"))
        assertTrue(rTrailing512.isFailure)
        assertTrue(rTrailing512.exceptionOrNull() is IOException)

        // 7. Valid empty TAR (1024 zero bytes) + data TAR concatenated still succeeds cleanly
        val emptyTarGz = File(tempDir, "empty_1024.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(emptyTarGz))).use { }
        val dataTarGz = File(tempDir, "data_part.tar.gz")
        createSingleFileTarGz(dataTarGz, "data.txt", "after-empty")
        val emptyPlusData = File(tempDir, "empty_plus_data.tar.gz")
        emptyPlusData.writeBytes(emptyTarGz.readBytes() + dataTarGz.readBytes())
        val emptyPlusDataDest = File(tempDir, "empty_plus_data_out")
        val rEmptyPlusData = sandboxManager.extractTarGz(emptyPlusData, emptyPlusDataDest)
        assertTrue("Empty TAR + data TAR failed: ${rEmptyPlusData.exceptionOrNull()}", rEmptyPlusData.isSuccess)
        assertEquals("after-empty", File(emptyPlusDataDest, "data.txt").readText())
    }

    @Test
    fun testExtractTarGzHandlesTypeMismatchOverwritesAndDoesNotSwallowFileAlreadyExists() = runBlocking {
        // 1. file -> dir (even when original file is 0400 read-only)
        val f2dArchive = File(tempDir, "file_to_dir.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(f2dArchive))).use { tos ->
            val oldData = "old-file".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("item").apply {
                size = oldData.size.toLong()
                mode = 0b100_000_000 // 0400
            })
            tos.write(oldData)
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("item/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()

            val childData = "inside-dir".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("item/child.txt").apply {
                size = childData.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(childData)
            tos.closeArchiveEntry()
        }
        val f2dDest = File(tempDir, "f2d_out")
        val f2dResult = sandboxManager.extractTarGz(f2dArchive, f2dDest)
        assertTrue("file->dir failed: ${f2dResult.exceptionOrNull()}", f2dResult.isSuccess)
        assertTrue(Files.isDirectory(File(f2dDest, "item").toPath(), LinkOption.NOFOLLOW_LINKS))
        assertEquals("inside-dir", File(f2dDest, "item/child.txt").readText())

        // 2. dir -> file (directory with nested 0400 file & 0555 subdir replaced by regular file)
        val d2fArchive = File(tempDir, "dir_to_file.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(d2fArchive))).use { tos ->
            tos.putArchiveEntry(TarArchiveEntry("item/").apply { mode = 0b101_101_101 })
            tos.closeArchiveEntry()
            tos.putArchiveEntry(TarArchiveEntry("item/nested/").apply { mode = 0b101_101_101 })
            tos.closeArchiveEntry()
            val nestedData = "old-nested".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("item/nested/old.txt").apply {
                size = nestedData.size.toLong()
                mode = 0b100_000_000
            })
            tos.write(nestedData)
            tos.closeArchiveEntry()

            val newData = "replaced-by-file".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("item").apply {
                size = newData.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(newData)
            tos.closeArchiveEntry()
        }
        val d2fDest = File(tempDir, "d2f_out")
        val d2fResult = sandboxManager.extractTarGz(d2fArchive, d2fDest)
        assertTrue("dir->file failed: ${d2fResult.exceptionOrNull()}", d2fResult.isSuccess)
        val d2fItem = File(d2fDest, "item")
        assertTrue(Files.isRegularFile(d2fItem.toPath(), LinkOption.NOFOLLOW_LINKS))
        assertEquals("replaced-by-file", d2fItem.readText())
        assertFalse(File(d2fDest, "item/nested").exists())

        // 3. dir -> sym (existing directory unlinked so no directory remains on disk)
        val d2sArchive = File(tempDir, "dir_to_sym.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(d2sArchive))).use { tos ->
            tos.putArchiveEntry(TarArchiveEntry("item/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()
            val oldData = "old".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("item/old.txt").apply {
                size = oldData.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(oldData)
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("other/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()

            tos.putArchiveEntry(TarArchiveEntry("item", TarConstants.LF_SYMLINK).apply {
                linkName = "other"
            })
            tos.closeArchiveEntry()
        }
        val d2sDest = File(tempDir, "d2s_out")
        val d2sOps = RecordingTarPlatformOps(SystemTarPlatformOps)
        val d2sResult = TarGzExtractor(d2sOps).extract(d2sArchive, d2sDest)
        assertTrue("dir->sym failed: ${d2sResult.exceptionOrNull()}", d2sResult.isSuccess)
        val d2sItemPath = d2sDest.toPath().toAbsolutePath().normalize().resolve("item")
        assertFalse("Directory must not remain after dir->sym overwrite", Files.isDirectory(d2sItemPath, LinkOption.NOFOLLOW_LINKS))
        assertTrue(d2sOps.symlinks.any { it.first == d2sItemPath && it.second == "other" })

        // Verify SystemTarPlatformOps.createSymlink never swallows FileAlreadyExistsException even on Windows
        val existingDir = File(tempDir, "existing_dir_for_symlink").apply { mkdirs() }.toPath()
        var threwAlreadyExists = false
        try {
            SystemTarPlatformOps.createSymlink(existingDir, "other")
        } catch (_: FileAlreadyExistsException) {
            threwAlreadyExists = true
        }
        assertTrue("SystemTarPlatformOps.createSymlink must throw FileAlreadyExistsException when target exists", threwAlreadyExists)

        // 4. sym -> dir (existing symlink unlinked before creating real directory)
        val s2dArchive = File(tempDir, "sym_to_dir.tar.gz")
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(s2dArchive))).use { tos ->
            tos.putArchiveEntry(TarArchiveEntry("other/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()
            tos.putArchiveEntry(TarArchiveEntry("item", TarConstants.LF_SYMLINK).apply {
                linkName = "other"
            })
            tos.closeArchiveEntry()
            tos.putArchiveEntry(TarArchiveEntry("item/").apply { mode = 0b111_101_101 })
            tos.closeArchiveEntry()
            val newData = "real-dir-child".toByteArray(Charsets.UTF_8)
            tos.putArchiveEntry(TarArchiveEntry("item/new.txt").apply {
                size = newData.size.toLong()
                mode = 0b110_100_100
            })
            tos.write(newData)
            tos.closeArchiveEntry()
        }
        val s2dDest = File(tempDir, "s2d_out")
        val s2dResult = sandboxManager.extractTarGz(s2dArchive, s2dDest)
        assertTrue("sym->dir failed: ${s2dResult.exceptionOrNull()}", s2dResult.isSuccess)
        val s2dItemPath = File(s2dDest, "item").toPath()
        assertTrue(Files.isDirectory(s2dItemPath, LinkOption.NOFOLLOW_LINKS))
        assertFalse(Files.isSymbolicLink(s2dItemPath))
        assertEquals("real-dir-child", File(s2dDest, "item/new.txt").readText())
        assertFalse(File(s2dDest, "other/new.txt").exists())
    }

    private fun createSingleFileTarGz(outFile: File, entryName: String, content: String) {
        TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(outFile))).use { tos ->
            val data = content.toByteArray(Charsets.UTF_8)
            val entry = TarArchiveEntry(entryName).apply {
                size = data.size.toLong()
                mode = 0b110_100_100
            }
            tos.putArchiveEntry(entry)
            tos.write(data)
            tos.closeArchiveEntry()
        }
    }

    private fun writeRawUstarArchive(outFile: File, prefix: String, name: String, payload: ByteArray) {
        val header = ByteArray(512)
        fun writeAscii(offset: Int, value: String, maxLen: Int) {
            val bytes = value.toByteArray(Charsets.US_ASCII)
            System.arraycopy(bytes, 0, header, offset, minOf(bytes.size, maxLen))
        }

        writeAscii(0, name, 100)
        writeAscii(100, "0000644\u0000", 8)
        writeAscii(108, "0000000\u0000", 8)
        writeAscii(116, "0000000\u0000", 8)
        writeAscii(124, String.format("%011o\u0000", payload.size), 12)
        writeAscii(136, "00000000000\u0000", 12)
        for (i in 148 until 156) header[i] = ' '.code.toByte()
        header[156] = '0'.code.toByte()
        writeAscii(257, "ustar\u0000", 6)
        writeAscii(263, "00", 2)
        writeAscii(329, "0000000\u0000", 8)
        writeAscii(337, "0000000\u0000", 8)
        writeAscii(345, prefix, 155)

        val checksum = header.sumOf { it.toInt() and 0xFF }
        writeAscii(148, String.format("%06o\u0000 ", checksum), 8)

        val baos = ByteArrayOutputStream()
        GzipCompressorOutputStream(baos).use { gzos ->
            gzos.write(header)
            gzos.write(payload)
            val pad = (512 - (payload.size % 512)) % 512
            if (pad > 0) gzos.write(ByteArray(pad))
            gzos.write(ByteArray(1024)) // two zero EOF blocks
        }
        outFile.writeBytes(baos.toByteArray())
    }

    private class RecordingTarPlatformOps(
        private val delegate: TarPlatformOps
    ) : TarPlatformOps {
        val modes = mutableMapOf<Path, Int>()
        val modeOrder = mutableListOf<Path>()
        val symlinks = mutableListOf<Pair<Path, String>>()
        val hardLinks = mutableListOf<Pair<Path, Path>>()

        override fun setMode(path: Path, mode: Int) {
            modes[path] = mode
            modeOrder.add(path)
            delegate.setMode(path, mode)
        }

        override fun createSymlink(linkPath: Path, target: String) {
            symlinks.add(linkPath to target)
            delegate.createSymlink(linkPath, target)
        }

        override fun createHardLink(linkPath: Path, targetPath: Path) {
            hardLinks.add(linkPath to targetPath)
            delegate.createHardLink(linkPath, targetPath)
        }
    }
}
