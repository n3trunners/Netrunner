package com.netrunner.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrunner.ui.theme.LocalNetRunnerTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BrowseScreen(
    onBackToHome: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var subscribed by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "browseRadar")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val ringRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringRotation",
    )
    val headerSpin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "headerSpin",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top Header
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
                    contentDescription = "Return to Home",
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = null,
                        tint = tokens.textTitle,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(headerSpin),
                    )
                    Text(
                        text = "BROWSE HUB",
                        color = tokens.textTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFFF59E0B).copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                    )
                    Text(
                        text = "Offline",
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        // Center Hero Card
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(128.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .border(
                            width = 1.dp,
                            color = if (tokens.isDark) Color.White else Color(0xFF94A3B8),
                            shape = CircleShape,
                        ),
                )

                Canvas(
                    modifier = Modifier
                        .size(104.dp)
                        .rotate(ringRotation),
                ) {
                    drawCircle(
                        color = if (tokens.isDark) Color.White.copy(alpha = 0.25f) else Color(0xFF94A3B8),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        ),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (tokens.isDark) Color.White.copy(alpha = 0.06f) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (tokens.isDark) Color.White.copy(alpha = 0.20f) else Color(0xFFE2E8F0),
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(
                        modifier = Modifier.background(
                            if (tokens.isDark) {
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f)),
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.White, Color.White))
                            },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = null,
                            tint = tokens.textTitle,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            Surface(
                shape = CircleShape,
                color = if (tokens.isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF3F4F6),
                border = BorderStroke(
                    1.dp,
                    if (tokens.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RadioButtonChecked,
                        contentDescription = null,
                        tint = tokens.textSub,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Community & Plugin Registry",
                        color = tokens.textSub,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Not available at the moment.",
                color = tokens.textTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "The NetRunner browse catalog, shared blueprints, and extension store are currently undergoing server maintenance.",
                color = tokens.textSub,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(24.dp))

            Surface(
                onClick = onBackToHome,
                shape = CircleShape,
                color = if (tokens.isDark) Color.White else Color(0xFF171717),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Back to Workspace",
                        color = if (tokens.isDark) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = {
                        if (!isChecking) {
                            isChecking = true
                            scope.launch {
                                delay(900)
                                isChecking = false
                                onShowToast("Server ping: 22ms. Registry deployment in progress (ETA 12m)")
                            }
                        }
                    },
                    shape = CircleShape,
                    color = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6),
                    border = BorderStroke(
                        1.dp,
                        if (tokens.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFD4D4D8),
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                strokeWidth = 1.8.dp,
                                color = tokens.textSub,
                                modifier = Modifier.size(14.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = tokens.textSub,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isChecking) "Checking..." else "Check Status",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Surface(
                    onClick = {
                        subscribed = true
                        onShowToast("Subscribed! We will notify you when the Browse Hub launches.")
                    },
                    shape = CircleShape,
                    color = if (subscribed) {
                        Color(0xFF10B981).copy(alpha = 0.10f)
                    } else if (tokens.isDark) {
                        Color.White.copy(alpha = 0.05f)
                    } else {
                        Color(0xFFF3F4F6)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (subscribed) {
                            Color(0xFF10B981).copy(alpha = 0.30f)
                        } else if (tokens.isDark) {
                            Color.White.copy(alpha = 0.10f)
                        } else {
                            Color(0xFFD4D4D8)
                        },
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (subscribed) Icons.Rounded.Check else Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = if (subscribed) Color(0xFF10B981) else tokens.textSub,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (subscribed) "Subscribed" else "Notify Me",
                            color = if (subscribed) Color(0xFF10B981) else tokens.textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Bottom Status Strip
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.divider),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "ERR_CATALOG_UNAVAILABLE",
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "SYS_VER 1.0.0",
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
