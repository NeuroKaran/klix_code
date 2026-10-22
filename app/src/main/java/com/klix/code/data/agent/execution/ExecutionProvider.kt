package com.klix.code.data.agent.execution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long = 0L,
    val newCwd: String? = null
) {
    val isSuccess: Boolean get() = exitCode == 0
    val combinedOutput: String get() = if (stderr.isBlank()) stdout else "$stdout\n$stderr".trim()
}

interface ExecutionProvider {
    fun getTierName(): String
    fun isAvailable(): Boolean
    suspend fun runCommand(command: String, workingDir: String? = null): CommandResult
}

class LocalExecutionProvider : ExecutionProvider {

    override fun getTierName(): String = "Local Sandbox (On-Device)"

    override fun isAvailable(): Boolean = true

    override suspend fun runCommand(command: String, workingDir: String?): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            // Run command via sh/system shell
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val outThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdoutBuilder.append(line).append("\n")
                    }
                }
            }

            val errThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderrBuilder.append(line).append("\n")
                    }
                }
            }

            outThread.start()
            errThread.start()

            // Wait up to 60 seconds
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext CommandResult(
                    exitCode = -1,
                    stdout = stdoutBuilder.toString().trim(),
                    stderr = "Command timed out after 60 seconds",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            outThread.join(1000)
            errThread.join(1000)

            CommandResult(
                exitCode = process.exitValue(),
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim(),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            CommandResult(
                exitCode = -1,
                stdout = "",
                stderr = "Command failed to execute: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
}
