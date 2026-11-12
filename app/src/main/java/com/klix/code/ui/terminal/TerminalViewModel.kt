package com.klix.code.ui.terminal

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.workspace.SandboxWorkspace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal object TerminalColors {
    val Primary = Color(0xFF3B82F6)
    val Secondary = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF6E7681)
}

data class TerminalLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val color: Color = Color.Unspecified,
    val isCommand: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isPrompt: Boolean = false
)

data class TerminalTab(
    val id: String,
    val title: String,
    val isExecuting: Boolean = false,
    val currentCwd: String = "/workspaces/default"
)

internal class TerminalSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    val executionProvider: SandboxExecutionProvider,
    initialCwd: String = "/workspaces/default"
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

    private val workspaceSessionsMap = mutableMapOf<String, MutableList<TerminalSession>>()
    private val workspaceActiveTabIdMap = mutableMapOf<String, String>()
    private var currentWorkspace: SandboxWorkspace? = null

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

    private val _currentCwd = MutableStateFlow("/workspaces/default")
    val currentCwd: StateFlow<String> = _currentCwd.asStateFlow()

    private val _showSetupDialog = MutableStateFlow(false)
    val showSetupDialog: StateFlow<Boolean> = _showSetupDialog.asStateFlow()

    init {
        val initialSession = TerminalSession(
            title = "Terminal 1",
            executionProvider = sandboxExecutionProvider,
            initialCwd = "/workspaces/default"
        )
        workspaceSessionsMap["default_workspace"] = mutableListOf(initialSession)
        workspaceActiveTabIdMap["default_workspace"] = initialSession.id
        _activeTerminalId.value = initialSession.id
        printBanner(initialSession, "Default Workspace")
        syncActiveState(initialSession)
        updateTabsState()
    }

    private fun getCurrentSessions(): MutableList<TerminalSession> {
        val wsId = currentWorkspace?.id ?: "default_workspace"
        return workspaceSessionsMap.getOrPut(wsId) {
            val defaultCwd = currentWorkspace?.relativePath ?: "/workspaces/default"
            val initialSession = TerminalSession(
                title = "Terminal 1",
                executionProvider = SandboxExecutionProvider(getApplication(), sandboxManager),
                initialCwd = defaultCwd
            )
            printBanner(initialSession, currentWorkspace?.name)
            mutableListOf(initialSession)
        }
    }

    private fun getActiveSession(): TerminalSession? {
        val sessions = getCurrentSessions()
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
        val sessions = getCurrentSessions()
        _tabs.value = sessions.map { session ->
            TerminalTab(
                id = session.id,
                title = session.title,
                isExecuting = session.isExecuting,
                currentCwd = session.currentCwd
            )
        }
    }

    fun onActiveWorkspaceChanged(workspace: SandboxWorkspace) {
        if (workspace.id == currentWorkspace?.id) return

        val prevWorkspace = currentWorkspace
        if (prevWorkspace != null) {
            val activeId = _activeTerminalId.value
            if (activeId.isNotEmpty()) {
                workspaceActiveTabIdMap[prevWorkspace.id] = activeId
            }
        }

        currentWorkspace = workspace
        val wsId = workspace.id
        val sessions = workspaceSessionsMap.getOrPut(wsId) {
            val initialSession = TerminalSession(
                title = "Terminal 1",
                executionProvider = SandboxExecutionProvider(getApplication(), sandboxManager),
                initialCwd = workspace.relativePath
            )
            printBanner(initialSession, workspace.name)
            mutableListOf(initialSession)
        }

        val lastActiveId = workspaceActiveTabIdMap[wsId]
        val activeSession = sessions.find { it.id == lastActiveId } ?: sessions.first()
        _activeTerminalId.value = activeSession.id
        workspaceActiveTabIdMap[wsId] = activeSession.id

        syncActiveState(activeSession)
        updateTabsState()
    }

    fun onWorkspaceDeleted(workspaceId: String) {
        val removedSessions = workspaceSessionsMap.remove(workspaceId)
        removedSessions?.forEach { session ->
            session.executionProvider.cancelActiveProcess()
            session.activeJob?.cancel()
        }
        workspaceActiveTabIdMap.remove(workspaceId)
        if (currentWorkspace?.id == workspaceId) {
            currentWorkspace = null
            val remainingWsId = workspaceSessionsMap.keys.firstOrNull()
            if (remainingWsId != null) {
                val sessions = workspaceSessionsMap[remainingWsId]
                val activeSession = sessions?.find { it.id == workspaceActiveTabIdMap[remainingWsId] } ?: sessions?.firstOrNull()
                if (activeSession != null) {
                    _activeTerminalId.value = activeSession.id
                    syncActiveState(activeSession)
                }
            } else {
                _tabs.value = emptyList()
                _activeTerminalId.value = ""
                _terminalLines.value = emptyList()
            }
            updateTabsState()
        }
    }

    fun addNewTerminal(title: String? = null, workingDir: String? = null): String {
        val sessions = getCurrentSessions()
        val sessionTitle = title ?: run {
            var count = 1
            while (sessions.any { it.title == "Terminal $count" }) {
                count++
            }
            "Terminal $count"
        }
        val defaultCwd = workingDir ?: currentWorkspace?.relativePath ?: getActiveSession()?.currentCwd ?: "/workspaces/default"
        val newProvider = SandboxExecutionProvider(getApplication(), sandboxManager)
        val session = TerminalSession(
            title = sessionTitle,
            executionProvider = newProvider,
            initialCwd = defaultCwd
        )
        printBanner(session, currentWorkspace?.name)
        sessions.add(session)
        selectTerminal(session.id)
        return session.id
    }

    fun closeTerminal(id: String) {
        val sessions = getCurrentSessions()
        val sessionToClose = sessions.find { it.id == id } ?: return
        sessionToClose.executionProvider.cancelActiveProcess()
        sessionToClose.activeJob?.cancel()

        val wasActive = (_activeTerminalId.value == id)
        val index = sessions.indexOf(sessionToClose)
        sessions.remove(sessionToClose)

        val defaultCwd = currentWorkspace?.relativePath ?: "/workspaces/default"
        if (sessions.isEmpty()) {
            val fresh = TerminalSession(
                title = "Terminal 1",
                executionProvider = SandboxExecutionProvider(getApplication(), sandboxManager),
                initialCwd = defaultCwd
            )
            printBanner(fresh, currentWorkspace?.name)
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
        val sessions = getCurrentSessions()
        val session = sessions.find { it.id == id } ?: return
        _activeTerminalId.value = session.id
        val wsId = currentWorkspace?.id ?: "default_workspace"
        workspaceActiveTabIdMap[wsId] = session.id
        syncActiveState(session)
        updateTabsState()
    }

    fun renameTerminal(id: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        val sessions = getCurrentSessions()
        val session = sessions.find { it.id == id } ?: return
        session.title = trimmed
        updateTabsState()
    }

    fun getInputDraft(terminalId: String? = null): String {
        val sessions = getCurrentSessions()
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return ""
        return session.inputDraft
    }

    fun setInputDraft(draft: String, terminalId: String? = null) {
        val sessions = getCurrentSessions()
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        session.inputDraft = draft
    }

    private fun printBanner(session: TerminalSession, workspaceName: String? = null) {
        val isReady = sandboxManager.isInstalled()
        val wsName = workspaceName ?: currentWorkspace?.name
        val titleSuffix = if (!wsName.isNullOrBlank()) " - $wsName" else ""
        val banner = mutableListOf(
            TerminalLine(text = "KlixCode Linux Terminal [Alpine Sandbox]$titleSuffix (${session.title})", color = TerminalColors.Primary)
        )
        if (!isReady) {
            banner.add(
                TerminalLine(
                    text = "Sandbox not configured. Tap ⚙ or type 'sandbox-setup' to install Alpine Linux.",
                    color = TerminalColors.Warning
                )
            )
        } else {
            banner.add(
                TerminalLine(
                    text = "Alpine Linux sandbox active. Type 'help' for shortcuts or 'sandbox-setup' to manage tools.",
                    color = TerminalColors.Secondary
                )
            )
        }
        banner.add(
            TerminalLine(text = "--------------------------------------------------------", color = TerminalColors.TextSecondary)
        )
        val promptLine = TerminalLine(
            text = "klix@mobile:${session.currentCwd}$",
            color = TerminalColors.Secondary,
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
        val sessions = getCurrentSessions()
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        session.currentCwd = path
        if (session.id == _activeTerminalId.value) {
            _currentCwd.value = path
        }
        synchronized(session) {
            if (session.lines.isNotEmpty() && session.lines.last().isPrompt) {
                session.lines[session.lines.size - 1] = TerminalLine(
                    text = "klix@mobile:${path}$",
                    color = TerminalColors.Secondary,
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

        val sessions = getCurrentSessions()
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
            color = TerminalColors.Secondary,
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
            "exit" -> {
                val sessions = getCurrentSessions()
                if (sessions.size > 1 && sessions.indexOf(session) > 0) {
                    closeTerminal(session.id)
                } else {
                    clearTerminal(session.id)
                }
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
                val errLine = TerminalLine(text = "Execution failed: ${e.message}", color = TerminalColors.Error, isError = true)
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
        val sessions = getCurrentSessions()
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
        val sessions = getCurrentSessions()
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
        val sessions = getCurrentSessions()
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        session.executionProvider.cancelActiveProcess()
        session.activeJob?.cancel()
        setSessionExecuting(session, false)
        appendSessionOutput(session, "^C (Process interrupted)\n", isStdout = false)
        appendIdlePrompt(session)
    }

    fun clearTerminal(terminalId: String? = null) {
        val sessions = getCurrentSessions()
        val session = (if (terminalId != null) sessions.find { it.id == terminalId } else getActiveSession()) ?: return
        val promptLine = TerminalLine(
            text = "klix@mobile:${session.currentCwd}$",
            color = TerminalColors.Secondary,
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
            color = TerminalColors.Secondary,
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
        val sessions = getCurrentSessions()
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
        val sessions = getCurrentSessions()
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
            TerminalLine(text = "Available Klix Terminal Commands:", color = TerminalColors.Primary),
            TerminalLine(text = "  clear            Clear the terminal screen", color = TerminalColors.TextSecondary),
            TerminalLine(text = "  sandbox-setup    Open Alpine Linux sandbox installer & package manager", color = TerminalColors.TextSecondary),
            TerminalLine(text = "  sandbox-status   View rootfs status and installation state", color = TerminalColors.TextSecondary),
            TerminalLine(text = "  apk add <pkg>    Install tools (e.g. nodejs, npm, python3, git)", color = TerminalColors.TextSecondary),
            TerminalLine(text = "  help             Display this help guide", color = TerminalColors.TextSecondary),
            TerminalLine(text = "All standard Linux commands (ls, cat, grep, find, etc.) execute inside Alpine userland.", color = TerminalColors.TextMuted)
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
            TerminalLine(text = "Alpine Sandbox Status: $status", color = if (installed) TerminalColors.Secondary else TerminalColors.Warning),
            TerminalLine(text = "Sandbox Installed: $installed", color = if (installed) TerminalColors.Secondary else TerminalColors.Warning),
            TerminalLine(text = "PRoot Engine: ${if (prootAvailable) "Ready (native)" else "Not detected"}", color = if (prootAvailable) TerminalColors.Secondary else TerminalColors.Warning),
            TerminalLine(text = "Detected Architecture: $arch", color = TerminalColors.TextSecondary),
            TerminalLine(text = "RootFS Path: ${sandboxManager.getRootfsDir().absolutePath}", color = TerminalColors.TextMuted)
        )
        synchronized(session) {
            session.lines.addAll(lines)
        }
        appendIdlePrompt(session)
    }

    private fun parseAnsiLine(rawLine: String, isStdout: Boolean): TerminalLine {
        var color = if (isStdout) Color.Unspecified else TerminalColors.Error

        if (rawLine.contains("\u001B[31m") || rawLine.contains("\u001B[91m")) {
            color = TerminalColors.Error
        } else if (rawLine.contains("\u001B[32m") || rawLine.contains("\u001B[92m")) {
            color = TerminalColors.Secondary
        } else if (rawLine.contains("\u001B[33m") || rawLine.contains("\u001B[93m")) {
            color = TerminalColors.Warning
        } else if (rawLine.contains("\u001B[34m") || rawLine.contains("\u001B[36m")) {
            color = TerminalColors.Primary
        }

        val cleanText = rawLine.replace(Regex("\u001B\\[[;\\d]*m"), "")
        return TerminalLine(
            text = cleanText,
            color = color,
            isError = !isStdout
        )
    }
}
