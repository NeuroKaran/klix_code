package com.klix.code

import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.agent.provider.AnthropicAdapter
import com.klix.code.data.agent.provider.GeminiAdapter
import com.klix.code.data.agent.provider.OpenAIAdapter
import com.klix.code.data.agent.provider.ProviderAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAdapterTest {

    @Test
    fun testProviderAdapterFactoryResolution() {
        val anthropic = ProviderAdapterFactory.getAdapter(ModelProvider.ANTHROPIC)
        assertTrue(anthropic is AnthropicAdapter)

        val openAI = ProviderAdapterFactory.getAdapter(ModelProvider.OPENAI)
        assertTrue(openAI is OpenAIAdapter)

        val custom = ProviderAdapterFactory.getAdapter(ModelProvider.CUSTOM_OPENAI_COMPATIBLE)
        assertTrue(custom is OpenAIAdapter)

        val gemini = ProviderAdapterFactory.getAdapter(ModelProvider.GEMINI)
        assertTrue(gemini is GeminiAdapter)
    }

    @Test
    fun testProviderSettingsDefaults() {
        val settings = ProviderSettings()
        assertEquals(ModelProvider.ANTHROPIC, settings.activeProvider)
        assertEquals("claude-3-7-sonnet-20250219", settings.activeModel)
        assertTrue(settings.autoApproveReadTools)
        assertEquals(false, settings.autoApproveWriteTools)
    }

    @Test
    fun testModelProviderConfigurations() {
        for (provider in ModelProvider.entries) {
            assertNotNull(provider.displayName)
            assertNotNull(provider.defaultModel)
            assertNotNull(provider.defaultEndpoint)
            assertTrue(provider.supportedModels.isNotEmpty())
            assertTrue(provider.supportedModels.contains(provider.defaultModel))
        }
    }
}
