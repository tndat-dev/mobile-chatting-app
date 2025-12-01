package com.example.myapplication.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.ui.adapter.UsersAdapter
import com.example.myapplication.ui.adapter.UserItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.example.myapplication.data.repository.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddFriendActivity : AppCompatActivity() {
    private lateinit var networkManager: NetworkManager
    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private lateinit var adapter: UsersAdapter
    private val usersList = mutableListOf<UserItem>()
    
    private var targetGroupId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)
        
        networkManager = NetworkManager.getInstance()
        sessionManager = SessionManager.getInstance(this)

        // Đảm bảo đã có userId và kết nối trước khi thao tác
        val savedUserId = sessionManager.getUserId()
        if (networkManager.getUserId() == 0 && savedUserId > 0) {
            networkManager.setUserId(savedUserId)
        }
        if (!networkManager.isConnected()) {
            val host = sessionManager.getServerHost()
            val port = sessionManager.getServerPort()
            // Kết nối nền, không chặn UI; nếu lỗi sẽ có toast khi gọi API
            Thread {
                networkManager.connect(host, port)
            }.start()
        }
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // If this activity was opened to add members to a group, read the groupId
        targetGroupId = intent.getIntExtra("groupId", 0)
        supportActionBar?.title = if (targetGroupId > 0) "Add Members" else "Search & Add Friend"
        
        searchInput = findViewById(R.id.etSearch)
        recyclerView = findViewById(R.id.rvUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = UsersAdapter(usersList) { user -> onUserClick(user) }
        recyclerView.adapter = adapter
        
        setupNetworkCallback()
        setupSearchInput()
        loadAllUsers()
    }
    
    private fun setupSearchInput() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isEmpty()) {
                    loadAllUsers()
                } else {
                    searchUsers(query)
                }
            }
        })
    }
    
    private fun setupNetworkCallback() {
        networkManager.setCallback(object : NetworkManager.MessageCallback {
            override fun onMessageReceived(messageType: Int, payload: String) {
                runOnUiThread {
                    when (messageType) {
                        NetworkManager.MessageType.SUCCESS -> handleSuccessResponse(payload)
                        NetworkManager.MessageType.ERROR -> Toast.makeText(this@AddFriendActivity, "Error: $payload", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    
    private fun loadAllUsers() {
        if (networkManager.isConnected()) {
            networkManager.getAllUsers()
        } else {
            Toast.makeText(this, "Not connected to server", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun searchUsers(query: String) {
        if (networkManager.isConnected()) {
            networkManager.searchUsers(query)
        } else {
            Toast.makeText(this, "Not connected to server", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleSuccessResponse(payload: String) {
        val pairs = payload.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        
        val count = pairs["count"]?.toIntOrNull() ?: 0
        usersList.clear()
        
        for (i in 0 until count) {
            val userId = pairs["id$i"]?.toIntOrNull() ?: continue
            val username = pairs["name$i"] ?: continue
            val isFriend = pairs["isFriend$i"] == "1"
            usersList.add(UserItem(userId, username, isFriend))
            // Persist into users table so other screens can resolve usernames
            try {
                val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(this)
                val user = com.example.myapplication.data.model.User(
                    id = userId,
                    username = username,
                    email = "",
                    isOnline = false,
                    lastSeen = System.currentTimeMillis()
                )
                // Insert on background thread
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        db.userDao().insertUser(user)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        adapter.notifyDataSetChanged()
        
        if (usersList.isEmpty()) {
            Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show()
        }

        // If we are adding to a group, remove users who are already group members
        if (targetGroupId > 0) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(this@AddFriendActivity)
                    val toRemove = mutableListOf<UserItem>()
                    for (u in usersList) {
                        val member = db.groupMemberDao().getMember(targetGroupId, u.userId)
                        if (member != null) {
                            toRemove.add(u)
                        }
                    }
                    if (toRemove.isNotEmpty()) {
                        runOnUiThread {
                            usersList.removeAll(toRemove)
                            adapter.notifyDataSetChanged()
                        }
                    }
                } catch (e: Exception) {
                    // ignore filtering errors
                }
            }
        }
    }
    
    private fun onUserClick(user: UserItem) {
            if (targetGroupId > 0) {
                // Invite user to the group — do NOT insert locally; wait for acceptance
                val invited = networkManager.inviteToGroup(targetGroupId, user.userId)
                if (invited) {
                    Toast.makeText(this, "Invited ${user.username} to group", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to invite ${user.username}", Toast.LENGTH_SHORT).show()
                }
        } else {
            if (user.isFriend) {
                Toast.makeText(this, "Already friends with ${user.username}", Toast.LENGTH_SHORT).show()
            } else {
                if (networkManager.sendFriendRequest(user.username)) {
                    Toast.makeText(this, "Friend request sent to ${user.username}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send friend request", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
