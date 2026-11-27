package com.klix.code.e2e.helpers

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.SystemClock
import android.view.View
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.platform.app.InstrumentationRegistry
import com.klix.code.MainActivity
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.model.EditorTab
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.workspace.SandboxWorkspace
import com.klix.code.data.workspace.SandboxWorkspaceManagerImpl
import com.klix.code.e2e.contracts.ContractBridges
import io.github.rosemoe.sora.widget.CodeEditor
import org.hamcrest.Matcher
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.GZIPOutputStream

object E2ETestHelpers {

    fun targetContext(): Context = InstrumentationRegistry.getInstrumentation().targetContext

    fun targetApplication(): Application = targetContext().applicationContext as Application

    fun createTempTestDir(prefix: String): File {
        val base = File(targetContext().cacheDir, "e2e_tmp")
        if (!base.exists()) base.mkdirs()
        return File(base, "${prefix}_${UUID.randomUUID().toString().take(8)}").apply {
            mkdirs()
        }
    }

    /**
     * ContextWrapper with isolated filesDir and optional custom nativeLibraryDir
     * for non-destructive Onboarding / LanguageRunner / extractTarGz testing.
     */
    class IsolatedSandboxContext(
        base: Context,
        private val customFilesDir: File,
        private val customNativeLibDir: File? = null
    ) : ContextWrapper(base) {
        override fun getFilesDir(): File = customFilesDir

        override fun getApplicationContext(): Context = this

        override fun getApplicationInfo(): ApplicationInfo {
            val copy = ApplicationInfo(super.getApplicationInfo())
            if (customNativeLibDir != null) {
                copy.nativeLibraryDir = customNativeLibDir.absolutePath
            }
            return copy
        }
    }

    fun seedKlixPrefs(
        onboardingCompleted: Boolean,
        hasShownSandboxOnboarding: Boolean = true,
        clearAllFirst: Boolean = false
    ) {
        val prefs = targetContext().getSharedPreferences("klix_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (clearAllFirst) {
            editor.clear()
        }
        editor.putBoolean("onboarding_completed", onboardingCompleted)
        editor.putBoolean("has_shown_sandbox_onboarding", hasShownSandboxOnboarding)
        editor.commit()
    }

    fun launchMainActivity(
        onboardingCompleted: Boolean = true,
        hasShownSandboxOnboarding: Boolean = true,
        clearPrefsFirst: Boolean = false
    ): ActivityScenario<MainActivity> {
        seedKlixPrefs(
            onboardingCompleted = onboardingCompleted,
            hasShownSandboxOnboarding = hasShownSandboxOnboarding,
            clearAllFirst = clearPrefsFirst
        )
        return ActivityScenario.launch(MainActivity::class.java)
    }

