package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: Int,
    val username: String,
    val email: String,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0
)
