package com.example.linkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.CreatePollRequest
import com.example.linkit.data.models.PollResponse
import com.example.linkit.data.repo.PollRepository
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PollUiState(
    val isLoading: Boolean = false,
    val poll: PollResponse? = null,
    val question: String = "",
    val options: List<String> = listOf("", ""),
    val allowMultipleAnswers: Boolean = false
)

@HiltViewModel
class PollViewModel @Inject constructor(
    private val pollRepository: PollRepository
) : ViewModel() {

    private val _pollState = MutableStateFlow(PollUiState())
    val pollState = _pollState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onPollQuestionChanged(question: String) { _pollState.value = _pollState.value.copy(question = question) }
    fun onPollOptionChanged(index: Int, text: String) {
        val newOptions = _pollState.value.options.toMutableList().also { it[index] = text }
        _pollState.value = _pollState.value.copy(options = newOptions)
    }
    fun addPollOption() { _pollState.value = _pollState.value.copy(options = _pollState.value.options + "") }
    fun removePollOption(index: Int) {
        if (_pollState.value.options.size > 2) {
            val newOptions = _pollState.value.options.toMutableList().also { it.removeAt(index) }
            _pollState.value = _pollState.value.copy(options = newOptions)
        }
    }
    fun onAllowMultipleAnswersChanged(isEnabled: Boolean) { _pollState.value = _pollState.value.copy(allowMultipleAnswers = isEnabled) }
    fun clearCreateForm() { _pollState.value = PollUiState() }

    fun createPoll(projectId: Long) {
        viewModelScope.launch {
            val state = _pollState.value
            val validOptions = state.options.filter { it.isNotBlank() }
            if (state.question.isBlank() || validOptions.size < 2) {
                _uiEvent.emit(UiEvent.ShowToast("Question and at least two options are required."))
                return@launch
            }
            val request = CreatePollRequest(state.question, validOptions, state.allowMultipleAnswers)
            pollRepository.createProjectPoll(projectId, request).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _pollState.value = _pollState.value.copy(isLoading = true)
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("Poll created!"))
                        _pollState.value = _pollState.value.copy(poll = result.data, isLoading = false)

                        _uiEvent.emit(UiEvent.NavigateBack)
                    }
                    is NetworkResult.Error -> {
                        _pollState.value = _pollState.value.copy(isLoading = false)
                        _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }

    fun getPoll(projectId: Long) {
        viewModelScope.launch {
            pollRepository.getProjectPoll(projectId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _pollState.value = _pollState.value.copy(isLoading = true)
                    is NetworkResult.Success -> _pollState.value = _pollState.value.copy(poll = result.data, isLoading = false)
                    is NetworkResult.Error -> {
                        _pollState.value = _pollState.value.copy(isLoading = false, poll = null)
                        if (result.message != "NOT_FOUND") _uiEvent.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }

    fun voteOnPoll(pollId: Long, optionId: Long) {
        viewModelScope.launch {
            pollRepository.voteOnPoll(pollId, optionId).collect { result ->
                if (result is NetworkResult.Success) {
                    _pollState.value = _pollState.value.copy(poll = result.data)
                } else if (result is NetworkResult.Error) {
                    _uiEvent.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }
}