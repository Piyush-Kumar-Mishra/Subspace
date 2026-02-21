package com.example.linkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.ChatMessageResponse
import com.example.linkit.data.models.ChatUiState
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ChatRepository
import com.example.linkit.util.JwtUtils
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.TimeUtils
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var currentProjectId: Long? = null
    private var currentUserId: Long? = null
    private var webSocketJob: Job? = null

    init {
        loadCurrentUserId()
        observeWebSocketEvents()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            launch {
                authRepository.getUserId().collect { userId ->
                    if (userId != null) {
                        currentUserId = userId
                        reProcessMessages()
                    }
                }
            }

            launch {
                authRepository.getToken().collect { token ->
                    if (token != null && currentUserId == null) {
                        val userId = JwtUtils.getUserIdFromToken(token)
                        if (userId != null) {
                            currentUserId = userId
                            reProcessMessages()
                        }
                    }
                }
            }
        }
    }

    private fun reProcessMessages() {
        _uiState.update { state ->
            if (state.messages.isNotEmpty()) {
                state.copy(messages = processMessagesForReverseLayout(state.messages))
            } else state
        }
    }

    fun initializeChat(projectId: Long) {
        currentProjectId = projectId
        loadInitialMessages()
        connectWebSocket()
    }

    private fun loadInitialMessages() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            chatRepository.getChatHistory(currentProjectId!!).collect { networkResult ->
                when (networkResult) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        val processedMessages = processMessagesForReverseLayout(networkResult.data.messages)
                        _uiState.update {
                            it.copy(
                                messages = processedMessages,
                                isLoading = false,
                                hasMoreMessages = networkResult.data.hasMore
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = networkResult.message)
                        }
                        _uiEvent.emit(UiEvent.ShowToast("Failed to load messages: ${networkResult.message}"))
                    }
                }
            }
        }
    }

    private fun processMessagesForReverseLayout(
        messages: List<ChatMessageResponse>
    ): List<ChatMessageResponse> {
        if (messages.isEmpty()) return emptyList()

        val sortedList = messages.sortedByDescending { it.createdAt }
        val myId = currentUserId

        return sortedList.mapIndexed { index, message ->
            val nextOlderMessage = if (index < sortedList.lastIndex) sortedList[index + 1] else null

            val dateHeader = TimeUtils.formatDateHeader(message.createdAt)
            val olderDateHeader = nextOlderMessage?.let { TimeUtils.formatDateHeader(it.createdAt) }

            val showDateHeader = dateHeader != olderDateHeader

            val isOwn = if (message.sender != null) {
                message.sender.userId == myId
            } else {
                message.isOwnMessage
            }

            message.copy(
                isOwnMessage = isOwn,
                showDateHeader = showDateHeader,
                dateHeader = if (showDateHeader) dateHeader else null,
                formattedTime = TimeUtils.formatTime(message.createdAt)
            )
        }
    }

    fun loadMoreMessages() {
        if (currentProjectId == null || !_uiState.value.hasMoreMessages || _uiState.value.isLoadingMore) return

        val oldestMessageTime = _uiState.value.messages.lastOrNull()?.createdAt ?: return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            chatRepository.getChatHistory(currentProjectId!!, oldestMessageTime).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val currentList = _uiState.value.messages

                        val combinedRaw = (currentList + result.data.messages).distinctBy { it.id }
                        val finalProcessed = processMessagesForReverseLayout(combinedRaw)

                        _uiState.update {
                            it.copy(
                                messages = finalProcessed,
                                isLoadingMore = false,
                                hasMoreMessages = result.data.hasMore
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoadingMore = false) }
                        _uiEvent.emit(UiEvent.ShowToast("Failed to load history"))
                    }
                    else -> {}
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        if (messageText.isEmpty() || currentProjectId == null) return

        val tempId = System.currentTimeMillis()
        val tempMessage = ChatMessageResponse(
            id = tempId,
            projectId = currentProjectId!!,
            sender = null,
            messageType = "USER_MESSAGE",
            content = messageText,
            createdAt = TimeUtils.getCurrentIsoTime(),
            formattedTime = "Now",
            isOwnMessage = true,
            systemEventType = null,
            systemEventData = null,
            showDateHeader = false,
            dateHeader = null
        )

        _uiState.update { state ->
            val updatedList = (state.messages + tempMessage)
            state.copy(
                messages = processMessagesForReverseLayout(updatedList),
                isSending = true,
                inputText = ""
            )
        }

        viewModelScope.launch {
            chatRepository.sendMessage(currentProjectId!!, messageText).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val realMessage = result.data

                        _uiState.update { state ->
                            val listWithoutTemp = state.messages.filter { it.id != tempId }

                            val finalList = if (listWithoutTemp.any { it.id == realMessage.id }) {
                                listWithoutTemp.map {
                                    if (it.id == realMessage.id) realMessage else it
                                }
                            } else {
                                listWithoutTemp + realMessage
                            }

                            state.copy(
                                messages = processMessagesForReverseLayout(finalList),
                                isSending = false
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { state ->
                            val cleanList = state.messages.filter { it.id != tempId }
                            state.copy(
                                isSending = false,
                                inputText = messageText,
                                messages = cleanList
                            )
                        }
                        _uiEvent.emit(UiEvent.ShowToast("Send failed: ${result.message}"))
                    }
                    else -> {}
                }
            }
        }
    }

    private fun connectWebSocket() {
        currentProjectId?.let { chatRepository.connectToChat(it) }
    }

    private fun observeWebSocketEvents() {
        webSocketJob = viewModelScope.launch {
            launch {
                chatRepository.incomingMessages.collect { message ->
                    handleIncomingMessage(message)
                }
            }

            launch {
                chatRepository.connectionStatus.collect { isConnected ->
                    _uiState.update { it.copy(isConnected = isConnected) }
                }
            }
        }
    }

    private fun handleIncomingMessage(message: ChatMessageResponse) {
        if (_uiState.value.messages.any { it.id == message.id }) return

        _uiState.update { state ->
            val newList = state.messages + message
            state.copy(messages = processMessagesForReverseLayout(newList))
        }
    }


    fun disconnect() {
        webSocketJob?.cancel()
        currentProjectId?.let { chatRepository.disconnectFromChat(it) }
        currentProjectId = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
