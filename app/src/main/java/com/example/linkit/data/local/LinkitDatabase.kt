package com.example.linkit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.linkit.data.local.dao.ConnectionDao
import com.example.linkit.data.local.dao.UserDao
import com.example.linkit.data.local.entities.ConnectionEntity
import com.example.linkit.data.local.entities.UserEntity

@Database(
    entities = [UserEntity::class, ConnectionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LinkItDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun connectionDao(): ConnectionDao


}
