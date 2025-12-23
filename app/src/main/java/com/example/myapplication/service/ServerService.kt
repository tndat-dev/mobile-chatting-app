package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ServerService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var serverProcess: Process? = null
    private val CHANNEL_ID = "ServerServiceChannel"
    private val NOTIFICATION_ID = 1
    
    companion object {
        private const val TAG = "ServerService"
        var isRunning = false
            private set
        
        fun start(context: Context) {
            val intent = Intent(context, ServerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, ServerService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Server starting..."))
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startServer()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }
    
    private fun startServer() {
        serviceScope.launch {
            try {
                isRunning = true
                updateNotification("Server running on port 8080")
                
                // Get server executable path from assets or native libs
                val serverPath = getServerExecutablePath()
                
                if (serverPath != null && File(serverPath).exists()) {
                    // Start the C++ server process
                    val processBuilder = ProcessBuilder(serverPath)
                    processBuilder.redirectErrorStream(true)
                    
                    serverProcess = processBuilder.start()
                    
                    // Read server output
                    val reader = BufferedReader(InputStreamReader(serverProcess!!.inputStream))
                    var line: String?
                    
                    while (reader.readLine().also { line = it } != null) {
                        android.util.Log.d(TAG, "Server: $line")
                    }
                    
                    serverProcess?.waitFor()
                } else {
                    android.util.Log.e(TAG, "Server executable not found at: $serverPath")
                    updateNotification("Server error: executable not found")
                    
                    // Try alternative: use system command if server is installed
                    startServerAlternative()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error starting server", e)
                updateNotification("Server error: ${e.message}")
                isRunning = false
            }
        }
    }
    
    private suspend fun startServerAlternative() = withContext(Dispatchers.IO) {
        try {
            // Try to find and run server from common paths
            val possiblePaths = listOf(
                "/data/local/tmp/chat_server",
                "${applicationContext.filesDir.absolutePath}/chat_server",
                "${applicationContext.getExternalFilesDir(null)?.absolutePath}/chat_server"
            )
            
            for (path in possiblePaths) {
                val file = File(path)
                if (file.exists()) {
                    android.util.Log.d(TAG, "Found server at: $path")
                    
                    // Make executable
                    Runtime.getRuntime().exec("chmod 755 $path").waitFor()
                    
                    // Start server
                    serverProcess = Runtime.getRuntime().exec(path)
                    updateNotification("Server running (alternative)")
                    return@withContext
                }
            }
            
            android.util.Log.w(TAG, "Server executable not found in any location")
            updateNotification("Server not available - using remote server")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in alternative server start", e)
        }
    }
    
    private fun getServerExecutablePath(): String? {
        // Check if server binary exists in app's native libs
        val nativeLibDir = applicationContext.applicationInfo.nativeLibraryDir
        val serverInLib = File(nativeLibDir, "libchat_server.so")
        
        if (serverInLib.exists()) {
            return serverInLib.absolutePath
        }
        
        // Check in files directory
        val serverInFiles = File(applicationContext.filesDir, "chat_server")
        if (serverInFiles.exists()) {
            return serverInFiles.absolutePath
        }
        
        return null
    }
    
    private fun stopServer() {
        try {
            serverProcess?.destroy()
            serverProcess = null
            isRunning = false
            android.util.Log.d(TAG, "Server stopped")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error stopping server", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Server Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps chat server running in background"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chat Server")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
