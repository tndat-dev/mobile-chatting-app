package com.example.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.data.model.Group

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): LiveData<List<Group>>

    @Query("SELECT g.* FROM groups g JOIN group_members gm ON g.id = gm.groupId WHERE gm.userId = :userId ORDER BY g.name ASC")
    fun getAllGroupsForUser(userId: Int): LiveData<List<Group>>
    
    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroup(groupId: Int): Group?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group)
    
    @Update
    suspend fun updateGroup(group: Group)
    
    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Int)
}
