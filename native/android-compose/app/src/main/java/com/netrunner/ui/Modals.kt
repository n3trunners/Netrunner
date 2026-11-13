package com.netrunner.ui

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netrunner.model.GitHubUser
import com.netrunner.model.SettingsModalType
import com.netrunner.model.UserCustomIntegration
import com.netrunner.network.GitHubApiClient
import com.netrunner.ui.components.GitHubMarkIcon
import com.netrunner.ui.components.NetRunnerLogo
import com.netrunner.ui.components.NetworkAvatarImage
import com.netrunner.ui.components.rememberPressScaleInteraction
import com.netrunner.ui.theme.LocalNetRunnerTokens
import com.netrunner.ui.theme.NetRunnerEase
import kotlinx.coroutines.launch

private fun maskApiKey(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.length <= 8) return "••••••••"
    return "${trimmed.take(4)}••••••••${trimmed.takeLast(4)}"
}

@Composable
private fun SmoothDialogCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(220, easing = NetRunnerEase),
        label = "dialogAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = tween(240, easing = NetRunnerEase),
        label = "dialogScale",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 14f,
        animationSpec = tween(240, easing = NetRunnerEase),
        label = "dialogOffsetY",
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = tokens.modalBg,
        border = BorderStroke(1.dp, tokens.modalBorder),
        shadowElevation = 24.dp,
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = offsetY * density
            }
            .animateContentSize(spring(stiffness = 380f, dampingRatio = 0.82f)),
    ) {
        content()
    }
}