    fun getDefaultWorkspaceDir(context: Context = targetContext()): File {
        val dir = File(context.filesDir, "sandbox/rootfs/workspaces/default")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun seedWorkspaceFile(
        relativePath: String,
        content: String,
        workspaceDir: File = getDefaultWorkspaceDir()
    ): File {
        val normalized = relativePath.trimStart('/')
        val target = File(workspaceDir, normalized)
        target.parentFile?.mkdirs()
        target.writeText(content, Charsets.UTF_8)
        return target
    }

    fun defaultWorkspaceObject(context: Context = targetContext()): SandboxWorkspace {
        val wsDir = getDefaultWorkspaceDir(context)
        return SandboxWorkspace(
            id = "default_workspace",
            name = "Default Workspace",
            directoryName = "default",
            relativePath = "/workspaces/default",
            rootUri = Uri.fromFile(wsDir)
        )
    }

    fun editorTabForFile(file: File): EditorTab {
        val text = if (file.exists()) file.readText(Charsets.UTF_8) else ""
        return EditorTab(
            uri = Uri.fromFile(file),
            name = file.name,
            content = text,
            originalContent = text,
            isModified = false
        )
    }

    /**
     * Resolves a file via LanguageRunner and executes it via SandboxExecutionProvider.
     */
    suspend fun resolveAndExecuteFile(
        file: File,
        workspace: SandboxWorkspace? = defaultWorkspaceObject(),
        sandboxManager: SandboxManager = SandboxManager(targetContext()),
        provider: SandboxExecutionProvider = SandboxExecutionProvider(targetContext(), sandboxManager)
    ): CommandResult {
        val tab = editorTabForFile(file)
        val resolution = ContractBridges.resolveRunCommand(tab, workspace, sandboxManager)
        val cmd = resolution.command
        if (cmd.isNullOrBlank()) {
            fail("LanguageRunner returned null/blank command for ${file.name}: error=${resolution.errorMessage}, missingTool=${resolution.missingToolId}")
        }
        return provider.runCommand(cmd!!, workspace?.relativePath ?: "/workspaces/default")
    }

    /**
     * Installs the bundled Bionic sandbox once if it is not yet extracted on the device.
     */
    fun ensureSandboxInstalled(context: Context = targetContext()): SandboxManager {
        val mgr = SandboxManager(context)
        if (!mgr.isInstalled()) {
            kotlinx.coroutines.runBlocking {
                mgr.installSandbox().getOrThrow()
            }
        }
        return mgr
    }

    /**
     * Ensures real Bionic runtime/compiler tools exist cleanly before running tool tests.
     * Fails loudly if sandbox or required tool is missing.
     */
    fun assertToolchainReady(
        sandboxManager: SandboxManager,
        vararg toolIds: String
    ) {
        assertTrue(
            "SandboxManager.isInstalled() must be true before executing toolchains",
            sandboxManager.isInstalled()
        )
        for (id in toolIds) {
            assertTrue(
                "Required toolchain '$id' is not installed (SandboxManager.isToolInstalled('$id') == false)",
                sandboxManager.isToolInstalled(id)
            )
        }
    }

    fun waitUntil(timeoutMs: Long = 10_000L, pollMs: Long = 100L, description: String = "condition", condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(pollMs)
        }
        fail("Timed out after ${timeoutMs}ms waiting for: $description")
    }

