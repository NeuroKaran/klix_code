package com.klix.code

import android.app.Application
import android.net.Uri
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.challenges.ChallengeCatalog
import com.klix.code.data.challenges.ChallengeDifficulty
import com.klix.code.data.challenges.ChallengeEvaluator
import com.klix.code.data.challenges.ChallengeProgress
import com.klix.code.data.challenges.ChallengeRepositoryImpl
import com.klix.code.data.challenges.ChallengeSubmissionRecord
import com.klix.code.data.challenges.ChallengeTestResult
import com.klix.code.ui.viewmodel.ChallengeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChallengeSystemTest {

    private lateinit var tempRoot: File
    private lateinit var workspaceDir: File
    private lateinit var mockApp: Application

    @Before
    fun setup() {
        tempRoot = File(System.getProperty("java.io.tmpdir"), "klix_challenge_test_${System.currentTimeMillis()}")
        tempRoot.mkdirs()
        workspaceDir = File(tempRoot, "workspace").apply { mkdirs() }
        mockApp = mock(Application::class.java)
        `when`(mockApp.filesDir).thenReturn(tempRoot)
        `when`(mockApp.applicationContext).thenReturn(mockApp)
    }

    private fun mockUri(file: File): Uri {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("file")
        `when`(uri.path).thenReturn(file.absolutePath)
        `when`(uri.toString()).thenReturn("file://${file.absolutePath}")
        return uri
    }

    @Test
    fun testCatalogCoversAllDifficultyLevelsWithExpectedPoints() {
        val challenges = ChallengeCatalog.dailyChallenges
        assertEquals(3, challenges.size)

        val easy = challenges.filter { it.difficulty == ChallengeDifficulty.EASY }
        val medium = challenges.filter { it.difficulty == ChallengeDifficulty.MEDIUM }

        assertEquals(2, easy.size)
        assertEquals(1, medium.size)
        assertTrue(easy.all { it.points == 50 })
        assertTrue(medium.all { it.points == 100 })
        assertEquals(200, ChallengeDifficulty.HARD.points)

        assertEquals(challenges.size, challenges.map { it.id }.toSet().size)
        assertEquals(challenges.size, challenges.map { it.fileName }.toSet().size)
        assertTrue(challenges.all { it.starterCode.isNotBlank() && it.testCases.isNotEmpty() })
    }

    @Test
    fun testRepositoryPersistenceRoundTrip() {
        val storageFile = File(tempRoot, "challenges_progress.json")
        val repo = ChallengeRepositoryImpl(mockApp, storageFile)

        val progress = ChallengeProgress(
            challengeId = "two_sum_on",
            trackedCode = "def two_sum(nums, target): return [0, 1]",
            lastOutput = "[0, 1]",
            lastStderr = "",
            lastExitCode = 0,
            lastExecutionTimeMs = 42L,
            lastRunAt = 123456789L,
            passedTests = 4,
            totalTests = 4,
            testResults = listOf(
                ChallengeTestResult("basic", "Basic pair", true, "[0, 1]", "[0, 1]")
            ),
            isSolved = true,
            solvedAt = 123456789L,
            pointsEarned = 50,
            attemptCount = 2,
            history = listOf(
                ChallengeSubmissionRecord(
                    timestamp = 123456789L,
                    code = "def two_sum(nums, target): return [0, 1]",
                    output = "[0, 1]",
                    stderr = "",
                    exitCode = 0,
                    passedTests = 4,
                    totalTests = 4,
                    solved = true,
                    executionTimeMs = 42L
                )
            )
        )

        repo.saveChallengeProgress(progress)

        val reloaded = ChallengeRepositoryImpl(mockApp, storageFile).loadProgress()
        val loaded = reloaded["two_sum_on"]
        assertNotNull(loaded)
        assertEquals(progress.trackedCode, loaded?.trackedCode)
        assertEquals("[0, 1]", loaded?.lastOutput)
        assertEquals(0, loaded?.lastExitCode)
        assertEquals(4, loaded?.passedTests)
        assertEquals(4, loaded?.totalTests)
        assertTrue(loaded?.isSolved == true)
        assertEquals(50, loaded?.pointsEarned)
        assertEquals(2, loaded?.attemptCount)
        assertEquals(1, loaded?.testResults?.size)
        assertEquals(1, loaded?.history?.size)
    }

    @Test
    fun testParseVerificationOutputAndRuntimeMissingDetection() {
        val twoSum = ChallengeCatalog.findById("two_sum_on")!!
        val stdout = buildString {
            appendLine("__KLIX_TEST_JSON__:{\"idx\":0,\"ok\":true,\"actual\":\"[0, 1]\"}")
            appendLine("__KLIX_TEST_JSON__:{\"idx\":1,\"ok\":false,\"actual\":\"[0, 2]\"}")
            appendLine("__KLIX_TEST_JSON__:{\"idx\":2,\"ok\":true,\"actual\":\"[0, 1]\"}")
            appendLine("__KLIX_TEST_JSON__:{\"idx\":3,\"ok\":true,\"actual\":\"[0, 2]\"}")
        }

        val results = ChallengeEvaluator.parseVerificationOutput(twoSum, stdout, "", 0)
        assertEquals(4, results.size)
        assertTrue(results[0].passed)
        assertFalse(results[1].passed)
        assertEquals("[0, 2]", results[1].actual)
        assertTrue(results[2].passed)
        assertTrue(results[3].passed)

        assertTrue(ChallengeEvaluator.isRuntimeMissing(127, "sh: python3: not found", twoSum.language))
        assertFalse(ChallengeEvaluator.isRuntimeMissing(1, "NameError: name 'x' is not defined", twoSum.language))
    }

    @Test
    fun testEvaluatorRunsTracksOutputAndCleansVerifier() = runBlocking {
        val twoSum = ChallengeCatalog.findById("two_sum_on")!!
        val challengeFile = File(workspaceDir, twoSum.relativeWorkspacePath)
        val recordedCommands = mutableListOf<String>()

        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                recordedCommands.add(command)
                return if (command.contains(".klix_verify_")) {
                    CommandResult(
                        exitCode = 0,
                        stdout = (0 until twoSum.testCases.size).joinToString("\n") { idx ->
                            "__KLIX_TEST_JSON__:{\"idx\":$idx,\"ok\":true,\"actual\":\"ok\"}"
                        },
                        stderr = "",
                        executionTimeMs = 15L
                    )
                } else {
                    CommandResult(
                        exitCode = 0,
                        stdout = "[0, 1]",
                        stderr = "",
                        executionTimeMs = 10L
                    )
                }
            }
        }

        val code = """
            def two_sum(nums, target):
                seen = {}
                for i, n in enumerate(nums):
                    if target - n in seen:
                        return [seen[target - n], i]
                    seen[n] = i
                return []
            print(two_sum([2, 7, 11, 15], 9))
        """.trimIndent()

        val outcome = ChallengeEvaluator(fakeProvider).evaluate(
            challenge = twoSum,
            code = code,
            challengeFile = challengeFile,
            sandboxWorkingDir = workspaceDir.absolutePath
        )

        assertTrue(outcome.allPassed)
        assertEquals(4, outcome.passedCount)
        assertEquals("[0, 1]", outcome.stdout)
        assertEquals(2, recordedCommands.size)
        assertEquals(code, challengeFile.readText(Charsets.UTF_8))
        assertFalse(
            "Temporary verifier script must be deleted after evaluation",
            File(challengeFile.parentFile, ".klix_verify_${twoSum.id}.py").exists()
        )
    }

    @Test
    fun testViewModelTracksCodeOutputAndAwardsDifficultyPointsOnce() = runBlocking {
        val storageFile = File(tempRoot, "vm_progress.json")
        val repo = ChallengeRepositoryImpl(mockApp, storageFile)

        var shouldPassTests = false
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                return if (command.contains(".klix_verify_")) {
                    val chId = if (command.contains("palindrome")) "palindrome_check"
                    else if (command.contains("fizz_buzz")) "fizz_buzz"
                    else "two_sum_on"
                    val ch = ChallengeCatalog.findById(chId)!!
                    val out = (0 until ch.testCases.size).joinToString("\n") { idx ->
                        "__KLIX_TEST_JSON__:{\"idx\":$idx,\"ok\":$shouldPassTests,\"actual\":\"res\"}"
                    }
                    CommandResult(exitCode = 0, stdout = out, stderr = "", executionTimeMs = 12L)
                } else {
                    CommandResult(
                        exitCode = 0,
                        stdout = if (shouldPassTests) "[0, 1]" else "[]",
                        stderr = "",
                        executionTimeMs = 8L
                    )
                }
            }
        }

        val vm = ChallengeViewModel(
            application = mockApp,
            repository = repo,
            executionProvider = fakeProvider,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )

        // 1. Prepare Easy challenge ("two_sum_on" -> 50 pts)
        val twoSumFile = vm.prepareChallengeFile("two_sum_on", workspaceDir)!!
        assertTrue(twoSumFile.exists())
        assertEquals("two_sum_on", vm.activeChallengeId.value)

        // Track edited code via URI
        val twoSumUri = mockUri(twoSumFile)
        val draftCode = "def two_sum(nums, target):\n    return []\nprint([])"
        vm.onCodeChanged(twoSumUri, draftCode)
        assertEquals(draftCode, vm.profileState.value.progressById["two_sum_on"]?.trackedCode)

        // 2. Failing attempt tracks output & attempt count without awarding points
        val failLatch = CountDownLatch(1)
        shouldPassTests = false
        vm.verifyAndRunChallenge("two_sum_on", draftCode, twoSumFile, workspaceDir.absolutePath) {
            failLatch.countDown()
        }
        assertTrue(failLatch.await(3, TimeUnit.SECONDS))
        Thread.sleep(50)

        val failedState = vm.profileState.value
        assertEquals(0, failedState.totalPoints)
        assertEquals(0, failedState.solvedCount)
        assertEquals("[]", failedState.progressById["two_sum_on"]?.lastOutput)
        assertEquals(1, failedState.progressById["two_sum_on"]?.attemptCount)
        assertNull(vm.rewardEvent.value)

        // 3. Solving Easy challenge awards +50 pts and emits rewardEvent
        val passLatch = CountDownLatch(1)
        shouldPassTests = true
        val solvedCode = "def two_sum(nums, target): return [0, 1]\nprint([0, 1])"
        vm.verifyAndRunChallenge("two_sum_on", solvedCode, twoSumFile, workspaceDir.absolutePath) {
            passLatch.countDown()
        }
        assertTrue(passLatch.await(3, TimeUnit.SECONDS))
        Thread.sleep(50)

        val solvedState = vm.profileState.value
        assertEquals(50, solvedState.totalPoints)
        assertEquals(1, solvedState.solvedCount)
        assertEquals("[0, 1]", solvedState.progressById["two_sum_on"]?.lastOutput)
        assertEquals(solvedCode, solvedState.progressById["two_sum_on"]?.trackedCode)
        assertEquals(2, solvedState.progressById["two_sum_on"]?.attemptCount)
        assertNotNull(vm.rewardEvent.value)
        assertEquals(50, vm.rewardEvent.value?.pointsAwarded)
        vm.dismissRewardEvent()

        // 4. Re-running Easy challenge again must NOT double-award points
        val repeatLatch = CountDownLatch(1)
        vm.verifyAndRunChallenge("two_sum_on", solvedCode, twoSumFile, workspaceDir.absolutePath) {
            repeatLatch.countDown()
        }
        assertTrue(repeatLatch.await(3, TimeUnit.SECONDS))
        Thread.sleep(50)
        assertEquals(50, vm.profileState.value.totalPoints)
        assertNull(vm.rewardEvent.value)

        // 5. Solving Medium challenge ("fizz_buzz" -> +100 pts) accumulates 150 total pts
        val fizzFile = vm.prepareChallengeFile("fizz_buzz", workspaceDir)!!
        val medLatch = CountDownLatch(1)
        vm.verifyAndRunChallenge("fizz_buzz", "def fizz_buzz(n): return []", fizzFile, workspaceDir.absolutePath) {
            medLatch.countDown()
        }
        assertTrue(medLatch.await(3, TimeUnit.SECONDS))
        Thread.sleep(50)

        assertEquals(150, vm.profileState.value.totalPoints)
        assertEquals(2, vm.profileState.value.solvedCount)
        assertEquals(100, vm.rewardEvent.value?.pointsAwarded)
    }

    @Test
    fun testPythonVerifierScriptsWorkEndToEndWithRealPythonWhenAvailable() = runBlocking {
        val pythonCmd = listOf("python3", "python").firstOrNull { cmd ->
            try {
                val p = ProcessBuilder(cmd, "--version").start()
                p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0
            } catch (_: Exception) {
                false
            }
        } ?: return@runBlocking

        val realProvider = object : ExecutionProvider {
            override fun getTierName(): String = "HostPython"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                val fixed = if (command.startsWith("python3 ")) {
                    command.replaceFirst("python3 ", "$pythonCmd ")
                } else {
                    command
                }
                val isWin = (System.getProperty("os.name") ?: "").lowercase().contains("win")
                val pb = if (isWin) {
                    ProcessBuilder("cmd", "/c", fixed)
                } else {
                    ProcessBuilder("sh", "-c", fixed)
                }
                if (workingDir != null) pb.directory(File(workingDir))
                val p = pb.start()
                val out = p.inputStream.bufferedReader().readText()
                val err = p.errorStream.bufferedReader().readText()
                p.waitFor(10, TimeUnit.SECONDS)
                return CommandResult(p.exitValue(), out.trim(), err.trim(), 10L)
            }
        }

        val evaluator = ChallengeEvaluator(realProvider)

        val solutions = mapOf(
            "two_sum_on" to """
                def two_sum(nums, target):
                    seen = {}
                    for i, x in enumerate(nums):
                        if target - x in seen:
                            return [seen[target - x], i]
                        seen[x] = i
                    return []
                print(two_sum([2, 7, 11, 15], 9))
            """.trimIndent(),
            "palindrome_check" to """
                def is_palindrome(word):
                    return word == word[::-1]
                print(is_palindrome("racecar"))
            """.trimIndent(),
            "fizz_buzz" to """
                def fizz_buzz(n):
                    res = []
                    for i in range(1, n + 1):
                        if i % 15 == 0:
                            res.append("FizzBuzz")
                        elif i % 3 == 0:
                            res.append("Fizz")
                        elif i % 5 == 0:
                            res.append("Buzz")
                        else:
                            res.append(str(i))
                    return res
                print(fizz_buzz(5))
            """.trimIndent()
        )

        for (challenge in ChallengeCatalog.dailyChallenges) {
            val sol = solutions[challenge.id]!!
            val targetFile = File(workspaceDir, challenge.relativeWorkspacePath)
            val outcome = evaluator.evaluate(
                challenge = challenge,
                code = sol,
                challengeFile = targetFile,
                sandboxWorkingDir = workspaceDir.absolutePath
            )
            assertTrue(
                "Expected ${challenge.id} to pass all cases, stderr=${outcome.stderr}, results=${outcome.testResults}",
                outcome.allPassed
            )
        }
    }

    @Test
    fun testManualTerminalRunTracksOutputAndAwardsPoints() {
        val storageFile = File(tempRoot, "terminal_track_progress.json")
        val repo = ChallengeRepositoryImpl(mockApp, storageFile)
        val fizzChallenge = ChallengeCatalog.findById("fizz_buzz")!!
        val targetFile = File(workspaceDir, fizzChallenge.relativeWorkspacePath)
        targetFile.parentFile?.mkdirs()
        val solvedCode = """
            def fizz_buzz(n):
                return ["FizzBuzz" if i % 15 == 0 else "Fizz" if i % 3 == 0 else "Buzz" if i % 5 == 0 else str(i) for i in range(1, n + 1)]
        """.trimIndent()
        targetFile.writeText(solvedCode, Charsets.UTF_8)

        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                val out = (0 until fizzChallenge.testCases.size).joinToString("\n") { idx ->
                    "__KLIX_TEST_JSON__:{\"idx\":$idx,\"ok\":true,\"actual\":\"ok\"}"
                }
                return CommandResult(exitCode = 0, stdout = out, stderr = "", executionTimeMs = 9L)
            }
        }

        val vm = ChallengeViewModel(
            application = mockApp,
            repository = repo,
            executionProvider = fakeProvider,
            scopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )

        val latch = CountDownLatch(1)
        vm.onTerminalCommandCompleted(
            command = "python3 challenges/fizz_buzz.py",
            workingDir = workspaceDir.absolutePath,
            stdout = "['1', '2', 'Fizz', '4', 'Buzz']",
            stderr = "",
            exitCode = 0,
            executionTimeMs = 15L,
            workspaceDir = workspaceDir
        ) {
            latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))

        val state = vm.profileState.value
        assertEquals(100, state.totalPoints)
        assertEquals(1, state.solvedCount)
        assertEquals("['1', '2', 'Fizz', '4', 'Buzz']", state.progressById["fizz_buzz"]?.lastOutput)
        assertEquals(solvedCode, state.progressById["fizz_buzz"]?.trackedCode)
        assertEquals(100, vm.rewardEvent.value?.pointsAwarded)
    }
}
