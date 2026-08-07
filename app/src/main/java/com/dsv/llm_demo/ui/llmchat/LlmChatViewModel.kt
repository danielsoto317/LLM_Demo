package com.dsv.llm_demo.ui.llmchat

import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.model.LlmResponse
import kotlinx.coroutines.flow.StateFlow

interface LlmChatViewModel {
    val uiState: StateFlow<LlmChatUiState>

    fun onInputTextChanged(newText: String)
    fun sendMessage()
}

data class LlmChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val selectedModel: String = "openai/gpt-4o-mini"
)