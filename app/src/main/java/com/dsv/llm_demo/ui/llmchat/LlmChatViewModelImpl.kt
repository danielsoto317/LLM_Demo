package com.dsv.llm_demo.ui.llmchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.repository.LlmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
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
            val assistantMsgId = UUID.randomUUID().toString()

            repository.streamLlmResponse(updatedMessages, currentModel)
                .catch { error ->
                    val errorMsg = ChatMessage(text = "Error: ${error.message}", isFromUser = false)
                    _uiState.update {
                        it.copy(messages = it.messages + errorMsg, isLoading = false)
                    }
                }
                .collect { chunk ->
                    _uiState.update { state ->
                        val currentList = state.messages.toMutableList()
                        val index = currentList.indexOfFirst { it.id == assistantMsgId }

                        if (index != -1) {
                            val existingMsg = currentList[index]
                            currentList[index] = existingMsg.copy(text = existingMsg.text + chunk)
                        } else {
                            currentList.add(ChatMessage(id = assistantMsgId, text = chunk, isFromUser = false))
                        }

                        state.copy(
                            messages = currentList,
                            isLoading = false
                        )
                    }
                }
        }
    }
}