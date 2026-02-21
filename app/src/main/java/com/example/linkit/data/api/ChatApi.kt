package com.example.linkit.data.api

import com.example.linkit.data.models.ChatHistoryResponse
import com.example.linkit.data.models.ChatMessageRequest
import com.example.linkit.data.models.ChatMessageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @GET("api/projects/{projectId}/chat")
    suspend fun getChatHistory(
        @Path("projectId") projectId: Long,
        @Query("before") beforeTimestamp: String? = null,
        @Query("limit") limit: Int = 20
    ): ChatHistoryResponse

    @POST("api/projects/{projectId}/chat")
    suspend fun sendMessage(
        @Path("projectId") projectId: Long,
        @Body request: ChatMessageRequest
    ): ChatMessageResponse

}
