package com.example.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.data.model.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: Int): User?
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): LiveData<List<User>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET isOnline = :isOnline WHERE id = :userId")
    suspend fun updateUserOnlineStatus(userId: Int, isOnline: Boolean)
}
