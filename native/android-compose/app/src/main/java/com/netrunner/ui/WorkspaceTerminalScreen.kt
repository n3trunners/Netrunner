package com.netrunner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netrunner.core.DaemonEvent
import com.netrunner.core.RustDaemonBridge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTerminalScreen(
    project: WorkspaceProject,
    userApiToken: String,
    modelId: String,
    onOpenTokenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    var mode by remember { mutableStateOf("agent") }
    var promptInput by remember { mutableStateOf("") }
    val streamLines = remember { mutableStateListOf<String>() }
    val ptyLines = remember { mutableStateListOf<String>() }

    LaunchedEffect(project.id) {
        RustDaemonBridge.events.collect { event ->
            when (event) {
                is DaemonEvent.AgentDelta -> if (event.sessionId == project.id) {
                    streamLines.add(event.text)
                }
                is DaemonEvent.PtyOutput -> if (event.sessionId == project.id) {
                    ptyLines.add(event.chunk)
                }
                is DaemonEvent.Error -> streamLines.add("Error: ${event.message}")
                else -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(project.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$modelId · BYOK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = mode == "agent",
                    onClick = { mode = "agent" },
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                ) { Text("Agent") }
                SegmentedButton(
                    selected = mode == "pty",
                    onClick = {
                        mode = "pty"
                        RustDaemonBridge.spawnPty(
                            sessionId = project.id,
                            cwd = "/data/user/0/com.netrunner.app/files/projects/${project.id}",
                            rows = 36,
                            cols = 90,
                            useProot = true,
                        )
                    },
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                ) { Text("PTY") }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (mode == "agent" && userApiToken.isBlank()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Bring Your Own AI Token",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "NetRunner provides the development workspace without free AI credits. Add your own Gemini or provider API token to run AI coding sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onOpenTokenSettings,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Configure Token")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val activeLines = if (mode == "agent") streamLines else ptyLines
                items(activeLines) { line ->
                    Text(
                        text = line,
                        fontFamily = if (mode == "pty") FontFamily.Monospace else FontFamily.Default,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = {
                    Text(if (mode == "agent") "Message coding agent (your token)..." else "Send shell command...")
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(
                onClick = {
                    val text = promptInput.trim()
                    if (text.isNotEmpty()) {
                        if (mode == "agent") {
                            if (userApiToken.isBlank()) {
                                onOpenTokenSettings()
                            } else {
                                RustDaemonBridge.startAgentSession(
                                    sessionId = project.id,
                                    projectId = project.id,
                                    agent = "gemini_cli",
                                    prompt = text,
                                    model = modelId,
                                    apiKey = userApiToken,
                                )
                                promptInput = ""
                            }
                        } else {
                            RustDaemonBridge.writePty(project.id, "$text\n")
                            promptInput = ""
                        }
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
            }
        }
    }
}
