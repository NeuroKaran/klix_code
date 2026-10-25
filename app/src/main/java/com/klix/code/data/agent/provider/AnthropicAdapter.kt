package com.klix.code.data.agent.provider

import com.klix.code.data.agent.tools.AgentTools
import com.klix.code.data.agent.tools.ToolCallRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnthropicAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : ModelProviderAdapter {

    override suspend fun sendTurn(request: ProviderTurnRequest): ProviderTurnResponse = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("model", request.model)
        root.put("max_tokens", 4096)
        root.put("system", request.systemPrompt)
        root.put("temperature", request.temperature.toDouble())

        // Tools
        val toolsArray = JSONArray()
        for (tool in request.tools) {
            val tObj = JSONObject()
            tObj.put("name", tool.name)
            tObj.put("description", tool.description)
            tObj.put("input_schema", tool.toJsonSchema())
            toolsArray.put(tObj)
        }
        root.put("tools", toolsArray)

        // Messages
        val messagesArray = JSONArray()
        for (msg in request.messages) {
            val msgObj = JSONObject()
            val role = if (msg.role == MessageRole.USER) "user" else "assistant"
            msgObj.put("role", role)

            val contentArray = JSONArray()

            if (msg.content.isNotBlank()) {
                val textBlock = JSONObject()
                textBlock.put("type", "text")
                textBlock.put("text", msg.content)
                contentArray.put(textBlock)
            }

            // If assistant initiated tool calls
            for (toolCall in msg.toolCalls) {
                val toolUseBlock = JSONObject()
                toolUseBlock.put("type", "tool_use")
                toolUseBlock.put("id", toolCall.id)
                toolUseBlock.put("name", toolCall.name)
                try {
                    toolUseBlock.put("input", JSONObject(toolCall.rawArguments))
                } catch (_: Exception) {
                    toolUseBlock.put("input", JSONObject())
                }
                contentArray.put(toolUseBlock)
            }

            // If user message is returning tool results
            for (result in msg.toolResults) {
                val toolResultBlock = JSONObject()
                toolResultBlock.put("type", "tool_result")
                toolResultBlock.put("tool_use_id", result.toolCallId)
                val content = if (result.isSuccess) result.output else "Error: ${result.error ?: "Unknown error"}"
                toolResultBlock.put("content", content)
                contentArray.put(toolResultBlock)
            }

            if (contentArray.length() > 0) {
                msgObj.put("content", contentArray)
                messagesArray.put(msgObj)
            }
        }
        root.put("messages", messagesArray)

        val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val endpoint = request.customEndpoint?.ifBlank { null } ?: "https://api.anthropic.com/v1/messages"

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", request.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: responseBody
                } catch (_: Exception) {
                    responseBody
                }
                throw IllegalStateException("Anthropic API Error (${response.code}): $errorMsg")
            }

            val respJson = JSONObject(responseBody)
            val contentArr = respJson.optJSONArray("content") ?: JSONArray()

            var textResp: String? = null
            var thoughtResp: String? = null
            val toolCalls = mutableListOf<ToolCallRequest>()

            for (i in 0 until contentArr.length()) {
                val block = contentArr.getJSONObject(i)
                when (block.optString("type")) {
                    "text" -> {
                        textResp = (textResp ?: "") + block.optString("text")
                    }
                    "thinking" -> {
                        thoughtResp = (thoughtResp ?: "") + block.optString("thinking")
                    }
                    "tool_use" -> {
                        val id = block.optString("id")
                        val name = block.optString("name")
                        val inputObj = block.optJSONObject("input") ?: JSONObject()
                        val rawArgs = inputObj.toString()
                        toolCalls.add(
                            ToolCallRequest(
                                id = id,
                                name = name,
                                rawArguments = rawArgs,
                                parsedArguments = AgentTools.parseArguments(rawArgs)
                            )
                        )
                    }
                }
            }

            val usage = respJson.optJSONObject("usage")
            val promptTokens = usage?.optInt("input_tokens", 0) ?: 0
            val completionTokens = usage?.optInt("output_tokens", 0) ?: 0

            ProviderTurnResponse(
                textResponse = textResp,
                thoughtResponse = thoughtResp,
                toolCalls = toolCalls,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                rawJson = responseBody
            )
        }
    }
}
