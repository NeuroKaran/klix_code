package com.klix.code.data.challenges

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

interface ChallengeRepository {
    fun loadProgress(): Map<String, ChallengeProgress>
    fun saveProgress(progressMap: Map<String, ChallengeProgress>)
    fun saveChallengeProgress(progress: ChallengeProgress)
    fun clearAllProgress()
}

class ChallengeRepositoryImpl @JvmOverloads constructor(
    private val context: Context,
    private val storageFileOverride: File? = null
) : ChallengeRepository {

    private val storageFile: File
        get() = storageFileOverride ?: File(context.filesDir, "klix_challenges_progress.json")

    @Synchronized
    override fun loadProgress(): Map<String, ChallengeProgress> {
        val file = storageFile
        if (!file.exists()) return emptyMap()
        return try {
            val raw = file.readText(Charsets.UTF_8)
            if (raw.isBlank()) return emptyMap()
            val root = JSONObject(raw)
            val result = mutableMapOf<String, ChallengeProgress>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val obj = root.optJSONObject(id) ?: continue
                result[id] = progressFromJson(id, obj)
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    @Synchronized
    override fun saveProgress(progressMap: Map<String, ChallengeProgress>) {
        val file = storageFile
        try {
            file.parentFile?.mkdirs()
            val root = JSONObject()
            progressMap.forEach { (id, progress) ->
                root.put(id, progressToJson(progress))
            }
            val tmpFile = File(file.parentFile, "${file.name}.tmp")
            tmpFile.writeText(root.toString(2), Charsets.UTF_8)
            if (!tmpFile.renameTo(file)) {
                file.writeText(root.toString(2), Charsets.UTF_8)
                tmpFile.delete()
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    override fun saveChallengeProgress(progress: ChallengeProgress) {
        val current = loadProgress().toMutableMap()
        current[progress.challengeId] = progress
        saveProgress(current)
    }

    @Synchronized
    override fun clearAllProgress() {
        val file = storageFile
        if (file.exists()) {
            file.delete()
        }
    }

    private fun progressToJson(p: ChallengeProgress): JSONObject {
        val testResultsArr = JSONArray().apply {
            p.testResults.forEach { tr ->
                put(JSONObject().apply {
                    put("testId", tr.testId)
                    put("name", tr.name)
                    put("passed", tr.passed)
                    put("expected", tr.expected)
                    put("actual", tr.actual)
                })
            }
        }
        val historyArr = JSONArray().apply {
            p.history.take(MAX_HISTORY_PER_CHALLENGE).forEach { rec ->
                put(JSONObject().apply {
                    put("timestamp", rec.timestamp)
                    put("code", rec.code)
                    put("output", rec.output)
                    put("stderr", rec.stderr)
                    put("exitCode", rec.exitCode)
                    put("passedTests", rec.passedTests)
                    put("totalTests", rec.totalTests)
                    put("solved", rec.solved)
                    put("executionTimeMs", rec.executionTimeMs)
                })
            }
        }
        return JSONObject().apply {
            put("challengeId", p.challengeId)
            put("trackedCode", p.trackedCode)
            put("lastOutput", p.lastOutput)
            put("lastStderr", p.lastStderr)
            if (p.lastExitCode != null) put("lastExitCode", p.lastExitCode) else put("lastExitCode", JSONObject.NULL)
            put("lastExecutionTimeMs", p.lastExecutionTimeMs)
            if (p.lastRunAt != null) put("lastRunAt", p.lastRunAt) else put("lastRunAt", JSONObject.NULL)
            put("passedTests", p.passedTests)
            put("totalTests", p.totalTests)
            put("testResults", testResultsArr)
            put("isSolved", p.isSolved)
            if (p.solvedAt != null) put("solvedAt", p.solvedAt) else put("solvedAt", JSONObject.NULL)
            put("pointsEarned", p.pointsEarned)
            put("attemptCount", p.attemptCount)
            put("history", historyArr)
        }
    }

    private fun progressFromJson(id: String, obj: JSONObject): ChallengeProgress {
        val testsArr = obj.optJSONArray("testResults")
        val tests = mutableListOf<ChallengeTestResult>()
        if (testsArr != null) {
            for (i in 0 until testsArr.length()) {
                val item = testsArr.optJSONObject(i) ?: continue
                tests.add(
                    ChallengeTestResult(
                        testId = item.optString("testId", "case_$i"),
                        name = item.optString("name", "Case ${i + 1}"),
                        passed = item.optBoolean("passed", false),
                        expected = item.optString("expected", ""),
                        actual = item.optString("actual", "")
                    )
                )
            }
        }
        val historyArr = obj.optJSONArray("history")
        val history = mutableListOf<ChallengeSubmissionRecord>()
        if (historyArr != null) {
            for (i in 0 until historyArr.length()) {
                val item = historyArr.optJSONObject(i) ?: continue
                history.add(
                    ChallengeSubmissionRecord(
                        timestamp = item.optLong("timestamp", 0L),
                        code = item.optString("code", ""),
                        output = item.optString("output", ""),
                        stderr = item.optString("stderr", ""),
                        exitCode = item.optInt("exitCode", -1),
                        passedTests = item.optInt("passedTests", 0),
                        totalTests = item.optInt("totalTests", 0),
                        solved = item.optBoolean("solved", false),
                        executionTimeMs = item.optLong("executionTimeMs", 0L)
                    )
                )
            }
        }
        return ChallengeProgress(
            challengeId = obj.optString("challengeId", id),
            trackedCode = obj.optString("trackedCode", ""),
            lastOutput = obj.optString("lastOutput", ""),
            lastStderr = obj.optString("lastStderr", ""),
            lastExitCode = if (obj.isNull("lastExitCode")) null else obj.optInt("lastExitCode"),
            lastExecutionTimeMs = obj.optLong("lastExecutionTimeMs", 0L),
            lastRunAt = if (obj.isNull("lastRunAt")) null else obj.optLong("lastRunAt"),
            passedTests = obj.optInt("passedTests", 0),
            totalTests = obj.optInt("totalTests", 0),
            testResults = tests,
            isSolved = obj.optBoolean("isSolved", false),
            solvedAt = if (obj.isNull("solvedAt")) null else obj.optLong("solvedAt"),
            pointsEarned = obj.optInt("pointsEarned", 0),
            attemptCount = obj.optInt("attemptCount", 0),
            history = history
        )
    }

    companion object {
        const val MAX_HISTORY_PER_CHALLENGE = 15
    }
}
