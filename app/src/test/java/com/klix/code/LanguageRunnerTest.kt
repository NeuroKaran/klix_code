package com.klix.code

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.system.OsConstants
import com.klix.code.data.model.EditorTab
import com.klix.code.data.sandbox.LanguageRunner
import com.klix.code.data.sandbox.PtySession
import com.klix.code.data.sandbox.PtyTransport
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.workspace.SandboxWorkspace
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Files

class LanguageRunnerTest {

    private lateinit var tempDir: File
    private lateinit var filesDir: File
    private lateinit var nativeLibDir: File
    private lateinit var mockContext: Context
    private lateinit var sandboxManager: SandboxManager
    private lateinit var workspaceDir: File
    private lateinit var workspace: SandboxWorkspace

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("klix_lang_runner_test_").toFile()
        filesDir = File(tempDir, "files").apply { mkdirs() }
        nativeLibDir = File(tempDir, "native").apply { mkdirs() }

        mockContext = mock(Context::class.java)
        val appInfo = ApplicationInfo().apply {
            nativeLibraryDir = this@LanguageRunnerTest.nativeLibDir.absolutePath
        }
        `when`(mockContext.filesDir).thenReturn(filesDir)
        `when`(mockContext.applicationInfo).thenReturn(appInfo)

        // Provision mock Bionic sandbox markers + native libs
        listOf("libdash.so", "libnode.so", "libpython3.so", "libclang.so", "liblld.so", "libklix_pty.so")
            .forEach { File(nativeLibDir, it).writeText("elf") }

        sandboxManager = SandboxManager(mockContext)
        val rootfs = sandboxManager.getRootfsDir()
        File(rootfs, "etc").mkdirs()
        File(rootfs, "etc/klix-bionic-release").writeText("klix-bionic-16kb=1\n")
        File(rootfs, "usr/lib/python3.12").mkdirs()
        File(rootfs, "usr/sysroot/usr/include").mkdirs()
        File(rootfs, "usr/sysroot/usr/include/stdio.h").writeText("// stdio")

        workspaceDir = File(rootfs, "workspaces/default").apply { mkdirs() }
        val wsUri = mock(Uri::class.java)
        `when`(wsUri.scheme).thenReturn("file")
        `when`(wsUri.path).thenReturn(workspaceDir.absolutePath)
        workspace = SandboxWorkspace(
            id = "default_workspace",
            name = "Default Workspace",
            directoryName = "default",
            relativePath = "/workspaces/default",
            rootUri = wsUri
        )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun mockTabForFile(file: File, name: String = file.name): EditorTab {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("file")
        `when`(uri.path).thenReturn(file.absolutePath)
        return EditorTab(uri = uri, name = name, content = "")
    }

