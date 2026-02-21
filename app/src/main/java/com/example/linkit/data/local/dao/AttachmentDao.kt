package com.example.linkit.data.local.dao

import androidx.room.*
import com.example.linkit.data.local.entities.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId")
    fun getAttachmentsForTask(taskId: Long): Flow<List<AttachmentEntity>>

    @Query("DELETE FROM task_attachments WHERE taskId = :taskId")
    suspend fun deleteAttachmentsByTask(taskId: Long)
}