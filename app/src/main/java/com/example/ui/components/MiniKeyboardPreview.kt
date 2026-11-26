package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.BannerPresetCatalog
import com.example.data.CustomBackgroundManager
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.TouchTypingEffect
import com.example.engine.RgbLightingEngine

/**
 * Sleek miniature QWERTY keyboard-shaped preview (replaces uniform grid/ludo boxes):
 * - Authentic 4-row staggered mechanical/Gboard keycap proportions (10 / 9 staggered / Shift+7+Del / ?123+Space+Enter)
 * - Optional slim top toolbar strip bar
 * - Optional HD photo / banner / gradient backdrop beneath translucent frosted keycaps
 * - 60fps Canvas-only rendering for live Ambient RGB waves & Touch-Reactive ripples with zero layout churn.
 */
@Composable
fun MiniKeyboardEffectPreview(
    modifier: Modifier = Modifier,
    effect: RgbEffect = RgbEffect.NORMAL_DEFAULT,
    touchEffect: TouchTypingEffect = TouchTypingEffect.NONE,
    palette: RgbPalette = RgbPalette.SPECTRUM_PRISM,
    keycapStyle: KeycapStyle = KeycapStyle.PUDDING,
    glowIntensity: Float = 0.85f,
    animationSpeed: Float = 1.0f,
    showKeyBorders: Boolean = true,
    timeProvider: () -> Float = { 0f },
    bannerPresetId: String? = null,
    remoteImageUrl: String? = null,
    gradientColors: List<Long>? = null,
    customBitmap: ImageBitmap? = null,
    dimAlpha: Float = 0.34f,
    forceDefaultBlack: Boolean = false,
    showTopStrip: Boolean = true
) {
    val context = LocalContext.current
    val preset = remember(bannerPresetId, forceDefaultBlack) {
        if (forceDefaultBlack) null else BannerPresetCatalog.findById(bannerPresetId)
    }

    val hasBackdropArt = !forceDefaultBlack && (
        customBitmap != null ||
            !remoteImageUrl.isNullOrBlank() ||
            preset != null ||
            !gradientColors.isNullOrEmpty()
        )

    val gradientBrush = remember(gradientColors, preset) {
        val cols = gradientColors?.takeIf { it.size >= 2 }
            ?: preset?.previewColors?.takeIf { it.size >= 2 }
            ?: listOf(0xFF141824, 0xFF202638)
        Brush.linearGradient(cols.map { Color(it) })
    }

    val previewSettings = remember(effect, touchEffect, palette, keycapStyle, glowIntensity, animationSpeed, showKeyBorders) {
        KeyboardSettings(
            effect = effect,
            touchEffect = touchEffect,
            palette = palette,
            keycapStyle = keycapStyle,
            glowIntensity = glowIntensity,
            animationSpeed = animationSpeed,
            showKeyBorders = showKeyBorders
        )
    }

    val containerShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(containerShape)
            .background(Color(0xFF111319))
            .border(
                width = 0.9.dp,
                color = when {
                    touchEffect != TouchTypingEffect.NONE -> Color(touchEffect.primaryHex).copy(alpha = 0.55f)
                    effect != RgbEffect.NORMAL_DEFAULT -> palette.colors.firstOrNull()?.copy(alpha = 0.55f)
                        ?: Color.White.copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.16f)
                },
                shape = containerShape
            )
    ) {
        if (hasBackdropArt) {
            when {
                customBitmap != null -> {
                    Image(
                        bitmap = customBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                !remoteImageUrl.isNullOrBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush)
                    )
                    AsyncImage(
                        model = remoteImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                preset?.drawableRes != null -> {
                    Image(
                        painter = painterResource(id = preset.drawableRes),
                        contentDescription = preset.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                preset != null -> {
                    val procBmp = remember(preset.id) {
                        CustomBackgroundManager.getInstance(context).renderProceduralPresetBitmap(
                            presetId = preset.id,
                            w = 320,
                            h = 180
                        )
                    }
                    Image(
                        bitmap = procBmp.asImageBitmap(),
                        contentDescription = preset.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070A12).copy(alpha = dimAlpha.coerceIn(0.12f, 0.72f)))
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val nowSec = timeProvider()
            val padX = 5.dp.toPx()
            val padY = 4.5.dp.toPx()
            val innerW = (size.width - padX * 2f).coerceAtLeast(12f)
            val innerH = (size.height - padY * 2f).coerceAtLeast(12f)

            val stripHeight = if (showTopStrip) (innerH * 0.13f).coerceIn(3.dp.toPx(), 7.dp.toPx()) else 0f
            val stripGap = if (showTopStrip) 2.5.dp.toPx() else 0f

            if (showTopStrip) {
                val stripBg = if (hasBackdropArt) Color(0x52101420) else Color(0xFF191D26)
                drawRoundRect(
                    color = stripBg,
                    topLeft = Offset(padX, padY),
                    size = Size(innerW, stripHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                // Mini accent pill inside toolbar strip
                val accentBar = if (effect != RgbEffect.NORMAL_DEFAULT) {
                    RgbLightingEngine.computeKeyColor(
                        nx = 0.2f,
                        ny = 0.1f,
                        keyHash = 7,
                        timeSec = nowSec,
                        settings = previewSettings,
                        ripples = emptyList(),
                        isPressed = false,
                        keyLabel = ""
                    )
                } else if (touchEffect != TouchTypingEffect.NONE) {
                    Color(touchEffect.primaryHex)
                } else {
                    Color(0xFF00E5FF)
                }
                drawRoundRect(
                    color = accentBar.copy(alpha = 0.85f),
                    topLeft = Offset(padX + 4.dp.toPx(), padY + stripHeight * 0.25f),
                    size = Size((innerW * 0.18f).coerceAtLeast(10.dp.toPx()), stripHeight * 0.5f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            val deckTop = padY + stripHeight + stripGap
            val deckH = (innerH - stripHeight - stripGap).coerceAtLeast(10f)
            val rowGap = (deckH * 0.065f).coerceIn(1.5.dp.toPx(), 3.2.dp.toPx())
            val keyGap = (innerW * 0.015f).coerceIn(1.2.dp.toPx(), 2.6.dp.toPx())
            val rowH = ((deckH - rowGap * 3f) / 4f).coerceAtLeast(2f)
            val keyRadius = CornerRadius(2.8.dp.toPx(), 2.8.dp.toPx())
            val borderPx = 0.7.dp.toPx()
            val shadowOffsetPx = 1.1.dp.toPx()

            // Authentic staggered QWERTY key weight rows:
            // Row 0: 10 letter keys
            // Row 1: staggered 9 letter keys (0.45f spacer left/right)
            // Row 2: Shift(1.45) + 7 letters + Del(1.45)
            // Row 3: ?123(1.35) + comma(0.85) + Spacebar(4.3) + period(0.85) + Enter(1.35)
            val rowSpecs = listOf(
                listOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f),
                listOf(-0.45f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, -0.45f),
                listOf(1.45f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.45f),
                listOf(1.35f, 0.85f, 4.3f, 0.85f, 1.35f)
            )

            rowSpecs.forEachIndexed { rowIdx, weights ->
                val y = deckTop + rowIdx * (rowH + rowGap)
                val visibleCount = weights.count { it > 0f }
                val totalWeight = weights.sumOf { kotlin.math.abs(it).toDouble() }.toFloat().coerceAtLeast(1f)
                val totalGaps = (visibleCount - 1).coerceAtLeast(0) * keyGap
                val usableW = (innerW - totalGaps).coerceAtLeast(4f)

                var cursorX = padX
                var visibleCol = 0

                weights.forEach { rawW ->
                    if (rawW < 0f) {
                        cursorX += usableW * ((-rawW) / totalWeight)
                        return@forEach
                    }

                    val keyW = (usableW * (rawW / totalWeight)).coerceAtLeast(2f)
                    val nx = ((cursorX - padX + keyW * 0.5f) / innerW).coerceIn(0f, 1f)
                    val ny = ((rowIdx + 0.5f) / 4f).coerceIn(0f, 1f)

                    val isEnterKey = rowIdx == 3 && visibleCol == visibleCount - 1
                    val isSpaceKey = rowIdx == 3 && rawW >= 3f
                    val isModKey = (rowIdx == 2 && (visibleCol == 0 || visibleCol == visibleCount - 1)) ||
                        (rowIdx == 3 && visibleCol == 0)

                    val ambientCol = if (effect != RgbEffect.NORMAL_DEFAULT) {
                        RgbLightingEngine.computeKeyColor(
                            nx = nx,
                            ny = ny,
                            keyHash = rowIdx * 31 + visibleCol * 17,
                            timeSec = nowSec,
                            settings = previewSettings,
                            ripples = emptyList(),
                            isPressed = false,
                            keyLabel = "K"
                        )
                    } else Color.Transparent

                    val touchCol = if (touchEffect != TouchTypingEffect.NONE) {
                        RgbLightingEngine.computeTouchPreviewColor(
                            touchEffect = touchEffect,
                            nx = nx,
                            ny = ny,
                            row = rowIdx,
                            col = visibleCol,
                            rows = 4,
                            cols = visibleCount,
                            timeSec = nowSec,
                            palette = palette,
                            glowIntensity = glowIntensity
                        )
                    } else Color.Transparent

                    val isTouchLit = touchCol.alpha > 0.08f
                    val hasAmbient = ambientCol.alpha > 0.06f

                    val keyFill = when {
                        isTouchLit -> touchCol.copy(alpha = (touchCol.alpha * 0.78f).coerceIn(0.32f, 0.94f))
                        isEnterKey && !hasAmbient -> Color(0xFF4285F4).copy(alpha = if (hasBackdropArt) 0.85f else 0.95f)
                        hasAmbient && hasBackdropArt -> ambientCol.copy(alpha = 0.34f)
                        hasAmbient -> ambientCol.copy(alpha = 0.42f)
                        hasBackdropArt && isSpaceKey -> Color(0x46161B26)
                        hasBackdropArt && isModKey -> Color(0x52141822)
                        hasBackdropArt -> Color(0x3D181C26)
                        isSpaceKey -> Color(0xFF282C34)
                        isModKey -> Color(0xFF1A1D24)
                        else -> Color(0xFF232730)
                    }

                    val keyBorder = when {
                        isTouchLit -> touchCol
                        hasAmbient -> ambientCol.copy(alpha = 0.92f)
                        showKeyBorders && hasBackdropArt -> Color.White.copy(alpha = 0.30f)
                        showKeyBorders -> Color.White.copy(alpha = 0.16f)
                        else -> Color.Transparent
                    }

                    val tl = Offset(cursorX, y)
                    val sz = Size(keyW, rowH)

                    // Subtle Gboard keycap bottom drop shadow
                    if (showKeyBorders || isTouchLit || hasAmbient || isSpaceKey || isEnterKey) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.34f),
                            topLeft = Offset(tl.x, tl.y + shadowOffsetPx),
                            size = sz,
                            cornerRadius = keyRadius
                        )
                    }

                    drawRoundRect(
                        color = keyFill,
                        topLeft = tl,
                        size = sz,
                        cornerRadius = keyRadius
                    )

                    if (keyBorder != Color.Transparent) {
                        drawRoundRect(
                            color = keyBorder,
                            topLeft = tl,
                            size = sz,
                            cornerRadius = keyRadius,
                            style = Stroke(width = if (isTouchLit) borderPx * 1.35f else borderPx)
                        )
                    }

                    cursorX += keyW + keyGap
                    visibleCol++
                }
            }
        }
    }
}
