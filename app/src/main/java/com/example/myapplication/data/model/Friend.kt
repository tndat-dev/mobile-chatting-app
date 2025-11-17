package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey
    val userId: Int,
    val username: String,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)
