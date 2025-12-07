package com.example.linkit.data.repo

import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.AnalyticsApiService
import com.example.linkit.data.models.*
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val api: AnalyticsApiService,
    private val tokenStore: TokenStore,
    private val networkUtils: NetworkUtils
) {

    fun getProjectSummary(projectId: Long): Flow<NetworkResult<ProjectSummaryResponse>> = safeApiCall {
        api.getProjectSummary(projectId)
    }

    fun getWorkload(projectId: Long): Flow<NetworkResult<List<AssigneeWorkloadResponse>>> = safeApiCall {
        api.getWorkload(projectId)
    }

    fun getProductivity(projectId: Long): Flow<NetworkResult<ProductivityResponse>> = safeApiCall {
        api.getProductivity(projectId)
    }

    fun getAssigneeStats(projectId: Long): Flow<NetworkResult<AssigneeStatsWrapper>> = safeApiCall {
        api.getAssigneeStats(projectId)
    }

    private fun <T> safeApiCall(apiCall: suspend () -> retrofit2.Response<T>): Flow<NetworkResult<T>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = apiCall()
                if (response.isSuccessful && response.body() != null) {
                    emit(NetworkResult.Success(response.body()!!))
                } else {
                    emit(NetworkResult.Error("Error ${response.code()}: ${response.message()}"))
                }
            } else {
                emit(NetworkResult.Error("No internet or invalid token"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }
}