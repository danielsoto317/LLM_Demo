package com.dsv.llm_demo.di.module

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.dsv.llm_demo.di.FragmentKey
import com.dsv.llm_demo.ui.llmchat.LlmChatFragment
import com.dsv.llm_demo.ui.llmchat.LlmChatViewModelImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object ListFragmentModule {
    @Provides
    @IntoMap
    @FragmentKey(LlmChatFragment::class)
    fun provideListFragment(): Fragment =
        LlmChatFragment { fragment ->
            ViewModelProvider(fragment)[LlmChatViewModelImpl::class.java]
        }
}
