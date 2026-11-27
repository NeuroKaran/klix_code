package com.klix.code.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.e2e.contracts.ContractBridges
import com.klix.code.e2e.helpers.E2ETestHelpers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tier 1 Core Feature Tests for F1 (JavaScript), F2 (Python), F3 (C), and F4 (C++).
 * 20 tests total (5 per feature).
 */
@RunWith(AndroidJUnit4::class)
class Tier1LanguageExecutionE2ETest {

    private lateinit var sandboxManager: SandboxManager
    private lateinit var provider: SandboxExecutionProvider
    private lateinit var testSubdir: File

    @Before
    fun setUp() {
        val ctx = E2ETestHelpers.targetContext()
        sandboxManager = E2ETestHelpers.ensureSandboxInstalled(ctx)
        provider = SandboxExecutionProvider(ctx, sandboxManager)
        val wsDir = E2ETestHelpers.getDefaultWorkspaceDir(ctx)
        testSubdir = File(wsDir, "t1_lang_${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        testSubdir.deleteRecursively()
    }

    // ==================== F1: JavaScript (5 tests) ====================

    @Test
    fun t1_f1_01_executeHelloJsPrintsExpectedStdoutAndExitsZero() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "hello.js",
            """console.log("KLIX_JS_OK");""",
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("Expected exit code 0 for hello.js. stderr=${result.stderr}", 0, result.exitCode)
        assertTrue("Expected KLIX_JS_OK in stdout, got: ${result.combinedOutput}", result.combinedOutput.contains("KLIX_JS_OK"))
    }

