package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_members")
data class GroupMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Int,
    val userId: Int,
    val username: String,
    val joinedAt: Long = System.currentTimeMillis(),
    val isAdmin: Boolean = false
)
