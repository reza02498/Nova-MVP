package com.nova.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.assistant.data.AppPreferences
import com.nova.assistant.data.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    val preferences = prefs.preferences

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { prefs.setSpeechRate(rate) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { prefs.setVoiceLanguage(lang) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle(
        initialValue = AppPreferences()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("تنظیمات صدا", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("سرعت گفتار: ${"%.1f".format(prefs.speechRate)}x")
                    Slider(
                        value = prefs.speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14
                    )
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("زبان پاسخ‌ها")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.voiceLanguage == "fa",
                            onClick = { viewModel.setLanguage("fa") },
                            label = { Text("فارسی") }
                        )
                        FilterChip(
                            selected = prefs.voiceLanguage == "en",
                            onClick = { viewModel.setLanguage("en") },
                            label = { Text("English") }
                        )
                    }
                }
            }

            Divider()

            // TTS Persian Download
            Text("صدای فارسی", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("اگر Nova با صدای انگلیسی صحبت می‌کند، بسته صدای فارسی گوگل را دانلود کنید:")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("دانلود بسته صدای فارسی") }
                }
            }

            Divider()

            Text("درباره", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("نُوا — دستیار صوتی فارسی")
                    Text(
                        "نسخه ۱.۰ MVP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "این نسخه حداقلی از دستیار نُوا است که می‌تواند آلارم و یادآوری تنظیم کند، " +
                                "پیام‌های شما را بخواند، و به فرمان‌های صوتی و متنی پاسخ دهد.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
