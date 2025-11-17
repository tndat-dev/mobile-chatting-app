package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: Int,
    val recipientId: Int,
    val groupId: Int? = null,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isSent: Boolean = false,
    val isDelivered: Boolean = false
)
