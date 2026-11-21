package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.RgbLightingEngine
import com.example.engine.RipplePulse
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.ui.theme.KeyCapSurface
import com.example.ui.theme.KeyChassisDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.isActive

@Composable
fun EffectsScreen(
    settings: KeyboardSettings,
    onSelectEffect: (RgbEffect) -> Unit,
    onSelectPalette: (RgbPalette) -> Unit,
    onSelectKeycapStyle: (KeycapStyle) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onGlowChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onIlluminatedLegendsChange: (Boolean) -> Unit,
    onChassisAuraChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var timeSec by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                timeSec = (now - startNanos) / 1_000_000_000f
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.effects_header),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.effects_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Live Mini Deck Visualizer showing current combined settings in real time
            LiveMiniDeckPreview(settings = settings, timeSec = timeSec)

            // Lighting Modes Grid (2 columns)
            SectionTitle(title = stringResource(R.string.section_lighting_mode))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RgbEffect.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { effect ->
                            EffectPreviewCard(
                                effect = effect,
                                settings = settings,
                                timeSec = timeSec,
                                selected = settings.effect == effect,
                                onClick = { onSelectEffect(effect) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Color Palettes Grid (2 columns)
            SectionTitle(title = stringResource(R.string.section_color_palette))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RgbPalette.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { palette ->
                            PaletteCard(
                                palette = palette,
                                selected = settings.palette == palette,
                                onClick = { onSelectPalette(palette) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Keycap Architecture Grid (2 columns)
            SectionTitle(title = stringResource(R.string.section_keycap_style))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeycapStyle.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { style ->
                            KeycapStyleCard(
                                style = style,
                                accentColor = RgbLightingEngine.samplePalette(settings.palette, timeSec * 0.25f),
                                selected = settings.keycapStyle == style,
                                onClick = { onSelectKeycapStyle(style) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Optical Tuning Sliders & Switches
            SectionTitle(title = stringResource(R.string.section_tuning))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TuningSliderRow(
                    label = stringResource(R.string.label_animation_speed),
                    valueLabel = String.format("%.1fx", settings.animationSpeed),
                    value = settings.animationSpeed,
                    valueRange = 0.2f..2.5f,
                    onValueChange = onSpeedChange,
                    testTag = "slider_animation_speed"
                )
                TuningSliderRow(
                    label = stringResource(R.string.label_glow_intensity),
                    valueLabel = "${(settings.glowIntensity * 100).toInt()}%",
                    value = settings.glowIntensity,
                    valueRange = 0.2f..1.0f,
                    onValueChange = onGlowChange,
                    testTag = "slider_glow_intensity"
                )
                TuningSliderRow(
                    label = stringResource(R.string.label_corner_radius),
                    valueLabel = "${settings.keyCornerRadiusDp.toInt()}dp",
                    value = settings.keyCornerRadiusDp,
                    valueRange = 2f..16f,
                    onValueChange = onCornerRadiusChange,
                    testTag = "slider_corner_radius"
                )

                ToggleSettingRow(
                    title = stringResource(R.string.label_legend_glow),
                    subtitle = stringResource(R.string.label_legend_glow_desc),
                    checked = settings.illuminatedLegends,
                    onCheckedChange = onIlluminatedLegendsChange,
                    testTag = "switch_illuminated_legends"
                )

                ToggleSettingRow(
                    title = stringResource(R.string.label_underglow_bar),
                    subtitle = stringResource(R.string.label_underglow_bar_desc),
                    checked = settings.chassisAura,
                    onCheckedChange = onChassisAuraChange,
                    testTag = "switch_chassis_aura"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LiveMiniDeckPreview(
    settings: KeyboardSettings,
    timeSec: Float
) {
    val barColors = RgbLightingEngine.computeBarColors(timeSec, settings)
    val demoRipples = remember(timeSec, settings.effect) {
        if (settings.effect == RgbEffect.REACTIVE_RIPPLE) {
            val cycle = (timeSec % 1.35f)
            val createdAt = timeSec - cycle
            listOf(RipplePulse(x = 0.5f, y = 0.5f, createdAtSec = createdAt, id = 1L))
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KeyChassisDark)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
    ) {
        if (settings.chassisAura) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Brush.horizontalGradient(barColors))
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            val cols = 10
            val rows = 3
            val gap = 5.dp.toPx()
            val keyW = (size.width - gap * (cols - 1)) / cols
            val keyH = (size.height - gap * (rows - 1)) / rows
            val radius = (settings.keyCornerRadiusDp * 0.7f).dp.toPx()
            val cr = CornerRadius(radius, radius)

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = c * (keyW + gap)
                    val y = r * (keyH + gap)
                    val nx = (c + 0.5f) / cols
                    val ny = (r + 0.5f) / rows
                    val idx = r * cols + c
                    val activeSparkIdx = ((timeSec * 2.4f).toInt() * 7) % (rows * cols)
                    val sparkAge = (timeSec * 2.4f) % 1f
                    val isSparked = settings.effect == RgbEffect.TOUCH_SPARK && idx == activeSparkIdx
                    val sparkStrength = if (isSparked) (1f - sparkAge * 0.75f).coerceIn(0f, 1f) else 0f

                    val color = RgbLightingEngine.computeKeyColor(
                        nx = nx,
                        ny = ny,
                        keyHash = r * 31 + c * 17,
                        timeSec = timeSec,
                        settings = settings,
                        ripples = demoRipples,
                        isPressed = false,
                        keyLabel = ('a' + (idx % 26)).toString(),
                        touchGlowStrength = sparkStrength
                    )

                    if (settings.effect == RgbEffect.TOUCH_SPARK) {
                        drawRoundRect(
                            color = Color(0xFF131620),
                            topLeft = Offset(x, y),
                            size = Size(keyW, keyH),
                            cornerRadius = cr
                        )
                        if (sparkStrength > 0.05f) {
                            drawRoundRect(
                                color = color.copy(alpha = sparkStrength * 0.45f),
                                topLeft = Offset(x, y),
                                size = Size(keyW, keyH),
                                cornerRadius = cr
                            )
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, y),
                                size = Size(keyW, keyH),
                                cornerRadius = cr,
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                        } else {
                            drawRoundRect(
                                color = Color(0xFF202434),
                                topLeft = Offset(x, y),
                                size = Size(keyW, keyH),
                                cornerRadius = cr,
                                style = Stroke(width = 0.8.dp.toPx())
                            )
                        }
                    } else {
                        when (settings.keycapStyle) {
                            KeycapStyle.PUDDING -> {
                                drawRoundRect(
                                    color = color.copy(alpha = 0.8f),
                                    topLeft = Offset(x, y),
                                    size = Size(keyW, keyH),
                                    cornerRadius = cr
                                )
                                drawRoundRect(
                                    color = Color(0xFF11141E),
                                    topLeft = Offset(x + 2f, y + 2f),
                                    size = Size((keyW - 4f).coerceAtLeast(2f), (keyH - 6f).coerceAtLeast(2f)),
                                    cornerRadius = cr
                                )
                            }
                            KeycapStyle.FLOATING_GLOW -> {
                                drawRoundRect(
                                    color = KeyCapSurface,
                                    topLeft = Offset(x, y),
                                    size = Size(keyW, keyH),
                                    cornerRadius = cr
                                )
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x, y),
                                    size = Size(keyW, keyH),
                                    cornerRadius = cr,
                                    style = Stroke(width = 1.4.dp.toPx())
                                )
                            }
                            KeycapStyle.NEON_WIREFRAME -> {
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x, y),
                                    size = Size(keyW, keyH),
                                    cornerRadius = cr,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                            KeycapStyle.SOLID_MECH -> {
                                drawRoundRect(
                                    color = Color(0xFF1C2133),
                                    topLeft = Offset(x, y),
                                    size = Size(keyW, keyH),
                                    cornerRadius = cr
                                )
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x + keyW * 0.25f, y + keyH - 4.dp.toPx()),
                                    size = Size(keyW * 0.5f, 2.dp.toPx()),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                    }
                }
            }
        }
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
private fun EffectPreviewCard(
    effect: RgbEffect,
    settings: KeyboardSettings,
    timeSec: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewSettings = remember(effect, settings.palette, settings.animationSpeed, settings.glowIntensity) {
        settings.copy(effect = effect)
    }
    val demoRipples = remember(timeSec, effect) {
        if (effect == RgbEffect.REACTIVE_RIPPLE) {
            val cycle = timeSec % 1.35f
            listOf(RipplePulse(x = 0.5f, y = 0.5f, createdAtSec = timeSec - cycle, id = 3L))
        } else {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) ObsidianSurfaceVariant else ObsidianSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) NeonCyan else ObsidianBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("effect_card_${effect.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianCanvas)
        ) {
            val cols = 7
            val rows = 2
            val gap = 3.dp.toPx()
            val pad = 4.dp.toPx()
            val kw = (size.width - pad * 2 - gap * (cols - 1)) / cols
            val kh = (size.height - pad * 2 - gap * (rows - 1)) / rows
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val idx = r * cols + c
                    val sparkIdx = ((timeSec * 2.2f).toInt() * 5) % (rows * cols)
                    val isTouchSpark = effect == RgbEffect.TOUCH_SPARK
                    val sparkVal = if (isTouchSpark && idx == sparkIdx) 1f else 0f
                    val col = RgbLightingEngine.computeKeyColor(
                        nx = (c + 0.5f) / cols,
                        ny = (r + 0.5f) / rows,
                        keyHash = r * 19 + c * 11,
                        timeSec = timeSec,
                        settings = previewSettings,
                        ripples = demoRipples,
                        isPressed = false,
                        keyLabel = ('q'.code + idx).toChar().toString(),
                        touchGlowStrength = sparkVal
                    )
                    drawRoundRect(
                        color = if (isTouchSpark && sparkVal <= 0f) Color(0xFF151824) else col,
                        topLeft = Offset(pad + c * (kw + gap), pad + r * (kh + gap)),
                        size = Size(kw, kh),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = effect.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) NeonCyan else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = effect.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaletteCard(
    palette: RgbPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ObsidianSurfaceVariant else ObsidianSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.colors.first() else ObsidianBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("palette_card_${palette.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(palette.colors))
        )
        Text(
            text = palette.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KeycapStyleCard(
    style: KeycapStyle,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ObsidianSurfaceVariant else ObsidianSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) NeonMagenta else ObsidianBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("keycap_style_${style.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mini single-keycap preview graphic
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cr = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                when (style) {
                    KeycapStyle.PUDDING -> {
                        drawRoundRect(color = accentColor.copy(alpha = 0.85f), cornerRadius = cr)
                        drawRoundRect(
                            color = Color(0xFF11141E),
                            topLeft = Offset(3.dp.toPx(), 2.dp.toPx()),
                            size = Size(size.width - 6.dp.toPx(), size.height - 7.dp.toPx()),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                    }
                    KeycapStyle.FLOATING_GLOW -> {
                        drawRoundRect(color = KeyCapSurface, cornerRadius = cr)
                        drawRoundRect(color = accentColor, cornerRadius = cr, style = Stroke(1.5.dp.toPx()))
                    }
                    KeycapStyle.NEON_WIREFRAME -> {
                        drawRoundRect(color = ObsidianCanvas, cornerRadius = cr)
                        drawRoundRect(color = accentColor, cornerRadius = cr, style = Stroke(1.8.dp.toPx()))
                    }
                    KeycapStyle.SOLID_MECH -> {
                        drawRoundRect(color = Color(0xFF1D2233), cornerRadius = cr)
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(size.width * 0.25f, size.height - 5.dp.toPx()),
                            size = Size(size.width * 0.5f, 2.dp.toPx()),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
            Text(
                text = "R",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) TextPrimary else TextSecondary
            )
            Text(
                text = style.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = NeonCyan
    )
}

@Composable
internal fun TuningSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = ObsidianBorder
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
internal fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF001B22),
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ObsidianSurfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
