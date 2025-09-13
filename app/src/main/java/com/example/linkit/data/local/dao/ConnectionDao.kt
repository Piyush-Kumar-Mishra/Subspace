package com.example.linkit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.linkit.data.local.entities.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnections(connections: List<ConnectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionEntity)

    @Query("SELECT * FROM connections WHERE userId = :userId ORDER BY name ASC")
    suspend fun getConnectionsForUser(userId: Long): List<ConnectionEntity>

    @Query("SELECT * FROM connections WHERE userId = :userId ORDER BY name ASC")
    fun getConnectionsForUserFlow(userId: Long): Flow<List<ConnectionEntity>>

    @Query("DELETE FROM connections WHERE userId = :userId")
    suspend fun clearConnectionsForUser(userId: Long)

    @Query("DELETE FROM connections")
    suspend fun clearAllConnections()
}
