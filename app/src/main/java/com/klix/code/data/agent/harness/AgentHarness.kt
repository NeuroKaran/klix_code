package com.klix.code.data.agent.harness

import android.net.Uri
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
import com.klix.code.data.agent.provider.ModelProviderAdapter
import com.klix.code.data.agent.provider.ProviderAdapterFactory
import com.klix.code.data.agent.provider.ProviderTurnRequest
import com.klix.code.data.agent.tools.AgentTools
import com.klix.code.data.agent.tools.ToolCallRequest
import com.klix.code.data.agent.tools.ToolCallResult
import com.klix.code.data.agent.tools.WorkspaceToolExecutor
import com.klix.code.data.model.FileDiff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HarnessStatus {
    IDLE,
    THINKING,
    AWAITING_APPROVAL,
    EXECUTING_TOOL,
    COMPLETED,
    ERROR
}

data class PendingApproval(
    val toolCall: ToolCallRequest,
    val diff: FileDiff? = null,
    val promptMessage: String
)

class AgentHarness(
    private val toolExecutor: WorkspaceToolExecutor,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow(HarnessStatus.IDLE)
    val status: StateFlow<HarnessStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("Ready")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _pendingApproval = MutableStateFlow<PendingApproval?>(null)
    val pendingApproval: StateFlow<PendingApproval?> = _pendingApproval.asStateFlow()

    private val _totalPromptTokens = MutableStateFlow(0)
    val totalPromptTokens: StateFlow<Int> = _totalPromptTokens.asStateFlow()

    private val _totalCompletionTokens = MutableStateFlow(0)
    val totalCompletionTokens: StateFlow<Int> = _totalCompletionTokens.asStateFlow()

    private var activeJob: Job? = null
    private var currentWorkspaceUri: Uri? = null
    private var currentWorkspaceName: String = "Workspace"
    private var activeFileName: String? = null
    private var activeFileContent: String? = null

    private var currentSettings: ProviderSettings = ProviderSettings()
    private var currentApiKey: String = ""

    fun updateContext(
        workspaceUri: Uri?,
        workspaceName: String,
        activeFileName: String? = null,
        activeFileContent: String? = null
    ) {
        this.currentWorkspaceUri = workspaceUri
        this.currentWorkspaceName = workspaceName
        this.activeFileName = activeFileName
        this.activeFileContent = activeFileContent
    }

    fun configureProvider(settings: ProviderSettings, apiKey: String) {
        this.currentSettings = settings
        this.currentApiKey = apiKey
    }

    fun startTask(userPrompt: String) {
        if (userPrompt.isBlank()) return
        if (currentApiKey.isBlank()) {
            _status.value = HarnessStatus.ERROR
            _statusMessage.value = "No API key configured for ${currentSettings.activeProvider.displayName}. Please configure it in Agent Settings."
            val errorMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "API key missing for ${currentSettings.activeProvider.displayName}. Please open Agent Settings (gear icon) and add your API key."
            )
            _messages.value = _messages.value + errorMsg
            return
        }

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = userPrompt
        )
        _messages.value = _messages.value + userMessage

        runOrchestrationLoop()
    }

    fun approvePendingTool() {
        val pending = _pendingApproval.value ?: return
        _pendingApproval.value = null

        activeJob = scope.launch {
            _status.value = HarnessStatus.EXECUTING_TOOL
            _statusMessage.value = "Executing ${pending.toolCall.name}..."

            // Execute tool with dryRunWrite = false to commit changes
            val result = toolExecutor.execute(
                rootTreeUri = currentWorkspaceUri,
                toolCall = pending.toolCall,
                dryRunWrite = false
            )

            recordToolResult(pending.toolCall, result)
            runOrchestrationLoop()
        }
    }

    fun rejectPendingTool(reason: String = "User rejected tool execution.") {
        val pending = _pendingApproval.value ?: return
        _pendingApproval.value = null

        val result = ToolCallResult(
            toolCallId = pending.toolCall.id,
            name = pending.toolCall.name,
            isSuccess = false,
            output = "",
            error = reason
        )

        recordToolResult(pending.toolCall, result)
        runOrchestrationLoop()
    }

    fun cancelTask() {
        activeJob?.cancel()
        _status.value = HarnessStatus.IDLE
        _statusMessage.value = "Task cancelled by user."
        _pendingApproval.value = null
    }

    fun clearHistory() {
        cancelTask()
        _messages.value = emptyList()
        _totalPromptTokens.value = 0
        _totalCompletionTokens.value = 0
        _status.value = HarnessStatus.IDLE
        _statusMessage.value = "Chat reset."
    }

    private fun runOrchestrationLoop() {
        activeJob = scope.launch {
            try {
                _status.value = HarnessStatus.THINKING
                _statusMessage.value = "Thinking..."

                val adapter: ModelProviderAdapter = ProviderAdapterFactory.getAdapter(currentSettings.activeProvider)
                val systemPrompt = buildSystemPrompt()

                val turnRequest = ProviderTurnRequest(
                    systemPrompt = systemPrompt,
                    messages = _messages.value,
                    tools = AgentTools.ALL_TOOLS,
                    model = currentSettings.activeModel,
                    apiKey = currentApiKey,
                    customEndpoint = if (currentSettings.activeProvider == ModelProvider.CUSTOM_OPENAI_COMPATIBLE) {
                        currentSettings.customEndpointUrl
                    } else null,
                    temperature = currentSettings.temperature
                )

                val response = adapter.sendTurn(turnRequest)

                _totalPromptTokens.value += response.promptTokens
                _totalCompletionTokens.value += response.completionTokens

                // Record Assistant Turn
                val assistantMsg = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = response.textResponse ?: "",
                    toolCalls = response.toolCalls
                )
                _messages.value = _messages.value + assistantMsg

                if (response.toolCalls.isEmpty()) {
                    _status.value = HarnessStatus.COMPLETED
                    _statusMessage.value = "Done"
                    return@launch
                }

                // Handle Tool Calls sequentially
                handleToolCalls(response.toolCalls)

            } catch (e: Exception) {
                _status.value = HarnessStatus.ERROR
                _statusMessage.value = "Error: ${e.message}"
                val errorMsg = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "Encountered an error: ${e.message}"
                )
                _messages.value = _messages.value + errorMsg
            }
        }
    }

    private suspend fun handleToolCalls(toolCalls: List<ToolCallRequest>) {
        for (toolCall in toolCalls) {
            val isDestructive = AgentTools.isDestructive(toolCall.name)
            val requiresApproval = if (isDestructive) {
                !currentSettings.autoApproveWriteTools
            } else {
                !currentSettings.autoApproveReadTools
            }

            if (requiresApproval) {
                // If write or edit, generate preview diff first
                val previewResult = if (toolCall.name in listOf("write_file", "edit_file")) {
                    toolExecutor.execute(currentWorkspaceUri, toolCall, dryRunWrite = true)
                } else null

                val promptMsg = when (toolCall.name) {
                    "write_file" -> "Proposing to write file '${toolCall.parsedArguments["path"]}'"
                    "edit_file" -> "Proposing targeted edit on '${toolCall.parsedArguments["path"]}'"
                    "run_command" -> "Proposing shell command: `${toolCall.parsedArguments["command"]}`"
                    else -> "Approval needed to execute '${toolCall.name}'"
                }

                _status.value = HarnessStatus.AWAITING_APPROVAL
                _statusMessage.value = "Awaiting approval for ${toolCall.name}"
                _pendingApproval.value = PendingApproval(
                    toolCall = toolCall,
                    diff = previewResult?.proposedDiff,
                    promptMessage = promptMsg
                )
                // Halt orchestration loop until user responds
                return
            } else {
                // Auto-approved
                _status.value = HarnessStatus.EXECUTING_TOOL
                _statusMessage.value = "Running ${toolCall.name}..."

                val result = toolExecutor.execute(
                    rootTreeUri = currentWorkspaceUri,
                    toolCall = toolCall,
                    dryRunWrite = false
                )

                recordToolResult(toolCall, result)
            }
        }

        // Once all tool calls in batch are processed, continue turn loop
        runOrchestrationLoop()
    }

    private fun recordToolResult(toolCall: ToolCallRequest, result: ToolCallResult) {
        val userResponseWithResult = ChatMessage(
            role = MessageRole.USER,
            content = "",
            toolResults = listOf(result)
        )
        _messages.value = _messages.value + userResponseWithResult
    }

    private fun buildSystemPrompt(): String {
        val sb = StringBuilder()
        sb.append("You are Klix AI, an expert agentic software engineer inside KlixCode, a mobile-first native Android IDE.\n")
        sb.append("You have direct access to workspace inspection and modification tools.\n\n")

        sb.append("### Active Environment:\n")
        sb.append("- Workspace: $currentWorkspaceName\n")
        if (activeFileName != null) {
            sb.append("- Currently Open File: $activeFileName\n")
            if (activeFileContent != null && activeFileContent!!.length < 3000) {
                sb.append("```\n$activeFileContent\n```\n")
            }
        }

        sb.append("\n### Instructions:\n")
        sb.append("1. Follow the Plan-Then-Execute methodology: for non-trivial requests, outline what files you will inspect or modify before acting.\n")
        sb.append("2. When making code changes, use 'edit_file' for surgical changes or 'write_file' for new/rewritten files. All changes will generate diffs for the user to review.\n")
        sb.append("3. Always inspect existing files using 'read_file' or 'search_files' before making modifications to ensure accuracy.\n")
        sb.append("4. Keep explanations clear, concise, and mobile-friendly.\n")

        return sb.toString()
    }
}
