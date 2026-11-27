package com.klix.code.data.challenges

import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.sandbox.SandboxExecutionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class ChallengeEvaluator(
    private val executionProvider: ExecutionProvider
) {
    suspend fun evaluate(
        challenge: CodingChallenge,
        code: String,
        challengeFile: File,
        sandboxWorkingDir: String?,
        relativeScriptPath: String = challenge.relativeWorkspacePath,
        runUserScriptFirst: Boolean = true,
        preCapturedRunResult: CommandResult? = null,
        onOutputLine: ((String, Boolean) -> Unit)? = null
    ): ChallengeEvaluationOutcome = withContext(Dispatchers.IO) {
        challengeFile.parentFile?.mkdirs()
        challengeFile.writeText(code, Charsets.UTF_8)

        // 1. Run user's own script to track exact program stdout / stderr / exitCode
        val userRunResult = when {
            preCapturedRunResult != null -> preCapturedRunResult
            runUserScriptFirst -> {
                val cmd = "${challenge.language.runCommandPrefix} \"$relativeScriptPath\""
                runCommand(cmd, sandboxWorkingDir, onOutputLine)
            }
            else -> CommandResult(exitCode = 0, stdout = "", stderr = "", executionTimeMs = 0L)
        }

        val combinedRunMsg = "${userRunResult.stdout}\n${userRunResult.stderr}"
        if (isRuntimeMissing(userRunResult.exitCode, combinedRunMsg, challenge.language)) {
            val missingResults = challenge.testCases.map { tc ->
                ChallengeTestResult(
                    testId = tc.id,
                    name = tc.name,
                    passed = false,
                    expected = tc.expectedDisplay,
                    actual = "${challenge.language.displayName} runtime not installed"
                )
            }
            return@withContext ChallengeEvaluationOutcome(
                challengeId = challenge.id,
                code = code,
                stdout = userRunResult.stdout,
                stderr = userRunResult.stderr.ifBlank {
                    "${challenge.language.displayName} (${challenge.language.runCommandPrefix}) is not installed in the sandbox."
                },
                exitCode = userRunResult.exitCode,
                executionTimeMs = userRunResult.executionTimeMs,
                testResults = missingResults,
                allPassed = false,
                runtimeMissing = true
            )
        }

        // If the user's file itself crashed with a syntax/runtime error during direct execution,
        // skip verification or still let verifier collect exact traceback; if exitCode != 0 and
        // stderr is non-empty, fail fast with exact stderr unless preCapturedRunResult was empty.
        if (userRunResult.exitCode != 0 && userRunResult.stderr.isNotBlank()) {
            val errorSnippet = userRunResult.stderr.lines().lastOrNull { it.isNotBlank() } ?: "Runtime error"
            val failedResults = challenge.testCases.map { tc ->
                ChallengeTestResult(
                    testId = tc.id,
                    name = tc.name,
                    passed = false,
                    expected = tc.expectedDisplay,
                    actual = errorSnippet
                )
            }
            return@withContext ChallengeEvaluationOutcome(
                challengeId = challenge.id,
                code = code,
                stdout = userRunResult.stdout,
                stderr = userRunResult.stderr,
                exitCode = userRunResult.exitCode,
                executionTimeMs = userRunResult.executionTimeMs,
                testResults = failedResults,
                allPassed = false,
                runtimeMissing = false
            )
        }

        // 2. Run structured test-case verification harness
        val verifyFileName = ".klix_verify_${challenge.id}.py"
        val verifyFile = File(challengeFile.parentFile, verifyFileName)
        val verifyRelPath = if (relativeScriptPath.contains('/')) {
            relativeScriptPath.substringBeforeLast('/') + "/" + verifyFileName
        } else {
            verifyFileName
        }

        val verifierScript = buildPythonVerifierScript(challenge, relativeScriptPath)
        verifyFile.writeText(verifierScript, Charsets.UTF_8)

        val verifyResult = try {
            runCommand("${challenge.language.runCommandPrefix} \"$verifyRelPath\"", sandboxWorkingDir, null)
        } finally {
            verifyFile.delete()
        }

        val verifyCombined = "${verifyResult.stdout}\n${verifyResult.stderr}"
        if (isRuntimeMissing(verifyResult.exitCode, verifyCombined, challenge.language)) {
            val missingResults = challenge.testCases.map { tc ->
                ChallengeTestResult(
                    testId = tc.id,
                    name = tc.name,
                    passed = false,
                    expected = tc.expectedDisplay,
                    actual = "${challenge.language.displayName} runtime not installed"
                )
            }
            return@withContext ChallengeEvaluationOutcome(
                challengeId = challenge.id,
                code = code,
                stdout = userRunResult.stdout,
                stderr = verifyResult.stderr,
                exitCode = verifyResult.exitCode,
                executionTimeMs = userRunResult.executionTimeMs + verifyResult.executionTimeMs,
                testResults = missingResults,
                allPassed = false,
                runtimeMissing = true
            )
        }

        val parsedResults = parseVerificationOutput(challenge, verifyResult.stdout, verifyResult.stderr, verifyResult.exitCode)
        val allPassed = parsedResults.size == challenge.testCases.size && parsedResults.all { it.passed }

        ChallengeEvaluationOutcome(
            challengeId = challenge.id,
            code = code,
            stdout = userRunResult.stdout,
            stderr = if (userRunResult.stderr.isNotBlank()) userRunResult.stderr else verifyResult.stderr,
            exitCode = if (userRunResult.exitCode != 0) userRunResult.exitCode else verifyResult.exitCode,
            executionTimeMs = userRunResult.executionTimeMs + verifyResult.executionTimeMs,
            testResults = parsedResults,
            allPassed = allPassed,
            runtimeMissing = false
        )
    }

    private suspend fun runCommand(
        command: String,
        workingDir: String?,
        onOutputLine: ((String, Boolean) -> Unit)?
    ): CommandResult {
        return if (executionProvider is SandboxExecutionProvider && onOutputLine != null) {
            executionProvider.runStreamingCommand(command, workingDir) { chunk, isStdout ->
                onOutputLine(chunk, isStdout)
            }
        } else {
            executionProvider.runCommand(command, workingDir)
        }
    }

    companion object {
        private const val JSON_LINE_PREFIX = "__KLIX_TEST_JSON__:"

        fun isRuntimeMissing(exitCode: Int, output: String, language: ChallengeLanguage): Boolean {
            val lower = output.lowercase()
            val cmd = language.runCommandPrefix.lowercase()
            return exitCode == 127 ||
                lower.contains("$cmd: not found") ||
                lower.contains("$cmd: command not found") ||
                lower.contains("no such file or directory") && lower.contains(cmd)
        }

        fun buildPythonVerifierScript(challenge: CodingChallenge, targetRelativePath: String): String {
            val sb = StringBuilder()
            sb.appendLine("import contextlib, importlib.util, io, json, os, sys")
            sb.appendLine()
            sb.appendLine("target_path = os.path.abspath(${JSONObject.quote(targetRelativePath)})")
            sb.appendLine("if not os.path.exists(target_path):")
            sb.appendLine("    target_path = os.path.abspath(${JSONObject.quote(challenge.fileName)})")
            sb.appendLine()
            sb.appendLine("def _emit(idx, ok, actual):")
            sb.appendLine("    print('${JSON_LINE_PREFIX}' + json.dumps({'idx': idx, 'ok': bool(ok), 'actual': str(actual)}))")
            sb.appendLine()
            sb.appendLine("try:")
            sb.appendLine("    spec = importlib.util.spec_from_file_location('klix_user_solution', target_path)")
            sb.appendLine("    mod = importlib.util.module_from_spec(spec)")
            sb.appendLine("    with contextlib.redirect_stdout(io.StringIO()):")
            sb.appendLine("        spec.loader.exec_module(mod)")
            sb.appendLine("except Exception as load_err:")
            sb.appendLine("    for idx in range(${challenge.testCases.size}):")
            sb.appendLine("        _emit(idx, False, f'{type(load_err).__name__}: {load_err}')")
            sb.appendLine("    sys.exit(1)")
            sb.appendLine()
            sb.appendLine("if not hasattr(mod, ${JSONObject.quote(challenge.entrySymbol)}):")
            sb.appendLine("    for idx in range(${challenge.testCases.size}):")
            sb.appendLine("        _emit(idx, False, 'Missing ${challenge.entrySymbol} in ${challenge.fileName}')")
            sb.appendLine("    sys.exit(1)")
            sb.appendLine()

            challenge.testCases.forEachIndexed { idx, tc ->
                sb.appendLine("try:")
                sb.appendLine("    with contextlib.redirect_stdout(io.StringIO()):")
                sb.appendLine("        ${tc.setupAndCallPython}")
                sb.appendLine("    _emit($idx, _ok, repr(_actual))")
                sb.appendLine("except Exception as case_err:")
                sb.appendLine("    _emit($idx, False, f'{type(case_err).__name__}: {case_err}')")
                sb.appendLine()
            }
            return sb.toString()
        }

        fun parseVerificationOutput(
            challenge: CodingChallenge,
            stdout: String,
            stderr: String,
            exitCode: Int
        ): List<ChallengeTestResult> {
            val parsedByIdx = mutableMapOf<Int, Pair<Boolean, String>>()
            stdout.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith(JSON_LINE_PREFIX)) {
                    val payload = trimmed.removePrefix(JSON_LINE_PREFIX)
                    try {
                        val obj = JSONObject(payload)
                        val idx = obj.optInt("idx", -1)
                        val ok = obj.optBoolean("ok", false)
                        val actual = obj.optString("actual", "")
                        if (idx in challenge.testCases.indices) {
                            parsedByIdx[idx] = Pair(ok, actual)
                        }
                    } catch (_: Exception) {}
                }
            }

            val fallbackError = stderr.lines().lastOrNull { it.isNotBlank() }
                ?: if (exitCode != 0) "Exit code $exitCode" else "No test output captured"

            return challenge.testCases.mapIndexed { idx, tc ->
                val result = parsedByIdx[idx]
                if (result != null) {
                    ChallengeTestResult(
                        testId = tc.id,
                        name = tc.name,
                        passed = result.first,
                        expected = tc.expectedDisplay,
                        actual = result.second
                    )
                } else {
                    ChallengeTestResult(
                        testId = tc.id,
                        name = tc.name,
                        passed = false,
                        expected = tc.expectedDisplay,
                        actual = fallbackError
                    )
                }
            }
        }
    }
}
