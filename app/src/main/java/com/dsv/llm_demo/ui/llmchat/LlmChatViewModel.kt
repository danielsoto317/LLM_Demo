package com.dsv.llm_demo.ui.llmchat

import com.dsv.llm_demo.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LlmChatViewModel {
    val uiState: StateFlow<LlmChatUiState>
    val toolActions: Flow<ToolAction>

    fun onInputTextChanged(newText: String)

    fun sendMessage()

    fun onModelSelected(newModelId: String)

    fun onReasoningEffortSelected(effort: String)

    fun onPhotoCaptured(imageBytes: ByteArray)
}

sealed interface ToolAction {
    data object OpenCamera : ToolAction

    data class SendEmail(
        val to: String,
        val subject: String,
        val body: String,
    ) : ToolAction
}

data class LlmChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val selectedModel: String = "openai/gpt-4o-mini",
    val selectedReasoningEffort: String = "none",
)
