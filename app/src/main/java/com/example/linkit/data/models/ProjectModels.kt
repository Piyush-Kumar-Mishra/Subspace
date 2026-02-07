package com.example.linkit.data.models

data class CreateProjectRequest(
    val name: String,
    val description: String?,
    val endDate: String,
    val priority: String,
    val assigneeIds: List<Long>,
    val tags: List<String>
)

data class UpdateProjectRequest(
    val name: String,
    val description: String?,
    val endDate: String,
    val priority: String,
    val assigneeIds: List<Long>,
    val tags: List<String>
)

data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val endDate: String,
    val priority: String,
    val assignees: List<ProjectAssigneeResponse>,
    val tags: List<String>,
    val createdBy: Long,
    val createdAt: String,
    val taskCount: Int = 0
)


data class ProjectAssigneeResponse(
    val userId: Long,
    val name: String,
    val profileImageUrl: String?
)


data class ProjectsResponse(
    val projects: List<ProjectResponse>
)

data class CreateTaskRequest(
    val name: String,
    val description: String?,
    val projectId: Long,
    val assigneeId: Long,
    val startDate: String,
    val endDate: String,
    val status: String
)

data class UpdateTaskRequest(
    val name: String,
    val description: String?,
    val assigneeId: Long,
    val startDate: String,
    val endDate: String,
    val status: String
)

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
    val attachmentCount: Int = 0
)

data class TasksResponse(
    val tasks: List<TaskResponse>
)

data class TaskMessageRequest(
    val content: String,
    val messageType: String = "TEXT"
)

data class TaskMessageResponse(
    val id: Long,
    val taskId: Long,
    val sender: ProjectAssigneeResponse,
    val content: String,
    val messageType: String,
    val createdAt: String
)

data class TaskMessagesResponse(
    val messages: List<TaskMessageResponse>
)

data class TaskAttachmentResponse(
    val id: Long,
    val taskId: Long,
    val fileName: String,
    val filePath: String,
    val downloadUrl: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedBy: ProjectAssigneeResponse,
    val uploadedAt: String
)

data class TaskAttachmentsResponse(
    val attachments: List<TaskAttachmentResponse>
)

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

data class UserSearchResult(
    val userId: Long,
    val name: String,
    val email: String,
    val company: String?,
    val profileImageUrl: String?,
    val isConnected: Boolean = false
)

