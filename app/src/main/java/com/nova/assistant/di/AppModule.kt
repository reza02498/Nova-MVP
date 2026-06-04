package com.nova.assistant.di

import android.content.Context
import androidx.room.Room
import com.nova.assistant.data.AlarmDao
import com.nova.assistant.data.ConversationDao
import com.nova.assistant.data.NoteDao
import com.nova.assistant.data.NotificationDao
import com.nova.assistant.data.NovaDatabase
import com.nova.assistant.util.TtsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NovaDatabase {
        return Room.databaseBuilder(
            context,
            NovaDatabase::class.java,
            NovaDatabase.NAME
        ).addMigrations(NovaDatabase.MIGRATION_1_2).build()
    }

    @Provides
    fun provideNoteDao(db: NovaDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideAlarmDao(db: NovaDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideConversationDao(db: NovaDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideNotificationDao(db: NovaDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun provideTtsManager(@ApplicationContext context: Context): TtsManager = TtsManager(context)
}
