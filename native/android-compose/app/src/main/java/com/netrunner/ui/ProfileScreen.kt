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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrunner.model.GitHubRepo
import com.netrunner.model.GitHubStats
import com.netrunner.model.GitHubUser
import com.netrunner.ui.components.GitHubMarkIcon
import com.netrunner.ui.components.NetworkAvatarImage
import com.netrunner.ui.theme.LocalNetRunnerTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    currentUser: GitHubUser?,
    userStats: GitHubStats?,
    userRepos: List<GitHubRepo>,
    isRefreshing: Boolean,
    onBackToHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGitHubAuth: () -> Unit,
    onSignOut: () -> Unit,
    onRefreshStats: () -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    val context = LocalContext.current

    fun openExternalUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to workspace",
                        tint = tokens.textSub,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onBackToHome() },
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(tokens.divider),
                    )
                    Text(
                        text = "Developer Profile",
                        color = tokens.textTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentUser != null) {
                        Surface(
                            onClick = onRefreshStats,
                            shape = CircleShape,
                            color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        strokeWidth = 1.8.dp,
                                        color = tokens.textSub,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh Real GitHub Stats",
                                        tint = if (tokens.isDark) Color(0xFFD1D5DB) else Color(0xFF374151),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = if (tokens.isDark) Color(0xFFD1D5DB) else Color(0xFF374151),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        if (currentUser != null) {
            // Profile Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = tokens.cardBg,
                    border = BorderStroke(1.dp, tokens.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NetworkAvatarImage(
                            url = currentUser.avatarUrl,
                            contentDescription = currentUser.login,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (tokens.isDark) Color.White.copy(alpha = 0.20f) else Color(0xFFD4D4D8),
                                    CircleShape,
                                ),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = currentUser.name ?: currentUser.login,
                            color = tokens.textTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.clickable { openExternalUrl(currentUser.htmlUrl) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            GitHubMarkIcon(size = 13.dp, tint = tokens.textSub)
                            Text(
                                text = "@${currentUser.login}",
                                color = tokens.textSub,
                                fontSize = 12.sp,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = tokens.textMuted,
                                modifier = Modifier.size(12.dp),
                            )
                        }

                        if (!currentUser.bio.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = currentUser.bio,
                                color = tokens.textSub,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                            )
                        }

                        if (!currentUser.location.isNullOrBlank() || !currentUser.company.isNullOrBlank() || !currentUser.blog.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                currentUser.location?.let { loc ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.LocationOn, null, tint = tokens.textMuted, modifier = Modifier.size(12.dp))
                                        Text(loc, color = tokens.textMuted, fontSize = 11.sp)
                                    }
                                }
                                currentUser.company?.let { comp ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.Business, null, tint = tokens.textMuted, modifier = Modifier.size(12.dp))
                                        Text(comp, color = tokens.textMuted, fontSize = 11.sp)
                                    }
                                }
                                currentUser.blog?.let { blog ->
                                    val url = if (blog.startsWith("http")) blog else "https://$blog"
                                    Row(
                                        modifier = Modifier.clickable { openExternalUrl(url) },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.Language, null, tint = tokens.textMuted, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = blog.removePrefix("https://").removePrefix("http://"),
                                            color = tokens.textMuted,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real Stats Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Real GitHub Metrics",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Live Synced",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MetricBox(
                            value = "${currentUser.publicRepos}",
                            label = "Repos",
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = tokens.cardBg,
                            border = BorderStroke(1.dp, tokens.cardBorder),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "${userStats?.totalStars ?: 0}",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                                Text(
                                    text = "Stars",
                                    color = tokens.textSub,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        MetricBox(
                            value = "${currentUser.followers}",
                            label = "Followers",
                            modifier = Modifier.weight(1f),
                        )
                        MetricBox(
                            value = "${currentUser.following}",
                            label = "Following",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Primary Languages
            if (userStats != null && userStats.topLanguages.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Primary Languages",
                                color = tokens.textSub,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                userStats.topLanguages.take(5).forEach { lang ->
                                    Surface(
                                        shape = CircleShape,
                                        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                                        border = BorderStroke(
                                            1.dp,
                                            if (tokens.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB),
                                        ),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (tokens.isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1F2937),
                                                    ),
                                            )
                                            Text(
                                                text = lang.language,
                                                color = if (tokens.isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937),
                                                fontSize = 12.sp,
                                            )
                                            Text(
                                                text = "(${lang.count})",
                                                color = tokens.textMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Repositories header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent Repositories",
                        color = tokens.textSub,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.clickable { openExternalUrl("${currentUser.htmlUrl}?tab=repositories") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "View all on GitHub",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = tokens.textSub,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            if (userRepos.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "No public repositories found.",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        )
                    }
                }
            } else {
                items(userRepos.take(6), key = { it.id }) { repo ->
                    Surface(
                        onClick = { openExternalUrl(repo.htmlUrl) },
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Book,
                                        contentDescription = null,
                                        tint = tokens.textMuted,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = repo.name,
                                        color = tokens.textTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!repo.description.isNullOrBlank()) {
                                    Text(
                                        text = repo.description,
                                        color = tokens.textSub,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (!repo.language.isNullOrBlank()) {
                                        Text(
                                            text = repo.language,
                                            color = tokens.textMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                        Text("${repo.stars}", color = tokens.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Icon(Icons.Rounded.CallSplit, null, tint = tokens.textMuted, modifier = Modifier.size(10.dp))
                                        Text("${repo.forks}", color = tokens.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = tokens.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Switch Account & Sign Out
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = onOpenGitHubAuth,
                        shape = RoundedCornerShape(16.dp),
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                        border = BorderStroke(
                            1.dp,
                            if (tokens.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFD4D4D8),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Switch Account",
                                color = if (tokens.isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Surface(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.20f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Sign Out",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        } else {
            // Not Signed In Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 48.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFF171717),
                        border = BorderStroke(
                            1.dp,
                            if (tokens.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF262626),
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(80.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            GitHubMarkIcon(size = 40.dp, tint = Color.White)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Connect GitHub Account",
                            color = tokens.textTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Sign in with GitHub to view your real statistics, explore your public repositories, and sync your developer identity.",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                    }

                    Surface(
                        onClick = onOpenGitHubAuth,
                        shape = CircleShape,
                        color = if (tokens.isDark) Color.White else Color(0xFF171717),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GitHubMarkIcon(
                                size = 16.dp,
                                tint = if (tokens.isDark) Color.Black else Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Sign In with GitHub",
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
private fun MetricBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNetRunnerTokens.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tokens.cardBg,
        border = BorderStroke(1.dp, tokens.cardBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = tokens.textTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = label,
                color = tokens.textSub,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
