package com.example.myapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.data.repository.SessionManager
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.ui.fragment.ChatsFragment
import com.example.myapplication.ui.fragment.FriendsFragment
import com.example.myapplication.ui.fragment.GroupsFragment
import com.example.myapplication.ui.fragment.ProfileFragment
import com.example.myapplication.utils.ActivityLogger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), NetworkManager.MessageCallback {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var networkManager: NetworkManager
    private lateinit var sessionManager: SessionManager
    private lateinit var logger: ActivityLogger
    
    // Cache pending messages to forward to fragments when they're created
    private val pendingMessages = mutableListOf<Pair<Int, String>>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        networkManager = NetworkManager.getInstance()
        sessionManager = SessionManager.getInstance(this)
        logger = ActivityLogger.getInstance(this)
        
        // Check if logged in
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        networkManager.setCallback(this)
        networkManager.setUserId(sessionManager.getUserId())
        
        setupUI()
        
        // Connect if not connected
        if (!networkManager.isConnected()) {
            connectToServer()
        }
        
        // Start with chats fragment
        if (savedInstanceState == null) {
            loadFragment(ChatsFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure this activity regains the network callback when returning from other screens
        networkManager.setCallback(this)
        // Proactively refresh friends/pending state after login or returning to app
        if (networkManager.isConnected()) {
            networkManager.getFriendsList()
            // Also refresh groups so Room is repopulated after rebuilds/resume
            try {
                networkManager.getUserGroups()
                // Ensure we have user metadata to resolve usernames for group members
                try {
                    networkManager.getAllUsers()
                } catch (e: Exception) {
                    // ignore
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    
    private fun setupUI() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    loadFragment(ChatsFragment())
                    true
                }
                R.id.nav_friends -> {
                    loadFragment(FriendsFragment())
                    true
                }
                R.id.nav_groups -> {
                    loadFragment(GroupsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
        
        // '+' action button removed per request
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun connectToServer() {
        val host = sessionManager.getServerHost()
        val port = sessionManager.getServerPort()
        
        lifecycleScope.launch {
            logger.logConnection("RECONNECT", host, port, false)
            val connected = networkManager.connect(host, port)
            
            if (connected) {
                logger.logConnection("RECONNECT", host, port, true)
                // Request friends list
                    networkManager.getFriendsList()
                    // Also request user's groups so local Room can be populated after rebuilds
                    networkManager.getUserGroups()
                    // Request list of all users so we can resolve usernames for group members
                    try {
                        networkManager.getAllUsers()
                    } catch (e: Exception) {
                        // ignore
                    }
            } else {
                logger.logConnection("RECONNECT", host, port, false)
                showConnectionError()
            }
        }
    }
    
    private fun showConnectionError() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Connection Error")
            .setMessage("Failed to connect to server. Do you want to retry?")
            .setPositiveButton("Retry") { _, _ ->
                connectToServer()
            }
            .setNegativeButton("Logout") { _, _ ->
                logout()
            }
            .show()
    }
    
    override fun onMessageReceived(messageType: Int, payload: String) {
        lifecycleScope.launch {
            logger.log(ActivityLogger.LogLevel.DEBUG, "MAIN", "Message received: type=$messageType, payload=$payload")
        }
        
        Log.d("MainActivity", "onMessageReceived: type=$messageType, payload=$payload")
        
        // Always cache message first to ensure it's not lost
        pendingMessages.add(Pair(messageType, payload))
        
        // Try to forward message to active fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is NetworkManager.MessageCallback) {
            Log.d("MainActivity", "Forwarding message to fragment: ${currentFragment.javaClass.simpleName}")
            currentFragment.onMessageReceived(messageType, payload)
        } else {
            Log.d("MainActivity", "No fragment available, message cached. Current fragment: ${currentFragment?.javaClass?.simpleName}")
        }
        
        // Handle different message types at activity level
        when (messageType) {
            NetworkManager.MessageType.SUCCESS -> {
                // Try to detect user list payloads (GET_ALL_USERS) and persist users locally
                try {
                    val pairsPreview = payload.split("&").mapNotNull {
                        val parts = it.split("=", limit = 2)
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }.toMap()
                    if (pairsPreview.containsKey("id0") && pairsPreview.containsKey("name0") && !pairsPreview.containsKey("groupId")) {
                        // Likely a user-list payload (GET_ALL_USERS). Insert users into local users table.
                        lifecycleScope.launch {
                            try {
                                val data = payload.split("&").mapNotNull {
                                    val parts = it.split("=", limit = 2)
                                    if (parts.size == 2) parts[0] to parts[1] else null
                                }.toMap()
                                val count = data["count"]?.toIntOrNull() ?: 0
                                val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(this@MainActivity)
                                for (i in 0 until count) {
                                    val uid = data["id$i"]?.toIntOrNull() ?: continue
                                    val uname = data["name$i"] ?: continue
                                    val user = com.example.myapplication.data.model.User(
                                        id = uid,
                                        username = uname,
                                        email = "",
                                        isOnline = false,
                                        lastSeen = System.currentTimeMillis()
                                    )
                                    try {
                                        db.userDao().insertUser(user)
                                    } catch (e: Exception) {
                                        // ignore individual insert failures
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }

                    // Detect friends list payload (GET_FRIENDS). It contains id0/name0 and pendingCount/req_id0 keys.
                    if (pairsPreview.containsKey("id0") && pairsPreview.containsKey("name0") && (pairsPreview.containsKey("pendingCount") || pairsPreview.containsKey("req_id0"))) {
                        // Parse and persist friends immediately so Chats list can show without opening Friends tab
                        lifecycleScope.launch {
                            parseAndSaveFriends(payload)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
                // Detect group-list payload (GET_USER_GROUPS): contains groupId0 & name0
                if (payload.contains("groupId0=") && payload.contains("name0=")) {
                    handleGroupListPayload(payload)
                } else if (payload.contains("groupId=") && payload.contains("name=")) {
                    // CREATE_GROUP response
                    handleGroupCreated(payload)
                } else if (payload.contains("pendingCount=")) {
                    val pairs = payload.split("&").mapNotNull {
                        val parts = it.split("=", limit = 2)
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }.toMap()
                    val pc = pairs["pendingCount"]?.toIntOrNull() ?: 0
                    if (pc > 0) {
                        runOnUiThread {
                            Toast.makeText(this, "You have $pc friend request(s)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            NetworkManager.MessageType.DIRECT_MESSAGE -> {
                // Message will be handled by ChatViewModel/Repository
            }
            NetworkManager.MessageType.GROUP_MESSAGE -> {
                // Group message will be handled by ChatViewModel/Repository
            }
            NetworkManager.MessageType.USER_ONLINE,
            NetworkManager.MessageType.USER_OFFLINE -> {
                // Update friend status
            }
            NetworkManager.MessageType.FRIEND_REQUEST -> {
                runOnUiThread {
                    Toast.makeText(this, "New friend request", Toast.LENGTH_SHORT).show()
                }
            }
            NetworkManager.MessageType.INVITE_TO_GROUP -> {
                // Incoming group invite: payload contains groupId, name, fromUserId
                val pairs = payload.split("&").mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()
                val gid = pairs["groupId"]?.toIntOrNull() ?: return
                val gname = pairs["name"] ?: "Group"
                val fromUid = pairs["fromUserId"]?.toIntOrNull() ?: -1

                runOnUiThread {
                        // Auto-join: server already added the user to the group, persist locally and fetch history
                        lifecycleScope.launch {
                            try {
                                val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(this@MainActivity)
                                val group = com.example.myapplication.data.model.Group(
                                    id = gid,
                                    name = gname,
                                    creatorId = fromUid,
                                    memberCount = 1
                                )
                                try {
                                    db.groupDao().insertGroup(group)
                                } catch (e: Exception) {
                                    // ignore if exists
                                }
                                val member = com.example.myapplication.data.model.GroupMember(
                                    groupId = gid,
                                    userId = sessionManager.getUserId(),
                                    username = sessionManager.getUsername() ?: "Me",
                                    nickname = null,
                                    isAdmin = false,
                                    joinedAt = System.currentTimeMillis()
                                )
                                try {
                                    db.groupMemberDao().insertMember(member)
                                } catch (e: Exception) {
                                    // ignore if exists
                                }

                                // Request group history to populate messages
                                try {
                                    if (networkManager.isConnected()) networkManager.getGroupHistory(gid, 100)
                                } catch (e: Exception) {
                                    // ignore
                                }

                                runOnUiThread { Toast.makeText(this@MainActivity, "You were added to group '$gname'", Toast.LENGTH_SHORT).show() }
                            } catch (e: Exception) {
                                runOnUiThread { Toast.makeText(this@MainActivity, "Error processing group add", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }
            }
        }
    }
    
    private fun handleGroupCreated(payload: String) {
        lifecycleScope.launch {
            try {
                val pairs = payload.split("&").mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()
                
                val groupId = pairs["groupId"]?.toIntOrNull() ?: return@launch
                val groupName = pairs["name"] ?: return@launch
                val memberCount = pairs["memberCount"]?.toIntOrNull() ?: 1
                val membersCsv = pairs["members"] ?: ""
                
                // Save to database
                val database = com.example.myapplication.data.database.ChatDatabase.getDatabase(this@MainActivity)
                val group = com.example.myapplication.data.model.Group(
                    id = groupId,
                    name = groupName,
                    creatorId = sessionManager.getUserId(),
                    memberCount = memberCount
                )
                database.groupDao().insertGroup(group)
                
                // Add current user as member
                val groupMember = com.example.myapplication.data.model.GroupMember(
                    groupId = groupId,
                    userId = sessionManager.getUserId(),
                    username = sessionManager.getUsername() ?: "Me",
                    nickname = null,
                    isAdmin = true,
                    joinedAt = System.currentTimeMillis()
                )
                database.groupMemberDao().insertMember(groupMember)

                // Insert other members from response
                if (membersCsv.isNotEmpty()) {
                    val otherIds = membersCsv.split(',')
                        .mapNotNull { it.toIntOrNull() }
                        .filter { it != sessionManager.getUserId() }
                    if (otherIds.isNotEmpty()) {
                        // Try resolve usernames from friendDao or default label
                        val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(this@MainActivity)
                        val friendDao = db.friendDao()
                        for (uid in otherIds) {
                            val friend = friendDao.getFriend(uid)
                            val uname = friend?.username ?: "User $uid"
                            val member = com.example.myapplication.data.model.GroupMember(
                                groupId = groupId,
                                userId = uid,
                                username = uname,
                                nickname = null,
                                isAdmin = false,
                            )
                            database.groupMemberDao().insertMember(member)
                        }
                    }
                }
                
                logger.log(ActivityLogger.LogLevel.INFO, "GROUP", "Group saved: $groupName (id=$groupId)")
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Group created", Toast.LENGTH_SHORT).show()
                }
                // After creating a group, proactively fetch all users so client can resolve usernames
                try {
                    if (networkManager.isConnected()) {
                        networkManager.getAllUsers()
                    }
                } catch (e: Exception) {
                    // ignore
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error handling group created", e)
                logger.logError("GROUP", "Error saving group", e)
            }
        }
    }

    private fun handleGroupListPayload(payload: String) {
        lifecycleScope.launch {
            try {
                val pairs = payload.split("&").mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()

                val count = pairs["count"]?.toIntOrNull() ?: 0
                val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(this@MainActivity)

                for (i in 0 until count) {
                    val gid = pairs["groupId$i"]?.toIntOrNull() ?: continue
                    val gname = pairs["name$i"] ?: continue
                    val membersCsv = pairs["members$i"] ?: ""
                    val memberIds = if (membersCsv.isNotEmpty()) membersCsv.split(',').mapNotNull { it.toIntOrNull() } else emptyList()
                    // Use server-provided memberCount when available; fall back to parsed member list size
                    val serverMemberCount = pairs["memberCount$i"]?.toIntOrNull()
                    // parse member nicknames if provided (base64 encoded, aligned with memberIds)
                    val memberNicksKey = "memberNicks${i}"
                    val memberNicksCsv = pairs[memberNicksKey] ?: ""
                    val memberNicks = if (memberNicksCsv.isNotEmpty()) memberNicksCsv.split(',') else emptyList()

                    val group = com.example.myapplication.data.model.Group(
                        id = gid,
                        name = gname,
                        creatorId = sessionManager.getUserId(),
                        memberCount = serverMemberCount ?: if (memberIds.size > 0) memberIds.size else 1
                    )
                    try {
                        db.groupDao().insertGroup(group)
                    } catch (e: Exception) {
                        // ignore
                    }

                    // Insert members (do not mutate the server-provided member list). Ensure local user record exists separately.
                    for ((idx, uid) in memberIds.withIndex()) {
                        val uname = try {
                            val friend = db.friendDao().getFriend(uid)
                            friend?.username ?: db.userDao().getUser(uid)?.username ?: "User $uid"
                        } catch (e: Exception) {
                            "User $uid"
                        }
                        val nick = try {
                            if (idx < memberNicks.size && memberNicks[idx].isNotEmpty()) {
                                android.util.Base64.decode(memberNicks[idx], android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                        val member = com.example.myapplication.data.model.GroupMember(
                            groupId = gid,
                            userId = uid,
                            username = uname,
                            nickname = nick,
                            isAdmin = (uid == sessionManager.getUserId()),
                            joinedAt = System.currentTimeMillis()
                        )
                        try {
                            db.groupMemberDao().insertMember(member)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }

                    // After persisting the group and its members, request group history
                    try {
                        if (networkManager.isConnected()) {
                            networkManager.getGroupHistory(gid, 100)
                        }
                    } catch (e: Exception) {
                        // ignore failures to request history
                    }

                    // Ensure local user has a GroupMember entry for this group (without changing memberCount)
                    try {
                        val myId = sessionManager.getUserId()
                        val existing = db.groupMemberDao().getMember(gid, myId)
                        if (existing == null) {
                            val myMember = com.example.myapplication.data.model.GroupMember(
                                groupId = gid,
                                userId = myId,
                                username = sessionManager.getUsername() ?: "Me",
                                nickname = null,
                                isAdmin = false,
                                joinedAt = System.currentTimeMillis()
                            )
                            db.groupMemberDao().insertMember(myMember)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    
    // Call this from fragments when they're created to receive pending messages
    fun consumePendingMessages(callback: NetworkManager.MessageCallback) {
        Log.d("MainActivity", "consumePendingMessages called, pending count: ${pendingMessages.size}")
        if (pendingMessages.isNotEmpty()) {
            val messages = pendingMessages.toList()
            pendingMessages.clear()
            messages.forEach { (type, payload) ->
                Log.d("MainActivity", "Delivering cached message: type=$type, payload=$payload")
                callback.onMessageReceived(type, payload)
            }
        }
    }
    
    fun logout() {
        lifecycleScope.launch {
            logger.logAuth("LOGOUT", sessionManager.getUsername() ?: "", true)
        }
        
        networkManager.logout()
        sessionManager.clearSession()
        navigateToLogin()
    }

    private suspend fun parseAndSaveFriends(payload: String) {
        try {
            val data = payload.split("&").associate {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else "" to ""
            }
            val count = data["count"]?.toIntOrNull() ?: 0
            val friends = mutableListOf<com.example.myapplication.data.model.Friend>()
            for (i in 0 until count) {
                val fid = data["id$i"]?.toIntOrNull() ?: continue
                val fname = data["name$i"] ?: continue
                friends.add(
                    com.example.myapplication.data.model.Friend(
                        userId = fid,
                        username = fname,
                        isOnline = false,
                        lastSeen = System.currentTimeMillis()
                    )
                )
            }

            withContext(Dispatchers.IO) {
                val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(this@MainActivity)
                try {
                    db.friendDao().clearAll()
                } catch (e: Exception) {
                    // ignore
                }
                if (friends.isNotEmpty()) {
                    try {
                        db.friendDao().insertFriends(friends)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        } catch (e: Exception) {
            // ignore parse errors
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (!isFinishing) {
            // Don't disconnect if just rotating or going to background
        }
    }
}