    fun waitForComposeText(
        rule: ComposeTestRule,
        text: String,
        substring: Boolean = true,
        timeoutMs: Long = 15_000L
    ) {
        rule.waitUntil(timeoutMillis = timeoutMs) {
            rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun dismissOptionalDialogsIfPresent(rule: ComposeTestRule) {
        val optionalLabels = listOf("Not now", "Later", "Dismiss")
        for (label in optionalLabels) {
            val nodes = rule.onAllNodesWithText(label, substring = false).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                rule.onAllNodesWithText(label, substring = false)[0].performClick()
                rule.waitForIdle()
            }
        }
    }

    fun openFileFromDrawer(rule: ComposeTestRule, fileName: String, subdirNames: List<String> = emptyList()) {
        rule.waitUntil(10_000L) {
            rule.onAllNodes(hasText("Open Drawer") or hasContentDescription("Open Drawer"))
                .fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodes(hasContentDescription("Menu"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodes(hasContentDescription("Open Drawer")).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithContentDescription("Open Drawer").performClick()
        } else {
            rule.onNodeWithContentDescription("Menu").performClick()
        }
        rule.waitForIdle()
        val refreshNodes = rule.onAllNodes(hasContentDescription("Refresh")).fetchSemanticsNodes()
        if (refreshNodes.isNotEmpty()) {
            rule.onNodeWithContentDescription("Refresh").performClick()
            SystemClock.sleep(300)
            rule.waitForIdle()
        }
        val scrollMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)
        fun scrollAndClickDrawerItem(label: String) {
            if (rule.onAllNodesWithText(label, substring = false).fetchSemanticsNodes().isEmpty()) {
                val scrollableCount = rule.onAllNodes(scrollMatcher).fetchSemanticsNodes().size
                for (idx in 0 until scrollableCount) {
                    try {
                        rule.onAllNodes(scrollMatcher)[idx]
                            .performScrollToNode(hasText(label, substring = false))
                        rule.waitForIdle()
                        break
                    } catch (_: Throwable) {
                    }
                }
            }
            waitForComposeText(rule, label, substring = false, timeoutMs = 10_000L)
            rule.onNodeWithText(label, substring = false).performClick()
            rule.waitForIdle()
        }
        for (dir in subdirNames) {
            scrollAndClickDrawerItem(dir)
        }
        scrollAndClickDrawerItem(fileName)
    }

    fun findTerminalInputNode(rule: ComposeTestRule): SemanticsNodeInteraction =
        rule.onNode(hasSetTextAction())

    fun submitTerminalCommand(rule: ComposeTestRule, command: String) {
        val input = findTerminalInputNode(rule)
        input.performTextReplacement(command)
        rule.waitForIdle()
        val executeNodes = rule.onAllNodes(hasContentDescription("Execute")).fetchSemanticsNodes()
        if (executeNodes.isNotEmpty()) {
            rule.onNodeWithContentDescription("Execute").performClick()
        } else {
            input.performImeAction()
        }
        rule.waitForIdle()
    }

    fun editSoraEditorText(newText: String) {
        onView(isAssignableFrom(CodeEditor::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(CodeEditor::class.java)
            override fun getDescription(): String = "Set text in Sora CodeEditor"
            override fun perform(uiController: UiController, view: View) {
                (view as CodeEditor).setText(newText)
                uiController.loopMainThreadUntilIdle()
            }
        })
    }

    /**
     * Pure-Kotlin POSIX TAR + GZIP builder for testing extractTarGz without external dependencies.
     * Supports standard files, directories, USTAR prefix (>100 chars), PAX extended headers (>255 chars),
     * GNU @LongLink ('L'), symlinks ('2'), forward hardlinks ('1'), and exact POSIX mode bits.
     */
    class TarGzFixtureBuilder {
        private val stream = ByteArrayOutputStream()

        fun addDirectory(name: String, mode: Int = 0b111_101_101 /* 0755 */): TarGzFixtureBuilder {
            val dirName = if (name.endsWith("/")) name else "$name/"
            writeEntryHeader(name = dirName, size = 0L, mode = mode, typeFlag = '5'.code.toByte())
            return this
        }

        fun addFile(name: String, content: ByteArray, mode: Int = 0b110_100_100 /* 0644 */): TarGzFixtureBuilder {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            if (nameBytes.size <= 100) {
                writeEntryHeader(name = name, size = content.size.toLong(), mode = mode, typeFlag = '0'.code.toByte())
            } else {
                // Use USTAR prefix if possible (<= 155 prefix + <= 100 leaf)
                val split = splitUstarPath(name)
                if (split != null) {
                    writeEntryHeader(
                        name = split.second,
                        prefix = split.first,
                        size = content.size.toLong(),
                        mode = mode,
                        typeFlag = '0'.code.toByte()
                    )
                } else {
                    addPaxLongPathFile(name, content, mode)
                    return this
                }
            }
            writeDataAndPad(content)
            return this
        }

        fun addUstarPrefixFile(prefix: String, leaf: String, content: ByteArray, mode: Int = 0b110_100_100): TarGzFixtureBuilder {
            writeEntryHeader(
                name = leaf,
                prefix = prefix,
                size = content.size.toLong(),
                mode = mode,
                typeFlag = '0'.code.toByte()
            )
            writeDataAndPad(content)
            return this
        }

        fun addPaxLongPathFile(fullPath: String, content: ByteArray, mode: Int = 0b110_100_100): TarGzFixtureBuilder {
            val recordPayload = " path=$fullPath\n".toByteArray(Charsets.UTF_8)
            var totalLen = recordPayload.size + 1
            while (true) {
                val candidate = totalLen.toString().length + recordPayload.size
                if (candidate == totalLen) break
                totalLen = candidate
            }
            val paxData = (totalLen.toString().toByteArray(Charsets.UTF_8)) + recordPayload
            writeEntryHeader(
                name = "PaxHeader/entry",
                size = paxData.size.toLong(),
                mode = 0b110_100_100,
                typeFlag = 'x'.code.toByte()
            )
            writeDataAndPad(paxData)

            val fallbackName = fullPath.takeLast(90)
            writeEntryHeader(
                name = fallbackName,
                size = content.size.toLong(),
                mode = mode,
                typeFlag = '0'.code.toByte()
            )
            writeDataAndPad(content)
            return this
        }

        fun addGnuLongPathFile(fullPath: String, content: ByteArray, mode: Int = 0b110_100_100): TarGzFixtureBuilder {
            val pathBytes = fullPath.toByteArray(Charsets.UTF_8) + byteArrayOf(0)
            writeEntryHeader(
                name = "././@LongLink",
                size = pathBytes.size.toLong(),
                mode = 0b110_100_100,
                typeFlag = 'L'.code.toByte()
            )
            writeDataAndPad(pathBytes)

            writeEntryHeader(
                name = fullPath.take(99),
                size = content.size.toLong(),
                mode = mode,
                typeFlag = '0'.code.toByte()
            )
            writeDataAndPad(content)
            return this
        }

        fun addSymlink(linkName: String, target: String, mode: Int = 0b111_111_111 /* 0777 */): TarGzFixtureBuilder {
            writeEntryHeader(
                name = linkName,
                size = 0L,
                mode = mode,
                typeFlag = '2'.code.toByte(),
                linkName = target
            )
            return this
        }

        fun addHardlink(linkName: String, target: String, mode: Int = 0b110_100_100): TarGzFixtureBuilder {
            writeEntryHeader(
                name = linkName,
                size = 0L,
                mode = mode,
                typeFlag = '1'.code.toByte(),
                linkName = target
            )
            return this
        }

        fun writeTo(outFile: File): File {
            outFile.parentFile?.mkdirs()
            // Two 512-byte zero blocks terminate a POSIX TAR stream
            val rawTar = stream.toByteArray() + ByteArray(1024)
            GZIPOutputStream(FileOutputStream(outFile)).use { gzos ->
                gzos.write(rawTar)
            }
            return outFile
        }

        private fun splitUstarPath(path: String): Pair<String, String>? {
            var idx = path.indexOf('/')
            while (idx != -1) {
                val prefix = path.substring(0, idx)
                val leaf = path.substring(idx + 1)
                if (prefix.toByteArray(Charsets.UTF_8).size <= 155 &&
                    leaf.toByteArray(Charsets.UTF_8).size <= 100 &&
                    leaf.isNotEmpty()
                ) {
                    return prefix to leaf
                }
                idx = path.indexOf('/', idx + 1)
            }
            return null
        }

        private fun writeDataAndPad(data: ByteArray) {
            stream.write(data)
            val rem = data.size % 512
            if (rem != 0) {
                stream.write(ByteArray(512 - rem))
            }
        }

        private fun writeEntryHeader(
            name: String,
            size: Long,
            mode: Int,
            typeFlag: Byte,
            linkName: String = "",
            prefix: String = ""
        ) {
            val header = ByteArray(512)
            putAscii(header, 0, 100, name)
            putOctal(header, 100, 8, mode.toLong())
            putOctal(header, 108, 8, 0L) // uid
            putOctal(header, 116, 8, 0L) // gid
            putOctal(header, 124, 12, size)
            putOctal(header, 136, 12, 1_700_000_000L) // mtime
            // Fill checksum field with spaces prior to checksum computation
            for (i in 148 until 156) header[i] = ' '.code.toByte()
            header[156] = typeFlag
            if (linkName.isNotEmpty()) {
                putAscii(header, 157, 100, linkName)
            }
            putAscii(header, 257, 6, "ustar") // "ustar\0"
            header[263] = '0'.code.toByte()
            header[264] = '0'.code.toByte()
            if (prefix.isNotEmpty()) {
                putAscii(header, 345, 155, prefix)
            }

            var sum = 0
            for (b in header) {
                sum += (b.toInt() and 0xFF)
            }
            val chkStr = sum.toString(8).padStart(6, '0')
            putAscii(header, 148, 6, chkStr)
            header[154] = 0
            header[155] = ' '.code.toByte()

            stream.write(header)
        }

        private fun putAscii(buf: ByteArray, offset: Int, len: Int, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            val count = minOf(bytes.size, len)
            System.arraycopy(bytes, 0, buf, offset, count)
        }

        private fun putOctal(buf: ByteArray, offset: Int, len: Int, value: Long) {
            val digits = len - 1
            val oct = value.toString(8).padStart(digits, '0')
            val trimmed = if (oct.length > digits) oct.takeLast(digits) else oct
            putAscii(buf, offset, digits, trimmed)
            buf[offset + digits] = 0
        }
    }

    // ==================== Device-Side 16 KB ELF & R5 Compliance Inspectors ====================

    data class ElfInspectionResult(
        val location: String,
        val eiClass: Int,
        val ptLoadAligns: List<Long>,
        val errors: List<String>
    ) {
        val isOk: Boolean get() = errors.isEmpty()
    }

    private const val ELF_MIN_PAGE_ALIGN = 0x4000L
    private const val PT_LOAD_TYPE = 1
    private val ALLOWED_64_ABIS = setOf("arm64-v8a", "x86_64")

    fun isElfMagic(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x7F.toByte() &&
            bytes[1] == 'E'.code.toByte() &&
            bytes[2] == 'L'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()

    fun inspectElfBytes(data: ByteArray, location: String): ElfInspectionResult {
        val errors = mutableListOf<String>()
        if (data.size < 52 || !isElfMagic(data)) {
            return ElfInspectionResult(location, 0, emptyList(), listOf("$location: Not a valid ELF binary"))
        }
        val eiClass = data[4].toInt() and 0xFF
        val eiData = data[5].toInt() and 0xFF
        val littleEndian = when (eiData) {
            1 -> true
            2 -> false
            else -> return ElfInspectionResult(location, eiClass, emptyList(), listOf("$location: Unknown ELF endianness=$eiData"))
        }
        val buf = java.nio.ByteBuffer.wrap(data).order(
            if (littleEndian) java.nio.ByteOrder.LITTLE_ENDIAN else java.nio.ByteOrder.BIG_ENDIAN
        )

        val eType = buf.getShort(16).toInt() and 0xFFFF
        val ePhOff: Long
        val ePhEntSize: Int
        val ePhNum: Int
        val pAlignOffset: Int
        val minPhEntSize: Int
        val is64: Boolean

        when (eiClass) {
            1 -> {
                is64 = false
                errors.add("$location: 32-bit ELF (ELFCLASS32) is forbidden; only 64-bit arm64-v8a/x86_64 allowed")
                ePhOff = buf.getInt(28).toLong() and 0xFFFFFFFFL
                ePhEntSize = buf.getShort(42).toInt() and 0xFFFF
                ePhNum = buf.getShort(44).toInt() and 0xFFFF
                pAlignOffset = 28
                minPhEntSize = 32
            }
            2 -> {
                is64 = true
                if (data.size < 64) {
                    return ElfInspectionResult(location, eiClass, emptyList(), listOf("$location: Truncated ELF64 header"))
                }
                ePhOff = buf.getLong(32)
                ePhEntSize = buf.getShort(54).toInt() and 0xFFFF
                ePhNum = buf.getShort(56).toInt() and 0xFFFF
                pAlignOffset = 48
                minPhEntSize = 56
            }
            else -> return ElfInspectionResult(location, eiClass, emptyList(), listOf("$location: Unknown ELF class=$eiClass"))
        }

        // Relocatable objects (ET_REL == 1, e.g., crt*.o) have no runtime PT_LOAD program headers.
        if (eType == 1 && ePhNum == 0) {
            return ElfInspectionResult(location, eiClass, emptyList(), errors)
        }

        if (ePhNum == 0 || ePhEntSize < minPhEntSize) {
            errors.add("$location: Invalid program header table (phnum=$ePhNum, phentsize=$ePhEntSize)")
            return ElfInspectionResult(location, eiClass, emptyList(), errors)
        }

        val aligns = mutableListOf<Long>()
        for (i in 0 until ePhNum) {
            val off = (ePhOff + i.toLong() * ePhEntSize).toInt()
            if (off < 0 || off + ePhEntSize > data.size) {
                errors.add("$location: Truncated program header $i")
                break
            }
            val pType = buf.getInt(off)
            if (pType == PT_LOAD_TYPE) {
                val pAlign = if (is64) {
                    buf.getLong(off + pAlignOffset)
                } else {
                    buf.getInt(off + pAlignOffset).toLong() and 0xFFFFFFFFL
                }
                aligns.add(pAlign)
                if (pAlign < ELF_MIN_PAGE_ALIGN) {
                    errors.add("$location: PT_LOAD[${aligns.size - 1}] p_align=0x${pAlign.toString(16)} < 0x4000")
                }
            }
        }
        if (aligns.isEmpty()) {
            errors.add("$location: No PT_LOAD segments found")
        }
        return ElfInspectionResult(location, eiClass, aligns, errors)
    }

    fun buildSyntheticElf64(pAlign: Long): ByteArray {
        val data = ByteArray(64 + 56)
        val buf = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        data[0] = 0x7F; data[1] = 'E'.code.toByte(); data[2] = 'L'.code.toByte(); data[3] = 'F'.code.toByte()
        data[4] = 2 // ELFCLASS64
        data[5] = 1 // ELFDATA2LSB
        data[6] = 1 // EV_CURRENT
        buf.putShort(16, 3) // ET_DYN
        buf.putShort(18, 183) // EM_AARCH64
        buf.putInt(20, 1)
        buf.putLong(32, 64L) // e_phoff
        buf.putShort(52, 64)
        buf.putShort(54, 56)
        buf.putShort(56, 1)
        // PT_LOAD
        buf.putInt(64, PT_LOAD_TYPE)
        buf.putInt(68, 5)
        buf.putLong(72, 0L)
        buf.putLong(80, 0L)
        buf.putLong(88, 0L)
        buf.putLong(96, 120L)
        buf.putLong(104, 120L)
        buf.putLong(112, pAlign)
        return data
    }

    fun buildSyntheticElf32(pAlign: Int): ByteArray {
        val data = ByteArray(52 + 32)
        val buf = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        data[0] = 0x7F; data[1] = 'E'.code.toByte(); data[2] = 'L'.code.toByte(); data[3] = 'F'.code.toByte()
        data[4] = 1 // ELFCLASS32
        data[5] = 1 // ELFDATA2LSB
        data[6] = 1
        buf.putShort(16, 3)
        buf.putShort(18, 40)
        buf.putInt(20, 1)
        buf.putInt(28, 52) // e_phoff
        buf.putShort(40, 52)
        buf.putShort(42, 32)
        buf.putShort(44, 1)
        buf.putInt(52, PT_LOAD_TYPE)
        buf.putInt(56, 0)
        buf.putInt(60, 0)
        buf.putInt(64, 0)
        buf.putInt(68, 84)
        buf.putInt(72, 84)
        buf.putInt(76, 5)
        buf.putInt(80, pAlign)
        return data
    }

    private fun readHeaderPrefix(stream: java.io.InputStream, maxBytes: Int = 65536): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (total < maxBytes) {
            val n = stream.read(buf, 0, minOf(buf.size, maxBytes - total))
            if (n <= 0) break
            out.write(buf, 0, n)
            total += n
        }
        return out.toByteArray()
    }

    fun inspectNativeLibraryDir(nativeLibDir: File): List<String> {
        val errors = mutableListOf<String>()
        if (!nativeLibDir.exists()) {
            return listOf("nativeLibraryDir does not exist: ${nativeLibDir.absolutePath}")
        }
        val files = nativeLibDir.listFiles()?.filter { it.isFile } ?: emptyList()
        if (files.isEmpty()) {
            return listOf("nativeLibraryDir contains no native libraries: ${nativeLibDir.absolutePath}")
        }
        for (file in files.sortedBy { it.name }) {
            val bytes = file.inputStream().use { readHeaderPrefix(it) }
            if (file.extension == "so" || isElfMagic(bytes)) {
                errors.addAll(inspectElfBytes(bytes, file.name).errors)
            }
        }
        return errors
    }

    fun inspectApkNativeLibs(apkPath: String): Pair<Set<String>, List<String>> {
        val errors = mutableListOf<String>()
        val abis = mutableSetOf<String>()
        java.util.zip.ZipFile(apkPath).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                if (entry.name.startsWith("lib/")) {
                    val parts = entry.name.split("/")
                    if (parts.size >= 3) {
                        val abi = parts[1]
                        abis.add(abi)
                        if (abi !in ALLOWED_64_ABIS) {
                            errors.add("${entry.name}: Forbidden ABI '$abi' in APK")
                        }
                    }
                    val bytes = zf.getInputStream(entry).use { readHeaderPrefix(it) }
                    errors.addAll(inspectElfBytes(bytes, entry.name).errors)
                }
            }
        }
        return abis to errors
    }

