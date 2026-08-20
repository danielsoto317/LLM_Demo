package com.dsv.llm_demo.data.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessageDto>,
    @SerializedName("stream") val stream: Boolean = true,
    @SerializedName("reasoning") val reasoningEffort: String? = null,
    @SerializedName("tools") val tools: List<ToolDefinition>? = null,
)

data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: JsonElement,
)

/**
 * Plain-text message content. OpenAI-compatible APIs accept `content` as either a string or an
 * array of content parts; a raw JsonPrimitive keeps existing text-only requests unchanged.
 */
fun textContent(text: String): JsonElement = JsonPrimitive(text)

/**
 * Multimodal message content (text + an attached image). Built as a raw JSON tree rather than
 * polymorphic data classes because Gson resolves a field's element type once for the whole
 * list — a `List<ContentPart>` field would serialize every element using the abstract base
 * type's own (empty) fields instead of the concrete subtype's, silently emitting `{}`.
 */
fun multimodalContent(
    text: String,
    imageDataUri: String,
): JsonElement =
    JsonArray().apply {
        add(
            JsonObject().apply {
                addProperty("type", "text")
                addProperty("text", text)
            },
        )
        add(
            JsonObject().apply {
                addProperty("type", "image_url")
                add(
                    "image_url",
                    JsonObject().apply { addProperty("url", imageDataUri) },
                )
            },
        )
    }

data class ToolDefinition(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: FunctionDefinition,
)

data class FunctionDefinition(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: JsonObject,
)

data class ChatCompletionChunkResponse(
    @SerializedName("choices") val choices: List<ChunkChoice>?,
)

data class ChunkChoice(
    @SerializedName("delta") val delta: ChunkDelta?,
    @SerializedName("finish_reason") val finishReason: String?,
)

data class ChunkDelta(
    @SerializedName("content") val content: String?,
    @SerializedName("tool_calls") val toolCalls: List<ToolCallDelta>?,
)

data class ToolCallDelta(
    @SerializedName("index") val index: Int,
    @SerializedName("id") val id: String?,
    @SerializedName("function") val function: FunctionCallDelta?,
)

data class FunctionCallDelta(
    @SerializedName("name") val name: String?,
    @SerializedName("arguments") val arguments: String?,
)
