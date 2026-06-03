package com.nova.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nova.assistant.ui.MainScreen
import com.nova.assistant.ui.SettingsScreen
import com.nova.assistant.ui.theme.NovaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaTheme {
                NovaNavGraph()
            }
        }
    }
}

@Composable
fun NovaNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAlarm = { alarmId, title ->
                    // AlarmScreen is started as a separate Activity by AlarmReceiver,
                    // so we don't need to navigate here for MVP
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
