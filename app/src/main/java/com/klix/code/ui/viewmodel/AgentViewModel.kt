package com.klix.code.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.data.agent.harness.AgentHarness
import com.klix.code.data.agent.harness.HarnessStatus
import com.klix.code.data.agent.harness.PendingApproval
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.tools.WorkspaceToolExecutor
import com.klix.code.data.model.FileDiff
import com.klix.code.data.repository.WorkspaceRepository
import com.klix.code.data.repository.WorkspaceRepositoryImpl
import com.klix.code.data.security.KeyManager
import com.klix.code.data.security.KeyManagerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import com.klix.code.data.sandbox.SandboxExecutionProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WorkspaceRepository = WorkspaceRepositoryImpl(application),
    private val keyManager: KeyManager = KeyManagerImpl(application),
    private val executionProvider: SandboxExecutionProvider = SandboxExecutionProvider(application)
) : AndroidViewModel(application) {

    private val toolExecutor = WorkspaceToolExecutor(repository, executionProvider)
    val harness = AgentHarness(toolExecutor, viewModelScope)

    val messages: StateFlow<List<ChatMessage>> = harness.messages
    val status: StateFlow<HarnessStatus> = harness.status
    val statusMessage: StateFlow<String?> = harness.statusMessage
    val pendingApproval: StateFlow<PendingApproval?> = harness.pendingApproval
    val totalPromptTokens: StateFlow<Int> = harness.totalPromptTokens
    val totalCompletionTokens: StateFlow<Int> = harness.totalCompletionTokens

    private val _settings = MutableStateFlow(keyManager.getSettings())
    val settings: StateFlow<ProviderSettings> = _settings.asStateFlow()

    private val _activeDiffToReview = MutableStateFlow<FileDiff?>(null)
    val activeDiffToReview: StateFlow<FileDiff?> = _activeDiffToReview.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    init {
        syncHarnessConfig()
    }

    fun syncHarnessConfig() {
        val currentSettings = keyManager.getSettings()
        _settings.value = currentSettings
        val key = keyManager.getApiKey(currentSettings.activeProvider) ?: ""
        harness.configureProvider(currentSettings, key)
    }

    fun updateWorkspaceContext(
        workspaceUri: Uri?,
        workspaceName: String,
        activeFileName: String? = null,
        activeFileContent: String? = null
    ) {
        harness.updateContext(workspaceUri, workspaceName, activeFileName, activeFileContent)
    }

    fun sendPrompt(prompt: String) {
        syncHarnessConfig()
        harness.startTask(prompt)
    }

    fun approvePendingTool() {
        harness.approvePendingTool()
    }

    fun rejectPendingTool(reason: String = "Rejected by user.") {
        harness.rejectPendingTool(reason)
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
}
