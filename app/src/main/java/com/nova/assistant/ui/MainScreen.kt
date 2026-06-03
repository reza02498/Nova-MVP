package com.nova.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToAlarm: (alarmId: Long, title: String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceInput(context)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(state.navigateToSettings) {
        if (state.navigateToSettings) { viewModel.onNavigatedToSettings(); onNavigateToSettings() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F0F1A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("نُوا", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        if (state.isListening) {
                            Spacer(Modifier.width(8.dp))
                            PulsingDot()
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = Color.White.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F1A))
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = Color(0xFF1A1A2E)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::onInputTextChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("فرمان خود را بنویسید...", color = Color(0xFF6B7280)) },
                        maxLines = 3,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF374151),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF6366F1),
                            focusedContainerColor = Color(0xFF111122),
                            unfocusedContainerColor = Color(0xFF111122),
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = viewModel::sendTextCommand,
                        enabled = state.inputText.isNotBlank() && !state.isProcessing,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF6366F1),
                            disabledContainerColor = Color(0xFF374151)
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "ارسال", tint = Color.White)
                    }

                    Spacer(Modifier.width(4.dp))

                    val micColor = if (state.isListening) Color(0xFFEF4444) else Color(0xFF10B981)
                    FilledIconButton(
                        onClick = {
                            if (state.isListening) viewModel.stopVoiceInput()
                            else if (hasMicPermission) viewModel.startVoiceInput(context)
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(if (state.isListening) 8.dp else 0.dp, CircleShape),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = micColor)
                    ) {
                        Icon(
                            if (state.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "میکروفن",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (state.messages.isEmpty() && !state.isProcessing) {
                item { EmptyWelcome() }
            }
            items(state.messages, key = { "${it.isUser}_${it.timestamp}" }) { msg ->
                ChatBubble(message = msg)
            }
            if (state.partialResult.isNotEmpty()) {
                item { ChatBubble(message = ChatMessage(text = state.partialResult, isUser = true), isPartial = true) }
            }
            if (state.isProcessing) {
                item { ProcessingBubble() }
            }
        }
    }

    // Error Toast
    state.errorMessage?.let { error ->
        LaunchedEffect(error) {
            delay(4000)
            viewModel.clearError()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isPartial: Boolean = false) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    val bgColor = if (message.isUser)
        Color(0xFF6366F1)
    else
        Color(0xFF1E1E3A)

    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (message.isUser) 18.dp else 6.dp,
        bottomEnd = if (message.isUser) 6.dp else 18.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        if (!message.isUser) {
            Text(
                "نُوا", fontSize = 11.sp, color = Color(0xFF8B5CF6),
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = shape,
            color = bgColor.copy(alpha = if (isPartial) 0.5f else 1f)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(14.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ProcessingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E1E3A)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF8B5CF6))
                Spacer(Modifier.width(8.dp))
                Text("در حال پردازش...", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "alpha"
    )
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981).copy(alpha = alpha)))
}

@Composable
private fun EmptyWelcome() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("ن", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("نُوا", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "دستیار صوتی فارسی", fontSize = 16.sp,
            color = Color(0xFF8B5CF6), fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(32.dp))
        Text("چطور می‌تونم کمکت کنم؟", fontSize = 18.sp, color = Color(0xFFD1D5DB))
        Spacer(Modifier.height(20.dp))
        val hints = listOf(
            "🔔 آلارم بذار برای ساعت ۷ صبح",
            "📝 یادآوری کن نان بخرم فردا ساعت ۱۰",
            "📩 پیامامو بخون",
            "⏰ ساعت چنده",
            "📅 امروز چندمه"
        )
        for (hint in hints) {
            Text(hint, fontSize = 14.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
