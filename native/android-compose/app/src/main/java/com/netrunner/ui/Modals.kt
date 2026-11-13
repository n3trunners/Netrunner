package com.netrunner.ui

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrunner.model.GitHubUser
import com.netrunner.model.SettingsModalType
import com.netrunner.network.GitHubApiClient
import com.netrunner.ui.components.DiscordMarkIcon
import com.netrunner.ui.components.GitHubMarkIcon
import com.netrunner.ui.components.NetRunnerLogo
import com.netrunner.ui.components.NetworkAvatarImage
import com.netrunner.ui.components.XMarkIcon
import com.netrunner.ui.components.rememberPressScaleInteraction
import com.netrunner.ui.theme.LocalNetRunnerTokens
import com.netrunner.ui.theme.NetRunnerEase
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun GlassOverlayContainer(
    visible: Boolean,
    onDismiss: () -> Unit,
    dismissEnabled: Boolean = true,
    cardModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    val glassShape = RoundedCornerShape(28.dp)
    val glassBg = if (tokens.isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0x88141926),
                Color(0x9C0A0D16),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xC2FFFFFF),
                Color(0xB3F1F5F9),
            ),
        )
    }
    val glassBorder = if (tokens.isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.16f),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                Color(0xFFCBD5E1).copy(alpha = 0.65f),
                Color.White,
            ),
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200, easing = NetRunnerEase)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (tokens.isDark) Color.Black.copy(alpha = 0.28f) else Color(0x280F172A),
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (dismissEnabled) onDismiss() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = cardModifier
                    .animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(240, easing = NetRunnerEase),
                        ),
                        exit = scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(150),
                        ),
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(glassShape)
                    .background(glassBg)
                    .border(1.dp, glassBorder, glassShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .animateContentSize(spring(stiffness = 380f, dampingRatio = 0.82f)),
            ) {
                // Soft ambient refraction glow inside the translucent glass
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(36.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .offset(x = (-32).dp, y = (-32).dp)
                            .clip(CircleShape)
                            .background(
                                if (tokens.isDark) Color(0x2660A5FA) else Color(0x1E3B82F6),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 32.dp, y = 32.dp)
                            .clip(CircleShape)
                            .background(
                                if (tokens.isDark) Color(0x2234D399) else Color(0x1A10B981),
                            ),
                    )
                }

                // Top specular glass gleam
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (tokens.isDark) 0.45f else 0.9f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                content()
            }
        }
    }
}

@Composable
fun GitHubAuthOverlay(
    visible: Boolean,
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

    GlassOverlayContainer(
        visible = visible,
        onDismiss = onDismiss,
        dismissEnabled = !busy,
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
                    color = if (tokens.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF171717),
                    border = BorderStroke(
                        1.dp,
                        if (tokens.isDark) Color.White.copy(alpha = 0.16f) else Color(0xFF262626),
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
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
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

@Composable
fun NewProjectOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreateProject: (name: String, description: String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    var name by remember(visible) { mutableStateOf("") }
    var description by remember(visible) { mutableStateOf("") }

    GlassOverlayContainer(
        visible = visible,
        onDismiss = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

            // Strictly 2 fields: Project Name & Project Description
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
                            Color.White.copy(alpha = 0.06f)
                        } else {
                            Color.White.copy(alpha = 0.72f)
                        },
                        unfocusedContainerColor = if (tokens.isDark) {
                            Color.White.copy(alpha = 0.03f)
                        } else {
                            Color.White.copy(alpha = 0.52f)
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
                            Color.White.copy(alpha = 0.06f)
                        } else {
                            Color.White.copy(alpha = 0.72f)
                        },
                        unfocusedContainerColor = if (tokens.isDark) {
                            Color.White.copy(alpha = 0.03f)
                        } else {
                            Color.White.copy(alpha = 0.52f)
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

@Composable
fun ConfirmDeleteOverlay(
    visible: Boolean,
    title: String,
    description: String,
    confirmLabel: String = "Delete",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    GlassOverlayContainer(
        visible = visible,
        onDismiss = onCancel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.14f))
                            .border(
                                1.dp,
                                Color(0xFFEF4444).copy(alpha = 0.28f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            color = tokens.textTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = description,
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        tint = tokens.textSub,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onCancel,
                    shape = CircleShape,
                    color = if (tokens.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        1.dp,
                        if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0),
                    ),
                ) {
                    Text(
                        text = "Cancel",
                        color = tokens.textTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                val (confirmInteraction, confirmScale) = rememberPressScaleInteraction(0.96f)
                Surface(
                    onClick = onConfirm,
                    interactionSource = confirmInteraction,
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    shadowElevation = 6.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = confirmScale
                        scaleY = confirmScale
                    },
                ) {
                    Text(
                        text = confirmLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDetailOverlay(
    modalType: SettingsModalType?,
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

    var lastModalType by remember { mutableStateOf(modalType) }
    if (modalType != null) {
        lastModalType = modalType
    }
    val displayModalType = modalType ?: lastModalType

    var buildAlerts by remember { mutableStateOf(true) }
    var securityNotices by remember { mutableStateOf(true) }
    var commitSync by remember { mutableStateOf(true) }

    GlassOverlayContainer(
        visible = modalType != null && displayModalType != null,
        onDismiss = onDismiss,
        cardModifier = Modifier.heightIn(max = 620.dp),
    ) {
        if (displayModalType != null) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (tokens.isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.35f),
                        )
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
                            text = displayModalType.title,
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
                    when (displayModalType) {
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
                                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
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
                                        color = if (tokens.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.65f),
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
                                    color = if (selected) {
                                        tokens.pillBg
                                    } else if (tokens.isDark) {
                                        Color.White.copy(alpha = 0.05f)
                                    } else {
                                        Color.White.copy(alpha = 0.6f)
                                    },
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

                        SettingsModalType.EDITOR,
                        SettingsModalType.DATA_STORAGE,
                        SettingsModalType.SECURITY,
                        -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (tokens.isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.72f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (tokens.isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFCBD5E1),
                                    ),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = tokens.textSub,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = "Currently Unavailable",
                                    color = tokens.textTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${displayModalType.title} is currently unavailable in this release.",
                                    color = tokens.textSub,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 17.sp,
                                )
                            }
                        }

                        SettingsModalType.ABOUT -> {
                            val (discordInteraction, discordScale) = rememberPressScaleInteraction(0.92f)
                            val (xInteraction, xScale) = rememberPressScaleInteraction(0.92f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (tokens.isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.72f),
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

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
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
                                                size = 21.dp,
                                                tint = if (tokens.isDark) Color(0xFF7289DA) else Color(0xFF5865F2),
                                                eyeTint = if (tokens.isDark) Color(0xFF0E1320) else Color.White,
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://x.com/Netrunnerapp"),
                                                    ),
                                                )
                                            }.onFailure {
                                                onShowToast("Could not open X link")
                                            }
                                        },
                                        interactionSource = xInteraction,
                                        shape = CircleShape,
                                        color = if (tokens.isDark) {
                                            Color.White.copy(alpha = 0.08f)
                                        } else {
                                            Color(0xFF0F172A).copy(alpha = 0.06f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (tokens.isDark) Color.White.copy(alpha = 0.18f) else Color(0xFFCBD5E1),
                                        ),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .graphicsLayer {
                                                scaleX = xScale
                                                scaleY = xScale
                                            },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            XMarkIcon(
                                                size = 18.dp,
                                                tint = tokens.textTitle,
                                            )
                                        }
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
                        .background(
                            if (tokens.isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.35f),
                        )
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
        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.6f),
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
