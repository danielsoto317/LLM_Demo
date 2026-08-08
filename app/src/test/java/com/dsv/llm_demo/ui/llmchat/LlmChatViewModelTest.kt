package com.dsv.llm_demo.ui.llmchat

import com.dsv.llm_demo.data.model.ChatMessage
import com.dsv.llm_demo.data.repository.LlmRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LlmChatViewModelTest {

    private val repository: LlmRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: LlmChatViewModelImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LlmChatViewModelImpl(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onInputTextChanged updates inputText in uiState`() {
        viewModel.onInputTextChanged("Hello World")

        assertEquals("Hello World", viewModel.uiState.value.inputText)
    }

    @Test
    fun `onModelSelected updates selectedModel in uiState`() {
        viewModel.onModelSelected("anthropic/claude-3.5-sonnet")

        assertEquals("anthropic/claude-3.5-sonnet", viewModel.uiState.value.selectedModel)
    }

    @Test
    fun `sendMessage appends user message, clears input, and streams repository tokens`() = runTest {
        // Arrange
        val userPrompt = "What is Kotlin?"
        val streamChunks = listOf("Kotlin is ", "a modern ", "programming language.")
        every { repository.streamLlmResponse(any(), any()) } returns flowOf(*streamChunks.toTypedArray())

        viewModel.onInputTextChanged(userPrompt)

        // Act
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)

        // Verify User Message
        assertEquals(userPrompt, state.messages[0].text)
        assertEquals(true, state.messages[0].isFromUser)

        // Verify Streamed Assistant Message
        assertEquals("Kotlin is a modern programming language.", state.messages[1].text)
        assertEquals(false, state.messages[1].isFromUser)

        verify(exactly = 1) { repository.streamLlmResponse(any(), "openai/gpt-4o-mini") }
    }

    @Test
    fun `sendMessage does nothing when input text is blank`() {
        viewModel.onInputTextChanged("   ")
        viewModel.sendMessage()

        assertEquals(0, viewModel.uiState.value.messages.size)
        verify(exactly = 0) { repository.streamLlmResponse(any(), any()) }
    }
}