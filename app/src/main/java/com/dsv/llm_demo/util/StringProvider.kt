package com.dsv.llm_demo.util

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StringProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    fun getString(
        @StringRes resId: Int,
    ): String = context.getString(resId)

    fun getString(
        @StringRes resId: Int,
        vararg formatArgs: Any,
    ): String = context.getString(resId, *formatArgs)

    fun getStringArray(
        @ArrayRes resId: Int,
    ): Array<String> = context.resources.getStringArray(resId)

    fun getQuantityString(
        @PluralsRes resId: Int,
        quantity: Int,
        vararg formatArgs: Any,
    ): String = context.resources.getQuantityString(resId, quantity, *formatArgs)
}
