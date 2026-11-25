package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.data.ComboThemeCatalog
import com.example.data.ComboThemePreset
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.LicenseUiState
import com.example.engine.RgbLightingEngine
import com.example.engine.RipplePulse
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipGlassRimGoldSubtle
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.FlagshipGoldAmberBrush
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.GlassCardSelected
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KeyCapSurface
import com.example.ui.theme.KeyChassisDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonError
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.luminousGlassCard
import kotlinx.coroutines.isActive

@Composable
fun EffectsScreen(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    onApplyVipToKeyboard: () -> Unit,
    onApplyComboTheme: (ComboThemePreset) -> Unit,
    onApplyAnimationOnly: (RgbEffect) -> Unit,
    onApplyStaticBannerOnly: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header (Pure White Typography)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.effects_header),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.effects_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }

            // Live Mini Keyboard Preview (shows active HD Photo/Banner + Live AI Animation together!)
            LiveMiniDeckPreview(
                settings = settings,
                customPhotoTheme = customPhotoTheme,
                timeSec = timeSec
            )

            // VIP Expired Lock Notice + Dynamic VIP Unlock CTA (respects remote rgbThemes override)
            val themesLocked = !licenseState.isRgbThemesUnlocked
            if (themesLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .luminousGlassCard(
                            shape = RoundedCornerShape(20.dp),
                            borderBrush = FlagshipGoldCyanBrush,
                            borderWidth = 1.4.dp,
                            glowColor = RoyalGold,
                            glowElevation = 14.dp
                        )
                        .padding(15.dp)
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
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.bn_demo_preview_effects_desc),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Button(
                        onClick = onApplyVipToKeyboard,
                        shape = RoundedCornerShape(14.dp),
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
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION 1 — 🔥 1-Tap Photo + AI Animation Combos (10 All-in-One Ready Themes)
            // =========================================================================
            GallerySectionHeader(
                title = stringResource(R.string.section_theme_combos),
                subtitle = stringResource(R.string.section_theme_combos_sub)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ComboThemeCatalog.allCombos.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { combo ->
                            val isSelected = customPhotoTheme.presetId == combo.bannerPresetId &&
                                settings.effect == combo.effect
                            ComboThemeCard(
                                combo = combo,
                                selected = isSelected,
                                isLockedVip = themesLocked,
                                timeSec = timeSec,
                                onClick = {
                                    if (themesLocked) {
                                        onApplyComboTheme(combo)
                                        onApplyVipToKeyboard()
                                    } else {
                                        onApplyComboTheme(combo)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 2 — ✨ AI Lighting Animations Only (12 Pure RGB Modes + Classic Black)
            // =========================================================================
            GallerySectionHeader(
                title = stringResource(R.string.section_animations_only),
                subtitle = stringResource(R.string.section_animations_only_sub)
            )
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                RgbEffect.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        pair.forEach { effect ->
                            val isSelected = settings.effect == effect && !customPhotoTheme.hasPhoto
                            EffectPreviewCard(
                                effect = effect,
                                settings = settings,
                                timeSec = timeSec,
                                selected = isSelected,
                                isLockedVip = themesLocked && effect.isVipEffect,
                                onClick = {
                                    onApplyAnimationOnly(effect)
                                    if (themesLocked && effect.isVipEffect) {
                                        onApplyVipToKeyboard()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 3 — 🖼️ HD Photo Banners Only (No Animation) + Custom Gallery Photo
            // =========================================================================
            GallerySectionHeader(
                title = stringResource(R.string.section_static_banners),
                subtitle = stringResource(R.string.section_static_banners_sub)
            )
            StaticPhotoBannersSection(
                photoTheme = customPhotoTheme,
                isStaticBannerMode = settings.effect == RgbEffect.NORMAL_DEFAULT,
                isFreeStandardMode = themesLocked,
                onApplyVipToKeyboard = onApplyVipToKeyboard,
                onSelectStaticBanner = { presetId ->
                    onApplyStaticBannerOnly(presetId)
                    if (themesLocked) {
                        onApplyVipToKeyboard()
                    }
                },
                onPickClick = {
                    if (themesLocked) {
                        onApplyVipToKeyboard()
                    } else {
                        galleryPickerLauncher.launch("image/*")
                    }
                },
                onDimChange = onPhotoDimChange,
                onRemoveClick = onRemoveCustomPhoto
            )

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

            // Optical Tuning Sliders & Switches (Luminous Flagship Glass Card)
            SectionTitle(title = stringResource(R.string.section_tuning))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .luminousGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        borderBrush = FlagshipGlassRimSubtle,
                        borderWidth = 1.2.dp,
                        glowColor = NeonCyan,
                        glowElevation = 10.dp
                    )
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

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun GallerySectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.86f)
        )
    }
}

@Composable
private fun ComboThemeCard(
    combo: ComboThemePreset,
    selected: Boolean,
    isLockedVip: Boolean,
    timeSec: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preset = remember(combo.bannerPresetId) { BannerPresetCatalog.findById(combo.bannerPresetId) }
    val proceduralPreview = remember(combo.bannerPresetId) {
        if (preset != null && preset.drawableRes == null) {
            CustomBackgroundManager.getInstance(context)
                .renderProceduralPresetBitmap(preset.id, w = 360, h = 180)
                .asImageBitmap()
        } else {
            null
        }
    }
    val pulseAlpha = remember(timeSec) {
        0.55f + 0.45f * ((kotlin.math.sin((timeSec * 3.2f).toDouble()) + 1.0) * 0.5).toFloat()
    }

    Column(
        modifier = modifier
            .luminousGlassCard(
                shape = RoundedCornerShape(18.dp),
                borderBrush = when {
                    selected -> FlagshipGoldCyanBrush
                    isLockedVip -> FlagshipGoldAmberBrush
                    else -> FlagshipGlassRimSubtle
                },
                borderWidth = if (selected) 1.8.dp else 1.1.dp,
                glowColor = if (selected) RoyalGold else NeonCyan,
                glowElevation = if (selected) 14.dp else 7.dp,
                surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
            )
            .clickable(onClick = onClick)
            .padding(9.dp)
            .testTag("combo_theme_${combo.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        combo.previewColors.map { Color(it).copy(alpha = pulseAlpha) }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            if (preset?.drawableRes != null) {
                Image(
                    painter = painterResource(id = preset.drawableRes),
                    contentDescription = combo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (proceduralPreview != null) {
                Image(
                    bitmap = proceduralPreview,
                    contentDescription = combo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(combo.previewColors.map { Color(it) }))
                )
            }

            // Dark readability scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.12f),
                            0.6f to Color.Black.copy(alpha = 0.25f),
                            1.0f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
            )

            // Mini live animated RGB bar at bottom of thumbnail
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            combo.previewColors.map { Color(it).copy(alpha = pulseAlpha) }
                        )
                    )
            )

            // Combo Animation Badge
            Text(
                text = combo.badge,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            if (isLockedVip) {
                Text(
                    text = "🔒 VIP",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xD9330B28))
                        .border(0.8.dp, NeonAmber, RoundedCornerShape(999.dp))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                )
            } else if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = combo.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = combo.subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StaticPhotoBannersSection(
    photoTheme: CustomPhotoTheme,
    isStaticBannerMode: Boolean,
    isFreeStandardMode: Boolean,
    onApplyVipToKeyboard: () -> Unit,
    onSelectStaticBanner: (String) -> Unit,
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
            .luminousGlassCard(
                shape = RoundedCornerShape(20.dp),
                borderBrush = FlagshipGlassRimGoldSubtle,
                borderWidth = 1.25.dp,
                glowColor = NeonCyan,
                glowElevation = 12.dp
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Custom Gallery Photo CTA + Remove Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(NeonCyan.copy(alpha = 0.28f), NeonMagenta.copy(alpha = 0.24f))
                        )
                    )
                    .border(1.2.dp, FlagshipCyanMagentaBrush, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Wallpaper,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.custom_photo_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.custom_photo_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }
        }

        // Active Photo Preview Banner
        if (previewBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
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
                Text(
                    text = activePreset?.title ?: "Custom Gallery Photo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Q", "W", "E", "R").forEach { ch ->
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x99121622))
                                .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
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

        // 10 Pure Static HD Photo Banners Grid (2 columns — tapping applies banner + No Animation)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BannerPresetCatalog.staticBannerPresets.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        val isSelected = photoTheme.presetId == preset.id && isStaticBannerMode
                        StaticBannerTile(
                            preset = preset,
                            selected = isSelected,
                            isLockedVip = isFreeStandardMode,
                            onClick = { onSelectStaticBanner(preset.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
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
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = RoyalBlueDeep
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_pick_custom_photo")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = null,
                    tint = RoyalBlueDeep,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (photoTheme.hasCustomGalleryPhoto) R.string.action_change_photo
                        else R.string.action_choose_gallery_photo
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlueDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (previewBitmap != null) {
                OutlinedButton(
                    onClick = onRemoveClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_remove_custom_photo")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.action_remove_photo),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_remove_photo),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

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
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StaticBannerTile(
    preset: BannerThemePreset,
    selected: Boolean,
    isLockedVip: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val proceduralBitmap = remember(preset.id) {
        if (preset.drawableRes == null) {
            CustomBackgroundManager.getInstance(context)
                .renderProceduralPresetBitmap(preset.id, w = 360, h = 180)
                .asImageBitmap()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else GlassBorder,
                shape = RoundedCornerShape(14.dp)
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
        } else if (proceduralBitmap != null) {
            Image(
                bitmap = proceduralBitmap,
                contentDescription = preset.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(preset.previewColors.map { Color(it) }))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.08f),
                        0.55f to Color.Black.copy(alpha = 0.25f),
                        1.0f to Color.Black.copy(alpha = 0.78f)
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
                    .background(Color(0xD9330B28))
                    .border(0.8.dp, NeonAmber, RoundedCornerShape(999.dp))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            )
        } else {
            Text(
                text = "HD STATIC",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.52f))
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
                    .background(Color.Black.copy(alpha = 0.65f)),
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = preset.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (preset.subtitle.isNotBlank()) {
                Text(
                    text = preset.subtitle,
                    fontSize = 9.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveMiniDeckPreview(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    timeSec: Float
) {
    val context = LocalContext.current
    val bgBitmap = remember(customPhotoTheme.cacheKey) {
        if (customPhotoTheme.hasPhoto) {
            CustomBackgroundManager.getInstance(context).decodeCachedBitmap(customPhotoTheme)?.asImageBitmap()
        } else {
            null
        }
    }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KeyChassisDark)
            .border(1.2.dp, GlassBorder, RoundedCornerShape(18.dp))
    ) {
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = customPhotoTheme.dimAlpha))
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
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

                        val keyAlpha = if (bgBitmap != null) 0.62f else 1f
                        if (settings.effect == RgbEffect.NORMAL_DEFAULT) {
                            drawRoundRect(
                                color = Color(0xFF15161D).copy(alpha = keyAlpha),
                                topLeft = Offset(x, y),
                                size = Size(keyW, keyH),
                                cornerRadius = cr
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.22f),
                                topLeft = Offset(x, y),
                                size = Size(keyW, keyH),
                                cornerRadius = cr,
                                style = Stroke(width = 0.8.dp.toPx())
                            )
                        } else if (settings.effect == RgbEffect.TOUCH_SPARK) {
                            drawRoundRect(
                                color = Color(0xFF131620).copy(alpha = keyAlpha),
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
                                    color = Color(0xFF202434).copy(alpha = keyAlpha),
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
                                        color = Color(0xFF11141E).copy(alpha = keyAlpha),
                                        topLeft = Offset(x + 2f, y + 2f),
                                        size = Size((keyW - 4f).coerceAtLeast(2f), (keyH - 6f).coerceAtLeast(2f)),
                                        cornerRadius = cr
                                    )
                                }
                                KeycapStyle.FLOATING_GLOW -> {
                                    drawRoundRect(
                                        color = KeyCapSurface.copy(alpha = keyAlpha),
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
                                        color = Color(0xFF1C2133).copy(alpha = keyAlpha),
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
            .luminousGlassCard(
                shape = RoundedCornerShape(16.dp),
                borderBrush = when {
                    selected -> FlagshipCyanMagentaBrush
                    isLockedVip -> FlagshipGoldAmberBrush
                    else -> FlagshipGlassRimSubtle
                },
                borderWidth = if (selected) 1.6.dp else 1.1.dp,
                glowColor = if (selected) NeonCyan else NeonMagenta,
                glowElevation = if (selected) 12.dp else 6.dp,
                surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
            )
            .clickable(onClick = onClick)
            .padding(11.dp)
            .testTag("effect_card_${effect.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(KeyChassisDark)
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
                        .background(Color(0xD9330B28))
                        .border(0.8.dp, NeonAmber, RoundedCornerShape(999.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            } else if (!effect.isVipEffect) {
                Text(
                    text = "FREE",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xD9052B1E))
                        .border(0.8.dp, NeonEmerald, RoundedCornerShape(999.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = effect.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = effect.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
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
            .background(if (selected) GlassCardSelected else GlassCardSurface)
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) Color.White else GlassBorder,
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
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
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
            .background(if (selected) GlassCardSelected else GlassCardSurface)
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) Color.White else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("keycap_style_${style.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 48.dp, height = 34.dp),
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
                        drawRoundRect(color = KeyChassisDark, cornerRadius = cr)
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
                color = Color.White
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = style.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
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
        fontWeight = FontWeight.Bold,
        color = Color.White
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
                color = Color.White
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
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
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RoyalBlueDeep,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = GlassCardInner,
                uncheckedBorderColor = GlassBorderStrong
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
