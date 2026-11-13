package com.klix.code.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.klix.code.data.agent.model.DiscoveredModel
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings

interface KeyManager {
    fun getApiKey(provider: ModelProvider): String?
    fun setApiKey(provider: ModelProvider, key: String?)
    fun hasApiKey(provider: ModelProvider): Boolean
    fun getSettings(): ProviderSettings
    fun updateSettings(settings: ProviderSettings)
    fun clearAllKeys()
    fun getCachedModels(provider: ModelProvider): List<DiscoveredModel>? = null
    fun setCachedModels(provider: ModelProvider, models: List<DiscoveredModel>) {}
}

class KeyManagerImpl(private val context: Context) : KeyManager {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "klix_secure_keys",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for testing environments or devices without KeyStore support
            context.getSharedPreferences("klix_keys_fallback", Context.MODE_PRIVATE)
        }
    }

    override fun getApiKey(provider: ModelProvider): String? {
        val key = prefs.getString("key_${provider.name}", null)
        return if (key.isNullOrBlank()) null else key.trim()
    }

    override fun setApiKey(provider: ModelProvider, key: String?) {
        prefs.edit().apply {
            if (key.isNullOrBlank()) {
                remove("key_${provider.name}")
            } else {
                putString("key_${provider.name}", key.trim())
            }
        }.apply()
    }

    override fun hasApiKey(provider: ModelProvider): Boolean {
        return !getApiKey(provider).isNullOrBlank()
    }

    override fun getSettings(): ProviderSettings {
        val providerName = prefs.getString("active_provider", ModelProvider.ANTHROPIC.name)
        val provider = try {
            ModelProvider.valueOf(providerName ?: ModelProvider.ANTHROPIC.name)
        } catch (_: Exception) {
            ModelProvider.ANTHROPIC
        }

        val model = prefs.getString("active_model", provider.defaultModel) ?: provider.defaultModel
        val endpoint = prefs.getString("custom_endpoint", ModelProvider.CUSTOM_OPENAI_COMPATIBLE.defaultEndpoint)
            ?: ModelProvider.CUSTOM_OPENAI_COMPATIBLE.defaultEndpoint
        val autoRead = prefs.getBoolean("auto_approve_read", true)
        val autoWrite = prefs.getBoolean("auto_approve_write", false)
        val autoVerify = prefs.getBoolean("auto_verify_edits", true)
        val temp = prefs.getFloat("temperature", 0.2f)
        val thinkingBudget = prefs.getInt("thinking_budget", 2048)

        return ProviderSettings(
            activeProvider = provider,
            activeModel = model,
            customEndpointUrl = endpoint,
            autoApproveReadTools = autoRead,
            autoApproveWriteTools = autoWrite,
            autoVerifyEdits = autoVerify,
            temperature = temp,
            thinkingBudget = thinkingBudget
        )
    }

    override fun updateSettings(settings: ProviderSettings) {
        prefs.edit()
            .putString("active_provider", settings.activeProvider.name)
            .putString("active_model", settings.activeModel)
            .putString("custom_endpoint", settings.customEndpointUrl)
            .putBoolean("auto_approve_read", settings.autoApproveReadTools)
            .putBoolean("auto_approve_write", settings.autoApproveWriteTools)
            .putBoolean("auto_verify_edits", settings.autoVerifyEdits)
            .putFloat("temperature", settings.temperature)
            .putInt("thinking_budget", settings.thinkingBudget)
            .apply()
    }

    override fun clearAllKeys() {
        val editor = prefs.edit()
        ModelProvider.entries.forEach { provider ->
            editor.remove("key_${provider.name}")
        }
        editor.apply()
    }

    override fun getCachedModels(provider: ModelProvider): List<DiscoveredModel>? {
        val raw = prefs.getString("cached_models_${provider.name}", null) ?: return null
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<DiscoveredModel>()
            for (i in 0 until arr.length()) {
                list.add(DiscoveredModel.fromJson(arr.getJSONObject(i)))
            }
            if (list.isEmpty()) null else list
        } catch (_: Exception) {
            null
        }
    }

    override fun setCachedModels(provider: ModelProvider, models: List<DiscoveredModel>) {
        try {
            val arr = org.json.JSONArray()
            models.forEach { arr.put(it.toJson()) }
            prefs.edit().putString("cached_models_${provider.name}", arr.toString()).apply()
        } catch (_: Exception) {}
    }
}
