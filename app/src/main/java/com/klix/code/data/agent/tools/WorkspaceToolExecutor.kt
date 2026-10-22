package com.klix.code.data.agent.tools

import android.net.Uri
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.agent.execution.LocalExecutionProvider
import com.klix.code.data.model.DiffCalculator
import com.klix.code.data.model.FileDiff
import com.klix.code.data.repository.WorkspaceRepository

class WorkspaceToolExecutor(
    private val repository: WorkspaceRepository,
    private val executionProvider: ExecutionProvider = LocalExecutionProvider()
) {

    /**
     * Executes or previews a normalized tool call against the specified workspace tree.
     */
    suspend fun execute(
        rootTreeUri: Uri?,
        toolCall: ToolCallRequest,
        dryRunWrite: Boolean = false
    ): ToolCallResult {
        return try {
            when (toolCall.name) {
                AgentTools.READ_FILE.name -> executeReadFile(rootTreeUri, toolCall)
                AgentTools.WRITE_FILE.name -> executeWriteFile(rootTreeUri, toolCall, dryRunWrite)
                AgentTools.EDIT_FILE.name -> executeEditFile(rootTreeUri, toolCall, dryRunWrite)
                AgentTools.LIST_DIR.name -> executeListDir(rootTreeUri, toolCall)
                AgentTools.SEARCH_FILES.name -> executeSearchFiles(rootTreeUri, toolCall)
                AgentTools.RUN_COMMAND.name -> executeRunCommand(toolCall)
                else -> ToolCallResult(
                    toolCallId = toolCall.id,
                    name = toolCall.name,
                    isSuccess = false,
                    output = "",
                    error = "Unknown tool: '${toolCall.name}'"
                )
            }
        } catch (e: Exception) {
            ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "Tool execution error: ${e.message}"
            )
        }
    }

    private suspend fun executeReadFile(rootTreeUri: Uri?, toolCall: ToolCallRequest): ToolCallResult {
        if (rootTreeUri == null) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "No workspace open.")
        }
        val path = toolCall.parsedArguments["path"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'path'.")

        val doc = repository.findDocumentByPath(rootTreeUri, path)
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "File not found at path: '$path'.")

        if (doc.isDirectory) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "Path is a directory, not a file: '$path'.")
        }

        val content = repository.readFile(doc.uri)
        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = true,
            output = content
        )
    }

    private suspend fun executeWriteFile(
        rootTreeUri: Uri?,
        toolCall: ToolCallRequest,
        dryRunWrite: Boolean
    ): ToolCallResult {
        if (rootTreeUri == null) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "No workspace open.")
        }
        val path = toolCall.parsedArguments["path"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'path'.")
        val content = toolCall.parsedArguments["content"] ?: ""

        val existingDoc = repository.findDocumentByPath(rootTreeUri, path)
        val originalText = if (existingDoc != null && !existingDoc.isDirectory) {
            repository.readFile(existingDoc.uri)
        } else {
            ""
        }

        val diff = DiffCalculator.computeDiff(
            filePath = path,
            originalText = originalText,
            modifiedText = content,
            fileUri = existingDoc?.uri
        )

        if (dryRunWrite) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = true,
                output = "Diff generated for '$path' (+${diff.additionsCount} / -${diff.deletionsCount}). Awaiting user approval.",
                proposedDiff = diff
            )
        }

        val targetDoc = repository.findOrCreateDocumentByPath(rootTreeUri, path)
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Failed to create/access file: '$path'.")

        repository.writeFile(targetDoc.uri, content)
        val updatedDiff = diff.copy(fileUri = targetDoc.uri)

        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = true,
            output = "Successfully wrote ${content.length} characters to '$path'.",
            proposedDiff = updatedDiff
        )
    }

    private suspend fun executeEditFile(
        rootTreeUri: Uri?,
        toolCall: ToolCallRequest,
        dryRunWrite: Boolean
    ): ToolCallResult {
        if (rootTreeUri == null) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "No workspace open.")
        }
        val path = toolCall.parsedArguments["path"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'path'.")
        val oldString = toolCall.parsedArguments["old_string"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'old_string'.")
        val newString = toolCall.parsedArguments["new_string"] ?: ""

        val existingDoc = repository.findDocumentByPath(rootTreeUri, path)
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "File not found: '$path'.")

        val originalText = repository.readFile(existingDoc.uri)
        if (!originalText.contains(oldString)) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "Target old_string was not found in '$path'. Please ensure exact match including whitespace."
            )
        }

        // Replace target content
        val modifiedText = originalText.replaceFirst(oldString, newString)

        val diff = DiffCalculator.computeDiff(
            filePath = path,
            originalText = originalText,
            modifiedText = modifiedText,
            fileUri = existingDoc.uri
        )

        if (dryRunWrite) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = true,
                output = "Diff generated for targeted edit on '$path'. Awaiting approval.",
                proposedDiff = diff
            )
        }

        repository.writeFile(existingDoc.uri, modifiedText)
        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = true,
            output = "Successfully edited '$path'.",
            proposedDiff = diff
        )
    }

    private suspend fun executeListDir(rootTreeUri: Uri?, toolCall: ToolCallRequest): ToolCallResult {
        if (rootTreeUri == null) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "No workspace open.")
        }
        val path = toolCall.parsedArguments["path"] ?: ""
        val nodes = repository.listDirectory(rootTreeUri, path)

        if (nodes.isEmpty()) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = true,
                output = "(Directory is empty or path '$path' not found)"
            )
        }

        val sb = StringBuilder()
        for (node in nodes) {
            if (node.isDirectory) {
                sb.append("[DIR]  ").append(node.name).append("/\n")
            } else {
                sb.append("[FILE] ").append(node.name).append(" (").append(node.size).append(" bytes)\n")
            }
        }

        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = true,
            output = sb.toString().trimEnd()
        )
    }

    private suspend fun executeSearchFiles(rootTreeUri: Uri?, toolCall: ToolCallRequest): ToolCallResult {
        if (rootTreeUri == null) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "No workspace open.")
        }
        val query = toolCall.parsedArguments["query"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'query'.")
        val scope = toolCall.parsedArguments["path"]

        val matches = repository.searchFiles(rootTreeUri, query, scope)
        if (matches.isEmpty()) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = true,
                output = "No matches found for query: '$query'"
            )
        }

        val sb = StringBuilder("Found ${matches.size} match(es):\n")
        for (match in matches) {
            sb.append("${match.filePath}:${match.lineNumber}: ${match.lineContent}\n")
        }

        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = true,
            output = sb.toString().trimEnd()
        )
    }

    private suspend fun executeRunCommand(toolCall: ToolCallRequest): ToolCallResult {
        val command = toolCall.parsedArguments["command"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'command'.")

        val result = executionProvider.runCommand(command)
        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = result.isSuccess,
            output = result.combinedOutput.ifEmpty { "(Process completed with exit code ${result.exitCode})" },
            error = if (!result.isSuccess && result.stderr.isNotBlank()) result.stderr else null
        )
    }
}
