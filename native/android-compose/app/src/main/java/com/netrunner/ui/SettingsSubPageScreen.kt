package com.netrunner.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrunner.model.SavedApiKeyEntry
import com.netrunner.model.SettingsModalType
import com.netrunner.model.UserCustomIntegration
import com.netrunner.ui.components.rememberPressScaleInteraction
import com.netrunner.ui.theme.LocalNetRunnerTokens
import com.netrunner.ui.theme.NetRunnerEase

private fun maskApiKey(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.length <= 10) return "••••••••"
    return "${trimmed.take(5)}••••••${trimmed.takeLast(4)}"
}

@Composable
fun SettingsSubPageScreen(
    pageType: SettingsModalType,
    savedKeys: List<SavedApiKeyEntry>,
    connectors: List<UserCustomIntegration>,
    agents: List<UserCustomIntegration>,
    skills: List<UserCustomIntegration>,
    onBack: () -> Unit,
    onSaveNewApiKey: (String) -> Unit,
    onUpdateApiKey: (id: String, newKey: String) -> Unit,
    onDeleteApiKey: (id: String) -> Unit,
    onAddIntegration: (SettingsModalType, String, String) -> Unit,
    onDeleteIntegration: (SettingsModalType, String) -> Unit,
    onShowToast: (String) -> Unit,
) {
    val tokens = LocalNetRunnerTokens.current
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    // Models & API Token state
    var showSupportedApis by remember(pageType) { mutableStateOf(false) }
    var selectedGeminiApi by remember(pageType) { mutableStateOf(false) }
    var newKeyInput by remember(pageType) { mutableStateOf("") }
    var showNewKeyPlain by remember(pageType) { mutableStateOf(false) }

    var expandedKeyId by remember(pageType) { mutableStateOf<String?>(null) }
    var editingKeyId by remember(pageType) { mutableStateOf<String?>(null) }
    var editKeyInput by remember(pageType) { mutableStateOf("") }
    val visibleKeyIds = remember(pageType) { mutableStateMapOf<String, Boolean>() }
    var copiedKeyId by remember(pageType) { mutableStateOf<String?>(null) }

    // Connectors / Agents / Skills state
    var isAddingCustom by remember(pageType) { mutableStateOf(false) }
    var customName by remember(pageType) { mutableStateOf("") }
    var customDetail by remember(pageType) { mutableStateOf("") }

    val isPlusOpen = if (pageType == SettingsModalType.MODELS) showSupportedApis else isAddingCustom
    val plusRotation by animateFloatAsState(
        targetValue = if (isPlusOpen) 45f else 0f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.72f),
        label = "subPagePlusRotate",
    )
    val (plusInteraction, plusScale) = rememberPressScaleInteraction(0.9f)

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header with Back + Title + Top-Right Plus Icon
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
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Back to Settings",
                        tint = tokens.textTitle,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onBack() },
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(22.dp)
                            .background(tokens.divider),
                    )
                    Text(
                        text = pageType.title,
                        color = tokens.textTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    onClick = {
                        if (pageType == SettingsModalType.MODELS) {
                            val next = !showSupportedApis
                            showSupportedApis = next
                            if (!next) {
                                selectedGeminiApi = false
                                newKeyInput = ""
                            }
                            editingKeyId = null
                        } else {
                            isAddingCustom = !isAddingCustom
                        }
                    },
                    interactionSource = plusInteraction,
                    shape = CircleShape,
                    color = if (isPlusOpen) {
                        if (tokens.isDark) Color.White else Color(0xFF171717)
                    } else if (tokens.isDark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.White
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isPlusOpen) Color.Transparent else tokens.cardBorder,
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .graphicsLayer {
                            scaleX = plusScale
                            scaleY = plusScale
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add ${pageType.title}",
                            tint = if (isPlusOpen) {
                                if (tokens.isDark) Color.Black else Color.White
                            } else tokens.textTitle,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(plusRotation),
                        )
                    }
                }
            }
        }

        if (pageType == SettingsModalType.MODELS) {
            // Step 1 & 2: Supported API selector -> Concise Gemini API key input card
            item {
                AnimatedVisibility(
                    visible = showSupportedApis,
                    enter = expandVertically(tween(280, easing = NetRunnerEase)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(220, easing = NetRunnerEase)) + fadeOut(tween(150)),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
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
                            Text(
                                text = "Supported APIs",
                                color = tokens.textSub,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )

                            val (apiInteraction, apiScale) = rememberPressScaleInteraction(0.97f)
                            Surface(
                                onClick = { selectedGeminiApi = true },
                                interactionSource = apiInteraction,
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    selectedGeminiApi && tokens.isDark -> Color.White
                                    selectedGeminiApi && !tokens.isDark -> Color(0xFF171717)
                                    tokens.isDark -> Color.White.copy(alpha = 0.05f)
                                    else -> Color.White
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedGeminiApi) Color.Transparent else tokens.cardBorder,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = apiScale
                                        scaleY = apiScale
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 13.dp),
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
                                                selectedGeminiApi && tokens.isDark -> Color.Black
                                                selectedGeminiApi && !tokens.isDark -> Color.White
                                                else -> tokens.textTitle
                                            },
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = "Gemini API",
                                            color = when {
                                                selectedGeminiApi && tokens.isDark -> Color.Black
                                                selectedGeminiApi && !tokens.isDark -> Color.White
                                                else -> tokens.textTitle
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Text(
                                        text = "Google AI Studio",
                                        color = when {
                                            selectedGeminiApi && tokens.isDark -> Color.Black.copy(alpha = 0.65f)
                                            selectedGeminiApi && !tokens.isDark -> Color.White.copy(alpha = 0.75f)
                                            else -> tokens.textMuted
                                        },
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }

                            // Step 2: Concise Card for Key Entry when Gemini API is selected
                            AnimatedVisibility(
                                visible = selectedGeminiApi,
                                enter = expandVertically(tween(300, easing = NetRunnerEase)) + fadeIn(tween(240)),
                                exit = shrinkVertically(tween(220, easing = NetRunnerEase)) + fadeOut(tween(150)),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (tokens.isDark) Color(0xFF0A0D14) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, tokens.cardBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "Gemini API Key",
                                                color = tokens.textSub,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
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
                                            value = newKeyInput,
                                            onValueChange = { newKeyInput = it },
                                            placeholder = { Text("AIza...") },
                                            visualTransformation = if (showNewKeyPlain) {
                                                VisualTransformation.None
                                            } else {
                                                PasswordVisualTransformation()
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = if (showNewKeyPlain) {
                                                        Icons.Rounded.VisibilityOff
                                                    } else {
                                                        Icons.Rounded.Visibility
                                                    },
                                                    contentDescription = "Toggle key visibility",
                                                    modifier = Modifier.clickable { showNewKeyPlain = !showNewKeyPlain },
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
                                                        showSupportedApis = false
                                                        selectedGeminiApi = false
                                                        newKeyInput = ""
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            val canSave = newKeyInput.trim().isNotEmpty()
                                            val (saveInteraction, saveScale) = rememberPressScaleInteraction(0.95f)
                                            Surface(
                                                onClick = {
                                                    if (canSave) {
                                                        onSaveNewApiKey(newKeyInput.trim())
                                                        newKeyInput = ""
                                                        selectedGeminiApi = false
                                                        showSupportedApis = false
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

            if (savedKeys.isEmpty() && !showSupportedApis) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Tap + in the top right to add a Gemini API key.",
                            color = tokens.textSub,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                itemsIndexed(savedKeys, key = { _, entry -> entry.id }) { idx, entry ->
                    val isExpanded = expandedKeyId == entry.id
                    val isVisible = visibleKeyIds[entry.id] == true
                    val isEditing = editingKeyId == entry.id
                    val (rowInteraction, rowScale) = rememberPressScaleInteraction(0.985f)

                    Surface(
                        onClick = {
                            expandedKeyId = if (isExpanded) null else entry.id
                        },
                        interactionSource = rowInteraction,
                        shape = RoundedCornerShape(18.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = tween(260, easing = NetRunnerEase),
                                fadeOutSpec = tween(180),
                                placementSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
                            )
                            .animateContentSize(spring(stiffness = 380f, dampingRatio = 0.82f))
                            .graphicsLayer {
                                scaleX = rowScale
                                scaleY = rowScale
                            },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Concise row + view eye icon at right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
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
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (tokens.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF171717),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (savedKeys.size > 1) {
                                                "${entry.providerLabel} · #${idx + 1}"
                                            } else {
                                                entry.providerLabel
                                            },
                                            color = tokens.textTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (isVisible) entry.apiKey else maskApiKey(entry.apiKey),
                                            color = tokens.textSub,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            visibleKeyIds[entry.id] = !isVisible
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isVisible) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = if (isVisible) "Hide key" else "View key",
                                        tint = tokens.textSub,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            // Expandable options below: Copy, Edit, Delete
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(tween(240, easing = NetRunnerEase)) + fadeIn(tween(200)),
                                exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    HorizontalDivider(color = tokens.divider)
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editKeyInput,
                                            onValueChange = { editKeyInput = it },
                                            placeholder = { Text("AIza...") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                        ) {
                                            Text(
                                                text = "Cancel",
                                                color = tokens.textSub,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        editingKeyId = null
                                                        editKeyInput = ""
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                onClick = {
                                                    if (editKeyInput.trim().isNotEmpty()) {
                                                        onUpdateApiKey(entry.id, editKeyInput.trim())
                                                        editingKeyId = null
                                                        editKeyInput = ""
                                                    }
                                                },
                                                shape = CircleShape,
                                                color = if (tokens.isDark) Color.White else Color(0xFF171717),
                                            ) {
                                                Text(
                                                    text = "Save",
                                                    color = if (tokens.isDark) Color.Black else Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            // Copy
                                            Surface(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(entry.apiKey))
                                                    copiedKeyId = entry.id
                                                    onShowToast("API key copied")
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (tokens.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF3F4F6),
                                                border = BorderStroke(1.dp, tokens.cardBorder),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 9.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        imageVector = if (copiedKeyId == entry.id) {
                                                            Icons.Rounded.Check
                                                        } else {
                                                            Icons.Rounded.ContentCopy
                                                        },
                                                        contentDescription = null,
                                                        tint = if (copiedKeyId == entry.id) Color(0xFF34D399) else tokens.textTitle,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = if (copiedKeyId == entry.id) "Copied" else "Copy",
                                                        color = tokens.textTitle,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                    )
                                                }
                                            }

                                            // Edit
                                            Surface(
                                                onClick = {
                                                    editingKeyId = entry.id
                                                    editKeyInput = entry.apiKey
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (tokens.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF3F4F6),
                                                border = BorderStroke(1.dp, tokens.cardBorder),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 9.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = null,
                                                        tint = tokens.textTitle,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "Edit",
                                                        color = tokens.textTitle,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                    )
                                                }
                                            }

                                            // Delete
                                            Surface(
                                                onClick = {
                                                    onDeleteApiKey(entry.id)
                                                    if (expandedKeyId == entry.id) expandedKeyId = null
                                                    if (editingKeyId == entry.id) editingKeyId = null
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 9.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF87171),
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "Delete",
                                                        color = Color(0xFFF87171),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
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
        } else {
            // SEPARATE PAGES FOR CONNECTORS / AGENTS / SKILLS
            val list = when (pageType) {
                SettingsModalType.CONNECTORS -> connectors
                SettingsModalType.AGENTS -> agents
                else -> skills
            }

            item {
                AnimatedVisibility(
                    visible = isAddingCustom,
                    enter = expandVertically(tween(260, easing = NetRunnerEase)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200, easing = NetRunnerEase)) + fadeOut(tween(140)),
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "New ${pageType.title.dropLast(1)}",
                                color = tokens.textTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = customDetail,
                                onValueChange = { customDetail = it },
                                label = { Text("Type / Objective") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
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
                                        .clickable { isAddingCustom = false }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    onClick = {
                                        if (customName.isNotBlank()) {
                                            onAddIntegration(
                                                pageType,
                                                customName.trim(),
                                                customDetail.trim().ifEmpty { "Custom" },
                                            )
                                            customName = ""
                                            customDetail = ""
                                            isAddingCustom = false
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (customName.isNotBlank()) {
                                        if (tokens.isDark) Color.White else Color(0xFF171717)
                                    } else tokens.textMuted.copy(alpha = 0.25f),
                                ) {
                                    Text(
                                        text = "Save",
                                        color = if (customName.isNotBlank()) {
                                            if (tokens.isDark) Color.Black else Color.White
                                        } else tokens.textMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (list.isEmpty() && !isAddingCustom) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = when (pageType) {
                                    SettingsModalType.CONNECTORS -> Icons.Rounded.Extension
                                    SettingsModalType.AGENTS -> Icons.Rounded.SmartToy
                                    else -> Icons.Rounded.Bolt
                                },
                                contentDescription = null,
                                tint = tokens.textMuted,
                                modifier = Modifier.size(30.dp),
                            )
                            Text(
                                text = "No ${pageType.title} Configured",
                                color = tokens.textTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Tap + in the top right to add one.",
                                color = tokens.textSub,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(list, key = { _, item -> item.id }) { _, item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.cardBg,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = tween(240, easing = NetRunnerEase),
                                fadeOutSpec = tween(160),
                                placementSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = tokens.textTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(item.detail, color = tokens.textSub, fontSize = 12.sp)
                            }
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Remove",
                                tint = Color(0xFFF87171),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { onDeleteIntegration(pageType, item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
