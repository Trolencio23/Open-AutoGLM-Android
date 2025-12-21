package com.autoglm.android.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelConfig(
    val baseUrl: String = "https://open.bigmodel.cn/api/paas/v4",
    val apiKey: String = "",
    val modelName: String = "autoglm-phone",
    val maxTokens: Int = 3000,
    val temperature: Double = 0.0,
    val topP: Double = 0.85,
    val frequencyPenalty: Double = 0.2,
    val lang: String = "cn"
)

data class ModelResponse(
    val thinking: String,
    val action: String,
    val rawContent: String,
    val timeToFirstToken: Long? = null,
    val timeToThinkingEnd: Long? = null,
    val totalTime: Long? = null
)

data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionChunk(
    val id: String? = null,
    val choices: List<ChunkChoice>? = null
)

@Serializable
data class ChunkChoice(
    val index: Int = 0,
    val delta: ChunkDelta? = null,
    val finish_reason: String? = null
)

@Serializable
data class ChunkDelta(
    val role: String? = null,
    val content: String? = null
)
