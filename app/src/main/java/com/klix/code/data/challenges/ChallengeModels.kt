package com.klix.code.data.challenges

enum class ChallengeDifficulty(
    val label: String,
    val points: Int
) {
    EASY("Easy", 50),
    MEDIUM("Medium", 100),
    HARD("Hard", 200)
}

enum class ChallengeLanguage(
    val displayName: String,
    val extension: String,
    val runCommandPrefix: String
) {
    PYTHON("Python", "py", "python3"),
    NODEJS("JavaScript", "js", "node"),
    CPP("C++", "cpp", "g++"),
    KOTLIN("Kotlin", "kt", "kotlinc")
}

data class ChallengeTestCase(
    val id: String,
    val name: String,
    val setupAndCallPython: String,
    val expectedDisplay: String
)

data class CodingChallenge(
    val id: String,
    val title: String,
    val shortTitle: String,
    val description: String,
    val difficulty: ChallengeDifficulty,
    val estimatedMinutes: Int,
    val language: ChallengeLanguage,
    val fileName: String,
    val previewSnippet: String,
    val starterCode: String,
    val entrySymbol: String,
    val testCases: List<ChallengeTestCase>
) {
    val points: Int get() = difficulty.points
    val relativeWorkspacePath: String get() = "challenges/$fileName"
}

data class ChallengeTestResult(
    val testId: String,
    val name: String,
    val passed: Boolean,
    val expected: String,
    val actual: String
)

data class ChallengeSubmissionRecord(
    val timestamp: Long,
    val code: String,
    val output: String,
    val stderr: String,
    val exitCode: Int,
    val passedTests: Int,
    val totalTests: Int,
    val solved: Boolean,
    val executionTimeMs: Long
)

data class ChallengeProgress(
    val challengeId: String,
    val trackedCode: String,
    val lastOutput: String = "",
    val lastStderr: String = "",
    val lastExitCode: Int? = null,
    val lastExecutionTimeMs: Long = 0L,
    val lastRunAt: Long? = null,
    val passedTests: Int = 0,
    val totalTests: Int = 0,
    val testResults: List<ChallengeTestResult> = emptyList(),
    val isSolved: Boolean = false,
    val solvedAt: Long? = null,
    val pointsEarned: Int = 0,
    val attemptCount: Int = 0,
    val history: List<ChallengeSubmissionRecord> = emptyList()
)

data class ChallengeProfileState(
    val challenges: List<CodingChallenge> = emptyList(),
    val progressById: Map<String, ChallengeProgress> = emptyMap(),
    val totalPoints: Int = 0,
    val solvedCount: Int = 0
) {
    val totalCount: Int get() = challenges.size
    val remainingCount: Int get() = (challenges.size - solvedCount).coerceAtLeast(0)
}

data class ChallengeEvaluationOutcome(
    val challengeId: String,
    val code: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executionTimeMs: Long,
    val testResults: List<ChallengeTestResult>,
    val allPassed: Boolean,
    val runtimeMissing: Boolean = false
) {
    val passedCount: Int get() = testResults.count { it.passed }
    val totalCount: Int get() = testResults.size
    val combinedOutput: String
        get() = when {
            stdout.isNotBlank() && stderr.isNotBlank() -> "$stdout\n$stderr"
            stdout.isNotBlank() -> stdout
            stderr.isNotBlank() -> stderr
            else -> ""
        }
}

data class ChallengeRewardEvent(
    val challenge: CodingChallenge,
    val pointsAwarded: Int,
    val newTotalPoints: Int
)
