package com.nova.assistant.util

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentRate = 1.0f

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val faResult = tts?.setLanguage(Locale.forLanguageTag("fa-IR"))
                if (faResult == TextToSpeech.LANG_MISSING_DATA ||
                    faResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                isInitialized = true
            }
        }
    }

    fun speak(text: String, onStart: (() -> Unit)? = null, onDone: (() -> Unit)? = null) {
        if (!isInitialized) return
        tts?.setSpeechRate(currentRate)

        val utteranceId = "nova_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { onStart?.invoke() }
            override fun onDone(utteranceId: String?) { onDone?.invoke() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onDone?.invoke() }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun increaseRate() {
        currentRate = (currentRate + 0.1f).coerceAtMost(2.0f)
    }

    fun decreaseRate() {
        currentRate = (currentRate - 0.1f).coerceAtLeast(0.5f)
    }

    fun setRate(rate: Float) {
        currentRate = rate.coerceIn(0.5f, 2.0f)
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
