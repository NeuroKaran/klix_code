package com.klix.code.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.data.agent.command.BuiltInSlashCommands
import com.klix.code.data.agent.command.SlashCommand
import com.klix.code.data.agent.command.SlashCommandActionType
import com.klix.code.data.agent.command.SlashCommandProcessor
import com.klix.code.data.agent.command.SlashCommandRepository
import com.klix.code.data.agent.command.SlashCommandRepositoryImpl
import com.klix.code.data.agent.harness.AgentHarness
import com.klix.code.data.agent.harness.HarnessStatus
import com.klix.code.data.agent.harness.PendingApproval
import com.klix.code.data.agent.harness.PendingQuestion
import com.klix.code.data.agent.model.DiscoveredModel
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
import com.klix.code.data.agent.session.ChatSession
import com.klix.code.data.agent.session.ChatSessionRepository
import com.klix.code.data.agent.session.ChatSessionRepositoryImpl
import com.klix.code.data.agent.session.TaggedFileItem
import com.klix.code.data.agent.tools.WorkspaceToolExecutor
import com.klix.code.data.model.FileDiff
import com.klix.code.data.repository.WorkspaceRepository
import com.klix.code.data.repository.WorkspaceRepositoryImpl
import com.klix.code.data.security.KeyManager
import com.klix.code.data.security.KeyManagerImpl
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.workspace.SandboxWorkspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.klix.code.data.workspace.SandboxWorkspaceManager
import com.klix.code.data.workspace.SandboxWorkspaceManagerImpl

class AgentViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WorkspaceRepository = WorkspaceRepositoryImpl(application),
    private val keyManager: KeyManager = KeyManagerImpl(application),
    private val executionProvider: SandboxExecutionProvider = SandboxExecutionProvider(application),
    private val sessionRepository: ChatSessionRepository = ChatSessionRepositoryImpl(application),
    private val slashCommandRepository: SlashCommandRepository = SlashCommandRepositoryImpl(application),
    val workspaceManager: SandboxWorkspaceManager = SandboxWorkspaceManagerImpl(application)
) : AndroidViewModel(application) {

    private val toolExecutor = WorkspaceToolExecutor(repository, executionProvider)
    val harness = AgentHarness(toolExecutor, viewModelScope, keyManager)

    val messages: StateFlow<List<ChatMessage>> = harness.messages
    val status: StateFlow<HarnessStatus> = harness.status
    val statusMessage: StateFlow<String?> = harness.statusMessage
    val pendingApproval: StateFlow<PendingApproval?> = harness.pendingApproval
    val pendingQuestion: StateFlow<PendingQuestion?> = harness.pendingQuestion
    val totalPromptTokens: StateFlow<Int> = harness.totalPromptTokens
    val totalCompletionTokens: StateFlow<Int> = harness.totalCompletionTokens
    val availableModels: StateFlow<List<DiscoveredModel>> = harness.availableModels
    val isFetchingModels: StateFlow<Boolean> = harness.isFetchingModels

    private val _settings = MutableStateFlow(ProviderSettings())
    val settings: StateFlow<ProviderSettings> = _settings.asStateFlow()

    private val _activeDiffToReview = MutableStateFlow<FileDiff?>(null)
    val activeDiffToReview: StateFlow<FileDiff?> = _activeDiffToReview.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Multi-chat sessions state
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String>("")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    // Tagged files/folders in the project workspace
    private val _availableWorkspaceFiles = MutableStateFlow<List<TaggedFileItem>>(emptyList())
    val availableWorkspaceFiles: StateFlow<List<TaggedFileItem>> = _availableWorkspaceFiles.asStateFlow()

    // Slash commands state
    private val _slashCommands = MutableStateFlow<List<SlashCommand>>(emptyList())
    val slashCommands: StateFlow<List<SlashCommand>> = _slashCommands.asStateFlow()

    private val _isSlashCommandManagerOpen = MutableStateFlow(false)
    val isSlashCommandManagerOpen: StateFlow<Boolean> = _isSlashCommandManagerOpen.asStateFlow()

    // Active workspace context tracked for slash command template expansions and tool execution
    private var currentWorkspace: SandboxWorkspace? = null
    var currentWorkspaceUri: Uri? = null
        private set
    var currentWorkspaceName: String = "Workspace"
        private set
    var activeFileName: String? = null
        private set
    var activeFileContent: String? = null
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workspaceManager.initialize()
            } catch (_: Exception) {}
        }
        sessionRepository.setWorkspaceDirectoryProvider { wsId ->
            workspaceManager.getWorkspaceDirectoryById(wsId)
        }
        harness.onSessionUpdated = { msgs, promptTokens, completionTokens ->
            onHarnessSessionUpdated(msgs, promptTokens, completionTokens)
        }
        syncHarnessConfig()
        loadSavedSessions()
        loadSlashCommands()
    }

    private fun getCurrentWorkspaceId(): String = currentWorkspace?.id ?: "default_workspace"

    fun onActiveWorkspaceChanged(workspace: SandboxWorkspace) {
        if (workspace.id == currentWorkspace?.id) return

        // Cancel any in-flight agent tasks before switching context
        harness.cancelTask()

        // Persist currently active session of outgoing workspace
        val prevWorkspaceId = currentWorkspace?.id
        if (prevWorkspaceId != null) {
            val activeSession = _sessions.value.find { it.id == _activeSessionId.value }
            if (activeSession != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    sessionRepository.saveSession(prevWorkspaceId, activeSession)
                }
            }
        }

        currentWorkspace = workspace
        currentWorkspaceUri = workspace.rootUri
        currentWorkspaceName = workspace.name

        // Immediately update harness context with active workspace URI and name
        harness.updateContext(
            workspaceUri = workspace.rootUri,
            workspaceName = workspace.name,
            activeFileName = activeFileName,
            activeFileContent = activeFileContent
        )

        // Load chat sessions for new workspace
        loadSavedSessions(workspace.id)
    }

    fun onWorkspaceDeleted(workspaceId: String) {
        val wasActive = (currentWorkspace?.id == workspaceId)
        if (wasActive) {
            harness.cancelTask()
            currentWorkspace = null
            currentWorkspaceUri = null
            currentWorkspaceName = "Workspace"
            _sessions.value = emptyList()
            _activeSessionId.value = ""
            harness.restoreSession(emptyList(), 0, 0)
        }
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.onWorkspaceDeleted(workspaceId)
        }
    }

    private fun loadSavedSessions(targetWorkspaceId: String? = null) {
        val wsId = targetWorkspaceId ?: getCurrentWorkspaceId()
        viewModelScope.launch(Dispatchers.IO) {
            val list = sessionRepository.getSessions(wsId)
            val savedActiveId = sessionRepository.getActiveSessionId(wsId)

            if (list.isEmpty()) {
                val initialSession = ChatSession(title = "Chat 1")
                _sessions.value = listOf(initialSession)
                _activeSessionId.value = initialSession.id
                sessionRepository.saveSession(wsId, initialSession)
                sessionRepository.setActiveSessionId(wsId, initialSession.id)
                withContext(Dispatchers.Main) {
                    harness.restoreSession(
                        initialSession.messages,
                        initialSession.promptTokens,
                        initialSession.completionTokens
                    )
                }
            } else {
                _sessions.value = list
                val target = list.find { it.id == savedActiveId } ?: list.first()
                _activeSessionId.value = target.id
                sessionRepository.setActiveSessionId(wsId, target.id)
                withContext(Dispatchers.Main) {
                    harness.restoreSession(
                        target.messages,
                        target.promptTokens,
                        target.completionTokens
                    )
                }
            }
        }
    }

    private fun onHarnessSessionUpdated(
        msgs: List<ChatMessage>,
        promptTokens: Int,
        completionTokens: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val wsId = getCurrentWorkspaceId()
            val currentId = _activeSessionId.value
            val currentList = _sessions.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == currentId }
            if (index >= 0) {
                val current = currentList[index]
                // Automatically assign descriptive title if default
                val title = if (current.title == "New Chat" || current.title.startsWith("Chat ")) {
                    val firstUserMsg = msgs.firstOrNull { it.role == MessageRole.USER }?.content
                    if (!firstUserMsg.isNullOrBlank()) {
                        firstUserMsg.lines().first().take(35).trim()
                    } else current.title
                } else current.title

                val updated = current.copy(
                    title = title,
                    updatedAt = System.currentTimeMillis(),
                    messages = msgs,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens
                )
                currentList[index] = updated
                _sessions.value = currentList
                sessionRepository.saveSession(wsId, updated)
            }
        }
    }

    fun createNewSession(title: String = "New Chat"): String {
        harness.cancelTask()
        val wsId = getCurrentWorkspaceId()
        val newSession = ChatSession(title = title)
        val updatedList = listOf(newSession) + _sessions.value
        _sessions.value = updatedList
        _activeSessionId.value = newSession.id

        harness.restoreSession(emptyList(), 0, 0)

        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.saveSession(wsId, newSession)
            sessionRepository.setActiveSessionId(wsId, newSession.id)
        }
        return newSession.id
    }

    fun switchSession(sessionId: String) {
        if (sessionId == _activeSessionId.value) return
        harness.cancelTask()
        val wsId = getCurrentWorkspaceId()

        val target = _sessions.value.find { it.id == sessionId } ?: return
        _activeSessionId.value = target.id
        harness.restoreSession(target.messages, target.promptTokens, target.completionTokens)

        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.setActiveSessionId(wsId, target.id)
        }
    }

    fun deleteSession(sessionId: String) {
        val wsId = getCurrentWorkspaceId()
        val currentList = _sessions.value.filter { it.id != sessionId }
        val wasActive = _activeSessionId.value == sessionId

        if (wasActive) {
            harness.cancelTask()
        }

        if (currentList.isEmpty()) {
            val fresh = ChatSession(title = "Chat 1")
            _sessions.value = listOf(fresh)
            _activeSessionId.value = fresh.id
            harness.restoreSession(emptyList(), 0, 0)
            viewModelScope.launch(Dispatchers.IO) {
                sessionRepository.deleteSession(wsId, sessionId)
                sessionRepository.saveSession(wsId, fresh)
                sessionRepository.setActiveSessionId(wsId, fresh.id)
            }
        } else {
            _sessions.value = currentList
            if (wasActive) {
                val nextSession = currentList.first()
                _activeSessionId.value = nextSession.id
                harness.restoreSession(nextSession.messages, nextSession.promptTokens, nextSession.completionTokens)
                viewModelScope.launch(Dispatchers.IO) {
                    sessionRepository.deleteSession(wsId, sessionId)
                    sessionRepository.setActiveSessionId(wsId, nextSession.id)
                }
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    sessionRepository.deleteSession(wsId, sessionId)
                }
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        val wsId = getCurrentWorkspaceId()
        val currentList = _sessions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            val updated = currentList[index].copy(
                title = newTitle.trim(),
                updatedAt = System.currentTimeMillis()
            )
            currentList[index] = updated
            _sessions.value = currentList
            viewModelScope.launch(Dispatchers.IO) {
                sessionRepository.saveSession(wsId, updated)
            }
        }
    }

    fun updateWorkspaceFiles(files: List<TaggedFileItem>) {
        _availableWorkspaceFiles.value = files
    }

    fun syncHarnessConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = keyManager.getSettings()
            _settings.value = currentSettings
            val key = keyManager.getApiKey(currentSettings.activeProvider) ?: ""
            harness.configureProvider(currentSettings, key)
        }
    }

    fun updateWorkspaceContext(
        workspaceUri: Uri?,
        workspaceName: String,
        activeFileName: String? = null,
        activeFileContent: String? = null
    ) {
        this.currentWorkspaceUri = workspaceUri
        this.currentWorkspaceName = workspaceName
        this.activeFileName = activeFileName
        this.activeFileContent = activeFileContent
        harness.updateContext(workspaceUri, workspaceName, activeFileName, activeFileContent)
    }

    fun loadSlashCommands() {
        viewModelScope.launch(Dispatchers.IO) {
            _slashCommands.value = slashCommandRepository.getAllCommands()
        }
    }

    fun saveCustomCommand(command: SlashCommand) {
        viewModelScope.launch(Dispatchers.IO) {
            slashCommandRepository.saveCustomCommand(command)
            loadSlashCommands()
        }
    }

    fun deleteCustomCommand(commandId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            slashCommandRepository.deleteCustomCommand(commandId)
            loadSlashCommands()
        }
    }

    fun toggleSlashCommandManager(open: Boolean? = null) {
        _isSlashCommandManagerOpen.value = open ?: !_isSlashCommandManagerOpen.value
    }

    fun sendPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.startsWith("/")) {
            handleSlashCommand(trimmed)
            return
        }
        syncHarnessConfig()
        harness.startTask(prompt)
    }

    private fun handleSlashCommand(rawPrompt: String) {
        val commandToken = rawPrompt.substring(1).substringBefore(" ").trim()
        val commandArgs = if (rawPrompt.contains(" ")) rawPrompt.substringAfter(" ").trim() else ""

        val command = _slashCommands.value.find { it.trigger.equals(commandToken, ignoreCase = true) }
        if (command == null) {
            harness.postAssistantInfo(
                "Unknown slash command `/$commandToken`.\n\n" +
                "Type `/help` to see all available commands, or `/commands` to create your own custom commands."
            )
            return
        }

        when (command.actionType) {
            SlashCommandActionType.CLEAR_CHAT -> {
                clearChat()
            }
            SlashCommandActionType.NEW_CHAT -> {
                createNewSession()
            }
            SlashCommandActionType.SETTINGS -> {
                toggleSettings()
            }
            SlashCommandActionType.COMMANDS -> {
                toggleSlashCommandManager(true)
            }
            SlashCommandActionType.HELP -> {
                displayHelpMessage()
            }
            SlashCommandActionType.PROMPT -> {
                val expandedPrompt = SlashCommandProcessor.resolveTemplate(
                    template = command.template,
                    input = commandArgs,
                    fileName = activeFileName,
                    fileContent = activeFileContent
                )
                syncHarnessConfig()
                harness.startTask(userPrompt = expandedPrompt, command = "/${command.trigger}")
            }
        }
    }

    private fun displayHelpMessage() {
        val builtIns = _slashCommands.value.filter { it.isBuiltIn }
        val customs = _slashCommands.value.filter { !it.isBuiltIn }

        val sb = StringBuilder()
        sb.append("### Available Slash Commands\n\n")
        sb.append("**Built-in Commands:**\n")
        for (cmd in builtIns) {
            sb.append("- `/${cmd.trigger}` — ${cmd.description}\n")
        }

        if (customs.isNotEmpty()) {
            sb.append("\n**Custom Commands:**\n")
            for (cmd in customs) {
                sb.append("- `/${cmd.trigger}` — ${cmd.description}\n")
            }
        }

        sb.append("\n**Template Placeholders for Custom Commands:**\n")
        sb.append("- `{input}`: Arguments or query typed after the command\n")
        sb.append("- `{file}`: Active editor file name\n")
        sb.append("- `{code}`: Active editor file contents\n\n")
        sb.append("Type `/commands` or tap the **/** button to create and manage your custom slash commands!")

        harness.postAssistantInfo(sb.toString())
    }

    fun approvePendingTool(selectedAnswers: List<String>? = null) {
        harness.approvePendingTool(selectedAnswers)
    }

    fun rejectPendingTool(reason: String = "Rejected by user.") {
        harness.rejectPendingTool(reason)
    }

    fun answerQuestion(answers: List<String>) {
        harness.answerQuestion(answers)
    }

    fun rejectQuestion(reason: String = "Declined by user.") {
        harness.rejectPendingQuestion(reason)
    }

    fun compactHistory() {
        harness.compactHistory()
    }

    fun cancelTask() {
        harness.cancelTask()
    }

    fun clearChat() {
        harness.clearHistory()
    }

    fun openReviewDiff(diff: FileDiff) {
        _activeDiffToReview.value = diff
    }

    fun closeReviewDiff() {
        _activeDiffToReview.value = null
    }

    fun toggleSettings() {
        _isSettingsOpen.value = !_isSettingsOpen.value
    }

    fun saveApiKey(provider: ModelProvider, key: String?) {
        keyManager.setApiKey(provider, key)
        syncHarnessConfig()
    }

    fun getApiKey(provider: ModelProvider): String? {
        return keyManager.getApiKey(provider)
    }

    fun updateSettings(newSettings: ProviderSettings) {
        keyManager.updateSettings(newSettings)
        syncHarnessConfig()
    }

    fun fetchModelsForProvider(
        provider: ModelProvider,
        apiKey: String? = null,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val key = apiKey ?: keyManager.getApiKey(provider) ?: ""
            harness.fetchModelsForProvider(
                provider = provider,
                apiKey = key,
                forceRefresh = forceRefresh
            )
        }
    }
}