    @Test
    fun t1_f1_02_executeEs2022SyntaxFeatures() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "es2022.js",
            """
            const items = [10, 20, 30];
            const obj = { nested: { val: items.at(-1) } };
            Promise.resolve(obj?.nested?.val ?? -1).then(v => {
                console.log("ES2022_VAL=" + v);
            });
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("ES2022_VAL=30"))
    }

    @Test
    fun t1_f1_03_executeJsFsModuleReadWriteInsideWorkspace() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val outFile = File(testSubdir, "js_fs_out.txt")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "fs_io.js",
            """
            const fs = require("fs");
            const p = ${org.json.JSONObject.quote(outFile.absolutePath)};
            fs.writeFileSync(p, "KLIX_FS_PAYLOAD", "utf8");
            console.log("READ_BACK=" + fs.readFileSync(p, "utf8"));
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("READ_BACK=KLIX_FS_PAYLOAD"))
        assertTrue(outFile.exists())
        assertEquals("KLIX_FS_PAYLOAD", outFile.readText())
    }

    @Test
    fun t1_f1_04_executeJsNonZeroExitCodePropagates() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "exit42.js",
            """console.log("BEFORE_EXIT"); process.exit(42);""",
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals(42, result.exitCode)
        assertTrue(result.combinedOutput.contains("BEFORE_EXIT"))
    }

    @Test
    fun t1_f1_05_nodeBinaryLivesInNativeLibDirAndPrintsVersion() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "nodejs")
        val nodePath = ContractBridges.getExecutablePath(sandboxManager, "nodejs")
        assertNotNull("Expected non-null executable path for nodejs", nodePath)
        val nativeDir = E2ETestHelpers.targetContext().applicationInfo.nativeLibraryDir
        assertTrue(
            "Expected node binary '$nodePath' to reside under nativeLibraryDir '$nativeDir'",
            File(nodePath!!).canonicalPath.startsWith(File(nativeDir).canonicalPath)
        )
        val result = provider.runCommand("'$nodePath' --version", "/workspaces/default")
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(
            "Expected v<major>. version output, got: ${result.combinedOutput}",
            Regex("""v\d+\.\d+""").containsMatchIn(result.combinedOutput)
        )
    }

    // ==================== F2: Python (5 tests) ====================

    @Test
    fun t1_f2_01_executeHelloPyPrintsExpectedStdoutAndExitsZero() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "hello.py",
            """print("KLIX_PY_OK")""",
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("KLIX_PY_OK"))
    }

    @Test
    fun t1_f2_02_pythonStdlibModulesWorkViaPythonHome() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "stdlib_check.py",
            """
            import json, math, pathlib, os
            payload = {"sqrt": int(math.sqrt(144)), "cwd_exists": pathlib.Path(".").exists(), "pid": os.getpid()}
            print("PY_JSON=" + json.dumps(payload, sort_keys=True))
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("\"sqrt\": 12"))
        assertTrue(result.combinedOutput.contains("\"cwd_exists\": true"))
    }

    @Test
    fun t1_f2_03_pythonUnbufferedStreamsChunkBeforeSleepCompletes() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "unbuffered.py",
            """
            import time
            print("FIRST_CHUNK_NOW")
            time.sleep(2.0)
            print("SECOND_CHUNK_DONE")
            """.trimIndent(),
            testSubdir
        )
        val tab = E2ETestHelpers.editorTabForFile(script)
        val cmd = ContractBridges.resolveRunCommand(tab, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager).command!!
        val startMs = System.currentTimeMillis()
        var firstChunkElapsedMs = Long.MAX_VALUE
        val chunks = CopyOnWriteArrayList<String>()

        val result = provider.runStreamingCommand(cmd, "/workspaces/default") { line, _ ->
            chunks.add(line)
            if (line.contains("FIRST_CHUNK_NOW") && firstChunkElapsedMs == Long.MAX_VALUE) {
                firstChunkElapsedMs = System.currentTimeMillis() - startMs
            }
        }
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue("Expected FIRST_CHUNK_NOW before 1600ms, took ${firstChunkElapsedMs}ms", firstChunkElapsedMs < 1600L)
        assertTrue(chunks.joinToString("").contains("SECOND_CHUNK_DONE"))
    }

    @Test
    fun t1_f2_04_pythonTracebackPropagatesNonZeroExitAndDiagnostic() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val script = E2ETestHelpers.seedWorkspaceFile(
            "fail.py",
            """
            def boom():
                return 1 // 0
            boom()
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(script, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertFalse("Expected non-zero exit code for ZeroDivisionError", result.isSuccess)
        assertTrue("Expected ZeroDivisionError in output: ${result.combinedOutput}", result.combinedOutput.contains("ZeroDivisionError"))
    }

    @Test
    fun t1_f2_05_pythonVersionRunsUnderUntrustedApp() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "python3")
        val pyPath = ContractBridges.getExecutablePath(sandboxManager, "python3")
        assertNotNull("Expected non-null executable path for python3", pyPath)
        val result = provider.runCommand("'$pyPath' --version", "/workspaces/default")
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue("Expected Python 3.x, got: ${result.combinedOutput}", result.combinedOutput.contains("Python 3."))
    }

    // ==================== F3: C (5 tests) ====================

    @Test
    fun t1_f3_01_compileAndRunHelloCPrintsExpectedStdoutAndExitsZero() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "hello.c",
            """
            #include <stdio.h>
            int main(void) {
                printf("KLIX_C_OK\n");
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("KLIX_C_OK"))
    }

    @Test
    fun t1_f3_02_compileAndRunCMathWithLibm() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "math_test.c",
            """
            #include <stdio.h>
            #include <math.h>
            int main(void) {
                double val = sqrt(25.0) + pow(2.0, 3.0);
                printf("MATH_RES=%.1f\n", val);
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("MATH_RES=13.0"))
    }

    @Test
    fun t1_f3_03_compileAndRunCDynamicMemoryAllocationOn16KbPages() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "mem_test.c",
            """
            #include <stdio.h>
            #include <stdlib.h>
            #include <string.h>
            int main(void) {
                char *buf = (char *)malloc(65536);
                if (!buf) return 2;
                memset(buf, 'A', 65535);
                buf[65535] = '\0';
                printf("LEN=%zu\n", strlen(buf));
                free(buf);
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("LEN=65535"))
    }

    @Test
    fun t1_f3_04_cCompilerSyntaxErrorReturnsDiagnosticsAndNonZero() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "broken.c",
            """
            #include <stdio.h>
            int main(void) {
                int x = ;
                return x;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertFalse("Expected non-zero exit code for C syntax error", result.isSuccess)
        assertTrue("Expected compiler error diagnostic, got: ${result.combinedOutput}", result.combinedOutput.contains("error:", ignoreCase = true))
    }

    @Test
    fun t1_f3_05_compiledCBinaryRunsCleanlyUnderUntrustedAppSelinux() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "c")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "selinux_c.c",
            """
            #include <stdio.h>
            #include <unistd.h>
            int main(void) {
                printf("UID=%d\n", (int)getuid());
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("SELinux/execution failure: ${result.combinedOutput}", 0, result.exitCode)
        assertFalse("Unexpected Permission denied", result.combinedOutput.contains("Permission denied", ignoreCase = true))
        assertTrue(Regex("""UID=\d+""").containsMatchIn(result.combinedOutput))
    }

    // ==================== F4: C++ (5 tests) ====================

    @Test
    fun t1_f4_01_compileAndRunHelloCppPrintsExpectedStdoutAndExitsZero() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "hello.cpp",
            """
            #include <iostream>
            int main() {
                std::cout << "KLIX_CPP_OK\n";
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("KLIX_CPP_OK"))
    }

    @Test
    fun t1_f4_02_compileAndRunCpp17StlAndStructuredBindings() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "stl17.cpp",
            """
            #include <iostream>
            #include <string>
            #include <utility>
            #include <vector>
            int main() {
                std::vector<std::pair<std::string, int>> items = {{"alpha", 3}, {"beta", 4}};
                int sum = 0;
                for (const auto& [name, count] : items) {
                    sum += count;
                }
                std::cout << "CPP17_SUM=" << sum << "\n";
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("CPP17_SUM=7"))
    }

    @Test
    fun t1_f4_03_compileAndRunCcAndCxxExtensions() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val ccFile = E2ETestHelpers.seedWorkspaceFile(
            "prog.cc",
            """
            #include <iostream>
            int main() { std::cout << "CC_OK\n"; return 0; }
            """.trimIndent(),
            testSubdir
        )
        val cxxFile = E2ETestHelpers.seedWorkspaceFile(
            "prog.cxx",
            """
            #include <iostream>
            int main() { std::cout << "CXX_OK\n"; return 0; }
            """.trimIndent(),
            testSubdir
        )
        val resCc = E2ETestHelpers.resolveAndExecuteFile(ccFile, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        val resCxx = E2ETestHelpers.resolveAndExecuteFile(cxxFile, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("cc stderr=${resCc.stderr}", 0, resCc.exitCode)
        assertTrue(resCc.combinedOutput.contains("CC_OK"))
        assertEquals("cxx stderr=${resCxx.stderr}", 0, resCxx.exitCode)
        assertTrue(resCxx.combinedOutput.contains("CXX_OK"))
    }

    @Test
    fun t1_f4_04_cppExceptionHandlingRuntimeErrorCaughtCleanly() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "exceptions.cpp",
            """
            #include <iostream>
            #include <stdexcept>
            int main() {
                try {
                    throw std::runtime_error("KLIX_CAUGHT_ERR");
                } catch (const std::runtime_error& e) {
                    std::cout << "CAUGHT=" << e.what() << "\n";
                }
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(result.combinedOutput.contains("CAUGHT=KLIX_CAUGHT_ERR"))
    }

    @Test
    fun t1_f4_05_compiledCppBinaryExecutesUnderUntrustedApp() = runBlocking {
        E2ETestHelpers.assertToolchainReady(sandboxManager, "cpp")
        val src = E2ETestHelpers.seedWorkspaceFile(
            "untrusted_cpp.cpp",
            """
            #include <iostream>
            #include <unistd.h>
            int main() {
                std::cout << "CPP_PID=" << getpid() << "\n";
                return 0;
            }
            """.trimIndent(),
            testSubdir
        )
        val result = E2ETestHelpers.resolveAndExecuteFile(src, E2ETestHelpers.defaultWorkspaceObject(), sandboxManager, provider)
        assertEquals("stderr=${result.stderr}", 0, result.exitCode)
        assertTrue(Regex("""CPP_PID=\d+""").containsMatchIn(result.combinedOutput))
    }
}
