package com.klix.code.data.agent.tools

import android.net.Uri
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.agent.execution.LocalExecutionProvider
import com.klix.code.data.model.DiffCalculator
import com.klix.code.data.model.FileDiff
import com.klix.code.data.repository.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WorkspaceToolExecutor(
    private val repository: WorkspaceRepository,
    private val executionProvider: ExecutionProvider = LocalExecutionProvider(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    /**
     * Executes or previews a normalized tool call against the specified workspace tree.
     */
    suspend fun execute(
        rootTreeUri: Uri?,
        toolCall: ToolCallRequest,
        dryRunWrite: Boolean = false
    ): ToolCallResult {
        val cleanName = toolCall.name.removePrefix("default_api:").trim()
        return try {
            when (cleanName) {
                AgentTools.READ_FILE.name, "view_file" -> executeReadFile(rootTreeUri, toolCall)
                AgentTools.WRITE_FILE.name -> executeWriteFile(rootTreeUri, toolCall, dryRunWrite)
                AgentTools.EDIT_FILE.name, "replace_file_content" -> executeEditFile(rootTreeUri, toolCall, dryRunWrite)
                AgentTools.MULTI_REPLACE_FILE_CONTENT.name, "multi_replace_file_content", "multi_replace" ->
                    executeMultiReplaceFileContent(rootTreeUri, toolCall, dryRunWrite)
                AgentTools.LIST_DIR.name -> executeListDir(rootTreeUri, toolCall)
                AgentTools.SEARCH_FILES.name -> executeSearchFiles(rootTreeUri, toolCall)
                AgentTools.RUN_COMMAND.name -> executeRunCommand(toolCall)
                AgentTools.FETCH_WEB_PAGE.name, "read_url_content", "fetch_url" -> executeFetchWebPage(toolCall)
                AgentTools.SEARCH_WEB.name -> executeSearchWeb(toolCall)
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
        val rawPath = toolCall.parsedArguments["path"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'path'.")
        val path = rawPath.trim().removePrefix("./").removePrefix("/")

        val doc = repository.findDocumentByPath(rootTreeUri, path)
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "File not found at path: '$path'. Use 'list_dir' or 'search_files' to verify file locations.")

        if (doc.isDirectory) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "Path is a directory, not a file: '$path'. Use 'list_dir' instead.")
        }

        val content = repository.readFile(doc.uri)
        if (content.isEmpty()) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = true,
                output = "(File is empty)"
            )
        }

        val lines = content.split("\n")
        val totalLines = lines.size

        val startLineArg = toolCall.parsedArguments["start_line"]?.toIntOrNull()
            ?: toolCall.parsedArguments["startLine"]?.toIntOrNull()
        val endLineArg = toolCall.parsedArguments["end_line"]?.toIntOrNull()
            ?: toolCall.parsedArguments["endLine"]?.toIntOrNull()

        val start = if (startLineArg != null) maxOf(1, startLineArg) else 1
        if (start > totalLines) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "start_line ($start) exceeds total lines in file ($totalLines)."
            )
        }

        val end = if (endLineArg != null) {
            minOf(totalLines, maxOf(start, endLineArg))
        } else {
            if (startLineArg == null && totalLines > 800) 800 else totalLines
        }

        if (start > end) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "start_line ($start) is greater than end_line ($end)."
            )
        }

        val sb = StringBuilder()
        for (i in start..end) {
            val lineContent = lines[i - 1].removeSuffix("\r")
            sb.append(i).append(": ").append(lineContent)
            if (i < end) sb.append("\n")
        }

        if (startLineArg == null && endLineArg == null && totalLines > 800) {
            sb.append("\n\n[File has $totalLines lines. Showing lines 1 to 800. Use start_line and end_line to inspect specific ranges.]")
        }

        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = true,
            output = sb.toString()
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
        val rawPath = toolCall.parsedArguments["path"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'path'.")
        val path = rawPath.trim().removePrefix("./").removePrefix("/")
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
        val rawPath = toolCall.parsedArguments["path"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'path'.")
        val path = rawPath.trim().removePrefix("./").removePrefix("/")

        val oldString = toolCall.parsedArguments["old_string"]
            ?: toolCall.parsedArguments["target_content"]
            ?: toolCall.parsedArguments["targetContent"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'old_string'.")

        val newString = if (toolCall.parsedArguments.containsKey("new_string")) {
            toolCall.parsedArguments["new_string"] ?: ""
        } else if (toolCall.parsedArguments.containsKey("replacement_content")) {
            toolCall.parsedArguments["replacement_content"] ?: ""
        } else if (toolCall.parsedArguments.containsKey("replacementContent")) {
            toolCall.parsedArguments["replacementContent"] ?: ""
        } else {
            ""
        }

        val startLine = toolCall.parsedArguments["start_line"]?.toIntOrNull()
            ?: toolCall.parsedArguments["startLine"]?.toIntOrNull()
        val endLine = toolCall.parsedArguments["end_line"]?.toIntOrNull()
            ?: toolCall.parsedArguments["endLine"]?.toIntOrNull()

        val existingDoc = repository.findDocumentByPath(rootTreeUri, path)
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "File not found: '$path'.")

        val originalText = repository.readFile(existingDoc.uri)

        val match = findMatchRange(originalText, oldString, startLine, endLine)
        if (match == null) {
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "Target old_string was not found in '$path'${if (startLine != null) " within lines $startLine..$endLine" else ""}. Please verify exact match or use 'read_file'."
            )
        }

        if (match.ambiguousCount > 1) {
            val occurrences = findOccurrences(originalText, oldString)
            val lineNumbers = occurrences.map { getLineNumberForOffset(originalText, it) }
            return ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "Target old_string was found ${match.ambiguousCount} times in '$path' at lines $lineNumbers. Please specify 'start_line' and 'end_line' to disambiguate which occurrence to replace."
            )
        }

        val modifiedText = originalText.substring(0, match.startIndex) + newString + originalText.substring(match.endIndex)

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

    private suspend fun executeMultiReplaceFileContent(
        rootTreeUri: Uri?,
        toolCall: ToolCallRequest,
        dryRunWrite: Boolean
    ): ToolCallResult {
        if (rootTreeUri == null) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "No workspace open.")
        }
        val rawPath = toolCall.parsedArguments["path"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'path'.")
        val path = rawPath.trim().removePrefix("./").removePrefix("/")

        val rawChunks = toolCall.parsedArguments["replacement_chunks"]
            ?: toolCall.parsedArguments["chunks"]
            ?: toolCall.parsedArguments["replacements"]
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'replacement_chunks'.")

        val existingDoc = repository.findDocumentByPath(rootTreeUri, path)
            ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "File not found: '$path'.")

        val originalText = repository.readFile(existingDoc.uri)

        val chunksArray = try {
            org.json.JSONArray(rawChunks)
        } catch (e: Exception) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "Failed to parse replacement_chunks as JSON array: ${e.message}")
        }

        if (chunksArray.length() == 0) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "replacement_chunks cannot be empty.")
        }

        data class ChunkPlan(
            val chunkIndex: Int,
            val startIndex: Int,
            val endIndex: Int,
            val replacement: String
        )

        val plannedChunks = mutableListOf<ChunkPlan>()

        for (i in 0 until chunksArray.length()) {
            val chunkObj = chunksArray.optJSONObject(i)
                ?: return ToolCallResult(toolCall.id, toolCall.name, false, "", "Chunk at index $i is not a valid JSON object.")

            val oldString = chunkObj.optString("old_string")
                .ifEmpty { chunkObj.optString("target_content") }
                .ifEmpty { chunkObj.optString("targetContent") }

            if (oldString.isEmpty()) {
                return ToolCallResult(toolCall.id, toolCall.name, false, "", "Chunk $i missing required 'old_string'.")
            }

            val newString = if (chunkObj.has("new_string")) {
                chunkObj.optString("new_string")
            } else if (chunkObj.has("replacement_content")) {
                chunkObj.optString("replacement_content")
            } else if (chunkObj.has("replacementContent")) {
                chunkObj.optString("replacementContent")
            } else {
                ""
            }

            val startLine = if (chunkObj.has("start_line")) chunkObj.optInt("start_line")
            else if (chunkObj.has("startLine")) chunkObj.optInt("startLine")
            else null
            val endLine = if (chunkObj.has("end_line")) chunkObj.optInt("end_line")
            else if (chunkObj.has("endLine")) chunkObj.optInt("endLine")
            else null

            val match = findMatchRange(originalText, oldString, startLine, endLine)
            if (match == null) {
                return ToolCallResult(
                    toolCallId = toolCall.id,
                    name = toolCall.name,
                    isSuccess = false,
                    output = "",
                    error = "Chunk $i: target old_string was not found in '$path'${if (startLine != null) " (lines $startLine..$endLine)" else ""}. Please verify exact content."
                )
            }
            if (match.ambiguousCount > 1) {
                return ToolCallResult(
                    toolCallId = toolCall.id,
                    name = toolCall.name,
                    isSuccess = false,
                    output = "",
                    error = "Chunk $i: target old_string occurs ${match.ambiguousCount} times in '$path'. Please specify 'start_line' and 'end_line' for this chunk."
                )
            }

            plannedChunks.add(ChunkPlan(i, match.startIndex, match.endIndex, newString))
        }

        // Check for overlapping chunk ranges
        val sortedByStart = plannedChunks.sortedBy { it.startIndex }
        for (k in 0 until sortedByStart.size - 1) {
            val cur = sortedByStart[k]
            val next = sortedByStart[k + 1]
            if (cur.endIndex > next.startIndex) {
                return ToolCallResult(
                    toolCallId = toolCall.id,
                    name = toolCall.name,
                    isSuccess = false,
                    output = "",
                    error = "Replacement chunks ${cur.chunkIndex} and ${next.chunkIndex} overlap. Ensure all chunk targets are non-overlapping."
                )
            }
        }

        // Apply replacements from bottom to top (highest startIndex first) to maintain character offsets
        val sortedDescending = plannedChunks.sortedByDescending { it.startIndex }
        var modifiedText = originalText
        for (chunk in sortedDescending) {
            modifiedText = modifiedText.substring(0, chunk.startIndex) + chunk.replacement + modifiedText.substring(chunk.endIndex)
        }

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
                output = "Diff generated for multi-replace on '$path' (${plannedChunks.size} chunks, +${diff.additionsCount} / -${diff.deletionsCount}). Awaiting approval.",
                proposedDiff = diff
            )
        }

        repository.writeFile(existingDoc.uri, modifiedText)
        return ToolCallResult(
            toolCallId = toolCall.id,
            name = toolCall.name,
            isSuccess = true,
            output = "Successfully applied ${plannedChunks.size} replacement chunks to '$path'.",
            proposedDiff = diff
        )
    }

    private suspend fun executeListDir(rootTreeUri: Uri?, toolCall: ToolCallRequest): ToolCallResult {
        if (rootTreeUri == null) {
            return ToolCallResult(toolCall.id, toolCall.name, false, "", "No workspace open.")
        }
        val rawPath = (toolCall.parsedArguments["path"] ?: "").trim().removePrefix("./").removePrefix("/")
        val path = if (rawPath == "." || rawPath == "/") "" else rawPath
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

    private data class MatchResult(
        val startIndex: Int,
        val endIndex: Int,
        val ambiguousCount: Int = 1
    )

    private fun findMatchRange(
        text: String,
        oldString: String,
        startLine: Int?,
        endLine: Int?
    ): MatchResult? {
        val lines = text.split("\n")
        if (startLine != null || endLine != null) {
            val s = maxOf(1, startLine ?: 1)
            val e = minOf(lines.size, endLine ?: lines.size)
            if (s > lines.size || s > e) return null

            val lineOffsets = computeLineOffsets(lines)
            val windowStart = lineOffsets[s - 1]
            val windowEnd = lineOffsets[e - 1] + lines[e - 1].length
            val windowText = text.substring(windowStart, windowEnd)

            var idx = windowText.indexOf(oldString)
            var matchedLen = oldString.length
            if (idx == -1) {
                val stripped = stripLineNumberPrefixes(oldString)
                if (stripped != oldString) {
                    idx = windowText.indexOf(stripped)
                    matchedLen = stripped.length
                }
            }
            if (idx == -1) {
                val normOld = oldString.replace("\r", "")
                idx = indexOfIgnoringCr(windowText, normOld)
                if (idx != -1) {
                    matchedLen = normOld.length
                }
            }

            if (idx != -1) {
                return MatchResult(windowStart + idx, windowStart + idx + matchedLen, 1)
            }
            return null
        }

        val matches = findOccurrences(text, oldString)
        if (matches.isNotEmpty()) {
            if (matches.size > 1) {
                return MatchResult(-1, -1, ambiguousCount = matches.size)
            }
            return MatchResult(matches[0], matches[0] + oldString.length, 1)
        }

        val stripped = stripLineNumberPrefixes(oldString)
        if (stripped != oldString) {
            val strippedMatches = findOccurrences(text, stripped)
            if (strippedMatches.isNotEmpty()) {
                if (strippedMatches.size > 1) {
                    return MatchResult(-1, -1, ambiguousCount = strippedMatches.size)
                }
                return MatchResult(strippedMatches[0], strippedMatches[0] + stripped.length, 1)
            }
        }

        val normOld = oldString.replace("\r", "")
        val normMatches = findOccurrences(text.replace("\r", ""), normOld)
        if (normMatches.size == 1) {
            val idx = indexOfIgnoringCr(text, normOld)
            if (idx != -1) {
                return MatchResult(idx, idx + normOld.length, 1)
            }
        } else if (normMatches.size > 1) {
            return MatchResult(-1, -1, ambiguousCount = normMatches.size)
        }

        return null
    }

    private fun computeLineOffsets(lines: List<String>): List<Int> {
        val offsets = ArrayList<Int>(lines.size)
        var cur = 0
        for (line in lines) {
            offsets.add(cur)
            cur += line.length + 1
        }
        return offsets
    }

    private fun findOccurrences(text: String, pattern: String): List<Int> {
        if (pattern.isEmpty()) return emptyList()
        val list = mutableListOf<Int>()
        var idx = 0
        while (true) {
            val found = text.indexOf(pattern, idx)
            if (found == -1) break
            list.add(found)
            idx = found + maxOf(1, pattern.length)
        }
        return list
    }

    private fun getLineNumberForOffset(text: String, offset: Int): Int {
        var line = 1
        val limit = minOf(offset, text.length)
        for (i in 0 until limit) {
            if (text[i] == '\n') line++
        }
        return line
    }

    private fun stripLineNumberPrefixes(text: String): String {
        val prefixRegex = Regex("""^\s*\d+[:|]\s?""")
        return text.lines().joinToString("\n") { line ->
            prefixRegex.replace(line, "")
        }
    }

    private fun indexOfIgnoringCr(haystack: String, needleNoCr: String): Int {
        if (needleNoCr.isEmpty()) return 0
        var hIdx = 0
        var nIdx = 0
        var matchStart = -1

        while (hIdx < haystack.length) {
            if (haystack[hIdx] == '\r') {
                hIdx++
                continue
            }
            if (haystack[hIdx] == needleNoCr[nIdx]) {
                if (nIdx == 0) matchStart = hIdx
                nIdx++
                hIdx++
                if (nIdx == needleNoCr.length) return matchStart
            } else {
                if (matchStart != -1) {
                    hIdx = matchStart + 1
                    matchStart = -1
                    nIdx = 0
                } else {
                    hIdx++
                }
            }
        }
        return -1
    }

    suspend fun executeFetchWebPage(toolCall: ToolCallRequest): ToolCallResult = withContext(Dispatchers.IO) {
        val rawUrl = toolCall.parsedArguments["url"]
            ?: return@withContext ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'url'.")
        val url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return@withContext ToolCallResult(toolCall.id, toolCall.name, false, "", "Invalid URL '$url'. Must start with http:// or https://")
        }

        val maxLen = toolCall.parsedArguments["max_length"]?.toIntOrNull() ?: 10000

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) KlixCode/0.3.1 Mobile")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,text/plain;q=0.8,*/*;q=0.5")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ToolCallResult(
                        toolCallId = toolCall.id,
                        name = toolCall.name,
                        isSuccess = false,
                        output = "",
                        error = "HTTP error ${response.code}: ${response.message}"
                    )
                }

                val body = response.body?.string() ?: ""
                val contentType = response.header("Content-Type") ?: ""

                val textContent = if (contentType.contains("text/html") || body.contains("<html", ignoreCase = true)) {
                    convertHtmlToMarkdown(body)
                } else {
                    body.trim()
                }

                val truncated = if (textContent.length > maxLen) {
                    textContent.take(maxLen) + "\n\n[Content truncated at $maxLen characters. Specify a narrower URL or section if needed.]"
                } else {
                    textContent
                }

                ToolCallResult(
                    toolCallId = toolCall.id,
                    name = toolCall.name,
                    isSuccess = true,
                    output = truncated.ifEmpty { "(Web page returned no readable text)" }
                )
            }
        } catch (e: Exception) {
            ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "Failed to fetch web page: ${e.message}"
            )
        }
    }

    suspend fun executeSearchWeb(toolCall: ToolCallRequest): ToolCallResult = withContext(Dispatchers.IO) {
        val query = toolCall.parsedArguments["query"]
            ?: return@withContext ToolCallResult(toolCall.id, toolCall.name, false, "", "Missing required parameter 'query'.")
        val maxResults = minOf(10, maxOf(1, toolCall.parsedArguments["max_results"]?.toIntOrNull() ?: 5))

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ToolCallResult(
                        toolCallId = toolCall.id,
                        name = toolCall.name,
                        isSuccess = false,
                        output = "",
                        error = "Search request returned HTTP ${response.code}: ${response.message}"
                    )
                }

                val html = response.body?.string() ?: ""
                val searchResults = parseDuckDuckGoHtml(html, maxResults)

                if (searchResults.isEmpty()) {
                    return@withContext ToolCallResult(
                        toolCallId = toolCall.id,
                        name = toolCall.name,
                        isSuccess = true,
                        output = "No search results found for: '$query'"
                    )
                }

                val sb = StringBuilder("Found ${searchResults.size} search result(s) for '$query':\n\n")
                for ((idx, res) in searchResults.withIndex()) {
                    sb.append("${idx + 1}. **${res.title}**\n")
                    sb.append("   URL: ${res.url}\n")
                    if (res.snippet.isNotBlank()) {
                        sb.append("   ${res.snippet}\n")
                    }
                    sb.append("\n")
                }

                ToolCallResult(
                    toolCallId = toolCall.id,
                    name = toolCall.name,
                    isSuccess = true,
                    output = sb.toString().trimEnd()
                )
            }
        } catch (e: Exception) {
            ToolCallResult(
                toolCallId = toolCall.id,
                name = toolCall.name,
                isSuccess = false,
                output = "",
                error = "Search failed: ${e.message}"
            )
        }
    }

    /**
     * Verifies the workspace or specific modified files for syntax, build, or test errors.
     * Returns error output if verification fails, or null if verification succeeds or is skipped.
     */
    suspend fun verifyWorkspace(rootTreeUri: Uri?, modifiedPaths: Set<String>): String? {
        if (modifiedPaths.isEmpty()) return null

        val errorList = mutableListOf<String>()

        // 1. In-process syntax check for JSON files
        for (path in modifiedPaths) {
            if (path.endsWith(".json", ignoreCase = true)) {
                val doc = if (rootTreeUri != null) repository.findDocumentByPath(rootTreeUri, path) else null
                if (doc != null) {
                    val content = repository.readFile(doc.uri)
                    try {
                        org.json.JSONObject(content)
                    } catch (e1: Exception) {
                        try {
                            org.json.JSONArray(content)
                        } catch (e2: Exception) {
                            errorList.add("JSON Syntax Error in '$path': ${e1.message}")
                        }
                    }
                }
            }
        }

        // 2. Sandbox compiler/linter checks via executionProvider
        for (path in modifiedPaths) {
            val cmd = when {
                path.endsWith(".py", ignoreCase = true) -> "python3 -m py_compile \"$path\" 2>&1 || python -m py_compile \"$path\" 2>&1"
                path.endsWith(".js", ignoreCase = true) || path.endsWith(".mjs", ignoreCase = true) -> "node -c \"$path\" 2>&1"
                path.endsWith(".sh", ignoreCase = true) || path.endsWith(".bash", ignoreCase = true) -> "sh -n \"$path\" 2>&1"
                else -> null
            }

            if (cmd != null) {
                val result = executionProvider.runCommand(cmd)
                if (!result.isSuccess) {
                    val output = result.combinedOutput
                    val isCommandMissing = output.contains("not found", ignoreCase = true) ||
                                           output.contains("No such file or directory", ignoreCase = true)
                    if (!isCommandMissing && output.isNotBlank()) {
                        errorList.add("Verification error in '$path':\n$output")
                    }
                }
            }
        }

        return if (errorList.isNotEmpty()) errorList.joinToString("\n\n") else null
    }

    fun convertHtmlToMarkdown(rawHtml: String): String {
        var s = rawHtml
        s = s.replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
        s = s.replace(Regex("(?is)<style[^>]*>.*?</style>"), "")
        s = s.replace(Regex("(?is)<svg[^>]*>.*?</svg>"), "")
        s = s.replace(Regex("(?is)<noscript[^>]*>.*?</noscript>"), "")

        s = s.replace(Regex("(?is)<pre[^>]*><code[^>]*>(.*?)</code></pre>")) { match ->
            "\n```\n${match.groupValues[1].trim()}\n```\n"
        }
        s = s.replace(Regex("(?is)<code[^>]*>(.*?)</code>")) { match ->
            "`${match.groupValues[1]}`"
        }

        s = s.replace(Regex("(?is)<h1[^>]*>(.*?)</h1>"), "\n\n# $1\n")
        s = s.replace(Regex("(?is)<h2[^>]*>(.*?)</h2>"), "\n\n## $1\n")
        s = s.replace(Regex("(?is)<h3[^>]*>(.*?)</h3>"), "\n\n### $1\n")
        s = s.replace(Regex("(?is)<h[4-6][^>]*>(.*?)</h[4-6]>"), "\n\n#### $1\n")

        s = s.replace(Regex("(?is)<p[^>]*>(.*?)</p>"), "\n\n$1\n")
        s = s.replace(Regex("(?is)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?is)<li[^>]*>(.*?)</li>"), "\n* $1")

        s = s.replace(Regex("<[^>]+>"), " ")

        s = s.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")

        return s.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    data class SearchItem(
        val title: String,
        val url: String,
        val snippet: String
    )

    fun parseDuckDuckGoHtml(html: String, maxResults: Int): List<SearchItem> {
        val results = mutableListOf<SearchItem>()
        val linkRegex = Regex("""(?is)<a[^>]*class="[^"]*result__a[^"]*"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""")
        val snippetRegex = Regex("""(?is)<a[^>]*class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</a>""")

        val linkMatches = linkRegex.findAll(html).toList()
        val snippetMatches = snippetRegex.findAll(html).toList()

        val count = minOf(linkMatches.size, maxResults)
        for (i in 0 until count) {
            val rawHref = linkMatches[i].groupValues[1]
            val rawTitle = linkMatches[i].groupValues[2]
            val rawSnippet = if (i < snippetMatches.size) snippetMatches[i].groupValues[1] else ""

            val title = cleanHtmlText(rawTitle)
            val snippet = cleanHtmlText(rawSnippet)
            val cleanUrl = extractTargetUrl(rawHref)

            if (cleanUrl.isNotBlank() && title.isNotBlank()) {
                results.add(SearchItem(title, cleanUrl, snippet))
            }
        }
        return results
    }

    private fun extractTargetUrl(rawHref: String): String {
        return try {
            if (rawHref.contains("uddg=")) {
                val encoded = rawHref.substringAfter("uddg=").substringBefore("&")
                URLDecoder.decode(encoded, "UTF-8")
            } else if (rawHref.startsWith("//")) {
                "https:$rawHref"
            } else {
                rawHref
            }
        } catch (_: Exception) {
            rawHref
        }
    }

    private fun cleanHtmlText(text: String): String {
        return text.replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .trim()
    }
}
