package com.example.linkit.util

sealed class UiEvent {
    data class ShowToast(val msg : String): UiEvent()
    object NavigateToGetStarted: UiEvent()
    object NavigateToAuth: UiEvent()
    object NavigateToEnterDetails: UiEvent()
    object NavigateToMain: UiEvent()
    object NavigateBack : UiEvent()
}