package com.dsv.llm_demo.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import javax.inject.Inject
import javax.inject.Provider

/**
 * Instantiates fragments from providers contributed with `@IntoMap` + `@FragmentKey`, so adding a
 * screen never requires editing this class. Falls back to reflection for fragments with no
 * registered provider.
 */
class AppFragmentFactory @Inject constructor(
    private val fragmentProviders: Map<Class<out Fragment>, @JvmSuppressWildcards Provider<Fragment>>,
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragmentClass = FragmentFactory.loadFragmentClass(classLoader, className)
        return fragmentProviders[fragmentClass]?.get()
            ?: super.instantiate(classLoader, className)
    }
}