    @Test
    fun testResolveRunCommandForJsPyCCppSh() {
        val js = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "app.js")), workspace, sandboxManager)
        val py = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "app.py")), workspace, sandboxManager)
        val c = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "main.c")), workspace, sandboxManager)
        val cpp = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "main.cpp")), workspace, sandboxManager)
        val cc = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "mod.cc")), workspace, sandboxManager)
        val cxx = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "mod.cxx")), workspace, sandboxManager)
        val sh = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "run.sh")), workspace, sandboxManager)

        assertFalse(js.requiresSetup)
        assertEquals("node 'app.js'", js.command)
        assertEquals("python3 -u 'app.py'", py.command)
        assertTrue(c.command!!.contains("clang -Wall -Wextra 'main.c' -o ") && c.command!!.contains(" -lm && "))
        assertTrue(cpp.command!!.contains("clang++ -std=c++17 -Wall -Wextra 'main.cpp' -o "))
        assertTrue(cc.command!!.contains("clang++ -std=c++17 -Wall -Wextra 'mod.cc' -o "))
        assertTrue(cxx.command!!.contains("clang++ -std=c++17 -Wall -Wextra 'mod.cxx' -o "))
        assertEquals("sh 'run.sh'", sh.command)
    }

    @Test
    fun testNestedPathsSpacesAndShellMetacharacterQuoting() {
        val tricky = File(workspaceDir, "sub dir/a'b \$HOME `id`;.py")
        val res = LanguageRunner.resolveRunCommand(mockTabForFile(tricky), workspace, sandboxManager)
        assertFalse(res.requiresSetup)
        assertEquals("python3 -u 'sub dir/a'\\''b \$HOME `id`;.py'", res.command)
    }

    @Test
    fun testPathTraversalRejectedWithoutSetup() {
        val outside = File(tempDir, "outside.py")
        val res = LanguageRunner.resolveRunCommand(mockTabForFile(outside), workspace, sandboxManager)
        assertNull(res.command)
        assertFalse(res.requiresSetup)
        assertFalse(res.errorMessage.isNullOrBlank())
    }

    @Test
    fun testUnsupportedExtensionRejectedWithoutSetup() {
        val md = File(workspaceDir, "README.md")
        val res = LanguageRunner.resolveRunCommand(mockTabForFile(md), workspace, sandboxManager)
        assertNull(res.command)
        assertFalse(res.requiresSetup)
        assertTrue(res.errorMessage!!.contains(".md"))
    }

    @Test
    fun testPreflightDetectsMissingSandboxAndMissingToolchain() {
        File(nativeLibDir, "libclang.so").delete()
        val cRes = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "main.c")), workspace, sandboxManager)
        assertNull(cRes.command)
        assertTrue(cRes.requiresSetup)
        assertEquals("c", cRes.missingToolId)

        sandboxManager.getRootfsDir().deleteRecursively()
        val jsRes = LanguageRunner.resolveRunCommand(mockTabForFile(File(workspaceDir, "app.js")), workspace, sandboxManager)
        assertNull(jsRes.command)
        assertTrue(jsRes.requiresSetup)
        assertEquals("nodejs", jsRes.missingToolId)
    }

    @Test
    fun testPtyTransportStreamingResizeWriteAndSignal() = runBlocking {
        val chunks = listOf(
            "Enter:".toByteArray(Charsets.UTF_8),
            "caf".toByteArray(Charsets.UTF_8) + byteArrayOf(0xC3.toByte()),
            byteArrayOf(0xA9.toByte()) + "\n__KLIX_PWD:/workspaces/default:STATUS:0\n".toByteArray(Charsets.UTF_8)
        )
        var chunkIdx = 0
        val writtenData = mutableListOf<ByteArray>()
        var resizedTo: Pair<Int, Int>? = null
        val signalsSent = mutableListOf<Int>()
        var closedFd = -1

        val fakeTransport = object : PtyTransport {
            override val isAvailable: Boolean = true
            override fun openAndForkPty(argv: Array<String>, envp: Array<String>, cwd: String, rows: Int, cols: Int) =
                PtySession(pid = 4242, masterFd = 99)

            override fun readPty(masterFd: Int, buffer: ByteArray): Int {
                if (chunkIdx >= chunks.size) return 0
                val data = chunks[chunkIdx++]
                System.arraycopy(data, 0, buffer, 0, data.size)
                return data.size
            }

            override fun writePty(masterFd: Int, data: ByteArray): Int {
                writtenData.add(data)
                return data.size
            }

            override fun resizePty(masterFd: Int, rows: Int, cols: Int): Int {
                resizedTo = rows to cols
                return 0
            }

            override fun sendSignal(pid: Int, signal: Int): Int {
                signalsSent.add(signal)
                return 0
            }

            override fun waitPid(pid: Int): Int = 0
            override fun closeFd(masterFd: Int) { closedFd = masterFd }
        }

        val provider = SandboxExecutionProvider(mockContext, sandboxManager, fakeTransport)
        val streamed = StringBuilder()
        val result = provider.runStreamingCommand("python3 -u app.py", "/workspaces/default") { text, _ ->
            streamed.append(text)
            if (text.contains("Enter:")) {
                provider.resizeTerminal(40, 120)
                provider.sendInput("test_in")
                provider.cancelActiveProcess()
            }
        }

        assertEquals(0, result.exitCode)
        assertTrue(streamed.toString().contains("Enter:"))
        assertTrue(streamed.toString().contains("café"))
        assertFalse(streamed.toString().contains("__KLIX_PWD"))
        assertEquals(40 to 120, resizedTo)
        assertTrue(writtenData.any { String(it, Charsets.UTF_8) == "test_in\n" })
        assertTrue(signalsSent.contains(OsConstants.SIGINT))
        assertEquals(99, closedFd)
    }
}

