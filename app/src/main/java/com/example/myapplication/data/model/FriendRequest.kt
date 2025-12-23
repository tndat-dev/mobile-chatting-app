package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "friend_requests",
    indices = [Index(value = ["fromUserId", "toUserId"], unique = true)]
)
data class FriendRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromUserId: Int,
    val toUserId: Int,
    val fromUsername: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING
)
