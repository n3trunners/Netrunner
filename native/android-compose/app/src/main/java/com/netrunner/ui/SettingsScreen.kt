package com.netrunner.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrunner.model.GitHubUser
import com.netrunner.model.SettingsModalType
import com.netrunner.ui.components.GitHubMarkIcon
import com.netrunner.ui.components.NetworkAvatarImage
import com.netrunner.ui.theme.LocalNetRunnerTokens

@Composable
fun SettingsScreen(
    currentUser: GitHubUser?,
    selectedLanguage: String,
    onBack: () -> Unit,
    onOpenModal: (SettingsModalType) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenGitHubAuth: () -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    val thumbOffset by animateDpAsState(
        targetValue = if (tokens.isDark) 0.dp else 28.dp,
        animationSpec = spring(stiffness = 320f, dampingRatio = 0.85f),
        label = "themePillOffset",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 48.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Top Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Back",
                    tint = tokens.textTitle,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                )
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(24.dp)
                        .background(tokens.divider),
                )
                Column {
                    Text(
                        text = "Settings",
                        color = tokens.textTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Customize your NetRunner experience",
                        color = tokens.textSub,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Real Account Profile Card
        item {
            Surface(
                onClick = {
                    if (currentUser != null) {
                        onOpenModal(SettingsModalType.PROFILE_DETAIL)
                    } else {
                        onOpenGitHubAuth()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = tokens.cardBg,
                border = BorderStroke(1.dp, tokens.cardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (currentUser != null) {
                            NetworkAvatarImage(
                                url = currentUser.avatarUrl,
                                contentDescription = currentUser.login,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFFD4D4D8), CircleShape),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tokens.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF3F4F6),
                                    )
                                    .then(
                                        if (!tokens.isDark) Modifier.border(1.dp, Color(0xFFE5E7EB), CircleShape) else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                GitHubMarkIcon(size = 24.dp, tint = tokens.textTitle)
                            }
                        }

                        Column {
                            Text(
                                text = currentUser?.let { it.name ?: it.login } ?: "Sign In with GitHub",
                                color = tokens.textTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = currentUser?.let { "@${it.login}" } ?: "Connect your developer account",
                                color = tokens.textSub,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = tokens.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Section 1: AI & Integrations
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("AI & Integrations")
                SettingRow(
                    icon = Icons.Rounded.Extension,
                    title = "Connectors",
                    subtitle = "Link external tools and services",
                    onClick = { onOpenModal(SettingsModalType.CONNECTORS) },
                )
                SettingRow(
                    icon = Icons.Rounded.ViewInAr,
                    title = "Models & API Token",
                    subtitle = "Bring your own token & choose AI models",
                    onClick = { onOpenModal(SettingsModalType.MODELS) },
                )
                SettingRow(
                    icon = Icons.Rounded.SmartToy,
                    title = "Agents",
                    subtitle = "Create and manage AI agents",
                    onClick = { onOpenModal(SettingsModalType.AGENTS) },
                )
                SettingRow(
                    icon = Icons.Rounded.Bolt,
                    title = "Skills",
                    subtitle = "Add skills to extend capabilities",
                    onClick = { onOpenModal(SettingsModalType.SKILLS) },
                )
            }
        }

        // Section 2: Preferences
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Preferences")

                // Appearance row with animated Moon/Sun pill toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.cardBg,
                    border = BorderStroke(1.dp, tokens.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                imageVector = if (tokens.isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = tokens.textTitle,
                                modifier = Modifier.size(20.dp),
                            )
                            Column {
                                Text(
                                    text = "Appearance",
                                    color = tokens.textTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (tokens.isDark) "Dark mode" else "Light mode",
                                    color = tokens.textSub,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        // Animated Pill Switch
                        Surface(
                            onClick = onToggleTheme,
                            shape = CircleShape,
                            color = if (tokens.isDark) Color(0xFF15171D) else Color(0xFFE2E8F0),
                            border = BorderStroke(
                                1.dp,
                                if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1),
                            ),
                        ) {
                            Box(modifier = Modifier.padding(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = thumbOffset)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (tokens.isDark) Color.White.copy(alpha = 0.20f) else Color.White),
                                )
                                Row {
                                    Box(
                                        modifier = Modifier.size(28.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DarkMode,
                                            contentDescription = "Dark mode",
                                            tint = if (tokens.isDark) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.size(28.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LightMode,
                                            contentDescription = "Light mode",
                                            tint = if (!tokens.isDark) Color(0xFF090A0F) else Color(0xFF9CA3AF),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SettingRow(
                    icon = Icons.Rounded.Language,
                    title = "Language",
                    subtitle = selectedLanguage,
                    onClick = { onOpenModal(SettingsModalType.LANGUAGE) },
                )
                SettingRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    subtitle = "Project updates and more",
                    onClick = { onOpenModal(SettingsModalType.NOTIFICATIONS) },
                )
                SettingRow(
                    icon = Icons.Rounded.Tune,
                    title = "Editor Preferences",
                    subtitle = "Customize your editor",
                    onClick = { onOpenModal(SettingsModalType.EDITOR) },
                )
            }
        }

        // Section 3: Data & Storage
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Data & Storage")
                SettingRow(
                    icon = Icons.Rounded.Storage,
                    title = "Data & Storage",
                    subtitle = "Manage your data and storage",
                    onClick = { onOpenModal(SettingsModalType.DATA_STORAGE) },
                )
            }
        }

        // Section 4: Security
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Security")
                SettingRow(
                    icon = Icons.Rounded.Shield,
                    title = "Security",
                    subtitle = "Permissions, privacy and keys",
                    onClick = { onOpenModal(SettingsModalType.SECURITY) },
                )
            }
        }

        // Section 5: About
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("About")
                SettingRow(
                    icon = Icons.Rounded.Info,
                    title = "About NetRunner",
                    subtitle = "Version 1.0.0 • Built for developers",
                    onClick = { onOpenModal(SettingsModalType.ABOUT) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val tokens = LocalNetRunnerTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = tokens.textSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(tokens.divider),
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = tokens.cardBg,
        border = BorderStroke(1.dp, tokens.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.textTitle,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = title,
                        color = tokens.textTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        color = tokens.textSub,
                        fontSize = 12.sp,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
