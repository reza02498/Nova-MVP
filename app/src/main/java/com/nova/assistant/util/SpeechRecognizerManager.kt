package com.nova.assistant.util

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var hasTriedEnglish = false

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit
    ) {
        if (SpeechRecognizer.isRecognitionAvailable(context).not()) {
            onError("تشخیص گفتار روی این دستگاه در دسترس نیست.")
            return
        }

        hasTriedEnglish = false
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
                        SpeechRecognizer.ERROR_AUDIO -> "خطای ضبط صدا. میکروفن در دسترس نیست."
                        SpeechRecognizer.ERROR_CLIENT -> "خطای داخلی برنامه."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی به میکروفن داده نشده."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            if (!hasTriedEnglish) {
                                // Persian offline model not available — try English
                                hasTriedEnglish = true
                                startWithEnglish(onResult, onPartialResult, onError, onRmsChanged)
                                return
                            } else {
                                "خطای شبکه. برای تشخیص گفتار آفلاین، بسته زبان فارسی را از تنظیمات گوشی دانلود کنید."
                            }
                        }
                        SpeechRecognizer.ERROR_NO_MATCH -> return // Silent — no speech detected
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "میکروفن در حال استفاده است. لطفاً صبر کنید."
                        SpeechRecognizer.ERROR_SERVER -> {
                            if (!hasTriedEnglish) {
                                hasTriedEnglish = true
                                startWithEnglish(onResult, onPartialResult, onError, onRmsChanged)
                                return
                            } else {
                                "مشکل در ارتباط با سرور گفتار. لطفاً از تایپ متنی استفاده کنید یا بسته زبان فارسی را دانلود کنید."
                            }
                        }
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "صدایی تشخیص داده نشد. لطفاً بلندتر صحبت کنید."
                        else -> "خطای تشخیص گفتار (کد $error). لطفاً از تایپ متنی استفاده کنید."
                    }
                    onError(msg)
                }

                override fun onReadyForSpeech(params: android.os.Bundle) {}
                override fun onBeginningOfSpeech() {}
                override fun onEndOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle) {}
            })
        }

        startWithPersian(onResult, onPartialResult, onError, onRmsChanged)
    }

    private fun startWithPersian(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit
    ) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        recognizer?.startListening(intent)
    }

    private fun startWithEnglish(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit
    ) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
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
