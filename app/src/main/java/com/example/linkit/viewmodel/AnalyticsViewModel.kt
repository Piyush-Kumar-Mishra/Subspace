package com.example.linkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.*
import com.example.linkit.data.repo.AnalyticsRepository
import com.example.linkit.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val summary: ProjectSummaryResponse? = null,
    val workload: List<AssigneeWorkloadResponse> = emptyList(),
    val productivity: List<TimeSeriesPointResponse> = emptyList(),
    val assigneeStats: List<AssigneeStatsResponse> = emptyList(),
    val selectedTab: Int = 0,
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

fun loadAnalytics(projectId: Long) {
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
        repository.getProjectSummary(projectId).collect { res ->
            when (res) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(summary = res.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = res.message) }
                }
                else -> Unit
            }
        }
    }

    viewModelScope.launch {
        repository.getWorkload(projectId).collect { res ->
            when (res) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(workload = res.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = res.message) }
                }
                else -> Unit
            }
        }
    }

    viewModelScope.launch {
        repository.getProductivity(projectId).collect { res ->
            when (res) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(productivity = res.data.timeseries) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = res.message) }
                }
                else -> Unit
            }
        }
    }


    viewModelScope.launch {
        repository.getAssigneeStats(projectId).collect { res ->
            when (res) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            assigneeStats = res.data.assignees,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            error = res.message,
                            isLoading = false
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}