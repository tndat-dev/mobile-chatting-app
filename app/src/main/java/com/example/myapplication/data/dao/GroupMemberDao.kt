package com.example.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.data.model.GroupMember

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY username ASC")
    fun getGroupMembers(groupId: Int): LiveData<List<GroupMember>>
    
    @Query("SELECT * FROM group_members WHERE userId = :userId")
    fun getUserGroups(userId: Int): LiveData<List<GroupMember>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember)
    
    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeMember(groupId: Int, userId: Int)
    
    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun removeAllMembers(groupId: Int)
    
    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId")
    suspend fun getMemberCount(groupId: Int): Int

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY username ASC")
    suspend fun getMembersList(groupId: Int): List<GroupMember>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getMember(groupId: Int, userId: Int): GroupMember?
}
