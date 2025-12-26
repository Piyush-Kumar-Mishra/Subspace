package com.example.linkit.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageRequest(
    val content: String
)

@Serializable
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

@Serializable
data class ChatHistoryResponse(
    val messages: List<ChatMessageResponse>,
    val hasMore: Boolean,
    val nextPageToken: String?
)

@Serializable
data class WebSocketChatMessage(
    val type: String,
    val message: ChatMessageResponse? = null,
    val typingUserId: Long? = null,
    val typingUserName: String? = null,
    val onlineUsers: List<Long>? = null,
    val timestamp: String = ""
)

@Serializable
data class TypingIndicatorRequest(
    val isTyping: Boolean
)

data class ChatUiState(
    val messages: List<ChatMessageResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val inputText: String = "",
    val isSending: Boolean = false,
    val onlineUsers: List<Long> = emptyList(),
    val typingUsers: Map<Long, String> = emptyMap(),
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val showDateHeaders: Boolean = true
)
//
//enum class MessageType {
//    USER_MESSAGE, SYSTEM_MESSAGE
//}
//
//enum class SystemEventType {
//    TASK_CREATED,
//    TASK_UPDATED,
//    PROJECT_UPDATED,
//    POLL_CREATED,
//    USER_JOINED,
//    USER_LEFT
//}