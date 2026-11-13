package com.klix.code.data.agent.provider

import com.klix.code.data.agent.model.DiscoveredModel
import com.klix.code.data.agent.model.ModelProvider
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
            val reasoningContent = message?.optString("reasoning_content")?.takeIf { it.isNotBlank() && it != "null" }
                ?: message?.optString("reasoning")?.takeIf { it.isNotBlank() && it != "null" }

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
                thoughtResponse = reasoningContent,
                toolCalls = toolCalls,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                rawJson = responseBody
            )
        }
    }

    override suspend fun listModels(apiKey: String, customEndpoint: String?): List<DiscoveredModel> = withContext(Dispatchers.IO) {
        val isOpenRouter = customEndpoint?.contains("openrouter.ai") == true ||
            (customEndpoint.isNullOrBlank() && apiKey.startsWith("sk-or-"))

        if (!isOpenRouter && apiKey.isBlank()) {
            return@withContext emptyList()
        }

        val url = when {
            isOpenRouter -> "https://openrouter.ai/api/v1/models"
            !customEndpoint.isNullOrBlank() -> {
                when {
                    customEndpoint.endsWith("/chat/completions") -> customEndpoint.replace("/chat/completions", "/models")
                    customEndpoint.endsWith("/") -> "${customEndpoint}models"
                    else -> "$customEndpoint/models"
                }
            }
            else -> "https://api.openai.com/v1/models"
        }

        val reqBuilder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        try {
            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val isOr = url.contains("openrouter.ai")
                parseModelsJson(body, isOr, customEndpoint)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun parseModelsJson(jsonString: String, isOr: Boolean, customEndpoint: String?): List<DiscoveredModel> {
        return try {
            val json = JSONObject(jsonString)
            val data = json.optJSONArray("data") ?: return emptyList()
            val list = mutableListOf<DiscoveredModel>()
            val prov = if (isOr || !customEndpoint.isNullOrBlank()) ModelProvider.CUSTOM_OPENAI_COMPATIBLE else ModelProvider.OPENAI

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id")
                if (id.isBlank()) continue
                val name = item.optString("name", id)
                val description = item.optString("description").takeIf { it.isNotBlank() }
                val contextLength = if (item.has("context_length")) item.optInt("context_length") else null

                var isFree = false
                var promptPrice: Double? = null
                var compPrice: Double? = null

                if (isOr) {
                    val pricing = item.optJSONObject("pricing")
                    if (pricing != null) {
                        val promptStr = pricing.optString("prompt", "")
                        val compStr = pricing.optString("completion", "")
                        promptPrice = promptStr.toDoubleOrNull() ?: if (pricing.has("prompt")) pricing.optDouble("prompt", -1.0) else null
                        compPrice = compStr.toDoubleOrNull() ?: if (pricing.has("completion")) pricing.optDouble("completion", -1.0) else null
                    }

                    val isZeroPrice = promptPrice != null && compPrice != null && promptPrice == 0.0 && compPrice == 0.0
                    val hasPositivePrice = (promptPrice != null && promptPrice > 0.0) || (compPrice != null && compPrice > 0.0)
                    isFree = (id.contains(":free", ignoreCase = true) ||
                        name.contains("(free)", ignoreCase = true) ||
                        isZeroPrice) && !hasPositivePrice
                }

                // For standard OpenAI, filter out non-chat models
                if (prov == ModelProvider.OPENAI) {
                    val isNonChat = id.contains("tts") || id.contains("whisper") ||
                        id.contains("embedding") || id.contains("dall-e") ||
                        id.contains("moderation") || id.contains("davinci") ||
                        id.contains("babbage") || id.contains("curie") ||
                        id.contains("realtime") || id.contains("transcription")
                    if (isNonChat) continue
                }

                list.add(
                    DiscoveredModel(
                        id = id,
                        name = name,
                        description = description,
                        isFree = isFree,
                        contextLength = contextLength,
                        provider = prov,
                        promptPrice = if (promptPrice != null && promptPrice >= 0.0) promptPrice else null,
                        completionPrice = if (compPrice != null && compPrice >= 0.0) compPrice else null
                    )
                )
            }

            // If standard OpenAI, prioritize common coding models
            if (prov == ModelProvider.OPENAI) {
                list.sortByDescending {
                    when {
                        it.id.startsWith("gpt-4o") -> 100
                        it.id.startsWith("o3-mini") -> 95
                        it.id.startsWith("o1") -> 90
                        it.id.startsWith("gpt-4.5") -> 85
                        it.id.startsWith("chatgpt") -> 80
                        else -> 10
                    }
                }
            } else if (isOr) {
                // For OpenRouter, group by free first or preserve rich sorting
                list.sortByDescending { if (it.isFree) 1 else 0 }
            }

            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
