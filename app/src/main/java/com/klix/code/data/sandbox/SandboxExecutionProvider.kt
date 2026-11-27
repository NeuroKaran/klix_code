package com.klix.code.data.sandbox

import android.content.Context
import android.system.Os
import android.system.OsConstants
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class SandboxExecutionProvider @JvmOverloads constructor(
    private val context: Context,
    val sandboxManager: SandboxManager = SandboxManager(context),
    private val ptyTransport: PtyTransport = PtyBridge
) : ExecutionProvider {

    override fun getTierName(): String = "Bionic 16 KB Sandbox (On-Device)"

    override fun isAvailable(): Boolean = true

    fun isPtyAvailable(): Boolean = ptyTransport.isAvailable

    override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
        return runStreamingCommand(command, workingDir) { _, _ -> }
    }

    @Volatile
    private var activePtySession: PtySession? = null

    @Volatile
    private var activeFallbackProcess: Process? = null

    @Volatile
    private var currentRows: Int = 24

    @Volatile
    private var currentCols: Int = 80

    fun resizeTerminal(rows: Int, cols: Int) {
        if (rows <= 0 || cols <= 0) return
        currentRows = rows
        currentCols = cols
        val session = activePtySession ?: return
        if (ptyTransport.isAvailable && session.masterFd >= 0) {
            try {
                ptyTransport.resizePty(session.masterFd, rows, cols)
            } catch (_: Throwable) {}
        }
    }

    fun sendInput(input: String) {
        sendRawBytes((input + "\n").toByteArray(Charsets.UTF_8))
    }

    fun sendRawBytes(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val session = activePtySession
        if (session != null && ptyTransport.isAvailable && session.masterFd >= 0) {
            try {
                ptyTransport.writePty(session.masterFd, bytes)
            } catch (_: Throwable) {}
            return
        }
        val proc = activeFallbackProcess ?: return
        try {
            proc.outputStream.write(bytes)
            proc.outputStream.flush()
        } catch (_: Throwable) {}
    }

    fun cancelActiveProcess() {
        val session = activePtySession
        if (session != null && ptyTransport.isAvailable && session.pid > 0) {
            val targetPid = session.pid
            if (session.masterFd >= 0) {
                try {
                    ptyTransport.writePty(session.masterFd, byteArrayOf(3))
                } catch (_: Throwable) {}
            }
            try {
                ptyTransport.sendSignal(targetPid, OsConstants.SIGINT)
            } catch (_: Throwable) {}

            Thread {
                try {
                    Thread.sleep(120)
                    if (activePtySession?.pid == targetPid) {
                        ptyTransport.sendSignal(targetPid, OsConstants.SIGTERM)
                    }
                    Thread.sleep(150)
                    if (activePtySession?.pid == targetPid) {
                        ptyTransport.sendSignal(targetPid, OsConstants.SIGKILL)
                    }
                } catch (_: Throwable) {}
            }.start()
            return
        }

        val proc = activeFallbackProcess ?: return
        try {
            proc.outputStream.write(3)
            proc.outputStream.flush()
        } catch (_: Throwable) {}
        val rootPid = getProcessPid(proc)
        rootPid?.let {
            try { Os.kill(-it, OsConstants.SIGINT) } catch (_: Throwable) {}
            try { Os.kill(it, OsConstants.SIGINT) } catch (_: Throwable) {}
        }
        Thread {
            try {
                Thread.sleep(120)
                rootPid?.let {
                    try { Os.kill(-it, OsConstants.SIGTERM) } catch (_: Throwable) {}
                    try { Os.kill(it, OsConstants.SIGTERM) } catch (_: Throwable) {}
                }
                Thread.sleep(150)
                proc.destroyForcibly()
            } catch (_: Throwable) {}
        }.start()
    }

    private fun getProcessPid(process: Process): Int? {
        return try {
            val pidMethod = process.javaClass.getMethod("pid")
            (pidMethod.invoke(process) as? Number)?.toInt()
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun runStreamingCommand(
        command: String,
        workingDir: String? = null,
        onOutputLine: (line: String, isStdout: Boolean) -> Unit
    ): CommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        try {
            val rootfsDir = sandboxManager.getRootfsDir()
            val isSandboxReady = sandboxManager.isInstalled()

            if (!isSandboxReady && requiresSandboxToolchain(command)) {
                val notice = "Klix Bionic sandbox is not installed yet. Please open Sandbox Setup to initialize the environment.\n"
                onOutputLine(notice, false)
                stderrBuilder.append(notice)
                return@withContext CommandResult(
                    exitCode = 127,
                    stdout = "",
                    stderr = notice.trim(),
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            val effectiveWorkingDir = resolveWorkingDirectory(rootfsDir, workingDir)
            val envMap = sandboxManager.getToolchainEnv(effectiveWorkingDir).toMutableMap()
            val dashExe = if (isSandboxReady) sandboxManager.getExecutablePath("dash") else null

            val envScript = File(rootfsDir, "etc/klix_env.sh")
            val sourcePreamble = if (dashExe != null && envScript.exists()) {
                ". ${LanguageRunner.posixQuote(envScript.absolutePath)}\n"
            } else {
                ""
            }

            val wrappedCommand = buildString {
                append("cd ")
                append(LanguageRunner.posixQuote(effectiveWorkingDir.absolutePath))
                append(" 2>/dev/null || true\n")
                append(sourcePreamble)
                append(command)
                append("\n__KLIX_STATUS=$?\n")
                append("printf '\\n__KLIX_PWD:%s:STATUS:%d\\n' \"\$PWD\" \"\$__KLIX_STATUS\"\n")
                append("exit \$__KLIX_STATUS\n")
            }

            val shellPath = dashExe ?: "/system/bin/sh"
            val argv = arrayOf(shellPath, "-c", wrappedCommand)

            if (ptyTransport.isAvailable) {
                runViaPty(
                    argv = argv,
                    envMap = envMap,
                    cwd = effectiveWorkingDir,
                    rootfsDir = rootfsDir,
                    startTime = startTime,
                    stdoutBuilder = stdoutBuilder,
                    stderrBuilder = stderrBuilder,
                    onOutputLine = onOutputLine
                )
            } else {
                runViaFallbackProcess(
                    argv = argv,
                    envMap = envMap,
                    cwd = effectiveWorkingDir,
                    rootfsDir = rootfsDir,
                    startTime = startTime,
                    stdoutBuilder = stdoutBuilder,
                    stderrBuilder = stderrBuilder,
                    onOutputLine = onOutputLine
                )
            }
        } catch (e: CancellationException) {
            cancelActiveProcess()
            throw e
        } catch (e: Exception) {
            val err = "Execution error: ${e.message}"
            onOutputLine(err, false)
            CommandResult(
                exitCode = -1,
                stdout = stdoutBuilder.toString().trim(),
                stderr = err,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun runViaPty(
        argv: Array<String>,
        envMap: Map<String, String>,
        cwd: File,
        rootfsDir: File,
        startTime: Long,
        stdoutBuilder: StringBuilder,
        stderrBuilder: StringBuilder,
        onOutputLine: (line: String, isStdout: Boolean) -> Unit
    ): CommandResult {
        val envp = envMap.entries.map { "${it.key}=${it.value}" }.toTypedArray()
        val session = ptyTransport.openAndForkPty(
            argv = argv,
            envp = envp,
            cwd = cwd.absolutePath,
            rows = currentRows,
            cols = currentCols
        )
        if (session.pid <= 0 || session.masterFd < 0) {
            val err = "Failed to launch PTY session for ${argv.firstOrNull()}"
            onOutputLine(err, false)
            return CommandResult(
                exitCode = -1,
                stdout = "",
                stderr = err,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        activePtySession = session
        var capturedNewCwd: String? = null
        var capturedExitCode: Int? = null
        val pendingText = StringBuilder()

        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        val byteBuf = ByteArray(4096)
        var carryBytes = ByteArray(0)

        fun emitCleanSegment(text: String) {
            if (text.isEmpty()) return
            stdoutBuilder.append(text)
            onOutputLine(text, true)
        }

        fun drainCompleteLines(flushPartial: Boolean) {
            while (true) {
                val nlIdx = pendingText.indexOf("\n")
                if (nlIdx == -1) break
                val rawLine = pendingText.substring(0, nlIdx).trimEnd('\r')
                pendingText.delete(0, nlIdx + 1)

                if (rawLine.startsWith("__KLIX_PWD:") && rawLine.contains(":STATUS:")) {
                    val parts = rawLine.removePrefix("__KLIX_PWD:").split(":STATUS:")
                    capturedNewCwd = normalizeDisplayCwd(parts.getOrNull(0)?.trim(), rootfsDir)
                    capturedExitCode = parts.getOrNull(1)?.trim()?.toIntOrNull()
                } else {
                    emitCleanSegment(rawLine + "\n")
                }
            }

            if (pendingText.isNotEmpty()) {
                val cur = pendingText.toString()
                val couldBeMarker = "__KLIX_PWD:".startsWith(cur) || cur.startsWith("__KLIX_")
                if (flushPartial && cur.startsWith("__KLIX_PWD:") && cur.contains(":STATUS:")) {
                    val parts = cur.trimEnd('\r').removePrefix("__KLIX_PWD:").split(":STATUS:")
                    capturedNewCwd = normalizeDisplayCwd(parts.getOrNull(0)?.trim(), rootfsDir)
                    capturedExitCode = parts.getOrNull(1)?.trim()?.toIntOrNull()
                    pendingText.setLength(0)
                } else if (flushPartial || !couldBeMarker) {
                    emitCleanSegment(cur)
                    pendingText.setLength(0)
                }
            }
        }

        try {
            val timeoutMs = 600_000L
            while (coroutineContext.isActive) {
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    cancelActiveProcess()
                    val timeoutMsg = "Command timed out after 600 seconds."
                    onOutputLine(timeoutMsg, false)
                    return CommandResult(
                        exitCode = -1,
                        stdout = stdoutBuilder.toString().trim(),
                        stderr = timeoutMsg,
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                }

                val n = ptyTransport.readPty(session.masterFd, byteBuf)
                if (n > 0) {
                    val combined = if (carryBytes.isEmpty()) {
                        byteBuf.copyOf(n)
                    } else {
                        carryBytes + byteBuf.copyOf(n)
                    }
                    val inBb = ByteBuffer.wrap(combined)
                    val outCb = CharBuffer.allocate(combined.size + 4)
                    decoder.decode(inBb, outCb, false)
                    outCb.flip()
                    if (outCb.hasRemaining()) {
                        pendingText.append(outCb.toString())
                    }
                    carryBytes = if (inBb.hasRemaining()) {
                        val rem = ByteArray(inBb.remaining())
                        inBb.get(rem)
                        rem
                    } else {
                        ByteArray(0)
                    }
                    drainCompleteLines(flushPartial = false)
                } else if (n == 0) {
                    break
                } else {
                    // Timeout with no bytes yet; check if anything pending can flush
                    drainCompleteLines(flushPartial = false)
                }
            }

            if (!coroutineContext.isActive) {
                cancelActiveProcess()
                throw CancellationException("Command cancelled")
            }

            // Flush remaining decoded chars
            if (carryBytes.isNotEmpty()) {
                val inBb = ByteBuffer.wrap(carryBytes)
                val outCb = CharBuffer.allocate(carryBytes.size + 4)
                decoder.decode(inBb, outCb, true)
                decoder.flush(outCb)
                outCb.flip()
                if (outCb.hasRemaining()) pendingText.append(outCb.toString())
            }
            drainCompleteLines(flushPartial = true)

            val waitStatus = ptyTransport.waitPid(session.pid)
            val exitVal = capturedExitCode ?: waitStatus
            return CommandResult(
                exitCode = exitVal,
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim(),
                executionTimeMs = System.currentTimeMillis() - startTime,
                newCwd = if (exitVal == 0) capturedNewCwd else null
            )
        } finally {
            if (activePtySession?.pid == session.pid) {
                activePtySession = null
            }
            if (session.masterFd >= 0) {
                try {
                    ptyTransport.closeFd(session.masterFd)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun runViaFallbackProcess(
        argv: Array<String>,
        envMap: Map<String, String>,
        cwd: File,
        rootfsDir: File,
        startTime: Long,
        stdoutBuilder: StringBuilder,
        stderrBuilder: StringBuilder,
        onOutputLine: (line: String, isStdout: Boolean) -> Unit
    ): CommandResult {
        val processBuilder = ProcessBuilder(*argv)
        processBuilder.environment().putAll(envMap)
        if (cwd.exists()) {
            processBuilder.directory(cwd)
        }
        val process = processBuilder.start()
        activeFallbackProcess = process

        var capturedNewCwd: String? = null
        var capturedExitCode: Int? = null

        val outThread = Thread {
            try {
                val reader = InputStreamReader(process.inputStream, Charsets.UTF_8)
                val buf = CharArray(1024)
                val pending = StringBuilder()
                var count: Int
                while (reader.read(buf).also { count = it } != -1) {
                    pending.append(buf, 0, count)
                    while (true) {
                        val nl = pending.indexOf("\n")
                        if (nl == -1) break
                        val line = pending.substring(0, nl).trimEnd('\r')
                        pending.delete(0, nl + 1)
                        if (line.startsWith("__KLIX_PWD:") && line.contains(":STATUS:")) {
                            val parts = line.removePrefix("__KLIX_PWD:").split(":STATUS:")
                            capturedNewCwd = normalizeDisplayCwd(parts.getOrNull(0)?.trim(), rootfsDir)
                            capturedExitCode = parts.getOrNull(1)?.trim()?.toIntOrNull()
                        } else {
                            stdoutBuilder.append(line).append("\n")
                            onOutputLine(line + "\n", true)
                        }
                    }
                    if (pending.isNotEmpty() && !pending.startsWith("__KLIX_")) {
                        val partial = pending.toString()
                        stdoutBuilder.append(partial)
                        onOutputLine(partial, true)
                        pending.setLength(0)
                    }
                }
            } catch (_: Exception) {}
        }

        val errThread = Thread {
            try {
                val reader = InputStreamReader(process.errorStream, Charsets.UTF_8)
                val buf = CharArray(1024)
                var count: Int
                while (reader.read(buf).also { count = it } != -1) {
                    val chunk = String(buf, 0, count)
                    stderrBuilder.append(chunk)
                    onOutputLine(chunk, false)
                }
            } catch (_: Exception) {}
        }

        outThread.start()
        errThread.start()

        try {
            val finished = process.waitFor(600, TimeUnit.SECONDS)
            if (!finished) {
                cancelActiveProcess()
                val timeoutMsg = "Command timed out after 600 seconds."
                onOutputLine(timeoutMsg, false)
                return CommandResult(-1, stdoutBuilder.toString().trim(), timeoutMsg, System.currentTimeMillis() - startTime)
            }
            outThread.join(1000)
            errThread.join(1000)
            val exitVal = capturedExitCode ?: process.exitValue()
            return CommandResult(
                exitCode = exitVal,
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim(),
                executionTimeMs = System.currentTimeMillis() - startTime,
                newCwd = if (exitVal == 0) capturedNewCwd else null
            )
        } finally {
            if (activeFallbackProcess === process) {
                activeFallbackProcess = null
            }
        }
    }

    private fun resolveWorkingDirectory(rootfsDir: File, workingDir: String?): File {
        if (!workingDir.isNullOrBlank()) {
            val direct = File(workingDir)
            if (direct.exists() && direct.isDirectory) {
                return direct
            }
            val insideRootfs = File(rootfsDir, workingDir.removePrefix("/"))
            if (!insideRootfs.exists()) {
                insideRootfs.mkdirs()
            }
            return insideRootfs
        }
        return File(rootfsDir, "workspaces/default").apply { if (!exists()) mkdirs() }
    }

    private fun normalizeDisplayCwd(rawCwd: String?, rootfsDir: File): String? {
        if (rawCwd.isNullOrBlank()) return null
        val rootPrefix = rootfsDir.absolutePath.trimEnd(File.separatorChar) + File.separator
        val canonicalPrefix = try {
            rootfsDir.canonicalPath.trimEnd(File.separatorChar) + File.separator
        } catch (_: Throwable) {
            rootPrefix
        }
        val rel = when {
            rawCwd.startsWith(canonicalPrefix) -> rawCwd.removePrefix(canonicalPrefix)
            rawCwd.startsWith(rootPrefix) -> rawCwd.removePrefix(rootPrefix)
            else -> return rawCwd
        }.replace('\\', '/')
        return if (rel.startsWith("workspaces/")) "/$rel" else rawCwd
    }

    private fun requiresSandboxToolchain(command: String): Boolean {
        val tokens = command.trim().split(Regex("\\s+"))
        val first = tokens.firstOrNull()?.substringAfterLast('/')?.lowercase() ?: return false
        return first in setOf("node", "nodejs", "python", "python3", "clang", "clang++", "gcc", "g++", "lld")
    }
}
