package com.example.linkit.data.repo

import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.ProjectApiService
import com.example.linkit.data.models.CreatePollRequest
import com.example.linkit.data.models.PollResponse
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PollRepository @Inject constructor(
    private val api: ProjectApiService,
    private val tokenStore: TokenStore,
    private val networkUtils: NetworkUtils
) {
    fun getProjectPoll(projectId: Long): Flow<NetworkResult<PollResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            if (networkUtils.isInternetAvailable()) {
                val response = api.getProjectPoll(projectId)
                if (response.isSuccessful) {
                    response.body()?.let { emit(NetworkResult.Success(it)) }
                        ?: emit(NetworkResult.Error("Empty response body."))
                } else if (response.code() == 404) {
                    emit(NetworkResult.Error("NOT_FOUND"))
                } else {
                    emit(NetworkResult.Error("Error: ${response.code()}"))
                }
            } else {
                emit(NetworkResult.Error("No internet connection."))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "An unknown error occurred."))
        }
    }

    fun createProjectPoll(projectId: Long, request: CreatePollRequest): Flow<NetworkResult<PollResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            if (networkUtils.isInternetAvailable()) {
                val response = api.createProjectPoll(projectId, request)
                if (response.isSuccessful) {
                    response.body()?.let { emit(NetworkResult.Success(it)) }
                        ?: emit(NetworkResult.Error("Empty response body."))
                } else {
                    emit(NetworkResult.Error("Error: ${response.code()}"))
                }
            } else {
                emit(NetworkResult.Error("No internet connection."))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "An unknown error occurred."))
        }
    }

    fun voteOnPoll(pollId: Long, optionId: Long): Flow<NetworkResult<PollResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            if (networkUtils.isInternetAvailable()) {
                val response = api.voteOnPoll(pollId, optionId)
                if (response.isSuccessful) {
                    response.body()?.let { emit(NetworkResult.Success(it)) }
                        ?: emit(NetworkResult.Error("Empty response body."))
                } else {
                    emit(NetworkResult.Error("Error: ${response.code()}"))
                }
            } else {
                emit(NetworkResult.Error("No internet connection."))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "An unknown error occurred."))
        }
    }
}