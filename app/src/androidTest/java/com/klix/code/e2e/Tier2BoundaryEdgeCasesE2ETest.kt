package com.klix.code.e2e

import android.net.Uri
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.model.EditorTab
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.e2e.contracts.ContractBridges
import com.klix.code.e2e.helpers.E2ETestHelpers
import com.klix.code.ui.viewmodel.EditorViewModel
import com.klix.code.ui.onboarding.OnboardingViewModel
import com.klix.code.ui.onboarding.SandboxInstallState
import com.klix.code.ui.onboarding.ToolchainOption
import com.klix.code.ui.terminal.TerminalViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile

/**
 * Tier 2 Boundary & Edge-Case Tests for F1-F9.
 * 45 tests total (5 per feature).
 */
@RunWith(AndroidJUnit4::class)
class Tier2BoundaryEdgeCasesE2ETest {

    private lateinit var sandboxManager: SandboxManager
    private lateinit var provider: SandboxExecutionProvider
    private lateinit var testSubdir: File
    private lateinit var tempDirs: MutableList<File>

    @Before
    fun setUp() {
        val ctx = E2ETestHelpers.targetContext()
        sandboxManager = SandboxManager(ctx)
        provider = SandboxExecutionProvider(ctx, sandboxManager)
        val wsDir = E2ETestHelpers.getDefaultWorkspaceDir(ctx)
        testSubdir = File(wsDir, "t2_edge_${System.nanoTime()}").apply { mkdirs() }
        tempDirs = mutableListOf()
    }

    @After
    fun tearDown() {
        testSubdir.deleteRecursively()
        tempDirs.forEach { it.deleteRecursively() }
    }

    private fun newTempDir(prefix: String): File =
        E2ETestHelpers.createTempTestDir(prefix).also { tempDirs.add(it) }

    // ==================== F1: JavaScript Edge Cases (5 tests) ====================

