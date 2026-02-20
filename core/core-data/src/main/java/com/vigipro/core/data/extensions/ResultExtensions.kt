package com.vigipro.core.data.extensions

/**
 * Maps the success value of a Result, preserving the failure.
 */
inline fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> =
    map(transform)

/**
 * Maps the failure exception, preserving the success value.
 */
inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
    onFailure { return Result.failure(transform(it)) }

/**
 * Converts a Result<T> to a Result<Unit>, discarding the success value.
 */
fun <T> Result<T>.toUnit(): Result<Unit> = map { }

/**
 * Runs [block] only if Result is success.
 */
inline fun <T> Result<T>.onSuccessSuspend(
    crossinline block: suspend (T) -> Unit,
): Result<T> {
    getOrNull()?.let { value ->
        kotlinx.coroutines.runBlocking { block(value) }
    }
    return this
}
