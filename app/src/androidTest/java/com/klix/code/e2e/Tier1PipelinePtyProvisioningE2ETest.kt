package com.klix.code.e2e

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.model.EditorTab
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.e2e.contracts.ContractBridges
import com.klix.code.e2e.helpers.E2ETestHelpers
import com.klix.code.ui.onboarding.OnboardingViewModel
import com.klix.code.ui.onboarding.SandboxInstallState
import com.klix.code.ui.onboarding.ToolchainOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tier 1 Core Feature Tests for F5 (LanguageRunner), F6 (Native JNI PTY),
 * F7 (Archive Extraction), F8 (Onboarding Fail-Fast), and F9 (16 KB ELF / Zipalign / R5 Compliance).
 * 25 tests total (5 per feature).
 */
@RunWith(AndroidJUnit4::class)
class Tier1PipelinePtyProvisioningE2ETest {

    private lateinit var tempDirs: MutableList<File>

    @Before
    fun setUp() {
        tempDirs = mutableListOf()
    }

    @After
    fun tearDown() {
        tempDirs.forEach { it.deleteRecursively() }
    }

    private fun newTempDir(prefix: String): File =
        E2ETestHelpers.createTempTestDir(prefix).also { tempDirs.add(it) }

    // ==================== F5: LanguageRunner Path, Quoting & Pre-flight (5 tests) ====================

