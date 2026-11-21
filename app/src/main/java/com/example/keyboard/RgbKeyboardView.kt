package com.example.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.RgbLightingEngine
import com.example.engine.RipplePulse
import com.example.model.KeyboardLanguage
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KeyCapModifierSurface
import com.example.ui.theme.KeyCapPressedSurface
import com.example.ui.theme.KeyCapSurface
import com.example.ui.theme.KeyChassisDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun RgbKeyboardView(
    settings: KeyboardSettings,
    modifier: Modifier = Modifier,
    isImeMode: Boolean = false,
    imeAction: ImeActionType = ImeActionType.ENTER,
    shiftState: ShiftState = ShiftState.OFF,
    onCommitText: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onShiftTap: () -> Unit,
    onCursorMove: (Int) -> Unit = {},
    onFeedback: (isModifier: Boolean) -> Unit = {},
    onCycleEffect: () -> Unit = {},
    onCyclePalette: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onSwitchIme: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onHideKeyboard: () -> Unit = {}
) {
    var currentPage by remember { mutableStateOf(KeyboardPage.LETTERS) }
    var timeSec by remember { mutableFloatStateOf(0f) }
    val ripples = remember { mutableStateListOf<RipplePulse>() }
    var rippleSeq by remember { mutableLongStateOf(1L) }

    // 60fps RGB animation clock scoped to composition lifetime
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameNanos ->
                val elapsed = (frameNanos - startNanos) / 1_000_000_000f
                timeSec = elapsed
                if (ripples.isNotEmpty()) {
                    ripples.removeAll { elapsed - it.createdAtSec > 1.5f }
                }
            }
        }
    }

    val emitRipple: (Float, Float) -> Unit = { nx, ny ->
        rippleSeq += 1L
        if (ripples.size >= 6) {
            ripples.removeAt(0)
        }
        ripples.add(RipplePulse(x = nx, y = ny, createdAtSec = timeSec, id = rippleSeq))
    }

    val rows = remember(settings.language, currentPage, shiftState, settings.showNumberRow, imeAction) {
        KeyboardLayoutData.buildRows(
            language = settings.language,
            page = currentPage,
            shiftState = shiftState,
            showNumberRow = settings.showNumberRow,
            imeAction = imeAction
        )
    }

    val barColors = RgbLightingEngine.computeBarColors(timeSec, settings)
    val rowHeight = settings.heightPreset.rowHeightDp.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KeyChassisDark)
            .then(if (isImeMode) Modifier.navigationBarsPadding() else Modifier)
            .testTag("rgb_keyboard_root")
    ) {
        // Top chassis RGB aura bar
        if (settings.chassisAura) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Brush.horizontalGradient(barColors))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF1F2436))
            )
        }

        // Optional Quick RGB Toolbar
        if (settings.showToolbar) {
            KeyboardQuickToolbar(
                settings = settings,
                isImeMode = isImeMode,
                isEmojiOpen = currentPage == KeyboardPage.EMOJI,
                barColors = barColors,
                onCycleEffect = {
                    onFeedback(true)
                    onCycleEffect()
                },
                onCyclePalette = {
                    onFeedback(true)
                    onCyclePalette()
                },
                onToggleLanguage = {
                    onFeedback(true)
                    onToggleLanguage()
                },
                onToggleEmoji = {
                    onFeedback(true)
                    currentPage = if (currentPage == KeyboardPage.EMOJI) {
                        KeyboardPage.LETTERS
                    } else {
                        KeyboardPage.EMOJI
                    }
                },
                onSwitchIme = {
                    onFeedback(true)
                    onSwitchIme()
                },
                onOpenSettings = {
                    onFeedback(true)
                    onOpenSettings()
                },
                onHideKeyboard = {
                    onFeedback(true)
                    onHideKeyboard()
                }
            )
        }

        if (currentPage == KeyboardPage.EMOJI) {
            EmojiDrawerSection(
                settings = settings,
                timeSec = timeSec,
                rowHeight = rowHeight,
                onEmojiSelected = { emoji ->
                    onFeedback(false)
                    onCommitText(emoji)
                },
                onBackToLetters = {
                    onFeedback(true)
                    currentPage = KeyboardPage.LETTERS
                },
                onBackspace = {
                    onFeedback(true)
                    onBackspace()
                }
            )
        } else {
            // Keycap Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val totalRows = rows.size.coerceAtLeast(1)
                rows.forEachIndexed { rowIdx, rowKeys ->
                    val ny = (rowIdx + 0.5f) / totalRows.toFloat()
                    val isSecondEnglishLetterRow = settings.language == KeyboardLanguage.ENGLISH &&
                        currentPage == KeyboardPage.LETTERS &&
                        ((settings.showNumberRow && rowIdx == 2) || (!settings.showNumberRow && rowIdx == 1))
                    val horizontalInset = if (isSecondEnglishLetterRow) 14.dp else 0.dp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalInset),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val totalWeight = rowKeys.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
                        var runningWeight = 0f

                        rowKeys.forEach { keySpec ->
                            val nx = (runningWeight + keySpec.weight * 0.5f) / totalWeight
                            runningWeight += keySpec.weight

                            val isShiftActive = keySpec.action == KeyAction.Shift && shiftState != ShiftState.OFF
                            val isCapsLocked = keySpec.action == KeyAction.Shift && shiftState == ShiftState.CAPS_LOCK

                            RgbKeyCap(
                                keySpec = keySpec,
                                nx = nx,
                                ny = ny,
                                timeSec = timeSec,
                                settings = settings,
                                ripples = ripples,
                                rowHeight = rowHeight,
                                isHighlighted = isShiftActive || keySpec.action == KeyAction.Enter,
                                isCapsLocked = isCapsLocked,
                                modifier = Modifier.weight(keySpec.weight),
                                onCursorMove = onCursorMove,
                                onKeyTrigger = {
                                    emitRipple(nx, ny)
                                    onFeedback(keySpec.isModifier)
                                    when (val action = keySpec.action) {
                                        is KeyAction.Character -> onCommitText(action.text)
                                        KeyAction.Backspace -> onBackspace()
                                        KeyAction.Space -> onSpace()
                                        KeyAction.Enter -> onEnter()
                                        KeyAction.Shift -> onShiftTap()
                                        is KeyAction.SwitchPage -> currentPage = action.page
                                        KeyAction.ToggleEmoji -> currentPage = KeyboardPage.EMOJI
                                        KeyAction.ToggleLanguage -> onToggleLanguage()
                                        KeyAction.SwitchIme -> onSwitchIme()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Subtle bottom chassis RGB reflection strip
        if (settings.chassisAura) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Brush.horizontalGradient(barColors.reversed()))
            )
        }
    }
}

