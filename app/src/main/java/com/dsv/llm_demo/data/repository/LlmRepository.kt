package com.dsv.llm_demo.data.repository

import com.dsv.llm_demo.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface LlmRepository {
    fun streamLlmResponse(
        history: List<ChatMessage>,
        model: String
    ): Flow<String>
}