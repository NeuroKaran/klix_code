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
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : ModelProviderAdapter {

    override suspend fun sendTurn(request: ProviderTurnRequest): ProviderTurnResponse = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // System instruction
        if (request.systemPrompt.isNotBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray().put(JSONObject().put("text", request.systemPrompt))
            sysObj.put("parts", sysParts)
            root.put("system_instruction", sysObj)
        }

        // Contents
        val contentsArray = JSONArray()
        for (msg in request.messages) {
            val contentObj = JSONObject()
            contentObj.put("role", if (msg.role == MessageRole.USER) "user" else "model")
            val partsArray = JSONArray()

            if (msg.content.isNotBlank()) {
                partsArray.put(JSONObject().put("text", msg.content))
            }

            for (tc in msg.toolCalls) {
                val fcObj = JSONObject()
                fcObj.put("name", tc.name)
                try {
                    fcObj.put("args", JSONObject(tc.rawArguments))
                } catch (_: Exception) {
                    fcObj.put("args", JSONObject())
                }
                partsArray.put(JSONObject().put("functionCall", fcObj))
            }

            for (result in msg.toolResults) {
                val frObj = JSONObject()
                frObj.put("name", result.name)
                val responseObj = JSONObject()
                responseObj.put("result", if (result.isSuccess) result.output else "Error: ${result.error}")
                frObj.put("response", responseObj)
                partsArray.put(JSONObject().put("functionResponse", frObj))
            }

            if (partsArray.length() > 0) {
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
        }
        root.put("contents", contentsArray)

        // Tools
        if (request.tools.isNotEmpty()) {
            val functionDeclarations = JSONArray()
            for (tool in request.tools) {
                val decl = JSONObject()
                decl.put("name", tool.name)
                decl.put("description", tool.description)
                decl.put("parameters", tool.toJsonSchema())
                functionDeclarations.put(decl)
            }
            val toolsArray = JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations))
            root.put("tools", toolsArray)
        }

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", request.temperature.toDouble())
        root.put("generationConfig", genConfig)

        val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/${request.model}:generateContent?key=${request.apiKey}"

        val httpRequest = Request.Builder()
            .url(endpoint)
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
                throw IllegalStateException("Gemini API Error (${response.code}): $errorMsg")
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext ProviderTurnResponse(
                    textResponse = null,
                    toolCalls = emptyList(),
                    rawJson = responseBody
                )
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var textResp: String? = null
            val toolCalls = mutableListOf<ToolCallRequest>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textResp = (textResp ?: "") + part.optString("text")
                    }
                    if (part.has("functionCall")) {
                        val fc = part.getJSONObject("functionCall")
                        val name = fc.optString("name")
                        val argsObj = fc.optJSONObject("args") ?: JSONObject()
                        val rawArgs = argsObj.toString()
                        toolCalls.add(
                            ToolCallRequest(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                rawArguments = rawArgs,
                                parsedArguments = AgentTools.parseArguments(rawArgs)
                            )
                        )
                    }
                }
            }

            val usage = respJson.optJSONObject("usageMetadata")
            val promptTokens = usage?.optInt("promptTokenCount", 0) ?: 0
            val completionTokens = usage?.optInt("candidatesTokenCount", 0) ?: 0

            ProviderTurnResponse(
                textResponse = textResp,
                toolCalls = toolCalls,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                rawJson = responseBody
            )
        }
    }
}
