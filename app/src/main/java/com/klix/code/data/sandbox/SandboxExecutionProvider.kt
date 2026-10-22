package com.klix.code.data.sandbox

import android.content.Context
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class SandboxExecutionProvider(
    private val context: Context,
    val sandboxManager: SandboxManager = SandboxManager(context)
) : ExecutionProvider {

    override fun getTierName(): String = "Alpine Linux Sandbox (On-Device)"

    override fun isAvailable(): Boolean = true

    override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
        return runStreamingCommand(command, workingDir) { _, _ -> }
    }

    @Volatile
    private var activeProcess: Process? = null

    fun sendInput(input: String) {
        try {
            val proc = activeProcess ?: return
            val out = proc.outputStream
            val bytes = (input + "\n").toByteArray(Charsets.UTF_8)
            out.write(bytes)
            out.flush()
        } catch (_: Exception) {}
    }

    fun cancelActiveProcess() {
        try {
            activeProcess?.destroyForcibly()
        } catch (_: Exception) {}
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

            val currentDir = if (!workingDir.isNullOrBlank()) workingDir else "/workspace"
            val wrappedCommand = "cd \"$currentDir\" 2>/dev/null; $command; __KLIX_STATUS=$?; echo \"__KLIX_PWD:\$(pwd):STATUS:\$__KLIX_STATUS\"; exit \$__KLIX_STATUS"

            val effectiveWorkingDir = if (!workingDir.isNullOrBlank() && File(workingDir).exists()) {
                File(workingDir)
            } else {
                File(rootfsDir, "workspace").apply { if (!exists()) mkdirs() }
            }

            // Command builder
            val processBuilder = ProcessBuilder()
            val env = processBuilder.environment()

            if (isSandboxReady) {
                val prootBin = findProotExecutable()
                val loaderBin = findLoaderExecutable()

                if (prootBin != null) {
                    val cmdList = mutableListOf(
                        prootBin.absolutePath,
                        "-r", rootfsDir.absolutePath,
                        "-0", // Emulate root (UID 0) for apk and package installations
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", "${rootfsDir.absolutePath}/tmp:/dev/shm",
                        "-w", currentDir,
                        "/bin/sh", "-c", wrappedCommand
                    )
                    processBuilder.command(cmdList)

                    if (loaderBin != null) {
                        env["PROOT_LOADER"] = loaderBin.absolutePath
                    }
                    env["PROOT_TMP_DIR"] = "${rootfsDir.absolutePath}/tmp"
                    env["PROOT_NO_SECCOMP"] = "1"
                    env["HOME"] = "/root"
                    env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                    env["TERM"] = "xterm-256color"
                    env["LANG"] = "C.UTF-8"
                    env["TMPDIR"] = "/tmp"
                    env["USER"] = "root"
                    env["PYTHONUNBUFFERED"] = "1"
                } else {
                    // Direct userland fallback if proot is missing
                    val alpineSh = File(rootfsDir, "bin/sh")
                    if (alpineSh.exists() && alpineSh.canExecute()) {
                        processBuilder.command(alpineSh.absolutePath, "-c", wrappedCommand)
                    } else {
                        processBuilder.command("sh", "-c", wrappedCommand)
                    }
                    env["PATH"] = "${rootfsDir.absolutePath}/bin:${rootfsDir.absolutePath}/usr/bin:${rootfsDir.absolutePath}/sbin:/system/bin:/system/xbin"
                    env["HOME"] = "${rootfsDir.absolutePath}/root"
                    env["TERM"] = "xterm-256color"
                    env["LANG"] = "C.UTF-8"
                    env["TMPDIR"] = "${rootfsDir.absolutePath}/tmp"
                    env["PYTHONUNBUFFERED"] = "1"
                }
            } else {
                // If user runs apk or linux package commands before sandbox is installed
                val trimmedCmd = command.trim()
                if (trimmedCmd.startsWith("apk ") || trimmedCmd == "apk") {
                    val notice = "[Klix Sandbox]: Alpine Linux is not installed yet.\n" +
                            "Please tap ⚙ (Settings) or type 'sandbox-setup' to download and install Alpine Linux (~5MB).\n"
                    onOutputLine(notice, false)
                    stderrBuilder.append(notice)
                    return@withContext CommandResult(
                        exitCode = 127,
                        stdout = "",
                        stderr = notice.trim(),
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                }

                val notice = "[Sandbox note: Alpine Linux rootfs not installed. Running in host fallback userland.]\n"
                onOutputLine(notice, true)
                stdoutBuilder.append(notice)
                processBuilder.command("sh", "-c", wrappedCommand)
                env["PATH"] = "/system/bin:/system/xbin"
                env["HOME"] = context.filesDir.absolutePath
                env["TERM"] = "xterm-256color"
                env["LANG"] = "C.UTF-8"
                env["PYTHONUNBUFFERED"] = "1"
            }

            if (effectiveWorkingDir.exists()) {
                processBuilder.directory(effectiveWorkingDir)
            }

            val process = processBuilder.start()
            activeProcess = process
            var capturedNewCwd: String? = null
            var capturedExitCode: Int? = null

            val outThread = Thread {
                try {
                    val reader = InputStreamReader(process.inputStream, Charsets.UTF_8)
                    val buffer = CharArray(1024)
                    var count: Int
                    val pendingText = java.lang.StringBuilder()

                    while (reader.read(buffer).also { count = it } != -1) {
                        pendingText.append(buffer, 0, count)

                        while (true) {
                            val newlineIndex = pendingText.indexOf("\n")
                            if (newlineIndex == -1) break

                            val fullLine = pendingText.substring(0, newlineIndex).trimEnd('\r')
                            pendingText.delete(0, newlineIndex + 1)

                            if (fullLine.startsWith("__KLIX_PWD:") && fullLine.contains(":STATUS:")) {
                                val parts = fullLine.removePrefix("__KLIX_PWD:").split(":STATUS:")
                                capturedNewCwd = parts.getOrNull(0)?.trim()
                                capturedExitCode = parts.getOrNull(1)?.trim()?.toIntOrNull()
                            } else {
                                stdoutBuilder.append(fullLine).append("\n")
                                onOutputLine(fullLine + "\n", true)
                            }
                        }

                        // Emit interactive prompts without waiting for a newline
                        if (pendingText.isNotEmpty() && !pendingText.startsWith("__KLIX_PWD")) {
                            val partial = pendingText.toString()
                            stdoutBuilder.append(partial)
                            onOutputLine(partial, true)
                            pendingText.setLength(0)
                        }
                    }

                    if (pendingText.isNotEmpty()) {
                        val remaining = pendingText.toString().trimEnd('\r')
                        if (remaining.startsWith("__KLIX_PWD:") && remaining.contains(":STATUS:")) {
                            val parts = remaining.removePrefix("__KLIX_PWD:").split(":STATUS:")
                            capturedNewCwd = parts.getOrNull(0)?.trim()
                            capturedExitCode = parts.getOrNull(1)?.trim()?.toIntOrNull()
                        } else {
                            stdoutBuilder.append(remaining)
                            onOutputLine(remaining, true)
                        }
                    }
                } catch (_: Exception) {}
            }

            val errThread = Thread {
                try {
                    val reader = InputStreamReader(process.errorStream, Charsets.UTF_8)
                    val buffer = CharArray(1024)
                    var count: Int
                    while (reader.read(buffer).also { count = it } != -1) {
                        val chunk = String(buffer, 0, count)
                        stderrBuilder.append(chunk)
                        onOutputLine(chunk, false)
                    }
                } catch (_: Exception) {}
            }

            outThread.start()
            errThread.start()

            val finished = process.waitFor(600, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                val timeoutMsg = "Command timed out after 600 seconds."
                onOutputLine(timeoutMsg, false)
                return@withContext CommandResult(
                    exitCode = -1,
                    stdout = stdoutBuilder.toString().trim(),
                    stderr = timeoutMsg,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            outThread.join(1000)
            errThread.join(1000)

            val exitVal = capturedExitCode ?: process.exitValue()
            CommandResult(
                exitCode = exitVal,
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim(),
                executionTimeMs = System.currentTimeMillis() - startTime,
                newCwd = if (exitVal == 0) capturedNewCwd else null
            )
        } catch (e: Exception) {
            val err = "Execution error: ${e.message}"
            onOutputLine(err, false)
            CommandResult(
                exitCode = -1,
                stdout = stdoutBuilder.toString().trim(),
                stderr = err,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } finally {
            activeProcess = null
        }
    }

    fun findProotExecutable(): File? {
        // 1. Check application's nativeLibraryDir (standard for bundled native binaries in Android)
        try {
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val nativeProot = File(nativeLibDir, "libproot.so")
            if (nativeProot.exists()) {
                if (!nativeProot.canExecute()) {
                    nativeProot.setExecutable(true, false)
                }
                return nativeProot
            }
        } catch (_: Throwable) {}

        // 2. Check sandbox/bin directory
        val sandboxBin = File(sandboxManager.getBinDir(), "proot")
        if (sandboxBin.exists()) {
            sandboxBin.setExecutable(true, false)
            return sandboxBin
        }

        val sandboxLib = File(sandboxManager.getBinDir(), "libproot.so")
        if (sandboxLib.exists()) {
            sandboxLib.setExecutable(true, false)
            return sandboxLib
        }

        return null
    }

    fun findLoaderExecutable(): File? {
        try {
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val nativeLoader = File(nativeLibDir, "libloader.so")
            if (nativeLoader.exists()) {
                if (!nativeLoader.canExecute()) {
                    nativeLoader.setExecutable(true, false)
                }
                return nativeLoader
            }
        } catch (_: Throwable) {}

        val sandboxLoader = File(sandboxManager.getBinDir(), "loader")
        if (sandboxLoader.exists()) {
            sandboxLoader.setExecutable(true, false)
            return sandboxLoader
        }

        val sandboxLoaderSo = File(sandboxManager.getBinDir(), "libloader.so")
        if (sandboxLoaderSo.exists()) {
            sandboxLoaderSo.setExecutable(true, false)
            return sandboxLoaderSo
        }

        return null
    }
}
