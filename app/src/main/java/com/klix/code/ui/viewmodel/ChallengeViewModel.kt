package com.klix.code.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.challenges.ChallengeCatalog
import com.klix.code.data.challenges.ChallengeEvaluationOutcome
import com.klix.code.data.challenges.ChallengeEvaluator
import com.klix.code.data.challenges.ChallengeProfileState
import com.klix.code.data.challenges.ChallengeProgress
import com.klix.code.data.challenges.ChallengeRepository
import com.klix.code.data.challenges.ChallengeRepositoryImpl
import com.klix.code.data.challenges.ChallengeRewardEvent
import com.klix.code.data.challenges.ChallengeSubmissionRecord
import com.klix.code.data.challenges.CodingChallenge
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.stats.StreakManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChallengeViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ChallengeRepository = ChallengeRepositoryImpl(application),
    private val executionProvider: ExecutionProvider = SandboxExecutionProvider(application),
    private val challenges: List<CodingChallenge> = ChallengeCatalog.dailyChallenges,
    private val scopeOverride: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val scope: CoroutineScope
        get() = scopeOverride ?: viewModelScope

    private val evaluator = ChallengeEvaluator(executionProvider)

    private val _profileState = MutableStateFlow(buildProfileState(repository.loadProgress()))
    val profileState: StateFlow<ChallengeProfileState> = _profileState.asStateFlow()

    private val _activeChallengeId = MutableStateFlow<String?>(null)
    val activeChallengeId: StateFlow<String?> = _activeChallengeId.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _rewardEvent = MutableStateFlow<ChallengeRewardEvent?>(null)
    val rewardEvent: StateFlow<ChallengeRewardEvent?> = _rewardEvent.asStateFlow()

    private val _runtimeMissingPrompt = MutableStateFlow(false)
    val runtimeMissingPrompt: StateFlow<Boolean> = _runtimeMissingPrompt.asStateFlow()

    private var codePersistJob: Job? = null

    private fun buildProfileState(progressMap: Map<String, ChallengeProgress>): ChallengeProfileState {
        val merged = challenges.associate { challenge ->
            val existing = progressMap[challenge.id]
            val progress = existing ?: ChallengeProgress(
                challengeId = challenge.id,
                trackedCode = challenge.starterCode,
                totalTests = challenge.testCases.size
            )
            challenge.id to progress
        }
        return ChallengeProfileState(
            challenges = challenges,
            progressById = merged,
            totalPoints = merged.values.sumOf { it.pointsEarned },
            solvedCount = merged.values.count { it.isSolved }
        )
    }

    /**
     * Prepares the challenge file inside [workspaceDir]/challenges/<fileName> using tracked code
     * if available (or starter code for a new challenge), marks it active, and returns the [File].
     */
    suspend fun prepareChallengeFile(challengeId: String, workspaceDir: File): File? = withContext(Dispatchers.IO) {
        val challenge = challenges.find { it.id == challengeId } ?: return@withContext null
        val existingProgress = _profileState.value.progressById[challengeId]
        val challengesDir = File(workspaceDir, "challenges").apply { if (!exists()) mkdirs() }
        val targetFile = File(challengesDir, challenge.fileName)

        val codeToUse = when {
            targetFile.exists() -> targetFile.readText(Charsets.UTF_8)
            existingProgress != null && existingProgress.trackedCode.isNotBlank() -> existingProgress.trackedCode
            else -> challenge.starterCode
        }

        if (!targetFile.exists() || targetFile.readText(Charsets.UTF_8) != codeToUse) {
            targetFile.writeText(codeToUse, Charsets.UTF_8)
        }

        val updatedProgress = (existingProgress ?: ChallengeProgress(
            challengeId = challenge.id,
            trackedCode = codeToUse,
            totalTests = challenge.testCases.size
        )).copy(
            trackedCode = codeToUse,
            totalTests = challenge.testCases.size
        )

        updateSingleProgress(updatedProgress, persistNow = true)
        _activeChallengeId.value = challenge.id
        targetFile
    }

    fun getChallengeForUri(uri: Uri?): CodingChallenge? {
        if (uri == null) return null
        val path = uri.path ?: return null
        val normalized = path.replace('\\', '/')
        return challenges.find { challenge ->
            normalized.endsWith("/challenges/${challenge.fileName}", ignoreCase = true) ||
                normalized.endsWith("/${challenge.fileName}", ignoreCase = true) && normalized.contains("/challenges/")
        }
    }

    fun setActiveChallenge(challengeId: String?) {
        _activeChallengeId.value = challengeId
    }

    fun onCodeChanged(uri: Uri, newCode: String) {
        val challenge = getChallengeForUri(uri) ?: return
        val current = _profileState.value.progressById[challenge.id] ?: ChallengeProgress(
            challengeId = challenge.id,
            trackedCode = newCode,
            totalTests = challenge.testCases.size
        )
        if (current.trackedCode == newCode) return

        val updated = current.copy(trackedCode = newCode)
        updateSingleProgress(updated, persistNow = false)

        codePersistJob?.cancel()
        codePersistJob = scope.launch(Dispatchers.IO) {
            delay(350)
            repository.saveChallengeProgress(updated)
        }
    }

    fun verifyAndRunChallenge(
        challengeId: String,
        currentCode: String,
        challengeFile: File,
        sandboxWorkingDir: String?,
        onOutputLine: ((String, Boolean) -> Unit)? = null,
        onComplete: ((ChallengeEvaluationOutcome) -> Unit)? = null
    ) {
        val challenge = challenges.find { it.id == challengeId } ?: return
        if (_isVerifying.value) return

        scope.launch {
            _isVerifying.value = true
            try {
                try {
                    StreakManager.recordActivityRun(getApplication())
                } catch (_: Exception) {}

                val outcome = evaluator.evaluate(
                    challenge = challenge,
                    code = currentCode,
                    challengeFile = challengeFile,
                    sandboxWorkingDir = sandboxWorkingDir,
                    relativeScriptPath = challenge.relativeWorkspacePath,
                    runUserScriptFirst = true,
                    onOutputLine = onOutputLine
                )

                applyEvaluationOutcome(challenge, outcome)
                onComplete?.invoke(outcome)
            } finally {
                _isVerifying.value = false
            }
        }
    }

    /**
     * Observes manual terminal executions (e.g., user ran `python3 challenges/two_sum.py` in Terminal)
     * so output & code are still tracked and verified even outside the Run button.
     */
    fun onTerminalCommandCompleted(
        command: String,
        workingDir: String?,
        stdout: String,
        stderr: String,
        exitCode: Int,
        executionTimeMs: Long,
        workspaceDir: File?,
        onComplete: ((ChallengeEvaluationOutcome) -> Unit)? = null
    ) {
        if (workspaceDir == null) return
        val trimmed = command.trim()
        val matched = challenges.find { ch ->
            trimmed.contains(ch.fileName, ignoreCase = true)
        } ?: return

        val challengeFile = File(workspaceDir, matched.relativeWorkspacePath)
        if (!challengeFile.exists()) return

        scope.launch {
            val codeOnDisk = withContext(Dispatchers.IO) {
                try {
                    challengeFile.readText(Charsets.UTF_8)
                } catch (_: Exception) {
                    _profileState.value.progressById[matched.id]?.trackedCode ?: matched.starterCode
                }
            }
            val preCaptured = CommandResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                executionTimeMs = executionTimeMs
            )
            val outcome = evaluator.evaluate(
                challenge = matched,
                code = codeOnDisk,
                challengeFile = challengeFile,
                sandboxWorkingDir = workingDir,
                relativeScriptPath = matched.relativeWorkspacePath,
                runUserScriptFirst = false,
                preCapturedRunResult = preCaptured
            )
            applyEvaluationOutcome(matched, outcome)
            onComplete?.invoke(outcome)
        }
    }

    internal fun applyEvaluationOutcome(challenge: CodingChallenge, outcome: ChallengeEvaluationOutcome) {
        if (outcome.runtimeMissing) {
            _runtimeMissingPrompt.value = true
        }

        val current = _profileState.value.progressById[challenge.id] ?: ChallengeProgress(
            challengeId = challenge.id,
            trackedCode = outcome.code,
            totalTests = challenge.testCases.size
        )

        val now = System.currentTimeMillis()
        val wasSolved = current.isSolved
        val nowSolved = wasSolved || outcome.allPassed
        val points = if (nowSolved) challenge.difficulty.points else 0
        val newlySolved = !wasSolved && outcome.allPassed

        val record = ChallengeSubmissionRecord(
            timestamp = now,
            code = outcome.code,
            output = outcome.stdout,
            stderr = outcome.stderr,
            exitCode = outcome.exitCode,
            passedTests = outcome.passedCount,
            totalTests = outcome.totalCount,
            solved = outcome.allPassed,
            executionTimeMs = outcome.executionTimeMs
        )

        val updated = current.copy(
            trackedCode = outcome.code,
            lastOutput = outcome.stdout,
            lastStderr = outcome.stderr,
            lastExitCode = outcome.exitCode,
            lastExecutionTimeMs = outcome.executionTimeMs,
            lastRunAt = now,
            passedTests = outcome.passedCount,
            totalTests = outcome.totalCount,
            testResults = outcome.testResults,
            isSolved = nowSolved,
            solvedAt = current.solvedAt ?: if (outcome.allPassed) now else null,
            pointsEarned = points,
            attemptCount = current.attemptCount + 1,
            history = (listOf(record) + current.history).take(ChallengeRepositoryImpl.MAX_HISTORY_PER_CHALLENGE)
        )

        updateSingleProgress(updated, persistNow = true)

        if (newlySolved) {
            _rewardEvent.value = ChallengeRewardEvent(
                challenge = challenge,
                pointsAwarded = challenge.difficulty.points,
                newTotalPoints = _profileState.value.totalPoints
            )
        }
    }

    suspend fun resetChallengeToStarter(challengeId: String, workspaceDir: File): String? = withContext(Dispatchers.IO) {
        val challenge = challenges.find { it.id == challengeId } ?: return@withContext null
        val targetFile = File(workspaceDir, challenge.relativeWorkspacePath)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(challenge.starterCode, Charsets.UTF_8)

        val current = _profileState.value.progressById[challenge.id]
        val updated = (current ?: ChallengeProgress(
            challengeId = challenge.id,
            trackedCode = challenge.starterCode,
            totalTests = challenge.testCases.size
        )).copy(
            trackedCode = challenge.starterCode
        )
        updateSingleProgress(updated, persistNow = true)
        challenge.starterCode
    }

    fun dismissRewardEvent() {
        _rewardEvent.value = null
    }

    fun dismissRuntimeMissingPrompt() {
        _runtimeMissingPrompt.value = false
    }

    private fun updateSingleProgress(progress: ChallengeProgress, persistNow: Boolean) {
        val currentMap = _profileState.value.progressById.toMutableMap()
        currentMap[progress.challengeId] = progress
        _profileState.value = buildProfileState(currentMap)
        if (persistNow) {
            scope.launch(Dispatchers.IO) {
                repository.saveChallengeProgress(progress)
            }
        }
    }
}
