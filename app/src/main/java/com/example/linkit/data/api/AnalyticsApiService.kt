package com.example.linkit.data.api

import com.example.linkit.data.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AnalyticsApiService {

    @GET("api/projects/{projectId}/analytics/summary")
    suspend fun getProjectSummary(@Path("projectId") projectId: Long): Response<ProjectSummaryResponse>

    @GET("api/projects/{projectId}/analytics/workload")
    suspend fun getWorkload(@Path("projectId") projectId: Long): Response<List<AssigneeWorkloadResponse>>

    @GET("api/projects/{projectId}/analytics/productivity")
    suspend fun getProductivity(@Path("projectId") projectId: Long): Response<ProductivityResponse>

    @GET("api/projects/{projectId}/analytics/assignees")
    suspend fun getAssigneeStats(@Path("projectId") projectId: Long): Response<AssigneeStatsWrapper>
}