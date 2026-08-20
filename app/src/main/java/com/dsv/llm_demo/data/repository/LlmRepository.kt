package com.dsv.llm_demo.data.repository

import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.model.LlmStreamEvent
import kotlinx.coroutines.flow.Flow

interface LlmRepository {
    fun streamLlmResponse(
        history: List<ChatMessage>,
        model: String,
        reasoningEffort: String? = null,
    ): Flow<LlmStreamEvent>
}
