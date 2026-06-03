package com.nova.assistant.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert suspend fun insert(note: NoteEntity): Long
    @Delete suspend fun delete(note: NoteEntity)
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") suspend fun getAll(): List<NoteEntity>
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun getById(id: Long): NoteEntity?
    @Query("SELECT * FROM notes WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC") suspend fun search(query: String): List<NoteEntity>
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM notes") suspend fun deleteAll()
}

@Dao
interface AlarmDao {
    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM alarms")
    suspend fun deleteAll()

    @Query("SELECT * FROM alarms WHERE isActive = 1 ORDER BY triggerTime ASC")
    suspend fun getAllActive(): List<AlarmEntity>

    @Query("SELECT * FROM alarms ORDER BY triggerTime ASC")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE isActive = 1 AND triggerTime > :now ORDER BY triggerTime ASC LIMIT 1")
    suspend fun getNextActive(now: Long): AlarmEntity?
}

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC LIMIT 1")
    suspend fun getLast(): NotificationEntity?

    @Query("UPDATE notifications SET wasReadAloud = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications WHERE receivedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
