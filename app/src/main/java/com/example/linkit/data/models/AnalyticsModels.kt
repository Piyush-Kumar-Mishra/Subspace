package com.example.linkit.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ProjectSummaryResponse(
    val totalTasks: Long,
    val completed: Long,
    val inProgress: Long,
    val todo: Long,
    val overdue: Long
)

@Serializable
data class AssigneeWorkloadResponse(
    val assigneeId: Long,
    val assigneeName: String?,
    val taskCount: Long
)

@Serializable
data class TimeSeriesPointResponse(
    val label: String,
    val value: Long
)

@Serializable
data class ProductivityResponse(
    val timeseries: List<TimeSeriesPointResponse>
)

@Serializable
data class AssigneeStatsResponse(
    val assigneeId: Long,
    val assigneeName: String?,
    val assigned: Int,
    val completed: Int,
    val pending: Int,
    val overdue: Int
)

@Serializable
data class AssigneeStatsWrapper(
    val assignees: List<AssigneeStatsResponse>
)