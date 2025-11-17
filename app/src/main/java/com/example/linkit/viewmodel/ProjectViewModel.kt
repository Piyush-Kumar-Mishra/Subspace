package com.example.linkit.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.*
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.PollRepository
import com.example.linkit.data.repo.ProjectRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.util.NetworkResult
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
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject


data class ProjectUiState(
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
    val currentMonth: YearMonth = YearMonth.now()
)

enum class ProjectFilter {
    ALL, TODAY, HIGH, MEDIUM, LOW
}

data class CreateProjectUiState(
    val name: String = "",
    val description: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val priority: ProjectPriority = ProjectPriority.MEDIUM,
    val selectedAssignees: List<ProjectAssigneeResponse> = emptyList(),
    val availableAssignees: List<UserSearchResult> = emptyList(),
    val tags: List<String> = emptyList(),
    val currentTag: String = "",
    val isLoading: Boolean = false,
    val showAssigneeDialog: Boolean = false,
    val showDatePicker: Boolean = false
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
    private val authRepository: AuthRepository
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
        loadLoggedInUser()
        loadProjects()
        loadProjectsForMonth(uiState.value.currentMonth)
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

                    is NetworkResult.Success ->
                        _uiState.update { it.copy(isLoading = false, projects = result.data) }

                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }

    fun loadProjectsForMonth(month: YearMonth) {
        val from = month.atDay(1).format(DateTimeFormatter.ISO_DATE)
        val to = month.atEndOfMonth().format(DateTimeFormatter.ISO_DATE)

        viewModelScope.launch {
            projectRepository.getProjectsFiltered(startDateFrom = from, startDateTo = to)
                .collect { result ->
                    if (result is NetworkResult.Success) {
                        val days = result.data.mapNotNull { proj ->
                            runCatching {
                                LocalDate.parse(proj.startDate.substring(0, 10)).dayOfMonth
                            }.getOrNull()
                        }.toSet()

                        _uiState.update {
                            it.copy(
                                daysWithProjectsInMonth = days,
                                currentMonth = month
                            )
                        }
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
                // Priority filters MUST reset date
                _uiState.update { it.copy(selectedDate = null) }
            }
        }

        loadProjects()
    }

    fun onDateSelected(date: LocalDate?) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                // When selecting a date, override filter
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

    fun updateTaskStatus(taskId: Long, newStatus: TaskStatus) {
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

            projectRepository.updateTask(taskId, request).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("Task status updated!"))
                        loadProjectTasks(projectId)
                    }
                    is NetworkResult.Error -> {
                        _uiEvent.emit(UiEvent.ShowToast("Failed to update task: ${result.message}"))
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onProjectNameChanged(name: String) {
        _createProjectState.value = _createProjectState.value.copy(name = name)
    }

    fun onProjectDescriptionChanged(description: String) {
        _createProjectState.value = _createProjectState.value.copy(description = description)
    }

    fun onProjectStartDateChanged(date: LocalDate) {
        _createProjectState.value = _createProjectState.value.copy(startDate = date)
    }

    fun onProjectPriorityChanged(priority: ProjectPriority) {
        _createProjectState.value = _createProjectState.value.copy(priority = priority)
    }

    fun onCurrentTagChanged(tag: String) {
        _createProjectState.value = _createProjectState.value.copy(currentTag = tag)
    }

    fun addTag() {
        val currentTag = _createProjectState.value.currentTag.trim()
        if (currentTag.isNotEmpty() && !_createProjectState.value.tags.contains(currentTag)) {
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
        val state = _createProjectState.value
        if (state.name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Project name is required")) }
            return
        }

        if (state.selectedAssignees.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("At least one assignee is required")) }
            return
        }

        val request = CreateProjectRequest(
            name = state.name.trim(),
            description = state.description.trim().takeIf { it.isNotEmpty() },
            startDate = state.startDate.format(DateTimeFormatter.ISO_DATE),
            priority = state.priority.name,
            assigneeIds = state.selectedAssignees.map { it.userId },
            tags = state.tags
        )

        _createProjectState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            projectRepository.createProject(request).collect { result ->
                _createProjectState.value = _createProjectState.value.copy(isLoading = false)
                when (result) {
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("Project created successfully"))
                        clearProjectForm()
                        loadProjects()
                        _uiEvent.emit(UiEvent.NavigateBack)
                    }
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast(result.message))
                    else -> Unit
                }
            }
        }
    }

    fun clearProjectForm() {
        _createProjectState.value = CreateProjectUiState()
        _editingProjectId.value = null
    }

    fun onTaskNameChanged(name: String) {
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
        val request = CreateTaskRequest(
            name = state.name.trim(),
            description = state.description.trim().takeIf { it.isNotEmpty() },
            projectId = projectId,
            assigneeId = state.selectedAssignee.userId,
            startDate = state.startDate.format(DateTimeFormatter.ISO_DATE),
            endDate = state.endDate.format(DateTimeFormatter.ISO_DATE),
            status = state.status.name
        )
        _createTaskState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            projectRepository.createTask(request).collect { result ->
                _createTaskState.value = _createTaskState.value.copy(isLoading = false)
                when (result) {
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("Task created successfully"))
                        clearTaskForm()
                        loadProjectTasks(projectId)
                        _uiEvent.emit(UiEvent.NavigateBack)
                    }
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast(result.message))
                    else -> Unit
                }
            }
        }
    }

    fun clearTaskForm() {
        _createTaskState.value = TaskUiState()
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
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
                        _createProjectState.value = CreateProjectUiState(
                            name = project.name,
                            description = project.description ?: "",
                            startDate = LocalDate.parse(project.startDate.take(10)),
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

    fun updateProject() {
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

        val request = UpdateProjectRequest(
            name = state.name.trim(),
            description = state.description.trim().takeIf { it.isNotEmpty() },
            startDate = state.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toString(),
            priority = state.priority.name,
            assigneeIds = state.selectedAssignees.map { it.userId },
            tags = state.tags
        )

        _createProjectState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            projectRepository.updateProject(projectId, request).collect { result ->
                _createProjectState.value = _createProjectState.value.copy(isLoading = false)
                when (result) {
                    is NetworkResult.Success -> {
                        val event = if (_createProjectState.value.showAssigneeDialog) {
                            UiEvent.ShowToast("Assignees updated successfully")
                        } else {
                            UiEvent.ShowToast("Project updated successfully")
                        }
                        _uiEvent.emit(event)

                        loadProjectById(projectId)
                        if (_createProjectState.value.showAssigneeDialog) {
                            toggleAssigneeDialog()
                        } else {
                            _uiEvent.emit(UiEvent.NavigateBack)
                        }
                        clearProjectForm()
                    }
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast(result.message))
                    else -> Unit
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
        val projectToDelete = _uiState.value.currentProject ?: return

        viewModelScope.launch {
            projectRepository.deleteProject(projectToDelete.id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("Project '${projectToDelete.name}' deleted"))
                        _uiState.value = _uiState.value.copy(currentProject = null)
                        loadProjects()
                        _uiEvent.emit(UiEvent.NavigateBack)
                    }
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast(result.message))
                    else -> Unit
                }
            }
            onDismissDeleteProjectDialog()
        }
    }


    private fun checkProjectPollStatus(projectId: Long) {
        viewModelScope.launch {
            pollRepository.getProjectPoll(projectId).collect { result ->
                _uiState.value = _uiState.value.copy(projectHasPoll = result is NetworkResult.Success)
            }
        }
    }



}
