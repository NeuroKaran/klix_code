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
    val timestamp: Long = System.currentTimeMillis(),
    val isPrompt: Boolean = false
)

data class TerminalTab(
    val id: String,
    val title: String,
    val isExecuting: Boolean = false,
    val currentCwd: String = "/workspace"
)

internal class TerminalSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    val executionProvider: SandboxExecutionProvider,
    initialCwd: String = "/workspace"
) {
    val lines: MutableList<TerminalLine> = mutableListOf()
    val commandHistory: MutableList<String> = mutableListOf()
    var isExecuting: Boolean = false
    var currentCwd: String = initialCwd
    var historyIndex: Int = -1
    var lastLineHadNewline: Boolean = true
    var activeJob: Job? = null
    var inputDraft: String = ""
}

class TerminalViewModel @JvmOverloads constructor(
    application: Application,
    val sandboxExecutionProvider: SandboxExecutionProvider = SandboxExecutionProvider(application),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AndroidViewModel(application) {

    val sandboxManager: SandboxManager = sandboxExecutionProvider.sandboxManager

    private val sessions = mutableListOf<TerminalSession>()

    private val _tabs = MutableStateFlow<List<TerminalTab>>(emptyList())
    val tabs: StateFlow<List<TerminalTab>> = _tabs.asStateFlow()

    private val _activeTerminalId = MutableStateFlow<String>("")
    val activeTerminalId: StateFlow<String> = _activeTerminalId.asStateFlow()

    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _currentCwd = MutableStateFlow("/workspace")
    val currentCwd: StateFlow<String> = _currentCwd.asStateFlow()

    private val _showSetupDialog = MutableStateFlow(false)
    val showSetupDialog: StateFlow<Boolean> = _showSetupDialog.asStateFlow()

    init {
        val initialSession = TerminalSession(
            title = "Terminal 1",
            executionProvider = sandboxExecutionProvider,
            initialCwd = "/workspace"
        )
        sessions.add(initialSession)
        _activeTerminalId.value = initialSession.id
        printBanner(initialSession)
        syncActiveState(initialSession)
        updateTabsState()
    }

    private fun getActiveSession(): TerminalSession? {
        val currentId = _activeTerminalId.value
        return sessions.find { it.id == currentId } ?: sessions.firstOrNull()
    }

    private fun syncActiveState(session: TerminalSession) {
        synchronized(session) {
            _terminalLines.value = session.lines.toList()
            _commandHistory.value = session.commandHistory.toList()
            _isExecuting.value = session.isExecuting
            _currentCwd.value = session.currentCwd
        }
    }

    private fun updateTabsState() {
        _tabs.value = sessions.map { session ->
            TerminalTab(
                id = session.id,
                title = session.title,
                isExecuting = session.isExecuting,
                currentCwd = session.currentCwd
            )
        }
    }

    fun addNewTerminal(title: String? = null, workingDir: String? = null): String {
        val sessionTitle = title ?: run {
            var count = 1
            while (sessions.any { it.title == "Terminal $count" }) {
                count++
            }
            "Terminal $count"
        }
        val defaultCwd = workingDir ?: getActiveSession()?.currentCwd ?: "/workspace"
        val newProvider = SandboxExecutionProvider(getApplication(), sandboxManager)
        val session = TerminalSession(
            title = sessionTitle,
            executionProvider = newProvider,
            initialCwd = defaultCwd
        )
        printBanner(session)
        sessions.add(session)
        selectTerminal(session.id)
        return session.id
    }

    fun closeTerminal(id: String) {
        val sessionToClose = sessions.find { it.id == id } ?: return
        sessionToClose.executionProvider.cancelActiveProcess()
        sessionToClose.activeJob?.cancel()

        val wasActive = (_activeTerminalId.value == id)
        val index = sessions.indexOf(sessionToClose)
        sessions.remove(sessionToClose)

        if (sessions.isEmpty()) {
            val fresh = TerminalSession(
                title = "Terminal 1",
                executionProvider = SandboxExecutionProvider(getApplication(), sandboxManager),
                initialCwd = "/workspace"
            )
            printBanner(fresh)
            sessions.add(fresh)
            selectTerminal(fresh.id)
        } else if (wasActive) {
            val newIndex = (index - 1).coerceAtLeast(0)
            selectTerminal(sessions[newIndex].id)
        } else {
            updateTabsState()
        }
    }

    fun selectTerminal(id: String) {
        val session = sessions.find { it.id == id } ?: return
        _activeTerminalId.value = session.id
        syncActiveState(session)
        updateTabsState()
    }

    fun renameTerminal(id: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        val session = sessions.find { it.id == id } ?: return
        session.title = trimmed
        updateTabsState()
    }

    fun getInputDraft(terminalId: String? = null): String {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return ""
        return session.inputDraft
    }

    fun setInputDraft(draft: String, terminalId: String? = null) {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        session.inputDraft = draft
    }

    private fun printBanner(session: TerminalSession) {
        val isReady = sandboxManager.isInstalled()
        val banner = mutableListOf(
            TerminalLine(text = "KlixCode Linux Terminal [Alpine Sandbox v1] - ${session.title}", color = AccentPrimary)
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
        val promptLine = TerminalLine(
            text = "klix@mobile:${session.currentCwd}$",
            color = AccentSecondary,
            isCommand = true,
            isPrompt = true
        )
        synchronized(session) {
            session.lines.clear()
            session.lines.addAll(banner)
            session.lines.add(promptLine)
            session.lastLineHadNewline = true
        }
        if (session.id == _activeTerminalId.value) {
            _terminalLines.value = session.lines.toList()
        }
    }

    fun setWorkingDirectory(path: String, terminalId: String? = null) {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        session.currentCwd = path
        if (session.id == _activeTerminalId.value) {
            _currentCwd.value = path
        }
        synchronized(session) {
            if (session.lines.isNotEmpty() && session.lines.last().isPrompt) {
                session.lines[session.lines.size - 1] = TerminalLine(
                    text = "klix@mobile:${path}$",
                    color = AccentSecondary,
                    isCommand = true,
                    isPrompt = true
                )
            }
        }
        if (session.id == _activeTerminalId.value) {
            _terminalLines.value = session.lines.toList()
        }
        updateTabsState()
    }

    fun openSetupDialog() {
        _showSetupDialog.value = true
    }

    fun closeSetupDialog() {
        _showSetupDialog.value = false
    }

    fun executeCommand(command: String, terminalId: String? = null) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return

        // Add to history
        synchronized(session) {
            if (session.commandHistory.isEmpty() || session.commandHistory.last() != trimmed) {
                session.commandHistory.add(trimmed)
            }
            session.historyIndex = -1
        }
        if (session.id == _activeTerminalId.value) {
            _commandHistory.value = session.commandHistory.toList()
        }

        // Print command prompt
        val promptLine = TerminalLine(
            text = "klix@mobile:${session.currentCwd}$ $trimmed",
            color = AccentSecondary,
            isCommand = true,
            isPrompt = false
        )
        synchronized(session) {
            if (session.lines.isNotEmpty() && session.lines.last().isPrompt) {
                session.lines[session.lines.size - 1] = promptLine
            } else {
                session.lines.add(promptLine)
            }
        }
        if (session.id == _activeTerminalId.value) {
            _terminalLines.value = session.lines.toList()
        }

        // Handle internal commands
        when (trimmed) {
            "clear" -> {
                clearTerminal(session.id)
                return
            }
            "help" -> {
                printHelp(session)
                return
            }
            "sandbox-setup" -> {
                openSetupDialog()
                appendIdlePrompt(session)
                return
            }
            "sandbox-status" -> {
                printStatus(session)
                return
            }
        }

        // Run in sandbox via provider
        session.lastLineHadNewline = true
        session.activeJob = scope.launch {
            setSessionExecuting(session, true)
            try {
                val result = session.executionProvider.runStreamingCommand(
                    command = trimmed,
                    workingDir = session.currentCwd
                ) { chunk, isStdout ->
                    appendSessionOutput(session, chunk, isStdout)
                }

                result.newCwd?.let { newDir ->
                    if (newDir.isNotBlank() && result.isSuccess) {
                        session.currentCwd = newDir
                        if (session.id == _activeTerminalId.value) {
                            _currentCwd.value = newDir
                        }
                        updateTabsState()
                    }
                }
            } catch (e: Exception) {
                val errLine = TerminalLine(text = "Execution failed: ${e.message}", color = AccentError, isError = true)
                appendTerminalLine(session, errLine)
            } finally {
                setSessionExecuting(session, false)
                appendIdlePrompt(session)
            }
        }
    }

    private fun setSessionExecuting(session: TerminalSession, executing: Boolean) {
        session.isExecuting = executing
        if (session.id == _activeTerminalId.value) {
            _isExecuting.value = executing
        }
        updateTabsState()
    }

    private fun appendTerminalLine(session: TerminalSession, line: TerminalLine) {
        synchronized(session) {
            session.lines.add(line)
        }
        if (session.id == _activeTerminalId.value) {
            _terminalLines.value = session.lines.toList()
        }
    }

    fun appendOutput(text: String, isStdout: Boolean, terminalId: String? = null) {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        appendSessionOutput(session, text, isStdout)
    }

    private fun appendSessionOutput(session: TerminalSession, text: String, isStdout: Boolean) {
        if (text.isEmpty()) return

        val rawSegments = text.split('\n')
        synchronized(session) {
            for (i in rawSegments.indices) {
                val segment = rawSegments[i]
                val isLastSegment = (i == rawSegments.size - 1)

                // Trailing empty segment from text ending with '\n'
                if (isLastSegment && segment.isEmpty()) {
                    session.lastLineHadNewline = true
                    break
                }

                val parsed = parseAnsiLine(segment, isStdout)

                if (!session.lastLineHadNewline && session.lines.isNotEmpty()) {
                    val last = session.lines.last()
                    session.lines[session.lines.size - 1] = last.copy(
                        text = last.text + parsed.text
                    )
                } else if (segment.isNotEmpty() || !isLastSegment) {
                    session.lines.add(parsed)
                }

                session.lastLineHadNewline = !isLastSegment
            }
        }

        if (session.id == _activeTerminalId.value) {
            _terminalLines.value = session.lines.toList()
        }
    }

    fun sendInput(input: String, terminalId: String? = null) {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        if (!session.isExecuting) {
            executeCommand(input, session.id)
            return
        }
        val trimmed = input.trim()
        if (trimmed == "^C" || trimmed == "Ctrl+C" || trimmed == "ctrl+c" || input == "\u0003") {
            cancelExecution(session.id)
            return
        }
        // Echo input into terminal lines and send to process stdin
        appendSessionOutput(session, input + "\n", isStdout = true)
        session.executionProvider.sendInput(input)
    }

    fun cancelExecution(terminalId: String? = null) {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        session.executionProvider.cancelActiveProcess()
        session.activeJob?.cancel()
        setSessionExecuting(session, false)
        appendSessionOutput(session, "^C (Process interrupted)\n", isStdout = false)
        appendIdlePrompt(session)
    }

    fun clearTerminal(terminalId: String? = null) {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        val promptLine = TerminalLine(
            text = "klix@mobile:${session.currentCwd}$",
            color = AccentSecondary,
            isCommand = true,
            isPrompt = true
        )
        synchronized(session) {
            session.lines.clear()
            session.lines.add(promptLine)
            session.lastLineHadNewline = true
        }
        if (session.id == _activeTerminalId.value) {
            _terminalLines.value = session.lines.toList()
        }
    }

    private fun appendIdlePrompt(session: TerminalSession) {
        val idlePrompt = TerminalLine(
            text = "klix@mobile:${session.currentCwd}$",
            color = AccentSecondary,
            isCommand = true,
            isPrompt = true
        )
        synchronized(session) {
            if (session.lines.isEmpty() || !session.lines.last().isPrompt) {
                session.lines.add(idlePrompt)
                session.lastLineHadNewline = true
            }
        }
        if (session.id == _activeTerminalId.value) {
            _terminalLines.value = session.lines.toList()
        }
    }

    fun getPreviousHistory(terminalId: String? = null): String? {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return null
        val history = session.commandHistory
        if (history.isEmpty()) return null
        val newIndex = if (session.historyIndex == -1) {
            history.size - 1
        } else {
            maxOf(0, session.historyIndex - 1)
        }
        session.historyIndex = newIndex
        return history[newIndex]
    }

    fun getNextHistory(terminalId: String? = null): String? {
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return null
        val history = session.commandHistory
        if (history.isEmpty() || session.historyIndex == -1) return null
        val newIndex = session.historyIndex + 1
        return if (newIndex >= history.size) {
            session.historyIndex = -1
            ""
        } else {
            session.historyIndex = newIndex
            history[newIndex]
        }
    }

    private fun printHelp(session: TerminalSession) {
        val helpLines = listOf(
            TerminalLine(text = "Available Klix Terminal Commands:", color = AccentPrimary),
            TerminalLine(text = "  clear            Clear the terminal screen", color = TextSecondary),
            TerminalLine(text = "  sandbox-setup    Open Alpine Linux sandbox installer & package manager", color = TextSecondary),
            TerminalLine(text = "  sandbox-status   View rootfs status and installation state", color = TextSecondary),
            TerminalLine(text = "  apk add <pkg>    Install tools (e.g. nodejs, npm, python3, git)", color = TextSecondary),
            TerminalLine(text = "  help             Display this help guide", color = TextSecondary),
            TerminalLine(text = "All standard Linux commands (ls, cat, grep, find, etc.) execute inside Alpine userland.", color = TextMuted)
        )
        synchronized(session) {
            session.lines.addAll(helpLines)
        }
        appendIdlePrompt(session)
    }

    private fun printStatus(session: TerminalSession) {
        val status = sandboxManager.status.value
        val installed = sandboxManager.isInstalled()
        val arch = sandboxManager.detectArch()
        val prootAvailable = session.executionProvider.findProotExecutable() != null
        val lines = listOf(
            TerminalLine(text = "Alpine Sandbox Status: $status", color = if (installed) AccentSecondary else AccentWarning),
            TerminalLine(text = "Sandbox Installed: $installed", color = if (installed) AccentSecondary else AccentWarning),
            TerminalLine(text = "PRoot Engine: ${if (prootAvailable) "Ready (native)" else "Not detected"}", color = if (prootAvailable) AccentSecondary else AccentWarning),
            TerminalLine(text = "Detected Architecture: $arch", color = TextSecondary),
            TerminalLine(text = "RootFS Path: ${sandboxManager.getRootfsDir().absolutePath}", color = TextMuted)
        )
        synchronized(session) {
            session.lines.addAll(lines)
        }
        appendIdlePrompt(session)
    }

    private fun parseAnsiLine(rawLine: String, isStdout: Boolean): TerminalLine {
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

        val cleanText = rawLine.replace(Regex("\u001B\\[[;\\d]*m"), "")
        return TerminalLine(
            text = cleanText,
            color = color,
            isError = !isStdout
        )
    }
}
