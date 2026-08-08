package com.dsv.llm_demo.ui.llmchat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dsv.llm_demo.data.model.ChatMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LlmChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTopBarTitleAndInitialMessages() {
        val messages = listOf(
            ChatMessage(text = "Hello LLM", isFromUser = true),
            ChatMessage(text = "Hello Human!", isFromUser = false)
        )
        val viewModel = mockViewModel(messages = messages)

        setScreenContent(viewModel)

        composeTestRule.onNodeWithText("LLM Assistant").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Hello LLM").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Hello Human!").assertIsDisplayed()
    }

    @Test
    fun sendButtonIsDisabledWhenInputIsBlank() {
        val viewModel = mockViewModel(inputText = "")

        setScreenContent(viewModel)

        composeTestRule.onNodeWithContentDescription("Send message").assertIsNotEnabled()
    }

    @Test
    fun sendButtonIsEnabledWhenInputIsPresent() {
        val viewModel = mockViewModel(inputText = "Hello")

        setScreenContent(viewModel)

        composeTestRule.onNodeWithContentDescription("Send message").assertIsEnabled()
    }

    @Test
    fun enteringTextTriggersOnInputTextChanged() {
        val viewModel = mockViewModel()

        setScreenContent(viewModel)

        composeTestRule.onNodeWithText("Type a message...").performTextInput("Test query")

        verify { viewModel.onInputTextChanged("Test query") }
    }

    @Test
    fun clickingSendButtonTriggersSendMessage() {
        val viewModel = mockViewModel(inputText = "Send this prompt")

        setScreenContent(viewModel)

        composeTestRule.onNodeWithContentDescription("Send message").performClick()

        verify(exactly = 1) { viewModel.sendMessage() }
    }

    private fun setScreenContent(viewModel: LlmChatViewModel) {
        composeTestRule.setContent {
            MaterialTheme {
                LlmChatScreen(viewModel = viewModel)
            }
        }
    }

    private fun mockViewModel(
        inputText: String = "",
        isLoading: Boolean = false,
        messages: List<ChatMessage> = emptyList()
    ): LlmChatViewModel = mockk<LlmChatViewModel>(relaxed = true).also { vm ->
        every { vm.uiState } returns MutableStateFlow(
            LlmChatUiState(
                inputText = inputText,
                isLoading = isLoading,
                messages = messages
            )
        )
    }
}