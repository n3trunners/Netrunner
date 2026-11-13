package com.netrunner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrunner.app.R
import com.netrunner.model.ActiveTab
import com.netrunner.network.GitHubApiClient
import com.netrunner.ui.theme.LocalNetRunnerTokens

@Composable
fun NetRunnerLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val tokens = LocalNetRunnerTokens.current
    Image(
        painter = painterResource(id = R.drawable.ic_netrunner_logo),
        contentDescription = "NetRunner Logo",
        colorFilter = ColorFilter.tint(tokens.textTitle),
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Composable
fun GitHubMarkIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = LocalNetRunnerTokens.current.textTitle,
) {
    Canvas(modifier = modifier.size(size)) {
        val sx = this.size.width / 24f
        val sy = this.size.height / 24f

        val bodyPath = Path().apply {
            moveTo(15f * sx, 22f * sy)
            verticalLineTo(18f * sy)
            cubicTo(15f * sx, 16.8f * sy, 15.1f * sx, 16.1f * sy, 14.5f * sx, 15.5f * sy)
            cubicTo(17.7f * sx, 15.1f * sy, 21f * sx, 13.9f * sy, 21f * sx, 8.5f * sy)
            cubicTo(21f * sx, 7.1f * sy, 20.5f * sx, 5.8f * sy, 19.6f * sx, 4.8f * sy)
            cubicTo(19.9f * sx, 3.8f * sy, 19.9f * sx, 2.7f * sy, 19.5f * sx, 1.8f * sy)
            cubicTo(19.5f * sx, 1.8f * sy, 18.4f * sx, 1.5f * sy, 16f * sx, 3.1f * sy)
            cubicTo(13.4f * sx, 2.4f * sy, 10.6f * sx, 2.4f * sy, 8f * sx, 3.1f * sy)
            cubicTo(5.6f * sx, 1.5f * sy, 4.5f * sx, 1.8f * sy, 4.5f * sx, 1.8f * sy)
            cubicTo(4.1f * sx, 2.7f * sy, 4.1f * sx, 3.8f * sy, 4.4f * sx, 4.8f * sy)
            cubicTo(3.5f * sx, 5.8f * sy, 3f * sx, 7.1f * sy, 3f * sx, 8.5f * sy)
            cubicTo(3f * sx, 13.9f * sy, 6.3f * sx, 15.1f * sy, 9.5f * sx, 15.5f * sy)
            cubicTo(9f * sx, 16.1f * sy, 9f * sx, 16.9f * sy, 9f * sx, 18f * sy)
            verticalLineTo(22f * sy)
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val tailPath = Path().apply {
            moveTo(9f * sx, 18f * sy)
            cubicTo(4.5f * sx, 20f * sy, 4f * sx, 16f * sy, 2f * sx, 16f * sy)
        }
        drawPath(
            path = tailPath,
            color = tint,
            style = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
fun NetworkAvatarImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = GitHubApiClient.loadAvatar(url)
    }
    val tokens = LocalNetRunnerTokens.current
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(tokens.cardBg),
            contentAlignment = Alignment.Center,
        ) {
            GitHubMarkIcon(size = 18.dp, tint = tokens.textTitle)
        }
    }
}

