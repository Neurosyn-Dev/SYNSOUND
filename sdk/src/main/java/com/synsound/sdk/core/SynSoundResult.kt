package com.synsound.sdk.core

/**
 * A sealed class representing the outcome of an SDK operation.
 */
sealed class SynSoundResult<out T> {
    data class Success<out T>(val data: T) : SynSoundResult<T>()
    data class Failure(val error: Throwable, val message: String = error.localizedMessage ?: "Unknown error") : SynSoundResult<Nothing>()
    data class Error(val code: Int, val message: String) : SynSoundResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure || this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    inline fun onSuccess(action: (T) -> Unit): SynSoundResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (Throwable, String) -> Unit): SynSoundResult<T> {
        when (this) {
            is Failure -> action(error, message)
            is Error -> action(Exception(message), message)
            is Success -> {}
        }
        return this
    }
}
