package com.example.linkit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey
    val connectionId: String, // userId of connectedUserId
    val userId: Long, // Owner of this connection list
    val connectedUserId: Long,
    val name: String,
    val company: String?,
    val profileImageUrl: String?,
    val profileImageLocalPath: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
