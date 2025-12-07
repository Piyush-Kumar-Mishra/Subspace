package com.example.linkit.data.api

import com.example.linkit.data.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ProjectApiService {

    @GET("api/projects")
    suspend fun getProjects(
        @Query("priority") priority: String? = null,
        @Query("date") date: String? = null,
        @Query("today") today: Boolean? = null,
        @Query("startDateFrom") startDateFrom: String? = null,
        @Query("startDateTo") startDateTo: String? = null
    ): Response<ProjectsResponse>

    @POST("api/projects")
    suspend fun createProject(@Body request: CreateProjectRequest): Response<ProjectResponse>

    @GET("api/projects/{id}")
    suspend fun getProject(@Path("id") projectId: Long): Response<ProjectResponse>

    @PUT("api/projects/{id}")
    suspend fun updateProject(
        @Path("id") projectId: Long,
        @Body request: UpdateProjectRequest
    ): Response<ProjectResponse>

    @DELETE("api/projects/{id}")
    suspend fun deleteProject(@Path("id") projectId: Long): Response<Unit>

    @GET("api/tasks/project/{projectId}")
    suspend fun getProjectTasks(@Path("projectId") projectId: Long): Response<TasksResponse>

    @POST("api/tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<TaskResponse>

    @GET("api/tasks/{id}")
    suspend fun getTask(@Path("id") taskId: Long): Response<TaskResponse>

    @PUT("api/tasks/{id}")
    suspend fun updateTask(
        @Path("id") taskId: Long,
        @Body request: UpdateTaskRequest
    ): Response<TaskResponse>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") taskId: Long): Response<Unit>

    @GET("api/tasks/{taskId}/messages")
    suspend fun getTaskMessages(@Path("taskId") taskId: Long): Response<TaskMessagesResponse>

    @POST("api/tasks/{taskId}/messages")
    suspend fun sendTaskMessage(
        @Path("taskId") taskId: Long,
        @Body request: TaskMessageRequest
    ): Response<TaskMessageResponse>

    @GET("api/tasks/{taskId}/attachments")
    suspend fun getTaskAttachments(@Path("taskId") taskId: Long): Response<TaskAttachmentsResponse>

    @Multipart
    @POST("api/tasks/{taskId}/attachments")
    suspend fun uploadTaskAttachment(
        @Path("taskId") taskId: Long,
        @Part file: MultipartBody.Part
    ): Response<TaskAttachmentResponse>

    @POST("api/notifications/register")
    suspend fun registerDeviceToken(@Body request: NotificationRequest): Response<Unit>

    @POST("api/projects/{projectId}/poll")
    suspend fun createProjectPoll(
        @Path("projectId") projectId: Long,
        @Body request: CreatePollRequest
    ): Response<PollResponse>

    @GET("api/projects/{projectId}/poll")
    suspend fun getProjectPoll(@Path("projectId") projectId: Long): Response<PollResponse>

    @POST("api/polls/{pollId}/options/{optionId}/vote")
    suspend fun voteOnPoll(
        @Path("pollId") pollId: Long,
        @Path("optionId") optionId: Long
    ): Response<PollResponse>



}
