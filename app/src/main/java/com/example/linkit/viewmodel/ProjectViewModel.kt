
package com.example.linkit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.TokenStore
import com.example.linkit.data.models.*
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ProjectRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.util.JwtUtils
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ProjectUiState(
    val projects: List<ProjectResponse> = emptyList(),
    val isLoading: Boolean = false,
    val currentProject: ProjectResponse? = null,
    val tasks: List<TaskResponse> = emptyList(),
    val selectedTab: Int = 0,
    val showDeleteProjectDialog: Boolean = false,
    val loggedInUserId: Long? = null
)

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
        loadProjects()
        loadLoggedInUserId()
    }

    private fun loadLoggedInUserId() {
        viewModelScope.launch {

            authRepository.getUserId().collect { userId ->
                if (userId != null) {
                    _uiState.value = _uiState.value.copy(loggedInUserId = userId)
                }
            }
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            projectRepository.getProjects().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is NetworkResult.Success -> _uiState.value = _uiState.value.copy(projects = result.data, isLoading = false)
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }

    fun selectProject(project: ProjectResponse) {
        _uiState.value = _uiState.value.copy(currentProject = project)
    }

    fun loadProjectById(projectId: Long) {
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
                    is NetworkResult.Success -> _uiState.value = _uiState.value.copy(tasks = result.data, isLoading = false)
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
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


    fun updateTaskStatus(taskId: Long, newStatus: TaskStatus) {
        viewModelScope.launch {

            val currentTask = _uiState.value.tasks.find { it.id == taskId }
            val projectId = _uiState.value.currentProject?.id


            if (currentTask == null) {
                Log.e("TASK_UPDATE_DEBUG", "ERROR: Task not found in state")
                _uiEvent.emit(UiEvent.ShowToast("Error: Task not found. Please refresh."))
                return@launch
            }

            if (projectId == null) {
                Log.e("TASK_UPDATE_DEBUG", "ERROR: Project not found in state")
                _uiEvent.emit(UiEvent.ShowToast("Error: Project not found. Please refresh."))
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
                Log.d("TASK_UPDATE_DEBUG", "Repository Result: $result")
                when (result) {
                    is NetworkResult.Success -> {
                        Log.d("TASK_UPDATE_DEBUG", "SUCCESS: Task updated successfully")
                        _uiEvent.emit(UiEvent.ShowToast("Task status updated to ${newStatus.displayName}!"))
                        loadProjectTasks(projectId)
                    }
                    is NetworkResult.Error -> {
                        Log.e("TASK_UPDATE_DEBUG", "ERROR: ${result.message}")
                        _uiEvent.emit(UiEvent.ShowToast("Failed to update task: ${result.message}"))
                    }
                    is NetworkResult.Loading -> {
                        Log.d("TASK_UPDATE_DEBUG", "Loading...")
                    }
                }
            }
        }
    }

}
