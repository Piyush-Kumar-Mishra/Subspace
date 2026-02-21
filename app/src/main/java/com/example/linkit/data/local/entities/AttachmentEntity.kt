package com.example.linkit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_attachments")
data class AttachmentEntity(
    @PrimaryKey val id: Long,
    val taskId: Long,
    val fileName: String,
    val downloadUrl: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedByName: String,
    val uploadedById: Long,
    val uploadedByImageUrl: String?
)