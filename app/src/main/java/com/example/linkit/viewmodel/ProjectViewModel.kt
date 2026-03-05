package com.example.linkit.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.*
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.PollRepository
import com.example.linkit.data.repo.ProjectRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject


data class ProjectUiState(
    val isOffline: Boolean = false,
    val projects: List<ProjectResponse> = emptyList(),
    val isLoading: Boolean = false,
    val currentProject: ProjectResponse? = null,
    val tasks: List<TaskResponse> = emptyList(),
    val attachmentsByTask: Map<Long, List<TaskAttachmentResponse>> = emptyMap(),
    val selectedTab: Int = 0,
    val showDeleteProjectDialog: Boolean = false,
    val loggedInUserId: Long? = null,
    val projectHasPoll: Boolean = false,
    val selectedDate: LocalDate? = null,
    val selectedFilter: ProjectFilter = ProjectFilter.ALL,
    val daysWithProjectsInMonth: Set<Int> = emptySet(),
    val currentMonth: YearMonth = YearMonth.now(),
    val analyticsSummary: ProjectSummaryResponse? = null,
    val analyticsWorkload: List<AssigneeWorkloadResponse> = emptyList(),
    val analyticsProductivity: List<TimeSeriesPointResponse> = emptyList(),
    val analyticsAssigneeStats: List<AssigneeStatsResponse> = emptyList()

)

enum class ProjectFilter {
    ALL, TODAY, HIGH, MEDIUM, LOW
}

data class CreateProjectUiState(
    val name: String = "",
    val description: String = "",
    val endDate: LocalDate = LocalDate.now().plusDays(7), // Default deadline 1 week from now
    val endTime: LocalTime = LocalTime.of(17, 0), // Default 5 PM
    val priority: ProjectPriority = ProjectPriority.MEDIUM,
    val selectedAssignees: List<ProjectAssigneeResponse> = emptyList(),
    val availableAssignees: List<UserSearchResult> = emptyList(),
    val tags: List<String> = emptyList(),
    val currentTag: String = "",
    val isLoading: Boolean = false,
    val showAssigneeDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false
)

data class TaskUiState(
    val name: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val projectId: Long? = null,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(1),
    val selectedAssignee: ProjectAssigneeResponse? = null,
    val availableAssignees: List<ProjectAssigneeResponse> = emptyList(),
    val isLoading: Boolean = false,
    val showAssigneeDialog: Boolean = false,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false
)


