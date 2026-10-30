package com.nibras.nova.core.common

sealed interface NovaResult<out T> {
    data class Success<T>(val data: T) : NovaResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : NovaResult<Nothing>
    data object Loading : NovaResult<Nothing>
}
