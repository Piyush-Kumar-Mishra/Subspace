package com.example.linkit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.linkit.data.local.dao.AttachmentDao
import com.example.linkit.data.local.dao.ChatMessageDao
import com.example.linkit.data.local.dao.ConnectionDao
import com.example.linkit.data.local.dao.ProjectDao
import com.example.linkit.data.local.dao.TaskDao
import com.example.linkit.data.local.dao.UserDao
import com.example.linkit.data.local.entities.AttachmentEntity
import com.example.linkit.data.local.entities.ConnectionEntity
import com.example.linkit.data.local.entities.ProjectEntity
import com.example.linkit.data.local.entities.TaskEntity
import com.example.linkit.data.local.entities.UserEntity
import com.example.linkit.data.local.entity.ChatMessageEntity

@Database(
    entities = [UserEntity::class, ConnectionEntity::class, ChatMessageEntity::class, ProjectEntity::class,
        TaskEntity::class, AttachmentEntity::class],
    version = 9,
    exportSchema = false
)
abstract class LinkItDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun connectionDao(): ConnectionDao

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun attachmentDao(): AttachmentDao

}
