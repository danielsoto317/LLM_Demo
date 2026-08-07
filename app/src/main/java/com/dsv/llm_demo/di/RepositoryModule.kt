package com.dsv.llm_demo.di

import com.dsv.llm_demo.data.repository.LlmRepository
import com.dsv.llm_demo.data.repository.LlmRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindPostRepository(impl: LlmRepositoryImpl): LlmRepository
}