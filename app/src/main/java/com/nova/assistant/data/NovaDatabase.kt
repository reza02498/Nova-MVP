package com.nova.assistant.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class, ConversationEntity::class, NotificationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun conversationDao(): ConversationDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "nova_mvp.db"
    }
}
