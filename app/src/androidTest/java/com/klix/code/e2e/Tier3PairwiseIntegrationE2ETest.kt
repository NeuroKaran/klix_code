package com.klix.code.e2e

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.e2e.contracts.ContractBridges
import com.klix.code.e2e.helpers.E2ETestHelpers
import com.klix.code.ui.onboarding.OnboardingViewModel
import com.klix.code.ui.onboarding.SandboxInstallState
import com.klix.code.ui.terminal.TerminalViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tier 3 Cross-Feature Pairwise Integration Tests (12 tests: T3-01..T3-12).
 */
@RunWith(AndroidJUnit4::class)
class Tier3PairwiseIntegrationE2ETest {

    private lateinit var sandboxManager: SandboxManager
    private lateinit var provider: SandboxExecutionProvider
    private lateinit var testSubdir: File
    private lateinit var tempDirs: MutableList<File>

    @Before
    fun setUp() {
        val ctx = E2ETestHelpers.targetContext()
        sandboxManager = E2ETestHelpers.ensureSandboxInstalled(ctx)
        provider = SandboxExecutionProvider(ctx, sandboxManager)
        val wsDir = E2ETestHelpers.getDefaultWorkspaceDir(ctx)
        testSubdir = File(wsDir, "t3_pair_${System.nanoTime()}").apply { mkdirs() }
        tempDirs = mutableListOf()
    }

    @After
    fun tearDown() {
        testSubdir.deleteRecursively()
        tempDirs.forEach { it.deleteRecursively() }
    }

    private fun newTempDir(prefix: String): File =
        E2ETestHelpers.createTempTestDir(prefix).also { tempDirs.add(it) }

    @Test
    fun t3_01_f1xf5_languageRunnerExecutesJsInNestedSubdirWithSpaces() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val file = E2ETestHelpers.seedWorkspaceFile(
            "nested dir/app with space.js",
            """console.log("T3_JS_SPACE_OK:" + (21 * 2));""",
            testSubdir
        )
        val res = E2ETestHelpers.resolveAndExecuteFile(file, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue(res.combinedOutput.contains("T3_JS_SPACE_OK:42"))
    }

    @Test
    fun t3_02_f2xf5_languageRunnerExecutesPyUnbufferedInNestedSubdirWithSpaces() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val file = E2ETestHelpers.seedWorkspaceFile(
            "nested dir/app with space.py",
            """
            import time
            print("T3_PY_STREAM_1")
            time.sleep(1.5)
            print("T3_PY_STREAM_2")
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(file),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val t0 = SystemClock.uptimeMillis()
        var firstElapsed = Long.MAX_VALUE
        val res = provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
            if (chunk.contains("T3_PY_STREAM_1") && firstElapsed == Long.MAX_VALUE) {
                firstElapsed = SystemClock.uptimeMillis() - t0
            }
        }
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue("Expected unbuffered first chunk < 1200ms, got ${firstElapsed}ms", firstElapsed < 1200L)
        assertTrue(res.combinedOutput.contains("T3_PY_STREAM_2"))
    }

    @Test
    fun t3_03_f3xf5_languageRunnerCompilesAndRunsCInNestedSubdirWithSpaces() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val file = E2ETestHelpers.seedWorkspaceFile(
            "nested dir/calc app.c",
            """
            #include <stdio.h>
            #include <math.h>
            int main(void) {
                printf("T3_C_SPACE=%.0f\n", sqrt(144.0));
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val res = E2ETestHelpers.resolveAndExecuteFile(file, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue(res.combinedOutput.contains("T3_C_SPACE=12"))
    }

    @Test
    fun t3_04_f4xf5_languageRunnerCompilesAndRunsCpp17InNestedSubdirWithSpaces() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val file = E2ETestHelpers.seedWorkspaceFile(
            "nested dir/calc app.cpp",
            """
            #include <iostream>
            #include <optional>
            int main() {
                std::optional<int> v = 88;
                std::cout << "T3_CPP17=" << v.value() << "\n";
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val res = E2ETestHelpers.resolveAndExecuteFile(file, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue(res.combinedOutput.contains("T3_CPP17=88"))
    }

    @Test
    fun t3_05_f2xf6_pythonInteractiveInputOverNativePty() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val file = E2ETestHelpers.seedWorkspaceFile(
            "greet.py",
            """
            name = input("Name: ")
            print(f"Hello, {name}")
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(file),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val promptArrived = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<CommandResult>()
        val job = launch(Dispatchers.IO) {
            val res = provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("Name:") && !promptArrived.isCompleted) promptArrived.complete(Unit)
            }
            resultDeferred.complete(res)
        }
        withTimeout(8000L) { promptArrived.await() }
        provider.sendInput("Klix")
        val res = withTimeout(5000L) { resultDeferred.await() }
        job.join()
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue("Expected 'Hello, Klix', got: ${res.combinedOutput}", res.combinedOutput.contains("Hello, Klix"))
    }

