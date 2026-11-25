package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.CloudAnimationConfig
import com.example.data.ComboCategory
import com.example.data.ComboThemeCatalog
import com.example.data.ComboThemePreset
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.LicenseUiState
import com.example.engine.RgbLightingEngine
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.TouchTypingEffect
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
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.luminousGlassCard
import kotlinx.coroutines.isActive
import kotlin.math.sin

@Composable
fun EffectsScreen(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    downloadedThemeIds: Set<String>,
    downloadingThemeIds: Set<String>,
    onApplyVipToKeyboard: () -> Unit,
    onApplyComboTheme: (ComboThemePreset) -> Unit,
    onDownloadComboTheme: (ComboThemePreset) -> Unit,
    onApplyAnimationOnly: (RgbEffect) -> Unit,
    onSelectTouchEffect: (TouchTypingEffect) -> Unit,
    onApplyCloudAnimation: (CloudAnimationConfig) -> Unit,
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
    var selectedCategoryName by rememberSaveable { mutableStateOf(ComboCategory.ALL.name) }
    val selectedCategory = remember(selectedCategoryName) {
        ComboCategory.entries.firstOrNull { it.name == selectedCategoryName } ?: ComboCategory.ALL
    }

    val filteredCombos = remember(selectedCategory, licenseState.adminConfig.cloudThemes) {
        ComboThemeCatalog.byCategory(selectedCategory, licenseState.adminConfig.cloudThemes)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
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

    val themesLocked = !licenseState.isRgbThemesUnlocked

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
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.effects_header),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Dual-Layer RGB Engine: Combine any Background Theme / Ambient Physics + Touch Typing Effect simultaneously!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }

            // Live Dual-Layer Keyboard Preview (Theme Photo + Layer 1 Ambient + Layer 2 Touch Effect)
            LiveMiniDeckPreview(
                settings = settings,
                customPhotoTheme = customPhotoTheme,
                timeSec = timeSec
            )

            // VIP Expired Lock Notice + Dynamic VIP Unlock CTA
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
            // SECTION 1 — ⚡ 7 Touch-Reactive Typing Effects (Layer 2 — Stacks on ANY Theme/Ambient!)
            // =========================================================================
            GallerySectionHeader(
                title = "⚡ 7 Touch-Reactive Typing Effects (Layer 2)",
                subtitle = "Bursts vividly on every keypress — stacks simultaneously on top of your active Theme & Ambient Animation"
            )
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                TouchTypingEffect.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        pair.forEach { touchEffect ->
                            val isSelected = settings.touchEffect == touchEffect
                            val isUnlocked = licenseState.isThemeItemUnlocked(touchEffect.id, isPermanentlyFree = false)
                            TouchTypingEffectCard(
                                touchEffect = touchEffect,
                                settings = settings,
                                timeSec = timeSec,
                                selected = isSelected,
                                isLockedVip = !isUnlocked,
                                onClick = {
                                    if (!isUnlocked) {
                                        onApplyVipToKeyboard()
                                    } else {
                                        onSelectTouchEffect(touchEffect)
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
            // SECTION 2 — 🌈 Classic Black (FREE) + 20 Next-Gen Ambient Physics RGB Animations
            // =========================================================================
            GallerySectionHeader(
                title = "🌈 20 Ambient Physics RGB Animations (Layer 1)",
                subtitle = "Classic Black is permanently FREE • All 20 Next-Gen physics modes combine cleanly with your Touch Effect"
            )
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                RgbEffect.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        pair.forEach { effect ->
                            val isSelected = settings.effect == effect
                            val isUnlocked = licenseState.isThemeItemUnlocked(
                                themeId = effect.id,
                                isPermanentlyFree = !effect.isVipEffect
                            )
                            EffectPreviewCard(
                                effect = effect,
                                settings = settings,
                                timeSec = timeSec,
                                selected = isSelected,
                                isLockedVip = !isUnlocked,
                                onClick = {
                                    if (!isUnlocked) {
                                        onApplyVipToKeyboard()
                                    } else {
                                        onApplyAnimationOnly(effect)
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

                // Remote Firebase Cloud Animations (/admin_config/cloudAnimations)
                if (licenseState.adminConfig.cloudAnimations.isNotEmpty()) {
                    Text(
                        text = "☁️ LIVE CLOUD ANIMATIONS",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    licenseState.adminConfig.cloudAnimations.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            pair.forEach { cloudAnim ->
                                val mappedEffect = RgbEffect.fromSavedName(cloudAnim.baseEffectId)
                                val mappedPalette = RgbPalette.entries.firstOrNull {
                                    it.name.equals(cloudAnim.paletteName, ignoreCase = true)
                                } ?: settings.palette
                                val isUnlocked = licenseState.isThemeItemUnlocked(
                                    themeId = cloudAnim.id,
                                    isPermanentlyFree = cloudAnim.isFree
                                )
                                CloudAnimationCard(
                                    cloudAnim = cloudAnim,
                                    previewEffect = mappedEffect,
                                    previewPalette = mappedPalette,
                                    settings = settings,
                                    timeSec = timeSec,
                                    selected = settings.effect == mappedEffect && settings.palette == mappedPalette,
                                    isLockedVip = !isUnlocked,
                                    onClick = {
                                        if (!isUnlocked) {
                                            onApplyVipToKeyboard()
                                        } else {
                                            onApplyCloudAnimation(cloudAnim)
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
            }

            // =========================================================================
            // SECTION 3 — ☁️ 52 On-Demand Downloadable 1-Tap Combos + Remote Cloud Themes
            // =========================================================================
            GallerySectionHeader(
                title = "☁️ 52 Downloadable 1-Tap Flagship Combos",
                subtitle = "Download HD themes locally on demand (filesDir/themes/) — combines Background + Ambient + Touch Effect in 1 tap"
            )

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("combo_category_filter_row")
            ) {
                items(ComboCategory.entries, key = { it.id }) { cat ->
                    val active = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (active) {
                                    Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.28f), NeonMagenta.copy(alpha = 0.28f)))
                                } else {
                                    Brush.horizontalGradient(listOf(GlassCardSurface, GlassCardSurface))
                                }
                            )
                            .border(
                                width = if (active) 1.4.dp else 1.dp,
                                brush = if (active) FlagshipGoldCyanBrush else FlagshipGlassRimSubtle,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable { selectedCategoryName = cat.name }
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                            .testTag("combo_cat_${cat.id}")
                    ) {
                        Text(
                            text = cat.label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredCombos.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { combo ->
                            val isDownloaded = downloadedThemeIds.contains(combo.id) ||
                                BannerPresetCatalog.findById(combo.bannerPresetId)?.drawableRes != null
                            val isDownloading = downloadingThemeIds.contains(combo.id)
                            val isSelected = (customPhotoTheme.presetId == combo.id ||
                                customPhotoTheme.presetId == combo.bannerPresetId) &&
                                settings.effect == combo.effect
                            val isUnlocked = licenseState.isThemeItemUnlocked(
                                themeId = combo.id,
                                isPermanentlyFree = combo.isCloudFree
                            )
                            ComboThemeCard(
                                combo = combo,
                                selected = isSelected,
                                isDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                isLockedVip = !isUnlocked,
                                timeSec = timeSec,
                                onClick = {
                                    if (!isUnlocked) {
                                        onApplyVipToKeyboard()
                                    } else if (isDownloaded) {
                                        onApplyComboTheme(combo)
                                    } else if (!isDownloading) {
                                        onDownloadComboTheme(combo)
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
            // SECTION 4 — 🖼️ HD Photo Banners Only + Custom Gallery Photo (👑 VIP)
            // =========================================================================
            GallerySectionHeader(
                title = stringResource(R.string.section_static_banners),
                subtitle = stringResource(R.string.section_static_banners_sub)
            )
            StaticPhotoBannersSection(
                photoTheme = customPhotoTheme,
                licenseState = licenseState,
                isStaticBannerMode = settings.effect == RgbEffect.NORMAL_DEFAULT,
                isFreeStandardMode = themesLocked,
                onApplyVipToKeyboard = onApplyVipToKeyboard,
                onSelectStaticBanner = { presetId ->
                    val unlocked = licenseState.isThemeItemUnlocked(presetId, isPermanentlyFree = false)
                    if (!unlocked) {
                        onApplyVipToKeyboard()
                    } else {
                        onApplyStaticBannerOnly(presetId)
                    }
                },
                onPickClick = {
                    val galleryUnlocked = licenseState.isThemeItemUnlocked("custom_gallery_photo", isPermanentlyFree = false)
                    if (!galleryUnlocked) {
                        onApplyVipToKeyboard()
                    } else {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                onDimChange = onPhotoDimChange,
                onRemoveClick = onRemoveCustomPhoto
            )

            // Color Palettes Grid (2 columns — 👑 VIP when locked)
            SectionTitle(title = stringResource(R.string.section_color_palette))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RgbPalette.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { palette ->
                            val unlocked = licenseState.isThemeItemUnlocked(
                                themeId = "palette_${palette.name.lowercase()}",
                                isPermanentlyFree = false
                            )
                            PaletteCard(
                                palette = palette,
                                selected = settings.palette == palette,
                                isLockedVip = !unlocked,
                                onClick = {
                                    if (!unlocked) {
                                        onApplyVipToKeyboard()
                                    } else {
                                        onSelectPalette(palette)
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

            // Keycap Architecture Grid (2 columns — 👑 VIP when locked)
            SectionTitle(title = stringResource(R.string.section_keycap_style))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeycapStyle.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { style ->
                            val unlocked = licenseState.isThemeItemUnlocked(
                                themeId = "keycap_${style.name.lowercase()}",
                                isPermanentlyFree = false
                            )
                            KeycapStyleCard(
                                style = style,
                                accentColor = RgbLightingEngine.samplePalette(settings.palette, timeSec * 0.25f),
                                selected = settings.keycapStyle == style,
                                isLockedVip = !unlocked,
                                onClick = {
                                    if (!unlocked) {
                                        onApplyVipToKeyboard()
                                    } else {
                                        onSelectKeycapStyle(style)
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

            // Optical Tuning Sliders & Switches
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
private fun TouchTypingEffectCard(
    touchEffect: TouchTypingEffect,
    settings: KeyboardSettings,
    timeSec: Float,
    selected: Boolean,
    isLockedVip: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .luminousGlassCard(
                shape = RoundedCornerShape(16.dp),
                borderBrush = when {
                    selected -> FlagshipGoldCyanBrush
                    isLockedVip -> FlagshipGoldAmberBrush
                    else -> FlagshipGlassRimSubtle
                },
                borderWidth = if (selected) 1.7.dp else 1.1.dp,
                glowColor = if (selected) NeonCyan else RoyalGold,
                glowElevation = if (selected) 12.dp else 6.dp,
                surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
            )
            .clickable(onClick = onClick)
            .padding(11.dp)
            .testTag("touch_effect_card_${touchEffect.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KeyChassisDark)
            ) {
                val cols = 7
                val rows = 3
                val gap = 2.5.dp.toPx()
                val pad = 4.dp.toPx()
                val kw = (size.width - pad * 2 - gap * (cols - 1)) / cols
                val kh = (size.height - pad * 2 - gap * (rows - 1)) / rows
                val cr = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val x = pad + c * (kw + gap)
                        val y = pad + r * (kh + gap)
                        val nx = (c + 0.5f) / cols
                        val ny = (r + 0.5f) / rows
                        val touchCol = RgbLightingEngine.computeTouchPreviewColor(
                            touchEffect = touchEffect,
                            nx = nx,
                            ny = ny,
                            row = r,
                            col = c,
                            rows = rows,
                            cols = cols,
                            timeSec = timeSec,
                            palette = settings.palette,
                            glowIntensity = settings.glowIntensity
                        )
                        drawRoundRect(
                            color = Color(0xFF151926),
                            topLeft = Offset(x, y),
                            size = Size(kw, kh),
                            cornerRadius = cr
                        )
                        if (touchCol.alpha > 0.03f) {
                            drawRoundRect(
                                color = touchCol,
                                topLeft = Offset(x, y),
                                size = Size(kw, kh),
                                cornerRadius = cr
                            )
                        }
                    }
                }
            }

            if (isLockedVip) {
                VipCornerPill(
                    text = "👑 VIP",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )
            } else if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f)),
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
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = touchEffect.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = touchEffect.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.84f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CloudAnimationCard(
    cloudAnim: CloudAnimationConfig,
    previewEffect: RgbEffect,
    previewPalette: RgbPalette,
    settings: KeyboardSettings,
    timeSec: Float,
    selected: Boolean,
    isLockedVip: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewSettings = remember(previewEffect, previewPalette, settings.animationSpeed, settings.glowIntensity) {
        settings.copy(effect = previewEffect, palette = previewPalette)
    }

    Column(
        modifier = modifier
            .luminousGlassCard(
                shape = RoundedCornerShape(16.dp),
                borderBrush = if (selected) FlagshipGoldCyanBrush else FlagshipCyanMagentaBrush,
                borderWidth = if (selected) 1.7.dp else 1.1.dp,
                glowColor = NeonCyan,
                glowElevation = if (selected) 12.dp else 7.dp,
                surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
            )
            .clickable(onClick = onClick)
            .padding(11.dp)
            .testTag("cloud_anim_${cloudAnim.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
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
                        val col = RgbLightingEngine.computeKeyColor(
                            nx = (c + 0.5f) / cols,
                            ny = (r + 0.5f) / rows,
                            keyHash = r * 19 + c * 11,
                            timeSec = timeSec,
                            settings = previewSettings,
                            ripples = emptyList(),
                            isPressed = false
                        )
                        drawRoundRect(
                            color = if (col == Color.Transparent) Color(0xFF1A1B23) else col,
                            topLeft = Offset(pad + c * (kw + gap), pad + r * (kh + gap)),
                            size = Size(kw, kh),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
            if (isLockedVip) {
                VipCornerPill(
                    text = "👑 VIP",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )
            } else {
                FreeCornerPill(
                    text = "☁️ CLOUD",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = cloudAnim.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cloudAnim.subtitle.ifBlank { previewEffect.displayName },
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.84f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ComboThemeCard(
    combo: ComboThemePreset,
    selected: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
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
                .renderProceduralPresetBitmap(preset.id, w = 320, h = 160)
                .asImageBitmap()
        } else {
            null
        }
    }
    val pulseAlpha = remember(timeSec) {
        0.55f + 0.45f * ((sin((timeSec * 3.2f).toDouble()) + 1.0) * 0.5).toFloat()
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
                            0.0f to Color.Black.copy(alpha = 0.14f),
                            0.6f to Color.Black.copy(alpha = 0.28f),
                            1.0f to Color.Black.copy(alpha = 0.74f)
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
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            if (isLockedVip) {
                VipCornerPill(
                    text = "👑 VIP",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
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
                text = "${combo.touchEffect.badge} • ${combo.effect.shortLabel}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.84f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // On-Demand Download / Apply Pill Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        selected -> NeonEmerald.copy(alpha = 0.22f)
                        isLockedVip -> Color(0xCC2C1329)
                        isDownloaded -> NeonCyan.copy(alpha = 0.20f)
                        else -> Color(0xFF17213B)
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        selected -> NeonEmerald
                        isLockedVip -> NeonAmber.copy(alpha = 0.85f)
                        isDownloaded -> NeonCyan.copy(alpha = 0.75f)
                        else -> Color.White.copy(alpha = 0.24f)
                    },
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.6.dp,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Downloading...",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    maxLines = 1
                )
            } else if (selected) {
                Text(
                    text = "✓ Active",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonEmerald,
                    maxLines = 1
                )
            } else if (isLockedVip) {
                Text(
                    text = "👑 Unlock VIP",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    maxLines = 1
                )
            } else if (isDownloaded) {
                Text(
                    text = "✨ Apply",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "☁️ Download",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StaticPhotoBannersSection(
    photoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
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
    val galleryLocked = !licenseState.isThemeItemUnlocked("custom_gallery_photo", isPermanentlyFree = false)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.custom_photo_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (galleryLocked) {
                        VipCornerPill(text = "👑 VIP")
                    }
                }
                Text(
                    text = stringResource(R.string.custom_photo_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }
        }

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
                    text = activePreset?.title ?: "Active Keyboard Theme Photo",
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BannerPresetCatalog.staticBannerPresets.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        val isSelected = photoTheme.presetId == preset.id && isStaticBannerMode
                        val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
                        StaticBannerTile(
                            preset = preset,
                            selected = isSelected,
                            isLockedVip = isLocked,
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
                    text = if (galleryLocked) {
                        "👑 Gallery Photo (VIP)"
                    } else {
                        stringResource(
                            if (photoTheme.hasCustomGalleryPhoto) R.string.action_change_photo
                            else R.string.action_choose_gallery_photo
                        )
                    },
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
            VipCornerPill(
                text = "👑 VIP",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KeyChassisDark)
            .border(1.2.dp, FlagshipGoldCyanBrush, RoundedCornerShape(18.dp))
    ) {
        // Dual-Layer Status Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0E1D))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "L1: ${settings.effect.shortLabel}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF121C33))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = "L2: ${settings.touchEffect.badge}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22162E))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
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
                        .height(94.dp)
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

                            // Layer 1: Ambient Physics RGB Color
                            val ambientColor = RgbLightingEngine.computeKeyColor(
                                nx = nx,
                                ny = ny,
                                keyHash = r * 31 + c * 17,
                                timeSec = timeSec,
                                settings = settings,
                                ripples = emptyList(),
                                isPressed = false,
                                keyLabel = ('a' + (idx % 26)).toString()
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
                            } else {
                                when (settings.keycapStyle) {
                                    KeycapStyle.PUDDING -> {
                                        drawRoundRect(
                                            color = ambientColor.copy(alpha = 0.82f),
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
                                            color = ambientColor,
                                            topLeft = Offset(x, y),
                                            size = Size(keyW, keyH),
                                            cornerRadius = cr,
                                            style = Stroke(width = 1.4.dp.toPx())
                                        )
                                    }
                                    KeycapStyle.NEON_WIREFRAME -> {
                                        drawRoundRect(
                                            color = ambientColor,
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
                                            color = ambientColor,
                                            topLeft = Offset(x + keyW * 0.25f, y + keyH - 4.dp.toPx()),
                                            size = Size(keyW * 0.5f, 2.dp.toPx()),
                                            cornerRadius = CornerRadius(2f, 2f)
                                        )
                                    }
                                }
                            }

                            // Layer 2: Simultaneous Touch-Reactive Typing Effect Overlay
                            val touchOverlay = RgbLightingEngine.computeTouchPreviewColor(
                                touchEffect = settings.touchEffect,
                                nx = nx,
                                ny = ny,
                                row = r,
                                col = c,
                                rows = rows,
                                cols = cols,
                                timeSec = timeSec,
                                palette = settings.palette,
                                glowIntensity = settings.glowIntensity
                            )
                            if (touchOverlay.alpha > 0.04f) {
                                drawRoundRect(
                                    color = touchOverlay.copy(alpha = (touchOverlay.alpha * 0.62f).coerceIn(0f, 0.85f)),
                                    topLeft = Offset(x, y),
                                    size = Size(keyW, keyH),
                                    cornerRadius = cr
                                )
                                drawRoundRect(
                                    color = touchOverlay.copy(alpha = touchOverlay.alpha.coerceIn(0.45f, 1f)),
                                    topLeft = Offset(x, y),
                                    size = Size(keyW, keyH),
                                    cornerRadius = cr,
                                    style = Stroke(width = 1.6.dp.toPx())
                                )
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
                val isNormalBlack = effect == RgbEffect.NORMAL_DEFAULT
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val idx = r * cols + c
                        val col = RgbLightingEngine.computeKeyColor(
                            nx = (c + 0.5f) / cols,
                            ny = (r + 0.5f) / rows,
                            keyHash = r * 19 + c * 11,
                            timeSec = timeSec,
                            settings = previewSettings,
                            ripples = emptyList(),
                            isPressed = false,
                            keyLabel = ('q'.code + idx).toChar().toString()
                        )
                        drawRoundRect(
                            color = if (isNormalBlack || col == Color.Transparent) Color(0xFF1A1B23) else col,
                            topLeft = Offset(pad + c * (kw + gap), pad + r * (kh + gap)),
                            size = Size(kw, kh),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }

            if (isLockedVip) {
                VipCornerPill(
                    text = "👑 VIP",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )
            } else if (!effect.isVipEffect) {
                FreeCornerPill(
                    text = "FREE",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
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
    isLockedVip: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) GlassCardSelected else GlassCardSurface)
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) Color.White else if (isLockedVip) NeonAmber.copy(alpha = 0.6f) else GlassBorder,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = palette.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isLockedVip) {
                VipCornerPill(text = "👑 VIP")
            }
        }
    }
}

@Composable
private fun KeycapStyleCard(
    style: KeycapStyle,
    accentColor: Color,
    selected: Boolean,
    isLockedVip: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) GlassCardSelected else GlassCardSurface)
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) Color.White else if (isLockedVip) NeonAmber.copy(alpha = 0.6f) else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("keycap_style_${style.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
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
            if (isLockedVip) {
                VipCornerPill(text = "👑 VIP")
            }
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
private fun VipCornerPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontFamily = JetBrainsMonoFamily,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xD9330B28))
            .border(0.8.dp, NeonAmber, RoundedCornerShape(999.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
    )
}

@Composable
private fun FreeCornerPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontFamily = JetBrainsMonoFamily,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xD9052B1E))
            .border(0.8.dp, NeonEmerald, RoundedCornerShape(999.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
    )
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
