package com.example.linkit.util

sealed class UiEvent {
    data class ShowToast(val msg : String): UiEvent()
    object NavigateToGetStarted: UiEvent()
    object NavigateToAuth: UiEvent()
    object NavigateToEnterDetails: UiEvent()
    object NavigateToMain: UiEvent()
    object NavigateToProfile : UiEvent()
    object NavigateBack : UiEvent()
    object NavigateToTaskScreen : UiEvent()
    data class NavigateToProject(val projectId: Long) : UiEvent()
    data class NavigateToTask(val taskId: Long) : UiEvent()
    data class NavigateToCreateProject(val route: String) : UiEvent()
    data class NavigateToCreateTask(val projectId: Long) : UiEvent()
    data class NavigateToCreatePoll(val projectId: Long) : UiEvent()
    data class NavigateToRoute(val route: String) : UiEvent()
}