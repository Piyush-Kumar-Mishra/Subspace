package com.example.linkit.util

sealed class NetworkResult<out T> {
    class Loading :NetworkResult<Nothing>()
    data class Success<out T>(val data:T):NetworkResult<T>()
    data class Error<T>(val message: String):NetworkResult<T>()
}