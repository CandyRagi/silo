package com.example.silo.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HistoryEntity(
    val id: Long = 0,
    val fileName: String,
    val totalBytes: Long,
    val direction: String,
    val timestamp: Long,
    val savedPath: String? = null
)

class HistoryDao(private val dbHelper: SiloDatabase) {
    fun insert(history: HistoryEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("fileName", history.fileName)
            put("totalBytes", history.totalBytes)
            put("direction", history.direction)
            put("timestamp", history.timestamp)
            put("savedPath", history.savedPath)
        }
        db.insert("history_transfers", null, values)
        dbHelper.notifyChange()
    }

    fun getAllHistory(): StateFlow<List<HistoryEntity>> {
        return dbHelper.historyFlow
    }

    fun clearHistory() {
        val db = dbHelper.writableDatabase
        db.delete("history_transfers", null, null)
        dbHelper.notifyChange()
    }
}

class SiloDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "silo_database.db", null, 1) {
    val historyFlow = MutableStateFlow<List<HistoryEntity>>(emptyList())

    init {
        notifyChange()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE history_transfers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fileName TEXT NOT NULL,
                totalBytes INTEGER NOT NULL,
                direction TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                savedPath TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS history_transfers")
        onCreate(db)
    }

    fun historyDao(): HistoryDao = HistoryDao(this)

    fun notifyChange() {
        val db = readableDatabase
        val cursor = db.query(
            "history_transfers", null, null, null, null, null, "timestamp DESC"
        )
        val list = mutableListOf<HistoryEntity>()
        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow("id"))
                val fileName = getString(getColumnIndexOrThrow("fileName"))
                val totalBytes = getLong(getColumnIndexOrThrow("totalBytes"))
                val direction = getString(getColumnIndexOrThrow("direction"))
                val timestamp = getLong(getColumnIndexOrThrow("timestamp"))
                val savedPath = getString(getColumnIndexOrThrow("savedPath"))
                list.add(HistoryEntity(id, fileName, totalBytes, direction, timestamp, savedPath))
            }
        }
        cursor.close()
        historyFlow.value = list
    }

    companion object {
        @Volatile
        private var INSTANCE: SiloDatabase? = null

        fun getDatabase(context: Context): SiloDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = SiloDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