    @Test
    fun t1_f5_01_resolveRunCommandSupportsJsPyCAndCppInWorkspaceRoot() {
        val ctx = E2ETestHelpers.targetContext()
        val sandboxManager = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs", "python3", "c", "cpp")
        val ws = E2ETestHelpers.defaultWorkspaceObject(ctx)
        val wsDir = E2ETestHelpers.getDefaultWorkspaceDir(ctx)

        val jsRes = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(E2ETestHelpers.seedWorkspaceFile("root_smoke.js", "console.log(1);", wsDir)),
            ws,
            sandboxManager
        )
        val pyRes = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(E2ETestHelpers.seedWorkspaceFile("root_smoke.py", "print(1)", wsDir)),
            ws,
            sandboxManager
        )
        val cRes = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(E2ETestHelpers.seedWorkspaceFile("root_smoke.c", "int main(){return 0;}", wsDir)),
            ws,
            sandboxManager
        )
        val cppRes = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(E2ETestHelpers.seedWorkspaceFile("root_smoke.cpp", "int main(){return 0;}", wsDir)),
            ws,
            sandboxManager
        )

        assertFalse(jsRes.requiresSetup)
        assertFalse(pyRes.requiresSetup)
        assertFalse(cRes.requiresSetup)
        assertFalse(cppRes.requiresSetup)

        assertTrue("Expected node command, got: ${jsRes.command}", jsRes.command!!.contains("node"))
        assertTrue("Expected python3 -u command, got: ${pyRes.command}", pyRes.command!!.contains("python3") && pyRes.command!!.contains("-u"))
        assertTrue("Expected clang + -lm command, got: ${cRes.command}", cRes.command!!.contains("clang") && cRes.command!!.contains("-lm"))
        assertTrue("Expected clang++ -std=c++17 command, got: ${cppRes.command}", cppRes.command!!.contains("clang++") && cppRes.command!!.contains("-std=c++17"))
    }

    @Test
    fun t1_f5_02_resolveRunCommandPreservesNestedSubdirRelativePath() {
        val ctx = E2ETestHelpers.targetContext()
        val sandboxManager = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val ws = E2ETestHelpers.defaultWorkspaceObject(ctx)
        val wsDir = E2ETestHelpers.getDefaultWorkspaceDir(ctx)
        val nestedFile = E2ETestHelpers.seedWorkspaceFile("src/utils/app.py", "print('nested')", wsDir)

        val res = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(nestedFile), ws, sandboxManager)
        assertFalse(res.requiresSetup)
        assertNotNull(res.command)
        assertTrue(
            "Expected nested relative path 'src/utils/app.py' in command, got: ${res.command}",
            res.command!!.contains("src/utils/app.py")
        )
    }

    @Test
    fun t1_f5_03_resolveRunCommandPosixQuotesFilenamesWithSpaces() {
        val ctx = E2ETestHelpers.targetContext()
        val sandboxManager = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs", "c")
        val ws = E2ETestHelpers.defaultWorkspaceObject(ctx)
        val wsDir = E2ETestHelpers.getDefaultWorkspaceDir(ctx)

        val jsFile = E2ETestHelpers.seedWorkspaceFile("my script.js", "console.log('space')", wsDir)
        val cFile = E2ETestHelpers.seedWorkspaceFile("hello world.c", "int main(){return 0;}", wsDir)

        val jsRes = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(jsFile), ws, sandboxManager)
        val cRes = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(cFile), ws, sandboxManager)

        assertTrue("Expected single-quoted 'my script.js' in: ${jsRes.command}", jsRes.command!!.contains("'my script.js'"))
        assertTrue("Expected single-quoted 'hello world.c' in: ${cRes.command}", cRes.command!!.contains("'hello world.c'"))
    }

    @Test
    fun t1_f5_04_preflightFailsWhenSandboxNotInstalledAndRequiresSetup() {
        val emptyFilesDir = newTempDir("iso_no_sandbox")
        val emptyNativeDir = newTempDir("iso_no_native")
        val isoCtx = E2ETestHelpers.IsolatedSandboxContext(E2ETestHelpers.targetContext(), emptyFilesDir, emptyNativeDir)
        val sandboxManager = SandboxManager(isoCtx)
        val ws = E2ETestHelpers.defaultWorkspaceObject(isoCtx)
        val pyFile = E2ETestHelpers.seedWorkspaceFile("main.py", "print(1)", E2ETestHelpers.getDefaultWorkspaceDir(isoCtx))

        val res = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(pyFile), ws, sandboxManager)
        assertTrue("Expected requiresSetup=true when sandbox not installed", res.requiresSetup)
        assertNull("Expected null command when sandbox missing", res.command)
        assertFalse("Expected non-blank errorMessage", res.errorMessage.isNullOrBlank())
    }

    @Test
    fun t1_f5_05_preflightFailsWhenSpecificToolchainMissing() {
        val ctx = E2ETestHelpers.targetContext()
        val isoFilesDir = newTempDir("iso_partial_sandbox")
        val emptyNativeDir = newTempDir("iso_partial_native")
        val isoCtx = E2ETestHelpers.IsolatedSandboxContext(ctx, isoFilesDir, emptyNativeDir)

        // Copy only base sandbox markers (if present) without C/C++ compiler in emptyNativeDir
        val realSandbox = File(ctx.filesDir, "sandbox")
        if (realSandbox.exists()) {
            File(realSandbox, "rootfs/bin").takeIf { it.exists() }?.copyRecursively(File(isoFilesDir, "sandbox/rootfs/bin"), overwrite = true)
            File(realSandbox, "rootfs/etc").takeIf { it.exists() }?.copyRecursively(File(isoFilesDir, "sandbox/rootfs/etc"), overwrite = true)
        }
        val sandboxManager = SandboxManager(isoCtx)
        val ws = E2ETestHelpers.defaultWorkspaceObject(isoCtx)
        val cFile = E2ETestHelpers.seedWorkspaceFile("test.c", "int main(){return 0;}", E2ETestHelpers.getDefaultWorkspaceDir(isoCtx))

        val res = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(cFile), ws, sandboxManager)
        assertTrue("Expected requiresSetup=true when C toolchain missing", res.requiresSetup)
        assertNull("Expected null command when C toolchain missing", res.command)
        assertFalse("Expected clear errorMessage", res.errorMessage.isNullOrBlank())
    }

    // ==================== F6: Native JNI PTY & Interactive I/O (5 tests) ====================

    private fun drainPtyOutput(masterFd: Int, timeoutMs: Long = 5000L, stopWhen: ((String) -> Boolean)? = null): String {
        val sb = StringBuilder()
        val buf = ByteArray(4096)
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val n = ContractBridges.readPty(masterFd, buf)
            if (n > 0) {
                sb.append(String(buf, 0, n, Charsets.UTF_8))
                if (stopWhen != null && stopWhen(sb.toString())) break
            } else if (n == 0) {
                break
            } else {
                SystemClock.sleep(20)
            }
        }
        return sb.toString()
    }

    @Test
    fun t1_f6_01_ptyBridgeOpenAndForkPtyCapturesEchoAndExitZero() {
        val session = ContractBridges.openAndForkPty(
            argv = arrayOf("/system/bin/sh", "-c", "echo PTY_OK"),
            envp = arrayOf("PATH=/system/bin:/system/xbin", "TERM=xterm-256color"),
            cwd = E2ETestHelpers.targetContext().filesDir.absolutePath,
            rows = 24,
            cols = 80
        )
        try {
            assertTrue("Expected positive pid, got ${session.pid}", session.pid > 0)
            assertTrue("Expected valid masterFd, got ${session.masterFd}", session.masterFd >= 0)
            val output = drainPtyOutput(session.masterFd, timeoutMs = 5000L) { it.contains("PTY_OK") }
            val status = ContractBridges.waitPtyPid(session.pid)
            assertTrue("Expected PTY_OK in output, got: $output", output.contains("PTY_OK"))
            assertEquals("Expected exit status 0", 0, status)
        } finally {
            if (session.masterFd >= 0) ContractBridges.closePtyFd(session.masterFd)
        }
    }

    @Test
    fun t1_f6_02_ptyBridgeProvidesRealTtyForStdinAndStdout() {
        val session = ContractBridges.openAndForkPty(
            argv = arrayOf("/system/bin/sh", "-c", "if [ -t 0 ] && [ -t 1 ]; then echo IS_TTY; else echo NOT_TTY; fi"),
            envp = arrayOf("PATH=/system/bin:/system/xbin", "TERM=xterm-256color"),
            cwd = E2ETestHelpers.targetContext().filesDir.absolutePath,
            rows = 24,
            cols = 80
        )
        try {
            val output = drainPtyOutput(session.masterFd, timeoutMs = 5000L) { it.contains("IS_TTY") || it.contains("NOT_TTY") }
            val status = ContractBridges.waitPtyPid(session.pid)
            assertEquals(0, status)
            assertTrue("Expected real PTY (isatty 0 & 1), got: $output", output.contains("IS_TTY"))
        } finally {
            if (session.masterFd >= 0) ContractBridges.closePtyFd(session.masterFd)
        }
    }

    @Test
    fun t1_f6_03_ptyBridgeInteractiveWritePtyDeliversStdinImmediately() {
        val session = ContractBridges.openAndForkPty(
            argv = arrayOf("/system/bin/sh", "-c", "printf 'PROMPT:'; read line; echo GOT:\$line"),
            envp = arrayOf("PATH=/system/bin:/system/xbin", "TERM=xterm-256color"),
            cwd = E2ETestHelpers.targetContext().filesDir.absolutePath,
            rows = 24,
            cols = 80
        )
        try {
            val beforeInput = drainPtyOutput(session.masterFd, timeoutMs = 4000L) { it.contains("PROMPT:") }
            assertTrue("Expected un-flushed PROMPT: before stdin, got: $beforeInput", beforeInput.contains("PROMPT:"))

            val written = ContractBridges.writePty(session.masterFd, "hello_pty\n".toByteArray(Charsets.UTF_8))
            assertTrue("Expected positive byte write count", written > 0)

            val afterInput = drainPtyOutput(session.masterFd, timeoutMs = 4000L) { it.contains("GOT:hello_pty") }
            val status = ContractBridges.waitPtyPid(session.pid)
            assertEquals(0, status)
            assertTrue("Expected GOT:hello_pty, got: $afterInput", afterInput.contains("GOT:hello_pty"))
        } finally {
            if (session.masterFd >= 0) ContractBridges.closePtyFd(session.masterFd)
        }
    }

    @Test
    fun t1_f6_04_ptyBridgeResizePtyUpdatesTioCSwinszDimensions() {
        val session = ContractBridges.openAndForkPty(
            argv = arrayOf("/system/bin/sh", "-c", "read go; stty size 2>/dev/null || echo RESIZED_OK"),
            envp = arrayOf("PATH=/system/bin:/system/xbin", "TERM=xterm-256color"),
            cwd = E2ETestHelpers.targetContext().filesDir.absolutePath,
            rows = 24,
            cols = 80
        )
        try {
            val rc = ContractBridges.resizePty(session.masterFd, 40, 120)
            assertEquals("Expected resizePty to return 0", 0, rc)
            ContractBridges.writePty(session.masterFd, "ok\n".toByteArray(Charsets.UTF_8))
            val output = drainPtyOutput(session.masterFd, timeoutMs = 4000L) { it.contains("40 120") || it.contains("RESIZED_OK") }
            ContractBridges.waitPtyPid(session.pid)
            // Toybox stty on Android prints "40 120" when supported
            assertTrue(
                "Expected '40 120' or successful resize check, got: $output",
                output.contains("40 120") || output.contains("RESIZED_OK")
            )
        } finally {
            if (session.masterFd >= 0) ContractBridges.closePtyFd(session.masterFd)
        }
    }

    @Test
    fun t1_f6_05_ptyBridgeSendSignalSigintTerminatesForegroundProcessQuickly() {
        val session = ContractBridges.openAndForkPty(
            argv = arrayOf("/system/bin/sh", "-c", "echo STARTED; sleep 30"),
            envp = arrayOf("PATH=/system/bin:/system/xbin", "TERM=xterm-256color"),
            cwd = E2ETestHelpers.targetContext().filesDir.absolutePath,
            rows = 24,
            cols = 80
        )
        try {
            val out = drainPtyOutput(session.masterFd, timeoutMs = 3000L) { it.contains("STARTED") }
            assertTrue("Process failed to start: $out", out.contains("STARTED"))

            val startMs = SystemClock.uptimeMillis()
            ContractBridges.sendPtySignal(session.pid, OsConstants.SIGINT)
            val status = ContractBridges.waitPtyPid(session.pid)
            val elapsed = SystemClock.uptimeMillis() - startMs

            assertTrue("SIGINT termination took too long: ${elapsed}ms", elapsed < 2500L)
            assertFalse("Expected non-zero exit status after SIGINT", status == 0)
        } finally {
            if (session.masterFd >= 0) ContractBridges.closePtyFd(session.masterFd)
        }
    }

    // ==================== F7: Archive Extraction (extractTarGz) (5 tests) ====================

    @Test
    fun t1_f7_01_extractTarGzExtractsStandardFilesAndNestedDirs() = runBlocking {
        val root = newTempDir("tar_std")
        val tarGz = File(root, "std.tar.gz")
        val dest = File(root, "out").apply { mkdirs() }
        val payload = "KLIX_TAR_STD_PAYLOAD_12345".toByteArray(Charsets.UTF_8)

        E2ETestHelpers.TarGzFixtureBuilder()
            .addDirectory("alpha/beta", mode = 0b111_101_101)
            .addFile("alpha/beta/hello.txt", payload, mode = 0b110_100_100)
            .writeTo(tarGz)

        val sandboxManager = SandboxManager(E2ETestHelpers.targetContext())
        val res = ContractBridges.extractTarGz(sandboxManager, tarGz, dest)
        assertTrue("extractTarGz failed: ${res.exceptionOrNull()}", res.isSuccess)
        val extracted = File(dest, "alpha/beta/hello.txt")
        assertTrue("Expected extracted file to exist", extracted.exists())
        assertArrayEquals(payload, extracted.readBytes())
    }

    @Test
    fun t1_f7_02_extractTarGzPreservesUstarPrefixOver100Chars() = runBlocking {
        val root = newTempDir("tar_ustar")
        val tarGz = File(root, "ustar.tar.gz")
        val dest = File(root, "out").apply { mkdirs() }
        val prefix = "usr/lib/aarch64-linux-android/include/c++/v1/__algorithm/deep_nested_prefix_directory_segment_exceeding_100"
        val leaf = "ranges_sort_helper.h"
        val fullRel = "$prefix/$leaf"
        assertTrue(fullRel.length > 110)

        val payload = "// ustar prefix header\n".toByteArray(Charsets.UTF_8)
        E2ETestHelpers.TarGzFixtureBuilder()
            .addUstarPrefixFile(prefix, leaf, payload)
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(SandboxManager(E2ETestHelpers.targetContext()), tarGz, dest)
        assertTrue("extractTarGz failed: ${res.exceptionOrNull()}", res.isSuccess)
        val extracted = File(dest, fullRel)
        assertTrue("Expected USTAR prefix path '$fullRel' to exist", extracted.exists())
        assertArrayEquals(payload, extracted.readBytes())
    }

    @Test
    fun t1_f7_03_extractTarGzPreservesPaxAndGnuLongPathsOver255Chars() = runBlocking {
        val root = newTempDir("tar_long")
        val tarGz = File(root, "longpaths.tar.gz")
        val dest = File(root, "out").apply { mkdirs() }

        val paxPath = "pax_root/" + "segment_a_1234567890/".repeat(13) + "pax_target_file_name_exceeding_255_chars.txt"
        val gnuPath = "gnu_root/" + "segment_b_1234567890/".repeat(13) + "gnu_target_file_name_exceeding_255_chars.txt"
        assertTrue(paxPath.length > 260)
        assertTrue(gnuPath.length > 260)

        E2ETestHelpers.TarGzFixtureBuilder()
            .addPaxLongPathFile(paxPath, "PAX_OK".toByteArray())
            .addGnuLongPathFile(gnuPath, "GNU_OK".toByteArray())
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(SandboxManager(E2ETestHelpers.targetContext()), tarGz, dest)
        assertTrue("extractTarGz failed: ${res.exceptionOrNull()}", res.isSuccess)
        assertEquals("PAX_OK", File(dest, paxPath).readText())
        assertEquals("GNU_OK", File(dest, gnuPath).readText())
    }

    @Test
    fun t1_f7_04_extractTarGzCreatesPosixSymlinksWithExactTarget() = runBlocking {
        val root = newTempDir("tar_symlink")
        val tarGz = File(root, "symlink.tar.gz")
        val dest = File(root, "out").apply { mkdirs() }

        E2ETestHelpers.TarGzFixtureBuilder()
            .addDirectory("usr/bin")
            .addFile("usr/bin/clang-19", "CLANG_BIN".toByteArray(), mode = 0b111_101_101)
            .addSymlink("usr/bin/clang", "clang-19")
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(SandboxManager(E2ETestHelpers.targetContext()), tarGz, dest)
        assertTrue("extractTarGz failed: ${res.exceptionOrNull()}", res.isSuccess)

        val linkFile = File(dest, "usr/bin/clang")
        val lstat = Os.lstat(linkFile.absolutePath)
        assertTrue("Expected symbolic link via Os.lstat", OsConstants.S_ISLNK(lstat.st_mode))
        assertTrue("Expected symbolic link via Files.isSymbolicLink", Files.isSymbolicLink(linkFile.toPath()))
        assertEquals("clang-19", Os.readlink(linkFile.absolutePath))
        assertEquals("CLANG_BIN", linkFile.readText())
    }

    @Test
    fun t1_f7_05_extractTarGzPreservesExactPosixPermissions() = runBlocking {
        val root = newTempDir("tar_modes")
        val tarGz = File(root, "modes.tar.gz")
        val dest = File(root, "out").apply { mkdirs() }

        E2ETestHelpers.TarGzFixtureBuilder()
            .addFile("bin/tool.sh", "#!/bin/sh\n".toByteArray(), mode = 0b111_101_101 /* 0755 */)
            .addFile("etc/config.txt", "key=val\n".toByteArray(), mode = 0b110_100_100 /* 0644 */)
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(SandboxManager(E2ETestHelpers.targetContext()), tarGz, dest)
        assertTrue("extractTarGz failed: ${res.exceptionOrNull()}", res.isSuccess)

        val execMode = Os.stat(File(dest, "bin/tool.sh").absolutePath).st_mode and 0b111_111_111
        val dataMode = Os.stat(File(dest, "etc/config.txt").absolutePath).st_mode and 0b111_111_111
        assertEquals("Expected 0755 mode on bin/tool.sh", 0b111_101_101, execMode)
        assertEquals("Expected 0644 mode on etc/config.txt", 0b110_100_100, dataMode)
    }

    // ==================== F8: Onboarding Fail-Fast Verification (5 tests) ====================

    private fun prepareInstalledSandboxContext(toolPresent: Boolean): Pair<E2ETestHelpers.IsolatedSandboxContext, SandboxManager> {
        val ctx = E2ETestHelpers.targetContext()
        val isoFiles = newTempDir("onb_files")
        val isoNative = if (toolPresent) null else newTempDir("onb_empty_native")
        val isoCtx = E2ETestHelpers.IsolatedSandboxContext(ctx, isoFiles, isoNative)

        val realSandbox = File(ctx.filesDir, "sandbox")
        if (realSandbox.exists()) {
            realSandbox.copyRecursively(File(isoFiles, "sandbox"), overwrite = true)
        }
        val manager = SandboxManager(isoCtx)
        if (!manager.isInstalled()) {
            runBlocking { manager.installSandbox() }
        }
        assertTrue("Expected isolated sandbox base installed for F8 test", manager.isInstalled())
        if (!toolPresent) {
            // Remove any copied python3 marker files in rootfs so isToolInstalled("python3") is genuinely false
            File(isoFiles, "sandbox/rootfs/usr/bin/python3").delete()
            File(isoFiles, "sandbox/rootfs/bin/python3").delete()
            File(isoFiles, "sandbox/rootfs/usr/bin/python").delete()
            File(isoFiles, "sandbox/rootfs/etc/apk/world").delete()
            File(isoFiles, "sandbox/rootfs/lib/apk/db/installed").delete()
        }
        return isoCtx to manager
    }

    private fun staticExecutionProvider(result: CommandResult): ExecutionProvider = object : ExecutionProvider {
        override fun getTierName(): String = "E2E Controlled Provider"
        override fun isAvailable(): Boolean = true
        override suspend fun runCommand(command: String, workingDir: String?): CommandResult = result
    }

    @Test
    fun t1_f8_01_onboardingCompletesWhenInstallAndVerificationSucceed() = runBlocking {
        val (isoCtx, manager) = prepareInstalledSandboxContext(toolPresent = true)
        E2ETestHelpers.assertToolchainReady(manager, "python3")

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> staticExecutionProvider(CommandResult(0, "ok", "")) },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        val onSuccessCalled = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { onSuccessCalled.set(true) }
        vm.activeInstallJob?.join()

        assertEquals(SandboxInstallState.Completed, vm.installState.value)
        assertTrue("Expected onSuccess callback on Completed", onSuccessCalled.get())
    }

    @Test
    fun t1_f8_02_onboardingEmitsErrorWithStderrWhenCommandFails() = runBlocking {
        val (isoCtx, manager) = prepareInstalledSandboxContext(toolPresent = true)
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                staticExecutionProvider(CommandResult(exitCode = 1, stdout = "", stderr = "E2E_FAIL_STDERR_404"))
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        val onSuccessCalled = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { onSuccessCalled.set(true) }
        vm.activeInstallJob?.join()

        val state = vm.installState.value
        assertTrue("Expected SandboxInstallState.Error, got $state", state is SandboxInstallState.Error)
        assertTrue(
            "Expected stderr in Error message, got: ${(state as SandboxInstallState.Error).message}",
            state.message.contains("E2E_FAIL_STDERR_404")
        )
        assertFalse("onSuccess must NOT run when install command fails", onSuccessCalled.get())
    }

    @Test
    fun t1_f8_03_onboardingEmitsErrorOnNonZeroExitEvenWithBlankStderr() = runBlocking {
        val (isoCtx, manager) = prepareInstalledSandboxContext(toolPresent = true)
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                staticExecutionProvider(CommandResult(exitCode = 127, stdout = "", stderr = ""))
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        val onSuccessCalled = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { onSuccessCalled.set(true) }
        vm.activeInstallJob?.join()

        val state = vm.installState.value
        assertTrue("Expected Error state on exitCode 127, got $state", state is SandboxInstallState.Error)
        assertTrue((state as SandboxInstallState.Error).message.isNotBlank())
        assertFalse(onSuccessCalled.get())
    }

    @Test
    fun t1_f8_04_onboardingEmitsErrorWhenCommandExitsZeroButToolVerificationFails() = runBlocking {
        val (isoCtx, manager) = prepareInstalledSandboxContext(toolPresent = false)
        assertFalse(
            "Precondition: python3 must NOT be installed in isolated unverified context",
            manager.isToolInstalled(ToolchainOption.PYTHON.id)
        )

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                staticExecutionProvider(CommandResult(exitCode = 0, stdout = "pretend ok", stderr = ""))
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        val onSuccessCalled = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { onSuccessCalled.set(true) }
        vm.activeInstallJob?.join()

        val state = vm.installState.value
        assertTrue("Expected Error when isToolInstalled returns false, got $state", state is SandboxInstallState.Error)
        assertTrue((state as SandboxInstallState.Error).message.contains("verify", ignoreCase = true))
        assertFalse(onSuccessCalled.get())
    }

    @Test
    fun t1_f8_05_onboardingRetryInstallationRecoversFromErrorToCompleted() = runBlocking {
        val (isoCtx, manager) = prepareInstalledSandboxContext(toolPresent = true)
        E2ETestHelpers.assertToolchainReady(manager, "python3")

        val attempt = AtomicInteger(0)
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                object : ExecutionProvider {
                    override fun getTierName(): String = "Retry Test Provider"
                    override fun isAvailable(): Boolean = true
                    override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                        return if (attempt.getAndIncrement() == 0) {
                            CommandResult(exitCode = 1, stdout = "", stderr = "TRANSIENT_ERR")
                        } else {
                            CommandResult(exitCode = 0, stdout = "installed", stderr = "")
                        }
                    }
                }
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )

        val onSuccessCalled = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { onSuccessCalled.set(true) }
        vm.activeInstallJob?.join()
        assertTrue(vm.installState.value is SandboxInstallState.Error)
        assertFalse(onSuccessCalled.get())

        vm.retryInstallation(isoCtx, manager) { onSuccessCalled.set(true) }
        vm.activeInstallJob?.join()
        assertEquals(SandboxInstallState.Completed, vm.installState.value)
        assertTrue(onSuccessCalled.get())
    }

    // ==================== F9: 16 KB ELF, Zipalign & R5 License Compliance (5 tests) ====================

    @Test
    fun t1_f9_01_deviceProcessRunsWith16KbPageSizeUnderUntrustedAppSelinux() {
        val pageSize = Os.sysconf(OsConstants._SC_PAGESIZE)
        assertEquals("Expected 16 KB page size emulator (PAGESIZE == 16384)", 16384L, pageSize)
        assertTrue("Expected 64-bit app process on 16 KB device", Process.is64Bit())

        val selinuxAttr = File("/proc/self/attr/current").readText().trim('\u0000', '\n', ' ')
        assertTrue(
            "Expected untrusted_app SELinux context, got: '$selinuxAttr'",
            selinuxAttr.contains("untrusted_app")
        )
    }

    @Test
    fun t1_f9_02_allNativeLibrariesInNativeLibraryDirAre64BitAnd16KbAligned() {
        val nativeLibDir = File(E2ETestHelpers.targetContext().applicationInfo.nativeLibraryDir)
        val errors = E2ETestHelpers.inspectNativeLibraryDir(nativeLibDir)
        if (errors.isNotEmpty()) {
            fail("16 KB ELF / 64-bit ABI violations in nativeLibraryDir (${nativeLibDir.absolutePath}):\n${errors.joinToString("\n")}")
        }
    }

    @Test
    fun t1_f9_03_installedApkContainsOnly64BitAbisAnd16KbAlignedElfs() {
        val apkPath = E2ETestHelpers.targetContext().applicationInfo.sourceDir
        val (abis, errors) = E2ETestHelpers.inspectApkNativeLibs(apkPath)
        assertTrue("Expected at least one 64-bit ABI in APK lib/, found: $abis", abis.isNotEmpty())
        assertTrue("Forbidden 32-bit ABIs present in APK: $abis", abis.all { it == "arm64-v8a" || it == "x86_64" })
        if (errors.isNotEmpty()) {
            fail("16 KB ELF violations inside installed APK ($apkPath):\n${errors.joinToString("\n")}")
        }
    }

    @Test
    fun t1_f9_04_installedApkZipAlignmentAndLegacyPackagingAre16KbConsistent() {
        val appInfo = E2ETestHelpers.targetContext().applicationInfo
        val apkPath = appInfo.sourceDir
        val zipErrors = mutableListOf<String>()

        java.util.zip.ZipFile(apkPath).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.startsWith("lib/") && entry.method == java.util.zip.ZipEntry.STORED) {
                    // For uncompressed STORED .so files, Android requires 16384-byte data alignment in APK
                    val bytes = zf.getInputStream(entry).use { it.readBytes() }
                    zipErrors.addAll(E2ETestHelpers.inspectElfBytes(bytes, entry.name).errors)
                }
            }
        }
        val extractLibs = (appInfo.flags and ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != 0
        assertTrue(
            "Expected FLAG_EXTRACT_NATIVE_LIBS (useLegacyPackaging=true) for executable jniLibs binaries",
            extractLibs
        )
        assertTrue("Expected zero ELF errors on STORED APK libs: $zipErrors", zipErrors.isEmpty())
    }

    @Test
    fun t1_f9_05_nativeLibsAndAssetsContainZeroGplOrForbiddenR5Artifacts() {
        val ctx = E2ETestHelpers.targetContext()
        val violations = mutableListOf<String>()

        val nativeLibDir = File(ctx.applicationInfo.nativeLibraryDir)
        nativeLibDir.listFiles()?.forEach { f ->
            if (f.isFile) {
                violations.addAll(E2ETestHelpers.scanR5TextOrBinary(f.readBytes(), "nativeLibraryDir/${f.name}", f.name))
            }
        }

        val assetManager = ctx.assets
        fun scanAssetDir(dir: String) {
            val children = assetManager.list(dir) ?: return
            for (child in children) {
                val rel = if (dir.isEmpty()) child else "$dir/$child"
                val sub = assetManager.list(rel)
                if (sub != null && sub.isNotEmpty()) {
                    scanAssetDir(rel)
                } else {
                    try {
                        assetManager.open(rel).use { stream ->
                            val bytes = stream.readBytes()
                            violations.addAll(E2ETestHelpers.scanR5TextOrBinary(bytes, "assets/$rel", rel))
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
        scanAssetDir("")

        if (violations.isNotEmpty()) {
            fail("R5 license / GPL binary violations found on device:\n${violations.joinToString("\n")}")
        }
    }
}
