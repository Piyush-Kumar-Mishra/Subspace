package com.example.linkit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Long,
    val projectId: Long,
    val name: String,
    val description: String?,
    val status: String,
    val startDate: String,
    val endDate: String,
    val assigneeName: String,
    val assigneeId: Long,
    val assigneeImageUrl: String?
)