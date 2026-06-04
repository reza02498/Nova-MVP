package com.nova.assistant.util

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerManager(context: Context) {

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var hasTriedEnglish = false
    private var silenceTimer: Handler? = null
    private var currentOnResult: ((String) -> Unit)? = null
    private var currentOnError: ((String) -> Unit)? = null
    private var fullTranscript = StringBuilder()

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit
    ) {
        if (SpeechRecognizer.isRecognitionAvailable(appContext).not()) {
            onError("تشخیص گفتار روی این دستگاه در دسترس نیست.")
            return
        }

        stopListening()
        hasTriedEnglish = false
        fullTranscript = StringBuilder()
        currentOnResult = onResult
        currentOnError = onError
        silenceTimer = Handler(Looper.getMainLooper())

        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: android.os.Bundle) {}

                override fun onBeginningOfSpeech() {
                    // User started speaking — cancel silence timer
                    silenceTimer?.removeCallbacksAndMessages(null)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 10).coerceIn(0f, 10f) / 10f)
                    onRmsChanged(normalized)
                }

                override fun onEndOfSpeech() {
                    // User stopped speaking — start 3-second silence timer
                    silenceTimer?.removeCallbacksAndMessages(null)
                    silenceTimer?.postDelayed({
                        // 3 seconds of silence — stop and process
                        recognizer?.stopListening()
                    }, 3000)
                }

                override fun onResults(results: android.os.Bundle) {
                    silenceTimer?.removeCallbacksAndMessages(null)
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    fullTranscript.append(text)
                    if (fullTranscript.isNotEmpty()) {
                        currentOnResult?.invoke(fullTranscript.toString().trim())
                    }
                    fullTranscript = StringBuilder()
                }

                override fun onPartialResults(results: android.os.Bundle) {
                    val partial = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = partial?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) onPartialResult(text)
                }

                override fun onError(error: Int) {
                    silenceTimer?.removeCallbacksAndMessages(null)

                    // If we have partial transcript, use it
                    if (fullTranscript.isNotEmpty() && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        currentOnResult?.invoke(fullTranscript.toString().trim())
                        fullTranscript = StringBuilder()
                        return
                    }

                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO ->
                            "🎤 خطای میکروفن — مطمئن شوید میکروفن توسط برنامه دیگری استفاده نمیشود."
                        SpeechRecognizer.ERROR_CLIENT ->
                            "⚙️ خطای سیستمی — لطفاً دوباره تلاش کنید."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "🔒 دسترسی میکروفن داده نشده — از تنظیمات گوشی > برنامهها > Nova > مجوزها، دسترسی میکروفن را فعال کنید."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            if (!hasTriedEnglish) {
                                hasTriedEnglish = true
                                startWithEnglish(onResult, onPartialResult, onError, onRmsChanged)
                                return
                            } else {
                                "📡 خطای شبکه + بسته زبان آفلاین نصب نیست. برای تشخیص گفتار فارسی:\n۱. به اینترنت وصل شوید\n۲. یا بسته زبان فارسی را از مسیر زیر دانلود کنید:\nتنظیمات > زبان و ورودی > تشخیص گفتار Google > آفلاین > فارسی"
                            }
                        }
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            if (fullTranscript.isNotEmpty()) {
                                currentOnResult?.invoke(fullTranscript.toString().trim())
                                fullTranscript = StringBuilder()
                                return
                            }
                            "🎙️ تشخیص گفتار ناموفق. دو دلیل رایج:\n۱. بسته زبان فارسی نصب نیست — از تنظیمات گوشی دانلود کنید\n۲. صدای محیط زیاد است — در جای ساکتتر تلاش کنید"
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                            "⏳ سیستم تشخیص صدا مشغول است — چند لحظه صبر کنید."
                        SpeechRecognizer.ERROR_SERVER -> {
                            if (!hasTriedEnglish) {
                                hasTriedEnglish = true
                                startWithEnglish(onResult, onPartialResult, onError, onRmsChanged)
                                return
                            } else {
                                "🌐 سرور تشخیص گفتار در دسترس نیست. لطفاً از تایپ متنی استفاده کنید یا بسته زبان آفلاین را نصب کنید."
                            }
                        }
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            if (fullTranscript.isNotEmpty()) {
                                currentOnResult?.invoke(fullTranscript.toString().trim())
                                fullTranscript = StringBuilder()
                                return
                            }
                            "⏰ صدایی دریافت نشد — لطفاً دوباره تلاش کنید و نزدیکتر به میکروفن صحبت کنید."
                        }
                        else -> "❓ خطای تشخیص گفتار (کد $error). لطفاً از تایپ متنی استفاده کنید."
                    }
                    currentOnError?.invoke(msg)
                }

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
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
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
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        silenceTimer?.removeCallbacksAndMessages(null)
        silenceTimer = null
        fullTranscript = StringBuilder()
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}
