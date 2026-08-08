package com.dsv.llm_demo.ui.llmchat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.ui.HiltTestActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LlmChatFragmentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Test
    fun fragmentLaunchesAndRendersChatScreen() {
        val viewModel = mockLlmChatViewModel()

        launchLlmChatFragment(viewModel)

        composeTestRule.onNodeWithText("LLM Assistant").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Welcome to LLM Chat").assertIsDisplayed()
    }

    @Test
    fun clickingSendInFragmentTriggersViewModelSendMessage() {
        val viewModel = mockLlmChatViewModel(inputText = "Hello from Fragment test")

        launchLlmChatFragment(viewModel)

        composeTestRule.onNodeWithContentDescription("Send message").performClick()

        verify(exactly = 1) { viewModel.sendMessage() }
    }

    private fun mockLlmChatViewModel(
        inputText: String = "",
        messages: List<ChatMessage> = listOf(
            ChatMessage(text = "Welcome to LLM Chat", isFromUser = false)
        )
    ): LlmChatViewModel = mockk<LlmChatViewModel>(relaxed = true).also { vm ->
        every { vm.uiState } returns MutableStateFlow(
            LlmChatUiState(
                inputText = inputText,
                messages = messages
            )
        )
    }

    private fun launchLlmChatFragment(viewModel: LlmChatViewModel) {
        val testFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return LlmChatFragment { _ -> viewModel }
            }
        }

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.supportFragmentManager.fragmentFactory = testFactory
            val fragment = testFactory.instantiate(
                LlmChatFragment::class.java.classLoader!!,
                LlmChatFragment::class.java.name
            )
            composeTestRule.activity.supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()
        }
    }
}