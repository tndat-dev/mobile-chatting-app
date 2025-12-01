package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"]) 
data class GroupMember(
    val groupId: Int,
    val userId: Int,
    val username: String,
    val nickname: String? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val isAdmin: Boolean = false
)
