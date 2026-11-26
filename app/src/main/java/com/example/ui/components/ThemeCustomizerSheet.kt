package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.BannerPresetCatalog
import com.example.data.ComboThemePreset
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.engine.RgbLightingEngine
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.TouchTypingEffect
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.luminousGlassCard
import java.io.File

@Immutable
data class CustomizerTarget(
    val title: String,
    val subtitle: String,
    val badge: String,
    val bannerPresetId: String? = null,
    val combo: ComboThemePreset? = null,
    val effect: RgbEffect,
    val touchEffect: TouchTypingEffect,
    val palette: RgbPalette,
    val previewColors: List<Long> = emptyList(),
    val remoteImageUrl: String? = null,
    val allowBackdropSwitch: Boolean = false,
    val forceDefaultBlackBackdrop: Boolean = false,
    val isLocked: Boolean = false
)

private val previewRow1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
private val previewRow1Digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val previewRow2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
private val previewRow3 = listOf("⇧", "Z", "X", "C", "V", "B", "N", "M", "⌫")

/**
 * Rich Gboard-style full mini-keyboard preview showing:
 * - Top Toolbar Strip (`Rakib official`, 😊, 📋, Aa, 🎙️, ⚙️, ✨ AI)
 * - 6dp rounded keycaps with 2dp soft drop shadow, 0.22f translucent frosted glass over photos,
 *   #22252A / #1A1D22 charcoal on Default Black, 0.8dp thin outline when Key borders ON,
 *   1-0 top-right micro-digits on QWERTY top row, and Gboard blue circle Enter key.
 */
