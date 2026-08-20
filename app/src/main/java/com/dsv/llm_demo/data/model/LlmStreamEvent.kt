package com.dsv.llm_demo.data.model

sealed interface LlmStreamEvent {
    data class TextDelta(
        val text: String,
    ) : LlmStreamEvent

    data class ToolCall(
        val name: String,
        val argumentsJson: String,
    ) : LlmStreamEvent
}
