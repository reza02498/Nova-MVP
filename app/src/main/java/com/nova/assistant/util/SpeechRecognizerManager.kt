package com.nova.assistant.util

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit
    ) {
        if (SpeechRecognizer.isRecognitionAvailable(context).not()) {
            onError("Speech recognition not available on this device")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: android.os.Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) onResult(text)
                }

                override fun onPartialResults(results: android.os.Bundle) {
                    val partial = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = partial?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) onPartialResult(text)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 10).coerceIn(0f, 10f) / 10f)
                    onRmsChanged(normalized)
                }

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "No microphone permission"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        else -> "Speech error code: $error"
                    }
                    if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                        onError(msg)
                    }
                }

                override fun onReadyForSpeech(params: android.os.Bundle) {}
                override fun onBeginningOfSpeech() {}
                override fun onEndOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}
