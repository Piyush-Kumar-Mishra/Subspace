package com.example.linkit.data.api

import com.example.linkit.data.models.AddConnectionRequest
import com.example.linkit.data.models.ConnectionsResponse
import com.example.linkit.data.models.CreateProfileRequest
import com.example.linkit.data.models.FCMTokenRequest
import com.example.linkit.data.models.auth_models.LoginRequest
import com.example.linkit.data.models.ProfileResponse
import com.example.linkit.data.models.ProfileStatusResponse
import com.example.linkit.data.models.auth_models.RegisterRequest
import com.example.linkit.data.models.auth_models.RegisterResponse
import com.example.linkit.data.models.SearchUsersResponse
import com.example.linkit.data.models.auth_models.TokenResponse
import com.example.linkit.data.models.UpdateProfileRequest
import com.example.linkit.data.models.UserConnection
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<TokenResponse>

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<RegisterResponse>

    @POST("api/profile")
    suspend fun createProfile(@Body request: CreateProfileRequest): Response<ProfileResponse>

    @GET("api/profile")
    suspend fun getUserProfile(): Response<ProfileResponse>

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>

    @GET("api/profile/status")
    suspend fun getProfileStatus(): Response<ProfileStatusResponse>

    @GET("api/connections/search")
    suspend fun searchUsers(@Query("q") query: String): Response<SearchUsersResponse>

    @POST("api/connections")
    suspend fun addConnection(@Body request: AddConnectionRequest): Response<Unit>

    @GET("api/connections")
    suspend fun getConnections(): Response<ConnectionsResponse>

    @GET("api/connections/{userId}")
    suspend fun getConnectionById(@Path("userId") userId: Long): Response<ConnectionsResponse>

    @GET("api/connections/requests")
    suspend fun getPendingRequests(): Response<List<UserConnection>>

    @POST("api/connections/requests/{requestId}/accept")
    suspend fun acceptRequest(@Path("requestId") requestId: Long): Response<Unit>

    @DELETE("api/connections/requests/{requestId}/reject")
    suspend fun rejectRequest(@Path("requestId") requestId: Long): Response<Unit>

    @DELETE("api/connections/{userId}")
    suspend fun removeConnection(@Path("userId") userId: Long): Response<Unit>

    @POST("api/connections/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: Long): Response<Unit>

    @GET("api/profile/{userId}")
    suspend fun getProfileById(@Path("userId") userId: Long): Response<ProfileResponse>

    @POST("api/notifications/register")
    suspend fun registerFCMToken(@Body request: FCMTokenRequest): Response<Unit>

    @POST("api/notifications/unregister")
    suspend fun unregisterFCMToken(@Body request: FCMTokenRequest): Response<Unit>

    @DELETE("api/profile/account")
    suspend fun deleteAccount(): Response<Unit>


}

