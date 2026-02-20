package com.vigipro.core.data.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen

/**
 * Sealed class representing a loading/success/error state for UI consumption.
 */
sealed interface ResultState<out T> {
    data object Loading : ResultState<Nothing>
    data class Success<T>(val data: T) : ResultState<T>
    data class Error(val exception: Throwable) : ResultState<Nothing>
}

/**
 * Converts a Flow<T> to Flow<ResultState<T>> with automatic Loading emission.
 */
fun <T> Flow<T>.asResultState(): Flow<ResultState<T>> =
    map<T, ResultState<T>> { ResultState.Success(it) }
        .onStart { emit(ResultState.Loading) }
        .catch { emit(ResultState.Error(it)) }

/**
 * Retries a Flow with exponential backoff.
 * @param maxRetries Maximum number of retry attempts.
 * @param initialDelay Initial delay in milliseconds before first retry.
 * @param maxDelay Maximum delay cap in milliseconds.
 * @param factor Multiplier applied to delay after each retry.
 */
fun <T> Flow<T>.retryWithBackoff(
    maxRetries: Long = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 30_000,
    factor: Double = 2.0,
): Flow<T> = retryWhen { _, attempt ->
    if (attempt >= maxRetries) return@retryWhen false
    val delayMs = (initialDelay * Math.pow(factor, attempt.toDouble())).toLong()
        .coerceAtMost(maxDelay)
    delay(delayMs)
    true
}
