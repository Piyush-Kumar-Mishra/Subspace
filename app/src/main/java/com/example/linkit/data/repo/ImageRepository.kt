package com.example.linkit.data.repo

import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.ApiService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ImageRepository @Inject constructor(private val api: ApiService, private val tokenStore: TokenStore) {

    suspend fun getProfileImageUrl(filename: String?): String? {
        if (filename.isNullOrBlank()) return null

        return try {
            val token = tokenStore.token.first()
            if (token != null) {
                // Return URL with token for Coil to use with custom headers
                "${com.example.linkit.util.Constants.BASE_URL}api/images/profiles/$filename"
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAuthHeaders(): Map<String, String> {
        val token = tokenStore.token.first()
        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else emptyMap()
    }
}