@Composable
fun GlossyFolderGraphic(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "folderSparkles")
    val alpha1 by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "s1",
    )
    val alpha2 by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "s2",
    )
    val tokens = LocalNetRunnerTokens.current
    val sparkleColor = if (tokens.isDark) Color.White else Color(0xFF475569)

    Box(modifier = modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sx = size.width / 120f
            val sy = size.height / 100f

            // Folder Back Tab
            val backPath = Path().apply {
                moveTo(18f * sx, 24f * sy)
                cubicTo(18f * sx, 20.7f * sy, 20.7f * sx, 18f * sy, 24f * sx, 18f * sy)
                lineTo(44f * sx, 18f * sy)
                cubicTo(46.8f * sx, 18f * sy, 49.3f * sx, 19.5f * sy, 50.7f * sx, 21.8f * sy)
                lineTo(54.3f * sx, 27.5f * sy)
                cubicTo(55.2f * sx, 29f * sy, 56.8f * sx, 30f * sy, 58.6f * sx, 30f * sy)
                lineTo(96f * sx, 30f * sy)
                cubicTo(99.3f * sx, 30f * sy, 102f * sx, 32.7f * sy, 102f * sx, 36f * sy)
                lineTo(102f * sx, 78f * sy)
                cubicTo(102f * sx, 81.3f * sy, 99.3f * sx, 84f * sy, 96f * sx, 84f * sy)
                lineTo(24f * sx, 84f * sy)
                cubicTo(20.7f * sx, 84f * sy, 18f * sx, 81.3f * sy, 18f * sx, 78f * sy)
                close()
            }
            drawPath(
                path = backPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2C3038), Color(0xFF1A1C22), Color(0xFF101115)),
                    start = Offset(10f * sx, 20f * sy),
                    end = Offset(110f * sx, 85f * sy),
                ),
            )
            drawPath(
                path = backPath,
                color = Color.White.copy(alpha = 0.08f),
                style = Stroke(width = 1.2f * sx),
            )

            // Paper / Inner Glow peek
            val peekPath = Path().apply {
                moveTo(26f * sx, 30f * sy)
                lineTo(94f * sx, 30f * sy)
                cubicTo(96f * sx, 30f * sy, 98f * sx, 31.5f * sy, 98f * sx, 33.5f * sy)
                lineTo(98f * sx, 45f * sy)
                lineTo(22f * sx, 45f * sy)
                lineTo(22f * sx, 33.5f * sy)
                cubicTo(22f * sx, 31.5f * sy, 24f * sx, 30f * sy, 26f * sx, 30f * sy)
                close()
            }
            drawPath(path = peekPath, color = Color.White.copy(alpha = 0.04f))

            // Folder Front Flap
            val frontPath = Path().apply {
                moveTo(24f * sx, 37f * sy)
                cubicTo(24f * sx, 34.2f * sy, 26.2f * sx, 32f * sy, 29f * sx, 32f * sy)
                lineTo(91f * sx, 32f * sy)
                cubicTo(93.8f * sx, 32f * sy, 96f * sx, 34.2f * sy, 96f * sx, 37f * sy)
                lineTo(91f * sx, 82f * sy)
                cubicTo(90.6f * sx, 85.2f * sy, 87.8f * sx, 87.5f * sy, 84.6f * sx, 87.5f * sy)
                lineTo(23.4f * sx, 87.5f * sy)
                cubicTo(20.2f * sx, 87.5f * sy, 17.4f * sx, 85.2f * sy, 17f * sx, 82f * sy)
                close()
            }
            drawPath(
                path = frontPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3A3F4B), Color(0xFF242730), Color(0xFF12141A)),
                    start = Offset(15f * sx, 36f * sy),
                    end = Offset(105f * sx, 92f * sy),
                ),
            )
            drawPath(
                path = frontPath,
                color = Color.White.copy(alpha = 0.10f),
                style = Stroke(width = 1.2f * sx),
            )

            // Top Rim Specular Highlight
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.02f),
                    ),
                    startX = 27f * sx,
                    endX = 93f * sx,
                ),
                start = Offset(27f * sx, 34f * sy),
                end = Offset(93f * sx, 34f * sy),
                strokeWidth = 1.5f * sx,
                cap = StrokeCap.Round,
            )

            // Sparkle 1 & Sparkle 2
            fun drawSparkle(cx: Float, cy: Float, r: Float, alpha: Float) {
                val sp = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.22f, cy - r * 0.22f)
                    lineTo(cx + r, cy)
                    lineTo(cx + r * 0.22f, cy + r * 0.22f)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.22f, cy + r * 0.22f)
                    lineTo(cx - r, cy)
                    lineTo(cx - r * 0.22f, cy - r * 0.22f)
                    close()
                }
                drawPath(sp, color = sparkleColor.copy(alpha = alpha))
            }
            drawSparkle(98f * sx, 12f * sy, 8.5f * sx, alpha1)
            drawSparkle(112f * sx, 26f * sy, 6.5f * sx, alpha2)
        }
    }
}

private data class DockTabItem(
    val tab: ActiveTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun BottomDock(
    activeTab: ActiveTab,
    onChangeTab: (ActiveTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNetRunnerTokens.current
    val items = remember {
        listOf(
            DockTabItem(ActiveTab.HOME, "Workspace", Icons.Rounded.Home, Icons.Outlined.Home),
            DockTabItem(ActiveTab.BROWSE, "Browse", Icons.Rounded.Explore, Icons.Outlined.Explore),
            DockTabItem(ActiveTab.PROFILE, "Profile", Icons.Rounded.Person, Icons.Outlined.Person),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = tokens.dockBg,
            border = BorderStroke(1.dp, tokens.dockBorder),
            shadowElevation = if (tokens.isDark) 20.dp else 14.dp,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isActive = activeTab == item.tab
                    val iconScale by animateFloatAsState(
                        targetValue = if (isActive) 1.12f else 1.0f,
                        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
                        label = "dockIconScale",
                    )
                    val indicatorAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0f,
                        animationSpec = tween(220),
                        label = "dockIndicatorAlpha",
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onChangeTab(item.tab) },
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = if (isActive) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (isActive) tokens.textTitle else tokens.textMuted,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(iconScale),
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.5.dp)
                                .alpha(indicatorAlpha)
                                .clip(CircleShape)
                                .background(tokens.textTitle)
                                .then(
                                    if (isActive && tokens.isDark) {
                                        Modifier.shadow(8.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
                                    } else Modifier
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToastBanner(
    message: String?,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalNetRunnerTokens.current
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        if (message != null) {
            Surface(
                shape = CircleShape,
                color = if (tokens.isDark) Color(0xEE181A20) else Color(0xEE090A0F),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shadowElevation = 12.dp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
