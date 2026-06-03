package com.nova.assistant.data

import androidx.room.Database
import androidx.room.RoomDatabase

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
    }
}
