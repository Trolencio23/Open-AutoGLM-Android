package com.autoglm.android.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

object MessageBuilder {
    
    fun createSystemMessage(content: String): Map<String, Any> {
        return mapOf(
            "role" to "system",
            "content" to content
        )
    }
    
    fun createUserMessage(text: String, imageBase64: String? = null): Map<String, Any> {
        val contentList = mutableListOf<Map<String, Any>>()
        
        if (imageBase64 != null) {
            contentList.add(mapOf(
                "type" to "image_url",
                "image_url" to mapOf(
                    "url" to "data:image/png;base64,$imageBase64"
                )
            ))
        }
        
        contentList.add(mapOf(
            "type" to "text",
            "text" to text
        ))
        
        return mapOf(
            "role" to "user",
            "content" to contentList
        )
    }
    
    fun createAssistantMessage(content: String): Map<String, Any> {
        return mapOf(
            "role" to "assistant",
            "content" to content
        )
    }
    
    fun removeImagesFromMessage(message: Map<String, Any>): Map<String, Any> {
        val content = message["content"]
        if (content is List<*>) {
            val filteredContent = content.filterIsInstance<Map<*, *>>()
                .filter { it["type"] == "text" }
            return message.toMutableMap().apply {
                this["content"] = filteredContent
            }
        }
        return message
    }
    
    fun buildScreenInfo(currentApp: String, extraInfo: Map<String, Any> = emptyMap()): String {
        val info = mutableMapOf<String, Any>("current_app" to currentApp)
        info.putAll(extraInfo)
        
        return buildJsonObject {
            info.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, JsonPrimitive(value))
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    else -> put(key, JsonPrimitive(value.toString()))
                }
            }
        }.toString()
    }
}