    @Test
    fun t3_06_f3xf6_cUnflushedPrintfScanfOverNativePty() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val file = E2ETestHelpers.seedWorkspaceFile(
            "scan_num.c",
            """
            #include <stdio.h>
            int main(void) {
                int n = 0;
                printf("N? ");
                if (scanf("%d", &n) != 1) return 2;
                printf("N=%d\n", n);
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(file),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val promptArrived = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<CommandResult>()
        val job = launch(Dispatchers.IO) {
            val res = provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("N?") && !promptArrived.isCompleted) promptArrived.complete(Unit)
            }
            resultDeferred.complete(res)
        }
        withTimeout(15_000L) { promptArrived.await() }
        provider.sendInput("7")
        val res = withTimeout(5000L) { resultDeferred.await() }
        job.join()
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue("Expected 'N=7', got: ${res.combinedOutput}", res.combinedOutput.contains("N=7"))
    }

    @Test
    fun t3_07_f4xf6_cppUnflushedCoutCinOverNativePty() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val file = E2ETestHelpers.seedWorkspaceFile(
            "cin_val.cpp",
            """
            #include <iostream>
            int main() {
                int v = 0;
                std::cout << "Val? ";
                std::cin >> v;
                std::cout << "Val=" << v << "\n";
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(file),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val promptArrived = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<CommandResult>()
        val job = launch(Dispatchers.IO) {
            val res = provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("Val?") && !promptArrived.isCompleted) promptArrived.complete(Unit)
            }
            resultDeferred.complete(res)
        }
        withTimeout(15_000L) { promptArrived.await() }
        provider.sendInput("99")
        val res = withTimeout(5000L) { resultDeferred.await() }
        job.join()
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue("Expected 'Val=99', got: ${res.combinedOutput}", res.combinedOutput.contains("Val=99"))
    }

    @Test
    fun t3_08_f5xf8_missingToolchainPreflightTriggersSetupThenSucceedsAfterProvisioning() = runBlocking {
        val ctx = E2ETestHelpers.targetContext()
        val isoFiles = newTempDir("t3_preflight_files")
        val isoEmptyNative = newTempDir("t3_empty_native")
        val isoCtx = E2ETestHelpers.IsolatedSandboxContext(ctx, isoFiles, isoEmptyNative)
        val missingMgr = SandboxManager(isoCtx)
        val ws = E2ETestHelpers.defaultWorkspaceObject(isoCtx)
        val script = E2ETestHelpers.seedWorkspaceFile("app.py", """print("RECOVERED_T3_08")""", E2ETestHelpers.getDefaultWorkspaceDir(isoCtx))

        val preRes = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(script), ws, missingMgr)
        assertTrue("Pre-flight must flag requiresSetup=true when missing", preRes.requiresSetup)

        // Provision into real isolated context with real nativeLibraryDir
        val readyCtx = E2ETestHelpers.IsolatedSandboxContext(ctx, isoFiles, null)
        val readyMgr = SandboxManager(readyCtx)
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                object : ExecutionProvider {
                    override fun getTierName() = "T3"
                    override fun isAvailable() = true
                    override suspend fun runCommand(command: String, workingDir: String?) = CommandResult(0, "ok", "")
                }
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        vm.installSelectedPackages(readyCtx, readyMgr) {}
        vm.activeInstallJob?.join()
        assertEquals(SandboxInstallState.Completed, vm.installState.value)

        val provisionedScript = E2ETestHelpers.seedWorkspaceFile(
            "app.py",
            """print("RECOVERED_T3_08")""",
            E2ETestHelpers.getDefaultWorkspaceDir(readyCtx)
        )
        val postRes = E2ETestHelpers.resolveAndExecuteFile(
            provisionedScript,
            E2ETestHelpers.defaultWorkspaceObject(readyCtx),
            readyMgr,
            SandboxExecutionProvider(readyCtx, readyMgr)
        )
        assertEquals(0, postRes.exitCode)
        assertTrue(postRes.combinedOutput.contains("RECOVERED_T3_08"))
    }

    @Test
    fun t3_09_f6xf8_terminalViewModelSigintRecoversPromptForNextCommand() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val app = E2ETestHelpers.targetApplication()
        val termVm = TerminalViewModel(app)
        val script = E2ETestHelpers.seedWorkspaceFile(
            "endless.py",
            """
            import time
            print("INFINITE_STARTED")
            while True:
                time.sleep(0.2)
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(script),
            E2ETestHelpers.defaultWorkspaceObject(app),
            sandboxManager
        ).command!!

        termVm.executeCommand(cmd)
        E2ETestHelpers.waitUntil(8000L, description = "INFINITE_STARTED in terminal") {
            termVm.terminalLines.value.any { it.text.contains("INFINITE_STARTED") }
        }
        termVm.cancelExecution()
        E2ETestHelpers.waitUntil(3000L, description = "terminal idle after Ctrl+C") {
            !termVm.isExecuting.value
        }

        termVm.executeCommand("echo AFTER_CTRL_C_RECOVERED")
        E2ETestHelpers.waitUntil(5000L, description = "AFTER_CTRL_C_RECOVERED printed") {
            termVm.terminalLines.value.any { it.text.contains("AFTER_CTRL_C_RECOVERED") }
        }
    }

    @Test
    fun t3_10_f7xf8_extractTarGzProvisionsSymlinksAndModesForSandbox() = runBlocking {
        val root = newTempDir("t3_tar_prov")
        val tarGz = File(root, "sysroot.tar.gz")
        val dest = File(root, "sandbox/rootfs").apply { mkdirs() }
        E2ETestHelpers.TarGzFixtureBuilder()
            .addDirectory("bin", mode = 0b111_101_101)
            .addFile("bin/busybox", "BIN_PAYLOAD".toByteArray(), mode = 0b111_101_101)
            .addSymlink("bin/sh", "busybox")
            .addDirectory("etc", mode = 0b111_101_101)
            .addFile("etc/alpine-release", "bionic-16kb\n".toByteArray(), mode = 0b110_100_100)
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(sandboxManager, tarGz, dest)
        assertTrue("extractTarGz failed: ${res.exceptionOrNull()}", res.isSuccess)
        val isoCtx = E2ETestHelpers.IsolatedSandboxContext(E2ETestHelpers.targetContext(), root)
        val isoMgr = SandboxManager(isoCtx)
        assertTrue("SandboxManager.isInstalled() should recognize extracted rootfs", isoMgr.isInstalled())
    }

    @Test
    fun t3_11_f3xf9_compiledCBinaryReports16384PageSizeOnEmulator() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "pagesize_check.c",
            """
            #include <stdio.h>
            #include <unistd.h>
            int main(void) {
                long ps = sysconf(_SC_PAGESIZE);
                printf("C_PAGESIZE=%ld\n", ps);
                return (ps == 16384L) ? 0 : 1;
            }
            """.trimIndent(),
            testSubdir
        )
        val res = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("Expected C binary to confirm PAGESIZE == 16384. out=${res.combinedOutput}", 0, res.exitCode)
        assertTrue(res.combinedOutput.contains("C_PAGESIZE=16384"))
    }

    @Test
    fun t3_12_f7xf9_extractedRuntimeFilesAndExecutablePathsPass16KbAndR5() {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs", "python3", "c", "cpp")
        val tools = listOf("dash", "nodejs", "python3", "clang", "lld")
        val elfErrors = mutableListOf<String>()
        val r5Violations = mutableListOf<String>()

        for (tool in tools) {
            val path = ContractBridges.getExecutablePath(sandboxManager, tool)
            assertNotNull("Missing executable path for '$tool'", path)
            val f = File(path!!)
            assertTrue("Executable for '$tool' does not exist at $path", f.exists())
            val bytes = f.readBytes()
            elfErrors.addAll(E2ETestHelpers.inspectElfBytes(bytes, "$tool($path)").errors)
            r5Violations.addAll(E2ETestHelpers.scanR5TextOrBinary(bytes, "$tool($path)", f.name))
        }
        if (elfErrors.isNotEmpty()) fail("16 KB ELF errors on toolchain executables:\n${elfErrors.joinToString("\n")}")
        if (r5Violations.isNotEmpty()) fail("R5 license violations on toolchain executables:\n${r5Violations.joinToString("\n")}")
    }
}
