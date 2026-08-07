package com.dsv.llm_demo.data.repository

import com.dsv.llm_demo.data.model.ChatMessage

interface LlmRepository {
    suspend fun fetchLlmResponse(
        history: List<ChatMessage>,
        model: String
    ): Result<String>
}