    private val R5_FORBIDDEN_NAMES = listOf(
        "libproot.so", "proot", "libloader.so", "libloader-m32.so",
        "libtalloc", "libgdbm", "libreadline", "libbash.so"
    )

    private val R5_FORBIDDEN_REGEXES = listOf(
        "GPL v2/v3 notice" to Regex("""\bgpl\s*v?[23]\b""", RegexOption.IGNORE_CASE),
        "GPL-2.0/GPL-3.0 id" to Regex("""(?<!l)\bgpl-[23](?:\.0)?\b""", RegexOption.IGNORE_CASE),
        "GNU General Public License (non-LGPL)" to Regex("""gnu\s+general\s+public\s+license""", RegexOption.IGNORE_CASE),
        "AGPL license" to Regex("""\bagpl(?:-[23](?:\.0)?)?\b|affero\s+general\s+public\s+license""", RegexOption.IGNORE_CASE),
        "Non-commercial restriction" to Regex("""\bnon-?commercial\b""", RegexOption.IGNORE_CASE),
        "Paid/trial SDK" to Regex("""\b(?:commercial\s+license\s+required|paid\s+sdk|trial[\s-]only)\b""", RegexOption.IGNORE_CASE),
        "proot reference" to Regex("""\bproot\b""", RegexOption.IGNORE_CASE),
        "talloc reference" to Regex("""\btalloc(?:\.c)?\b""", RegexOption.IGNORE_CASE),
        "gdbm reference" to Regex("""\bgdbm\b""", RegexOption.IGNORE_CASE),
        "readline reference" to Regex("""\blibreadline\b|\bgnu\s+readline\b(?! library - keyseq-timeout)""", RegexOption.IGNORE_CASE)
    )

