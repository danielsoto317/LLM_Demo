package com.dsv.llm_demo.data.repository

import com.dsv.llm_demo.data.model.ChatCompletionRequest
import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.model.ChatMessageDto
import com.dsv.llm_demo.data.network.LlmService
import javax.inject.Inject
import com.dsv.llm_demo.data.model.ChatCompletionChunkResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class LlmRepositoryImpl @Inject constructor(
    private val llmService: LlmService
) : LlmRepository {

    private val gson = Gson()

    private val systemInstruction = ChatMessageDto(
        role = "system",
        content = """
            You are a helpful AI assistant. 
            When requested to build web apps, interactive tools, or games (such as Tic-Tac-Toe), always provide self-contained, working HTML/JS code wrapped in standard ```html or ```js code blocks so that the app can display an interactive preview to the user.
        """.trimIndent()
    )

    override fun streamLlmResponse(
        history: List<ChatMessage>,
        model: String,
        reasoningEffort: String?
    ): Flow<String> = flow {
        // Map user/assistant chat history
        val userHistoryDtos = history.map { msg ->
            ChatMessageDto(
                role = if (msg.isFromUser) "user" else "assistant",
                content = msg.text
            )
        }

        // Prepend system message at the start of the list
        val fullMessagesList = listOf(systemInstruction) + userHistoryDtos

        // Reasoning model check
        val isReasoningModel = model.contains("o1") || model.contains("o3")
        val validReasoningEffort = if (isReasoningModel && reasoningEffort != "none") {
            reasoningEffort
        } else {
            null
        }

        val request = ChatCompletionRequest(
            model = model,
            messages = fullMessagesList, // Send updated list
            stream = true,
            reasoningEffort = validReasoningEffort
        )

        val responseBody = llmService.streamMessage(request)
        val reader = responseBody.charStream().buffered()
        reader.useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") return@forEach
                    runCatching {
                        val chunk = gson.fromJson(data, ChatCompletionChunkResponse::class.java)
                        val textDelta = chunk.choices?.firstOrNull()?.delta?.content
                        if (!textDelta.isNullOrEmpty()) {
                            emit(textDelta)
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}