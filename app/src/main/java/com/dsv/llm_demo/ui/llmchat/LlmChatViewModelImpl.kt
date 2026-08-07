package com.dsv.llm_demo.ui.llmchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.model.LlmResponse
import com.dsv.llm_demo.data.repository.LlmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LlmChatViewModelImpl @Inject constructor(
    private val repository: LlmRepository,
) : ViewModel(), LlmChatViewModel {

    private val _uiState = MutableStateFlow(LlmChatUiState())
    override val uiState: StateFlow<LlmChatUiState> = _uiState.asStateFlow()


    override fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun onModelSelected(newModelId: String) {
        _uiState.update { it.copy(selectedModel = newModelId) }
    }

    override fun sendMessage() {
        val userText = _uiState.value.inputText.trim()
        if (userText.isEmpty() || _uiState.value.isLoading) return

        val userMsg = ChatMessage(text = userText, isFromUser = true)
        val updatedMessages = _uiState.value.messages + userMsg

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            val currentModel = _uiState.value.selectedModel
            val result = repository.fetchLlmResponse(updatedMessages, currentModel)

            result.onSuccess { llmAnswer ->
                val llmMsg = ChatMessage(text = llmAnswer, isFromUser = false)
                _uiState.update {
                    it.copy(messages = it.messages + llmMsg, isLoading = false)
                }
            }.onFailure { error ->
                val errorMsg = ChatMessage(text = "Error: ${error.message}", isFromUser = false)
                _uiState.update {
                    it.copy(messages = it.messages + errorMsg, isLoading = false)
                }
            }
        }
    }

}