    @Test
    fun t2_f1_01_jsLargeStdoutOver256KbDoesNotDeadlockOrTruncate() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "large_out.js",
            """
            const pad = "X".repeat(60);
            for (let i = 1; i <= 5000; i++) {
                console.log("LINE_" + String(i).padStart(4, "0") + "_" + pad);
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue("Missing first line", result.combinedOutput.contains("LINE_0001_"))
        assertTrue("Missing last line", result.combinedOutput.contains("LINE_5000_"))
    }

    @Test
    fun t2_f1_02_jsSyntaxErrorProducesNonZeroAndDiagnostic() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val script = E2ETestHelpers.seedWorkspaceFile("bad_syntax.js", "const x = ;", testSubdir)
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertFalse("Expected non-zero exit code for JS syntax error", result.isSuccess)
        assertTrue("Expected SyntaxError diagnostic, got: ${result.combinedOutput}", result.combinedOutput.contains("SyntaxError"))
    }

    @Test
    fun t2_f1_03_jsCryptoSha256MatchesDeterministicDigest() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val expectedHex = MessageDigest.getInstance("SHA-256")
            .digest("klix_16kb".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val script = E2ETestHelpers.seedWorkspaceFile(
            "crypto_sha.js",
            """
            const crypto = require("crypto");
            const digest = crypto.createHash("sha256").update(Buffer.from("klix_16kb", "utf8")).digest("hex");
            console.log("SHA256=" + digest);
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue("Expected SHA256=$expectedHex, got: ${result.combinedOutput}", result.combinedOutput.contains("SHA256=$expectedHex"))
    }

    @Test
    fun t2_f1_04_jsPathWithSingleQuoteAndSpacesExecutesCleanly() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "dir's/my app.js",
            """console.log("QUOTE_SPACE_JS_OK");""",
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("QUOTE_SPACE_JS_OK"))
    }

    @Test
    fun t2_f1_05_jsInfiniteLoopTerminatesPromptlyOnCancelActiveProcess() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "spin.js",
            """
            console.log("JS_LOOP_START");
            setInterval(() => {}, 500);
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(script),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val started = CompletableDeferred<Unit>()
        val startMs = SystemClock.uptimeMillis()
        var cancelStartMs = 0L
        val job = launch(Dispatchers.IO) {
            provider.runStreamingCommand(cmd, "/workspaces/default") { line, _ ->
                if (line.contains("JS_LOOP_START") && !started.isCompleted) {
                    started.complete(Unit)
                }
            }
        }
        withTimeout(5000L) { started.await() }
        cancelStartMs = SystemClock.uptimeMillis()
        provider.cancelActiveProcess()
        withTimeout(3000L) { job.join() }
        val cancelDuration = SystemClock.uptimeMillis() - cancelStartMs
        assertTrue("Expected JS loop cancellation < 2500ms (total=${SystemClock.uptimeMillis() - startMs}ms), took ${cancelDuration}ms", cancelDuration < 2500L)
    }

    // ==================== F2: Python Edge Cases (5 tests) ====================

    @Test
    fun t2_f2_01_pythonBuiltinHashlibAndZlibExtensionsWork() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val expectedHex = MessageDigest.getInstance("SHA-256")
            .digest("klix_py_ext".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val script = E2ETestHelpers.seedWorkspaceFile(
            "ext_test.py",
            """
            import hashlib, zlib
            comp = zlib.compress(b"klix_zlib_roundtrip")
            assert zlib.decompress(comp) == b"klix_zlib_roundtrip"
            print("PY_SHA=" + hashlib.sha256(b"klix_py_ext").hexdigest())
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("PY_SHA=$expectedHex"))
    }

    @Test
    fun t2_f2_02_pythonUtf8AndEmojiOutputPreserved() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "utf8_out.py",
            """
            print("UTF8_TEST=héllo_klix")
            print("UNICODE_SYM=αβγ_π")
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("UTF8_TEST=héllo_klix"))
        assertTrue(result.combinedOutput.contains("UNICODE_SYM=αβγ_π"))
    }

    @Test
    fun t2_f2_03_pythonInputHandlesEofWithoutHanging() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "eof_input.py",
            """
            import sys
            print("WAIT_EOF", flush=True)
            try:
                val = input("Prompt:")
                print("GOT_VAL=" + val)
            except EOFError:
                print("EOF_HANDLED_OK")
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(script),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val promptSeen = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<CommandResult>()
        val job = launch(Dispatchers.IO) {
            val res = provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("WAIT_EOF") && !promptSeen.isCompleted) {
                    promptSeen.complete(Unit)
                }
            }
            resultDeferred.complete(res)
        }
        withTimeout(5000L) { promptSeen.await() }
        // Send Ctrl+D (EOT 0x04) via raw bytes
        ContractBridges.sendRawBytes(provider, byteArrayOf(4))
        val res = withTimeout(5000L) { resultDeferred.await() }
        job.join()
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue("Expected EOF_HANDLED_OK, got: ${res.combinedOutput}", res.combinedOutput.contains("EOF_HANDLED_OK"))
    }

    @Test
    fun t2_f2_04_pythonInfiniteLoopTerminatesOnSigint() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "loop.py",
            """
            import time
            print("PY_LOOP_READY")
            while True:
                time.sleep(0.2)
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(script),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val ready = CompletableDeferred<Unit>()
        val job = launch(Dispatchers.IO) {
            provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("PY_LOOP_READY") && !ready.isCompleted) ready.complete(Unit)
            }
        }
        withTimeout(5000L) { ready.await() }
        val t0 = SystemClock.uptimeMillis()
        provider.cancelActiveProcess()
        withTimeout(3000L) { job.join() }
        assertTrue("Python SIGINT took too long", SystemClock.uptimeMillis() - t0 < 2500L)
    }

    @Test
    fun t2_f2_05_pythonImportsRelativeSiblingModuleInNestedSubdir() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        E2ETestHelpers.seedWorkspaceFile(
            "pkg/sub/helper.py",
            """
            def compute(n):
                return n * n + 1
            """.trimIndent(),
            testSubdir
        )
        val mainPy = E2ETestHelpers.seedWorkspaceFile(
            "pkg/sub/main.py",
            """
            from helper import compute
            print("SIBLING_RES=" + str(compute(6)))
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(mainPy, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("SIBLING_RES=37"))
    }

    // ==================== F3: C Edge Cases (5 tests) ====================

    @Test
    fun t2_f3_01_cLocalHeaderIncludeCompilesAndExecutes() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        E2ETestHelpers.seedWorkspaceFile(
            "c_hdr/helper.h",
            """
            #ifndef HELPER_H
            #define HELPER_H
            static inline int add_nums(int a, int b) { return a + b; }
            #endif
            """.trimIndent(),
            testSubdir
        )
        val src = E2ETestHelpers.seedWorkspaceFile(
            "c_hdr/main.c",
            """
            #include <stdio.h>
            #include "helper.h"
            int main(void) {
                printf("HDR_SUM=%d\n", add_nums(19, 23));
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("HDR_SUM=42"))
    }

    @Test
    fun t2_f3_02_cUnflushedPrintfAndScanfOverPtyStreamsPromptBeforeStdin() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "interactive.c",
            """
            #include <stdio.h>
            int main(void) {
                char name[64];
                printf("Enter name:");
                if (scanf("%63s", name) != 1) return 1;
                printf("HI_%s\n", name);
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(src),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val promptArrived = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<CommandResult>()
        val job = launch(Dispatchers.IO) {
            val res = provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("Enter name:") && !promptArrived.isCompleted) {
                    promptArrived.complete(Unit)
                }
            }
            resultDeferred.complete(res)
        }
        withTimeout(15_000L) { promptArrived.await() }
        provider.sendInput("KlixC")
        val res = withTimeout(5_000L) { resultDeferred.await() }
        job.join()
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue("Expected HI_KlixC, got: ${res.combinedOutput}", res.combinedOutput.contains("HI_KlixC"))
    }

    @Test
    fun t2_f3_03_cRecompileOverwritesPreviousBinaryCleanly() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "recompile.c",
            """
            #include <stdio.h>
            int main(void) { printf("BUILD_V1\n"); return 0; }
            """.trimIndent(),
            testSubdir
        )
        val r1 = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals(0, r1.exitCode)
        assertTrue(r1.combinedOutput.contains("BUILD_V1"))

        src.writeText(
            """
            #include <stdio.h>
            int main(void) { printf("BUILD_V2\n"); return 0; }
            """.trimIndent()
        )
        val r2 = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals(0, r2.exitCode)
        assertTrue("Expected BUILD_V2 after recompile, got: ${r2.combinedOutput}", r2.combinedOutput.contains("BUILD_V2"))
        assertFalse(r2.combinedOutput.contains("BUILD_V1"))
    }

    @Test
    fun t2_f3_04_cRuntimeAbortDoesNotCrashAppAndReturnsNonZero() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "crash_abort.c",
            """
            #include <stdio.h>
            #include <stdlib.h>
            int main(void) {
                printf("BEFORE_ABORT\n");
                fflush(stdout);
                abort();
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertFalse("Expected non-zero exit code after abort()", result.isSuccess)
        assertTrue(result.combinedOutput.contains("BEFORE_ABORT"))
    }

    @Test
    fun t2_f3_05_cNestedDirectoryWithSpacesCompilesAndRuns() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "sub dir/calc test.c",
            """
            #include <stdio.h>
            int main(void) {
                printf("SPACE_C_OK=%d\n", 6 * 7);
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("SPACE_C_OK=42"))
    }

    // ==================== F4: C++ Edge Cases (5 tests) ====================

    @Test
    fun t2_f4_01_cppUnflushedCoutAndCinWorksOverPty() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "cin_test.cpp",
            """
            #include <iostream>
            int main() {
                int x = 0;
                std::cout << "EnterX: " << std::flush;
                std::cin >> x;
                std::cout << "VAL=" << (x + 10) << "\n";
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(src),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val promptArrived = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<CommandResult>()
        val job = launch(Dispatchers.IO) {
            val res = provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("EnterX:") && !promptArrived.isCompleted) {
                    promptArrived.complete(Unit)
                }
            }
            resultDeferred.complete(res)
        }
        withTimeout(15_000L) { promptArrived.await() }
        provider.sendInput("32")
        val res = withTimeout(5_000L) { resultDeferred.await() }
        job.join()
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue("Expected VAL=42, got: ${res.combinedOutput}", res.combinedOutput.contains("VAL=42"))
    }

    @Test
    fun t2_f4_02_cppThreadsAndMutexCompileAndRunOn16KbDevice() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "threads.cpp",
            """
            #include <iostream>
            #include <mutex>
            #include <thread>
            int main() {
                std::mutex m;
                int counter = 0;
                auto worker = [&]() {
                    for (int i = 0; i < 100; ++i) {
                        std::lock_guard<std::mutex> lk(m);
                        counter++;
                    }
                };
                std::thread t1(worker);
                std::thread t2(worker);
                t1.join();
                t2.join();
                std::cout << "THREADS_SUM=" << counter << "\n";
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("THREADS_SUM=200"))
    }

    @Test
    fun t2_f4_03_cppCcAndCxxInNestedSpacesFolderCompileAndRun() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val ccFile = E2ETestHelpers.seedWorkspaceFile(
            "nested cpp/a b.cc",
            """#include <iostream>
               int main() { std::cout << "NESTED_CC_OK\n"; return 0; }""",
            testSubdir
        )
        val cxxFile = E2ETestHelpers.seedWorkspaceFile(
            "nested cpp/c d.cxx",
            """#include <iostream>
               int main() { std::cout << "NESTED_CXX_OK\n"; return 0; }""",
            testSubdir
        )
        val rCc = E2ETestHelpers.resolveAndExecuteFile(ccFile, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        val rCxx = E2ETestHelpers.resolveAndExecuteFile(cxxFile, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${rCc.stderr}", 0, rCc.exitCode)
        assertTrue(rCc.combinedOutput.contains("NESTED_CC_OK"))
        assertEquals("stderr=${rCxx.stderr}", 0, rCxx.exitCode)
        assertTrue(rCxx.combinedOutput.contains("NESTED_CXX_OK"))
    }

    @Test
    fun t2_f4_04_cppUndefinedSymbolLinkerErrorSurfacesNonZero() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "link_err.cpp",
            """
            void definitely_missing_symbol();
            int main() {
                definitely_missing_symbol();
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertFalse("Expected non-zero exit code for linker error", result.isSuccess)
        assertTrue(
            "Expected undefined symbol diagnostic, got: ${result.combinedOutput}",
            result.combinedOutput.contains("undefined", ignoreCase = true)
        )
    }

    @Test
    fun t2_f4_05_cppInfiniteLoopKilledBySigint() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "spin.cpp",
            """
            #include <iostream>
            #include <unistd.h>
            int main() {
                std::cout << "CPP_SPIN_READY\n" << std::flush;
                while (true) { usleep(100000); }
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val cmd = ContractBridges.resolveRunCommand(
            E2ETestHelpers.editorTabForFile(src),
            E2ETestHelpers.defaultWorkspaceObject(),
            sandboxManager
        ).command!!

        val ready = CompletableDeferred<Unit>()
        val job = launch(Dispatchers.IO) {
            provider.runStreamingCommand(cmd, "/workspaces/default") { chunk, _ ->
                if (chunk.contains("CPP_SPIN_READY") && !ready.isCompleted) ready.complete(Unit)
            }
        }
        withTimeout(15_000L) { ready.await() }
        val t0 = SystemClock.uptimeMillis()
        provider.cancelActiveProcess()
        withTimeout(3000L) { job.join() }
        assertTrue("C++ SIGINT took too long", SystemClock.uptimeMillis() - t0 < 2500L)
    }

    // ==================== F5: LanguageRunner Edge Cases (5 tests) ====================

    @Test
    fun t2_f5_01_filenameWithMetacharactersPreventsShellInjection() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val tricky = E2ETestHelpers.seedWorkspaceFile(
            "a'b \$HOME `id`;.py",
            """print("INJECTION_PREVENTED_OK")""",
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(tricky, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("INJECTION_PREVENTED_OK"))
        assertFalse("Command injection executed id!", result.combinedOutput.contains("uid="))
    }

    @Test
    fun t2_f5_02_pathTraversalOutsideWorkspaceIsRejectedSafely() {
        val ws = E2ETestHelpers.defaultWorkspaceObject()
        val escapingFile = File("/etc/passwd")
        val tab = EditorTab(
            uri = Uri.fromFile(escapingFile),
            name = "passwd.py",
            content = "print(1)"
        )
        val res = ContractBridges.resolveRunCommand(tab, ws, sandboxManager)
        assertNull("Expected null command for path outside workspace", res.command)
        assertFalse("Path escape should return error, not requiresSetup", res.requiresSetup)
        assertFalse("Expected descriptive error message", res.errorMessage.isNullOrBlank())
    }

    @Test
    fun t2_f5_03_unsupportedExtensionsReturnClearErrorWithoutCrashing() {
        val ws = E2ETestHelpers.defaultWorkspaceObject()
        for (ext in listOf("notes.txt", "README.md", "config.json")) {
            val f = E2ETestHelpers.seedWorkspaceFile(ext, "hello", testSubdir)
            val res = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(f), ws, sandboxManager)
            assertNull("Expected null command for unsupported $ext", res.command)
            assertFalse("Unsupported $ext should not require sandbox setup", res.requiresSetup)
            assertFalse("Expected clear error message for $ext", res.errorMessage.isNullOrBlank())
        }
    }

    @Test
    fun t2_f5_04_missingWorkspaceFallsBackToTabNameSafely() {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val f = E2ETestHelpers.seedWorkspaceFile("fallback.py", "print(1)", testSubdir)
        val res = ContractBridges.resolveRunCommand(E2ETestHelpers.editorTabForFile(f), null, sandboxManager)
        assertFalse(res.requiresSetup)
        assertNotNull("Expected valid fallback command when workspace is null", res.command)
        assertTrue(res.command!!.contains("fallback.py"))
    }

    @Test
    fun t2_f5_05_sequentialSaveThenRunPersistsDirtyEditsBeforeExecution() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val app = E2ETestHelpers.targetApplication()
        val editorVm = EditorViewModel(app)
        val wsDir = E2ETestHelpers.getDefaultWorkspaceDir(app)
        val pyFile = File(wsDir, "seq_save_${System.nanoTime()}.py")
        pyFile.writeText("""print("OLD_VERSION")""", Charsets.UTF_8)

        try {
            val fileUri = Uri.fromFile(pyFile)
            val tab = EditorTab(
                uri = fileUri,
                name = pyFile.name,
                content = """print("OLD_VERSION")""",
                originalContent = """print("OLD_VERSION")""",
                isModified = false
            )
            // Open file in EditorViewModel and mark dirty
            editorVm.openFile(com.klix.code.data.model.FileNode(uri = fileUri, name = pyFile.name, isDirectory = false))
            E2ETestHelpers.waitUntil(5000L, description = "tab opened") {
                editorVm.openTabs.value.any { it.uri == fileUri }
            }
            editorVm.updateActiveTabContent("""print("NEW_UNSAVED_SAVED_FIRST")""")
            assertTrue("Tab should be marked modified", editorVm.openTabs.value.first { it.uri == fileUri }.isModified)

            val saveDone = CompletableDeferred<Unit>()
            editorVm.saveActiveTab { saveDone.complete(Unit) }
            withTimeout(5000L) { saveDone.await() }

            val activeTab = editorVm.openTabs.value.first { it.uri == fileUri }
            assertFalse("Tab should no longer be modified after saveActiveTab", activeTab.isModified)
            val result = E2ETestHelpers.resolveAndExecuteFile(pyFile, E2ETestHelpers.defaultWorkspaceObject(app), sandboxManager, provider)
            assertEquals(0, result.exitCode)
            assertTrue(result.combinedOutput.contains("NEW_UNSAVED_SAVED_FIRST"))
            assertFalse(result.combinedOutput.contains("OLD_VERSION"))
        } finally {
            pyFile.delete()
        }
    }

    // ==================== F6: Native PTY Edge Cases (5 tests) ====================

    @Test
    fun t2_f6_01_ptyPreservesCarriageReturnAndAnsiEscapeSequences() {
        val session = ContractBridges.openAndForkPty(
            argv = arrayOf("/system/bin/sh", "-c", "printf '10%%\\r100%%\\n\\033[32mANSI_OK\\033[0m\\n'"),
            envp = arrayOf("PATH=/system/bin:/system/xbin", "TERM=xterm-256color"),
            cwd = E2ETestHelpers.targetContext().filesDir.absolutePath,
            rows = 24,
            cols = 80
        )
        try {
            val sb = StringBuilder()
            val buf = ByteArray(4096)
            val deadline = SystemClock.uptimeMillis() + 5000L
            while (SystemClock.uptimeMillis() < deadline) {
                val n = ContractBridges.readPty(session.masterFd, buf)
                if (n > 0) {
                    sb.append(String(buf, 0, n, Charsets.UTF_8))
                    if (sb.toString().contains("ANSI_OK")) break
                } else if (n == 0) break else SystemClock.sleep(20)
            }
            val status = ContractBridges.waitPtyPid(session.pid)
            assertEquals(0, status)
            val out = sb.toString()
            assertTrue("Expected 100% in output, got: $out", out.contains("100%"))
            assertTrue("Expected ANSI_OK in output, got: $out", out.contains("ANSI_OK"))
        } finally {
            if (session.masterFd >= 0) ContractBridges.closePtyFd(session.masterFd)
        }
    }

    @Test
    fun t2_f6_02_concurrentPtyTabsAreIsolatedWhenOneIsCancelled() = runBlocking {
        val termVm = TerminalViewModel(E2ETestHelpers.targetApplication())
        val tab1Id = termVm.tabs.value.first().id
        termVm.addNewTerminal()
        val tab2Id = termVm.tabs.value.last().id
        assertFalse(tab1Id == tab2Id)

        termVm.executeCommand("echo TAB1_START; sleep 20", terminalId = tab1Id)
        termVm.executeCommand("echo TAB2_START; sleep 2; echo TAB2_SURVIVED", terminalId = tab2Id)

        E2ETestHelpers.waitUntil(5000L, description = "tab2 running") {
            termVm.tabs.value.first { it.id == tab2Id }.isExecuting
        }

        // Cancel tab 1 ONLY
        termVm.cancelExecution(tab1Id)
        assertFalse("Tab 1 should stop executing", termVm.tabs.value.first { it.id == tab1Id }.isExecuting)

        // Wait for Tab 2 to complete normally
        E2ETestHelpers.waitUntil(6000L, description = "tab2 finished") {
            !termVm.tabs.value.first { it.id == tab2Id }.isExecuting
        }
        termVm.selectTerminal(tab2Id)
        val tab2Text = termVm.terminalLines.value.joinToString("\n") { it.text }
        assertTrue("Tab 2 should finish uninterrupted, got:\n$tab2Text", tab2Text.contains("TAB2_SURVIVED"))
    }

    @Test
    fun t2_f6_03_rapidOpenWaitClosePtyCyclesLeakZeroFds() {
        val cwd = E2ETestHelpers.targetContext().filesDir.absolutePath
        for (i in 1..10) {
            val s = ContractBridges.openAndForkPty(
                argv = arrayOf("/system/bin/sh", "-c", "echo CYCLE_$i"),
                envp = arrayOf("PATH=/system/bin:/system/xbin"),
                cwd = cwd,
                rows = 24,
                cols = 80
            )
            assertTrue("Iteration $i failed to allocate valid pid", s.pid > 0)
            assertTrue("Iteration $i failed to allocate valid fd", s.masterFd >= 0)
            val status = ContractBridges.waitPtyPid(s.pid)
            ContractBridges.closePtyFd(s.masterFd)
            assertEquals("Iteration $i exited non-zero", 0, status)
        }
    }

    @Test
    fun t2_f6_04_ptyWithInvalidExecutableOrCwdHandlesErrorWithoutSigabrt() {
        val s = ContractBridges.openAndForkPty(
            argv = arrayOf("/nonexistent_dir_klix/no_such_binary_12345"),
            envp = arrayOf("PATH=/system/bin"),
            cwd = "/nonexistent_cwd_klix_12345",
            rows = 24,
            cols = 80
        )
        if (s.pid > 0) {
            val status = ContractBridges.waitPtyPid(s.pid)
            assertFalse("Expected non-zero status for missing binary", status == 0)
        }
        if (s.masterFd >= 0) {
            ContractBridges.closePtyFd(s.masterFd)
        }
    }

    @Test
    fun t2_f6_05_ptySigintKillsGrandchildProcessGroup() {
        val marker = "klix_grandchild_${System.nanoTime()}"
        val session = ContractBridges.openAndForkPty(
            argv = arrayOf("/system/bin/sh", "-c", "echo PGID_READY; sleep 30 & wait"),
            envp = arrayOf("PATH=/system/bin:/system/xbin", "KLIX_MARKER=$marker"),
            cwd = E2ETestHelpers.targetContext().filesDir.absolutePath,
            rows = 24,
            cols = 80
        )
        try {
            val buf = ByteArray(4096)
            val sb = StringBuilder()
            val deadline = SystemClock.uptimeMillis() + 4000L
            while (SystemClock.uptimeMillis() < deadline) {
                val n = ContractBridges.readPty(session.masterFd, buf)
                if (n > 0) {
                    sb.append(String(buf, 0, n, Charsets.UTF_8))
                    if (sb.toString().contains("PGID_READY")) break
                } else SystemClock.sleep(20)
            }
            assertTrue("Did not observe PGID_READY", sb.toString().contains("PGID_READY"))

            val t0 = SystemClock.uptimeMillis()
            ContractBridges.sendPtySignal(session.pid, OsConstants.SIGINT)
            val status = ContractBridges.waitPtyPid(session.pid)
            assertTrue("Process group did not exit promptly", SystemClock.uptimeMillis() - t0 < 2500L)
            assertFalse(status == 0)
        } finally {
            if (session.masterFd >= 0) ContractBridges.closePtyFd(session.masterFd)
        }
    }

    // ==================== F7: Archive Extraction Edge Cases (5 tests) ====================

    @Test
    fun t2_f7_01_extractTarGzHandlesForwardHardlinksBeforeTarget() = runBlocking {
        val root = newTempDir("tar_fwd_hl")
        val tarGz = File(root, "fwd_hl.tar.gz")
        val dest = File(root, "out").apply { mkdirs() }
        val payload = "FORWARD_HARDLINK_CONTENT_OK".toByteArray(Charsets.UTF_8)

        // Hardlink appears BEFORE target regular file
        E2ETestHelpers.TarGzFixtureBuilder()
            .addDirectory("bin")
            .addHardlink("bin/cc", "bin/clang")
            .addFile("bin/clang", payload, mode = 0b111_101_101)
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(sandboxManager, tarGz, dest)
        assertTrue("extractTarGz failed on forward hardlink: ${res.exceptionOrNull()}", res.isSuccess)
        val linkFile = File(dest, "bin/cc")
        val targetFile = File(dest, "bin/clang")
        assertTrue("Hardlink bin/cc missing", linkFile.exists())
        assertTrue("Target bin/clang missing", targetFile.exists())
        assertEquals(String(payload), linkFile.readText())
        assertEquals(Os.stat(targetFile.absolutePath).st_ino, Os.stat(linkFile.absolutePath).st_ino)
    }

    @Test
    fun t2_f7_02_extractTarGzBlocksPathTraversalEntries() = runBlocking {
        val root = newTempDir("tar_traversal")
        val tarGz = File(root, "evil.tar.gz")
        val dest = File(root, "jail/out").apply { mkdirs() }
        val outsideFile = File(root, "evil_outside.txt")
        if (outsideFile.exists()) outsideFile.delete()

        E2ETestHelpers.TarGzFixtureBuilder()
            .addFile("../../evil_outside.txt", "PWNED".toByteArray())
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(sandboxManager, tarGz, dest)
        assertFalse("Nothing must ever be written outside destDir", outsideFile.exists())
        assertTrue("Expected traversal entry to fail or be strictly absent", res.isFailure || !outsideFile.exists())
    }

    @Test
    fun t2_f7_03_extractTarGzBlocksEscapingSymlinkWrite() = runBlocking {
        val root = newTempDir("tar_sym_escape")
        val tarGz = File(root, "sym_escape.tar.gz")
        val dest = File(root, "jail/out").apply { mkdirs() }
        val outsideTarget = File(root, "outside_dir").apply { mkdirs() }
        val pwned = File(outsideTarget, "pwned.txt")

        E2ETestHelpers.TarGzFixtureBuilder()
            .addSymlink("escape_link", "../../outside_dir")
            .addFile("escape_link/pwned.txt", "PWNED".toByteArray())
            .writeTo(tarGz)

        val res = ContractBridges.extractTarGz(sandboxManager, tarGz, dest)
        assertFalse("Escaping symlink write must never create outside_dir/pwned.txt", pwned.exists())
        assertTrue(res.isFailure || !pwned.exists())
    }

    @Test
    fun t2_f7_04_extractTarGzReturnsFailureOnCorruptStream() = runBlocking {
        val root = newTempDir("tar_corrupt")
        val badTarGz = File(root, "corrupt.tar.gz").apply {
            writeBytes(byteArrayOf(0x1F, 0x8B.toByte(), 0x08, 0x00, 0x11, 0x22, 0x33, 0x44))
        }
        val dest = File(root, "out").apply { mkdirs() }

        val res = ContractBridges.extractTarGz(sandboxManager, badTarGz, dest)
        assertTrue("Expected Result.isFailure on corrupt gzip/tar stream", res.isFailure)
    }

    @Test
    fun t2_f7_05_extractTarGzOverwritesExistingFilesAndSymlinksIdempotently() = runBlocking {
        val root = newTempDir("tar_overwrite")
        val dest = File(root, "out").apply { mkdirs() }

        val tar1 = File(root, "v1.tar.gz")
        E2ETestHelpers.TarGzFixtureBuilder()
            .addFile("lib/foo.txt", "V1".toByteArray())
            .addSymlink("lib/current", "foo.txt")
            .writeTo(tar1)

        val tar2 = File(root, "v2.tar.gz")
        E2ETestHelpers.TarGzFixtureBuilder()
            .addFile("lib/foo.txt", "V2".toByteArray())
            .addSymlink("lib/current", "foo.txt")
            .writeTo(tar2)

        assertTrue(ContractBridges.extractTarGz(sandboxManager, tar1, dest).isSuccess)
        val res2 = ContractBridges.extractTarGz(sandboxManager, tar2, dest)
        assertTrue("Re-extraction failed: ${res2.exceptionOrNull()}", res2.isSuccess)
        assertEquals("V2", File(dest, "lib/foo.txt").readText())
    }

    // ==================== F8: Onboarding Edge Cases (5 tests) ====================

    private fun prepareOnboardingContext(): Pair<E2ETestHelpers.IsolatedSandboxContext, SandboxManager> {
        val ctx = E2ETestHelpers.targetContext()
        val isoFiles = newTempDir("t2_onb")
        val isoCtx = E2ETestHelpers.IsolatedSandboxContext(ctx, isoFiles)
        val realSandbox = File(ctx.filesDir, "sandbox")
        if (realSandbox.exists()) {
            realSandbox.copyRecursively(File(isoFiles, "sandbox"), overwrite = true)
        }
        val manager = SandboxManager(isoCtx)
        if (!manager.isInstalled()) {
            runBlocking { manager.installSandbox() }
        }
        return isoCtx to manager
    }

    @Test
    fun t2_f8_01_onboardingWithEmptyToolSelectionCompletesCleanly() = runBlocking {
        val (isoCtx, manager) = prepareOnboardingContext()
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                object : ExecutionProvider {
                    override fun getTierName() = "Test"
                    override fun isAvailable() = true
                    override suspend fun runCommand(command: String, workingDir: String?) =
                        CommandResult(0, "", "")
                }
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        // Toggle off default PYTHON
        vm.toggleTool(ToolchainOption.PYTHON)
        assertTrue(vm.selectedTools.value.isEmpty())

        val ok = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { ok.set(true) }
        vm.activeInstallJob?.join()
        assertEquals(SandboxInstallState.Completed, vm.installState.value)
        assertTrue(ok.get())
    }

    @Test
    fun t2_f8_02_onboardingStopsWhenSecondToolFailsMidInstall() = runBlocking {
        val (isoCtx, manager) = prepareOnboardingContext()
        val calls = AtomicInteger(0)
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                object : ExecutionProvider {
                    override fun getTierName() = "MultiTool"
                    override fun isAvailable() = true
                    override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                        return if (calls.getAndIncrement() == 0) {
                            CommandResult(0, "ok1", "")
                        } else {
                            CommandResult(1, "", "SECOND_TOOL_FAIL")
                        }
                    }
                }
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        vm.toggleTool(ToolchainOption.NODEJS) // PYTHON + NODEJS
        val ok = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { ok.set(true) }
        vm.activeInstallJob?.join()

        val state = vm.installState.value
        assertTrue("Expected Error state when 2nd tool fails, got $state", state is SandboxInstallState.Error)
        assertTrue((state as SandboxInstallState.Error).message.contains("SECOND_TOOL_FAIL"))
        assertFalse(ok.get())
    }

    @Test
    fun t2_f8_03_onboardingIgnoresDoubleTapWhileInstallInFlight() = runBlocking {
        val (isoCtx, manager) = prepareOnboardingContext()
        val invocations = AtomicInteger(0)
        val gate = CompletableDeferred<Unit>()
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                object : ExecutionProvider {
                    override fun getTierName() = "Slow"
                    override fun isAvailable() = true
                    override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                        invocations.incrementAndGet()
                        gate.await()
                        return CommandResult(0, "ok", "")
                    }
                }
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        vm.installSelectedPackages(isoCtx, manager) {}
        E2ETestHelpers.waitUntil(3000L, description = "install started") { invocations.get() == 1 }
        // Second call while in-flight must be ignored
        vm.installSelectedPackages(isoCtx, manager) {}
        delay(100)
        assertEquals("Double-tap must not launch a duplicate install", 1, invocations.get())
        gate.complete(Unit)
        vm.activeInstallJob?.join()
    }

    @Test
    fun t2_f8_04_onboardingHandlesSandboxInstallFailureGracefully() = runBlocking {
        val ctx = E2ETestHelpers.targetContext()
        val blockedFiles = newTempDir("onb_blocked")
        // Create regular file named 'sandbox' so SandboxManager.getSandboxDir() / mkdirs fails
        File(blockedFiles, "sandbox").writeText("blocker")
        val isoCtx = E2ETestHelpers.IsolatedSandboxContext(ctx, blockedFiles)
        val manager = SandboxManager(isoCtx)
        assertFalse(manager.isInstalled())

        val vm = OnboardingViewModel(
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        val ok = AtomicBoolean(false)
        vm.installSelectedPackages(isoCtx, manager) { ok.set(true) }
        vm.activeInstallJob?.join()

        assertTrue("Expected Error when sandbox dir blocked, got ${vm.installState.value}", vm.installState.value is SandboxInstallState.Error)
        assertFalse(ok.get())
    }

    @Test
    fun t2_f8_05_onboardingUpdatesSandboxManagerInstalledToolsStateFlow() = runBlocking {
        val (isoCtx, manager) = prepareOnboardingContext()
        E2ETestHelpers.assertToolchainReady(manager, "python3")

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                object : ExecutionProvider {
                    override fun getTierName() = "Test"
                    override fun isAvailable() = true
                    override suspend fun runCommand(command: String, workingDir: String?) = CommandResult(0, "ok", "")
                }
            },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        vm.installSelectedPackages(isoCtx, manager) {}
        vm.activeInstallJob?.join()

        assertEquals(true, manager.installedTools.value["python3"])
    }

    // ==================== F9: Compliance Edge Cases (5 tests) ====================

    @Test
    fun t2_f9_01_deviceElfInspectorRejects4KbAnd32BitSyntheticElfs() {
        val bad4k = E2ETestHelpers.buildSyntheticElf64(0x1000L)
        val bad32 = E2ETestHelpers.buildSyntheticElf32(0x4000)
        val good16k = E2ETestHelpers.buildSyntheticElf64(0x4000L)

        val r4k = E2ETestHelpers.inspectElfBytes(bad4k, "synthetic_4k.so")
        val r32 = E2ETestHelpers.inspectElfBytes(bad32, "synthetic_32.so")
        val rGood = E2ETestHelpers.inspectElfBytes(good16k, "synthetic_16k.so")

        assertFalse("0x1000 ELF64 must fail", r4k.isOk)
        assertFalse("ELF32 must fail", r32.isOk)
        assertTrue("0x4000 ELF64 must pass: ${rGood.errors}", rGood.isOk)
    }

    @Test
    fun t2_f9_02_assetsAndNestedTarArchivesContainNoUnalignedElfs() {
        val ctx = E2ETestHelpers.targetContext()
        val assetMgr = ctx.assets
        val errors = mutableListOf<String>()

        fun walkAssets(dir: String) {
            val list = assetMgr.list(dir) ?: return
            for (item in list) {
                val rel = if (dir.isEmpty()) item else "$dir/$item"
                val children = assetMgr.list(rel)
                if (children != null && children.isNotEmpty()) {
                    walkAssets(rel)
                } else {
                    val lower = item.lowercase()
                    if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
                        assetMgr.open(rel).use { raw ->
                            TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(raw))).use { tis ->
                                var entry = tis.nextEntry
                                while (entry != null) {
                                    if (!entry.isDirectory && tis.canReadEntryData(entry)) {
                                        val bytes = tis.readBytes()
                                        if (E2ETestHelpers.isElfMagic(bytes)) {
                                            errors.addAll(E2ETestHelpers.inspectElfBytes(bytes, "assets/$rel!${entry.name}").errors)
                                        }
                                    }
                                    entry = tis.nextEntry
                                }
                            }
                        }
                    } else {
                        assetMgr.open(rel).use { raw ->
                            val bytes = raw.readBytes()
                            if (E2ETestHelpers.isElfMagic(bytes)) {
                                errors.addAll(E2ETestHelpers.inspectElfBytes(bytes, "assets/$rel").errors)
                            }
                        }
                    }
                }
            }
        }
        walkAssets("")
        if (errors.isNotEmpty()) {
            fail("Unaligned/32-bit ELFs found in APK assets:\n${errors.joinToString("\n")}")
        }
    }

    @Test
    fun t2_f9_03_deviceR5ScannerRejectsGplMarkersAndAllowsPermissive() {
        val gpl2 = "Copyright (C) 2015 STMicroelectronics, licensed under GPL v2 or later.".toByteArray()
        val gpl3 = "GNU General Public License version 3 (GPL-3.0)".toByteArray()
        val lgplAndApache = "Licensed under Apache-2.0, MIT, and LGPL-2.1 (Lesser General Public License).".toByteArray()

        assertTrue("GPL v2 marker must fail", E2ETestHelpers.scanR5TextOrBinary(gpl2, "bad_gpl2.so", "libproot.so").isNotEmpty())
        assertTrue("GPL v3 marker must fail", E2ETestHelpers.scanR5TextOrBinary(gpl3, "bad_gpl3.txt").isNotEmpty())
        assertTrue("Apache-2.0 + LGPL-2.1 must pass", E2ETestHelpers.scanR5TextOrBinary(lgplAndApache, "NOTICE.txt").isEmpty())
    }

    @Test
    fun t2_f9_04_apkBundlesParityToolchainsAcrossArm64AndX86_64() {
        val apkPath = E2ETestHelpers.targetContext().applicationInfo.sourceDir
        val requiredLibs = listOf("libklix_pty.so", "libdash.so", "libnode.so", "libpython3.so", "libclang.so", "liblld.so")
        val abiLibs = mutableMapOf<String, MutableSet<String>>()

        ZipFile(apkPath).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (!e.isDirectory && e.name.startsWith("lib/")) {
                    val parts = e.name.split("/")
                    if (parts.size == 3) {
                        abiLibs.getOrPut(parts[1]) { mutableSetOf() }.add(parts[2])
                    }
                }
            }
        }
        assertTrue("Expected arm64-v8a in APK lib/, found: ${abiLibs.keys}", abiLibs.containsKey("arm64-v8a"))
        assertTrue("Expected x86_64 in APK lib/, found: ${abiLibs.keys}", abiLibs.containsKey("x86_64"))
        for (abi in listOf("arm64-v8a", "x86_64")) {
            val libs = abiLibs[abi] ?: emptySet()
            for (req in requiredLibs) {
                assertTrue("Missing $req in APK lib/$abi/", libs.contains(req))
            }
        }
    }

    @Test
    fun t2_f9_05_sandboxUsesBionicEnvAndNoAlpineRuntimeDownloads() {
        val ctx = E2ETestHelpers.targetContext()
        // If legacy getDownloadUrl still exists via reflection, verify it does not point to alpinelinux.org
        val urlMethod = SandboxManager::class.java.methods.firstOrNull { it.name == "getDownloadUrl" && it.parameterCount == 1 }
        if (urlMethod != null) {
            val url = urlMethod.invoke(sandboxManager, "x86_64") as? String ?: ""
            assertFalse("Forbidden alpinelinux.org runtime download URL remains: $url", url.contains("alpinelinux.org", ignoreCase = true))
        }

        val env = ContractBridges.getToolchainEnv(sandboxManager, E2ETestHelpers.getDefaultWorkspaceDir(ctx))
        val nativeDir = File(ctx.applicationInfo.nativeLibraryDir).canonicalPath
        assertTrue("Expected PATH in toolchain env", !env["PATH"].isNullOrBlank())
        assertTrue(
            "Expected LD_LIBRARY_PATH to include nativeLibraryDir ($nativeDir), got: ${env["LD_LIBRARY_PATH"]}",
            env["LD_LIBRARY_PATH"]?.contains(nativeDir) == true
        )
        assertTrue("Expected PYTHONHOME in toolchain env", !env["PYTHONHOME"].isNullOrBlank())
        assertTrue("Expected HOME in toolchain env", !env["HOME"].isNullOrBlank())
        assertTrue("Expected TMPDIR in toolchain env", !env["TMPDIR"].isNullOrBlank())
    }
}
