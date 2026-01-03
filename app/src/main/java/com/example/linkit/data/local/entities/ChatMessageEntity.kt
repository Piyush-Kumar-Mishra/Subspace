package com.example.linkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.linkit.data.models.ChatMessageResponse
import com.example.linkit.data.models.ProjectAssigneeResponse
import com.example.linkit.util.TimeUtils

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val projectId: Long,
    val senderId: Long?,
    val messageType: String,
    val content: String,
    val systemEventType: String?,
    val systemEventData: String?,
    val createdAt: String,
    val isOwnMessage: Boolean = false,
    val showDateHeader: Boolean = false,
    val dateHeader: String? = null,
    val isSynced: Boolean = true
)

fun ChatMessageEntity.toChatMessageResponse(sender: ProjectAssigneeResponse? = null): ChatMessageResponse {
    return ChatMessageResponse(
        id = id,
        projectId = projectId,
        sender = sender,
        messageType = messageType,
        content = content,
        systemEventType = systemEventType,
        systemEventData = systemEventData,
        createdAt = createdAt,
        formattedTime = TimeUtils.formatTime(createdAt),
        isOwnMessage = isOwnMessage,
        showDateHeader = showDateHeader,
        dateHeader = dateHeader
    )
}

fun ChatMessageResponse.toChatMessageEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        projectId = projectId,
        senderId = sender?.userId,
        messageType = messageType,
        content = content,
        systemEventType = systemEventType,
        systemEventData = systemEventData,
        createdAt = createdAt,
        isOwnMessage = isOwnMessage,
        showDateHeader = showDateHeader,
        dateHeader = dateHeader,
        isSynced = true
    )
}