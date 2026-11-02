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

    companion object {
        const val MAX_TURNS_PER_TASK = 25
        const val MAX_AUTO_CORRECTIONS = 3
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

    var onSessionUpdated: ((messages: List<ChatMessage>, promptTokens: Int, completionTokens: Int) -> Unit)? = null
    private val detectedTaggedContext = mutableMapOf<String, String>()

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
    private var autoCorrectionAttempts = 0
    private val modifiedFilesInCurrentTask = mutableSetOf<String>()

    init {
        toolExecutor.subagentRunner = { task, maxTurns ->
            runSubagentSession(task, maxTurns)
        }
    }

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

    fun restoreSession(messages: List<ChatMessage>, promptTokens: Int, completionTokens: Int) {
        cancelTask()
        _messages.value = messages
        _totalPromptTokens.value = promptTokens
        _totalCompletionTokens.value = completionTokens
        currentTurnCount = 0
        autoCorrectionAttempts = 0
        modifiedFilesInCurrentTask.clear()
        detectedTaggedContext.clear()
        _status.value = HarnessStatus.IDLE
        _statusMessage.value = "Ready"
    }

    private fun notifySessionUpdated() {
        onSessionUpdated?.invoke(_messages.value, _totalPromptTokens.value, _totalCompletionTokens.value)
    }

    private suspend fun resolveTaggedResources(userPrompt: String) {
        val tagRegex = Regex("""@([a-zA-Z0-9_.\-/]+)""")
        val matched = tagRegex.findAll(userPrompt).map { it.groupValues[1].trim() }.distinct().toList()
        if (matched.isEmpty() || currentWorkspaceUri == null) return

        for (tag in matched) {
            val cleanTag = tag.trim().removePrefix("./").removePrefix("/")
            // Try reading as file first
            val fileReq = ToolCallRequest(
                id = "tag_read_${cleanTag.hashCode()}",
                name = "read_file",
                rawArguments = "{\"path\": \"$cleanTag\"}",
                parsedArguments = mapOf("path" to cleanTag)
            )
            val fileRes = toolExecutor.execute(currentWorkspaceUri, fileReq, dryRunWrite = false)
            if (fileRes.isSuccess && fileRes.output.isNotBlank()) {
                val lines = fileRes.output.lines()
                val truncatedOutput = if (lines.size > 200) {
                    lines.take(200).joinToString("\n") + "\n... (truncated, ${lines.size} total lines)"
                } else {
                    fileRes.output
                }
                detectedTaggedContext[cleanTag] = truncatedOutput
            } else {
                // Try listing as directory
                val dirReq = ToolCallRequest(
                    id = "tag_dir_${cleanTag.hashCode()}",
                    name = "list_dir",
                    rawArguments = "{\"path\": \"$cleanTag\"}",
                    parsedArguments = mapOf("path" to cleanTag)
                )
                val dirRes = toolExecutor.execute(currentWorkspaceUri, dirReq, dryRunWrite = false)
                if (dirRes.isSuccess && dirRes.output.isNotBlank()) {
                    detectedTaggedContext[cleanTag] = "[Directory Listing]\n${dirRes.output}"
                }
            }
        }
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
            notifySessionUpdated()
            return
        }

        currentTurnCount = 0
        autoCorrectionAttempts = 0
        modifiedFilesInCurrentTask.clear()
        currentBatchResults.clear()
        remainingPendingTools.clear()

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = userPrompt
        )
        _messages.value = _messages.value + userMessage
        notifySessionUpdated()

        activeJob = scope.launch {
            _status.value = HarnessStatus.THINKING
            _statusMessage.value = "Detecting tagged project items..."
            resolveTaggedResources(userPrompt)
            runOrchestrationLoop()
        }
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

            val cleanName = pending.toolCall.name.removePrefix("default_api:").trim()
            if (result.isSuccess && cleanName in listOf("write_file", "edit_file", "multi_replace_file_content", "multi_replace")) {
                val path = pending.toolCall.parsedArguments["path"]
                if (!path.isNullOrBlank()) {
                    modifiedFilesInCurrentTask.add(path.trim().removePrefix("./").removePrefix("/"))
                }
            }

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
        autoCorrectionAttempts = 0
        modifiedFilesInCurrentTask.clear()
        detectedTaggedContext.clear()
        _status.value = HarnessStatus.IDLE
        _statusMessage.value = "Chat reset."
        notifySessionUpdated()
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
                notifySessionUpdated()

                if (response.toolCalls.isEmpty()) {
                    // Self-Correction & Verification Hook
                    if (currentSettings.autoVerifyEdits && modifiedFilesInCurrentTask.isNotEmpty() && autoCorrectionAttempts < MAX_AUTO_CORRECTIONS) {
                        _status.value = HarnessStatus.EXECUTING_TOOL
                        _statusMessage.value = "Verifying workspace changes..."

                        val verificationError = toolExecutor.verifyWorkspace(currentWorkspaceUri, modifiedFilesInCurrentTask)
                        if (verificationError != null) {
                            autoCorrectionAttempts++
                            _status.value = HarnessStatus.THINKING
                            _statusMessage.value = "Self-correcting verification errors ($autoCorrectionAttempts/$MAX_AUTO_CORRECTIONS)..."

                            val feedbackMsg = ChatMessage(
                                role = MessageRole.USER,
                                content = "[Automated Verification Feedback]\nVerification detected the following error(s) after code modifications:\n```\n$verificationError\n```\nPlease inspect the error(s), inspect the relevant code using 'read_file', and apply a fix using 'edit_file' or 'multi_replace_file_content'."
                            )
                            _messages.value = _messages.value + feedbackMsg
                            notifySessionUpdated()

                            // Automatically continue loop for self-correction
                            runOrchestrationLoop()
                            return@launch
                        }
                    }

                    _status.value = HarnessStatus.COMPLETED
                    _statusMessage.value = if (modifiedFilesInCurrentTask.isNotEmpty() && currentSettings.autoVerifyEdits) "Done (Verified)" else "Done"
                    notifySessionUpdated()
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
                notifySessionUpdated()
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
                val previewResult = if (cleanName in listOf("write_file", "edit_file", "multi_replace_file_content", "multi_replace")) {
                    toolExecutor.execute(currentWorkspaceUri, toolCall, dryRunWrite = true)
                } else null

                val promptMsg = when (cleanName) {
                    "write_file" -> "Proposing to write file '${toolCall.parsedArguments["path"]}'"
                    "edit_file" -> "Proposing targeted edit on '${toolCall.parsedArguments["path"]}'"
                    "multi_replace_file_content", "multi_replace" -> "Proposing multi-chunk edits on '${toolCall.parsedArguments["path"]}'"
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
                if (result.isSuccess && cleanName in listOf("write_file", "edit_file", "multi_replace_file_content", "multi_replace")) {
                    val path = toolCall.parsedArguments["path"]
                    if (!path.isNullOrBlank()) {
                        modifiedFilesInCurrentTask.add(path.trim().removePrefix("./").removePrefix("/"))
                    }
                }
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
            notifySessionUpdated()
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

        if (detectedTaggedContext.isNotEmpty()) {
            sb.append("\n### Tagged Project Resources (@ Mentions):\n")
            sb.append("The user specifically tagged and highlighted the following project files/folders:\n")
            for ((tagPath, content) in detectedTaggedContext) {
                sb.append("\n#### @$tagPath:\n")
                sb.append("```\n$content\n```\n")
            }
        }

        sb.append("\n### Operating Guidelines:\n")
        sb.append("1. **Plan-Then-Execute**: For complex or non-trivial requests, outline the inspection steps and planned edits before invoking modification tools.\n")
        sb.append("2. **Inspect First**: Always inspect existing files using `read_file`, `list_dir`, or `search_files` before modifying them. Never guess file paths or content.\n")
        sb.append("3. **Line-Sliced Reading**: Use `read_file` with optional `start_line` and `end_line` to inspect specific sections of large files. Output is 1-indexed.\n")
        sb.append("4. **Pinpoint Surgical Edits**: Use `edit_file` for single contiguous changes, specifying `start_line` and `end_line` whenever possible along with exact `old_string`.\n")
        sb.append("5. **Multi-Chunk Edits**: Use `multi_replace_file_content` when making multiple non-contiguous edits across a file in a single operation.\n")
        sb.append("6. **Web Documentation & Search**: Use `search_web` to look up accurate APIs, documentation, and error solutions. Use `fetch_web_page` to retrieve official docs and reference pages.\n")
        sb.append("7. **Automated Verification**: When you make code changes, the environment automatically verifies syntax and compilation. If errors are found, you will receive feedback to self-correct.\n")
        sb.append("8. **Workspace Paths**: All tool paths must be relative to the workspace root without leading slashes (e.g., 'app/src/Main.kt').\n")
        sb.append("9. **Concise Explanations**: Provide mobile-friendly, crisp summaries of changes made.\n")
        sb.append("10. **Subagent Delegation**: Use 'invoke_subagent' when an investigation, extensive search, or complex exploration would clutter your context window. The subagent will run independently and return a synthesized summary.\n")

        return sb.toString()
    }

    private suspend fun runSubagentSession(task: String, maxTurns: Int): String = withContext(Dispatchers.IO) {
        val adapter = ProviderAdapterFactory.getAdapter(currentSettings.activeProvider)
        val subagentPrompt = buildString {
            append("You are a specialized autonomous subagent inside KlixCode IDE.\n")
            append("Your objective is to execute an isolated investigation or research subtask and return a concise, structured final report.\n\n")
            append("### Subagent Task:\n")
            append("$task\n\n")
            append("### Guidelines:\n")
            append("1. Use 'read_file', 'list_dir', and 'search_files' to inspect codebase files and symbols.\n")
            append("2. Use 'search_web' and 'fetch_web_page' if web documentation is required.\n")
            append("3. Once your investigation is complete, synthesize your findings into a clear, comprehensive markdown report with exact file paths, line ranges, and actionable findings.\n")
        }

        val subMessages = mutableListOf<ChatMessage>()
        subMessages.add(ChatMessage(role = MessageRole.USER, content = task))

        var turns = 0
        var finalSummary = ""

        while (turns < maxTurns) {
            turns++
            val turnRequest = ProviderTurnRequest(
                systemPrompt = subagentPrompt,
                messages = subMessages,
                tools = AgentTools.SUBAGENT_TOOLS,
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

            val assistantMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = response.textResponse ?: "",
                thought = response.thoughtResponse,
                toolCalls = response.toolCalls
            )
            subMessages.add(assistantMsg)

            if (response.toolCalls.isEmpty()) {
                finalSummary = response.textResponse ?: "(Subagent completed without textual response)"
                break
            }

            // Execute subagent tool calls
            val toolResults = mutableListOf<ToolCallResult>()
            for (tc in response.toolCalls) {
                val res = toolExecutor.execute(
                    rootTreeUri = currentWorkspaceUri,
                    toolCall = tc,
                    dryRunWrite = false
                )
                toolResults.add(res)
            }

            subMessages.add(
                ChatMessage(
                    role = MessageRole.USER,
                    content = "",
                    toolResults = toolResults
                )
            )
        }

        if (finalSummary.isBlank()) {
            finalSummary = subMessages.lastOrNull { it.role == MessageRole.ASSISTANT && it.content.isNotBlank() }?.content
                ?: "(Subagent reached turn budget without generating final report)"
        }

        finalSummary
    }
}
