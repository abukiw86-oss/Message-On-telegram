package com.msgrelay.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LocalMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val status: String, // PENDING, SENT, FAILED
    val retryCount: Int,
    val errorMessage: String?,
    val simSlot: Int? = null
)

class LocalDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "sms_relay.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_MESSAGES = "messages"
        private const val COL_ID = "id"
        private const val COL_SENDER = "sender"
        private const val COL_BODY = "body"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_STATUS = "status"
        private const val COL_RETRY_COUNT = "retry_count"
        private const val COL_ERROR_MSG = "error_message"
        private const val COL_SIM_SLOT = "sim_slot"

        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SENDER TEXT NOT NULL,
                $COL_BODY TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_RETRY_COUNT INTEGER DEFAULT 0,
                $COL_ERROR_MSG TEXT,
                $COL_SIM_SLOT INTEGER
            )
        """.trimIndent()
        db.execSQL(createQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_SIM_SLOT INTEGER")
            } catch (e: Exception) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
                onCreate(db)
            }
        }
    }

    fun insertMessage(sender: String, body: String, timestamp: Long, simSlot: Int? = null): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SENDER, sender)
            put(COL_BODY, body)
            put(COL_TIMESTAMP, timestamp)
            put(COL_STATUS, "PENDING")
            put(COL_RETRY_COUNT, 0)
            if (simSlot != null) {
                put(COL_SIM_SLOT, simSlot)
            }
        }
        return db.insert(TABLE_MESSAGES, null, values)
    }

    fun markStatus(id: Long, status: String, errorMessage: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, status)
            if (errorMessage != null) {
                put(COL_ERROR_MSG, errorMessage)
            }
        }
        db.update(TABLE_MESSAGES, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun incrementRetry(id: Long) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_MESSAGES SET $COL_RETRY_COUNT = $COL_RETRY_COUNT + 1 WHERE $COL_ID = ?", arrayOf(id))
    }

    fun getPendingMessages(): List<LocalMessage> {
        val list = mutableListOf<LocalMessage>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "$COL_STATUS != ?",
            arrayOf("SENT"),
            null,
            null,
            "$COL_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val simSlotIdx = it.getColumnIndex(COL_SIM_SLOT)
                val simSlot = if (simSlotIdx >= 0 && !it.isNull(simSlotIdx)) it.getInt(simSlotIdx) else null

                list.add(
                    LocalMessage(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        sender = it.getString(it.getColumnIndexOrThrow(COL_SENDER)),
                        body = it.getString(it.getColumnIndexOrThrow(COL_BODY)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        status = it.getString(it.getColumnIndexOrThrow(COL_STATUS)),
                        retryCount = it.getInt(it.getColumnIndexOrThrow(COL_RETRY_COUNT)),
                        errorMessage = it.getString(it.getColumnIndexOrThrow(COL_ERROR_MSG)),
                        simSlot = simSlot
                    )
                )
            }
        }
        return list
    }

    fun hasPendingMessages(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(1) FROM $TABLE_MESSAGES WHERE $COL_STATUS != 'SENT'",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0) > 0
            }
        }
        return false
    }

    fun resetAllFailedToPending() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, "PENDING")
        }
        db.update(TABLE_MESSAGES, values, "$COL_STATUS != ?", arrayOf("SENT"))
    }

    fun getRecentMessages(limit: Int = 30): List<LocalMessage> {
        val list = mutableListOf<LocalMessage>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                val simSlotIdx = it.getColumnIndex(COL_SIM_SLOT)
                val simSlot = if (simSlotIdx >= 0 && !it.isNull(simSlotIdx)) it.getInt(simSlotIdx) else null

                list.add(
                    LocalMessage(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        sender = it.getString(it.getColumnIndexOrThrow(COL_SENDER)),
                        body = it.getString(it.getColumnIndexOrThrow(COL_BODY)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        status = it.getString(it.getColumnIndexOrThrow(COL_STATUS)),
                        retryCount = it.getInt(it.getColumnIndexOrThrow(COL_RETRY_COUNT)),
                        errorMessage = it.getString(it.getColumnIndexOrThrow(COL_ERROR_MSG)),
                        simSlot = simSlot
                    )
                )
            }
        }
        return list
    }
}
