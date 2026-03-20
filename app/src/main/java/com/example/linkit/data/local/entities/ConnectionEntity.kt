package com.example.linkit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey
    val connectionId: String,
    val userId: Long,
    val connectedUserId: Long,
    val name: String,
    val company: String?,
    val profileImageUrl: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
