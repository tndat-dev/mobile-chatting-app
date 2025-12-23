package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.model.*

@Database(
    entities = [
        User::class,
        Message::class,
        Friend::class,
        FriendRequest::class,
        Group::class,
        GroupMember::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun friendDao(): FriendDao
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun conversationDao(): ConversationDao
    
    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null
        
        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                    .addMigrations(
                        // Migration from version 3 -> 4: add `nickname` column to group_members
                        object : androidx.room.migration.Migration(3, 4) {
                            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                                // Add nullable column `nickname` to existing table
                                database.execSQL("ALTER TABLE group_members ADD COLUMN nickname TEXT")
                            }
                        }
                        ,
                        // Migration 4 -> 5: convert group_members to use composite primary key (groupId, userId)
                        object : androidx.room.migration.Migration(4, 5) {
                            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                                // Create new table with composite primary key
                                database.execSQL("CREATE TABLE IF NOT EXISTS group_members_new (groupId INTEGER NOT NULL, userId INTEGER NOT NULL, username TEXT NOT NULL, nickname TEXT, joinedAt INTEGER NOT NULL, isAdmin INTEGER NOT NULL, PRIMARY KEY(groupId, userId))")
                                // Copy distinct rows from old table into new table
                                database.execSQL("INSERT OR REPLACE INTO group_members_new (groupId, userId, username, nickname, joinedAt, isAdmin) SELECT groupId, userId, username, nickname, joinedAt, CASE WHEN isAdmin=1 THEN 1 ELSE 0 END FROM group_members")
                                // Drop old table and rename new
                                database.execSQL("DROP TABLE IF EXISTS group_members")
                                database.execSQL("ALTER TABLE group_members_new RENAME TO group_members")
                            }
                        }
                    )
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
