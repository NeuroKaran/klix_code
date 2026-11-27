package com.klix.code.e2e

import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.workspace.SandboxWorkspaceManagerImpl
import com.klix.code.e2e.contracts.ContractBridges
import com.klix.code.e2e.helpers.E2ETestHelpers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tier 4 Full End-to-End Real-World User Scenarios (8 tests: T4-01..T4-08).
 */
@RunWith(AndroidJUnit4::class)
class Tier4RealWorldScenariosE2ETest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var createdFiles: MutableList<File>

    @Before
    fun setUp() {
        createdFiles = mutableListOf()
    }

    @After
    fun tearDown() {
        createdFiles.forEach { it.deleteRecursively() }
    }

    @Test
    fun t4_01_firstLaunchOnboardingFlowSelectsToolchainsAndLandsOnDashboard() {
        val ctx = E2ETestHelpers.targetContext()
        val scenario = E2ETestHelpers.launchMainActivity(
            onboardingCompleted = false,
            hasShownSandboxOnboarding = false,
            clearPrefsFirst = true
        )
        try {
            // Page 1 -> Page 2
            E2ETestHelpers.waitForComposeText(composeRule, "Prove Me Wrong", substring = false, timeoutMs = 15_000L)
            SystemClock.sleep(350)
            composeRule.onNodeWithText("Prove Me Wrong", substring = false).performClick()
            composeRule.waitForIdle()

            // Page 2: Python 3 is selected by default; select Node.js and C / C++ too
            E2ETestHelpers.waitForComposeText(composeRule, "Install & Continue", substring = false, timeoutMs = 10_000L)
            SystemClock.sleep(350)
            composeRule.onNodeWithText("Node.js", substring = false).performClick()
            composeRule.onNodeWithText("C / C++", substring = false).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Install & Continue", substring = false).performClick()
            composeRule.waitForIdle()

            // Page 3 appears once provisioning completes
            E2ETestHelpers.waitForComposeText(composeRule, "I'll Code Offline First", substring = false, timeoutMs = 60_000L)
            SystemClock.sleep(350)
            composeRule.onNodeWithText("I'll Code Offline First", substring = false).performClick()
            composeRule.waitForIdle()

            // Page 4 -> IDE Ready
            E2ETestHelpers.waitForComposeText(composeRule, "Let's Ship Something", substring = false, timeoutMs = 10_000L)
            SystemClock.sleep(350)
            composeRule.onNodeWithText("Let's Ship Something", substring = false).performClick()
            composeRule.waitForIdle()

            E2ETestHelpers.waitUntil(15_000L, description = "HomeDashboard rendered") {
                composeRule.onAllNodesWithText("Default Workspace", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            val sandboxManager = SandboxManager(ctx)
            E2ETestHelpers.assertToolchainReady(sandboxManager, "python3", "nodejs", "c", "cpp")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun t4_02_dirtyUnsavedEditThenTapRunExecutesUpdatedPythonInTerminalUi() {
        val ctx = E2ETestHelpers.targetContext()
        val mgr = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(mgr, "python3")

        val fileName = "t4_unsaved_${System.nanoTime()}.py"
        val pyFile = E2ETestHelpers.seedWorkspaceFile(fileName, """print("ON_DISK_INITIAL")""").also {
            createdFiles.add(it)
        }

        val scenario = E2ETestHelpers.launchMainActivity(onboardingCompleted = true, hasShownSandboxOnboarding = true)
        try {
            E2ETestHelpers.dismissOptionalDialogsIfPresent(composeRule)
            E2ETestHelpers.openFileFromDrawer(composeRule, fileName)

            E2ETestHelpers.waitForComposeText(composeRule, "Run", substring = false, timeoutMs = 10_000L)
            // Modify CodeEditor content in-memory without manual Save
            E2ETestHelpers.editSoraEditorText("""print("UNSAVED_EDIT_EXECUTED_OK")""")
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Run", substring = false).performClick()
            composeRule.waitForIdle()

            E2ETestHelpers.waitForComposeText(composeRule, "UNSAVED_EDIT_EXECUTED_OK", substring = true, timeoutMs = 15_000L)
            assertTrue(
                "Disk file should have been saved before execution",
                pyFile.readText().contains("UNSAVED_EDIT_EXECUTED_OK")
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun t4_03_interactiveCCliPrintfScanfInUiTerminal() {
        val ctx = E2ETestHelpers.targetContext()
        E2ETestHelpers.assertToolchainReady(SandboxManager(ctx), "c")

        val fileName = "t4_greet_${System.nanoTime()}.c"
        E2ETestHelpers.seedWorkspaceFile(
            fileName,
            """
            #include <stdio.h>
            int main(void) {
                char name[64];
                printf("Name? ");
                if (scanf("%63s", name) != 1) return 1;
                printf("GREET_%s\n", name);
                return 0;
            }
            """.trimIndent()
        ).also { createdFiles.add(it) }

        val scenario = E2ETestHelpers.launchMainActivity(onboardingCompleted = true, hasShownSandboxOnboarding = true)
        try {
            E2ETestHelpers.dismissOptionalDialogsIfPresent(composeRule)
            E2ETestHelpers.openFileFromDrawer(composeRule, fileName)

            E2ETestHelpers.waitForComposeText(composeRule, "Run", substring = false, timeoutMs = 10_000L)
            composeRule.onNodeWithText("Run", substring = false).performClick()
            composeRule.waitForIdle()

            // Wait for un-flushed C prompt in TerminalView
            E2ETestHelpers.waitForComposeText(composeRule, "Name?", substring = true, timeoutMs = 20_000L)

            val input = composeRule.onNode(hasSetTextAction())
            input.performTextReplacement("KlixUser")
            composeRule.waitForIdle()
            input.performImeAction()
            composeRule.waitForIdle()

            E2ETestHelpers.waitForComposeText(composeRule, "GREET_KlixUser", substring = true, timeoutMs = 10_000L)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun t4_04_interactivePythonLoopAndCtrlCKeycapRecoveryInUiTerminal() {
        val ctx = E2ETestHelpers.targetContext()
        E2ETestHelpers.assertToolchainReady(SandboxManager(ctx), "python3")

        val fileName = "t4_spin_${System.nanoTime()}.py"
        E2ETestHelpers.seedWorkspaceFile(
            fileName,
            """
            import time
            print("PY_UI_LOOP_ACTIVE")
            while True:
                time.sleep(0.2)
            """.trimIndent()
        ).also { createdFiles.add(it) }

        val scenario = E2ETestHelpers.launchMainActivity(onboardingCompleted = true, hasShownSandboxOnboarding = true)
        try {
            E2ETestHelpers.dismissOptionalDialogsIfPresent(composeRule)
            E2ETestHelpers.openFileFromDrawer(composeRule, fileName)

            E2ETestHelpers.waitForComposeText(composeRule, "Run", substring = false, timeoutMs = 10_000L)
            composeRule.onNodeWithText("Run", substring = false).performClick()
            composeRule.waitForIdle()

            E2ETestHelpers.waitForComposeText(composeRule, "PY_UI_LOOP_ACTIVE", substring = true, timeoutMs = 15_000L)
            E2ETestHelpers.waitForComposeText(composeRule, "CTRL-C", substring = false, timeoutMs = 10_000L)
            composeRule.onNodeWithText("CTRL-C", substring = false).performClick()
            composeRule.waitForIdle()

            // Confirm prompt recovered by executing a new shell command via TerminalView
            E2ETestHelpers.waitUntil(5000L, description = "Execute button visible after CTRL-C") {
                composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Execute"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            E2ETestHelpers.submitTerminalCommand(composeRule, "echo AFTER_CTRL_C_UI_OK")
            E2ETestHelpers.waitForComposeText(composeRule, "AFTER_CTRL_C_UI_OK", substring = true, timeoutMs = 10_000L)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun t4_05_missingToolchainPreflightRoutesToSetupAndRecoversInUi() {
        val ctx = E2ETestHelpers.targetContext()
        val mgr = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(mgr, "cpp")

        val sandboxDir = File(ctx.filesDir, "sandbox")
        val backupDir = File(ctx.filesDir, "sandbox_t4_05_backup")
        if (backupDir.exists()) backupDir.deleteRecursively()

        val wsRelDir = "sandbox/rootfs/workspaces/default"
        val fileName = "t4_preflight_${System.nanoTime()}.cpp"

        // Move sandbox data aside so pre-flight detects missing sysroot/sandbox, keeping workspace file ready
        assertTrue("Failed to move sandbox aside for preflight test", sandboxDir.renameTo(backupDir))
        val wsDir = File(ctx.filesDir, wsRelDir).apply { mkdirs() }
        val cppFile = File(wsDir, fileName).apply {
            writeText(
                """
                #include <iostream>
                int main() { std::cout << "KLIX_CPP_RECOVERED_OK\n"; return 0; }
                """.trimIndent()
            )
        }
        createdFiles.add(cppFile)

        try {
            val scenario = E2ETestHelpers.launchMainActivity(onboardingCompleted = true, hasShownSandboxOnboarding = true)
            try {
                E2ETestHelpers.dismissOptionalDialogsIfPresent(composeRule)
                E2ETestHelpers.openFileFromDrawer(composeRule, fileName)

                E2ETestHelpers.waitForComposeText(composeRule, "Run", substring = false, timeoutMs = 10_000L)
                composeRule.onNodeWithText("Run", substring = false).performClick()
                composeRule.waitForIdle()

                // Pre-flight must route to setup dialog ("Local Sandbox") or show clear setup/error without running broken fallback
                E2ETestHelpers.waitUntil(10_000L, description = "Pre-flight setup dialog or error message") {
                    composeRule.onAllNodesWithText("Local Sandbox", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                        composeRule.onAllNodesWithText("Install", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                        composeRule.onAllNodesWithText("not installed", substring = true).fetchSemanticsNodes().isNotEmpty()
                }
                assertTrue(
                    "Broken fallback must not succeed when toolchain missing",
                    composeRule.onAllNodesWithText("KLIX_CPP_RECOVERED_OK", substring = true).fetchSemanticsNodes().isEmpty()
                )

                // Dismiss dialog if shown, restore sandbox provisioning, and tap Run again
                val closeNodes = composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Close")).fetchSemanticsNodes()
                if (closeNodes.isNotEmpty()) {
                    composeRule.onNodeWithContentDescription("Close").performClick()
                    composeRule.waitForIdle()
                }

                // Restore backed-up sandbox + keep cppFile
                val savedCpp = cppFile.readText()
                sandboxDir.deleteRecursively()
                assertTrue(backupDir.renameTo(sandboxDir))
                File(ctx.filesDir, "$wsRelDir/$fileName").writeText(savedCpp)
                mgr.refreshInstalledTools()

                composeRule.onNodeWithText("Run", substring = false).performClick()
                composeRule.waitForIdle()
                E2ETestHelpers.waitForComposeText(composeRule, "KLIX_CPP_RECOVERED_OK", substring = true, timeoutMs = 20_000L)
            } finally {
                scenario.close()
            }
        } finally {
            if (backupDir.exists()) {
                sandboxDir.deleteRecursively()
                backupDir.renameTo(sandboxDir)
            }
            File(ctx.filesDir, "$wsRelDir/$fileName").delete()
        }
    }

    @Test
    fun t4_06_javascriptMultiFileJsonProcessingWorkload() = runBlocking {
        val ctx = E2ETestHelpers.targetContext()
        val mgr = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(mgr, "nodejs")
        val provider = SandboxExecutionProvider(ctx, mgr)
        val projDir = File(E2ETestHelpers.getDefaultWorkspaceDir(ctx), "t4_js_proj_${System.nanoTime()}").apply { mkdirs() }
        createdFiles.add(projDir)

        E2ETestHelpers.seedWorkspaceFile(
            "lib/transform.js",
            """
            function processRecords(records) {
                return records
                    .filter(r => r.active)
                    .map(r => ({ id: r.id, score: r.value * 2 }));
            }
            module.exports = { processRecords };
            """.trimIndent(),
            projDir
        )
        E2ETestHelpers.seedWorkspaceFile(
            "data/input.json",
            """[{"id":1,"active":true,"value":10},{"id":2,"active":false,"value":99},{"id":3,"active":true,"value":25}]""",
            projDir
        )
        val mainJs = E2ETestHelpers.seedWorkspaceFile(
            "main.js",
            """
            const fs = require("fs");
            const path = require("path");
            const { processRecords } = require("./lib/transform");
            const base = ${JSONObject.quote(projDir.absolutePath)};
            const input = JSON.parse(fs.readFileSync(path.join(base, "data/input.json"), "utf8"));
            const output = processRecords(input);
            fs.writeFileSync(path.join(base, "data/output.json"), JSON.stringify(output), "utf8");
            console.log("JS_TRANSFORM_COUNT=" + output.length + "_SUM=" + output.reduce((a, b) => a + b.score, 0));
            """.trimIndent(),
            projDir
        )

        val res = E2ETestHelpers.resolveAndExecuteFile(mainJs, E2ETestHelpers.defaultWorkspaceObject(ctx), mgr, provider)
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        assertTrue(res.combinedOutput.contains("JS_TRANSFORM_COUNT=2_SUM=70"))
        val outFile = File(projDir, "data/output.json")
        assertTrue(outFile.exists())
        assertTrue(outFile.readText().contains("\"score\":50"))
    }

    @Test
    fun t4_07_pythonMultiModuleAlgorithmAndSubprocessWorkload() = runBlocking {
        val ctx = E2ETestHelpers.targetContext()
        val mgr = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(mgr, "python3")
        val provider = SandboxExecutionProvider(ctx, mgr)
        val projDir = File(E2ETestHelpers.getDefaultWorkspaceDir(ctx), "t4_py_proj_${System.nanoTime()}").apply { mkdirs() }
        createdFiles.add(projDir)

        E2ETestHelpers.seedWorkspaceFile(
            "algo/stats.py",
            """
            def primes_up_to(limit):
                sieve = [True] * (limit + 1)
                for p in range(2, int(limit ** 0.5) + 1):
                    if sieve[p]:
                        for m in range(p * p, limit + 1, p):
                            sieve[m] = False
                return [i for i in range(2, limit + 1) if sieve[i]]
            """.trimIndent(),
            projDir
        )
        val cliPy = E2ETestHelpers.seedWorkspaceFile(
            "cli.py",
            """
            import json, subprocess, sys
            from algo.stats import primes_up_to

            primes = primes_up_to(50)
            child = subprocess.run(
                [sys.executable, "-c", "import sys; print('CHILD_SUM=' + str(sum(map(int, sys.argv[1:]))))", *map(str, primes)],
                capture_output=True, text=True, check=True
            )
            print(json.dumps({"prime_count": len(primes), "child": child.stdout.strip()}))
            """.trimIndent(),
            projDir
        )

        val res = E2ETestHelpers.resolveAndExecuteFile(cliPy, E2ETestHelpers.defaultWorkspaceObject(ctx), mgr, provider)
        assertEquals("stderr=${res.stderr}", 0, res.exitCode)
        // Primes <= 50: 15 primes summing to 328
        assertTrue("Expected prime_count 15 & CHILD_SUM=328, got: ${res.combinedOutput}", res.combinedOutput.contains("\"prime_count\": 15"))
        assertTrue(res.combinedOutput.contains("CHILD_SUM=328"))
    }

    @Test
    fun t4_08_cMathAndCpp17MultiWorkspaceSwitchWorkload() = runBlocking {
        val ctx = E2ETestHelpers.targetContext()
        val mgr = SandboxManager(ctx)
        E2ETestHelpers.assertToolchainReady(mgr, "c", "cpp")
        val provider = SandboxExecutionProvider(ctx, mgr)
        val wsMgr = SandboxWorkspaceManagerImpl(ctx, mgr)
        wsMgr.initialize()

        val wsA = wsMgr.createWorkspace("T4 C Workspace ${System.nanoTime()}").getOrThrow()
        val wsB = wsMgr.createWorkspace("T4 Cpp Workspace ${System.nanoTime()}").getOrThrow()
        try {
            val dirA = File(wsA.rootUri.path!!).also { createdFiles.add(it) }
            val dirB = File(wsB.rootUri.path!!).also { createdFiles.add(it) }

            E2ETestHelpers.seedWorkspaceFile(
                "math project/math_ops.h",
                """
                #ifndef MATH_OPS_H
                #define MATH_OPS_H
                #include <math.h>
                static inline double norm2(double x, double y) { return sqrt(x * x + y * y); }
                #endif
                """.trimIndent(),
                dirA
            )
            val statsC = E2ETestHelpers.seedWorkspaceFile(
                "math project/stats.c",
                """
                #include <stdio.h>
                #include "math_ops.h"
                int main(void) {
                    printf("WS_A_NORM=%.1f\n", norm2(3.0, 4.0));
                    return 0;
                }
                """.trimIndent(),
                dirA
            )
            val resA = E2ETestHelpers.resolveAndExecuteFile(statsC, wsA, mgr, provider)
            assertEquals("WS A C failed: ${resA.combinedOutput}", 0, resA.exitCode)
            assertTrue(resA.combinedOutput.contains("WS_A_NORM=5.0"))

            wsMgr.switchWorkspace(wsB.id)
            val stlCpp = E2ETestHelpers.seedWorkspaceFile(
                "stl app/sorter.cpp",
                """
                #include <algorithm>
                #include <iostream>
                #include <map>
                #include <string>
                #include <vector>
                int main() {
                    std::vector<int> v = {9, 3, 7, 1};
                    std::sort(v.begin(), v.end());
                    std::map<std::string, int> m = {{"min", v.front()}, {"max", v.back()}};
                    std::cout << "WS_B_MIN=" << m["min"] << "_MAX=" << m["max"] << "\n";
                    return 0;
                }
                """.trimIndent(),
                dirB
            )
            val resB = E2ETestHelpers.resolveAndExecuteFile(stlCpp, wsB, mgr, provider)
            assertEquals("WS B C++ failed: ${resB.combinedOutput}", 0, resB.exitCode)
            assertTrue(resB.combinedOutput.contains("WS_B_MIN=1_MAX=9"))
        } finally {
            wsMgr.switchWorkspace("default_workspace")
            wsMgr.deleteWorkspace(wsA.id)
            wsMgr.deleteWorkspace(wsB.id)
        }
    }
}
