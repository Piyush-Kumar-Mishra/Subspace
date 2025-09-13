package com.example.linkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.util.JwtUtils
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val offlineMessage: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        // React automatically to network changes
        viewModelScope.launch {
            networkUtils.networkStatus.collect { isOnline ->
                _uiState.value = _uiState.value.copy(
                    isOffline = !isOnline,
                    offlineMessage = if (!isOnline) "You are offline. Internet connection required." else ""
                )
            }
        }
    }

    fun checkAuthentication() {
        viewModelScope.launch {
            val isOnline = !_uiState.value.isOffline

            authRepository.getToken().collectLatest { token ->
                if (token != null) {
                    if (JwtUtils.isTokenExpired(token)) {
                        authRepository.clearToken()
                        _uiEvent.emit(UiEvent.NavigateToAuth)
                    } else {
                        if (isOnline) {
                            profileRepository.getProfileStatus().collectLatest { result ->
                                when (result) {
                                    is NetworkResult.Success -> {
                                        if (result.data.profileCompleted) {
                                            _uiEvent.emit(UiEvent.NavigateToMain)
                                        } else {
                                            _uiEvent.emit(UiEvent.NavigateToEnterDetails)
                                        }
                                    }
                                    is NetworkResult.Error -> {
                                        if (isTokenExpiredError(result.message)) {
                                            authRepository.clearToken()
                                            _uiEvent.emit(UiEvent.NavigateToAuth)
                                        } else {
                                            _uiEvent.emit(UiEvent.NavigateToEnterDetails)
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        } else {
                            profileRepository.getProfileStatus().collectLatest { result ->
                                when (result) {
                                    is NetworkResult.Success -> {
                                        if (result.data.profileCompleted) {
                                            _uiEvent.emit(UiEvent.NavigateToMain)
                                        } else {
                                            _uiEvent.emit(UiEvent.NavigateToEnterDetails)
                                        }
                                    }
                                    is NetworkResult.Error -> {
                                        _uiEvent.emit(UiEvent.NavigateToMain)
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                } else {
                    if (isOnline) {
                        _uiEvent.emit(UiEvent.NavigateToGetStarted)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            offlineMessage = "You are offline. Internet required to log in."
                        )
                        _uiEvent.emit(UiEvent.NavigateToAuth)
                    }
                }
            }
        }
    }

    private fun isTokenExpiredError(message: String): Boolean {
        val expiredIndicators = listOf(
            "unauthorized", "invalid token", "token expired", "expired",
            "401", "403", "jwt expired", "token not found"
        )
        return expiredIndicators.any { message.contains(it, ignoreCase = true) }
    }

    fun onGetStarted() {
        viewModelScope.launch { _uiEvent.emit(UiEvent.NavigateToAuth) }
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(isLoginMode = !_uiState.value.isLoginMode)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onUsernameChanged(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onSubmit() {
        val state = _uiState.value

        if (state.isOffline) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowToast("Internet connection required"))
            }
            return
        }

        if (state.username.isBlank() || state.password.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowToast("Please enter both username and password"))
            }
            return
        }

        if (!state.isLoginMode && state.email.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowToast("Please enter a valid email for sign up"))
            }
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val result = if (state.isLoginMode) {
                authRepository.login(state.username, state.password)
            } else {
                authRepository.register(state.email, state.username, state.password)
            }

            result.collectLatest { networkResult ->
                when (networkResult) {
                    is NetworkResult.Loading -> Unit
                    is NetworkResult.Success -> {
                        _uiState.value = state.copy(isLoading = false)
                        clearAllState()
                        if (state.isLoginMode) {
                            checkProfileCompletion()
                        } else {
                            authRepository.login(state.username, state.password).collectLatest { loginRes ->
                                when (loginRes) {
                                    is NetworkResult.Success -> _uiEvent.emit(UiEvent.NavigateToEnterDetails)
                                    is NetworkResult.Error -> {
                                        _uiEvent.emit(UiEvent.ShowToast("Auto-login failed: ${loginRes.message}"))
                                        _uiEvent.emit(UiEvent.NavigateToAuth)
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = state.copy(isLoading = false)
                        _uiEvent.emit(UiEvent.ShowToast(networkResult.message))
                    }
                }
            }
        }
    }

    private fun checkProfileCompletion() {
        viewModelScope.launch {
            profileRepository.getProfileStatus().collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        if (result.data.profileCompleted) {
                            _uiEvent.emit(UiEvent.NavigateToMain)
                        } else {
                            _uiEvent.emit(UiEvent.NavigateToEnterDetails)
                        }
                    }
                    is NetworkResult.Error -> {
                        if (isTokenExpiredError(result.message)) {
                            authRepository.clearToken()
                            _uiEvent.emit(UiEvent.NavigateToAuth)
                        } else {
                            _uiEvent.emit(UiEvent.NavigateToEnterDetails)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun clearAllState() {
        _uiState.value = AuthUiState(isOffline = _uiState.value.isOffline)
    }

    fun logout() {
        viewModelScope.launch {
            clearAllState()
            authRepository.clearToken()
            _uiEvent.emit(UiEvent.NavigateToGetStarted)
        }
    }
}