@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val profileRepository: ProfileRepository,
    private val pollRepository: PollRepository,
    private val authRepository: AuthRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {


    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState = _uiState.asStateFlow()

    private val _createProjectState = MutableStateFlow(CreateProjectUiState())
    val createProjectState = _createProjectState.asStateFlow()

    private val _createTaskState = MutableStateFlow(TaskUiState())
    val createTaskState = _createTaskState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _editingProjectId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            networkUtils.networkStatus.collect { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
            }
        }

        loadProjects()
        loadLoggedInUser()
        loadProjectsForMonth(YearMonth.now())
    }


    private fun loadLoggedInUser() {
        viewModelScope.launch {
            authRepository.getUserId().collect { id ->
                _uiState.update { it.copy(loggedInUserId = id) }
            }
        }
    }

    fun loadProjects() {
        val state = uiState.value

        val priorityParam = when (state.selectedFilter) {
            ProjectFilter.HIGH -> "HIGH"
            ProjectFilter.MEDIUM -> "MEDIUM"
            ProjectFilter.LOW -> "LOW"
            else -> null
        }

        val dateParam = state.selectedDate?.format(DateTimeFormatter.ISO_DATE)

        viewModelScope.launch {
            projectRepository.getProjectsFiltered(
                priority = priorityParam,
                date = dateParam,
                startDateFrom = null,
                startDateTo = null
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading ->
                        _uiState.update { it.copy(isLoading = true) }

                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false, projects = result.data
                            )
                        }
                    }

                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }

    fun loadProjectsForMonth(month: YearMonth) {

        _uiState.update {
            it.copy(
                currentMonth = month,
                daysWithProjectsInMonth = emptySet()
            )
        }

        val from = month.atDay(1).format(DateTimeFormatter.ISO_DATE)
        val to = month.atEndOfMonth().format(DateTimeFormatter.ISO_DATE)
        viewModelScope.launch {
            projectRepository.getProjectsFiltered(
                startDateFrom = from,
                startDateTo = to
            ).collect { result ->
                if (result is NetworkResult.Success) {
                    val days = result.data.mapNotNull { proj ->
                        runCatching {
                            java.time.Instant.parse(proj.createdAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .let { date ->
                                    if (YearMonth.from(date) == month) date.dayOfMonth else null
                                }
                        }.getOrElse {
                            runCatching { LocalDate.parse(proj.endDate).dayOfMonth }.getOrNull()
                        }
                    }.toSet()

                    _uiState.update { it.copy(daysWithProjectsInMonth = days) }
                }
            }
        }
    }

    fun clearSelectedDate() {
        _uiState.update { it.copy(selectedDate = null) }
    }

    fun applyFilter(filter: ProjectFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }

        when (filter) {
            ProjectFilter.ALL -> _uiState.update { it.copy(selectedDate = null) }

            ProjectFilter.TODAY -> _uiState.update {
                it.copy(selectedDate = LocalDate.now())
            }

            ProjectFilter.HIGH, ProjectFilter.MEDIUM, ProjectFilter.LOW -> {
                _uiState.update { it.copy(selectedDate = null) }
            }
        }
        loadProjects()
    }

    fun onDateSelected(date: LocalDate?) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedFilter = if (date != null) ProjectFilter.ALL else it.selectedFilter
            )
        }

        loadProjects()
    }


    fun goToPreviousMonth() {
        val newMonth = uiState.value.currentMonth.minusMonths(1)
        loadProjectsForMonth(newMonth)
    }

    fun goToNextMonth() {
        val newMonth = uiState.value.currentMonth.plusMonths(1)
        loadProjectsForMonth(newMonth)
    }

    fun selectProject(project: ProjectResponse) {
        _uiState.value = _uiState.value.copy(currentProject = project)
    }

    fun loadProjectById(projectId: Long) {
        checkProjectPollStatus(projectId)
        viewModelScope.launch {
            projectRepository.getProject(projectId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> _uiState.value = _uiState.value.copy(currentProject = result.data)
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast("Failed to load project: ${result.message}"))
                    else -> Unit
                }
            }
        }
    }

    fun loadProjectTasks(projectId: Long) {
        viewModelScope.launch {
            projectRepository.getProjectTasks(projectId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(tasks = result.data, isLoading = false)
                        result.data.forEach { task ->
                            loadTaskAttachments(task.id)
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }

    fun loadTaskAttachments(taskId: Long) {
        viewModelScope.launch {
            projectRepository.getTaskAttachments(taskId).collect { result ->
                if (result is NetworkResult.Success) {
                    val currentAttachments = _uiState.value.attachmentsByTask.toMutableMap()
                    currentAttachments[taskId] = result.data
                    _uiState.value = _uiState.value.copy(attachmentsByTask = currentAttachments)
                }
            }
        }
    }

    fun uploadAttachment(taskId: Long, fileUri: Uri, fileName: String) {
        viewModelScope.launch {

            val currentAttachments = _uiState.value.attachmentsByTask[taskId] ?: emptyList()

            if (currentAttachments.size >= 5) {
                return@launch
            }

            projectRepository.uploadTaskAttachment(taskId, fileUri, fileName).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiEvent.emit(UiEvent.ShowToast("Uploading file..."))
                    }
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("File uploaded successfully!"))
                        uiState.value.currentProject?.id?.let { projectId ->
                            loadProjectTasks(projectId)
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiEvent.emit(UiEvent.ShowToast("Upload failed: ${result.message}"))
                    }
                }
            }
        }
    }

    fun viewAttachment(context: Context, attachment: TaskAttachmentResponse) {
        viewModelScope.launch {
            val localFile = File(context.cacheDir, attachment.fileName)

            if (localFile.exists()) {
                openSecureFile(context, localFile, attachment.mimeType)
                return@launch
            }

            val fullUrl = attachment.downloadUrl

            println("Attempting to download file from S3: $fullUrl")
            val downloadedFile = projectRepository.downloadAndCacheFile(attachment.fileName, fullUrl)

            if (downloadedFile != null) {
                openSecureFile(context, downloadedFile, attachment.mimeType)
            } else {
                _uiEvent.emit(UiEvent.ShowToast("Offline: File not found in cache"))
            }
        }
    }


    private fun openSecureFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            //  no PDF/Image viewer app is installed
        }
    }
    fun updateTaskStatus(taskId: Long, newStatus: TaskStatus) {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            val currentTask = _uiState.value.tasks.find { it.id == taskId }
            val projectId = _uiState.value.currentProject?.id

            if (currentTask == null || projectId == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error: Task or project not found."))
                return@launch
            }

            val request = UpdateTaskRequest(
                name = currentTask.name,
                description = currentTask.description,
                assigneeId = currentTask.assignee.userId,
                startDate = currentTask.startDate,
                endDate = currentTask.endDate,
                status = newStatus.name
            )
            _uiState.update { it.copy(isLoading = true) }

            projectRepository.updateTask(taskId, request).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast("Task status updated!"))
                        loadProjectTasks(projectId)
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast("Failed to update task: ${result.message}"))
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onProjectNameChanged(name: String) {
       if(name.length <= 25){
           _createProjectState.value = _createProjectState.value.copy(name = name)
       }
    }

    fun onProjectDescriptionChanged(description: String) {
        _createProjectState.value = _createProjectState.value.copy(description = description)
    }

    fun onProjectEndDateChanged(date: LocalDate) {
        _createProjectState.value = _createProjectState.value.copy(endDate = date)
    }

    fun onProjectPriorityChanged(priority: ProjectPriority) {
        _createProjectState.value = _createProjectState.value.copy(priority = priority)
    }

    fun onCurrentTagChanged(tag: String) {
        if (tag.length < 12) {
            _createProjectState.value = _createProjectState.value.copy(currentTag = tag)
        }
    }
    fun addTag() {
        val currentTag = _createProjectState.value.currentTag.trim()
        if (currentTag.isNotEmpty() &&!_createProjectState.value.tags.contains(currentTag)) {
            _createProjectState.value = _createProjectState.value.copy(
                tags = _createProjectState.value.tags + currentTag,
                currentTag = ""
            )
        }

    }

    fun removeTag(tag: String) {
        _createProjectState.value = _createProjectState.value.copy(
            tags = _createProjectState.value.tags - tag
        )
    }

    fun toggleAssigneeDialog() {
        val showDialog = !_createProjectState.value.showAssigneeDialog
        _createProjectState.value = _createProjectState.value.copy(showAssigneeDialog = showDialog)
        if (showDialog && _createProjectState.value.availableAssignees.isEmpty()) {
            loadAvailableAssignees()
        }
    }

    fun toggleDatePicker() {
        _createProjectState.value = _createProjectState.value.copy(
            showDatePicker = !_createProjectState.value.showDatePicker
        )
    }

    fun addAssignee(assignee: UserSearchResult) {
        val projectAssignee = ProjectAssigneeResponse(
            userId = assignee.userId,
            name = assignee.name,
            profileImageUrl = assignee.profileImageUrl
        )

        if (!_createProjectState.value.selectedAssignees.any { it.userId == assignee.userId }) {
            _createProjectState.value = _createProjectState.value.copy(
                selectedAssignees = _createProjectState.value.selectedAssignees + projectAssignee
            )
        }
    }

    fun removeAssignee(assignee: ProjectAssigneeResponse) {
        _createProjectState.value = _createProjectState.value.copy(
            selectedAssignees = _createProjectState.value.selectedAssignees - assignee
        )
    }

    fun removeAssignee(user: UserSearchResult) {
        _createProjectState.value = _createProjectState.value.copy(
            selectedAssignees = _createProjectState.value.selectedAssignees.filterNot { it.userId == user.userId }
        )
    }

    private fun loadAvailableAssignees() {
        viewModelScope.launch {
            profileRepository.getConnections().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val userSearchResults = result.data.connections.map { connection ->
                            UserSearchResult(
                                userId = connection.userId, name = connection.name, email = "",
                                company = connection.company, profileImageUrl = connection.profileImageUrl, isConnected = true
                            )
                        }
                        _createProjectState.value = _createProjectState.value.copy(availableAssignees = userSearchResults)
                    }
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast("Failed to load connections: ${result.message}"))
                    else -> Unit
                }
            }
        }
    }

    fun createProject() {
        if (_createProjectState.value.isLoading) return
        val state = _createProjectState.value
        
        if (state.name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Project name is required")) }
            return
        }

        if (state.selectedAssignees.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("At least one assignee is required")) }
            return
        }
        if(state.description.isEmpty()){
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Description is required")) }
            return
        }

        val startDateTime = LocalDateTime.of(state.endDate, state.endTime)
        val formattedStartDate = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()

        _createProjectState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            projectRepository.createProject(
                CreateProjectRequest(
                    name = state.name.trim(),
                    description = state.description.trim().takeIf { it.isNotEmpty() },
                    endDate = formattedStartDate,
                    priority = state.priority.name,
                    assigneeIds = state.selectedAssignees.map { it.userId },
                    tags = state.tags
                )
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _createProjectState.update { it.copy(isLoading = false) }
                        _uiState.update { current ->
                            current.copy(projects = listOf(result.data) + current.projects)
                        }
                        loadProjectsForMonth(uiState.value.currentMonth)
                        _uiEvent.emit(UiEvent.ShowToast("Project created successfully"))
                        clearProjectForm()
                        _uiEvent.emit(UiEvent.NavigateBack)
                    }
                    is NetworkResult.Error -> {
                        _createProjectState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                    is NetworkResult.Loading -> {
                        _createProjectState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun clearProjectForm() {
        _createProjectState.value = CreateProjectUiState()
        _editingProjectId.value = null
    }

    fun onTaskNameChanged(name: String) {
        if(name.length<=25)
        _createTaskState.value = _createTaskState.value.copy(name = name)
    }

    fun onTaskDescriptionChanged(description: String) {
        _createTaskState.value = _createTaskState.value.copy(description = description)
    }

    fun onTaskStatusChanged(status: TaskStatus) {
        _createTaskState.value = _createTaskState.value.copy(status = status)
    }

    fun onTaskStartDateChanged(date: LocalDate) {
        _createTaskState.value = _createTaskState.value.copy(startDate = date)
    }

    fun onTaskEndDateChanged(date: LocalDate) {
        _createTaskState.value = _createTaskState.value.copy(endDate = date)
    }

    fun selectTaskAssignee(assignee: ProjectAssigneeResponse) {
        _createTaskState.value = _createTaskState.value.copy(
            selectedAssignee = assignee,
            showAssigneeDialog = false
        )
    }

    fun toggleTaskAssigneeDialog() {
        val showDialog = !_createTaskState.value.showAssigneeDialog
        _createTaskState.value = _createTaskState.value.copy(showAssigneeDialog = showDialog)
        if (showDialog && _createTaskState.value.availableAssignees.isEmpty()) {
            loadAvailableTaskAssignees()
        }
    }

    private fun loadAvailableTaskAssignees() {
        viewModelScope.launch {
            profileRepository.getConnections().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val assignees = result.data.connections.map { connection ->
                            ProjectAssigneeResponse(
                                userId = connection.userId,
                                name = connection.name,
                                profileImageUrl = connection.profileImageUrl
                            )
                        }
                        _createTaskState.value = _createTaskState.value.copy(availableAssignees = assignees)
                    }
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast("Failed to load connections: ${result.message}"))
                    else -> Unit
                }
            }
        }
    }

    fun toggleTaskStartDatePicker() {
        _createTaskState.value = _createTaskState.value.copy(
            showStartDatePicker = !_createTaskState.value.showStartDatePicker
        )
    }

    fun toggleTaskEndDatePicker() {
        _createTaskState.value = _createTaskState.value.copy(
            showEndDatePicker = !_createTaskState.value.showEndDatePicker
        )
    }

    fun initializeTaskCreation(projectId: Long) {
        _createTaskState.value = _createTaskState.value.copy(projectId = projectId)
        loadProjectById(projectId)
    }

    fun createTask() {
        if (_createTaskState.value.isLoading) return
        val state = _createTaskState.value
        val projectId = state.projectId

        if (projectId == null) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("No project selected")) }
            return
        }
        if (state.name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Task name is required")) }
            return
        }
        if (state.selectedAssignee == null) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Task assignee is required")) }
            return
        }
        if (state.endDate.isBefore(state.startDate)) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("End date cannot be before start date")) }
            return
        }

        _createTaskState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            projectRepository.createTask(
                CreateTaskRequest(
                    name = state.name.trim(),
                    description = state.description.trim().takeIf { it.isNotEmpty() },
                    projectId = projectId,
                    assigneeId = state.selectedAssignee.userId,
                    startDate = state.startDate.format(DateTimeFormatter.ISO_DATE),
                    endDate = state.endDate.format(DateTimeFormatter.ISO_DATE),
                    status = state.status.name
                )
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _createTaskState.update { it.copy(isLoading = false) }
                        _uiState.update { current ->
                            current.copy(
                                projects = current.projects.map {
                                    if (it.id == projectId) it.copy(taskCount = it.taskCount + 1)
                                    else it
                                }
                            )
                        }
                        loadProjectTasks(projectId)
                        _uiEvent.emit(UiEvent.NavigateBack)
                        clearTaskForm()
                    }
                    is NetworkResult.Error -> {
                        _createTaskState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                    is NetworkResult.Loading -> {
                        _createTaskState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun clearTaskForm() {
        _createTaskState.value = TaskUiState()
    }

    fun goBackToProjects() {
        _uiState.value = _uiState.value.copy(
            currentProject = null,
            tasks = emptyList(),
            selectedTab = 0
        )
    }

    fun loadProjectForEditing(projectId: Long, openAssigneeDialog: Boolean = false) {
        viewModelScope.launch {
            projectRepository.getProject(projectId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val project = result.data
                        _editingProjectId.value = project.id

                        val zonedDateTime = try {
                            java.time.Instant.parse(project.endDate)
                                .atZone(ZoneId.systemDefault())
                        }
                        catch (e: Exception) {
                            java.time.ZonedDateTime.now().plusDays(7)
                        }

                        _createProjectState.value = CreateProjectUiState(
                            name = project.name,
                            description = project.description ?: "",

                            endDate = zonedDateTime.toLocalDate(),
                            endTime = zonedDateTime.toLocalTime(),

                            priority = ProjectPriority.valueOf(project.priority),
                            selectedAssignees = project.assignees,
                            tags = project.tags,
                            showAssigneeDialog = openAssigneeDialog
                        )
                        if (openAssigneeDialog) {
                            loadAvailableAssignees()
                        }
                    }
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast("Failed to load project details: ${result.message}"))
                    else -> Unit
                }
            }
        }
    }

    fun onProjectEndTimeChanged(time: LocalTime) {
        _createProjectState.value = _createProjectState.value.copy(endTime = time)
    }

    fun toggleTimePicker() {
        _createProjectState.value = _createProjectState.value.copy(
            showTimePicker = !_createProjectState.value.showTimePicker
        )
    }

    fun updateProject() {
        if (_createProjectState.value.isLoading) return
        val state = _createProjectState.value
        val projectId = _editingProjectId.value ?: return

        if (state.name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Project name is required")) }
            return
        }
        if (state.selectedAssignees.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("At least one assignee is required")) }
            return
        }
        if (state.description.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Description is required")) }
            return
        }


        val endDateTime = LocalDateTime.of(state.endDate, state.endTime)
        val formattedEndDate = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()

        _createProjectState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            projectRepository.updateProject(
                projectId,
                UpdateProjectRequest(
                    name = state.name.trim(),
                    description = state.description.trim().takeIf { it.isNotEmpty() },
                    endDate = formattedEndDate,
                    priority = state.priority.name,
                    assigneeIds = state.selectedAssignees.map { it.userId },
                    tags = state.tags
                )
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _createProjectState.update { it.copy(isLoading = false) }
                        val event = if (state.showAssigneeDialog) {
                            UiEvent.ShowToast("Assignees updated successfully")
                        } else {
                            UiEvent.ShowToast("Project updated successfully")
                        }
                        _uiEvent.emit(event)
                        loadProjectById(projectId)
                        loadProjectsForMonth(uiState.value.currentMonth)
                        if (state.showAssigneeDialog) {
                            toggleAssigneeDialog()
                        } else {
                            _uiEvent.emit(UiEvent.NavigateBack)
                        }
                        clearProjectForm()
                    }
                    is NetworkResult.Error -> {
                        _createProjectState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                    is NetworkResult.Loading -> {
                        _createProjectState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun onDeleteProjectClicked() {
        _uiState.value = _uiState.value.copy(showDeleteProjectDialog = true)
    }

    fun onDismissDeleteProjectDialog() {
        _uiState.value = _uiState.value.copy(showDeleteProjectDialog = false)
    }

    fun confirmDeleteCurrentProject() {
        val state = _uiState.value
        val projectToDelete = _uiState.value.currentProject ?: return
        if (state.isLoading) return

        viewModelScope.launch {
            projectRepository.deleteProject(projectToDelete.id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                projects = it.projects.filterNot { p -> p.id == projectToDelete.id },
                                currentProject = null,
                                showDeleteProjectDialog = false,
                                isLoading = false
                            )
                        }
                        _uiEvent.emit(UiEvent.NavigateBack)
                    }

                    is NetworkResult.Error ->{
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }


                    else -> Unit
                }
            }
        }
    }

    private fun checkProjectPollStatus(projectId: Long) {
        viewModelScope.launch {
            pollRepository.getProjectPoll(projectId).collect { result ->
                _uiState.value = _uiState.value.copy(projectHasPoll = result is NetworkResult.Success)
            }
        }
    }

    fun deleteTask(taskId: Long) {
        val currentProjectId = _uiState.value.currentProject?.id ?: return

        viewModelScope.launch {
            projectRepository.deleteTask(taskId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                // Filter out the deleted task from the current list
                                tasks = state.tasks.filterNot { it.id == taskId },
                                // Update the task count on the current project locally
                                projects = state.projects.map {
                                    if (it.id == currentProjectId) it.copy(taskCount = it.taskCount - 1)
                                    else it
                                }
                            )
                        }
                        _uiEvent.emit(UiEvent.ShowToast("Task deleted successfully"))
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }
}
