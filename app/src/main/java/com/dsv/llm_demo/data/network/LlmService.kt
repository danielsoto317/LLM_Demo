package com.dsv.llm_demo.data.network

import com.dsv.llm_demo.data.model.ChatCompletionRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface LlmService {
    @Streaming
    @POST("chat/completions")
    suspend fun streamMessage(
        @Body request: ChatCompletionRequest
    ): ResponseBody
}