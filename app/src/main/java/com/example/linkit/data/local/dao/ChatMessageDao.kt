package com.example.linkit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.linkit.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getMessages(projectId: Long, limit: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId AND createdAt < :before ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getMessagesBefore(projectId: Long, before: String, limit: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): ChatMessageEntity?

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearProjectMessages(projectId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE projectId = :projectId AND isSynced = 0")
    suspend fun getUnsyncedMessageCount(projectId: Long): Int

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId AND isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedMessages(projectId: Long): List<ChatMessageEntity>

    @Query("UPDATE chat_messages SET isSynced = 1 WHERE id = :messageId")
    suspend fun markMessageAsSynced(messageId: Long)

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeMessages(projectId: Long): Flow<List<ChatMessageEntity>>
}