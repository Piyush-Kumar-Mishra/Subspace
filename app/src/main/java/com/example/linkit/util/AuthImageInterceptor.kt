//package com.example.linkit.util
//
//import coil.intercept.Interceptor
//import coil.request.ImageResult
//import com.example.linkit.data.TokenStore
//import kotlinx.coroutines.flow.first
//import javax.inject.Inject
//
///**
// * A Coil Interceptor that adds an Authorization header to image requests
// * for images hosted on our own API.
// */
//class AuthImageInterceptor @Inject constructor(
//    private val tokenStore: TokenStore
//) : Interceptor {
//
//    override suspend fun intercept(chain: RealInterceptorChain): ImageResult {
//        val request = chain.request
//        val url = request.data.toString()
//
//        // Only add auth header for our API images
//        if (url.contains("/api/images/")) {
//            val token = tokenStore.token.first()
//            if (token != null) {
//                val newRequest = request.newBuilder()
//                    .setHeader("Authorization", "Bearer $token")
//                    .build()
//                return chain.proceed(newRequest)
//            }
//        }
//
//        return chain.proceed(request)
//    }
//}

package com.example.linkit.util

import coil.intercept.Interceptor
import coil.intercept.Interceptor.Chain
import coil.request.ImageResult
import coil.request.ImageRequest
import com.example.linkit.data.TokenStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthImageInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override suspend fun intercept(chain: Chain): ImageResult {
        val request = chain.request
        val url = request.data.toString()

        if (url.contains("/api/images/")) {
            val token = tokenStore.token.first()
            if (token != null) {
                val newRequest: ImageRequest = request.newBuilder()
                    .setHeader("Authorization", "Bearer $token")
                    .build()
                return chain.proceed(newRequest)
            }
        }
        return chain.proceed(request)
    }
}
