package com.example.linkit.data.repo

import android.content.Context
import android.net.Uri
import com.example.linkit.data.TokenStore
import com.example.linkit.data.api.ProjectApiService
import com.example.linkit.data.local.dao.AttachmentDao
import com.example.linkit.data.local.dao.ProjectDao
import com.example.linkit.data.local.dao.TaskDao
import com.example.linkit.data.local.entities.AttachmentEntity
import com.example.linkit.data.local.entities.ProjectEntity
import com.example.linkit.data.local.entities.TaskEntity
import com.example.linkit.data.models.*
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ProjectRepository @Inject constructor(
    private val api: ProjectApiService,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val attachmentDao: AttachmentDao,
    private val tokenStore: TokenStore,
    private val networkUtils: NetworkUtils,
    private val context: Context,

) {

    private val gson = com.google.gson.Gson()

    fun getProjectsFiltered(
        priority: String? = null,
        date: String? = null,
        startDateFrom: String? = null,
        startDateTo: String? = null
    ): Flow<NetworkResult<List<ProjectResponse>>> = flow {
        emit(NetworkResult.Loading())
        val token = tokenStore.token.first()

        if (token != null && networkUtils.isInternetAvailable()) {
            try {
                val response = api.getProjects(priority, date, null, startDateFrom, startDateTo)
                if (response.isSuccessful && response.body() != null) {
                    val projects = response.body()!!.projects
                    cacheProjects(projects)
                    emit(NetworkResult.Success(projects))
                } else {
                    emit(NetworkResult.Success(getCachedProjects()))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Success(getCachedProjects()))
            }
        } else {
            emit(NetworkResult.Success(getCachedProjects()))
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
                    emit(
                        NetworkResult.Error(
                            getErrorMessage(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    )
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
                    emit(
                        NetworkResult.Error(
                            getErrorMessage(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    )
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

    fun updateProject(
        projectId: Long,
        request: UpdateProjectRequest
    ): Flow<NetworkResult<ProjectResponse>> = flow {
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
                    emit(
                        NetworkResult.Error(
                            getErrorMessage(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    )
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
                    emit(
                        NetworkResult.Error(
                            getErrorMessage(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    )
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

        if (networkUtils.isInternetAvailable()) {
            try {
                val response = api.getProjectTasks(projectId)
                if (response.isSuccessful && response.body() != null) {
                    val tasks = response.body()!!.tasks
                    cacheTasks(projectId, tasks)
                    emit(NetworkResult.Success(tasks))
                    return@flow
                }
            }
            catch (e: Exception) {

            }
        }

        val localTasks = getCachedTasks(projectId)
        emit(NetworkResult.Success(localTasks))
    }

    private suspend fun cacheProjects(projects: List<ProjectResponse>) {
        val gson = com.google.gson.Gson()
        val entities = projects.map {
            ProjectEntity(
                id = it.id,
                name = it.name,
                description = it.description,
                endDate = it.endDate,
                priority = it.priority,
                taskCount = it.taskCount,
                createdAt = it.createdAt,
                assigneesJson = gson.toJson(it.assignees),
                tagsJson = gson.toJson(it.tags)
            )
        }
        projectDao.deleteAllProjects()
        projectDao.insertProjects(entities)
    }

    private suspend fun cacheTasks(projectId: Long, tasks: List<TaskResponse>) {
        val entities = tasks.map {
            TaskEntity(
                id = it.id,
                projectId = it.projectId,
                name = it.name,
                description = it.description,
                status = it.status,
                startDate = it.startDate,
                endDate = it.endDate,
                assigneeId = it.assignee.userId,
                assigneeName = it.assignee.name,
                assigneeImageUrl = it.assignee.profileImageUrl,

            )
        }
        taskDao.deleteTasksByProject(projectId)
        taskDao.insertTasks(entities)
    }

    private suspend fun getCachedTasks(projectId: Long): List<TaskResponse> {
        return taskDao.getTasksForProject(projectId).first().map {
            val assignee = ProjectAssigneeResponse(it.assigneeId, it.assigneeName, it.assigneeImageUrl)
            TaskResponse(
                id = it.id,
                name = it.name,
                description = it.description,
                projectId = it.projectId,
                assignee = assignee,
                creator = assignee,
                startDate = it.startDate,
                endDate = it.endDate,
                status = it.status,
                createdBy = 0L,
                createdAt = it.startDate,
                attachmentCount = 0
            )
        }
    }

    private suspend fun getCachedProjects(): List<ProjectResponse> {
        val assigneeListType =
            object : com.google.gson.reflect.TypeToken<List<ProjectAssigneeResponse>>() {}.type
        val tagListType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type

        return projectDao.getAllProjects().first().map { entity ->
            val assigneesList: List<ProjectAssigneeResponse> =
                gson.fromJson(entity.assigneesJson, assigneeListType) ?: emptyList()

            val tagsList: List<String> =
                gson.fromJson(entity.tagsJson, tagListType) ?: emptyList()

            ProjectResponse(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                endDate = entity.endDate,
                priority = entity.priority,
                assignees = assigneesList,
                createdBy = 0L,
                createdAt = entity.createdAt,
                taskCount = entity.taskCount,
                tags = tagsList
            )
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
                    emit(
                        NetworkResult.Error(
                            getErrorMessage(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    )
                }
            } else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }


    fun getTaskAttachments(taskId: Long): Flow<NetworkResult<List<TaskAttachmentResponse>>> = flow {
        emit(NetworkResult.Loading())

        if (networkUtils.isInternetAvailable()) {
            try {
                val response = api.getTaskAttachments(taskId)
                if (response.isSuccessful && response.body() != null) {
                    val attachments = response.body()!!.attachments
                    val entities = attachments.map {
                        AttachmentEntity(
                            id = it.id,
                            taskId = it.taskId,
                            fileName = it.fileName,
                            downloadUrl = it.downloadUrl,
                            fileSize = it.fileSize,
                            mimeType = it.mimeType,
                            uploadedByName = it.uploadedBy.name,
                            uploadedById = it.uploadedBy.userId,
                            uploadedByImageUrl = it.uploadedBy.profileImageUrl
                        )
                    }
                    attachmentDao.deleteAttachmentsByTask(taskId)
                    attachmentDao.insertAttachments(entities)

                    emit(NetworkResult.Success(attachments))
                    return@flow
                }
            } catch (e: Exception) { }
        }

        val localEntities = attachmentDao.getAttachmentsForTask(taskId).first()
        val responseList = localEntities.map {
            val localFile = File(context.cacheDir, it.fileName)
            val workingUrl = if (localFile.exists()) Uri.fromFile(localFile).toString() else it.downloadUrl

            TaskAttachmentResponse(
                id = it.id,
                taskId = it.taskId,
                fileName = it.fileName,
                filePath = "",
                downloadUrl = workingUrl,
                fileSize = it.fileSize,
                mimeType = it.mimeType,
                uploadedBy = ProjectAssigneeResponse(it.uploadedById, it.uploadedByName, it.uploadedByImageUrl),
                uploadedAt = ""
            )
        }
        emit(NetworkResult.Success(responseList))
    }

    fun uploadTaskAttachment(
        taskId: Long,
        fileUri: Uri,
        fileName: String
    ): Flow<NetworkResult<TaskAttachmentResponse>> = flow {
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
                inputStream.close()

                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    fileName,
                    requestBody
                )
                val response = api.uploadTaskAttachment(taskId, multipartBody)

                if (response.isSuccessful && response.body() != null) {
                    emit(NetworkResult.Success(response.body()!!))
                } else {
                    emit(
                        NetworkResult.Error(
                            getErrorMessage(
                                response.code(),
                                response.errorBody()?.string()
                            )
                        )
                    )
                }
            } else {
                emit(NetworkResult.Error("No internet connection or invalid token"))
            }
        } catch (e: Exception) {
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


    fun updateTask(taskId: Long, request: UpdateTaskRequest): Flow<NetworkResult<TaskResponse>> =
        flow {
            emit(NetworkResult.Loading())
            try {
                val token = tokenStore.token.first()
                if (token != null && networkUtils.isInternetAvailable()) {
                    val response = api.updateTask(taskId, request)
                    if (response.isSuccessful) {
                        response.body()?.let {
                            emit(NetworkResult.Success(it))
                        } ?: emit(NetworkResult.Error("Empty response"))
                    }
                    else {
                        emit(
                            NetworkResult.Error(
                                getErrorMessage(
                                    response.code(),
                                    response.errorBody()?.string()
                                )
                            )
                        )
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


    suspend fun downloadAndCacheFile(fileName: String, downloadUrl: String): File? {
        val file = File(context.cacheDir, fileName)
        if (file.exists()) return file

        return try {
            val response = api.downloadFile(downloadUrl)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    FileOutputStream(file).use { output ->
                        body.byteStream().copyTo(output)
                    }
                    file
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteTask(taskId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        try {
            val token = tokenStore.token.first()
            if (token != null && networkUtils.isInternetAvailable()) {
                val response = api.deleteTask(taskId)
                if (response.isSuccessful) {
                    taskDao.deleteTaskById(taskId) // Ensure you have this method in your TaskDao
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
}

