package com.example.linkit.network

import com.example.linkit.data.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for login/register endpoints
        val url = originalRequest.url.toString()
        if (url.contains("/api/auth/")) {
            return chain.proceed(originalRequest)
        }

        // Get token synchronously (blocking call in interceptor context)
        val token = runBlocking {
            try {
                tokenStore.token.first()
            }
            catch (e: Exception) {
                null
            }
        }

        // Add Authorization header if token exists
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        else {
            originalRequest
        }

        val response = chain.proceed(newRequest)

        // Handle token expiration globally
        if (response.code == 401 || response.code == 403) {
            // Token expired/invalid, clear it
            runBlocking {
                try {
                    tokenStore.clearToken()
                }
                catch (e: Exception) {

                }
            }
        }
        return response
    }
}
