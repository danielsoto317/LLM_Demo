package com.dsv.llm_demo.ui.llmchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LlmChatFragment (
    private val viewModelProvider: (Fragment) -> LlmChatViewModel
) : Fragment() {

    private val viewModel: LlmChatViewModel by lazy {
        viewModelProvider(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                LlmChatScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}