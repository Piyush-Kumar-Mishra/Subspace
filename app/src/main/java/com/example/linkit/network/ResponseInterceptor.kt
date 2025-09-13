package com.example.linkit.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class ResponseInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        Log.d("API_RESPONSE", "URL: ${request.url}")
        Log.d("API_RESPONSE", "Status: ${response.code}")

        // Handle global error responses
        when (response.code) {
            401 -> {
                Log.w("API_RESPONSE", "Unauthorized - Token may be expired")
            }
            403 -> {
                Log.w("API_RESPONSE", "Forbidden - Invalid permissions")
            }
            500 -> {
                Log.e("API_RESPONSE", "Server Error")
            }
        }

        return response
    }
}
