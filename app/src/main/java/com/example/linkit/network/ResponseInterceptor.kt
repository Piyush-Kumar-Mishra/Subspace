package com.example.linkit.network

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class ResponseInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        Timber.tag("API_RESPONSE").d("URL: ${request.url}")
        Timber.tag("API_RESPONSE").d("Status: ${response.code}")

        when (response.code) {
            401 -> {
                Timber.tag("API_RESPONSE").w("Unauthorized - Token may be expired")
            }
            403 -> {
                Timber.tag("API_RESPONSE").w("Forbidden - Invalid permissions")
            }
            500 -> {
                Timber.tag("API_RESPONSE").e("Server Error")
            }
        }
        return response
    }
}
