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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.nova.assistant.ui.theme.*
import androidx.compose.ui.draw.scale
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
    ) { if (it) viewModel.startVoiceInput(context) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }
    LaunchedEffect(state.navigateToSettings) {
        if (state.navigateToSettings) { viewModel.onNavigatedToSettings(); onNavigateToSettings() }
    }

    if (state.showVoiceGuide) VoiceSetupDialog { viewModel.dismissVoiceGuide() }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── HEADER ───
            NovaHeader(
                isListening = state.isListening,
                isSpeaking = state.isSpeaking,
                onSettingsClick = onNavigateToSettings
            )

            // ─── ERROR BANNER ───
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                state.errorMessage?.let { err ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Rose400.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = Rose400, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(err, color = Rose400, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, "بستن", tint = DarkMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // ─── CHAT ───
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (state.messages.isEmpty() && !state.isProcessing) {
                    item { WelcomeScreen() }
                }
                items(state.messages, key = { "${it.isUser}_${it.timestamp}" }) { msg ->
                    ChatBubble(msg)
                }
                if (state.partialResult.isNotEmpty()) {
                    item { ChatBubble(ChatMessage(text = state.partialResult, isUser = true), partial = true) }
                }
                if (state.isProcessing) {
                    item { ThinkingIndicator() }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }

            // ─── INPUT BAR ───
            InputBar(
                text = state.inputText,
                onTextChange = viewModel::onInputTextChanged,
                onSend = viewModel::sendTextCommand,
                canSend = state.inputText.isNotBlank() && !state.isProcessing,
                isListening = state.isListening,
                isProcessing = state.isProcessing,
                hasMicPermission = hasMicPermission,
                onMicClick = {
                    if (state.isListening) viewModel.stopVoiceInput()
                    else if (hasMicPermission) viewModel.startVoiceInput(context)
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    }
}

// ═══════════════════════════════════
//  HEADER
// ═══════════════════════════════════

@Composable
private fun NovaHeader(isListening: Boolean, isSpeaking: Boolean, onSettingsClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkBg,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Indigo500, Purple500))),
                contentAlignment = Alignment.Center
            ) {
                Text("ن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("نُوا", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isListening) {
                        PulsingDot(color = Emerald400)
                        Spacer(Modifier.width(6.dp))
                        Text("در حال گوش دادن...", fontSize = 11.sp, color = Emerald400)
                    } else if (isSpeaking) {
                        PulsingDot(color = Cyan400)
                        Spacer(Modifier.width(6.dp))
                        Text("در حال صحبت", fontSize = 11.sp, color = Cyan400)
                    } else {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Emerald400.copy(alpha = 0.5f)))
                        Spacer(Modifier.width(6.dp))
                        Text("آماده", fontSize = 11.sp, color = DarkMuted)
                    }
                }
            }

            // Settings
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "تنظیمات",
                    tint = DarkMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════
//  WELCOME SCREEN
// ═══════════════════════════════════

@Composable
private fun WelcomeScreen() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated logo
        val pulse = rememberInfiniteTransition(label = "welcome")
        val scale by pulse.animateFloat(1f, 1.06f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), "scale")
        val glow by pulse.animateFloat(0.3f, 0.6f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), "glow")

        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(24.dp, CircleShape, ambientColor = Indigo500.copy(alpha = glow), spotColor = Purple500.copy(alpha = glow))
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo500, Purple500, Cyan400))),
            contentAlignment = Alignment.Center
        ) {
            Text("ن", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(24.dp))
        Text("نُوا", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DarkText)
        Spacer(Modifier.height(4.dp))
        Text("دستیار هوشمند فارسی", fontSize = 15.sp, color = Purple400, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(28.dp))

        // Feature cards
        val features = listOf(
            Triple("🔔", "آلارم و یادآوری", "آلارم بذار برای ساعت ۷ صبح"),
            Triple("📝", "یادداشت سریع", "یادداشت کن شماره حساب ۶۰۳۷"),
            Triple("⏱️", "تایمر", "تایمر ۱۰ دقیقه"),
            Triple("📡", "کنترل گوشی", "وای‌فای روشن، چراغ قوه"),
            Triple("📩", "خواندن پیام‌ها", "پیامامو بخون"),
            Triple("💬", "گفتگو", "ساعت چنده؟ امروز چندمه؟"),
        )

        for ((icon, title, subtitle) in features) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = DarkCard,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, color = DarkText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(subtitle, color = DarkMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════
//  CHAT BUBBLE
// ═══════════════════════════════════

@Composable
private fun ChatBubble(message: ChatMessage, partial: Boolean = false) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleColor = if (isUser) Indigo500 else DarkCard
    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        if (!isUser) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Indigo500, Purple500))),
                    contentAlignment = Alignment.Center
                ) { Text("ن", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(6.dp))
                Text("نُوا", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Purple400)
            }
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .alpha(if (partial) 0.5f else 1f),
            shape = shape,
            color = bubbleColor,
            shadowElevation = if (isUser) 4.dp else 0.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = DarkText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val timeStr = remember(message.timestamp) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        }
        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = DarkMuted.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ═══════════════════════════════════
