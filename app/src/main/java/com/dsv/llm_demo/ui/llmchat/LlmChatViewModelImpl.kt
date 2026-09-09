package com.dsv.llm_demo.ui.llmchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.model.LlmStreamEvent
import com.dsv.llm_demo.data.model.Resource
import com.dsv.llm_demo.data.repository.LlmRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val VISION_FOLLOWUP_MODEL = "openai/gpt-4o-mini"

private fun JsonObject.stringOrNull(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString

@HiltViewModel
class LlmChatViewModelImpl
    @Inject
    constructor(
        private val repository: LlmRepository,
    ) : ViewModel(),
        LlmChatViewModel {
        private val _uiState = MutableStateFlow(LlmChatUiState())
        override val uiState: StateFlow<LlmChatUiState> = _uiState.asStateFlow()

        private val _toolActions = Channel<ToolAction>(Channel.BUFFERED)
        override val toolActions = _toolActions.receiveAsFlow()

        private val gson = Gson()

        override fun onInputTextChanged(newText: String) {
            _uiState.update { it.copy(inputText = newText) }
        }

        override fun onModelSelected(newModelId: String) {
            _uiState.update { it.copy(selectedModel = newModelId) }
        }

        override fun onReasoningEffortSelected(effort: String) {
            _uiState.update { it.copy(selectedReasoningEffort = effort) }
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
                    isLoading = true,
                )
            }

            val currentModel = _uiState.value.selectedModel
            viewModelScope.launch {
                streamAndAppend(updatedMessages, currentModel)
            }
        }

        override fun onPhotoCaptured(imageBytes: ByteArray) {
            val dataUri =
                "data:image/jpeg;base64," +
                    java.util.Base64
                        .getEncoder()
                        .encodeToString(imageBytes)
            val photoMsg =
                ChatMessage(
                    text = "Attached is the photo captured from the camera. Please describe what you see in detail.",
                    isFromUser = true,
                    imageDataUri = dataUri,
                )
            val updatedMessages = _uiState.value.messages + photoMsg

            _uiState.update {
                it.copy(messages = updatedMessages, isLoading = true)
            }

            viewModelScope.launch {
                streamAndAppend(updatedMessages, VISION_FOLLOWUP_MODEL)
            }
        }

        private suspend fun streamAndAppend(
            history: List<ChatMessage>,
            model: String,
        ) {
            val assistantMsgId = UUID.randomUUID().toString()

            repository
                .streamLlmResponse(history, model)
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> Unit
                        is Resource.Success ->
                            when (val event = resource.data) {
                                is LlmStreamEvent.TextDelta -> appendTextDelta(assistantMsgId, event.text)
                                is LlmStreamEvent.ToolCall -> handleToolCall(event)
                            }

                        is Resource.Error -> {
                            val errorMsg = ChatMessage(text = "Error: ${resource.error.message}", isFromUser = false)
                            _uiState.update {
                                it.copy(messages = it.messages + errorMsg, isLoading = false)
                            }
                        }
                    }
                }
        }

        private fun appendTextDelta(
            assistantMsgId: String,
            textDelta: String,
        ) {
            _uiState.update { state ->
                val currentList = state.messages.toMutableList()
                val index = currentList.indexOfFirst { it.id == assistantMsgId }

                if (index != -1) {
                    val existingMsg = currentList[index]
                    currentList[index] = existingMsg.copy(text = existingMsg.text + textDelta)
                } else {
                    currentList.add(ChatMessage(id = assistantMsgId, text = textDelta, isFromUser = false))
                }

                state.copy(messages = currentList, isLoading = false)
            }
        }

        private suspend fun handleToolCall(toolCall: LlmStreamEvent.ToolCall) {
            when (toolCall.name) {
                "open_camera" -> {
                    _toolActions.send(ToolAction.OpenCamera)
                    appendConfirmationMessage("📷 Opening camera…")
                }
                "send_email" -> {
                    val args = runCatching { gson.fromJson(toolCall.argumentsJson, JsonObject::class.java) }.getOrNull()
                    val to = args?.stringOrNull("to")?.trim().orEmpty()

                    if (to.isBlank()) {
                        appendConfirmationMessage("I need a recipient email address before I can send that.")
                    } else {
                        val subject = args?.stringOrNull("subject").orEmpty()
                        val body = args?.stringOrNull("body").orEmpty()
                        _toolActions.send(ToolAction.SendEmail(to, subject, body))
                        appendConfirmationMessage("📧 Opening email app…")
                    }
                }
            }
        }

        private fun appendConfirmationMessage(text: String) {
            val confirmationMsg = ChatMessage(text = text, isFromUser = false)
            _uiState.update {
                it.copy(messages = it.messages + confirmationMsg, isLoading = false)
            }
        }
    }
