package com.example.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.data.model.FriendRequest
import com.example.myapplication.data.model.RequestStatus

@Dao
interface FriendRequestDao {
    @Query("SELECT * FROM friend_requests WHERE toUserId = :userId AND status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRequests(userId: Int): LiveData<List<FriendRequest>>
    
    @Query("SELECT * FROM friend_requests WHERE fromUserId = :userId ORDER BY timestamp DESC")
    fun getSentRequests(userId: Int): LiveData<List<FriendRequest>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: FriendRequest)
    
    @Update
    suspend fun updateRequest(request: FriendRequest)
    
    @Query("UPDATE friend_requests SET status = :status WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: Long, status: RequestStatus)
    
    @Query("DELETE FROM friend_requests WHERE id = :requestId")
    suspend fun deleteRequest(requestId: Long)

    @Query("DELETE FROM friend_requests WHERE toUserId = :userId AND status = 'PENDING'")
    suspend fun clearPendingFor(userId: Int)

    @Query("DELETE FROM friend_requests WHERE fromUserId = :fromUserId AND toUserId = :toUserId")
    suspend fun deleteByUsers(fromUserId: Int, toUserId: Int)
}
