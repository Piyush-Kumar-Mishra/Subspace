package com.example.linkit.util

import retrofit2.Response

suspend fun <T> safeApiCallDirect(apiCall: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Unknown error occurred")
    }
}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) NetworkResult.Success(body) else NetworkResult.Error("Empty response body")
        } else {
            NetworkResult.Error("API failed: ${response.code()} ${response.message()}")
        }
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Unknown error occurred")
    }
}
