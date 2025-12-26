package com.example.linkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.models.ChatMessageResponse
import com.example.linkit.data.models.ChatUiState
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ChatRepository
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
            val result = chatRepository.getChatHistory(currentProjectId!!).collect { networkResult ->
                when (networkResult) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }

                    is NetworkResult.Success -> {
                        val messagesWithOwnStatus = processMessagesWithOwnStatus(networkResult.data.messages)
                        val sortedMessages = messagesWithOwnStatus.sortedBy { it.createdAt }
                        _uiState.update {
                            it.copy(
                                messages = sortedMessages,
                                isLoading = false,
                                hasMoreMessages = networkResult.data.hasMore
                            )
                        }
                    }

                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = networkResult.message
                            )
                        }
                        _uiEvent.emit(UiEvent.ShowToast("Failed to load messages: ${networkResult.message}"))
                    }
                }
            }
        }
    }

    private fun processMessagesWithOwnStatus(messages: List<ChatMessageResponse>): List<ChatMessageResponse> {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

        var previousDate: String? = null

        return messages.map { message ->
            val messageDate = try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                isoFormat.parse(message.createdAt) ?: Date()
            } catch (e: Exception) {
                Date()
            }

            val dateStr = dateFormatter.format(messageDate)
            val showDateHeader = dateStr != previousDate
            previousDate = dateStr

            val dateHeader = when (dateStr) {
                dateFormatter.format(today) -> "Today"
                dateFormatter.format(yesterday) -> "Yesterday"
                else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(messageDate)
            }

            message.copy(
                isOwnMessage = message.sender?.userId == currentUserId,
                showDateHeader = showDateHeader,
                dateHeader = if (showDateHeader) dateHeader else null,
                formattedTime = timeFormatter.format(messageDate)
            )
        }
    }

    fun loadMoreMessages() {
        val lastMessageTime = _uiState.value.messages.lastOrNull()?.createdAt
        if (lastMessageTime == null || !_uiState.value.hasMoreMessages || _uiState.value.isLoadingMore) {
            return
        }

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            chatRepository.getChatHistory(currentProjectId!!, lastMessageTime).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val existingMessages = _uiState.value.messages
                        val newMessagesWithOwnStatus = processMessagesWithOwnStatus(result.data.messages)


                        val allMessages = (existingMessages + newMessagesWithOwnStatus)
                            .distinctBy { it.id }
                            .sortedBy { it.createdAt }

                        _uiState.update {
                            it.copy(
                                messages = allMessages,
                                isLoadingMore = false,
                                hasMoreMessages = result.data.hasMore
                            )
                        }
                    }

                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoadingMore = false) }
                        _uiEvent.emit(UiEvent.ShowToast("Failed to load more messages"))
                    }

                    else -> {
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }

        // Send typing indicator
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            delay(300)
            currentProjectId?.let { projectId ->
                chatRepository.updateTypingStatus(projectId, true).collect { result ->
                    if (result is NetworkResult.Error) {
                        // Silent fail
                    }
                }

                delay(2000)
                chatRepository.updateTypingStatus(projectId, false).collect()
            }
        }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        if (messageText.isEmpty() || currentProjectId == null) {
            return
        }

        _uiState.update { it.copy(isSending = true, inputText = "") }
        viewModelScope.launch {
            chatRepository.sendMessage(currentProjectId!!, messageText).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val messageWithOwnStatus = result.data.copy(isOwnMessage = true)
                        _uiState.update { state ->
                            val updatedMessages = (state.messages + messageWithOwnStatus)
                                .sortedBy { it.createdAt }
                            state.copy(
                                messages = updatedMessages,
                                isSending = false
                            )
                        }
                    }

                    is NetworkResult.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                isSending = false,
                                inputText = messageText
                            )
                        }
                        _uiEvent.emit(UiEvent.ShowToast("Failed to send message: ${result.message}"))
                    }

                    else -> {
                        _uiState.update { it.copy(isSending = false) }
                    }
                }
            }
        }
    }

    private fun connectWebSocket() {
        currentProjectId?.let { projectId ->
            chatRepository.connectToChat(projectId)
        }
    }

    private fun observeWebSocketEvents() {
        webSocketJob = viewModelScope.launch {
            chatRepository.incomingMessages.collect { message ->
                handleIncomingMessage(message)
            }
        }

        viewModelScope.launch {
            chatRepository.typingUpdates.collect { (userId, userName) ->
                handleTypingUpdate(userId, userName)
            }
        }

        viewModelScope.launch {
            chatRepository.onlineUsersUpdates.collect { onlineUsers ->
                _uiState.update { it.copy(onlineUsers = onlineUsers) }
            }
        }

        viewModelScope.launch {
            chatRepository.connectionStatus.collect { isConnected ->
                _uiState.update { it.copy(isConnected = isConnected) }
            }
        }
    }

    private fun handleIncomingMessage(message: ChatMessageResponse) {
        val messageWithOwnStatus = message.copy(
            isOwnMessage = message.sender?.userId == currentUserId
        )

        _uiState.update { state ->
            val exists = state.messages.any { it.id == message.id }
            if (!exists) {
                val processedMessage = processSingleMessage(messageWithOwnStatus)
                val updatedMessages = (state.messages + processedMessage)
                    .sortedBy { it.createdAt }
                state.copy(messages = updatedMessages)
            } else {
                state
            }
        }
    }

    private fun processSingleMessage(message: ChatMessageResponse): ChatMessageResponse {
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val messageDate = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(message.createdAt) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        return message.copy(
            formattedTime = timeFormatter.format(messageDate)
        )
    }

    private fun handleTypingUpdate(userId: Long, userName: String) {
        _uiState.update { state ->
            val typingUsers = state.typingUsers.toMutableMap()
            typingUsers[userId] = userName
            state.copy(typingUsers = typingUsers)
        }

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
        currentProjectId?.let { projectId ->
            chatRepository.disconnectFromChat(projectId)
        }
        currentProjectId = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}