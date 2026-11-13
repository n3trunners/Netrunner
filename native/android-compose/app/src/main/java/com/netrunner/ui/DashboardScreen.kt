package com.netrunner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrunner.model.GitHubRepo
import com.netrunner.model.GitHubUser
import com.netrunner.model.WorkspaceProject
import com.netrunner.ui.components.GitHubMarkIcon
import com.netrunner.ui.components.GlossyFolderGraphic
import com.netrunner.ui.components.NetRunnerLogo
import com.netrunner.ui.components.NetworkAvatarImage
import com.netrunner.ui.theme.LocalNetRunnerTokens

@Composable
fun DashboardScreen(
    projects: List<WorkspaceProject>,
    hasApiToken: Boolean,
    selectedModel: String,
    currentUser: GitHubUser?,
    userRepos: List<GitHubRepo>,
    onOpenNewProjectModal: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTokenSettings: () -> Unit,
    onOpenHybridStudio: () -> Unit,
    onOpenGitHubAuth: () -> Unit,
    onImportGitHubRepo: (GitHubRepo) -> Unit,
    onSelectProject: (WorkspaceProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onResetProjects: () -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val filteredProjects = remember(projects, searchQuery, selectedTag) {
        projects.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.description.contains(searchQuery, ignoreCase = true) ||
                p.category.contains(searchQuery, ignoreCase = true)
            val matchesTag = selectedTag == null || p.category == selectedTag
            matchesSearch && matchesTag
        }
    }

    val welcomeName = remember(currentUser) {
        when {
            currentUser == null -> "Developer"
            !currentUser.name.isNullOrBlank() -> currentUser.name.trim().split(" ").first()
            else -> currentUser.login
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 116.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 1. Top Header: NetRunner Logo + Divider + NETRUNNER + GitHub Sign In / Avatar + Settings
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NetRunnerLogo(size = 28.dp)
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(20.dp)
                            .background(tokens.divider),
                    )
                    Text(
                        text = "NETRUNNER",
                        color = tokens.textTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.5.sp,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (currentUser != null) {
                        NetworkAvatarImage(
                            url = currentUser.avatarUrl,
                            contentDescription = "@${currentUser.login}",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    if (tokens.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD4D4D8),
                                    CircleShape,
                                )
                                .clickable { onOpenSettings() },
                        )
                    } else {
                        Surface(
                            onClick = onOpenGitHubAuth,
                            shape = CircleShape,
                            color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                            border = BorderStroke(
                                1.dp,
                                if (tokens.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFD4D4D8),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                GitHubMarkIcon(
                                    size = 14.dp,
                                    tint = if (tokens.isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937),
                                )
                                Text(
                                    text = "Sign In",
                                    color = if (tokens.isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                        border = BorderStroke(
                            1.dp,
                            if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFD4D4D8),
                        ),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Open Settings",
                                tint = if (tokens.isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1F2937),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // 2. Welcome Section
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Welcome back,",
                    color = tokens.textSub,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$welcomeName.",
                    color = tokens.textTitle,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Build. Code. Innovate.",
                    color = tokens.textSub,
                    fontSize = 14.sp,
                    letterSpacing = 0.3.sp,
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(tokens.accentBar),
                )
            }
        }

        // 3. Action Capsule Button (+ New Project >) & Quick GitHub Import
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onOpenNewProjectModal,
                    shape = CircleShape,
                    color = tokens.pillBg,
                    border = BorderStroke(1.dp, tokens.pillBorder),
                    shadowElevation = if (tokens.isDark) 8.dp else 10.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                            ),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = tokens.pillText,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "New Project",
                                    color = tokens.pillText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = tokens.pillText.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                if (currentUser != null && userRepos.isNotEmpty()) {
                    val latestRepo = userRepos.first()
                    Surface(
                        onClick = { onImportGitHubRepo(latestRepo) },
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (tokens.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFD4D4D8),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GitHubMarkIcon(
                                size = 14.dp,
                                tint = if (tokens.isDark) Color(0xFFD1D5DB) else Color(0xFF1F2937),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Import @${currentUser.login}/${latestRepo.name}",
                                color = if (tokens.isDark) Color(0xFFD1D5DB) else Color(0xFF1F2937),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // 4. Search Bar Input
        item {
            Surface(
                shape = CircleShape,
                color = tokens.inputBg,
                border = BorderStroke(1.dp, tokens.inputBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = tokens.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search projects...",
                                color = tokens.textMuted,
                                fontSize = 14.sp,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = tokens.textTitle,
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(tokens.textTitle),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = tokens.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" },
                        )
                    }
                }
            }
        }

        // 5. Quick Access Cards: BYOK AI Token & Hybrid Architecture & CI/CD
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onOpenTokenSettings,
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
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val badgeTint = if (hasApiToken) Color(0xFF34D399) else Color(0xFFFBBF24)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeTint.copy(alpha = 0.12f))
                                    .border(1.dp, badgeTint.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = badgeTint,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = if (hasApiToken) "Personal AI Token Active" else "Add Your AI Token (BYOK)",
                                    color = tokens.textTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (hasApiToken) {
                                        "$selectedModel · Using your own key"
                                    } else {
                                        "No free tokens provided · Paste your Gemini key"
                                    },
                                    color = tokens.textSub,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = tokens.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Surface(
                    onClick = onOpenHybridStudio,
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                        ) {
                            Text(
                                text = "Hybrid Architecture & CI/CD",
                                color = tokens.textTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Kotlin + Jetpack Compose · Rust Daemon · GitHub Actions",
                                color = tokens.textSub,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = tokens.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // 6. Empty State OR Projects List
        if (projects.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GlossyFolderGraphic()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "No Projects Yet",
                        color = tokens.textTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Create your first project to get started.",
                        color = tokens.textSub,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    Surface(
                        onClick = onOpenNewProjectModal,
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.07f) else Color(0xFF171717),
                        border = BorderStroke(
                            1.dp,
                            if (tokens.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF171717),
                        ),
                        shadowElevation = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Create Project",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Your Projects (${filteredProjects.size})",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Reset to Empty State",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onResetProjects() },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val categories = listOf<String?>(null, "Hybrid", "AI Agent", "Fullstack", "Frontend", "API")
                        categories.forEach { cat ->
                            val isSelected = selectedTag == cat
                            Surface(
                                onClick = { selectedTag = cat },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected && tokens.isDark -> Color.White
                                    isSelected && !tokens.isDark -> Color(0xFF171717)
                                    tokens.isDark -> Color.White.copy(alpha = 0.05f)
                                    else -> Color(0xFFE5E7EB)
                                },
                            ) {
                                Text(
                                    text = cat ?: "All",
                                    color = when {
                                        isSelected && tokens.isDark -> Color.Black
                                        isSelected && !tokens.isDark -> Color.White
                                        tokens.isDark -> Color(0xFF9CA3AF)
                                        else -> Color(0xFF374151)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (filteredProjects.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "No projects matched \"$searchQuery\"",
                            color = tokens.textSub,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                        )
                    }
                }
            } else {
                items(filteredProjects, key = { it.id }) { proj ->
                    Surface(
                        onClick = { onSelectProject(proj) },
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
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = proj.name,
                                    color = tokens.textTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = proj.description,
                                    color = tokens.textSub,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${proj.category} · Updated ${proj.updatedAt} · ${proj.filesCount} modules",
                                    color = tokens.textMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = { onSelectProject(proj) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = "Open Project Workspace",
                                            tint = if (tokens.isDark) Color(0xFFD1D5DB) else Color(0xFF374151),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                Surface(
                                    onClick = { onDeleteProject(proj.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete Project",
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(16.dp),
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
}
