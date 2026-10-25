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

    companion object {
        const val MAX_TURNS_PER_TASK = 25
    }

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

    // Batch state for parallel/sequential tool calls within a single model turn
    private val currentBatchResults = mutableListOf<ToolCallResult>()
    private val remainingPendingTools = mutableListOf<ToolCallRequest>()
    private var currentTurnCount = 0

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

        currentTurnCount = 0
        currentBatchResults.clear()
        remainingPendingTools.clear()

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

            currentBatchResults.add(result)
            processNextToolInBatch()
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

        currentBatchResults.add(result)
        activeJob = scope.launch {
            processNextToolInBatch()
        }
    }

    fun cancelTask() {
        activeJob?.cancel()
        currentBatchResults.clear()
        remainingPendingTools.clear()
        _status.value = HarnessStatus.IDLE
        _statusMessage.value = "Task cancelled by user."
        _pendingApproval.value = null
    }

    fun clearHistory() {
        cancelTask()
        _messages.value = emptyList()
        _totalPromptTokens.value = 0
        _totalCompletionTokens.value = 0
        currentTurnCount = 0
        _status.value = HarnessStatus.IDLE
        _statusMessage.value = "Chat reset."
    }

    private fun runOrchestrationLoop() {
        currentTurnCount++
        if (currentTurnCount > MAX_TURNS_PER_TASK) {
            _status.value = HarnessStatus.COMPLETED
            _statusMessage.value = "Reached turn limit ($MAX_TURNS_PER_TASK turns)."
            val warningMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "Task reached maximum turn budget ($MAX_TURNS_PER_TASK turns). Please review the actions taken above and continue with a new instruction if needed."
            )
            _messages.value = _messages.value + warningMsg
            return
        }

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

                // Record Assistant Turn with reasoning/thought and tool calls
                val assistantMsg = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = response.textResponse ?: "",
                    thought = response.thoughtResponse,
                    toolCalls = response.toolCalls
                )
                _messages.value = _messages.value + assistantMsg

                if (response.toolCalls.isEmpty()) {
                    _status.value = HarnessStatus.COMPLETED
                    _statusMessage.value = "Done"
                    return@launch
                }

                // Initialize batch execution for all tool calls in this turn
                currentBatchResults.clear()
                remainingPendingTools.clear()
                remainingPendingTools.addAll(response.toolCalls)

                processNextToolInBatch()

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

    private suspend fun processNextToolInBatch() {
        while (remainingPendingTools.isNotEmpty()) {
            val toolCall = remainingPendingTools.removeAt(0)
            val cleanName = toolCall.name.removePrefix("default_api:").trim()
            val isDestructive = AgentTools.isDestructive(cleanName)
            val requiresApproval = if (isDestructive) {
                !currentSettings.autoApproveWriteTools
            } else {
                !currentSettings.autoApproveReadTools
            }

            if (requiresApproval) {
                // If write or edit, generate preview diff first
                val previewResult = if (cleanName in listOf("write_file", "edit_file")) {
                    toolExecutor.execute(currentWorkspaceUri, toolCall, dryRunWrite = true)
                } else null

                val promptMsg = when (cleanName) {
                    "write_file" -> "Proposing to write file '${toolCall.parsedArguments["path"]}'"
                    "edit_file" -> "Proposing targeted edit on '${toolCall.parsedArguments["path"]}'"
                    "run_command" -> "Proposing shell command: `${toolCall.parsedArguments["command"]}`"
                    else -> "Approval needed to execute '${cleanName}'"
                }

                _status.value = HarnessStatus.AWAITING_APPROVAL
                _statusMessage.value = "Awaiting approval for $cleanName"
                _pendingApproval.value = PendingApproval(
                    toolCall = toolCall,
                    diff = previewResult?.proposedDiff,
                    promptMessage = promptMsg
                )
                // Pause batch execution until user approves or denies
                return
            } else {
                // Auto-approved
                _status.value = HarnessStatus.EXECUTING_TOOL
                _statusMessage.value = "Running $cleanName..."

                val result = toolExecutor.execute(
                    rootTreeUri = currentWorkspaceUri,
                    toolCall = toolCall,
                    dryRunWrite = false
                )
                currentBatchResults.add(result)
            }
        }

        // All tools in this turn's batch have finished: commit unified tool result message
        if (currentBatchResults.isNotEmpty()) {
            val userResponseWithResults = ChatMessage(
                role = MessageRole.USER,
                content = "",
                toolResults = currentBatchResults.toList()
            )
            _messages.value = _messages.value + userResponseWithResults
            currentBatchResults.clear()
        }

        // Continue next turn in orchestration loop
        runOrchestrationLoop()
    }

    private fun buildSystemPrompt(): String {
        val sb = StringBuilder()
        sb.append("You are Klix AI, an elite agentic software engineer inside KlixCode, a mobile-first native Android IDE.\n")
        sb.append("You have direct access to workspace tools to inspect, navigate, write, and execute code.\n\n")

        sb.append("### Active Environment:\n")
        sb.append("- Workspace: $currentWorkspaceName\n")
        if (activeFileName != null) {
            sb.append("- Currently Open File: $activeFileName\n")
            if (activeFileContent != null && activeFileContent!!.length < 3000) {
                sb.append("```\n$activeFileContent\n```\n")
            }
        }

        sb.append("\n### Operating Guidelines:\n")
        sb.append("1. **Plan-Then-Execute**: For complex or non-trivial requests, outline the inspection steps and planned edits before invoking modification tools.\n")
        sb.append("2. **Inspect First**: Always inspect existing files using `read_file`, `list_dir`, or `search_files` before modifying them. Never guess file paths or content.\n")
        sb.append("3. **Targeted Modifications**: Use `edit_file` for surgical changes by specifying exact `old_string` matches (including exact indentation and line breaks). Use `write_file` for creating new files or complete rewrites.\n")
        sb.append("4. **Workspace Paths**: All tool paths must be relative to the workspace root without leading slashes (e.g., 'app/src/Main.kt').\n")
        sb.append("5. **Concise Explanations**: Provide mobile-friendly, crisp summaries of changes made.\n")

        return sb.toString()
    }
}
