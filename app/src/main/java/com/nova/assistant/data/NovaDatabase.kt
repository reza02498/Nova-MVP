package com.nova.assistant.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, AlarmEntity::class, ConversationEntity::class, NotificationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun alarmDao(): AlarmDao
    abstract fun conversationDao(): ConversationDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "nova_mvp.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL DEFAULT 0)")
            }
        }
    }
}
