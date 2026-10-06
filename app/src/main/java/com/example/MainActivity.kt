package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.TncRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.onboarding.OnboardingNameScreen
import com.example.ui.theme.TNCNursingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TncRepository(applicationContext)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val savedMode = remember { repository.prefs.getThemeMode() }
            var isDarkTheme by remember {
                mutableStateOf(
                    when (savedMode) {
                        "dark" -> true
                        "light" -> false
                        else -> systemDark
                    }
                )
            }

            var hasCompletedOnboarding by remember {
                mutableStateOf(
                    repository.prefs.hasCompletedNamePrompt() && repository.prefs.getUserName().isNotBlank()
                )
            }

            var isBlocked by remember { mutableStateOf(repository.prefs.isBlocked()) }
            var blockReason by remember { mutableStateOf(repository.prefs.getBlockReason()) }

            LaunchedEffect(Unit) {
                val blocked = repository.checkInstallStatus()
                isBlocked = blocked
                blockReason = repository.prefs.getBlockReason()
            }

            TNCNursingTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        isBlocked -> {
                            BlockedScreen(
                                reason = blockReason,
                                onRetry = {
                                    isBlocked = repository.prefs.isBlocked()
                                    blockReason = repository.prefs.getBlockReason()
                                }
                            )
                        }
                        !hasCompletedOnboarding -> {
                            OnboardingNameScreen(
                                repository = repository,
                                onComplete = { _ ->
                                    hasCompletedOnboarding = true
                                },
                                onAdminLogin = {
                                    hasCompletedOnboarding = true
                                }
                            )
                        }
                        else -> {
                            AppNavigation(
                                repository = repository,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = { newTheme ->
                                    isDarkTheme = newTheme
                                    repository.prefs.setThemeMode(if (newTheme) "dark" else "light")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedScreen(reason: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F171A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Access Blocked",
                tint = Color(0xFFE76F51),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Access Paused",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reason.ifBlank { "This app installation is temporarily paused by the platform administrator." },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Check Status Again")
            }
        }
    }
}
