package com.netrunner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
fun NetRunnerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkPalette else LightPalette,
        content = content,
    )
}
