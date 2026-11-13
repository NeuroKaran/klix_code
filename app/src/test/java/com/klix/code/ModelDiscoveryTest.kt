package com.klix.code

import com.klix.code.data.agent.command.BuiltInSlashCommands
import com.klix.code.data.agent.harness.AgentHarness
import com.klix.code.data.agent.model.DiscoveredModel
import com.klix.code.data.agent.model.ModelDiscoveryRepository
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.agent.provider.AnthropicAdapter
import com.klix.code.data.agent.provider.GeminiAdapter
import com.klix.code.data.agent.provider.OpenAIAdapter
import com.klix.code.data.agent.tools.WorkspaceToolExecutor
import com.klix.code.data.repository.WorkspaceRepository
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.security.KeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class ModelDiscoveryTest {

    private class MockKeyManager : KeyManager {
        val keys = mutableMapOf<ModelProvider, String?>()
        val cached = mutableMapOf<ModelProvider, List<DiscoveredModel>>()
        private var currentSettingsState = ProviderSettings()

        override fun getApiKey(provider: ModelProvider): String? = keys[provider]
        override fun setApiKey(provider: ModelProvider, key: String?) {
            if (key == null) keys.remove(provider) else keys[provider] = key
        }
        override fun hasApiKey(provider: ModelProvider): Boolean = !keys[provider].isNullOrBlank()
        override fun getSettings(): ProviderSettings = currentSettingsState
        override fun updateSettings(settings: ProviderSettings) { this.currentSettingsState = settings }
        override fun clearAllKeys() { keys.clear() }
        override fun getCachedModels(provider: ModelProvider): List<DiscoveredModel>? = cached[provider]
        override fun setCachedModels(provider: ModelProvider, models: List<DiscoveredModel>) {
            cached[provider] = models
        }
    }

    @Test
    fun testDiscoveredModelJsonRoundTrip() {
        val original = DiscoveredModel(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            name = "Meta: Llama 3.3 70B Instruct (free)",
            description = "High quality 70B open model",
            isFree = true,
            contextLength = 131072,
            provider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE,
            promptPrice = 0.0,
            completionPrice = 0.0
        )

        val json = original.toJson()
        val parsed = DiscoveredModel.fromJson(json)

        assertEquals(original.id, parsed.id)
        assertEquals(original.name, parsed.name)
        assertEquals(original.description, parsed.description)
        assertEquals(original.isFree, parsed.isFree)
        assertEquals(original.contextLength, parsed.contextLength)
        assertEquals(original.provider, parsed.provider)
        assertEquals(original.promptPrice, parsed.promptPrice)
        assertEquals(original.completionPrice, parsed.completionPrice)
        assertEquals("Free", parsed.pricingCategory)
    }

    @Test
    fun testOpenRouterFreeModelIdentificationBySuffixAndPricingUsingAdapter() {
        val sampleJson = """
        {
          "data": [
            {
              "id": "meta-llama/llama-3.3-70b-instruct:free",
              "name": "Meta: Llama 3.3 70B Instruct (free)",
              "description": "Llama 3.3 free model",
              "context_length": 131072,
              "pricing": {
                "prompt": "0",
                "completion": "0"
              }
            },
            {
              "id": "deepseek/deepseek-r1:free",
              "name": "DeepSeek R1",
              "pricing": {
                "prompt": "0.0",
                "completion": "0.0"
              }
            },
            {
              "id": "qwen/qwen-2.5-coder-free-tier",
              "name": "Qwen 2.5 Coder (free)",
              "pricing": {
                "prompt": "0",
                "completion": "0"
              }
            },
            {
              "id": "anthropic/claude-3.5-sonnet",
              "name": "Anthropic: Claude 3.5 Sonnet",
              "context_length": 200000,
              "pricing": {
                "prompt": "0.000003",
                "completion": "0.000015"
              }
            },
            {
              "id": "openai/gpt-4o",
              "name": "OpenAI: GPT-4o",
              "pricing": {
                "prompt": "0.0000025",
                "completion": "0.00001"
              }
            }
          ]
        }
        """.trimIndent()

        val adapter = OpenAIAdapter()
        val parsedModels = adapter.parseModelsJson(
            sampleJson,
            isOr = true,
            customEndpoint = "https://openrouter.ai/api/v1/chat/completions"
        )

        assertEquals(5, parsedModels.size)

        // Free models verification
        val freeModels = parsedModels.filter { it.isFree }
        assertEquals(3, freeModels.size)
        assertTrue(freeModels.any { it.id == "meta-llama/llama-3.3-70b-instruct:free" })
        assertTrue(freeModels.any { it.id == "deepseek/deepseek-r1:free" })
        assertTrue(freeModels.any { it.id == "qwen/qwen-2.5-coder-free-tier" })

        // Paid models verification
        val paidModels = parsedModels.filter { !it.isFree }
        assertEquals(2, paidModels.size)
        assertTrue(paidModels.any { it.id == "anthropic/claude-3.5-sonnet" })
        assertTrue(paidModels.any { it.id == "openai/gpt-4o" })
        assertEquals(0.000003, paidModels.first { it.id == "anthropic/claude-3.5-sonnet" }.promptPrice ?: 0.0, 0.0000001)

        // Verify free models are grouped first in the list
        assertTrue(parsedModels[0].isFree)
        assertTrue(parsedModels[1].isFree)
        assertTrue(parsedModels[2].isFree)
    }

    @Test
    fun testOpenAIModelFilteringExcludesNonChatModelsUsingAdapter() {
        val sampleJson = """
        {
          "data": [
            { "id": "gpt-4o" },
            { "id": "gpt-4o-mini" },
            { "id": "text-embedding-3-small" },
            { "id": "whisper-1" },
            { "id": "tts-1" },
            { "id": "o3-mini" },
            { "id": "dall-e-3" }
          ]
        }
        """.trimIndent()

        val adapter = OpenAIAdapter()
        val chatModels = adapter.parseModelsJson(sampleJson, isOr = false, customEndpoint = null)

        val ids = chatModels.map { it.id }
        assertEquals(3, chatModels.size)
        assertTrue(ids.contains("gpt-4o"))
        assertTrue(ids.contains("gpt-4o-mini"))
        assertTrue(ids.contains("o3-mini"))
        assertFalse(ids.contains("text-embedding-3-small"))
        assertFalse(ids.contains("whisper-1"))
        assertFalse(ids.contains("tts-1"))
        assertFalse(ids.contains("dall-e-3"))

        // Flagship gpt-4o should be prioritized at the top
        assertEquals("gpt-4o", ids[0])
    }

    @Test
    fun testGeminiModelParsingStripsPrefixAndFiltersMethodsUsingAdapter() {
        val sampleJson = """
        {
          "models": [
            {
              "name": "models/gemini-2.5-flash",
              "displayName": "Gemini 2.5 Flash",
              "supportedGenerationMethods": ["generateContent", "countTokens"]
            },
            {
              "name": "models/gemini-2.5-pro",
              "displayName": "Gemini 2.5 Pro",
              "supportedGenerationMethods": ["generateContent"]
            },
            {
              "name": "models/text-embedding-004",
              "displayName": "Text Embedding 004",
              "supportedGenerationMethods": ["embedContent"]
            }
          ]
        }
        """.trimIndent()

        val adapter = GeminiAdapter()
        val parsed = adapter.parseModelsJson(sampleJson)

        assertEquals(2, parsed.size)
        assertEquals("gemini-2.5-flash", parsed[0].id)
        assertEquals("gemini-2.5-pro", parsed[1].id)
        assertFalse(parsed.any { it.id == "text-embedding-004" })
    }

    @Test
    fun testAnthropicModelParsingUsingAdapter() {
        val sampleJson = """
        {
          "data": [
            {
              "id": "claude-3-7-sonnet-20250219",
              "display_name": "Claude 3.7 Sonnet"
            },
            {
              "id": "claude-3-5-sonnet-20241022",
              "display_name": "Claude 3.5 Sonnet"
            }
          ]
        }
        """.trimIndent()

        val adapter = AnthropicAdapter()
        val parsed = adapter.parseModelsJson(sampleJson)

        assertEquals(2, parsed.size)
        assertEquals("claude-3-7-sonnet-20250219", parsed[0].id)
        assertEquals("Claude 3.7 Sonnet", parsed[0].name)
        assertEquals(ModelProvider.ANTHROPIC, parsed[0].provider)
        assertFalse(parsed[0].isFree)
    }

    @Test
    fun testDefaultFallbackModelsForOpenRouterIncludesBothFreeAndPaid() {
        val repo = ModelDiscoveryRepository(null)
        val defaults = repo.getDefaultModels(ModelProvider.CUSTOM_OPENAI_COMPATIBLE)

        assertTrue(defaults.isNotEmpty())

        val freeList = defaults.filter { it.isFree }
        val paidList = defaults.filter { !it.isFree }

        assertTrue("Must have free models by default", freeList.isNotEmpty())
        assertTrue("Must have paid models by default", paidList.isNotEmpty())
        assertTrue(freeList.any { it.id.contains(":free") })
        assertTrue(paidList.any { it.id == "anthropic/claude-3.5-sonnet" })
    }

    @Test
    fun testModelDiscoveryRepositoryFallbackToCacheAndDefaults() = runBlocking {
        val mockKeyManager = MockKeyManager()
        val repo = ModelDiscoveryRepository(mockKeyManager)

        // Offline / empty key, no cache yet -> should return defaults
        val defaultModels = repo.getAvailableModels(
            provider = ModelProvider.GEMINI,
            apiKey = "",
            forceRefresh = false
        )
        assertTrue(defaultModels.isNotEmpty())
        assertTrue(defaultModels.any { it.id == "gemini-2.5-flash" })

        // Prepopulate cache with custom discovered models
        val customCached = listOf(
            DiscoveredModel("gemini-test-custom", "Gemini Test Custom", provider = ModelProvider.GEMINI)
        )
        mockKeyManager.setCachedModels(ModelProvider.GEMINI, customCached)

        // Now without network, it should read from cache
        val cachedResult = repo.getAvailableModels(
            provider = ModelProvider.GEMINI,
            apiKey = "",
            forceRefresh = false
        )
        assertEquals(1, cachedResult.size)
        assertEquals("gemini-test-custom", cachedResult[0].id)
    }

    @Test
    fun testAllProvidersHaveDefaultDiscoveredModels() {
        val repo = ModelDiscoveryRepository(null)
        for (prov in ModelProvider.entries) {
            val models = repo.getDefaultModels(prov)
            assertTrue("Provider $prov must have default models", models.isNotEmpty())
            assertTrue(models.any { it.id == prov.defaultModel || prov.supportedModels.contains(it.id) })
        }
    }

    @Test
    fun testAgentHarnessWithKeyManagerCachesDiscoveredModels() = runBlocking {
        val mockKeyManager = MockKeyManager()
        val repo = ModelDiscoveryRepository(mockKeyManager)
        val mockExecutor = WorkspaceToolExecutor(mock(WorkspaceRepository::class.java), mock(SandboxExecutionProvider::class.java))
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val harness = AgentHarness(
            toolExecutor = mockExecutor,
            scope = scope,
            keyManager = mockKeyManager,
            modelDiscoveryRepository = repo
        )

        // Initially cache is empty
        assertNotNull(harness.availableModels.value)

        // Populate cache for OpenRouter
        val models = listOf(
            DiscoveredModel(
                id = "test-free:free",
                name = "Test Free",
                isFree = true,
                provider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE
            ),
            DiscoveredModel(
                id = "test-paid",
                name = "Test Paid",
                isFree = false,
                provider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE
            )
        )
        mockKeyManager.setCachedModels(ModelProvider.CUSTOM_OPENAI_COMPATIBLE, models)

        // Query models via harness
        val fetched = harness.fetchModelsForProvider(
            provider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE,
            apiKey = "test-key",
            forceRefresh = false
        )

        assertEquals(2, fetched.size)
        assertEquals("test-free:free", fetched[0].id)
        assertTrue(fetched[0].isFree)
        assertFalse(fetched[1].isFree)
    }

    @Test
    fun testBuiltInBoostSlashCommandExists() {
        val boost = BuiltInSlashCommands.ALL.find { it.trigger == "boost" }
        assertNotNull("BuiltInSlashCommands must contain 'boost'", boost)
        assertEquals("boost", boost?.trigger)
        assertTrue(boost?.isBuiltIn == true)
        assertTrue(boost?.description?.contains("performance", ignoreCase = true) == true ||
                   boost?.description?.contains("boost", ignoreCase = true) == true)
    }
}
