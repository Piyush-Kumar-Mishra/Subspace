package com.example.linkit.data.models

data class ChatMessageRequest(
    val content: String
)


data class ChatMessageResponse(
    val id: Long,
    val projectId: Long,
    val sender: ProjectAssigneeResponse?,
    val messageType: String,
    val content: String,
    val systemEventType: String?,
    val systemEventData: String?,
    val createdAt: String,
    val formattedTime: String,
    val isOwnMessage: Boolean = false,
    val showDateHeader: Boolean = false,
    val dateHeader: String? = null
)


data class ChatHistoryResponse(
    val messages: List<ChatMessageResponse>,
    val hasMore: Boolean,
    val nextPageToken: String?
)


data class WebSocketChatMessage(
    val type: String,
    val message: ChatMessageResponse? = null,
    val timestamp: String = ""
)


data class ChatUiState(
    val messages: List<ChatMessageResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val inputText: String = "",
    val isSending: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val showDateHeaders: Boolean = true
)