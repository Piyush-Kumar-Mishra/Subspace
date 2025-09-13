package com.example.linkit.data.api

import com.example.linkit.data.models.AddConnectionRequest
import com.example.linkit.data.models.ConnectionsResponse
import com.example.linkit.data.models.CreateProfileRequest
import com.example.linkit.data.models.LoginRequest
import com.example.linkit.data.models.ProfileResponse
import com.example.linkit.data.models.ProfileStatusResponse
import com.example.linkit.data.models.RegisterRequest
import com.example.linkit.data.models.RegisterResponse
import com.example.linkit.data.models.SearchUsersResponse
import com.example.linkit.data.models.TokenResponse
import com.example.linkit.data.models.UpdateProfileRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth endpoints
    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<TokenResponse>

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<RegisterResponse>

    // Protected Profile endpoints
    @POST("api/profile")
    suspend fun createProfile(@Body request: CreateProfileRequest): Response<ProfileResponse>

    @GET("api/profile")
    suspend fun getUserProfile(): Response<ProfileResponse>

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>

    @GET("api/profile/status")
    suspend fun getProfileStatus(): Response<ProfileStatusResponse>

    // Protected Connection endpoints
    @GET("api/connections/search")
    suspend fun searchUsers(@Query("q") query: String): Response<SearchUsersResponse>

    @POST("api/connections")
    suspend fun addConnection(@Body request: AddConnectionRequest): Response<Unit>

    @GET("api/connections")
    suspend fun getConnections(): Response<ConnectionsResponse>

    // Protected image endpoint
    @GET("api/images/profiles/{filename}")
    suspend fun getProfileImage(@Path("filename") filename: String): Response<ResponseBody>

    @GET("api/connections/{userId}")
    suspend fun getConnectionById(@Path("userId") userId: Long): Response<ConnectionsResponse>

    @GET("api/profile/{userId}")
    suspend fun getProfileById(@Path("userId") userId: Long): Response<ProfileResponse>

}

