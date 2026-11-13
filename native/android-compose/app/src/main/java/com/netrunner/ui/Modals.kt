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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netrunner.model.GitHubUser
import com.netrunner.model.SettingsModalType
import com.netrunner.network.GitHubApiClient
import com.netrunner.ui.components.DiscordMarkIcon
import com.netrunner.ui.components.GitHubMarkIcon
import com.netrunner.ui.components.NetRunnerLogo
import com.netrunner.ui.components.NetworkAvatarImage
import com.netrunner.ui.components.rememberPressScaleInteraction
import com.netrunner.ui.theme.LocalNetRunnerTokens
import com.netrunner.ui.theme.NetRunnerEase
import kotlinx.coroutines.launch

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
    onCreateProject: (name: String, description: String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(220, easing = NetRunnerEase),
        label = "glassAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = tween(250, easing = NetRunnerEase),
        label = "glassScale",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 16f,
        animationSpec = tween(250, easing = NetRunnerEase),
        label = "glassOffsetY",
    )

    val glassShape = RoundedCornerShape(28.dp)
    val glassBg = if (tokens.isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xD9161B28),
                Color(0xE00B0E17),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xF0FFFFFF),
                Color(0xE3F3F6FA),
            ),
        )
    }
    val glassBorder = if (tokens.isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.14f),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                Color(0xFFCBD5E1).copy(alpha = 0.7f),
                Color.White,
            ),
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                    this.translationY = offsetY * density
                }
                .clip(glassShape)
                .background(glassBg)
                .border(1.dp, glassBorder, glassShape),
        ) {
            // Soft blurred ambient refraction orbs behind crisp foreground inputs
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(32.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = (-30).dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(
                            if (tokens.isDark) Color(0x3360A5FA) else Color(0x283B82F6),
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 30.dp)
                        .clip(CircleShape)
                        .background(
                            if (tokens.isDark) Color(0x2B34D399) else Color(0x2210B981),
                        ),
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Top specular glass gleam
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (tokens.isDark) 0.42f else 0.9f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (tokens.isDark) Color.White.copy(alpha = 0.09f)
                                    else Color(0xFF0F172A).copy(alpha = 0.06f),
                                )
                                .border(
                                    1.dp,
                                    if (tokens.isDark) Color.White.copy(alpha = 0.14f)
                                    else Color(0xFF0F172A).copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = tokens.textTitle,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "New Project",
                                color = tokens.textTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Initialize a workspace",
                                color = tokens.textSub,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = tokens.textSub,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Form with strictly 2 fields: Project Name & Project Description
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project Name") },
                        placeholder = { Text("e.g. neural-runner") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.textTitle.copy(alpha = 0.45f),
                            unfocusedBorderColor = if (tokens.isDark) {
                                Color.White.copy(alpha = 0.14f)
                            } else {
                                Color(0xFFCBD5E1)
                            },
                            focusedContainerColor = if (tokens.isDark) {
                                Color.White.copy(alpha = 0.05f)
                            } else {
                                Color.White.copy(alpha = 0.75f)
                            },
                            unfocusedContainerColor = if (tokens.isDark) {
                                Color.White.copy(alpha = 0.03f)
                            } else {
                                Color.White.copy(alpha = 0.55f)
                            },
                            focusedTextColor = tokens.textTitle,
                            unfocusedTextColor = tokens.textTitle,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Project Description") },
                        placeholder = { Text("Briefly describe what you are building...") },
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.textTitle.copy(alpha = 0.45f),
                            unfocusedBorderColor = if (tokens.isDark) {
                                Color.White.copy(alpha = 0.14f)
                            } else {
                                Color(0xFFCBD5E1)
                            },
                            focusedContainerColor = if (tokens.isDark) {
                                Color.White.copy(alpha = 0.05f)
                            } else {
                                Color.White.copy(alpha = 0.75f)
                            },
                            unfocusedContainerColor = if (tokens.isDark) {
                                Color.White.copy(alpha = 0.03f)
                            } else {
                                Color.White.copy(alpha = 0.55f)
                            },
                            focusedTextColor = tokens.textTitle,
                            unfocusedTextColor = tokens.textTitle,
                        ),
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        val enabled = name.trim().isNotEmpty()
                        val (createInteraction, createScale) = rememberPressScaleInteraction(0.96f)
                        Surface(
                            onClick = {
                                if (enabled) {
                                    onCreateProject(name.trim(), description.trim())
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
                            shadowElevation = if (enabled) 8.dp else 0.dp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = createScale
                                scaleY = createScale
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Create Project",
                                    color = if (enabled) {
                                        if (tokens.isDark) Color.Black else Color.White
                                    } else tokens.textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = if (enabled) {
                                        if (tokens.isDark) Color.Black else Color.White
                                    } else tokens.textMuted,
                                    modifier = Modifier.size(15.dp),
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
    selectedLanguage: String,
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onOpenGitHubAuth: () -> Unit,
    onSignOut: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    val context = LocalContext.current

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
                                        Text(
                                            text = currentUser.name ?: currentUser.login,
                                            color = tokens.textTitle,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "@${currentUser.login}",
                                            color = tokens.textSub,
                                            fontSize = 12.sp,
                                        )
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
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) tokens.textTitle.copy(alpha = 0.3f) else tokens.cardBorder,
                                    ),
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
                            InfoRow("AI Token Policy", "Gemini API (Device Local)")
                            InfoRow("Vault Cipher", "AES-256-GCM + Zeroize")
                        }

                        SettingsModalType.ABOUT -> {
                            val (discordInteraction, discordScale) = rememberPressScaleInteraction(0.92f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = tokens.cardBg,
                                    border = BorderStroke(1.dp, tokens.cardBorder),
                                    modifier = Modifier.size(68.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        NetRunnerLogo(size = 38.dp)
                                    }
                                }

                                Text(
                                    text = "NETRUNNER",
                                    color = tokens.textTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 3.sp,
                                )

                                Surface(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://discord.gg/YKCdUCdGE"),
                                                ),
                                            )
                                        }.onFailure {
                                            onShowToast("Could not open Discord link")
                                        }
                                    },
                                    interactionSource = discordInteraction,
                                    shape = CircleShape,
                                    color = Color(0xFF5865F2).copy(alpha = if (tokens.isDark) 0.16f else 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF5865F2).copy(alpha = 0.38f)),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer {
                                            scaleX = discordScale
                                            scaleY = discordScale
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        DiscordMarkIcon(
                                            size = 22.dp,
                                            tint = if (tokens.isDark) Color(0xFF7289DA) else Color(0xFF5865F2),
                                            eyeTint = if (tokens.isDark) Color(0xFF0E1320) else Color.White,
                                        )
                                    }
                                }
                            }
                        }

                        else -> Unit
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
