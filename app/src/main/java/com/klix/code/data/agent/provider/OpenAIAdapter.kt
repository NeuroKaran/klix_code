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

class OpenAIAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : ModelProviderAdapter {

    override suspend fun sendTurn(request: ProviderTurnRequest): ProviderTurnResponse = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("model", request.model)
        root.put("temperature", request.temperature.toDouble())

        // Messages
        val messagesArray = JSONArray()

        // System prompt
        if (request.systemPrompt.isNotBlank()) {
            val sysObj = JSONObject()
            sysObj.put("role", "system")
            sysObj.put("content", request.systemPrompt)
            messagesArray.put(sysObj)
        }

        for (msg in request.messages) {
            val msgObj = JSONObject()
            val role = if (msg.role == MessageRole.USER) "user" else "assistant"
            msgObj.put("role", role)
            msgObj.put("content", msg.content.ifEmpty { JSONObject.NULL })

            if (msg.toolCalls.isNotEmpty()) {
                val toolCallsArr = JSONArray()
                for (tc in msg.toolCalls) {
                    val tcObj = JSONObject()
                    tcObj.put("id", tc.id)
                    tcObj.put("type", "function")
                    val fnObj = JSONObject()
                    fnObj.put("name", tc.name)
                    fnObj.put("arguments", tc.rawArguments)
                    tcObj.put("function", fnObj)
                    toolCallsArr.put(tcObj)
                }
                msgObj.put("tool_calls", toolCallsArr)
            }
            messagesArray.put(msgObj)

            // If there are tool results following, add tool messages
            for (result in msg.toolResults) {
                val toolResultObj = JSONObject()
                toolResultObj.put("role", "tool")
                toolResultObj.put("tool_call_id", result.toolCallId)
                val text = if (result.isSuccess) result.output else "Error: ${result.error ?: "Unknown error"}"
                toolResultObj.put("content", text)
                messagesArray.put(toolResultObj)
            }
        }
        root.put("messages", messagesArray)

        // Tools
        if (request.tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            for (tool in request.tools) {
                val tObj = JSONObject()
                tObj.put("type", "function")
                val fnObj = JSONObject()
                fnObj.put("name", tool.name)
                fnObj.put("description", tool.description)
                fnObj.put("parameters", tool.toJsonSchema())
                tObj.put("function", fnObj)
                toolsArray.put(tObj)
            }
            root.put("tools", toolsArray)
        }

        val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val endpoint = request.customEndpoint?.ifBlank { null } ?: "https://api.openai.com/v1/chat/completions"

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${request.apiKey}")
            .addHeader("Content-Type", "application/json")
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
                throw IllegalStateException("OpenAI API Error (${response.code}): $errorMsg")
            }

            val respJson = JSONObject(responseBody)
            val choices = respJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext ProviderTurnResponse(
                    textResponse = null,
                    toolCalls = emptyList(),
                    rawJson = responseBody
                )
            }

            val message = choices.getJSONObject(0).optJSONObject("message")
            val textContent = message?.optString("content")?.takeIf { it.isNotBlank() && it != "null" }

            val toolCalls = mutableListOf<ToolCallRequest>()
            val tcArr = message?.optJSONArray("tool_calls")
            if (tcArr != null) {
                for (i in 0 until tcArr.length()) {
                    val tcObj = tcArr.getJSONObject(i)
                    val id = tcObj.optString("id")
                    val fn = tcObj.optJSONObject("function")
                    val name = fn?.optString("name") ?: ""
                    val args = fn?.optString("arguments") ?: "{}"
                    toolCalls.add(
                        ToolCallRequest(
                            id = id,
                            name = name,
                            rawArguments = args,
                            parsedArguments = AgentTools.parseArguments(args)
                        )
                    )
                }
            }

            val usage = respJson.optJSONObject("usage")
            val promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0
            val completionTokens = usage?.optInt("completion_tokens", 0) ?: 0

            ProviderTurnResponse(
                textResponse = textContent,
                toolCalls = toolCalls,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                rawJson = responseBody
            )
        }
    }
}
