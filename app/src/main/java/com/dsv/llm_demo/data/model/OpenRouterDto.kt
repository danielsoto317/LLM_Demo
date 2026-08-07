package com.dsv.llm_demo.data.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessageDto>,
    @SerializedName("stream") val stream: Boolean = true
)

data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatCompletionChunkResponse(
    @SerializedName("choices") val choices: List<ChunkChoice>?
)

data class ChunkChoice(
    @SerializedName("delta") val delta: ChunkDelta?
)

data class ChunkDelta(
    @SerializedName("content") val content: String?
)