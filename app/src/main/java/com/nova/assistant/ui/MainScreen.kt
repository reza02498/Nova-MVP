package com.nova.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }
    LaunchedEffect(state.navigateToSettings) {
        if (state.navigateToSettings) { viewModel.onNavigatedToSettings(); onNavigateToSettings() }
    }

    // ─── VOICE SETUP GUIDE DIALOG ───
    if (state.showVoiceGuide) {
        VoiceSetupDialog(onDismiss = { viewModel.dismissVoiceGuide() })
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F1A))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── TOP BAR ───
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))),
                            contentAlignment = Alignment.Center
                        ) { Text("ن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
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

            // ─── ERROR BANNER ───
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                state.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ─── CHAT LIST ───
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
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
                // Extra spacer so keyboard doesn't cover last message
                item { Spacer(Modifier.height(60.dp)) }
            }

            // ─── BOTTOM INPUT BAR ───
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = Color(0xFF1A1A2E)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::onInputTextChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("فرمان خود را بنویسید...", color = Color(0xFF6B7280)) },
                        maxLines = 2,
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
                    ) { Icon(Icons.Default.Send, contentDescription = "ارسال", tint = Color.White) }

                    Spacer(Modifier.width(4.dp))

                    val micColor = if (state.isListening) Color(0xFFEF4444) else Color(0xFF10B981)
                    FilledIconButton(
                        onClick = {
                            if (state.isListening) viewModel.stopVoiceInput()
                            else if (hasMicPermission) viewModel.startVoiceInput(context)
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.size(56.dp).shadow(if (state.isListening) 8.dp else 0.dp, CircleShape),
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
    }
}

// ─── VOICE SETUP GUIDE ───
@Composable
private fun VoiceSetupDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("🎙️ راهنمای تشخیص صدا", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(
                    "گوشی شما مدل تشخیص گفتار فارسی آفلاین ندارد. برای استفاده از فرمان صوتی دو راه دارید:",
                    color = Color(0xFFD1D5DB), fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                Text("راه ۱: دانلود بسته زبان فارسی", fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6), fontSize = 15.sp)
                Text(
                    "تنظیمات گوشی ← مدیریت عمومی ← زبان و ورودی ← تشخیص گفتار Google ← تشخیص گفتار آفلاین ← All ← فارسی ← دانلود",
                    color = Color(0xFF9CA3AF), fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Text("راه ۲: استفاده از تایپ متنی", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 15.sp)
                Text(
                    "می‌توانید فرمان‌های خود را در کادر پایین صفحه تایپ کنید. همه قابلیت‌ها با تایپ هم کار می‌کنند.",
                    color = Color(0xFF9CA3AF), fontSize = 13.sp
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:com.google.android.googlequicksearchbox")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("رفتن به تنظیمات", color = Color(0xFF8B5CF6), fontSize = 13.sp) }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) { Text("متوجه شدم", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isPartial: Boolean = false) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) Color(0xFF6366F1) else Color(0xFF1E1E3A)
    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (message.isUser) 18.dp else 6.dp,
        bottomEnd = if (message.isUser) 6.dp else 18.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        if (!message.isUser) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))), contentAlignment = Alignment.Center) {
                    Text("ن", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                Text("نُوا", fontSize = 11.sp, color = Color(0xFF8B5CF6))
            }
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
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
        ) { Text("ن", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(Modifier.height(20.dp))
        Text("نُوا", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("دستیار صوتی فارسی", fontSize = 16.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Medium)
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
