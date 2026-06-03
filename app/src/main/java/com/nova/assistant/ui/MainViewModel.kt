package com.nova.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.assistant.data.ConversationDao
import com.nova.assistant.data.ConversationEntity
import com.nova.assistant.data.PreferencesManager
import com.nova.assistant.domain.Command
import com.nova.assistant.domain.CommandExecutor
import com.nova.assistant.domain.CommandParser
import com.nova.assistant.util.SpeechRecognizerManager
import com.nova.assistant.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class MainUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isSpeaking: Boolean = false,
    val inputText: String = "",
    val partialResult: String = "",
    val rmsLevel: Float = 0f,
    val errorMessage: String? = null,
    val navigateToSettings: Boolean = false,
    val navigateToAlarm: Pair<Long, String>? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val commandParser: CommandParser,
    private val commandExecutor: CommandExecutor,
    private val conversationDao: ConversationDao,
    private val ttsManager: TtsManager,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizerManager? = null

    init {
        viewModelScope.launch {
            conversationDao.observeAll().collect { conversations ->
                val msgs = conversations.reversed().flatMap { conv ->
                    listOf(
                        ChatMessage(text = conv.userMessage, isUser = true, timestamp = conv.timestamp),
                        ChatMessage(text = conv.assistantResponse, isUser = false, timestamp = conv.timestamp)
                    )
                }
                _state.update { it.copy(messages = msgs) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendTextCommand() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty()) return

        _state.update { it.copy(inputText = "", partialResult = "", isProcessing = true) }
        processCommand(text, "TEXT")
    }

    fun startVoiceInput(context: android.content.Context) {
        _state.update { it.copy(isListening = true, partialResult = "", errorMessage = null) }

        speechRecognizer = SpeechRecognizerManager(context)
        speechRecognizer?.startListening(
            onResult = { text ->
                _state.update { it.copy(isListening = false, isProcessing = true, partialResult = "") }
                processCommand(text, "VOICE")
            },
            onPartialResult = { partial ->
                _state.update { it.copy(partialResult = partial) }
            },
            onError = { error ->
                _state.update { it.copy(isListening = false, errorMessage = error) }
            },
            onRmsChanged = { rms ->
                _state.update { it.copy(rmsLevel = rms) }
            }
        )
    }

    fun stopVoiceInput() {
        speechRecognizer?.stopListening()
        speechRecognizer = null
        _state.update { it.copy(isListening = false) }
    }

    private fun processCommand(text: String, inputType: String) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            try {
                // Handle greetings manually
                val greeting = checkGreeting(text)
                val response = if (greeting != null) {
                    greeting
                } else {
                    val command = commandParser.parse(text)
                    commandExecutor.execute(command, inputType)
                }

                _state.update { it.copy(isProcessing = false, isSpeaking = true) }

                // Speak response safely
                try {
                    val prefsData = prefs.preferences.first()
                    ttsManager.setRate(prefsData.speechRate)
                } catch (_: Exception) {
                    ttsManager.setRate(1.0f)
                }
                ttsManager.speak(
                    text = response,
                    onDone = { _state.update { it.copy(isSpeaking = false) } }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        isListening = false,
                        errorMessage = "متوجه نشدم. لطفاً دوباره بگید."
                    )
                }
            }
        }
    }

    private fun checkGreeting(text: String): String? {
        val t = text.trim().lowercase()
        return when {
            t in listOf("سلام", "سلام.", "سلام!", "salam", "hello", "hi", "hey") ->
                "سلام! من نُوا هستم، دستیار صوتی تو. چطور می‌تونم کمکت کنم؟ می‌تونی ازم بپرسی «ساعت چنده» یا بگی «آلارم بذار برای ساعت ۷ صبح»."
            t.contains(Regex("خوبی|چطوری|حالت|چه خبر")) ->
                "مرسی، خوبم! تو چطوری؟ هر کاری داشتی بگو برات انجام بدم."
            t.contains(Regex("مرسی|ممنون|تشکر|دستت درد|دمت گرم")) ->
                "خواهش می‌کنم! کاری دیگه‌ای هست برات انجام بدم؟"
            t.contains(Regex("خداحافظ|بای|می‌بینمت|بعدا")) ->
                "خداحافظ! هر وقت نیاز داشتی من اینجام. فقط صدایم کن."
            else -> null
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _state.update { it.copy(isSpeaking = false) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onNavigatedToSettings() {
        _state.update { it.copy(navigateToSettings = false) }
    }

    fun onAlarmHandled() {
        _state.update { it.copy(navigateToAlarm = null) }
    }

    fun showAlarm(alarmId: Long, title: String) {
        _state.update { it.copy(navigateToAlarm = Pair(alarmId, title)) }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.stopListening()
        ttsManager.shutdown()
    }
}
