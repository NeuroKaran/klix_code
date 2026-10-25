package com.klix.code.data.agent.model

enum class ModelProvider(
    val displayName: String,
    val defaultModel: String,
    val defaultEndpoint: String,
    val supportedModels: List<String>
) {
    ANTHROPIC(
        displayName = "Anthropic Claude",
        defaultModel = "claude-3-7-sonnet-20250219",
        defaultEndpoint = "https://api.anthropic.com/v1/messages",
        supportedModels = listOf(
            "claude-3-7-sonnet-20250219",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022"
        )
    ),
    OPENAI(
        displayName = "OpenAI",
        defaultModel = "gpt-4o",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        supportedModels = listOf(
            "gpt-4o",
            "gpt-4o-mini",
            "o3-mini"
        )
    ),
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta",
        supportedModels = listOf(
            "gemini-2.5-flash",
            "gemini-3.1-flash-lite",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-pro",
            "gemini-1.5-flash"
        )
    ),
    CUSTOM_OPENAI_COMPATIBLE(
        displayName = "OpenAI Compatible (OpenRouter, Groq, etc.)",
        defaultModel = "anthropic/claude-3.5-sonnet",
        defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions",
        supportedModels = listOf(
            "anthropic/claude-3.5-sonnet",
            "meta-llama/llama-3.3-70b-instruct",
            "deepseek/deepseek-r1"
        )
    )
}

data class ProviderSettings(
    val activeProvider: ModelProvider = ModelProvider.ANTHROPIC,
    val activeModel: String = ModelProvider.ANTHROPIC.defaultModel,
    val customEndpointUrl: String = ModelProvider.CUSTOM_OPENAI_COMPATIBLE.defaultEndpoint,
    val autoApproveReadTools: Boolean = true,
    val autoApproveWriteTools: Boolean = false,
    val temperature: Float = 0.2f
)
