package com.example.linkit.util

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

object JwtUtils {

    data class JwtPayload(
        val userId: Long,
        val email: String,
        val exp: Long,
        val iat: Long
    )

    fun parseJwt(token: String): JwtPayload? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            // Decode the payload (second part of the token)
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING)
            val payloadJson = String(decodedBytes)

            val jsonElement = Json.parseToJsonElement(payloadJson)
            val jsonObject = jsonElement.jsonObject

            JwtPayload(
                userId = jsonObject["userId"]?.jsonPrimitive?.long ?: 0L,
                email = jsonObject["email"]?.jsonPrimitive?.content ?: "",
                exp = jsonObject["exp"]?.jsonPrimitive?.long ?: 0L,
                iat = jsonObject["iat"]?.jsonPrimitive?.long ?: 0L
            )
        }
        catch (e: Exception) {
            null
        }
    }

    fun isTokenExpired(token: String): Boolean {
        val payload = parseJwt(token) ?: return true
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        return currentTimeSeconds >= payload.exp
    }

    fun getUserIdFromToken(token: String): Long? {
        return parseJwt(token)?.userId
    }

    fun getEmailFromToken(token: String): String? {
        return parseJwt(token)?.email
    }
}
