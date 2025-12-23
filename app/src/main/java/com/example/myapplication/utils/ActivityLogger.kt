package com.example.myapplication.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ActivityLogger private constructor(context: Context) {
    
    companion object {
        private const val TAG = "ActivityLogger"
        private const val LOG_FILE_NAME = "chat_activity.log"
        private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
        
        @Volatile
        private var instance: ActivityLogger? = null
        
        fun getInstance(context: Context): ActivityLogger {
            return instance ?: synchronized(this) {
                instance ?: ActivityLogger(context).also { instance = it }
            }
        }
    }
    
    private val logFile: File = File(context.filesDir, LOG_FILE_NAME)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    enum class LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
    
    init {
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
    }
    
    suspend fun log(level: LogLevel, tag: String, message: String) {
        withContext(Dispatchers.IO) {
            try {
                // Check file size and rotate if needed
                if (logFile.length() > MAX_LOG_SIZE) {
                    rotateLog()
                }
                
                val timestamp = dateFormat.format(Date())
                val logEntry = "[$timestamp] [${level.name}] [$tag] $message\n"
                
                // Write to file
                FileWriter(logFile, true).use { writer ->
                    writer.append(logEntry)
                }
                
                // Also log to Android logcat
                when (level) {
                    LogLevel.DEBUG -> Log.d(tag, message)
                    LogLevel.INFO -> Log.i(tag, message)
                    LogLevel.WARNING -> Log.w(tag, message)
                    LogLevel.ERROR -> Log.e(tag, message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing log", e)
            }
        }
    }
    
    suspend fun logAuth(action: String, username: String, success: Boolean) {
        val message = "Auth: $action - User: $username - Success: $success"
        log(LogLevel.INFO, "AUTH", message)
    }
    
    suspend fun logConnection(action: String, host: String, port: Int, success: Boolean) {
        val message = "Connection: $action - Server: $host:$port - Success: $success"
        log(LogLevel.INFO, "CONNECTION", message)
    }
    
    suspend fun logMessage(action: String, fromUser: Int, toUser: Int, messageId: Long = -1) {
        val message = "Message: $action - From: $fromUser - To: $toUser - ID: $messageId"
        log(LogLevel.INFO, "MESSAGE", message)
    }
    
    suspend fun logFriendAction(action: String, userId: Int, targetUserId: Int, success: Boolean) {
        val message = "Friend: $action - User: $userId - Target: $targetUserId - Success: $success"
        log(LogLevel.INFO, "FRIEND", message)
    }
    
    suspend fun logGroupAction(action: String, groupId: Int, userId: Int, success: Boolean) {
        val message = "Group: $action - Group: $groupId - User: $userId - Success: $success"
        log(LogLevel.INFO, "GROUP", message)
    }
    
    suspend fun logError(tag: String, error: String, exception: Exception? = null) {
        val message = if (exception != null) {
            "$error - Exception: ${exception.message}\n${exception.stackTraceToString()}"
        } else {
            error
        }
        log(LogLevel.ERROR, tag, message)
    }
    
    private fun rotateLog() {
        try {
            val backupFile = File(logFile.parent, "${LOG_FILE_NAME}.old")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
            logFile.createNewFile()
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating log", e)
        }
    }
    
    fun getLogFile(): File = logFile
    
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        try {
            FileWriter(logFile, false).use { writer ->
                writer.write("")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logs", e)
        }
    }
    
    suspend fun getLogs(): String = withContext(Dispatchers.IO) {
        try {
            logFile.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading logs", e)
            ""
        }
    }
}
