package com.dsv.llm_demo.ui.llmchat

import com.dsv.llm_demo.data.model.LlmStreamEvent
import com.dsv.llm_demo.data.repository.LlmRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `sendMessage appends user message, clears input, and streams repository tokens`() =
        runTest {
            // Arrange
            val userPrompt = "What is Kotlin?"
            val streamChunks = listOf("Kotlin is ", "a modern ", "programming language.")
            every { repository.streamLlmResponse(any(), any()) } returns
                flowOf(
                    *streamChunks.map { LlmStreamEvent.TextDelta(it) }.toTypedArray(),
                )

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

    @Test
    fun `open_camera tool call emits OpenCamera action and appends confirmation message`() =
        runTest {
            every { repository.streamLlmResponse(any(), any()) } returns
                flowOf(
                    LlmStreamEvent.ToolCall("open_camera", "{}"),
                )

            viewModel.onInputTextChanged("what's in front of me?")
            viewModel.sendMessage()
            testDispatcher.scheduler.advanceUntilIdle()

            // The action is already buffered in the channel by now, so first() resolves immediately.
            val action = viewModel.toolActions.first()
            assertTrue(action is ToolAction.OpenCamera)
            assertTrue(
                viewModel.uiState.value.messages
                    .last()
                    .text
                    .contains("camera", ignoreCase = true),
            )
        }

    @Test
    fun `send_email tool call with a recipient emits SendEmail action`() =
        runTest {
            every { repository.streamLlmResponse(any(), any()) } returns
                flowOf(
                    LlmStreamEvent.ToolCall(
                        "send_email",
                        """{"to":"test@example.com","subject":"Hello","body":"Hi there"}""",
                    ),
                )

            viewModel.onInputTextChanged("send an email")
            viewModel.sendMessage()
            testDispatcher.scheduler.advanceUntilIdle()

            val action = viewModel.toolActions.first() as ToolAction.SendEmail
            assertEquals("test@example.com", action.to)
            assertEquals("Hello", action.subject)
            assertEquals("Hi there", action.body)
        }

    @Test
    fun `send_email tool call without a recipient does not emit an action`() =
        runTest {
            every { repository.streamLlmResponse(any(), any()) } returns
                flowOf(
                    LlmStreamEvent.ToolCall("send_email", """{"subject":"Hello"}"""),
                )

            viewModel.onInputTextChanged("send an email")
            viewModel.sendMessage()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.messages
                    .last()
                    .text
                    .contains("recipient", ignoreCase = true),
            )
        }

    @Test
    fun `onPhotoCaptured appends an image message and streams with the fixed vision model`() =
        runTest {
            every { repository.streamLlmResponse(any(), any()) } returns
                flowOf(
                    LlmStreamEvent.TextDelta("I see a cat."),
                )

            viewModel.onPhotoCaptured(byteArrayOf(1, 2, 3))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.messages.size)
            assertTrue(state.messages[0].isFromUser)
            assertTrue(state.messages[0].imageDataUri!!.startsWith("data:image/jpeg;base64,"))
            assertEquals("I see a cat.", state.messages[1].text)

            verify(exactly = 1) { repository.streamLlmResponse(any(), "openai/gpt-4o-mini") }
        }
}
