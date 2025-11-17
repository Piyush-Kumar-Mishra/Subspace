package com.example.linkit.data.repo

import android.content.Context
import android.net.Uri
import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.ProjectApiService
import com.example.linkit.data.models.*
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProjectRepository @Inject constructor(
    private val api: ProjectApiService,
    private val tokenStore: TokenStore,
    private val networkUtils: NetworkUtils,
    private val context: Context
) {

    fun getProjects(): Flow<NetworkResult<List<ProjectResponse>>> =
        getProjectsFiltered(null, null, null, null)


    fun getProjectsFiltered(
        priority: String? = null,
        date: String? = null,
        startDateFrom: String? = null,
        startDateTo: String? = null
    ): Flow<NetworkResult<List<ProjectResponse>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.getProjects(priority = priority, date = date, startDateFrom = startDateFrom, startDateTo = startDateTo)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it.projects))
                    } ?: emit(NetworkResult.Error("Empty response"))
                } else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            } else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun createProject(request: CreateProjectRequest): Flow<NetworkResult<ProjectResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.createProject(request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun getProject(projectId: Long): Flow<NetworkResult<ProjectResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.getProject(projectId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun updateProject(projectId: Long, request: UpdateProjectRequest): Flow<NetworkResult<ProjectResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.updateProject(projectId, request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun deleteProject(projectId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.deleteProject(projectId)
                if (response.isSuccessful) {
                    emit(NetworkResult.Success(Unit))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun getProjectTasks(projectId: Long): Flow<NetworkResult<List<TaskResponse>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.getProjectTasks(projectId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it.tasks))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }


    fun createTask(request: CreateTaskRequest): Flow<NetworkResult<TaskResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.createTask(request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun getTaskMessages(taskId: Long): Flow<NetworkResult<List<TaskMessageResponse>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.getTaskMessages(taskId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it.messages))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun sendTaskMessage(taskId: Long, request: TaskMessageRequest): Flow<NetworkResult<TaskMessageResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.sendTaskMessage(taskId, request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun getTaskAttachments(taskId: Long): Flow<NetworkResult<List<TaskAttachmentResponse>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.getTaskAttachments(taskId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it.attachments))
                    } ?: emit(NetworkResult.Error("Empty response"))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

    fun uploadTaskAttachment(taskId: Long, fileUri: Uri, fileName: String): Flow<NetworkResult<TaskAttachmentResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {

                val inputStream = context.contentResolver.openInputStream(fileUri)
                if (inputStream == null) {
                    emit(NetworkResult.Error("Failed to open file stream from Uri."))
                    return@flow
                }
                val requestBody = inputStream.readBytes().toRequestBody(
                    context.contentResolver.getType(fileUri)?.toMediaTypeOrNull()
                )
                inputStream.close() // close the stream

                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    fileName,
                    requestBody
                )
                val response = api.uploadTaskAttachment(taskId, multipartBody)

                if (response.isSuccessful && response.body() != null) {
                    emit(NetworkResult.Success(response.body()!!))
                } else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            } else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }


    fun registerDeviceToken(token: String, platform: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val authToken = tokenStore.token.first()
            if (authToken != null && networkUtils.isInternetAvailable()) {
                val request = NotificationRequest(token, platform)
                val response = api.registerDeviceToken(request)
                if (response.isSuccessful) {
                    emit(NetworkResult.Success(Unit))
                }
                else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            }
            else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        }
        catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
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


    fun updateTask(taskId: Long, request: UpdateTaskRequest): Flow<NetworkResult<TaskResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.updateTask(taskId, request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(NetworkResult.Success(it))
                    } ?: emit(NetworkResult.Error("Empty response"))
                } else {
                    emit(NetworkResult.Error(getErrorMessage(response.code(), response.errorBody()?.string())))
                }
            } else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }

}



