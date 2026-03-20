package com.example.linkit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String?,
    val endDate: String,
    val priority: String,
    val taskCount: Int,
    val createdAt: String,
    val assigneesJson: String,
    val tagsJson: String
)

