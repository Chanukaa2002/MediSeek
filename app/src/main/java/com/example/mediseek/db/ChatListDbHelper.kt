package com.example.mediseek.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mediseek.model.ChatItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatListDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    // ... (Companion object and onCreate/onUpgrade are the same) ...
    companion object {
        private const val DATABASE_NAME = "mediseek.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_CHAT_LIST = "chat_list"

        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PROFILE_IMAGE_URL = "profile_image"
        private const val COLUMN_HAS_NEW_MESSAGE = "has_new_message"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_CHAT_LIST (
                $COLUMN_USER_ID TEXT PRIMARY KEY,
                $COLUMN_USERNAME TEXT,
                $COLUMN_PROFILE_IMAGE_URL TEXT,
                $COLUMN_HAS_NEW_MESSAGE INTEGER
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_LIST")
        onCreate(db)
    }


    /**
     * CORRECTION: Made this a suspend function to run on a background thread.
     */
    suspend fun saveChatList(chatList: List<ChatItem>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_CHAT_LIST, null, null)
            for (chat in chatList) {
                val values = ContentValues().apply {
                    put(COLUMN_USER_ID, chat.userId)
                    put(COLUMN_USERNAME, chat.username)
                    put(COLUMN_PROFILE_IMAGE_URL, chat.profileImageUrl)
                    put(COLUMN_HAS_NEW_MESSAGE, if (chat.hasNewMessage) 1 else 0)
                }
                db.insert(TABLE_CHAT_LIST, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * CORRECTION: Made this a suspend function to run on a background thread.
     */
    suspend fun getAllChats(): List<ChatItem> = withContext(Dispatchers.IO) {
        val chatList = mutableListOf<ChatItem>()
        val db = readableDatabase
        val cursor: Cursor = db.query(TABLE_CHAT_LIST, null, null, null, null, null, null)

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val chatItem = ChatItem(
                        userId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                        username = it.getString(it.getColumnIndexOrThrow(COLUMN_USERNAME)),
                        profileImageUrl = it.getString(it.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE_URL)),
                        hasNewMessage = it.getInt(it.getColumnIndexOrThrow(COLUMN_HAS_NEW_MESSAGE)) == 1
                    )
                    chatList.add(chatItem)
                } while (it.moveToNext())
            }
        }
        return@withContext chatList
    }
}