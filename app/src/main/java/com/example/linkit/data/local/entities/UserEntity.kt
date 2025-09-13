package com.example.linkit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: Long,
    val email: String,
    val username: String,
    val name: String,
    val jobTitle: String?,
    val company: String?,
    val aboutMe: String?,
    val profileImageUrl: String?,
    val profileImageLocalPath: String?, // Local cached image path
    val lastUpdated: Long = System.currentTimeMillis(),
    val isProfileCompleted: Boolean = true
)
