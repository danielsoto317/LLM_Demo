package com.dsv.llm_demo.data.repository

import com.dsv.llm_demo.data.model.ChatCompletionRequest
import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.model.ChatMessageDto
import com.dsv.llm_demo.data.network.LlmService
import javax.inject.Inject

class LlmRepositoryImpl @Inject constructor(
    private val llmService: LlmService
) : LlmRepository {

    override suspend fun fetchLlmResponse(
        history: List<ChatMessage>,
        model: String
    ): Result<String> {
        return runCatching {
            val dtoList = history.map { msg ->
                ChatMessageDto(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = msg.text
                )
            }

            val request = ChatCompletionRequest(
                model = model,
                messages = dtoList
            )

            val response = llmService.sendMessage(request)
            response.choices?.firstOrNull()?.message?.content
                ?: throw Exception("No response received from model.")
        }
    }
}