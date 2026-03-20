package com.example.linkit.util

suspend fun <T> safeApiCallDirect(apiCall: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Unknown error occurred")
    }
}