@Composable
fun GitHubAuthDialog(
    onDismiss: () -> Unit,
    isAuthorizing: Boolean = false,
    externalError: String? = null,
    onClearExternalError: () -> Unit = {},
) {
    val tokens = LocalNetRunnerTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isConnecting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayedError = externalError ?: localError
    val busy = isConnecting || isAuthorizing

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SmoothDialogCard(
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
                        .clickable(enabled = !busy) { onDismiss() },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Authorize with your GitHub account to access your workspace and sync repositories.",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                    }

                    AnimatedVisibility(
                        visible = displayedError != null,
                        enter = expandVertically(tween(220, easing = NetRunnerEase)) + fadeIn(tween(180)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
                    ) {
                        if (displayedError != null) {
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
                                        text = displayedError,
                                        color = Color(0xFFFBBF24),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                    )
                                }
                            }
                        }
                    }

                    val (btnInteraction, btnScale) = rememberPressScaleInteraction(0.97f)
                    Surface(
                        onClick = {
                            if (!busy) {
                                isConnecting = true
                                localError = null
                                onClearExternalError()
                                scope.launch {
                                    val result = GitHubApiClient.resolveAuthorizeUrl()
                                    isConnecting = false
                                    result.onSuccess { authUrl ->
                                        val launched = runCatching {
                                            val customTabs = CustomTabsIntent.Builder()
                                                .setShowTitle(true)
                                                .build()
                                            customTabs.launchUrl(context, Uri.parse(authUrl))
                                        }.isSuccess
                                        if (!launched) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                                            }.onFailure {
                                                localError = "Unable to open browser for GitHub authorization."
                                            }
                                        }
                                    }.onFailure { err ->
                                        localError = err.message ?: "Unable to start GitHub OAuth sign-in."
                                    }
                                }
                            }
                        },
                        interactionSource = btnInteraction,
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White else Color(0xFF171717),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = btnScale
                                scaleY = btnScale
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = if (tokens.isDark) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isAuthorizing) "Completing sign in..." else "Opening GitHub...",
                                    color = if (tokens.isDark) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else {
                                GitHubMarkIcon(
                                    size = 17.dp,
                                    tint = if (tokens.isDark) Color.Black else Color.White,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Continue with GitHub",
                                    color = if (tokens.isDark) Color.Black else Color.White,
                                    fontSize = 13.sp,
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
        SmoothDialogCard(
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
                            .clip(CircleShape)
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
                                    val (stackInteraction, stackScale) = rememberPressScaleInteraction(0.96f)
                                    Surface(
                                        onClick = { stack = id },
                                        interactionSource = stackInteraction,
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            selected && tokens.isDark -> Color.White
                                            selected && !tokens.isDark -> Color(0xFF171717)
                                            tokens.isDark -> Color.White.copy(alpha = 0.04f)
                                            else -> Color(0xFFF3F4F6)
                                        },
                                        border = BorderStroke(1.dp, if (selected) Color.Transparent else tokens.cardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer {
                                                scaleX = stackScale
                                                scaleY = stackScale
                                            },
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
                        val (createInteraction, createScale) = rememberPressScaleInteraction(0.96f)
                        Surface(
                            onClick = {
                                if (enabled) {
                                    onCreateProject(name.trim(), description.trim(), stack)
                                    onDismiss()
                                }
                            },
                            interactionSource = createInteraction,
                            shape = CircleShape,
                            color = if (enabled) {
                                if (tokens.isDark) Color.White else Color(0xFF171717)
                            } else {
                                tokens.textMuted.copy(alpha = 0.25f)
                            },
                            modifier = Modifier.graphicsLayer {
                                scaleX = createScale
                                scaleY = createScale
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
    val context = LocalContext.current

    // Concise Models & API card state
    var draftToken by remember(modalType, userApiToken) { mutableStateOf(userApiToken) }
    var showToken by remember { mutableStateOf(false) }
    var showProviderMenu by remember(modalType) { mutableStateOf(false) }
    var googleStudioSelected by remember(modalType) { mutableStateOf(false) }

    var newName by remember(modalType) { mutableStateOf("") }
    var newDetail by remember(modalType) { mutableStateOf("") }
    var buildAlerts by remember { mutableStateOf(true) }
    var securityNotices by remember { mutableStateOf(true) }
    var commitSync by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SmoothDialogCard(
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
                            .clip(CircleShape)
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
                            val hasSavedToken = userApiToken.isNotBlank()
                            val plusRotation by animateFloatAsState(
                                targetValue = if (showProviderMenu) 45f else 0f,
                                animationSpec = spring(stiffness = 450f, dampingRatio = 0.72f),
                                label = "modelsPlusRotate",
                            )
                            val (plusInteraction, plusScale) = rememberPressScaleInteraction(0.9f)

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = tokens.cardBg,
                                border = BorderStroke(1.dp, tokens.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(spring(stiffness = 380f, dampingRatio = 0.82f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Concise Header: Active Key Config + Top-Right + Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (hasSavedToken) Color(0xFF10B981).copy(alpha = 0.14f)
                                                        else if (tokens.isDark) Color.White.copy(alpha = 0.08f)
                                                        else Color(0xFF171717),
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (hasSavedToken) Color(0xFF34D399) else Color.White,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Text(
                                                        text = if (hasSavedToken) "Google AI Studio" else "No Active Key",
                                                        color = tokens.textTitle,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (hasSavedToken) {
                                                            Color(0xFF10B981).copy(alpha = 0.14f)
                                                        } else if (tokens.isDark) {
                                                            Color.White.copy(alpha = 0.06f)
                                                        } else {
                                                            Color(0xFFE5E7EB)
                                                        },
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (hasSavedToken) Color(0xFF10B981).copy(alpha = 0.30f)
                                                            else tokens.cardBorder,
                                                        ),
                                                    ) {
                                                        Text(
                                                            text = if (hasSavedToken) "Active" else "Empty",
                                                            color = if (hasSavedToken) Color(0xFF34D399) else tokens.textMuted,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (hasSavedToken) {
                                                        "${maskApiKey(userApiToken)} · $selectedModel"
                                                    } else {
                                                        "Tap + to add a Google AI Studio key"
                                                    },
                                                    color = tokens.textSub,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }

                                        // Top-right Plus icon button
                                        Surface(
                                            onClick = {
                                                val next = !showProviderMenu
                                                showProviderMenu = next
                                                if (!next) {
                                                    googleStudioSelected = false
                                                }
                                            },
                                            interactionSource = plusInteraction,
                                            shape = CircleShape,
                                            color = if (showProviderMenu) {
                                                if (tokens.isDark) Color.White else Color(0xFF171717)
                                            } else if (tokens.isDark) {
                                                Color.White.copy(alpha = 0.08f)
                                            } else {
                                                Color.White
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (showProviderMenu) Color.Transparent else tokens.cardBorder,
                                            ),
                                            modifier = Modifier
                                                .size(34.dp)
                                                .graphicsLayer {
                                                    scaleX = plusScale
                                                    scaleY = plusScale
                                                },
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = "Add API Key",
                                                    tint = if (showProviderMenu) {
                                                        if (tokens.isDark) Color.Black else Color.White
                                                    } else tokens.textTitle,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .rotate(plusRotation),
                                                )
                                            }
                                        }
                                    }

                                    // Present key footer when active and add menu collapsed
                                    AnimatedVisibility(
                                        visible = hasSavedToken && !showProviderMenu,
                                        enter = fadeIn(tween(200)) + expandVertically(tween(220, easing = NetRunnerEase)),
                                        exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            HorizontalDivider(color = tokens.divider)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = "Stored locally on this device",
                                                    color = tokens.textMuted,
                                                    fontSize = 11.sp,
                                                )
                                                Text(
                                                    text = "Remove",
                                                    color = Color(0xFFF87171),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            draftToken = ""
                                                            onClearToken()
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                )
                                            }
                                        }
                                    }

                                    // Step 1: Smoothly reveal single allowed provider: Google AI Studio
                                    AnimatedVisibility(
                                        visible = showProviderMenu,
                                        enter = expandVertically(
                                            animationSpec = tween(280, easing = NetRunnerEase),
                                        ) + fadeIn(tween(220)),
                                        exit = shrinkVertically(
                                            animationSpec = tween(220, easing = NetRunnerEase),
                                        ) + fadeOut(tween(160)),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            HorizontalDivider(color = tokens.divider)
                                            Text(
                                                text = "Select Provider",
                                                color = tokens.textSub,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                            )

                                            val (studioInteraction, studioScale) = rememberPressScaleInteraction(0.97f)
                                            Surface(
                                                onClick = { googleStudioSelected = true },
                                                interactionSource = studioInteraction,
                                                shape = RoundedCornerShape(14.dp),
                                                color = when {
                                                    googleStudioSelected && tokens.isDark -> Color.White
                                                    googleStudioSelected && !tokens.isDark -> Color(0xFF171717)
                                                    tokens.isDark -> Color.White.copy(alpha = 0.05f)
                                                    else -> Color.White
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (googleStudioSelected) Color.Transparent else tokens.cardBorder,
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        scaleX = studioScale
                                                        scaleY = studioScale
                                                    },
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.AutoAwesome,
                                                            contentDescription = null,
                                                            tint = when {
                                                                googleStudioSelected && tokens.isDark -> Color.Black
                                                                googleStudioSelected && !tokens.isDark -> Color.White
                                                                else -> tokens.textTitle
                                                            },
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                        Text(
                                                            text = "Google AI Studio",
                                                            color = when {
                                                                googleStudioSelected && tokens.isDark -> Color.Black
                                                                googleStudioSelected && !tokens.isDark -> Color.White
                                                                else -> tokens.textTitle
                                                            },
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                    Text(
                                                        text = "Gemini API",
                                                        color = when {
                                                            googleStudioSelected && tokens.isDark -> Color.Black.copy(alpha = 0.7f)
                                                            googleStudioSelected && !tokens.isDark -> Color.White.copy(alpha = 0.8f)
                                                            else -> tokens.textMuted
                                                        },
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                    )
                                                }
                                            }

                                            // Step 2: Butter-smooth key entry pop-out when Google AI Studio is tapped
                                            AnimatedVisibility(
                                                visible = googleStudioSelected,
                                                enter = expandVertically(
                                                    animationSpec = tween(300, easing = NetRunnerEase),
                                                ) + fadeIn(tween(240)),
                                                exit = shrinkVertically(
                                                    animationSpec = tween(220, easing = NetRunnerEase),
                                                ) + fadeOut(tween(150)),
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(top = 4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(
                                                            text = "Enter Google AI Studio Key",
                                                            color = tokens.textSub,
                                                            fontSize = 11.sp,
                                                        )
                                                        Row(
                                                            modifier = Modifier.clickable {
                                                                runCatching {
                                                                    context.startActivity(
                                                                        Intent(
                                                                            Intent.ACTION_VIEW,
                                                                            Uri.parse("https://aistudio.google.com/apikey"),
                                                                        ),
                                                                    )
                                                                }
                                                            },
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        ) {
                                                            Text("Get key", color = tokens.textSub, fontSize = 11.sp)
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                                contentDescription = null,
                                                                tint = tokens.textSub,
                                                                modifier = Modifier.size(11.dp),
                                                            )
                                                        }
                                                    }

                                                    OutlinedTextField(
                                                        value = draftToken,
                                                        onValueChange = { draftToken = it },
                                                        placeholder = { Text("AIza...") },
                                                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                                                        trailingIcon = {
                                                            Icon(
                                                                imageVector = if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                                contentDescription = "Toggle key visibility",
                                                                modifier = Modifier.clickable { showToken = !showToken },
                                                            )
                                                        },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(14.dp),
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
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(
                                                            text = "Cancel",
                                                            color = tokens.textSub,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .clickable {
                                                                    showProviderMenu = false
                                                                    googleStudioSelected = false
                                                                }
                                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        val canSave = draftToken.trim().isNotEmpty()
                                                        val (saveInteraction, saveScale) = rememberPressScaleInteraction(0.96f)
                                                        Surface(
                                                            onClick = {
                                                                if (canSave) {
                                                                    onSaveToken(
                                                                        draftToken.trim(),
                                                                        selectedModel.ifBlank { "gemini-4-hydrogen" },
                                                                    )
                                                                    showProviderMenu = false
                                                                    googleStudioSelected = false
                                                                }
                                                            },
                                                            interactionSource = saveInteraction,
                                                            shape = CircleShape,
                                                            color = if (canSave) {
                                                                if (tokens.isDark) Color.White else Color(0xFF171717)
                                                            } else {
                                                                tokens.textMuted.copy(alpha = 0.25f)
                                                            },
                                                            modifier = Modifier.graphicsLayer {
                                                                scaleX = saveScale
                                                                scaleY = saveScale
                                                            },
                                                        ) {
                                                            Text(
                                                                text = "Save",
                                                                color = if (canSave) {
                                                                    if (tokens.isDark) Color.Black else Color.White
                                                                } else tokens.textMuted,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
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
                            InfoRow("Auth Provider", "GitHub OAuth")
                            InfoRow("AI Token Policy", "Google AI Studio (Device Local)")
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
                                Text("Version 1.0.0 • Stable Release", color = tokens.textSub, fontSize = 12.sp)
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
                    val (doneInteraction, doneScale) = rememberPressScaleInteraction(0.95f)
                    Surface(
                        onClick = onDismiss,
                        interactionSource = doneInteraction,
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White else Color(0xFF171717),
                        modifier = Modifier.graphicsLayer {
                            scaleX = doneScale
                            scaleY = doneScale
                        },
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
