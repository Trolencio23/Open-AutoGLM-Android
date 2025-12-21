package com.autoglm.android.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class ModelClient(private val config: ModelConfig) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    suspend fun request(messages: List<Map<String, Any>>): ModelResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var timeToFirstToken: Long? = null
        var timeToThinkingEnd: Long? = null
        
        val requestBody = buildRequestBody(messages)
        val request = buildRequest(requestBody)
        
        val rawContent = StringBuilder()
        var inActionPhase = false
        val actionMarkers = listOf("finish(message=", "do(action=")
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API request failed: ${response.code} ${response.message}")
            }
            
            val source = response.body?.source() ?: throw Exception("Empty response body")
            
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                
                if (data == "[DONE]") break
                if (data.isEmpty()) continue
                
                try {
                    val chunk = json.decodeFromString<ChatCompletionChunk>(data)
                    val content = chunk.choices?.firstOrNull()?.delta?.content ?: continue
                    
                    rawContent.append(content)
                    
                    if (timeToFirstToken == null) {
                        timeToFirstToken = System.currentTimeMillis() - startTime
                    }
                    
                    if (!inActionPhase) {
                        for (marker in actionMarkers) {
                            if (rawContent.contains(marker)) {
                                inActionPhase = true
                                if (timeToThinkingEnd == null) {
                                    timeToThinkingEnd = System.currentTimeMillis() - startTime
                                }
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip malformed chunks
                }
            }
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        val (thinking, action) = parseResponse(rawContent.toString())
        
        ModelResponse(
            thinking = thinking,
            action = action,
            rawContent = rawContent.toString(),
            timeToFirstToken = timeToFirstToken,
            timeToThinkingEnd = timeToThinkingEnd,
            totalTime = totalTime
        )
    }
    
    fun requestStream(messages: List<Map<String, Any>>): Flow<String> = callbackFlow {
        val requestBody = buildRequestBody(messages)
        val request = buildRequest(requestBody)
        
        val eventSourceFactory = EventSources.createFactory(client)
        
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                
                try {
                    val chunk = json.decodeFromString<ChatCompletionChunk>(data)
                    val content = chunk.choices?.firstOrNull()?.delta?.content
                    if (content != null) {
                        trySend(content)
                    }
                } catch (e: Exception) {
                    // Skip malformed chunks
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                close(t ?: Exception("SSE connection failed"))
            }
            
            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }
        
        val eventSource = eventSourceFactory.newEventSource(request, listener)
        
        awaitClose {
            eventSource.cancel()
        }
    }
    
    private fun buildRequestBody(messages: List<Map<String, Any>>): String {
        val messagesJsonArray = buildJsonArray {
            for (message in messages) {
                add(mapToJsonObject(message))
            }
        }
        
        val requestData = buildJsonObject {
            put("model", config.modelName)
            put("messages", messagesJsonArray)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature)
            put("top_p", config.topP)
            put("frequency_penalty", config.frequencyPenalty)
            put("stream", true)
        }
        return requestData.toString()
    }
    
    private fun mapToJsonObject(map: Map<String, Any>): JsonObject {
        return buildJsonObject {
            for ((key, value) in map) {
                put(key, anyToJsonElement(value))
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> mapToJsonObject(value as Map<String, Any>)
            is List<*> -> buildJsonArray {
                for (item in value) {
                    add(anyToJsonElement(item))
                }
            }
            else -> JsonPrimitive(value.toString())
        }
    }
    
    private fun buildRequest(body: String): Request {
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        return Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    private fun parseResponse(content: String): Pair<String, String> {
        // Rule 1: Check for finish(message=
        if ("finish(message=" in content) {
            val parts = content.split("finish(message=", limit = 2)
            val thinking = parts[0].trim()
            val action = "finish(message=" + parts[1]
            return thinking to action
        }
        
        // Rule 2: Check for do(action=
        if ("do(action=" in content) {
            val parts = content.split("do(action=", limit = 2)
            val thinking = parts[0].trim()
            val action = "do(action=" + parts[1]
            return thinking to action
        }
        
        // Rule 3: Fallback to legacy XML tag parsing
        if ("<answer>" in content) {
            val parts = content.split("<answer>", limit = 2)
            val thinking = parts[0].replace("<think>", "").replace("</think>", "").trim()
            val action = parts[1].replace("</answer>", "").trim()
            return thinking to action
        }
        
        // Rule 4: No markers found
        return "" to content
    }
}
