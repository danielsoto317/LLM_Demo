package com.dsv.llm_demo.data.network

import com.dsv.llm_demo.data.model.ChatCompletionRequest
import com.dsv.llm_demo.data.model.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LlmService {
    @POST("chat/completions")
    suspend fun sendMessage(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}