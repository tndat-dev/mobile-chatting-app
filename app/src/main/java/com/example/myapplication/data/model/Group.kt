package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey
    val id: Int,
    val name: String,
    val creatorId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val memberCount: Int = 0
)
