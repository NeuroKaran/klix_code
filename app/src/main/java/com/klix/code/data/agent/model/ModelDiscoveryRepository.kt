package com.klix.code.data.agent.model

import com.klix.code.data.agent.provider.ProviderAdapterFactory
import com.klix.code.data.security.KeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelDiscoveryRepository(
    private val keyManager: KeyManager? = null
) {
    suspend fun getAvailableModels(
        provider: ModelProvider,
        apiKey: String?,
        customEndpoint: String? = null,
        forceRefresh: Boolean = false
    ): List<DiscoveredModel> = withContext(Dispatchers.IO) {
        // If not force refresh, check cache first
        if (!forceRefresh && keyManager != null) {
            val cached = keyManager.getCachedModels(provider)
            if (!cached.isNullOrEmpty()) {
                return@withContext cached
            }
        }

        // Try network fetch via provider adapter
        var fetchedModels: List<DiscoveredModel> = emptyList()
        try {
            val adapter = ProviderAdapterFactory.getAdapter(provider)
            val effectiveEndpoint = if (provider == ModelProvider.CUSTOM_OPENAI_COMPATIBLE) {
                customEndpoint?.ifBlank { null } ?: ModelProvider.CUSTOM_OPENAI_COMPATIBLE.defaultEndpoint
            } else null
            fetchedModels = adapter.listModels(apiKey ?: "", effectiveEndpoint)
        } catch (_: Exception) {
            // Network failure or adapter error
        }

        if (fetchedModels.isNotEmpty()) {
            keyManager?.setCachedModels(provider, fetchedModels)
            return@withContext fetchedModels
        }

        // Network call failed or returned empty; fall back to cache
        if (keyManager != null) {
            val cached = keyManager.getCachedModels(provider)
            if (!cached.isNullOrEmpty()) {
                return@withContext cached
            }
        }

        // Complete fallback to default static supported models
        getDefaultModels(provider)
    }

    fun getDefaultModels(provider: ModelProvider): List<DiscoveredModel> {
        return when (provider) {
            ModelProvider.ANTHROPIC -> listOf(
                DiscoveredModel(
                    id = "claude-3-7-sonnet-20250219",
                    name = "Claude 3.7 Sonnet",
                    description = "Hybrid reasoning and coding powerhouse",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "claude-3-5-sonnet-20241022",
                    name = "Claude 3.5 Sonnet",
                    description = "Smart, fast coding and analysis",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "claude-3-5-haiku-20241022",
                    name = "Claude 3.5 Haiku",
                    description = "Fast, lightweight assistant",
                    isFree = false,
                    provider = provider
                )
            )
            ModelProvider.OPENAI -> listOf(
                DiscoveredModel(
                    id = "gpt-4o",
                    name = "GPT-4o",
                    description = "Flagship omni model for multimodal coding",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "gpt-4o-mini",
                    name = "GPT-4o Mini",
                    description = "Fast, affordable intelligent model",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "o3-mini",
                    name = "o3-mini",
                    description = "High-reasoning STEM and math coding model",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "o1",
                    name = "o1",
                    description = "Deep reasoning model for hard problems",
                    isFree = false,
                    provider = provider
                )
            )
            ModelProvider.GEMINI -> listOf(
                DiscoveredModel(
                    id = "gemini-2.5-flash",
                    name = "Gemini 2.5 Flash",
                    description = "Next-gen multimodal workhorse with thinking",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "gemini-3.1-flash-lite",
                    name = "Gemini 3.1 Flash Lite",
                    description = "Ultra-fast lightweight latency champion",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "gemini-2.5-pro",
                    name = "Gemini 2.5 Pro",
                    description = "Advanced reasoning for complex coding tasks",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "gemini-2.0-flash",
                    name = "Gemini 2.0 Flash",
                    description = "High speed multimodal model",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "gemini-1.5-pro",
                    name = "Gemini 1.5 Pro",
                    description = "1M+ context window pro model",
                    isFree = false,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "gemini-1.5-flash",
                    name = "Gemini 1.5 Flash",
                    description = "High efficiency fast model",
                    isFree = false,
                    provider = provider
                )
            )
            ModelProvider.CUSTOM_OPENAI_COMPATIBLE -> listOf(
                // Free OpenRouter Models
                DiscoveredModel(
                    id = "meta-llama/llama-3.3-70b-instruct:free",
                    name = "Meta: Llama 3.3 70B Instruct (free)",
                    description = "Open-weights 70B flagship model",
                    isFree = true,
                    contextLength = 131072,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "deepseek/deepseek-r1:free",
                    name = "DeepSeek: R1 (free)",
                    description = "DeepSeek open reasoning model",
                    isFree = true,
                    contextLength = 65536,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "google/gemini-2.0-flash-exp:free",
                    name = "Google: Gemini 2.0 Flash Exp (free)",
                    description = "Experimental next-gen Flash",
                    isFree = true,
                    contextLength = 1048576,
                    provider = provider
                ),
                DiscoveredModel(
                    id = "qwen/qwen-2.5-coder-32b-instruct:free",
                    name = "Qwen: 2.5 Coder 32B Instruct (free)",
                    description = "Code intelligence specialist",
                    isFree = true,
                    contextLength = 32768,
                    provider = provider
                ),
                // Paid Models
                DiscoveredModel(
                    id = "anthropic/claude-3.5-sonnet",
                    name = "Anthropic: Claude 3.5 Sonnet",
                    description = "Industry standard coding benchmark leader",
                    isFree = false,
                    contextLength = 200000,
                    provider = provider,
                    promptPrice = 0.000003,
                    completionPrice = 0.000015
                ),
                DiscoveredModel(
                    id = "meta-llama/llama-3.3-70b-instruct",
                    name = "Meta: Llama 3.3 70B Instruct",
                    description = "Dedicated compute 70B instruction-tuned",
                    isFree = false,
                    contextLength = 131072,
                    provider = provider,
                    promptPrice = 0.0000004,
                    completionPrice = 0.0000004
                ),
                DiscoveredModel(
                    id = "deepseek/deepseek-r1",
                    name = "DeepSeek: R1",
                    description = "High-availability DeepSeek R1 reasoning",
                    isFree = false,
                    contextLength = 65536,
                    provider = provider,
                    promptPrice = 0.00000055,
                    completionPrice = 0.00000219
                )
            )
        }
    }
}