//  THINKING INDICATOR
// ═══════════════════════════════════

@Composable
private fun ThinkingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = DarkCard) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Animated dots
                repeat(3) { i ->
                    val alpha by rememberInfiniteTransition(label = "dot$i").animateFloat(
                        0.3f, 1f, infiniteRepeatable(tween(400, delayMillis = i * 150), RepeatMode.Reverse), "dot$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Purple400.copy(alpha = alpha))
                    )
                    if (i < 2) Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("نُوا در حال فکر کردن...", color = DarkMuted, fontSize = 13.sp)
            }
        }
    }
}

// ═══════════════════════════════════
//  INPUT BAR
// ═══════════════════════════════════

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
    isListening: Boolean,
    isProcessing: Boolean,
    hasMicPermission: Boolean,
    onMicClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.95f),
        shadowElevation = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("چطور میتونم کمکت کنم؟", color = DarkMuted, fontSize = 14.sp) },
                maxLines = 2,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = DarkText),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Indigo500.copy(alpha = 0.6f),
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkBg,
                    unfocusedContainerColor = DarkBg,
                    cursorColor = Indigo500,
                )
            )

            Spacer(Modifier.width(8.dp))

            // Send button
            val sendScale by animateFloatAsState(if (canSend) 1f else 0.8f, label = "send")
            FilledIconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.size(44.dp).scale(sendScale),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Indigo500,
                    disabledContainerColor = DarkCard
                )
            ) { Icon(Icons.Filled.Send, "ارسال", tint = Color.White, modifier = Modifier.size(18.dp)) }

            Spacer(Modifier.width(4.dp))

            // Mic button
            val micColor = if (isListening) Rose400 else Emerald400
            val micPulse by rememberInfiniteTransition(label = "mic").animateFloat(
                1f, 1.15f, infiniteRepeatable(tween(900), RepeatMode.Reverse), "micpulse"
            )
            FilledIconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(52.dp)
                    .then(if (isListening) Modifier.scale(micPulse) else Modifier)
                    .shadow(if (isListening) 12.dp else 4.dp, CircleShape, ambientColor = micColor),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = micColor)
            ) {
                Icon(
                    if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    "میکروفن",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════
//  VOICE SETUP DIALOG
// ═══════════════════════════════════

@Composable
private fun VoiceSetupDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val isFa = com.nova.assistant.util.PhoneLanguage.isPersian(ctx)
    val path = com.nova.assistant.util.PhoneLanguage.offlineSpeechPath(ctx)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    if (isFa) "🎙️ راهنمای تشخیص صدا" else "🎙️ Voice Setup",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isFa) "گوشی شما بسته زبان فارسی را ندارد." else "Persian speech pack not installed.",
                    color = DarkSubtext, fontSize = 14.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    if (isFa) "مسیر دانلود:" else "Download path:",
                    fontWeight = FontWeight.SemiBold, color = Purple400, fontSize = 13.sp
                )
                Text(path, color = DarkMuted, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            try { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                            catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                    ) { Text(if (isFa) "تنظیمات" else "Settings", color = Purple400, fontSize = 13.sp) }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                    ) { Text(if (isFa) "متوجه شدم" else "Got it", color = Color.White) }
                }
            }
        }
    }
}

// ═══════════════════════════════════
//  UTILITY
// ═══════════════════════════════════

@Composable
private fun PulsingDot(color: Color = Emerald400) {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "a"
    )
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}
