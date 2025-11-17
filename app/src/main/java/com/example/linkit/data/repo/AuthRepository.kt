package com.example.linkit.data.repo

import com.example.linkit.data.api.ApiService
import com.example.linkit.data.TokenStore
import com.example.linkit.data.models.auth_models.LoginRequest
import com.example.linkit.data.models.auth_models.RegisterRequest
import com.example.linkit.data.models.auth_models.RegisterResponse
import com.example.linkit.data.models.TokenResponse
import com.example.linkit.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {

    fun getToken(): Flow<String?> = tokenStore.token

    fun getUserId(): Flow<Long?> = tokenStore.userId

    fun register(email: String, username: String, password: String): Flow<NetworkResult<RegisterResponse>> = flow {

        emit(NetworkResult.Loading())

        try {
            val response = api.register(RegisterRequest(email, username, password))

            if (response.isSuccessful) {
                response.body()?.let { emit(NetworkResult.Success(it)) }
                    ?: emit(NetworkResult.Error("Empty response"))
            }

            else {
                val errorMsg = response.errorBody()?.string()?.ifBlank { response.message() } ?: response.message()
                emit(NetworkResult.Error(errorMsg.ifBlank { "Registration failed" }))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network Error"))
        }
    }

    fun login(username: String, password: String): Flow<NetworkResult<TokenResponse>> = flow {

        emit(NetworkResult.Loading())

        try {

            val response = api.login(LoginRequest(username, password))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    tokenStore.saveToken(body.token)
                    emit(NetworkResult.Success(body))
                }

                else {
                    emit(NetworkResult.Error("Empty response"))
                }

            }
            else {
                val errorMsg = response.errorBody()?.string()?.ifBlank { response.message() } ?: response.message()
                if (errorMsg.contains("invalid", ignoreCase = true)) {
                    emit(NetworkResult.Error("Invalid username or password"))
                } else {
                    emit(NetworkResult.Error(errorMsg.ifBlank { "Login failed" }))
                }
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun clearToken() = tokenStore.clearToken()

}
