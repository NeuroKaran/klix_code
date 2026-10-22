package com.klix.code.data.agent.provider

import com.klix.code.data.agent.model.ModelProvider

object ProviderAdapterFactory {
    private val anthropicAdapter by lazy { AnthropicAdapter() }
    private val openAIAdapter by lazy { OpenAIAdapter() }
    private val geminiAdapter by lazy { GeminiAdapter() }

    fun getAdapter(provider: ModelProvider): ModelProviderAdapter {
        return when (provider) {
            ModelProvider.ANTHROPIC -> anthropicAdapter
            ModelProvider.OPENAI,
            ModelProvider.CUSTOM_OPENAI_COMPATIBLE -> openAIAdapter
            ModelProvider.GEMINI -> geminiAdapter
        }
    }
}
