package com.netrunner.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.netrunner.app.R

val NetRunnerEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val NetRunnerThemeEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Immutable
data class NetRunnerVisualTokens(
    val isDark: Boolean,
    val backgroundBase: Color,
    val textTitle: Color,
    val textSub: Color,
    val textMuted: Color,
    val divider: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val pillBg: Color,
    val pillBorder: Color,
    val pillText: Color,
    val inputBg: Color,
    val inputBorder: Color,
    val dockBg: Color,
    val dockBorder: Color,
    val accentBar: Color,
    val modalBg: Color,
    val modalBorder: Color,
)

private val DarkTokens = NetRunnerVisualTokens(
    isDark = true,
    backgroundBase = Color(0xFF060709),
    textTitle = Color(0xFFFFFFFF),
    textSub = Color(0xFF9CA3AF),
    textMuted = Color(0xFF6B7280),
    divider = Color.White.copy(alpha = 0.15f),
    cardBg = Color.White.copy(alpha = 0.035f),
    cardBorder = Color.White.copy(alpha = 0.08f),
    pillBg = Color.White.copy(alpha = 0.06f),
    pillBorder = Color.White.copy(alpha = 0.15f),
    pillText = Color(0xFFFFFFFF),
    inputBg = Color.White.copy(alpha = 0.04f),
    inputBorder = Color.White.copy(alpha = 0.10f),
    dockBg = Color(0xD90E1014),
    dockBorder = Color.White.copy(alpha = 0.12f),
    accentBar = Color(0xFF52525B),
    modalBg = Color(0xFF0D0F14),
    modalBorder = Color.White.copy(alpha = 0.12f),
)

private val LightTokens = NetRunnerVisualTokens(
    isDark = false,
    backgroundBase = Color(0xFFF4F5F8),
    textTitle = Color(0xFF090A0F),
    textSub = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    divider = Color.Black.copy(alpha = 0.12f),
    cardBg = Color.White.copy(alpha = 0.86f),
    cardBorder = Color(0xFFE2E8F0),
    pillBg = Color(0xFF090A0F),
    pillBorder = Color(0xFF090A0F),
    pillText = Color(0xFFFFFFFF),
    inputBg = Color.White.copy(alpha = 0.95f),
    inputBorder = Color(0xFFE2E8F0),
    dockBg = Color(0xE6FFFFFF),
    dockBorder = Color(0xFFE2E8F0),
    accentBar = Color(0xFF94A3B8),
    modalBg = Color(0xFFFFFFFF),
    modalBorder = Color(0xFFE2E8F0),
)

val LocalNetRunnerTokens = staticCompositionLocalOf { DarkTokens }

private val DarkPalette = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF060709),
    background = Color(0xFF060709),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF0E1014),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF151820),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0x1FFFFFFF),
)

private val LightPalette = lightColorScheme(
    primary = Color(0xFF090A0F),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F5F8),
    onBackground = Color(0xFF090A0F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF090A0F),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
)

@Composable
private fun animateThemeColor(target: Color, label: String): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 460, easing = NetRunnerThemeEase),
        label = label,
    )
    return color
}

@Composable
fun NetRunnerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val raw = if (darkTheme) DarkTokens else LightTokens
    val animatedTokens = NetRunnerVisualTokens(
        isDark = darkTheme,
        backgroundBase = animateThemeColor(raw.backgroundBase, "bgBase"),
        textTitle = animateThemeColor(raw.textTitle, "textTitle"),
        textSub = animateThemeColor(raw.textSub, "textSub"),
        textMuted = animateThemeColor(raw.textMuted, "textMuted"),
        divider = animateThemeColor(raw.divider, "divider"),
        cardBg = animateThemeColor(raw.cardBg, "cardBg"),
        cardBorder = animateThemeColor(raw.cardBorder, "cardBorder"),
        pillBg = animateThemeColor(raw.pillBg, "pillBg"),
        pillBorder = animateThemeColor(raw.pillBorder, "pillBorder"),
        pillText = animateThemeColor(raw.pillText, "pillText"),
        inputBg = animateThemeColor(raw.inputBg, "inputBg"),
        inputBorder = animateThemeColor(raw.inputBorder, "inputBorder"),
        dockBg = animateThemeColor(raw.dockBg, "dockBg"),
        dockBorder = animateThemeColor(raw.dockBorder, "dockBorder"),
        accentBar = animateThemeColor(raw.accentBar, "accentBar"),
        modalBg = animateThemeColor(raw.modalBg, "modalBg"),
        modalBorder = animateThemeColor(raw.modalBorder, "modalBorder"),
    )

    CompositionLocalProvider(LocalNetRunnerTokens provides animatedTokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkPalette else LightPalette,
            content = content,
        )
    }
}

@Composable
fun NetRunnerWallpaperBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.backgroundBase),
    ) {
        Crossfade(
            targetState = tokens.isDark,
            animationSpec = tween(durationMillis = 480, easing = NetRunnerThemeEase),
            label = "wallpaperThemeCrossfade",
        ) { dark ->
            if (dark) {
                Image(
                    painter = painterResource(id = R.drawable.bg_dark_wallpaper),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFF8FAFC),
                                        Color(0xFFEEF2F7),
                                    ),
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xD9E2E8F0),
                                        Color.Transparent,
                                    ),
                                    center = Offset(0f, 0f),
                                    radius = 900f,
                                ),
                            ),
                    )
                }
            }
        }
        content()
    }
}
