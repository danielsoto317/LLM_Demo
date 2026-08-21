package com.dsv.llm_demo.data.repository

import com.dsv.llm_demo.data.model.ChatCompletionChunkResponse
import com.dsv.llm_demo.data.model.ChatCompletionRequest
import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.model.ChatMessageDto
import com.dsv.llm_demo.data.model.FunctionDefinition
import com.dsv.llm_demo.data.model.LlmStreamEvent
import com.dsv.llm_demo.data.model.ToolDefinition
import com.dsv.llm_demo.data.model.multimodalContent
import com.dsv.llm_demo.data.model.textContent
import com.dsv.llm_demo.data.network.LlmService
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LlmRepositoryImpl
    @Inject
    constructor(
        private val llmService: LlmService,
    ) : LlmRepository {
        private val gson = Gson()

        private val systemInstruction =
            ChatMessageDto(
                role = "system",
                content =
                    textContent(
                        """
                        You are a helpful AI assistant.
                        When requested to build web apps, interactive tools, or games (such as Tic-Tac-Toe), always provide self-contained, working HTML/JS code wrapped in standard ```html or ```js code blocks so that the app can display an interactive preview to the user.
                        Use the open_camera tool when the user asks you to look at, describe, or identify something in front of them (for example "what is this?" or "describe what's in front of me").
                        Use the send_email tool when the user asks you to send an email. If they haven't given you a recipient address, or subject, or body, use it anyway with those fields empty.
                        """.trimIndent(),
                    ),
            )

        private val availableTools =
            listOf(
                ToolDefinition(
                    function =
                        FunctionDefinition(
                            name = "open_camera",
                            description =
                                "Opens the device camera to take a photo of what's in front of the user, " +
                                    "so it can be analyzed and described.",
                            parameters =
                                JsonObject().apply {
                                    addProperty("type", "object")
                                    add("properties", JsonObject())
                                },
                        ),
                ),
                ToolDefinition(
                    function =
                        FunctionDefinition(
                            name = "send_email",
                            description =
                                "Opens the device's email app with a new message pre-filled with the " +
                                    "given recipient, subject, and body, some of the fields may be empty.",
                            parameters =
                                JsonObject().apply {
                                    addProperty("type", "object")
                                    add(
                                        "properties",
                                        JsonObject().apply {
                                            add(
                                                "to",
                                                JsonObject().apply {
                                                    addProperty("type", "string")
                                                    addProperty("description", "Recipient email address")
                                                },
                                            )
                                            add(
                                                "subject",
                                                JsonObject().apply {
                                                    addProperty("type", "string")
                                                    addProperty("description", "Email subject line")
                                                },
                                            )
                                            add(
                                                "body",
                                                JsonObject().apply {
                                                    addProperty("type", "string")
                                                    addProperty("description", "Email body text")
                                                },
                                            )
                                        },
                                    )
                                    add("required", JsonArray().apply { add("to") })
                                },
                        ),
                ),
            )

        override fun streamLlmResponse(
            history: List<ChatMessage>,
            model: String,
            reasoningEffort: String?,
        ): Flow<LlmStreamEvent> =
            flow {
                // Map user/assistant chat history
                val userHistoryDtos =
                    history.map { msg ->
                        ChatMessageDto(
                            role = if (msg.isFromUser) "user" else "assistant",
                            content = msg.imageDataUri?.let { multimodalContent(msg.text, it) } ?: textContent(msg.text),
                        )
                    }

                // Prepend system message at the start of the list
                val fullMessagesList = listOf(systemInstruction) + userHistoryDtos

                // Reasoning model check
                val isReasoningModel = model.contains("o1") || model.contains("o3")
                val validReasoningEffort =
                    if (isReasoningModel && reasoningEffort != "none") {
                        reasoningEffort
                    } else {
                    null
                    }

                val request =
                    ChatCompletionRequest(
                        model = model,
                        messages = fullMessagesList, // Send updated list
                        stream = true,
                        reasoningEffort = validReasoningEffort,
                        tools = availableTools,
                    )

                // Accumulates streamed tool_call deltas by index until finish_reason arrives.
                val toolCallNames = mutableMapOf<Int, String>()
                val toolCallArgs = mutableMapOf<Int, StringBuilder>()
                var toolCallsFinalized = false

                val finalizeToolCalls: suspend () -> Unit = {
                    if (!toolCallsFinalized) {
                        toolCallsFinalized = true
                        toolCallNames.forEach { (index, name) ->
                            val argsJson = toolCallArgs[index]?.toString()?.ifBlank { "{}" } ?: "{}"
                            emit(LlmStreamEvent.ToolCall(name, argsJson))
                        }
                    }
                }

                val responseBody = llmService.streamMessage(request)
                val reader = responseBody.charStream().buffered()
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") return@forEach
                            val chunk =
                                runCatching {
                                    gson.fromJson(data, ChatCompletionChunkResponse::class.java)
                                }.getOrNull()
                            val choice = chunk?.choices?.firstOrNull()

                            val textDelta = choice?.delta?.content
                            if (!textDelta.isNullOrEmpty()) {
                                emit(LlmStreamEvent.TextDelta(textDelta))
                            }

                            choice?.delta?.toolCalls?.forEach { toolCallDelta ->
                                toolCallDelta.function?.name?.let { name ->
                                    toolCallNames[toolCallDelta.index] = name
                                }
                                toolCallDelta.function?.arguments?.let { argsFragment ->
                                    toolCallArgs.getOrPut(toolCallDelta.index) { StringBuilder() }.append(argsFragment)
                                }
                            }

                            if (choice?.finishReason != null) {
                                finalizeToolCalls()
                            }
                        }
                    }
                }
                finalizeToolCalls()
            }.flowOn(Dispatchers.IO)
    }
