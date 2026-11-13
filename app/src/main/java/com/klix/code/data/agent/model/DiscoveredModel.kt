package com.klix.code.data.agent.model

import org.json.JSONObject

data class DiscoveredModel(
    val id: String,
    val name: String,
    val description: String? = null,
    val isFree: Boolean = false,
    val contextLength: Int? = null,
    val provider: ModelProvider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE,
    val promptPrice: Double? = null,
    val completionPrice: Double? = null
) {
    val displayName: String
        get() = name.ifBlank { id }

    val pricingCategory: String
        get() = if (isFree) "Free" else "Paid"

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        if (description != null) obj.put("description", description)
        obj.put("isFree", isFree)
        if (contextLength != null) obj.put("contextLength", contextLength)
        obj.put("provider", provider.name)
        if (promptPrice != null) obj.put("promptPrice", promptPrice)
        if (completionPrice != null) obj.put("completionPrice", completionPrice)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): DiscoveredModel {
            val providerStr = obj.optString("provider", ModelProvider.CUSTOM_OPENAI_COMPATIBLE.name)
            val prov = try {
                ModelProvider.valueOf(providerStr)
            } catch (_: Exception) {
                ModelProvider.CUSTOM_OPENAI_COMPATIBLE
            }
            return DiscoveredModel(
                id = obj.getString("id"),
                name = obj.optString("name", obj.optString("id")),
                description = obj.optString("description").takeIf { it.isNotBlank() },
                isFree = obj.optBoolean("isFree", false),
                contextLength = if (obj.has("contextLength")) obj.optInt("contextLength") else null,
                provider = prov,
                promptPrice = if (obj.has("promptPrice")) obj.optDouble("promptPrice") else null,
                completionPrice = if (obj.has("completionPrice")) obj.optDouble("completionPrice") else null
            )
        }
    }
}

