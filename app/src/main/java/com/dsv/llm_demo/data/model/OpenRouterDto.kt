package com.dsv.llm_demo.data.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatCompletionResponse(
    @SerializedName("choices") val choices: List<Choice>?
)

data class Choice(
    @SerializedName("message") val message: ChatMessageDto?
)