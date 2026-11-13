package com.klix.code.data.agent.provider

import com.klix.code.data.agent.model.DiscoveredModel
import com.klix.code.data.agent.tools.ToolCallRequest
import com.klix.code.data.agent.tools.ToolCallResult
import com.klix.code.data.agent.tools.ToolDefinition

enum class MessageRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val thought: String? = null,
    val toolCalls: List<ToolCallRequest> = emptyList(),
    val toolResults: List<ToolCallResult> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val command: String? = null
)

data class ProviderTurnRequest(
    val systemPrompt: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>,
    val model: String,
    val apiKey: String,
    val customEndpoint: String? = null,
    val temperature: Float = 0.2f,
    val thinkingBudget: Int = 0
)

data class ProviderTurnResponse(
    val textResponse: String?,
    val thoughtResponse: String? = null,
    val toolCalls: List<ToolCallRequest>,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val rawJson: String? = null
)

interface ModelProviderAdapter {
    suspend fun sendTurn(request: ProviderTurnRequest): ProviderTurnResponse
    suspend fun listModels(apiKey: String, customEndpoint: String? = null): List<DiscoveredModel> = emptyList()
}
