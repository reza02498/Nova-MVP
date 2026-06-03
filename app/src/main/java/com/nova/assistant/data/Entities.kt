package com.nova.assistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val triggerTime: Long,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null, // "DAILY", "WEEKLY", "NONE"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMessage: String,
    val assistantResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val inputType: String // "VOICE" or "TEXT"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val senderName: String,
    val content: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val wasReadAloud: Boolean = false
)
