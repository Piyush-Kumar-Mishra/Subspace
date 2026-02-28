package com.example.linkit.data.repo

import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.ApiService
import com.example.linkit.data.local.dao.ConnectionDao
import com.example.linkit.data.local.dao.UserDao
import com.example.linkit.data.local.entities.ConnectionEntity
import com.example.linkit.data.local.entities.UserEntity
import com.example.linkit.data.models.*
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
    private val networkUtils: NetworkUtils
) {

    fun createProfile(request: CreateProfileRequest): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    val response = api.createProfile(request)

                    if (response.isSuccessful) {
                        response.body()?.let { profileResponse ->
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
                    try {
                        val response = api.getUserProfile()
                        if (response.isSuccessful) {
                            response.body()?.let { profileResponse ->
                                cacheUserProfile(profileResponse, token)
                                emit(NetworkResult.Success(profileResponse))
                            } ?: emit(NetworkResult.Error("Empty response"))
                        }
                        else {
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
                    try {
                        val response = api.getConnections()
                        if (response.isSuccessful) {
                            response.body()?.let { connectionsResponse ->
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
                            userId?.let { id ->
                                val cachedConnections = getCachedConnections(id)
                                emit(NetworkResult.Success(ConnectionsResponse(cachedConnections)))
                            }
                        }
                    }
                    catch (e: Exception) {
                        userId?.let { id ->
                            val cachedConnections = getCachedConnections(id)
                            emit(NetworkResult.Success(ConnectionsResponse(cachedConnections)))
                        }
                    }
                }
                else {
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

    fun getPendingRequests(): Flow<NetworkResult<List<UserConnection>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    val response = api.getPendingRequests()
                    if (response.isSuccessful) {
                        emit(NetworkResult.Success(response.body() ?: emptyList()))
                    } else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                } else {
                    emit(NetworkResult.Error("No internet connection. Cannot load requests."))
                }
            } else {
                emit(NetworkResult.Error("Token not found or expired"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun acceptRequest(requestId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    val response = api.acceptRequest(requestId)
                    if (response.isSuccessful) {
                        emit(NetworkResult.Success(Unit))
                    } else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                } else {
                    emit(NetworkResult.Error("No internet connection"))
                }
            } else {
                emit(NetworkResult.Error("Token not found or expired"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun rejectRequest(requestId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    val response = api.rejectRequest(requestId)
                    if (response.isSuccessful) {
                        emit(NetworkResult.Success(Unit))
                    } else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                } else {
                    emit(NetworkResult.Error("No internet connection"))
                }
            } else {
                emit(NetworkResult.Error("Token not found or expired"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun removeConnection(userId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    val response = api.removeConnection(userId)
                    if (response.isSuccessful) {
                        emit(NetworkResult.Success(Unit))
                    } else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                } else {
                    emit(NetworkResult.Error("No internet connection"))
                }
            } else {
                emit(NetworkResult.Error("Token not found or expired"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun blockUser(userId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                if (networkUtils.isInternetAvailable()) {
                    val response = api.blockUser(userId)
                    if (response.isSuccessful) {
                        emit(NetworkResult.Success(Unit))
                    } else {
                        val errorMsg =
                            getErrorMessage(response.code(), response.errorBody()?.string())
                        emit(NetworkResult.Error(errorMsg))
                    }
                } else {
                    emit(NetworkResult.Error("No internet connection"))
                }
            } else {
                emit(NetworkResult.Error("Token not found or expired"))
            }
        } catch (e: Exception) {
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

                ConnectionEntity(
                    connectionId = "${userId}_${connection.userId}",
                    userId = userId,
                    connectedUserId = connection.userId,
                    name = connection.name,
                    company = connection.company,
                    profileImageUrl = connection.profileImageUrl
                )
            }
            connectionDao.clearConnectionsForUser(userId)
            connectionDao.insertConnections(connectionEntities)
        } catch (e: Exception) { }
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
            val email = if (token != null) JwtUtils.getEmailFromToken(token) ?: "" else ""

            val userEntity = UserEntity(
                userId = userId,
                email = email,
                username = "",
                name = profileResponse.name ?: "",
                jobTitle = profileResponse.jobTitle,
                company = profileResponse.company,
                aboutMe = profileResponse.aboutMe,
                profileImageUrl = profileResponse.profileImageUrl,
                isProfileCompleted = true
            )
            userDao.insertUser(userEntity)
        } catch (e: Exception) { }
    }

    fun deleteAccount(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                val response = api.deleteAccount()
                if (response.isSuccessful) {
                    val userId = JwtUtils.getUserIdFromToken(token)
                    userId?.let {
                        userDao.deleteUser(it)
                        connectionDao.clearConnectionsForUser(it)
                    }
                    tokenStore.clearToken()
                    emit(NetworkResult.Success(Unit))
                } else {
                    emit(NetworkResult.Error("Failed to delete account: ${response.code()}"))
                }
            } else {
                emit(NetworkResult.Error("Session expired"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error"))
        }
    }
}