@Composable
private fun KeyboardQuickToolbar(
    settings: KeyboardSettings,
    isImeMode: Boolean,
    isEmojiOpen: Boolean,
    barColors: List<Color>,
    onCycleEffect: () -> Unit,
    onCyclePalette: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleEmoji: () -> Unit,
    onSwitchIme: () -> Unit,
    onOpenSettings: () -> Unit,
    onHideKeyboard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface.copy(alpha = 0.92f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Live RGB Effect & Palette quick cyclers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF171B2A))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(barColors),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onCycleEffect)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
                    .testTag("toolbar_cycle_effect"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = stringResource(R.string.cd_cycle_effect),
                    tint = barColors.firstOrNull() ?: Color.Cyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = settings.effect.shortLabel.uppercase(),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF171B2A))
                    .border(1.dp, Color(0xFF282E45), RoundedCornerShape(10.dp))
                    .clickable(onClick = onCyclePalette)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("toolbar_cycle_palette"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(settings.palette.colors))
                )
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = stringResource(R.string.cd_cycle_palette),
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Quick EN / বাং Language Switch Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF171B2A))
                    .border(1.dp, Color(0xFF282E45), RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleLanguage)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("toolbar_toggle_language"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = settings.language.shortBadge,
                    fontFamily = if (settings.language == KeyboardLanguage.BENGALI) FontFamily.Default else JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColors.lastOrNull() ?: Color.Cyan,
                    maxLines = 1
                )
            }
        }

        // Right: Emoji, Language/IME Switch, Studio Settings, Hide
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ToolbarIconAction(
                icon = Icons.Filled.EmojiEmotions,
                contentDescription = stringResource(R.string.cd_emoji),
                tint = if (isEmojiOpen) (barColors.firstOrNull() ?: Color.Cyan) else TextSecondary,
                testTag = "toolbar_emoji_btn",
                onClick = onToggleEmoji
            )
            if (isImeMode) {
                ToolbarIconAction(
                    icon = Icons.Filled.Language,
                    contentDescription = stringResource(R.string.cd_switch_ime),
                    tint = TextSecondary,
                    testTag = "toolbar_switch_ime_btn",
                    onClick = onSwitchIme
                )
                ToolbarIconAction(
                    icon = Icons.Filled.Tune,
                    contentDescription = stringResource(R.string.cd_open_app),
                    tint = TextSecondary,
                    testTag = "toolbar_open_settings_btn",
                    onClick = onOpenSettings
                )
                ToolbarIconAction(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_hide_keyboard),
                    tint = TextSecondary,
                    testTag = "toolbar_hide_keyboard_btn",
                    onClick = onHideKeyboard
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RgbKeyCap(
    keySpec: KeySpec,
    nx: Float,
    ny: Float,
    timeSec: Float,
    settings: KeyboardSettings,
    ripples: List<RipplePulse>,
    rowHeight: Dp,
    isHighlighted: Boolean,
    isCapsLocked: Boolean,
    modifier: Modifier = Modifier,
    onCursorMove: (Int) -> Unit,
    onKeyTrigger: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val latestTrigger by rememberUpdatedState(onKeyTrigger)
    val latestCursorMove by rememberUpdatedState(onCursorMove)
    val scope = rememberCoroutineScope()

    val glowColor = RgbLightingEngine.computeKeyColor(
        nx = nx,
        ny = ny,
        keyHash = keySpec.id.hashCode(),
        timeSec = timeSec,
        settings = settings,
        ripples = ripples,
        isPressed = isPressed
    )

    val cornerDp = settings.keyCornerRadiusDp.dp
    val isBackspace = keySpec.action == KeyAction.Backspace
    val isSpace = keySpec.action == KeyAction.Space
    val isEnter = keySpec.action == KeyAction.Enter
    val isShift = keySpec.action == KeyAction.Shift

    Box(
        modifier = modifier
            .height(rowHeight)
            .testTag("key_${keySpec.id}")
            .pointerInput(keySpec.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true

                    if (isBackspace) {
                        latestTrigger()
                        val repeatJob = scope.launch {
                            delay(360L)
                            while (isActive) {
                                latestTrigger()
                                delay(58L)
                            }
                        }
                        waitForUpOrCancellation()
                        repeatJob.cancel()
                        isPressed = false
                    } else if (isSpace) {
                        var totalDragX = 0f
                        var didCursorSwipe = false
                        var lastX = down.position.x
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                break
                            }
                            val dx = change.position.x - lastX
                            lastX = change.position.x
                            totalDragX += dx
                            if (kotlin.math.abs(totalDragX) > 28f) {
                                val steps = (totalDragX / 28f).toInt()
                                if (steps != 0) {
                                    didCursorSwipe = true
                                    latestCursorMove(steps)
                                    totalDragX -= steps * 28f
                                }
                            }
                        }
                        isPressed = false
                        if (!didCursorSwipe) {
                            latestTrigger()
                        }
                    } else {
                        val up = waitForUpOrCancellation()
                        isPressed = false
                        if (up != null) {
                            latestTrigger()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Custom Canvas for crisp 60fps mechanical keycap optics without layout thrashing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radiusPx = cornerDp.toPx()
            val cr = CornerRadius(radiusPx, radiusPx)
            val w = size.width
            val h = size.height
            val pressOffset = if (isPressed) 1.5.dp.toPx() else 0f

            val baseFill = when {
                isPressed -> KeyCapPressedSurface
                isEnter -> glowColor.copy(alpha = 0.28f)
                keySpec.isModifier -> KeyCapModifierSurface
                else -> KeyCapSurface
            }

            when (settings.keycapStyle) {
                KeycapStyle.PUDDING -> {
                    // Radiant glowing translucent lower pudding skirt
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glowColor.copy(alpha = (0.35f * settings.glowIntensity).coerceIn(0.15f, 0.7f)),
                                glowColor.copy(alpha = (0.85f * settings.glowIntensity).coerceIn(0.35f, 0.98f))
                            )
                        ),
                        topLeft = Offset(0f, pressOffset),
                        size = Size(w, h - pressOffset),
                        cornerRadius = cr
                    )
                    // Dark opaque top keycap face floating inside
                    val insetX = 2.dp.toPx()
                    val insetTop = 1.5.dp.toPx() + pressOffset
                    val insetBottom = 4.5.dp.toPx()
                    val innerH = (h - insetTop - insetBottom).coerceAtLeast(4f)
                    drawRoundRect(
                        color = if (isPressed) Color(0xFF1F2538) else Color(0xFF11141E),
                        topLeft = Offset(insetX, insetTop),
                        size = Size((w - insetX * 2).coerceAtLeast(4f), innerH),
                        cornerRadius = CornerRadius((radiusPx - 2f).coerceAtLeast(2f))
                    )
                    // Subtle top specular highlight
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (isPressed) 0.16f else 0.06f),
                        topLeft = Offset(insetX, insetTop),
                        size = Size((w - insetX * 2).coerceAtLeast(4f), innerH),
                        cornerRadius = CornerRadius((radiusPx - 2f).coerceAtLeast(2f)),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }

                KeycapStyle.FLOATING_GLOW -> {
                    // Ambient underglow shadow behind keycap
                    drawRoundRect(
                        color = glowColor.copy(alpha = (0.24f * settings.glowIntensity).coerceIn(0.1f, 0.45f)),
                        topLeft = Offset(0f, 1.5.dp.toPx()),
                        size = Size(w, h - 1.5.dp.toPx()),
                        cornerRadius = cr
                    )
                    // Main obsidian body
                    drawRoundRect(
                        color = baseFill,
                        topLeft = Offset(0.5.dp.toPx(), pressOffset),
                        size = Size(w - 1.dp.toPx(), h - 2.dp.toPx() - pressOffset),
                        cornerRadius = cr
                    )
                    // Razor RGB rim stroke
                    drawRoundRect(
                        color = glowColor.copy(alpha = if (isPressed) 1f else 0.85f),
                        topLeft = Offset(0.5.dp.toPx(), pressOffset),
                        size = Size(w - 1.dp.toPx(), h - 2.dp.toPx() - pressOffset),
                        cornerRadius = cr,
                        style = Stroke(width = if (isHighlighted || isPressed) 1.5.dp.toPx() else 1.dp.toPx())
                    )
                }

                KeycapStyle.NEON_WIREFRAME -> {
                    drawRoundRect(
                        color = if (isPressed) glowColor.copy(alpha = 0.35f) else Color(0xFF0A0C13),
                        topLeft = Offset(0f, pressOffset),
                        size = Size(w, h - pressOffset),
                        cornerRadius = cr
                    )
                    drawRoundRect(
                        color = glowColor.copy(alpha = if (isPressed) 1f else 0.92f),
                        topLeft = Offset(0.8.dp.toPx(), 0.8.dp.toPx() + pressOffset),
                        size = Size(w - 1.6.dp.toPx(), h - 1.6.dp.toPx() - pressOffset),
                        cornerRadius = cr,
                        style = Stroke(width = if (isPressed || isHighlighted) 1.7.dp.toPx() else 1.15.dp.toPx())
                    )
                }

                KeycapStyle.SOLID_MECH -> {
                    // Bottom bevel shadow
                    drawRoundRect(
                        color = Color(0xFF06070B),
                        topLeft = Offset(0f, 2.dp.toPx()),
                        size = Size(w, h - 2.dp.toPx()),
                        cornerRadius = cr
                    )
                    // Sculpted PBT keycap face
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isPressed) Color(0xFF282F45) else Color(0xFF1D2233),
                                if (isPressed) Color(0xFF1C2133) else Color(0xFF141826)
                            )
                        ),
                        topLeft = Offset(0f, pressOffset),
                        size = Size(w, h - 2.5.dp.toPx() - pressOffset),
                        cornerRadius = cr
                    )
                    // Subtle bottom LED accent slit
                    drawRoundRect(
                        color = glowColor.copy(alpha = if (isPressed) 1f else 0.85f),
                        topLeft = Offset(w * 0.22f, h - 5.dp.toPx()),
                        size = Size(w * 0.56f, 2.dp.toPx()),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }

        // Secondary corner number hint when number row is hidden
        if (keySpec.secondaryHint != null) {
            Text(
                text = keySpec.secondaryHint,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 8.sp,
                color = TextSecondary.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 4.dp)
            )
        }

        // Keycap Legend / Icon
        val legendColor = when {
            isPressed -> Color.White
            isEnter -> Color.White
            settings.illuminatedLegends -> glowColor.copy(alpha = 1f)
            else -> TextPrimary
        }

        when {
            isShift -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardCapslock,
                        contentDescription = stringResource(R.string.cd_shift),
                        tint = if (isHighlighted) glowColor.copy(alpha = 1f) else legendColor,
                        modifier = Modifier.size(19.dp)
                    )
                    if (isCapsLocked) {
                        Box(
                            modifier = Modifier
                                .size(width = 12.dp, height = 2.dp)
                                .clip(CircleShape)
                                .background(glowColor.copy(alpha = 1f))
                        )
                    }
                }
            }

            isBackspace -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.cd_backspace),
                    tint = legendColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            isEnter -> {
                if (keySpec.label == "↵") {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = stringResource(R.string.cd_enter),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = keySpec.label.uppercase(),
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            isSpace -> {
                Text(
                    text = keySpec.label,
                    fontFamily = if (keySpec.isBengaliGlyph) FontFamily.Default else JetBrainsMonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = if (keySpec.isBengaliGlyph) 0.4.sp else 1.4.sp,
                    color = legendColor.copy(alpha = 0.78f),
                    maxLines = 1
                )
            }

            else -> {
                val resolvedFont = when {
                    keySpec.isBengaliGlyph -> FontFamily.Default
                    keySpec.isModifier -> SpaceGroteskFamily
                    else -> JetBrainsMonoFamily
                }
                val resolvedSize = when {
                    keySpec.isBengaliGlyph && keySpec.isModifier -> 11.sp
                    keySpec.isBengaliGlyph -> 17.sp
                    keySpec.label.length > 2 -> 11.sp
                    else -> 16.sp
                }
                Text(
                    text = keySpec.label,
                    fontFamily = resolvedFont,
                    fontWeight = if (keySpec.isModifier) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = resolvedSize,
                    color = legendColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmojiDrawerSection(
    settings: KeyboardSettings,
    timeSec: Float,
    rowHeight: Dp,
    onEmojiSelected: (String) -> Unit,
    onBackToLetters: () -> Unit,
    onBackspace: () -> Unit
) {
    val accent = RgbLightingEngine.samplePalette(settings.palette, timeSec * 0.25f)
    val drawerHeight = rowHeight * 4 + 28.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(drawerHeight)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(KeyboardLayoutData.curatedEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KeyCapSurface)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KeyCapModifierSurface)
                    .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackToLetters)
                    .padding(horizontal = 18.dp)
                    .testTag("emoji_back_abc"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = accent
                )
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .width(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KeyCapModifierSurface)
                    .clickable(onClick = onBackspace)
                    .testTag("emoji_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.cd_backspace),
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
