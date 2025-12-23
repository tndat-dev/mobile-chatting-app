package com.example.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.data.model.Friend

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY username ASC")
    fun getAllFriends(): LiveData<List<Friend>>
    
    @Query("SELECT * FROM friends WHERE isOnline = 1 ORDER BY username ASC")
    fun getOnlineFriends(): LiveData<List<Friend>>
    
    @Query("SELECT * FROM friends WHERE userId = :userId")
    suspend fun getFriend(userId: Int): Friend?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<Friend>)
    
    @Delete
    suspend fun deleteFriend(friend: Friend)
    
    @Query("UPDATE friends SET isOnline = :isOnline, lastSeen = :lastSeen WHERE userId = :userId")
    suspend fun updateFriendStatus(userId: Int, isOnline: Boolean, lastSeen: Long)
    
    @Query("DELETE FROM friends WHERE userId = :userId")
    suspend fun deleteFriendById(userId: Int)

    @Query("DELETE FROM friends")
    suspend fun clearAll()
}
