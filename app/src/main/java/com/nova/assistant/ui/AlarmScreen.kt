package com.nova.assistant.ui

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.assistant.ui.theme.NovaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovaTheme {
                AlarmScreenContent()
            }
        }
    }
}

@Composable
fun AlarmScreenContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as ComponentActivity

    val alarmId = activity.intent.getLongExtra("alarm_id", -1L)
    val alarmTitle = activity.intent.getStringExtra("alarm_title") ?: "آلارم"

    var dismissEnabled by remember { mutableStateOf(false) }
    var swipeProgress by remember { mutableFloatStateOf(0f) }

    // Current time
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }

    // Update clock every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "clock_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Vibration + Sound
    LaunchedEffect(Unit) {
        // Vibrate
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }

        // Play alarm sound
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(context, alarmUri)
            isLooping = true
            prepare()
            start()
        }

        // Cleanup on disposal
        try {
            while (true) kotlinx.coroutines.delay(100)
        } finally {
            mediaPlayer.stop()
            mediaPlayer.release()
            vibrator.cancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large pulsing clock
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = Color(0xFFE0E0E0),
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = currentTime,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = alarmTitle,
                fontSize = 22.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))

            // Snooze button
            OutlinedButton(
                onClick = {
                    activity.finish()
                    // Snooze is handled by the user saying "چرت ۱۰ دقیقه" after dismissal
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("چرت زدن", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Dismiss button (swipe-enabled)
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeProgress > 0.3f) {
                                    dismissEnabled = true
                                    activity.finish()
                                }
                                swipeProgress = 0f
                            }
                        ) { _, dragAmount ->
                            swipeProgress = (swipeProgress + dragAmount / 200f).coerceIn(0f, 1f)
                        }
                    }
            ) {
                Button(
                    onClick = {
                        activity.finish()
                    },
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F).copy(alpha = 0.8f + (swipeProgress * 0.2f))
                    )
                ) {
                    Text("بکشید برای خاموش →", fontSize = 16.sp)
                }
            }
        }
    }
}
