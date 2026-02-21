package com.example.linkit.data.models

data class ProjectSummaryResponse(
    val totalTasks: Long,
    val completed: Long,
    val inProgress: Long,
    val todo: Long,
    val overdue: Long
)


data class AssigneeWorkloadResponse(
    val assigneeId: Long,
    val assigneeName: String?,
    val taskCount: Long
)


data class TimeSeriesPointResponse(
    val label: String,
    val value: Long
)


data class ProductivityResponse(
    val timeseries: List<TimeSeriesPointResponse>
)


data class AssigneeStatsResponse(
    val userId: Long,
    val assigneeId: Long,
    val assigneeName: String?,
    val profileImageUrl: String?,
    val assigned: Int,
    val completed: Int,
    val pending: Int,
    val overdue: Int
)

data class AssigneeStatsWrapper(
    val assignees: List<AssigneeStatsResponse>
)