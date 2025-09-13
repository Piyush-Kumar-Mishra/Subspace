package com.example.linkit.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.*
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.util.ImageUtils
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val aboutMe: String = "",
    val profileImageBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<UserSearchResult> = emptyList(),
    val connections: List<ConnectionResponse> = emptyList(),
    val selectedTab: Int = 0,
    val isOffline: Boolean = false,
    val offlineMessage: String = ""
)

data class ViewedProfileState(
    val profile: ProfileResponse? = null,
    val connections: List<ConnectionResponse> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val authRepository: AuthRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _profileState = MutableStateFlow<NetworkResult<ProfileResponse>>(NetworkResult.Loading())
    val profileState = _profileState.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    private val _viewedProfileState = MutableStateFlow(ViewedProfileState())
    val viewedProfileState = _viewedProfileState.asStateFlow()



    init {
        viewModelScope.launch {
            networkUtils.networkStatus.collect { isOnline ->
                _uiState.value = _uiState.value.copy(
                    isOffline = !isOnline,
                    offlineMessage = if (!isOnline) "You are offline" else "You are offline"
                )
            }
        }

        loadToken()
        loadProfile()
        loadConnections()
    }

    private fun loadToken() {
        viewModelScope.launch {
            _token.value = authRepository.getToken().first()
        }
    }

    private fun isTokenExpiredError(message: String): Boolean {
        val expiredTokenIndicators = listOf(
            "unauthorized", "invalid token", "token expired", "expired",
            "401", "403", "jwt expired", "token not found"
        )
        return expiredTokenIndicators.any { indicator ->
            message.contains(indicator, ignoreCase = true)
        }
    }

    fun clearAllState() {
        _uiState.value = ProfileUiState(isOffline = _uiState.value.isOffline)
        _profileState.value = NetworkResult.Loading()
        _token.value = null
    }

    fun refreshAllData() {
        clearAllState()
        loadToken()
        loadProfile()
        loadConnections()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            profileRepo.getUserProfile()
                .catch { e -> _uiEvent.emit(UiEvent.ShowToast("Error: ${e.message}")) }
                .collect { result ->
                    _profileState.value = result
                    when (result) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                name = result.data.name,
                                jobTitle = result.data.jobTitle ?: "",
                                company = result.data.company ?: "",
                                aboutMe = result.data.aboutMe ?: ""
                            )
                        }
                        is NetworkResult.Error -> {
                            if (isTokenExpiredError(result.message)) {
                                authRepository.clearToken()
                                _uiEvent.emit(UiEvent.ShowToast("Session expired. Please log in again."))
                                _uiEvent.emit(UiEvent.NavigateToAuth)
                            }
                        }
                        else -> Unit
                    }
                }
        }
    }

    private fun loadConnections() {
        viewModelScope.launch {
            profileRepo.getConnections().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(connections = result.data.connections)
                    }
                    is NetworkResult.Error -> {
                        if (isTokenExpiredError(result.message)) {
                            authRepository.clearToken()
                            _uiEvent.emit(UiEvent.ShowToast("Session expired. Please log in again."))
                            _uiEvent.emit(UiEvent.NavigateToAuth)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun createProfile() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Name is required")) }
            return
        }

        _uiState.value = state.copy(isLoading = true)

        viewModelScope.launch {
            val imageBase64 = state.profileImageBitmap?.let { ImageUtils.compressImage(it) }
            val request = CreateProfileRequest(
                name = state.name.trim(),
                jobTitle = state.jobTitle.takeIf { it.isNotBlank() },
                company = state.company.takeIf { it.isNotBlank() },
                aboutMe = state.aboutMe.takeIf { it.isNotBlank() },
                profileImageBase64 = imageBase64
            )

            profileRepo.createProfile(request).collect { result ->
                _uiState.value = state.copy(isLoading = false)
                when (result) {
                    is NetworkResult.Success -> _uiEvent.emit(UiEvent.NavigateToMain)
                    is NetworkResult.Error -> {
                        if (isTokenExpiredError(result.message)) {
                            authRepository.clearToken()
                            _uiEvent.emit(UiEvent.ShowToast("Session expired. Please log in again."))
                            _uiEvent.emit(UiEvent.NavigateToAuth)
                        } else {
                            _uiEvent.emit(UiEvent.ShowToast(result.message))
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun updateProfile() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("Name is required")) }
            return
        }

        _uiState.value = state.copy(isLoading = true)

        viewModelScope.launch {
            val imageBase64 = state.profileImageBitmap?.let { ImageUtils.compressImage(it) }
            val request = UpdateProfileRequest(
                name = state.name.trim(),
                jobTitle = state.jobTitle.takeIf { it.isNotBlank() },
                company = state.company.takeIf { it.isNotBlank() },
                aboutMe = state.aboutMe.takeIf { it.isNotBlank() },
                profileImageBase64 = imageBase64
            )

            profileRepo.updateProfile(request).collect { result ->
                _uiState.value = state.copy(isLoading = false)
                when (result) {
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("Profile updated successfully"))
                        loadProfile()
                    }
                    is NetworkResult.Error -> {
                        if (isTokenExpiredError(result.message)) {
                            authRepository.clearToken()
                            _uiEvent.emit(UiEvent.ShowToast("Session expired. Please log in again."))
                            _uiEvent.emit(UiEvent.NavigateToAuth)
                        } else {
                            _uiEvent.emit(UiEvent.ShowToast(result.message))
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun searchUsers(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            profileRepo.searchUsers(query).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(searchResults = result.data.users)
                    }
                    is NetworkResult.Error -> {
                        if (isTokenExpiredError(result.message)) {
                            authRepository.clearToken()
                            _uiEvent.emit(UiEvent.ShowToast("Session expired. Please log in again."))
                            _uiEvent.emit(UiEvent.NavigateToAuth)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun addConnection(email: String) {
        viewModelScope.launch {
            profileRepo.addConnection(email).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiEvent.emit(UiEvent.ShowToast("Connection added successfully"))
                        loadConnections()
                        _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList())
                    }
                    is NetworkResult.Error -> {
                        if (isTokenExpiredError(result.message)) {
                            authRepository.clearToken()
                            _uiEvent.emit(UiEvent.ShowToast("Session expired. Please log in again."))
                            _uiEvent.emit(UiEvent.NavigateToAuth)
                        } else {
                            _uiEvent.emit(UiEvent.ShowToast(result.message))
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onNameChanged(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun onJobTitleChanged(jobTitle: String) { _uiState.value = _uiState.value.copy(jobTitle = jobTitle) }
    fun onCompanyChanged(company: String) { _uiState.value = _uiState.value.copy(company = company) }
    fun onAboutMeChanged(aboutMe: String) { _uiState.value = _uiState.value.copy(aboutMe = aboutMe) }
    fun onImageSelected(bitmap: Bitmap) { _uiState.value = _uiState.value.copy(profileImageBitmap = bitmap) }
    fun onTabSelected(tabIndex: Int) { _uiState.value = _uiState.value.copy(selectedTab = tabIndex) }

    fun logout() {
        viewModelScope.launch {
            clearAllState()
            authRepository.clearToken()
            _uiEvent.emit(UiEvent.NavigateToGetStarted)
        }
    }


    fun viewUserProfile(userId: Long) {
        viewModelScope.launch {
            _viewedProfileState.value = ViewedProfileState(isLoading = true)

            val profileDeferred = async { profileRepo.getProfileById(userId) }
            val connectionsDeferred = async { profileRepo.getConnectionsById(userId) }

            val profileFlow = profileDeferred.await()
            val connectionsFlow = connectionsDeferred.await()

            var finalProfile: ProfileResponse? = null
            var finalConnections: List<ConnectionResponse> = emptyList()

            profileFlow.collect { result ->
                when(result) {
                    is NetworkResult.Success -> finalProfile = result.data
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast(result.message))
                    is NetworkResult.Loading -> Unit
                }
            }

            connectionsFlow.collect { result ->
                when(result) {
                    is NetworkResult.Success -> finalConnections = result.data.connections
                    is NetworkResult.Error -> _uiEvent.emit(UiEvent.ShowToast(result.message))
                    is NetworkResult.Loading -> Unit
                }
            }

            _viewedProfileState.value = ViewedProfileState(
                isLoading = false,
                profile = finalProfile,
                connections = finalConnections
            )
        }
    }

    fun closeUserProfileSheet() {
        _viewedProfileState.value = ViewedProfileState()
    }


}
