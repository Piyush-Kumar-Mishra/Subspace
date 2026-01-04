package com.example.linkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.ChatMessageResponse
import com.example.linkit.data.models.ChatUiState
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ChatRepository
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.TimeUtils
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var typingDebounceJob: Job? = null
    private var webSocketJob: Job? = null

    init {
        loadCurrentUserId()
        observeWebSocketEvents()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            authRepository.getUserId().collect { userId ->
                currentUserId = userId
            }
        }
    }

    fun initializeChat(projectId: Long) {
        currentProjectId = projectId
        loadInitialMessages()
        connectWebSocket()
        loadOnlineUsers()
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

        return sortedList.mapIndexed { index, message ->
            val nextOlderMessage = if (index < sortedList.lastIndex) sortedList[index + 1] else null

            val dateHeader = TimeUtils.formatDateHeader(message.createdAt)
            val olderDateHeader = nextOlderMessage?.let { TimeUtils.formatDateHeader(it.createdAt) }

            val showDateHeader = dateHeader != olderDateHeader

            message.copy(
                isOwnMessage = message.sender?.userId == currentUserId,
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
        handleTypingIndicator()
    }

    private fun handleTypingIndicator() {
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            delay(300)
            currentProjectId?.let { projectId ->
                // Fire and forget typing status
                chatRepository.updateTypingStatus(projectId, true).collect {}
                delay(2000)
                chatRepository.updateTypingStatus(projectId, false).collect {}
            }
        }
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
                messages = processMessagesForReverseLayout(updatedList), // Re-sorts putting temp at Index 0
                isSending = true,
                inputText = "",
                typingUsers = emptyMap()
            )
        }

        // 2. Network Call
        viewModelScope.launch {
            chatRepository.sendMessage(currentProjectId!!, messageText).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // Server returned real message. Replace temp message.
                        val realMessage = result.data
                        _uiState.update { state ->
                            // Remove temp, add real, re-process
                            val cleanList = state.messages.filter { it.id != tempId } + realMessage
                            state.copy(
                                messages = processMessagesForReverseLayout(cleanList),
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

    // WebSocket Logic
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
                chatRepository.typingUpdates.collect { (userId, userName) ->
                    handleTypingUpdate(userId, userName)
                }
            }
            launch {
                chatRepository.onlineUsersUpdates.collect { onlineUsers ->
                    _uiState.update { it.copy(onlineUsers = onlineUsers) }
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

        val updatedMsg = message.copy(
            isOwnMessage = message.sender?.userId == currentUserId
        )

        _uiState.update { state ->
            val newList = state.messages + updatedMsg
            state.copy(messages = processMessagesForReverseLayout(newList))
        }
    }

    private fun handleTypingUpdate(userId: Long, userName: String) {
        if (userId == currentUserId) return // Don't show own typing

        _uiState.update { state ->
            val typingUsers = state.typingUsers.toMutableMap()
            typingUsers[userId] = userName
            state.copy(typingUsers = typingUsers)
        }

        // Auto-remove typing indicator after 3 seconds
        viewModelScope.launch {
            delay(3000)
            _uiState.update { state ->
                val updatedTypingUsers = state.typingUsers.toMutableMap()
                updatedTypingUsers.remove(userId)
                state.copy(typingUsers = updatedTypingUsers)
            }
        }
    }

    private fun loadOnlineUsers() {
        currentProjectId?.let { projectId ->
            viewModelScope.launch {
                chatRepository.getOnlineUsers(projectId).collect { result ->
                    if (result is NetworkResult.Success) {
                        _uiState.update { it.copy(onlineUsers = result.data) }
                    }
                }
            }
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