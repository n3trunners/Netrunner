package com.netrunner

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.netrunner.core.RustDaemonBridge
import com.netrunner.ui.DashboardScreen
import com.netrunner.ui.WorkspaceProject
import com.netrunner.ui.WorkspaceTerminalScreen
import com.netrunner.ui.theme.NetRunnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RustDaemonBridge.boot(filesDir)

        val prefs = getSharedPreferences("netrunner_prefs", Context.MODE_PRIVATE)

        setContent {
            var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
            var userApiToken by remember { mutableStateOf(prefs.getString("user_api_token", "") ?: "") }
            var selectedModel by remember { mutableStateOf(prefs.getString("gemini_model", "gemini-4-hydrogen") ?: "gemini-4-hydrogen") }
            var showTokenDialog by remember { mutableStateOf(false) }
            var selectedProject by remember { mutableStateOf<WorkspaceProject?>(null) }
            val projects = remember { mutableStateListOf<WorkspaceProject>() }

            NetRunnerTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val active = selectedProject
                    if (active == null) {
                        DashboardScreen(
                            developerName = "Developer",
                            projects = projects,
                            hasApiToken = userApiToken.isNotBlank(),
                            isDarkMode = isDark,
                            onCreateProject = {
                                val idx = projects.size + 1
                                val created = WorkspaceProject(
                                    id = "proj-$idx",
                                    name = "rust-android-service-$idx",
                                    description = "Kotlin Compose UI + Rust Core Daemon workspace",
                                    runtime = "Rust + Compose",
                                    updatedAt = "Just now",
                                )
                                projects.add(0, created)
                            },
                            onToggleTheme = {
                                isDark = !isDark
                                prefs.edit().putBoolean("dark_mode", isDark).apply()
                            },
                            onOpenTokenSettings = { showTokenDialog = true },
                            onSelectProject = { selectedProject = it },
                        )
                    } else {
                        WorkspaceTerminalScreen(
                            project = active,
                            userApiToken = userApiToken,
                            modelId = selectedModel,
                            onOpenTokenSettings = { showTokenDialog = true },
                            onBack = { selectedProject = null },
                        )
                    }

                    if (showTokenDialog) {
                        var draftToken by remember(showTokenDialog) { mutableStateOf(userApiToken) }
                        var draftModel by remember(showTokenDialog) { mutableStateOf(selectedModel) }
                        AlertDialog(
                            onDismissRequest = { showTokenDialog = false },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            title = {
                                Text(
                                    text = "Bring Your Own AI Token",
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "NetRunner does not provide free AI tokens. Enter your own Gemini or provider API key to run AI development agents.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedTextField(
                                        value = draftToken,
                                        onValueChange = { draftToken = it },
                                        label = { Text("Your API Token") },
                                        placeholder = { Text("AIza... or sk-...") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = draftModel,
                                        onValueChange = { draftModel = it },
                                        label = { Text("Model ID") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        userApiToken = draftToken.trim()
                                        selectedModel = draftModel.trim().ifEmpty { "gemini-4-hydrogen" }
                                        prefs.edit()
                                            .putString("user_api_token", userApiToken)
                                            .putString("gemini_model", selectedModel)
                                            .apply()
                                        showTokenDialog = false
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    Text("Save Token")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTokenDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            RustDaemonBridge.trimCaches()
        }
    }
}
