package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BannerPresetCatalog
import com.example.data.BannerThemePreset
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.LicenseUiState
import com.example.engine.RgbLightingEngine
import com.example.engine.RipplePulse
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KeyCapSurface
import com.example.ui.theme.KeyChassisDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonError
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
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
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    onApplyVipToKeyboard: () -> Unit,
    onSelectBannerPreset: (String) -> Unit,
    onPickCustomPhoto: (Uri) -> Unit,
    onPhotoDimChange: (Float) -> Unit,
    onRemoveCustomPhoto: () -> Unit,
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

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { selectedUri: Uri? ->
        if (selectedUri != null) {
            onPickCustomPhoto(selectedUri)
        }
    }

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

            // Free Live Demo Preview Notice + Save / Apply to Real Keyboard CTA (shown after 7-day trial expires)
            if (licenseState.isFreeStandardMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurfaceVariant)
                        .border(
                            width = 1.4.dp,
                            brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                        .testTag("effects_live_demo_banner"),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = stringResource(R.string.vip_one_time_badge),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                    Text(
                        text = stringResource(R.string.bn_demo_preview_tag),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        text = stringResource(R.string.bn_demo_preview_effects_desc),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onApplyVipToKeyboard,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMagenta,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_effects_apply_vip_to_keyboard")
                    ) {
                        Text(
                            text = stringResource(R.string.bn_btn_apply_to_real_keyboard),
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Built-in Banner Theme Presets + Custom Gallery Photo Card
            SectionTitle(title = stringResource(R.string.section_custom_photo))
            CustomPhotoThemeCard(
                photoTheme = customPhotoTheme,
                isFreeStandardMode = licenseState.isFreeStandardMode,
                onApplyVipToKeyboard = onApplyVipToKeyboard,
                onSelectPreset = onSelectBannerPreset,
                onPickClick = { galleryPickerLauncher.launch("image/*") },
                onDimChange = onPhotoDimChange,
                onRemoveClick = onRemoveCustomPhoto
            )

            // Lighting Modes Grid (2 columns — Normal / Classic Black first, then Touch Spark & VIP effects)
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
                                isLockedVip = licenseState.isFreeStandardMode && effect.isVipEffect,
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
private fun CustomPhotoThemeCard(
    photoTheme: CustomPhotoTheme,
    isFreeStandardMode: Boolean,
    onApplyVipToKeyboard: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onPickClick: () -> Unit,
    onDimChange: (Float) -> Unit,
    onRemoveClick: () -> Unit
) {
    val context = LocalContext.current
    val previewBitmap = remember(photoTheme.cacheKey) {
        if (photoTheme.hasPhoto) {
            CustomBackgroundManager.getInstance(context).decodeCachedBitmap(photoTheme)?.asImageBitmap()
        } else {
            null
        }
    }
    val activePreset = remember(photoTheme.presetId) {
        BannerPresetCatalog.findById(photoTheme.presetId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianSurface)
            .border(
                width = if (previewBitmap != null) 1.4.dp else 1.dp,
                color = if (previewBitmap != null) NeonCyan.copy(alpha = 0.7f) else ObsidianBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceVariant)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Wallpaper,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.custom_photo_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFreeStandardMode) {
                        Text(
                            text = "🔒 VIP",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF330C27))
                                .border(1.dp, NeonMagenta.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.custom_photo_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Live preview banner for either built-in preset or custom gallery photo
        if (previewBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            ) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = stringResource(R.string.cd_custom_photo_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = photoTheme.dimAlpha))
                )
                // Active badge in top-left corner
                Text(
                    text = activePreset?.title ?: "Gallery Photo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xB30B0F1A))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
                // Simulated translucent keycap overlay showing RGB glow contrast
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Q" to NeonCyan, "W" to NeonMagenta, "E" to Color(0xFF76FF03), "R" to Color(0xFFFF6D00)).forEach { (ch, glow) ->
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x99121622))
                                .border(1.2.dp, glow.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ch,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Built-in Ready-to-Use Banner / Background Presets (Horizontal Scrollable Row)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.section_banner_presets),
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                // Stealth Dark (no background) tile
                item(key = "preset_none") {
                    val isSelected = !photoTheme.hasPhoto
                    Box(
                        modifier = Modifier
                            .size(width = 104.dp, height = 68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10131C))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonCyan else ObsidianBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onRemoveClick)
                            .testTag("banner_preset_none")
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.preset_stealth_none),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }

                // Built-in Vice City & Neon Banner Presets
                items(BannerPresetCatalog.allPresets, key = { it.id }) { preset ->
                    BannerPresetTile(
                        preset = preset,
                        selected = photoTheme.presetId == preset.id,
                        isLockedVip = isFreeStandardMode,
                        onClick = { onSelectPreset(preset.id) }
                    )
                }
            }
        }

        // Gallery Picker + Remove Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPickClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF001B22)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_pick_custom_photo")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (photoTheme.hasCustomGalleryPhoto) R.string.action_change_photo
                        else R.string.action_choose_gallery_photo
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (previewBitmap != null) {
                OutlinedButton(
                    onClick = onRemoveClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_remove_custom_photo")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.action_remove_photo),
                        tint = NeonError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_remove_photo),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonError
                    )
                }
            }
        }

        // Background Dim / Opacity slider works for BOTH built-in presets and custom gallery photos
        if (previewBitmap != null) {
            TuningSliderRow(
                label = stringResource(R.string.label_photo_dim),
                valueLabel = "${(photoTheme.dimAlpha * 100).toInt()}%",
                value = photoTheme.dimAlpha,
                valueRange = CustomBackgroundManager.MIN_DIM_ALPHA..CustomBackgroundManager.MAX_DIM_ALPHA,
                onValueChange = onDimChange,
                testTag = "slider_custom_photo_dim"
            )

            if (isFreeStandardMode) {
                Button(
                    onClick = onApplyVipToKeyboard,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonMagenta,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_photo_apply_vip_to_keyboard")
                ) {
                    Text(
                        text = stringResource(R.string.bn_btn_apply_to_real_keyboard),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerPresetTile(
    preset: BannerThemePreset,
    selected: Boolean,
    isLockedVip: Boolean,
    onClick: () -> Unit
) {
    val gradientColors = remember(preset.id) {
        preset.previewColors.map { Color(it) }
    }

    Box(
        modifier = Modifier
            .size(width = 114.dp, height = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) NeonCyan else ObsidianBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("banner_preset_${preset.id}")
    ) {
        if (preset.drawableRes != null) {
            Image(
                painter = painterResource(id = preset.drawableRes),
                contentDescription = preset.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradientColors))
            )
        }

        // Bottom readable label scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1.0f to Color(0xD9070A12)
                    )
                )
        )

        if (isLockedVip) {
            Text(
                text = "🔒 VIP",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xCC2A0B24))
                    .border(0.8.dp, NeonMagenta.copy(alpha = 0.75f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC06141B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = preset.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) NeonCyan else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 7.dp, vertical = 5.dp)
        )
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

                    if (settings.effect == RgbEffect.NORMAL_DEFAULT) {
                        drawRoundRect(
                            color = Color(0xFF15161D),
                            topLeft = Offset(x, y),
                            size = Size(keyW, keyH),
                            cornerRadius = cr
                        )
                        drawRoundRect(
                            color = Color(0xFF252834),
                            topLeft = Offset(x, y),
                            size = Size(keyW, keyH),
                            cornerRadius = cr,
                            style = Stroke(width = 0.8.dp.toPx())
                        )
                    } else if (settings.effect == RgbEffect.TOUCH_SPARK) {
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
    isLockedVip: Boolean,
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
        Box(modifier = Modifier.fillMaxWidth()) {
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
                        val isNormalBlack = effect == RgbEffect.NORMAL_DEFAULT
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
                            color = when {
                                isNormalBlack -> Color(0xFF1A1B23)
                                isTouchSpark && sparkVal <= 0f -> Color(0xFF151824)
                                else -> col
                            },
                            topLeft = Offset(pad + c * (kw + gap), pad + r * (kh + gap)),
                            size = Size(kw, kh),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }

            if (isLockedVip) {
                Text(
                    text = "🔒 VIP",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xD92B0B24))
                        .border(0.8.dp, NeonMagenta.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            } else if (!effect.isVipEffect) {
                Text(
                    text = "FREE",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonEmerald,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xD9052B1E))
                        .border(0.8.dp, NeonEmerald.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
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
