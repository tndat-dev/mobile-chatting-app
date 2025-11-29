package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.database.ChatDatabase
import com.example.myapplication.data.model.Group
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.repository.ChatRepository
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.utils.ActivityLogger
import kotlinx.coroutines.launch

class GroupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = ChatDatabase.getDatabase(application)
    private val networkManager = NetworkManager.getInstance()
    private val logger = ActivityLogger.getInstance(application)
    
    private val repository = ChatRepository(
        database.userDao(),
        database.messageDao(),
        database.friendDao(),
        database.friendRequestDao(),
        database.groupDao(),
        database.groupMemberDao(),
        database.conversationDao(),
        networkManager
    )
    
    private val _operationStatus = MutableLiveData<Pair<Boolean, String>>()
    val operationStatus: LiveData<Pair<Boolean, String>> = _operationStatus
    
    fun getAllGroups(): LiveData<List<Group>> {
        return repository.getAllGroups()
    }
    
    fun getGroupMembers(groupId: Int): LiveData<List<GroupMember>> {
        return repository.getGroupMembers(groupId)
    }
    
    fun createGroup(groupName: String, memberIds: List<Int>) {
        viewModelScope.launch {
            try {
                logger.log(ActivityLogger.LogLevel.INFO, "GROUP", "Creating group: $groupName")
                val success = repository.createGroup(groupName, memberIds)
                
                if (success) {
                    logger.log(ActivityLogger.LogLevel.INFO, "GROUP", "Group created: $groupName")
                    _operationStatus.postValue(Pair(true, "Group created"))
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to create group"))
                }
            } catch (e: Exception) {
                logger.logError("GROUP", "Error creating group", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
    
    fun inviteToGroup(groupId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                logger.logGroupAction("INVITE", groupId, userId, false)
                val success = repository.inviteToGroup(groupId, userId)
                
                if (success) {
                    logger.logGroupAction("INVITE", groupId, userId, true)
                    _operationStatus.postValue(Pair(true, "Invitation sent"))
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to send invitation"))
                }
            } catch (e: Exception) {
                logger.logError("GROUP", "Error inviting to group", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
    
    fun removeFromGroup(groupId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                logger.logGroupAction("REMOVE", groupId, userId, false)
                val success = repository.removeFromGroup(groupId, userId)
                
                if (success) {
                    logger.logGroupAction("REMOVE", groupId, userId, true)
                    _operationStatus.postValue(Pair(true, "Member removed"))
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to remove member"))
                }
            } catch (e: Exception) {
                logger.logError("GROUP", "Error removing from group", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
    
    fun leaveGroup(groupId: Int) {
        viewModelScope.launch {
            try {
                logger.logGroupAction("LEAVE", groupId, networkManager.getUserId(), false)
                val success = repository.leaveGroup(groupId)
                
                if (success) {
                    logger.logGroupAction("LEAVE", groupId, networkManager.getUserId(), true)
                    _operationStatus.postValue(Pair(true, "Left group"))
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to leave group"))
                }
            } catch (e: Exception) {
                logger.logError("GROUP", "Error leaving group", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
}
