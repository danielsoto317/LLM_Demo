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

    override fun streamLlmResponse(
        history: List<ChatMessage>,
        model: String
    ): Flow<String> = flow {
        val dtoList = history.map { msg ->
            ChatMessageDto(
                role = if (msg.isFromUser) "user" else "assistant",
                content = msg.text
            )
        }

        val request = ChatCompletionRequest(
            model = model,
            messages = dtoList,
            stream = true
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