    fun scanR5TextOrBinary(data: ByteArray, location: String, fileName: String = location): List<String> {
        val violations = mutableListOf<String>()
        val base = File(fileName).name.lowercase()
        for (banned in R5_FORBIDDEN_NAMES) {
            if (base == banned || base.startsWith("$banned.") || base.contains(banned)) {
                violations.add("$location: Forbidden binary name '$base' (matches '$banned')")
                break
            }
        }
        val asciiSb = StringBuilder()
        val run = StringBuilder()
        for (b in data) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) {
                run.append(c.toChar())
            } else {
                if (run.length >= 4) {
                    asciiSb.append(run).append('\n')
                }
                run.setLength(0)
            }
        }
        if (run.length >= 4) asciiSb.append(run).append('\n')
        var asciiText = asciiSb.toString()
        if (location.contains("usr/lib/python3.") && location.lowercase().endsWith(".py")) {
            asciiText = asciiText
                .replace(Regex("""'_?gdbm'\s*:\s*'(?:dbm\.gnu|gdbm)'"""), "[PSF-2.0-stdlib-pickle-map]")
                .replace(Regex("""GNU\s+readline\s+is\s+never\s+used"""), "[PSF-2.0-stdlib-comment]")
                .replace("in the GNU readline history", "[PSF-2.0-stdlib-reference]")
                .replace("enable GNU readline if it is available", "[PSF-2.0-stdlib-reference]")
                .replace("Word completion for GNU readline", "[PSF-2.0-stdlib-reference]")
                .replace(Regex("""1\)\s*GNU\s+GPLv2\s+2\)\s*PSF\s+license\s+for\s+Python"""), "[PSF-2.0-stdlib-license]")
        }
        for ((label, regex) in R5_FORBIDDEN_REGEXES) {
            val match = regex.find(asciiText)
            if (match != null) {
                violations.add("$location: $label (${match.value})")
            }
        }
        return violations
    }
}
