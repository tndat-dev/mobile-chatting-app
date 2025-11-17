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
        }
    }
    
    private fun setupUI() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    loadFragment(ChatsFragment())
                    binding.fabNewChat.visibility = android.view.View.VISIBLE
                    true
                }
                R.id.nav_friends -> {
                    loadFragment(FriendsFragment())
                    binding.fabNewChat.visibility = android.view.View.GONE
                    true
                }
                R.id.nav_groups -> {
                    loadFragment(GroupsFragment())
                    binding.fabNewChat.visibility = android.view.View.GONE
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    binding.fabNewChat.visibility = android.view.View.GONE
                    true
                }
                else -> false
            }
        }
        
        binding.fabNewChat.setOnClickListener {
            // Simple toast for now
            Toast.makeText(this, "Add friend feature", Toast.LENGTH_SHORT).show()
        }
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
                if (payload.contains("pendingCount=")) {
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
