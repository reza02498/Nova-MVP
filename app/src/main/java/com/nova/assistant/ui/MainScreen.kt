package com.nova.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
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
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Permission check
    val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceInput(context)
    }

    // Auto-scroll to bottom
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Handle navigation events
    LaunchedEffect(state.navigateToSettings) {
        if (state.navigateToSettings) {
            viewModel.onNavigatedToSettings()
            onNavigateToSettings()
        }
    }
    LaunchedEffect(state.navigateToAlarm) {
        state.navigateToAlarm?.let { (id, title) ->
            viewModel.onAlarmHandled()
            onNavigateToAlarm(id, title)
        }
    }

    // Error snackbar
    state.errorMessage?.let { error ->
        LaunchedEffect(error) {
            delay(3000)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── TOP BAR ───
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("نُوا", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (state.isListening) {
                            Spacer(Modifier.width(8.dp))
                            PulsingDot()
                        }
                        if (state.isSpeaking) {
                            Spacer(Modifier.width(8.dp))
                            Text("🔊", fontSize = 14.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // ─── CHAT LIST ───
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        EmptyWelcome()
                    }
                }
                items(state.messages, key = { "${it.isUser}_${it.timestamp}" }) { msg ->
                    ChatBubble(message = msg)
                }

                // Partial result during voice
                if (state.partialResult.isNotEmpty()) {
                    item {
                        ChatBubble(
                            message = ChatMessage(text = state.partialResult, isUser = true),
                            isPartial = true
                        )
                    }
                }

                // Processing indicator
                if (state.isProcessing) {
                    item {
                        ProcessingBubble()
                    }
                }
            }

            // ─── ERROR BAR ───
            state.errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ─── BOTTOM INPUT BAR ───
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("فرمان خود را بنویسید...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    // Send button
                    FilledIconButton(
                        onClick = { viewModel.sendTextCommand() },
                        enabled = state.inputText.isNotBlank() && !state.isProcessing,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "ارسال")
                    }

                    Spacer(Modifier.width(4.dp))

                    // Mic button
                    val micColor = if (state.isListening)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.secondary

                    FilledIconButton(
                        onClick = {
                            if (state.isListening) {
                                viewModel.stopVoiceInput()
                            } else if (hasMicPermission) {
                                viewModel.startVoiceInput(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .then(
                                if (state.isListening) Modifier.scale(1.1f) else Modifier
                            ),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = micColor
                        )
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

        // ─── READING CONTROL OVERLAY ───
        if (state.isSpeaking) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .fillMaxWidth(0.7f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("در حال خواندن...", modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.stopSpeaking() }) {
                        Icon(Icons.Default.Stop, contentDescription = "قطع")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isPartial: Boolean = false) {
    val bgColor = if (message.isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (message.isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isUser) 16.dp else 4.dp,
        bottomEnd = if (message.isUser) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = shape,
            color = bgColor.copy(alpha = if (isPartial) 0.6f else 1f)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Timestamp
        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ProcessingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("در حال پردازش...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(0xFF00E5FF).copy(alpha = alpha))
    )
}

@Composable
private fun EmptyWelcome() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "نُوا",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "سلام! چی کار می‌تونم برات بکنم؟",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = """
                |می‌تونی اینا رو امتحان کنی:
                |🔔 آلارم بذار برای ساعت ۷ صبح
                |📝 یادآوری کن نون بخرم فردا ساعت ۱۰
                |📩 پیامامو بخون
                |⏰ ساعت چنده
                |📅 امروز چندمه
            """.trimMargin(),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
