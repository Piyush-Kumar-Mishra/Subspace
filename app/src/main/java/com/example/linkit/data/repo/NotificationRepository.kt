package com.example.linkit.data.repo

import com.example.linkit.data.api.ApiService
import com.example.linkit.data.models.FCMTokenRequest
import com.example.linkit.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun registerToken(token: String): Flow<NetworkResult<Unit>> = flow {
        try {
            emit(NetworkResult.Loading())
            val response = api.registerFCMToken(FCMTokenRequest(token))
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error("Failed to register token"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "error"))
        }
    }
    suspend fun unregisterToken(token: String): Flow<NetworkResult<Unit>> = flow {
        try {
            emit(NetworkResult.Loading())
            val response = api.unregisterFCMToken(FCMTokenRequest(token))
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error("Failed to unregister token"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "error"))
        }
    }

}