package com.dsv.llm_demo.data.model

/**
 * Discriminated result wrapper, analogous to the `{ data, isLoading, error }` shape returned by
 * TanStack Query's hooks. Since [LlmRepository][com.dsv.llm_demo.data.repository.LlmRepository]
 * streams multiple values over time rather than resolving a single promise, this is emitted once
 * per flow ([Loading]) and then once per streamed value ([Success]) or terminal failure ([Error]),
 * instead of being a single snapshot object.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>

    data class Success<T>(
        val data: T,
    ) : Resource<T>

    data class Error(
        val error: Throwable,
    ) : Resource<Nothing>
}
