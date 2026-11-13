package com.netrunner.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netrunner.model.GitHubUser
import com.netrunner.model.SettingsModalType
import com.netrunner.model.UserCustomIntegration
import com.netrunner.network.GitHubApiClient
import com.netrunner.network.GitHubAuthResult
import com.netrunner.ui.components.GitHubMarkIcon
import com.netrunner.ui.components.NetRunnerLogo
import com.netrunner.ui.components.NetworkAvatarImage
import com.netrunner.ui.theme.LocalNetRunnerTokens
import kotlinx.coroutines.launch

@Composable
fun GitHubAuthDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (GitHubAuthResult) -> Unit,
    onShowToast: (String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var credentialInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.modalBg,
            border = BorderStroke(1.dp, tokens.modalBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = tokens.textSub,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFF171717),
                        border = BorderStroke(
                            1.dp,
                            if (tokens.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF262626),
                        ),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            GitHubMarkIcon(size = 28.dp, tint = Color.White)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sign in to NetRunner",
                            color = tokens.textTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sign in with your GitHub account to access your workspace and sync repositories.",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                    }

                    OutlinedTextField(
                        value = credentialInput,
                        onValueChange = {
                            credentialInput = it
                            errorMessage = null
                        },
                        label = { Text("GitHub @username or Personal Token") },
                        placeholder = { Text("username or ghp_...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.textTitle.copy(alpha = 0.45f),
                            unfocusedBorderColor = tokens.cardBorder,
                            focusedTextColor = tokens.textTitle,
                            unfocusedTextColor = tokens.textTitle,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Row(
                            modifier = Modifier.clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/settings/tokens/new?scopes=read:user,repo&description=NetRunner%20Android"),
                                        ),
                                    )
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Create GitHub Token",
                                color = tokens.textSub,
                                fontSize = 11.sp,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = tokens.textSub,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = GitHubApiClient.authenticate(credentialInput)
                                    isLoading = false
                                    result.onSuccess { authRes ->
                                        onLoginSuccess(authRes)
                                        onShowToast("Signed in as @${authRes.user.login}! Live stats synchronized.")
                                        onDismiss()
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Authentication failed."
                                    }
                                }
                            }
                        },
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White else Color(0xFF171717),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = if (tokens.isDark) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Connecting with GitHub...",
                                    color = if (tokens.isDark) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else {
                                GitHubMarkIcon(
                                    size = 16.dp,
                                    tint = if (tokens.isDark) Color.Black else Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Continue with GitHub",
                                    color = if (tokens.isDark) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (name: String, description: String, category: String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stack by remember { mutableStateOf("Hybrid") }

    val stacks = listOf(
        "Hybrid" to "Hybrid (Kotlin + Rust)",
        "Fullstack" to "Fullstack Web",
        "AI Agent" to "AI Agent",
        "API" to "API Service",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.modalBg,
            border = BorderStroke(1.dp, tokens.modalBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (tokens.isDark) Color.White.copy(alpha = 0.03f) else Color(0xFFF9FAFB))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (tokens.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E7EB)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = tokens.textTitle, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("New Project", color = tokens.textTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Initialize a clean workspace", color = tokens.textSub, fontSize = 11.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = tokens.textSub,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() },
                    )
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Architecture Stack",
                        color = tokens.textSub,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stacks.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                pair.forEach { (id, label) ->
                                    val selected = stack == id
                                    Surface(
                                        onClick = { stack = id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            selected && tokens.isDark -> Color.White
                                            selected && !tokens.isDark -> Color(0xFF171717)
                                            tokens.isDark -> Color.White.copy(alpha = 0.04f)
                                            else -> Color(0xFFF3F4F6)
                                        },
                                        border = BorderStroke(1.dp, if (selected) Color.Transparent else tokens.cardBorder),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = label,
                                            color = when {
                                                selected && tokens.isDark -> Color.Black
                                                selected && !tokens.isDark -> Color.White
                                                else -> tokens.textSub
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project Name") },
                        placeholder = {
                            Text(if (stack == "Hybrid") "e.g. netrunner-android-daemon" else "e.g. neural-runner")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("What are you building?") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Cancel",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onDismiss() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        val enabled = name.trim().isNotEmpty()
                        Surface(
                            onClick = {
                                if (enabled) {
                                    onCreateProject(name.trim(), description.trim(), stack)
                                    onDismiss()
                                }
                            },
                            shape = CircleShape,
                            color = if (enabled) {
                                if (tokens.isDark) Color.White else Color(0xFF171717)
                            } else {
                                tokens.textMuted.copy(alpha = 0.25f)
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Create Project",
                                    color = if (enabled) {
                                        if (tokens.isDark) Color.Black else Color.White
                                    } else tokens.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = if (enabled) {
                                        if (tokens.isDark) Color.Black else Color.White
                                    } else tokens.textMuted,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDetailDialog(
    modalType: SettingsModalType,
    currentUser: GitHubUser?,
    userApiToken: String,
    selectedModel: String,
    selectedLanguage: String,
    connectors: List<UserCustomIntegration>,
    agents: List<UserCustomIntegration>,
    skills: List<UserCustomIntegration>,
    onDismiss: () -> Unit,
    onSaveToken: (token: String, model: String) -> Unit,
    onClearToken: () -> Unit,
    onAddIntegration: (SettingsModalType, String, String) -> Unit,
    onDeleteIntegration: (SettingsModalType, String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onOpenGitHubAuth: () -> Unit,
    onSignOut: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    var draftToken by remember(modalType) { mutableStateOf(userApiToken) }
    var draftModel by remember(modalType) { mutableStateOf(selectedModel) }
    var showToken by remember { mutableStateOf(false) }

    var newName by remember(modalType) { mutableStateOf("") }
    var newDetail by remember(modalType) { mutableStateOf("") }
    var buildAlerts by remember { mutableStateOf(true) }
    var securityNotices by remember { mutableStateOf(true) }
    var commitSync by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.modalBg,
            border = BorderStroke(1.dp, tokens.modalBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .padding(horizontal = 18.dp),
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (tokens.isDark) Color.White.copy(alpha = 0.02f) else Color(0xFFF9FAFB))
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NetRunnerLogo(size = 20.dp)
                        Text(
                            text = modalType.title,
                            color = tokens.textTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = tokens.textSub,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() },
                    )
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (modalType) {
                        SettingsModalType.MODELS -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = tokens.cardBg,
                                border = BorderStroke(1.dp, tokens.cardBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Rounded.Shield, null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "NetRunner does not supply free AI tokens. Your personal API key stays locally on your device.",
                                        color = tokens.textSub,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = draftToken,
                                onValueChange = { draftToken = it },
                                label = { Text("Your API Token") },
                                placeholder = { Text("AIza... or sk-...") },
                                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "Toggle visibility",
                                        modifier = Modifier.clickable { showToken = !showToken },
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = draftModel,
                                onValueChange = { draftModel = it },
                                label = { Text("Model ID") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (userApiToken.isNotBlank()) {
                                    Text(
                                        text = "Clear Token",
                                        color = Color(0xFFF87171),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                draftToken = ""
                                                onClearToken()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    )
                                } else Spacer(Modifier.width(1.dp))

                                Surface(
                                    onClick = {
                                        onSaveToken(draftToken.trim(), draftModel.trim().ifEmpty { "gemini-4-hydrogen" })
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                    color = if (tokens.isDark) Color.White else Color(0xFF171717),
                                ) {
                                    Text(
                                        text = "Save Token",
                                        color = if (tokens.isDark) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                    )
                                }
                            }
                        }

                        SettingsModalType.CONNECTORS,
                        SettingsModalType.AGENTS,
                        SettingsModalType.SKILLS -> {
                            val list = when (modalType) {
                                SettingsModalType.CONNECTORS -> connectors
                                SettingsModalType.AGENTS -> agents
                                else -> skills
                            }
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = newDetail,
                                onValueChange = { newDetail = it },
                                label = { Text("Type / Objective") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Surface(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        onAddIntegration(modalType, newName.trim(), newDetail.trim().ifEmpty { "Custom" })
                                        newName = ""
                                        newDetail = ""
                                    }
                                },
                                shape = CircleShape,
                                color = if (tokens.isDark) Color.White else Color(0xFF171717),
                            ) {
                                Text(
                                    text = "Add ${modalType.title.dropLast(1)}",
                                    color = if (tokens.isDark) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }

                            if (list.isEmpty()) {
                                Text(
                                    text = "No ${modalType.title.lowercase()} configured yet.",
                                    color = tokens.textSub,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            } else {
                                list.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = tokens.cardBg,
                                        border = BorderStroke(1.dp, tokens.cardBorder),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column {
                                                Text(item.name, color = tokens.textTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text(item.detail, color = tokens.textSub, fontSize = 11.sp)
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable { onDeleteIntegration(modalType, item.id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsModalType.PROFILE_DETAIL -> {
                            if (currentUser != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    NetworkAvatarImage(
                                        url = currentUser.avatarUrl,
                                        contentDescription = currentUser.login,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape),
                                    )
                                    Column {
                                        Text(currentUser.name ?: currentUser.login, color = tokens.textTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("@${currentUser.login}", color = tokens.textSub, fontSize = 12.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        onClick = { onDismiss(); onSignOut() },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text("Sign Out", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Surface(
                                        onClick = { onDismiss(); onOpenGitHubAuth() },
                                        shape = RoundedCornerShape(12.dp),
                                        color = tokens.cardBg,
                                        border = BorderStroke(1.dp, tokens.cardBorder),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text("Switch Account", color = tokens.textTitle, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }

                        SettingsModalType.LANGUAGE -> {
                            val locales = listOf("English (US)", "English (UK)", "日本語", "Deutsch", "Español")
                            locales.forEach { lang ->
                                val selected = lang == selectedLanguage
                                Surface(
                                    onClick = {
                                        onSelectLanguage(lang)
                                        onShowToast("Locale set to $lang")
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) tokens.pillBg else tokens.cardBg,
                                    border = BorderStroke(1.dp, if (selected) tokens.textTitle.copy(alpha = 0.3f) else tokens.cardBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = lang,
                                            color = if (selected && !tokens.isDark) Color.White else tokens.textTitle,
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                        if (selected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                null,
                                                tint = if (!tokens.isDark) Color.White else tokens.textTitle,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsModalType.NOTIFICATIONS -> {
                            ToggleRow("Build & Compilation Alerts", buildAlerts) { buildAlerts = it }
                            ToggleRow("Security & Dependency Advisories", securityNotices) { securityNotices = it }
                            ToggleRow("GitHub Activity Sync", commitSync) { commitSync = it }
                        }

                        SettingsModalType.EDITOR -> {
                            InfoRow("Font Family", "JetBrains Mono")
                            InfoRow("Tab Size", "2 Spaces")
                        }

                        SettingsModalType.DATA_STORAGE -> {
                            InfoRow("Local Workspace Storage", "Active (Encrypted)")
                            InfoRow("Rust Daemon Rootfs", "ARM64 PRoot Ready")
                        }

                        SettingsModalType.SECURITY -> {
                            InfoRow("Auth Provider", "GitHub Account Sync")
                            InfoRow("AI Token Policy", "BYOK (Device Local Only)")
                            InfoRow("Vault Cipher", "AES-256-GCM + Zeroize")
                        }

                        SettingsModalType.ABOUT -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = tokens.cardBg,
                                    border = BorderStroke(1.dp, tokens.cardBorder),
                                    modifier = Modifier.size(64.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        NetRunnerLogo(size = 36.dp)
                                    }
                                }
                                Text("NETRUNNER", color = tokens.textTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Text("Version 1.0.0 • Production Build", color = tokens.textSub, fontSize = 12.sp)
                                InfoRow("Android UI Layer", "Kotlin + Jetpack Compose")
                                InfoRow("Core Engine / Daemon", "Rust (Tokio + PRoot + JNI)")
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (tokens.isDark) Color.White.copy(alpha = 0.02f) else Color(0xFFF9FAFB))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White else Color(0xFF171717),
                    ) {
                        Text(
                            text = "Done",
                            color = if (tokens.isDark) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HybridArchitectureDialog(
    onDismiss: () -> Unit,
    onCreateHybridProject: () -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.modalBg,
            border = BorderStroke(1.dp, tokens.modalBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Layers, null, tint = tokens.textTitle, modifier = Modifier.size(18.dp))
                        Text("Hybrid Architecture & CI/CD", color = tokens.textTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = tokens.textSub,
                        modifier = Modifier.size(20.dp).clickable { onDismiss() },
                    )
                }

                InfoRow("Android UI Layer", "Kotlin 2 · Jetpack Compose")
                InfoRow("IPC Boundary", "JNI C-ABI · LocalSocket UDS")
                InfoRow("Core Daemon", "Rust · Tokio · nix PTY · AES-GCM")
                InfoRow("ELF Alignment", "16 KB (Android 15+ Ready)")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = {
                            onCreateHybridProject()
                            onDismiss()
                        },
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White else Color(0xFF171717),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Create Hybrid Workspace",
                                color = if (tokens.isDark) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tokens.cardBg,
        border = BorderStroke(1.dp, tokens.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = tokens.textTitle, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val tokens = LocalNetRunnerTokens.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tokens.cardBg,
        border = BorderStroke(1.dp, tokens.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = tokens.textSub, fontSize = 12.sp)
            Text(value, color = tokens.textTitle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
