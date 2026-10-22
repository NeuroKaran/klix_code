package com.klix.code.ui.terminal

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TerminalLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val color: Color = TextPrimary,
    val isCommand: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class TerminalViewModel @JvmOverloads constructor(
    application: Application,
    val sandboxExecutionProvider: SandboxExecutionProvider = SandboxExecutionProvider(application),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AndroidViewModel(application) {

    val sandboxManager: SandboxManager = sandboxExecutionProvider.sandboxManager

    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _currentCwd = MutableStateFlow("/workspace")
    val currentCwd: StateFlow<String> = _currentCwd.asStateFlow()

    private val _historyIndex = MutableStateFlow(-1)

    private val _showSetupDialog = MutableStateFlow(false)
    val showSetupDialog: StateFlow<Boolean> = _showSetupDialog.asStateFlow()

    private var activeJob: Job? = null

    init {
        printBanner()
    }

    private fun printBanner() {
        val isReady = sandboxManager.isInstalled()
        val banner = mutableListOf(
            TerminalLine(text = "KlixCode Linux Terminal [Alpine Sandbox v1]", color = AccentPrimary)
        )
        if (!isReady) {
            banner.add(
                TerminalLine(
                    text = "Sandbox not configured. Tap ⚙ or type 'sandbox-setup' to install Alpine Linux.",
                    color = AccentWarning
                )
            )
        } else {
            banner.add(
                TerminalLine(
                    text = "Alpine Linux sandbox active. Type 'help' for shortcuts or 'sandbox-setup' to manage tools.",
                    color = AccentSecondary
                )
            )
        }
        banner.add(
            TerminalLine(text = "--------------------------------------------------------", color = TextSecondary)
        )
        _terminalLines.value = banner
    }

    fun setWorkingDirectory(path: String) {
        _currentCwd.value = path
    }

    fun openSetupDialog() {
        _showSetupDialog.value = true
    }

    fun closeSetupDialog() {
        _showSetupDialog.value = false
    }

    fun executeCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        // Add to history
        val updatedHistory = _commandHistory.value.toMutableList()
        if (updatedHistory.isEmpty() || updatedHistory.last() != trimmed) {
            updatedHistory.add(trimmed)
        }
        _commandHistory.value = updatedHistory
        _historyIndex.value = -1

        // Print command prompt
        val promptLine = TerminalLine(
            text = "klix@mobile:${_currentCwd.value}$ $trimmed",
            color = AccentSecondary,
            isCommand = true
        )
        _terminalLines.value = _terminalLines.value + promptLine

        // Handle internal commands
        when (trimmed) {
            "clear" -> {
                _terminalLines.value = emptyList()
                return
            }
            "help" -> {
                printHelp()
                return
            }
            "sandbox-setup" -> {
                openSetupDialog()
                return
            }
            "sandbox-status" -> {
                printStatus()
                return
            }
        }

        // Run in sandbox via provider
        lastLineHadNewline = true
        activeJob = scope.launch {
            _isExecuting.value = true
            try {
                val result = sandboxExecutionProvider.runStreamingCommand(
                    command = trimmed,
                    workingDir = _currentCwd.value
                ) { chunk, isStdout ->
                    appendOutput(chunk, isStdout)
                }

                result.newCwd?.let { newDir ->
                    if (newDir.isNotBlank() && result.isSuccess) {
                        _currentCwd.value = newDir
                    }
                }
            } catch (e: Exception) {
                val errLine = TerminalLine(text = "Execution failed: ${e.message}", color = AccentError, isError = true)
                _terminalLines.value = _terminalLines.value + errLine
            } finally {
                _isExecuting.value = false
            }
        }
    }

    private var lastLineHadNewline = true

    fun appendOutput(text: String, isStdout: Boolean) {
        if (text.isEmpty()) return

        val rawSegments = text.split('\n')
        val currentList = _terminalLines.value.toMutableList()

        for (i in rawSegments.indices) {
            val segment = rawSegments[i]
            val isLastSegment = (i == rawSegments.size - 1)

            // Trailing empty segment from text ending with '\n'
            if (isLastSegment && segment.isEmpty()) {
                lastLineHadNewline = true
                break
            }

            val parsed = parseAnsiLine(segment, isStdout)

            if (!lastLineHadNewline && currentList.isNotEmpty()) {
                val last = currentList.last()
                currentList[currentList.size - 1] = last.copy(
                    text = last.text + parsed.text
                )
            } else if (segment.isNotEmpty() || !isLastSegment) {
                currentList.add(parsed)
            }

            lastLineHadNewline = !isLastSegment
        }

        _terminalLines.value = currentList
    }

    fun sendInput(input: String) {
        if (!_isExecuting.value) {
            executeCommand(input)
            return
        }
        // Echo input into terminal lines and send to process stdin
        appendOutput(input + "\n", isStdout = true)
        sandboxExecutionProvider.sendInput(input)
    }

    fun cancelExecution() {
        sandboxExecutionProvider.cancelActiveProcess()
        activeJob?.cancel()
        _isExecuting.value = false
        appendOutput("^C (Process interrupted)\n", isStdout = false)
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    fun getPreviousHistory(): String? {
        val history = _commandHistory.value
        if (history.isEmpty()) return null
        val newIndex = if (_historyIndex.value == -1) {
            history.size - 1
        } else {
            maxOf(0, _historyIndex.value - 1)
        }
        _historyIndex.value = newIndex
        return history[newIndex]
    }

    fun getNextHistory(): String? {
        val history = _commandHistory.value
        if (history.isEmpty() || _historyIndex.value == -1) return null
        val newIndex = _historyIndex.value + 1
        return if (newIndex >= history.size) {
            _historyIndex.value = -1
            ""
        } else {
            _historyIndex.value = newIndex
            history[newIndex]
        }
    }

    private fun printHelp() {
        val helpLines = listOf(
            TerminalLine(text = "Available Klix Terminal Commands:", color = AccentPrimary),
            TerminalLine(text = "  clear            Clear the terminal screen", color = TextSecondary),
            TerminalLine(text = "  sandbox-setup    Open Alpine Linux sandbox installer & package manager", color = TextSecondary),
            TerminalLine(text = "  sandbox-status   View rootfs status and installation state", color = TextSecondary),
            TerminalLine(text = "  apk add <pkg>    Install tools (e.g. nodejs, npm, python3, git)", color = TextSecondary),
            TerminalLine(text = "  help             Display this help guide", color = TextSecondary),
            TerminalLine(text = "All standard Linux commands (ls, cat, grep, find, etc.) execute inside Alpine userland.", color = TextMuted)
        )
        _terminalLines.value = _terminalLines.value + helpLines
    }

    private fun printStatus() {
        val status = sandboxManager.status.value
        val installed = sandboxManager.isInstalled()
        val arch = sandboxManager.detectArch()
        val prootAvailable = sandboxExecutionProvider.findProotExecutable() != null
        val lines = listOf(
            TerminalLine(text = "Alpine Sandbox Status: $status", color = if (installed) AccentSecondary else AccentWarning),
            TerminalLine(text = "Sandbox Installed: $installed", color = if (installed) AccentSecondary else AccentWarning),
            TerminalLine(text = "PRoot Engine: ${if (prootAvailable) "Ready (native)" else "Not detected"}", color = if (prootAvailable) AccentSecondary else AccentWarning),
            TerminalLine(text = "Detected Architecture: $arch", color = TextSecondary),
            TerminalLine(text = "RootFS Path: ${sandboxManager.getRootfsDir().absolutePath}", color = TextMuted)
        )
        _terminalLines.value = _terminalLines.value + lines
    }

    private fun parseAnsiLine(rawLine: String, isStdout: Boolean): TerminalLine {
        // Simple ANSI escape code color stripper / parser
        var color = if (isStdout) TextPrimary else AccentError

        if (rawLine.contains("\u001B[31m") || rawLine.contains("\u001B[91m")) {
            color = AccentError
        } else if (rawLine.contains("\u001B[32m") || rawLine.contains("\u001B[92m")) {
            color = AccentSecondary
        } else if (rawLine.contains("\u001B[33m") || rawLine.contains("\u001B[93m")) {
            color = AccentWarning
        } else if (rawLine.contains("\u001B[34m") || rawLine.contains("\u001B[36m")) {
            color = AccentPrimary
        }

        // Clean ANSI escape sequences
        val cleanText = rawLine.replace(Regex("\u001B\\[[;\\d]*m"), "")
        return TerminalLine(
            text = cleanText,
            color = color,
            isError = !isStdout
        )
    }
}
