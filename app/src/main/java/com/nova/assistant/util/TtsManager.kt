package com.nova.assistant.util

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile private var isReady = false
    private var currentRate = 1.0f
    private var currentLocale: Locale = Locale("fa")
    private var retryCount = 0
    private val maxRetries = 10

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try multiple Persian locale formats (different devices support different ones)
                currentLocale = findBestPersianLocale()
                tts?.setLanguage(currentLocale)
                isReady = true
            }
        }
    }

    private fun findBestPersianLocale(): Locale {
        val persianLocales = listOf(
            Locale("fa", "IR"),       // fa-IR — standard Persian
            Locale("fa"),             // fa — generic Persian
            Locale.forLanguageTag("fa-IR"), // BCP 47 tag
            Locale("ar"),             // Arabic (fallback — similar TTS)
        )

        for (locale in persianLocales) {
            val result = tts?.setLanguage(locale) ?: continue
            if (result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED) {
                return locale
            }
        }

        // Absolutely nothing works — use whatever is available
        return Locale.getDefault()
    }

    fun speak(text: String, onStart: (() -> Unit)? = null, onDone: (() -> Unit)? = null) {
        if (!isReady || tts == null) {
            if (retryCount++ >= maxRetries) {
                retryCount = 0
                return // Give up after max retries
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                speak(text, onStart, onDone)
            }, 500)
            return
        }
        retryCount = 0

        // Always ensure Persian locale is set before speaking
        if (tts?.language != currentLocale) {
            tts?.setLanguage(currentLocale)
        }

        tts?.setSpeechRate(currentRate)
        tts?.setPitch(1.0f)

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
