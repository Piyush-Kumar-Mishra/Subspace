package com.example.linkit.data.repo

import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.ApiService
import com.example.linkit.data.local.dao.ConnectionDao
import com.example.linkit.data.local.dao.UserDao
import com.example.linkit.data.local.entities.ConnectionEntity
import com.example.linkit.data.local.entities.UserEntity
import com.example.linkit.data.models.*
import com.example.linkit.util.ImageCacheManager
import com.example.linkit.util.JwtUtils
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val userDao: UserDao,
    private val connectionDao: ConnectionDao,
    private val networkUtils: NetworkUtils,
    private val imageCacheManager: ImageCacheManager
) {

    fun createProfile(request: CreateProfileRequest): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    // Online: Create profile on server
                    val response = api.createProfile(request)

                    if (response.isSuccessful) {
                        response.body()?.let { profileResponse ->
                            // Cache profile data locally
                            cacheUserProfile(profileResponse, token)
                            emit(NetworkResult.Success(profileResponse))
                        } ?: emit(NetworkResult.Error("Empty response"))
                    }
                    else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                }
                else {
                    emit(NetworkResult.Error("No internet connection, profile creation requires internet."))
                }
            }
            else {
                emit(NetworkResult.Error("No valid token found"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun getUserProfile(): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                val userId = JwtUtils.getUserIdFromToken(token)

                if (networkUtils.isInternetAvailable()) {
                    // Online, Fetch data from server
                    try {
                        val response = api.getUserProfile()
                        if (response.isSuccessful) {
                            response.body()?.let { profileResponse ->
                                // Cache fresh data locally
                                cacheUserProfile(profileResponse, token)
                                emit(NetworkResult.Success(profileResponse))
                            } ?: emit(NetworkResult.Error("Empty response"))
                        }
                        else {
                            // Server error, fallback to cached data
                            userId?.let { id ->
                                val cachedProfile = getCachedProfile(id)
                                if (cachedProfile != null) {
                                    emit(NetworkResult.Success(cachedProfile))
                                }
                                else {
                                    val errorMsg = getErrorMessage(
                                        response.code(),
                                        response.errorBody()?.string()
                                    )
                                    emit(NetworkResult.Error(errorMsg))
                                }
                            }
                        }
                    }
                    catch (e: Exception) {
                        // Network error, fallback to cached data
                        userId?.let { id ->
                            val cachedProfile = getCachedProfile(id)
                            if (cachedProfile != null) {
                                emit(NetworkResult.Success(cachedProfile))
                            }
                            else {
                                emit(NetworkResult.Error("Network error and no cached data available"))
                            }
                        }
                    }
                }
                else {
                    // Offline: Use cached data
                    userId?.let { id ->
                        val cachedProfile = getCachedProfile(id)
                        if (cachedProfile != null) {
                            emit(NetworkResult.Success(cachedProfile))
                        }
                        else {
                            emit(NetworkResult.Error("No internet connection and no cached profile data"))
                        }
                    } ?: emit(NetworkResult.Error("Invalid token"))
                }
            }
            else {
                emit(NetworkResult.Error("No valid token found"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Error: ${e.message}"))
        }
    }

    fun updateProfile(request: UpdateProfileRequest): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    // Online, Update on server
                    val response = api.updateProfile(request)
                    if (response.isSuccessful) {
                        response.body()?.let { profileResponse ->
                            // Cache updated data locally
                            cacheUserProfile(profileResponse, token)
                            emit(NetworkResult.Success(profileResponse))
                        } ?: emit(NetworkResult.Error("Empty response"))
                    }
                    else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                }
                else {
                    emit(NetworkResult.Error("No internet connection. Profile update requires internet."))
                }
            }
            else {
                emit(NetworkResult.Error("No valid token found"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun getProfileStatus(): Flow<NetworkResult<ProfileStatusResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    // Online: Check server status
                    val response = api.getProfileStatus()
                    if (response.isSuccessful) {
                        response.body()?.let { emit(NetworkResult.Success(it)) }
                            ?: emit(NetworkResult.Error("Empty response"))
                    }
                    else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                }
                else {
                    // Offline: Check if we have cached profile
                    val userId = JwtUtils.getUserIdFromToken(token)
                    userId?.let { id ->
                        val cachedUser = userDao.getUserById(id)
                        val profileCompleted = cachedUser?.isProfileCompleted ?: false
                        emit(NetworkResult.Success(ProfileStatusResponse(profileCompleted)))
                    } ?: emit(NetworkResult.Error("Invalid token"))
                }
            }
            else {
                emit(NetworkResult.Error("No valid token found"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun getConnections(): Flow<NetworkResult<ConnectionsResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                val userId = JwtUtils.getUserIdFromToken(token)

                if (networkUtils.isInternetAvailable()) {
                    // Online, Fetch fresh connections
                    try {
                        val response = api.getConnections()
                        if (response.isSuccessful) {
                            response.body()?.let { connectionsResponse ->
                                // Cache connections locally
                                userId?.let { id ->
                                    cacheConnections(
                                        id,
                                        connectionsResponse.connections
                                    )
                                }
                                emit(NetworkResult.Success(connectionsResponse))
                            } ?: emit(NetworkResult.Error("Empty response"))
                        }
                        else {
                            // Server error, fallback to cached data
                            userId?.let { id ->
                                val cachedConnections = getCachedConnections(id)
                                emit(NetworkResult.Success(ConnectionsResponse(cachedConnections)))
                            }
                        }
                    }
                    catch (e: Exception) {
                        // Network error, fallback to cached data
                        userId?.let { id ->
                            val cachedConnections = getCachedConnections(id)
                            emit(NetworkResult.Success(ConnectionsResponse(cachedConnections)))
                        }
                    }
                }
                else {
                    // Offline, Use cached connections
                    userId?.let { id ->
                        val cachedConnections = getCachedConnections(id)
                        emit(NetworkResult.Success(ConnectionsResponse(cachedConnections)))
                    } ?: emit(NetworkResult.Error("Invalid token"))
                }
            }
            else {
                emit(NetworkResult.Error("No valid token found"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Error: ${e.message}"))
        }
    }

    fun searchUsers(query: String): Flow<NetworkResult<SearchUsersResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    // Online, Search on server
                    val response = api.searchUsers(query)
                    if (response.isSuccessful) {
                        response.body()?.let { emit(NetworkResult.Success(it)) }
                            ?: emit(NetworkResult.Error("Empty response"))
                    }
                    else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                }
                else {
                    emit(NetworkResult.Error("No internet connection. User search requires internet."))
                }
            }
            else {
                emit(NetworkResult.Error("Token not found"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun addConnection(email: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    // Online, Add connection on server
                    val response = api.addConnection(AddConnectionRequest(email))
                    if (response.isSuccessful) {
                        emit(NetworkResult.Success(Unit))
                    }
                    else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                }
                else {
                    emit(NetworkResult.Error("No internet connection. Adding connections requires internet."))
                }
            }
            else {
                emit(NetworkResult.Error("Token not found"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    private suspend fun getCachedProfile(userId: Long): ProfileResponse? {
        return try {
            val userEntity = userDao.getUserById(userId)
            userEntity?.let {
                ProfileResponse(
                    userId = it.userId,
                    name = it.name,
                    jobTitle = it.jobTitle,
                    company = it.company,
                    aboutMe = it.aboutMe,
                    profileImageUrl = it.profileImageUrl
                )
            }
        }
        catch (e: Exception) {
            null
        }
    }

    private suspend fun cacheConnections(userId: Long, connections: List<ConnectionResponse>) {
        try {
            val connectionEntities = connections.map { connection ->
                // Cache connection profile image if exists
                var localImagePath: String? = null
                connection.profileImageUrl?.let { imageUrl ->
                    val fileName =
                        "connection_${connection.userId}_${System.currentTimeMillis()}.jpg"
                    localImagePath = imageCacheManager.downloadAndCacheImage(imageUrl, fileName)
                }

                ConnectionEntity(
                    connectionId = "${userId}_${connection.userId}",
                    userId = userId,
                    connectedUserId = connection.userId,
                    name = connection.name,
                    company = connection.company,
                    profileImageUrl = connection.profileImageUrl,
                    profileImageLocalPath = localImagePath
                )
            }

            // Clear old connections and insert new ones
            connectionDao.clearConnectionsForUser(userId)
            connectionDao.insertConnections(connectionEntities)
        }
        catch (e: Exception) {
            // caching errors
        }
    }


    private suspend fun getCachedConnections(userId: Long): List<ConnectionResponse> {
        return try {
            val connectionEntities = connectionDao.getConnectionsForUser(userId)
            connectionEntities.map { entity ->
                ConnectionResponse(
                    userId = entity.connectedUserId,
                    name = entity.name,
                    company = entity.company,
                    profileImageUrl = entity.profileImageUrl
                )
            }
        }
        catch (e: Exception) {
            emptyList()
        }
    }

    private fun getErrorMessage(code: Int, errorBody: String?): String {
        return when (code) {
            401 -> "Unauthorized - Token expired or invalid"
            403 -> "Forbidden - Invalid token"
            404 -> "Not found"
            500 -> "Server error"
            else -> errorBody ?: "Request failed with code: $code"
        }
    }

    fun getProfileById(userId: Long): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            if (networkUtils.isInternetAvailable()) {
                val response = api.getProfileById(userId)
                if (response.isSuccessful) {
                    response.body()?.let { profileResponse ->
                        cacheUserProfile(profileResponse, token = null)
                        emit(NetworkResult.Success(profileResponse))
                    } ?: emit(NetworkResult.Error("User profile not found"))
                }
                else {
                    getCachedProfile(userId)?.let { emit(NetworkResult.Success(it)) }
                        ?: emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                getCachedProfile(userId)?.let { emit(NetworkResult.Success(it)) }
                    ?: emit(NetworkResult.Error("No internet connection and no cached data for this user."))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Error: ${e.message}"))
        }
    }

    fun getConnectionsById(userId: Long): Flow<NetworkResult<ConnectionsResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            if (networkUtils.isInternetAvailable()) {
                val response = api.getConnectionById(userId)

                if (response.isSuccessful) {
                    response.body()?.let { connectionsResponse ->
                        cacheConnections(userId, connectionsResponse.connections)
                        emit(NetworkResult.Success(connectionsResponse))
                    } ?: emit(NetworkResult.Error("Could not load connections"))
                }
                else {
                    emit(NetworkResult.Success(ConnectionsResponse(getCachedConnections(userId))))
                }
            }
            else {
                emit(NetworkResult.Success(ConnectionsResponse(getCachedConnections(userId))))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Error: ${e.message}"))
        }
    }

    private suspend fun cacheUserProfile(profileResponse: ProfileResponse, token: String?) {
        try {
            val userId = profileResponse.userId

            if (token != null) {
                tokenStore.saveUserId(userId)
            }


            val email = if (token != null) JwtUtils.getEmailFromToken(token) ?: "" else ""

            var localImagePath: String? = null
            profileResponse.profileImageUrl?.let { imageUrl ->
                val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
                localImagePath = imageCacheManager.downloadAndCacheImage(imageUrl, fileName)
            }

            val userEntity = UserEntity(
                userId = userId,
                email = email,
                username = "", // This field is not in the ProfileResponse
                name = profileResponse.name ?: "",
                jobTitle = profileResponse.jobTitle,
                company = profileResponse.company,
                aboutMe = profileResponse.aboutMe,
                profileImageUrl = profileResponse.profileImageUrl,
                profileImageLocalPath = localImagePath,
                isProfileCompleted = true
            )

            userDao.insertUser(userEntity)
        }
        catch (e: Exception) {
            // Caching errors should not crash the app
        }
    }
}


