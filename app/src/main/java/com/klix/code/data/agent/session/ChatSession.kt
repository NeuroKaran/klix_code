package com.klix.code.data.agent.session

import android.net.Uri
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
import com.klix.code.data.agent.tools.ToolCallRequest
import com.klix.code.data.agent.tools.ToolCallResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a workspace file or folder that can be tagged using "@".
 */
data class TaggedFileItem(
    val path: String,
    val isDirectory: Boolean,
    val uri: Uri? = null
) {
    val fileName: String
        get() = path.substringAfterLast('/')

    val parentPath: String
        get() = path.substringBeforeLast('/', "")
}

/**
 * Represents a complete agent chat session with its full message history and token usage.
 */
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(),
    val promptTokens: Int = 0,
    val completionTokens: Int = 0
) {
    val totalTokens: Int
        get() = promptTokens + completionTokens

    val previewText: String
        get() = messages.lastOrNull { it.content.isNotBlank() }?.content?.take(60) ?: "Empty conversation"

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("createdAt", createdAt)
        obj.put("updatedAt", updatedAt)
        obj.put("promptTokens", promptTokens)
        obj.put("completionTokens", completionTokens)

        val msgsArray = JSONArray()
        for (msg in messages) {
            msgsArray.put(serializeChatMessage(msg))
        }
        obj.put("messages", msgsArray)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): ChatSession {
            val id = obj.optString("id", UUID.randomUUID().toString())
            val title = obj.optString("title", "Chat")
            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            val promptTokens = obj.optInt("promptTokens", 0)
            val completionTokens = obj.optInt("completionTokens", 0)

            val messages = mutableListOf<ChatMessage>()
            val msgsArray = obj.optJSONArray("messages")
            if (msgsArray != null) {
                for (i in 0 until msgsArray.length()) {
                    val msgObj = msgsArray.optJSONObject(i) ?: continue
                    messages.add(deserializeChatMessage(msgObj))
                }
            }

            return ChatSession(
                id = id,
                title = title,
                createdAt = createdAt,
                updatedAt = updatedAt,
                messages = messages,
                promptTokens = promptTokens,
                completionTokens = completionTokens
            )
        }

        private fun serializeChatMessage(msg: ChatMessage): JSONObject {
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("role", msg.role.name)
            obj.put("content", msg.content)
            if (msg.thought != null) {
                obj.put("thought", msg.thought)
            }
            obj.put("timestamp", msg.timestamp)

            if (msg.toolCalls.isNotEmpty()) {
                val tcArray = JSONArray()
                for (tc in msg.toolCalls) {
                    val tcObj = JSONObject()
                    tcObj.put("id", tc.id)
                    tcObj.put("name", tc.name)
                    tcObj.put("rawArguments", tc.rawArguments)
                    if (tc.thoughtSignature != null) {
                        tcObj.put("thoughtSignature", tc.thoughtSignature)
                    }
                    val parsedArgsObj = JSONObject()
                    tc.parsedArguments.forEach { (k, v) -> parsedArgsObj.put(k, v) }
                    tcObj.put("parsedArguments", parsedArgsObj)
                    tcArray.put(tcObj)
                }
                obj.put("toolCalls", tcArray)
            }

            if (msg.toolResults.isNotEmpty()) {
                val resArray = JSONArray()
                for (res in msg.toolResults) {
                    val resObj = JSONObject()
                    resObj.put("toolCallId", res.toolCallId)
                    resObj.put("name", res.name)
                    resObj.put("isSuccess", res.isSuccess)
                    resObj.put("output", res.output)
                    if (res.error != null) {
                        resObj.put("error", res.error)
                    }
                    resArray.put(resObj)
                }
                obj.put("toolResults", resArray)
            }

            return obj
        }

        private fun deserializeChatMessage(obj: JSONObject): ChatMessage {
            val id = obj.optString("id", UUID.randomUUID().toString())
            val roleName = obj.optString("role", MessageRole.USER.name)
            val role = try {
                MessageRole.valueOf(roleName)
            } catch (_: Exception) {
                MessageRole.USER
            }
            val content = obj.optString("content", "")
            val thought = if (obj.has("thought")) obj.optString("thought") else null
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

            val toolCalls = mutableListOf<ToolCallRequest>()
            val tcArray = obj.optJSONArray("toolCalls")
            if (tcArray != null) {
                for (i in 0 until tcArray.length()) {
                    val tcObj = tcArray.optJSONObject(i) ?: continue
                    val tcId = tcObj.optString("id", "")
                    val tcName = tcObj.optString("name", "")
                    val rawArgs = tcObj.optString("rawArguments", "")
                    val thoughtSig = if (tcObj.has("thoughtSignature")) tcObj.optString("thoughtSignature") else null

                    val parsedMap = mutableMapOf<String, String>()
                    val parsedArgsObj = tcObj.optJSONObject("parsedArguments")
                    if (parsedArgsObj != null) {
                        val keys = parsedArgsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            parsedMap[key] = parsedArgsObj.optString(key, "")
                        }
                    }

                    toolCalls.add(
                        ToolCallRequest(
                            id = tcId,
                            name = tcName,
                            rawArguments = rawArgs,
                            parsedArguments = parsedMap,
                            thoughtSignature = thoughtSig
                        )
                    )
                }
            }

            val toolResults = mutableListOf<ToolCallResult>()
            val resArray = obj.optJSONArray("toolResults")
            if (resArray != null) {
                for (i in 0 until resArray.length()) {
                    val resObj = resArray.optJSONObject(i) ?: continue
                    val toolCallId = resObj.optString("toolCallId", "")
                    val name = resObj.optString("name", "")
                    val isSuccess = resObj.optBoolean("isSuccess", true)
                    val output = resObj.optString("output", "")
                    val error = if (resObj.has("error")) resObj.optString("error") else null

                    toolResults.add(
                        ToolCallResult(
                            toolCallId = toolCallId,
                            name = name,
                            isSuccess = isSuccess,
                            output = output,
                            error = error
                        )
                    )
                }
            }

            return ChatMessage(
                id = id,
                role = role,
                content = content,
                thought = thought,
                toolCalls = toolCalls,
                toolResults = toolResults,
                timestamp = timestamp
            )
        }
    }
}

/**
 * Recursively flattens a FileNode tree into a list of TaggedFileItems with relative paths.
 */
fun flattenFileTree(root: com.klix.code.data.model.FileNode?, basePath: String = ""): List<TaggedFileItem> {
    if (root == null) return emptyList()
    val list = mutableListOf<TaggedFileItem>()
    fun traverse(node: com.klix.code.data.model.FileNode, currentPath: String) {
        for (child in node.children) {
            val childPath = if (currentPath.isEmpty()) child.name else "$currentPath/${child.name}"
            list.add(TaggedFileItem(path = childPath, isDirectory = child.isDirectory, uri = child.uri))
            if (child.isDirectory) {
                traverse(child, childPath)
            }
        }
    }
    traverse(root, basePath)
    return list
}

