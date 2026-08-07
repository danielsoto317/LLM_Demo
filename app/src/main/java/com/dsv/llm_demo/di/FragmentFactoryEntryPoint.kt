package com.dsv.llm_demo.di

import com.dsv.llm_demo.ui.AppFragmentFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FragmentFactoryEntryPoint {
    fun getFragmentFactory(): AppFragmentFactory
}