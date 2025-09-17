package com.example.linkit.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(
    val name: String,
    val description: String?,
    val startDate: String,
    val priority: String,
    val assigneeIds: List<Long>,
    val tags: List<String>
)

@Serializable
data class UpdateProjectRequest(
    val name: String,
    val description: String?,
    val startDate: String,
    val priority: String,
    val assigneeIds: List<Long>,
    val tags: List<String>
)

@Serializable
data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val startDate: String,
    val priority: String,
    val assignees: List<ProjectAssigneeResponse>,
    val tags: List<String>,
    val createdBy: Long,
    val createdAt: String,
    val taskCount: Int = 0
)

@Serializable
data class ProjectAssigneeResponse(
    val userId: Long,
    val name: String,
    val profileImageUrl: String?
)

@Serializable
data class ProjectsResponse(
    val projects: List<ProjectResponse>
)

@Serializable
data class CreateTaskRequest(
    val name: String,
    val description: String?,
    val projectId: Long,
    val assigneeId: Long,
    val startDate: String,
    val endDate: String,
    val status: String
)

@Serializable
data class UpdateTaskRequest(
    val name: String,
    val assigneeId: Long,
    val startDate: String,
    val endDate: String,
    val status: String
)

@Serializable
data class TaskResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val projectId: Long,
    val assignee: ProjectAssigneeResponse,
    val creator: ProjectAssigneeResponse,
    val startDate: String,
    val endDate: String,
    val status: String,
    val createdBy: Long,
    val createdAt: String,
    val messageCount: Int = 0,
    val attachmentCount: Int = 0
)

@Serializable
data class TasksResponse(
    val tasks: List<TaskResponse>
)

@Serializable
data class TaskMessageRequest(
    val content: String,
    val messageType: String = "TEXT"
)

@Serializable
data class TaskMessageResponse(
    val id: Long,
    val taskId: Long,
    val sender: ProjectAssigneeResponse,
    val content: String,
    val messageType: String,
    val createdAt: String
)

@Serializable
data class TaskMessagesResponse(
    val messages: List<TaskMessageResponse>
)

@Serializable
data class TaskAttachmentResponse(
    val id: Long,
    val taskId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedBy: ProjectAssigneeResponse,
    val uploadedAt: String
)

@Serializable
data class TaskAttachmentsResponse(
    val attachments: List<TaskAttachmentResponse>
)

@Serializable
data class NotificationRequest(
    val token: String,
    val platform: String
)

enum class ProjectPriority(val displayName: String, val colorName: String) {
    LOW("Low", "Green"),
    MEDIUM("Medium", "Orange"),
    HIGH("High", "Red")
}

enum class TaskStatus(val displayName: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

@Serializable
data class UserSearchResult(
    val userId: Long,
    val name: String,
    val email: String,
    val company: String?,
    val profileImageUrl: String?,
    val isConnected: Boolean = false
)

@Serializable
data class WebSocketMessage(
    val type: String,
    val taskId: Long,
    val senderId: Long,
    val senderName: String,
    val content: String? = null,
    val timestamp: String
)
