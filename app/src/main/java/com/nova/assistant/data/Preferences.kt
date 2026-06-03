package com.nova.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_prefs")

data class AppPreferences(
    val speechRate: Float = 1.0f,
    val voiceLanguage: String = "fa",
    val isFirstLaunch: Boolean = true
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            voiceLanguage = prefs[Keys.VOICE_LANGUAGE] ?: "fa",
            isFirstLaunch = prefs[Keys.IS_FIRST_LAUNCH] ?: true
        )
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    }

    suspend fun setVoiceLanguage(lang: String) {
        context.dataStore.edit { it[Keys.VOICE_LANGUAGE] = lang }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { it[Keys.IS_FIRST_LAUNCH] = false }
    }
}