@Composable
fun FullMiniKeyboardPreview(
    settings: KeyboardSettings,
    timeSec: Float,
    customPhotoTheme: CustomPhotoTheme? = null,
    overrideBannerPresetId: String? = null,
    overrideRemoteImageUrl: String? = null,
    overrideGradientColors: List<Long>? = null,
    overrideCustomBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    overrideDimAlpha: Float? = null,
    overrideEffect: RgbEffect = settings.effect,
    overrideTouchEffect: TouchTypingEffect = settings.touchEffect,
    overridePalette: RgbPalette = settings.palette,
    showKeyBorders: Boolean = settings.showKeyBorders,
    forceDefaultBlack: Boolean = false,
    isInteractive: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastTapRowCol by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var lastTapTimeSec by remember { mutableFloatStateOf(-10f) }
    val tapAgeSec = if (lastTapRowCol != null) (timeSec - lastTapTimeSec).coerceAtLeast(0f) else null

    val effectiveSettings = remember(settings, overrideEffect, overrideTouchEffect, overridePalette, showKeyBorders) {
        settings.copy(
            effect = overrideEffect,
            touchEffect = overrideTouchEffect,
            palette = overridePalette,
            showKeyBorders = showKeyBorders
        )
    }

    val resolvedPresetId = when {
        forceDefaultBlack -> null
        overrideCustomBitmap != null -> null
        !overrideBannerPresetId.isNullOrBlank() -> overrideBannerPresetId
        customPhotoTheme?.hasPreset == true -> customPhotoTheme.presetId
        else -> null
    }
    val bannerPreset = remember(resolvedPresetId) { BannerPresetCatalog.findById(resolvedPresetId) }

    val customFileBitmap = remember(customPhotoTheme?.cacheKey, resolvedPresetId, forceDefaultBlack, overrideCustomBitmap) {
        when {
            forceDefaultBlack || resolvedPresetId != null -> null
            overrideCustomBitmap != null -> overrideCustomBitmap
            else -> {
                val path = customPhotoTheme?.photoPath
                if (!path.isNullOrBlank() && File(path).exists()) {
                    runCatching {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
                    }.getOrNull()
                } else null
            }
        }
    }

    val cornerShape = RoundedCornerShape(if (compact) 12.dp else 18.dp)
    val keyCornerDp = if (compact) 4.dp else 6.dp
    val keyCorner = RoundedCornerShape(keyCornerDp)
    val keyHeight: Dp = if (compact) 21.dp else 33.dp
    val rowGap: Dp = if (compact) 3.dp else 5.5.dp
    val keyGap: Dp = if (compact) 2.5.dp else 4.dp
    val fontSize = if (compact) 8.5.sp else 12.5.sp
    val topBarHeight: Dp = if (compact) 18.dp else 28.dp

    val hasAnyBackgroundArt = !forceDefaultBlack && (
        bannerPreset != null ||
            customFileBitmap != null ||
            !overrideRemoteImageUrl.isNullOrBlank() ||
            !overrideGradientColors.isNullOrEmpty()
        )
    val dimAlpha = (overrideDimAlpha ?: customPhotoTheme?.dimAlpha ?: CustomBackgroundManager.DEFAULT_DIM_ALPHA)
        .coerceIn(CustomBackgroundManager.MIN_DIM_ALPHA, CustomBackgroundManager.MAX_DIM_ALPHA)

    val gradientBrush = remember(overrideGradientColors, bannerPreset) {
        val cols = overrideGradientColors?.takeIf { it.size >= 2 }
            ?: bannerPreset?.previewColors?.takeIf { it.size >= 2 }
            ?: listOf(0xFF121418, 0xFF1A1E26)
        Brush.linearGradient(cols.map { Color(it) })
    }

    val perimeterColors = remember(timeSec, effectiveSettings) {
        RgbLightingEngine.computeBarColors(timeSec, effectiveSettings, stops = 5)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cornerShape)
            .background(Color(0xFF121418))
            .border(
                width = 1.dp,
                brush = if (overrideEffect != RgbEffect.NORMAL_DEFAULT) {
                    Brush.horizontalGradient(perimeterColors)
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF282C34), Color(0xFF282C34)))
                },
                shape = cornerShape
            )
    ) {
        // Backdrop Layer: Photo / Banner / Gradient / Pure Gboard Charcoal Black
        when {
            forceDefaultBlack -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF121418))
                )
            }
            customFileBitmap != null -> {
                Image(
                    bitmap = customFileBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF080A10).copy(alpha = dimAlpha))
                )
            }
            !overrideRemoteImageUrl.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(gradientBrush)
                )
                AsyncImage(
                    model = overrideRemoteImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF080A10).copy(alpha = dimAlpha))
                )
            }
            bannerPreset?.drawableRes != null -> {
                Image(
                    painter = painterResource(id = bannerPreset.drawableRes),
                    contentDescription = bannerPreset.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF080A10).copy(alpha = dimAlpha))
                )
            }
            bannerPreset != null -> {
                val proceduralBitmap = remember(bannerPreset.id) {
                    CustomBackgroundManager.getInstance(context).renderProceduralPresetBitmap(
                        presetId = bannerPreset.id,
                        w = 480,
                        h = 240
                    )
                }
                Image(
                    bitmap = proceduralBitmap.asImageBitmap(),
                    contentDescription = bannerPreset.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF080A10).copy(alpha = dimAlpha))
                )
            }
            !overrideGradientColors.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(gradientBrush)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF080A10).copy(alpha = dimAlpha))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 5.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(rowGap)
        ) {
            // 1. Gboard-Style Top Toolbar Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (hasAnyBackgroundArt) Color(0x4D10141E) else Color(0xFF191C22)
                    )
                    .padding(horizontal = if (compact) 5.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rakib official",
                    fontSize = if (compact) 7.5.sp else 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "😊", fontSize = if (compact) 8.sp else 11.sp)
                    Text(text = "📋", fontSize = if (compact) 8.sp else 11.sp)
                    Text(
                        text = "Aa",
                        fontSize = if (compact) 8.sp else 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(text = "🎙️", fontSize = if (compact) 8.sp else 11.sp)
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(if (compact) 10.dp else 13.dp)
                    )
                    Text(
                        text = "✨ AI",
                        fontSize = if (compact) 7.5.sp else 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFD81B60), Color(0xFFFF8F00))))
                            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 1.dp)
                    )
                }
            }

            // 2. Letter Rows 1..3 (Authentic Gboard keycaps + 2dp shadow + 1-0 micro-digits on Row 1)
            listOf(previewRow1, previewRow2, previewRow3).forEachIndexed { rowIdx, keys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(keyGap)
                ) {
                    if (rowIdx == 1 && !compact) {
                        Spacer(modifier = Modifier.weight(0.35f))
                    }
                    keys.forEachIndexed { colIdx, label ->
                        val isWideMod = label == "⇧" || label == "⌫"
                        val nx = (colIdx + 0.5f) / keys.size.toFloat()
                        val ny = (rowIdx + 0.5f) / 4f
                        val ambientCol = if (overrideEffect != RgbEffect.NORMAL_DEFAULT) {
                            RgbLightingEngine.computeKeyColor(
                                nx = nx,
                                ny = ny,
                                keyHash = rowIdx * 31 + colIdx * 17,
                                timeSec = timeSec,
                                settings = effectiveSettings,
                                ripples = emptyList(),
                                isPressed = false,
                                keyLabel = label
                            )
                        } else Color.Transparent

                        val touchCol = RgbLightingEngine.computeTouchPreviewColor(
                            touchEffect = overrideTouchEffect,
                            nx = nx,
                            ny = ny,
                            row = rowIdx,
                            col = colIdx,
                            rows = 4,
                            cols = keys.size,
                            timeSec = timeSec,
                            palette = overridePalette,
                            glowIntensity = effectiveSettings.glowIntensity,
                            explicitTap = lastTapRowCol,
                            explicitTapAgeSec = tapAgeSec
                        )

                        val isKeyLit = touchCol.alpha > 0.08f
                        val hasAmbient = ambientCol.alpha > 0.05f
                        val baseCapColor = when {
                            isKeyLit -> touchCol.copy(alpha = (touchCol.alpha * 0.72f).coerceIn(0.28f, 0.92f))
                            hasAmbient && effectiveSettings.keycapStyle == KeycapStyle.PUDDING ->
                                ambientCol.copy(alpha = 0.28f)
                            hasAmbient -> Color(0xCC161A29)
                            hasAnyBackgroundArt && !showKeyBorders -> Color.Transparent
                            hasAnyBackgroundArt && isWideMod -> Color(0x48161A24)
                            hasAnyBackgroundArt -> Color(0x38181C26) // ~0.22f frosted glass
                            !showKeyBorders -> Color.Transparent
                            isWideMod -> Color(0xFF1A1D22)
                            else -> Color(0xFF22252A)
                        }
                        val borderColor = when {
                            isKeyLit -> touchCol
                            hasAmbient && showKeyBorders -> ambientCol
                            hasAmbient && !showKeyBorders -> Color.Transparent
                            showKeyBorders -> Color.White.copy(alpha = if (hasAnyBackgroundArt) 0.32f else 0.18f)
                            else -> Color.Transparent
                        }
                        val showDropShadow = showKeyBorders || isKeyLit || hasAmbient

                        Box(
                            modifier = Modifier
                                .weight(if (isWideMod) 1.45f else 1f)
                                .height(keyHeight)
                                .then(
                                    if (showDropShadow && !compact) {
                                        Modifier.shadow(
                                            elevation = 2.dp,
                                            shape = keyCorner,
                                            clip = false,
                                            ambientColor = Color.Black,
                                            spotColor = Color.Black
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clip(keyCorner)
                                .background(baseCapColor)
                                .then(
                                    if (borderColor != Color.Transparent) {
                                        Modifier.border(
                                            width = if (isKeyLit) 1.2.dp else 0.8.dp,
                                            color = borderColor,
                                            shape = keyCorner
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (isInteractive) {
                                        Modifier.clickable {
                                            lastTapRowCol = rowIdx to colIdx
                                            lastTapTimeSec = timeSec
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rowIdx == 0 && colIdx < previewRow1Digits.size && !compact) {
                                Text(
                                    text = previewRow1Digits[colIdx],
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasAnyBackgroundArt) Color(0xD8EEF0F8) else Color(0xFF9AA0A6),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 3.dp, top = 1.dp),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = label,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = fontSize,
                                fontWeight = if (isKeyLit || isWideMod) FontWeight.Bold else FontWeight.Medium,
                                style = if (hasAnyBackgroundArt) {
                                    TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 3f
                                        )
                                    )
                                } else {
                                    TextStyle.Default
                                },
                                color = Color(0xFFF1F3F4),
                                modifier = if (rowIdx == 0 && !compact) Modifier.padding(top = 3.dp) else Modifier,
                                maxLines = 1
                            )
                        }
                    }
                    if (rowIdx == 1 && !compact) {
                        Spacer(modifier = Modifier.weight(0.35f))
                    }
                }
            }

            // 3. Exact Gboard Bottom Bar (?123, comma, Globe, Spacebar, period, Blue Circle Enter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(keyGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bottomItems = listOf(
                    "?123" to 1.25f,
                    "," to 0.8f,
                    "🌐" to 0.85f,
                    "Rakib official" to 4.15f,
                    "." to 0.8f
                )
                bottomItems.forEachIndexed { idx, (label, w) ->
                    val touchCol = RgbLightingEngine.computeTouchPreviewColor(
                        touchEffect = overrideTouchEffect,
                        nx = (idx + 0.5f) / 6f,
                        ny = 0.88f,
                        row = 3,
                        col = idx,
                        rows = 4,
                        cols = 6,
                        timeSec = timeSec,
                        palette = overridePalette,
                        glowIntensity = effectiveSettings.glowIntensity,
                        explicitTap = lastTapRowCol,
                        explicitTapAgeSec = tapAgeSec
                    )
                    val isSpace = label == "Rakib official"
                    val isLit = touchCol.alpha > 0.08f
                    val bg = when {
                        isLit -> touchCol.copy(alpha = 0.68f)
                        hasAnyBackgroundArt && isSpace -> Color(0x3A181C26) // ~0.22f frosted spacebar
                        hasAnyBackgroundArt && !showKeyBorders -> Color.Transparent
                        hasAnyBackgroundArt -> Color(0x48161A24)
                        !showKeyBorders && isSpace -> Color(0xFF22252A)
                        !showKeyBorders -> Color.Transparent
                        isSpace -> Color(0xFF22252A)
                        else -> Color(0xFF1A1D22)
                    }
                    val border = when {
                        isLit -> touchCol
                        showKeyBorders -> Color.White.copy(alpha = if (hasAnyBackgroundArt) 0.30f else 0.18f)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .weight(w)
                            .height(keyHeight)
                            .then(
                                if ((showKeyBorders || isSpace || isLit) && !compact) {
                                    Modifier.shadow(
                                        elevation = 2.dp,
                                        shape = keyCorner,
                                        clip = false,
                                        ambientColor = Color.Black,
                                        spotColor = Color.Black
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clip(keyCorner)
                            .background(bg)
                            .then(
                                if (border != Color.Transparent) {
                                    Modifier.border(0.8.dp, border, keyCorner)
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (isInteractive) {
                                    Modifier.clickable {
                                        lastTapRowCol = 3 to idx
                                        lastTapTimeSec = timeSec
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = if (isSpace && compact) 7.sp else if (isSpace) 9.5.sp else fontSize,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF1F3F4).copy(alpha = if (isSpace) 0.84f else 0.94f),
                            maxLines = 1
                        )
                    }
                }

                // Gboard circular blue Enter / Search action button (#4285F4 with 2dp shadow)
                Box(
                    modifier = Modifier
                        .size(keyHeight)
                        .shadow(if (compact) 1.dp else 2.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4))
                        .then(
                            if (isInteractive) {
                                Modifier.clickable {
                                    lastTapRowCol = 3 to 5
                                    lastTapTimeSec = timeSec
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↵",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = if (compact) 10.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Gboard-Style Full Keyboard Theme Customizer Modal:
 * - Interactive FullMiniKeyboardPreview (auto-animates & bursts immediately on key tap)
 * - Optional Default Black vs Custom Photo backdrop toggle (for Typing Effect previews)
 * - Key borders toggle Switch
 * - Cancel & Apply actions
 */
@Composable
fun ThemeCustomizerModal(
    target: CustomizerTarget,
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    timeSec: Float,
    onDismiss: () -> Unit,
    onApply: (showKeyBorders: Boolean, useDefaultBlack: Boolean) -> Unit,
    onOpenVipUnlock: () -> Unit
) {
    var localKeyBorders by remember(target, settings.showKeyBorders) {
        mutableStateOf(settings.showKeyBorders)
    }
    var localDefaultBlack by remember(target) {
        mutableStateOf(target.forceDefaultBlackBackdrop)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 560.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    borderBrush = if (target.isLocked) FlagshipGoldCyanBrush else FlagshipCyanMagentaBrush,
                    borderWidth = 1.5.dp,
                    glowColor = if (target.isLocked) RoyalGold else NeonCyan,
                    glowElevation = 18.dp
                )
                .padding(18.dp)
                .testTag("theme_customizer_modal"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = target.badge,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF102438))
                                .border(0.8.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        if (target.isLocked) {
                            Text(
                                text = "👑 VIP",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xD9330B28))
                                    .border(0.8.dp, NeonAmber, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = target.title,
                        fontFamily = NotoSansBengaliFamily,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = target.subtitle,
                        fontFamily = NotoSansBengaliFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }
            }

            // Interactive Full Keyboard Preview (tap any key to test live!)
            FullMiniKeyboardPreview(
                settings = settings,
                timeSec = timeSec,
                customPhotoTheme = customPhotoTheme,
                overrideBannerPresetId = target.bannerPresetId,
                overrideRemoteImageUrl = target.remoteImageUrl,
                overrideGradientColors = target.previewColors,
                overrideEffect = target.effect,
                overrideTouchEffect = target.touchEffect,
                overridePalette = target.palette,
                showKeyBorders = localKeyBorders,
                forceDefaultBlack = localDefaultBlack,
                isInteractive = true,
                compact = false
            )

            Text(
                text = "👆 উপরের কিবোর্ডে যেকোনো অক্ষরে ট্যাপ করে লাইভ টেস্ট করুন",
                fontFamily = NotoSansBengaliFamily,
                fontSize = 11.5.sp,
                color = NeonCyan.copy(alpha = 0.92f)
            )

            // Optional Default Black vs Custom Photo switch (for Typing Effects)
            if (target.allowBackdropSwitch && customPhotoTheme.hasPhoto) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val blackSelected = localDefaultBlack
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (blackSelected) Color(0xFF19243A) else GlassCardInner)
                            .border(
                                width = if (blackSelected) 1.4.dp else 1.dp,
                                color = if (blackSelected) NeonCyan else GlassBorderStrong,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { localDefaultBlack = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🖤 Default Black",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!blackSelected) Color(0xFF28163A) else GlassCardInner)
                            .border(
                                width = if (!blackSelected) 1.4.dp else 1.dp,
                                color = if (!blackSelected) NeonMagenta else GlassBorderStrong,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { localDefaultBlack = false }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🖼️ Active Photo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Gboard-style "Key borders" toggle switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassCardInner)
                    .border(1.dp, FlagshipGlassRimSubtle, RoundedCornerShape(16.dp))
                    .clickable { localKeyBorders = !localKeyBorders }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Key borders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (localKeyBorders) "Keycap outlines ON" else "Borderless clean Gboard style",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }
                Switch(
                    checked = localKeyBorders,
                    onCheckedChange = { localKeyBorders = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RoyalBlueDeep,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = ObsidianCanvas,
                        uncheckedBorderColor = GlassBorderStrong
                    ),
                    modifier = Modifier.testTag("customizer_switch_key_borders")
                )
            }

            // Cancel & Apply Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("customizer_btn_cancel")
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        if (target.isLocked) {
                            onOpenVipUnlock()
                        } else {
                            onApply(localKeyBorders, localDefaultBlack)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (target.isLocked) NeonAmber else NeonCyan,
                        contentColor = Color(0xFF061020)
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .heightIn(min = 48.dp)
                        .testTag("customizer_btn_apply")
                ) {
                    Icon(
                        imageVector = if (target.isLocked) Icons.Filled.AutoAwesome else Icons.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (target.isLocked) "👑 Unlock & Apply" else "Apply",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
