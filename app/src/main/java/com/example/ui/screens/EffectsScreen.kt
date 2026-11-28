package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.model.AppUiLanguage
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.LocalAppStrings
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.TouchEffectCategory
import com.example.model.TouchTypingEffect
import com.example.ui.components.BackdropBrightnessEditorDialog
import com.example.ui.components.BackdropEditTarget
import com.example.ui.components.CustomizerTarget
import com.example.ui.components.FullMiniKeyboardPreview
import com.example.ui.components.LuxuryStudioCategoryGrid
import com.example.ui.components.MiniKeyboardEffectPreview
import com.example.ui.components.StudioEffectsCategory
import com.example.ui.components.ThemeCustomizerModal
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipGlassRimGoldSubtle
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.FlagshipGoldCyanBrush
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
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.luminousGlassCard
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun EffectsScreen(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    downloadedThemeIds: Set<String>,
    downloadingThemeIds: Set<String>,
    selectedCategory: StudioEffectsCategory,
    categoryJumpTrigger: Int,
    onSelectCategory: (StudioEffectsCategory) -> Unit,
    onApplyVipToKeyboard: () -> Unit,
    onApplyComboTheme: (ComboThemePreset) -> Unit,
    onDownloadComboTheme: (ComboThemePreset) -> Unit,
    onApplyDefaultBlackPreservingTouch: () -> Unit,
    onApplyBannerPhotoPreservingTouch: (String) -> Unit,
    onApplyComboPhotoPreservingTouch: (ComboThemePreset) -> Unit,
    onClearAnimationsPreservingBackdrop: () -> Unit,
    onSelectTouchEffect: (TouchTypingEffect) -> Unit,
    onApplyCloudAnimation: (CloudAnimationConfig) -> Unit,
    onApplyThemeCustomizerSelection: (
        bannerPresetId: String?,
        combo: ComboThemePreset?,
        effect: RgbEffect,
        touchEffect: TouchTypingEffect,
        palette: RgbPalette,
        showKeyBorders: Boolean,
        clearPhotoToBlack: Boolean
    ) -> Unit,
    onPickCustomPhoto: (Uri) -> Unit,
    onPhotoDimChange: (Float) -> Unit,
    onRemoveCustomPhoto: () -> Unit,
    onSelectEffect: (RgbEffect) -> Unit,
    onSelectPalette: (RgbPalette) -> Unit,
    onSelectKeycapStyle: (KeycapStyle) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onGlowChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onShowKeyBordersChange: (Boolean) -> Unit,
    onIlluminatedLegendsChange: (Boolean) -> Unit,
    onChassisAuraChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var selectedTouchCatName by rememberSaveable { mutableStateOf(TouchEffectCategory.ALL.name) }
    val selectedTouchCategory = remember(selectedTouchCatName) {
        TouchEffectCategory.entries.firstOrNull { it.name == selectedTouchCatName } ?: TouchEffectCategory.ALL
    }

    var selectedComboCatName by rememberSaveable { mutableStateOf(ComboCategory.ALL.name) }
    val selectedComboCategory = remember(selectedComboCatName) {
        ComboCategory.entries.firstOrNull { it.name == selectedComboCatName } ?: ComboCategory.ALL
    }

    var customizerTarget by remember { mutableStateOf<CustomizerTarget?>(null) }
    var backdropEditTarget by remember { mutableStateOf<BackdropEditTarget?>(null) }
    var showAdvancedStudio by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val unifiedEngineCategories = remember {
        listOf(
            StudioEffectsCategory.TOUCH_RESPONSES,
            StudioEffectsCategory.THEMES,
            StudioEffectsCategory.ANIMATIONS,
            StudioEffectsCategory.PHOTOS
        )
    }

    // Step-by-step hierarchical Back navigation inside EffectsScreen
    BackHandler(enabled = selectedCategory != StudioEffectsCategory.TOUCH_RESPONSES && !showAdvancedStudio && customizerTarget == null && backdropEditTarget == null) {
        onSelectCategory(StudioEffectsCategory.TOUCH_RESPONSES)
    }
    BackHandler(enabled = showAdvancedStudio && customizerTarget == null && backdropEditTarget == null) {
        showAdvancedStudio = false
    }
    BackHandler(enabled = customizerTarget != null) {
        customizerTarget = null
    }
    BackHandler(enabled = backdropEditTarget != null) {
        backdropEditTarget = null
    }

    val filteredTouchEffects = remember(selectedTouchCategory) {
        TouchTypingEffect.byCategory(selectedTouchCategory)
    }
    val allCombos = remember(licenseState.adminConfig.cloudThemes) {
        ComboThemeCatalog.byCategory(ComboCategory.ALL, licenseState.adminConfig.cloudThemes)
    }
    val filteredCombos = remember(selectedComboCategory, licenseState.adminConfig.cloudThemes) {
        ComboThemeCatalog.byCategory(selectedComboCategory, licenseState.adminConfig.cloudThemes)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri: Uri? ->
        if (selectedUri != null) {
            backdropEditTarget = BackdropEditTarget.PhotoUri(selectedUri)
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

    // Smoothly scroll to the Live Keyboard Preview + Selected Section when triggered from Home or category card tap
    LaunchedEffect(categoryJumpTrigger) {
        if (categoryJumpTrigger > 0) {
            listState.animateScrollToItem(2)
        }
    }

    val themesLocked = !licenseState.isRgbThemesUnlocked

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Bold Luxury Studio Header + Quick Customizer Trigger
            item(key = "studio_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Luxury Studio",
                            fontFamily = OrbitronFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Curate HD photos, banners, live RGB motion & ${TouchTypingEffect.byCategory(TouchEffectCategory.ALL).size} touch engines",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {
                            customizerTarget = CustomizerTarget(
                                title = "Active Keyboard Theme",
                                subtitle = "Live full Gboard preview · Toggle Key borders & test keys",
                                badge = "CUSTOMIZE",
                                bannerPresetId = customPhotoTheme.presetId,
                                effect = settings.effect,
                                touchEffect = settings.touchEffect,
                                palette = settings.palette,
                                isLocked = !licenseState.isThemeItemUnlocked(settings.effect.id, !settings.effect.isVipEffect)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_open_active_customizer")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.effects_header),
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Customize",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 2. Five Large Luxury Category Cards with Direct Tap Navigation & Clear Badges
            item(key = "category_grid") {
                val adminBannersCount = listOf(
                    licenseState.adminConfig.banners.resolvedBanner1Image,
                    licenseState.adminConfig.banners.resolvedBanner2Image
                ).count { it.isNotBlank() }

                LuxuryStudioCategoryGrid(
                    selectedCategory = selectedCategory,
                    photosBadge = "Gallery + ${BannerPresetCatalog.staticBannerPresets.size} HD",
                    bannersBadge = "${BannerPresetCatalog.staticBannerPresets.size + adminBannersCount} Featured",
                    animationsBadge = "${RgbEffect.ambientPhysicsEffects.size} Live RGB",
                    touchResponsesBadge = "${TouchTypingEffect.byCategory(TouchEffectCategory.ALL).size} AI FX",
                    themesBadge = "${allCombos.size} Packs",
                    onSelectCategory = { category ->
                        onSelectCategory(category)
                        coroutineScope.launch {
                            listState.animateScrollToItem(2)
                        }
                    }
                )
            }

            // 3. Live Interactive Full-Width Gboard Preview Anchor
            item(key = "live_preview") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                FullMiniKeyboardPreview(
                    settings = settings,
                    timeSec = timeSec,
                    customPhotoTheme = customPhotoTheme,
                    isInteractive = true,
                    compact = false
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .luminousGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            borderBrush = FlagshipGlassRimSubtle,
                            borderWidth = 1.dp,
                            glowColor = NeonCyan,
                            glowElevation = 6.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val backdropLabel = when {
                            customPhotoTheme.hasCustomGalleryPhoto -> "Custom Photo"
                            !customPhotoTheme.presetId.isNullOrBlank() ->
                                BannerPresetCatalog.findById(customPhotoTheme.presetId)?.title ?: "HD Photo"
                            !customPhotoTheme.downloadedThemeId.isNullOrBlank() ->
                                ComboThemeCatalog.findById(customPhotoTheme.downloadedThemeId)?.title ?: "Theme Artwork"
                            else -> "Default Black"
                        }
                        val engineLabel = when {
                            settings.effect != RgbEffect.NORMAL_DEFAULT && settings.touchEffect != TouchTypingEffect.NONE ->
                                "${settings.touchEffect.shortLabel} + ${settings.effect.shortLabel}"
                            settings.touchEffect != TouchTypingEffect.NONE -> settings.touchEffect.shortLabel
                            settings.effect != RgbEffect.NORMAL_DEFAULT -> settings.effect.shortLabel
                            else -> "Clean Keycaps"
                        }
                        Text(
                            text = "$backdropLabel  •  $engineLabel",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap any key above to test live feel & lighting",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onShowKeyBordersChange(!settings.showKeyBorders) }
                            .padding(horizontal = 8.dp)
                            .testTag("quick_toggle_key_borders")
                    ) {
                        Text(
                            text = "Key borders",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Switch(
                            checked = settings.showKeyBorders,
                            onCheckedChange = onShowKeyBordersChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RoyalBlueDeep,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = GlassCardInner,
                                uncheckedBorderColor = GlassBorderStrong
                            )
                        )
                    }
                }

                if (customPhotoTheme.hasPhoto) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassCardInner)
                            .border(1.dp, FlagshipGlassRimSubtle, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val brightnessFraction = CustomBackgroundManager.dimAlphaToBrightness(customPhotoTheme.dimAlpha)
                            TuningSliderRow(
                                label = "Adjust Brightness",
                                valueLabel = "${(brightnessFraction * 100f).roundToInt()}%",
                                value = brightnessFraction,
                                valueRange = 0f..1f,
                                onValueChange = { nextBrightness ->
                                    onPhotoDimChange(CustomBackgroundManager.brightnessToDimAlpha(nextBrightness))
                                },
                                testTag = "slider_top_photo_dim"
                            )
                        }
                        OutlinedButton(
                            onClick = onRemoveCustomPhoto,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_remove_custom_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.action_remove_photo),
                                tint = NeonMagenta,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            }

            // VIP Expired Lock Notice + Unlock CTA
            if (themesLocked) {
                item(key = "vip_notice") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .luminousGlassCard(
                                shape = RoundedCornerShape(20.dp),
                                borderBrush = FlagshipGoldCyanBrush,
                                borderWidth = 1.5.dp,
                                glowColor = RoyalGold,
                                glowElevation = 14.dp
                            )
                            .padding(16.dp)
                            .testTag("effects_live_demo_banner"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "VIP Themes & Touch FX Preview — Unlock to save to your live system keyboard",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                                .testTag("btn_unlock_vip_themes")
                        ) {
                            Text(
                                text = stringResource(R.string.bn_btn_apply_to_real_keyboard),
                                fontFamily = SpaceGroteskFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // 4. Unified 4-Way Section Selector Bar (Touch FX | Themes | RGB Design | Photos)
            item(key = "unified_section_bar") {
                val unifiedLabels = listOf(
                    "Touch FX",
                    "Themes",
                    "RGB Design",
                    "Photos"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassCardInner)
                        .border(1.dp, GlassBorderStrong, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    unifiedLabels.forEachIndexed { idx, label ->
                        val isActiveTab = selectedCategory == unifiedEngineCategories[idx]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActiveTab) GlassCardSelected else Color.Transparent)
                                .border(
                                    width = if (isActiveTab) 1.3.dp else 0.dp,
                                    brush = if (isActiveTab) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectCategory(unifiedEngineCategories[idx])
                                }
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .testTag("effects_unified_bar_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = SpaceGroteskFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isActiveTab) NeonCyan else Color.White.copy(alpha = 0.80f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            when (selectedCategory) {
                StudioEffectsCategory.TOUCH_RESPONSES -> {
                    touchResponsesLuxuryItems(
                        settings = settings,
                        licenseState = licenseState,
                        timeProvider = { timeSec },
                        selectedCategory = selectedTouchCategory,
                        filteredEffects = filteredTouchEffects,
                        onSelectCategory = { selectedTouchCatName = it.name },
                        onTurnOffTouch = onClearAnimationsPreservingBackdrop,
                        onQuickApplyTouchEffect = { fx ->
                            val isLocked = !licenseState.isThemeItemUnlocked(fx.id, isPermanentlyFree = !fx.isVipEffect)
                            if (isLocked) onApplyVipToKeyboard() else onSelectTouchEffect(fx)
                        },
                        onOpenTouchCustomizer = { fx ->
                            val isLocked = !licenseState.isThemeItemUnlocked(fx.id, isPermanentlyFree = !fx.isVipEffect)
                            customizerTarget = CustomizerTarget(
                                title = fx.displayName,
                                subtitle = fx.description,
                                badge = fx.badge,
                                bannerPresetId = customPhotoTheme.presetId,
                                effect = RgbEffect.NORMAL_DEFAULT,
                                touchEffect = fx,
                                palette = settings.palette,
                                allowBackdropSwitch = true,
                                forceDefaultBlackBackdrop = !customPhotoTheme.hasPhoto,
                                isLocked = isLocked
                            )
                        }
                    )
                }

                StudioEffectsCategory.THEMES -> {
                    themesLuxuryItems(
                        settings = settings,
                        customPhotoTheme = customPhotoTheme,
                        licenseState = licenseState,
                        downloadedThemeIds = downloadedThemeIds,
                        downloadingThemeIds = downloadingThemeIds,
                        timeProvider = { timeSec },
                        selectedCategory = selectedComboCategory,
                        filteredCombos = filteredCombos,
                        onSelectCategory = { selectedComboCatName = it.name },
                        onOpenComboCustomizer = { combo ->
                            val isLocked = !licenseState.isThemeItemUnlocked(combo.id, isPermanentlyFree = combo.isCloudFree)
                            customizerTarget = CustomizerTarget(
                                title = combo.title,
                                subtitle = combo.subtitle,
                                badge = combo.badge,
                                bannerPresetId = combo.bannerPresetId,
                                combo = combo,
                                effect = combo.effect,
                                touchEffect = TouchTypingEffect.NONE,
                                palette = combo.palette,
                                previewColors = combo.previewColors,
                                remoteImageUrl = combo.remoteImageUrl,
                                isLocked = isLocked
                            )
                        },
                        onQuickApplyCombo = { combo ->
                            val isLocked = !licenseState.isThemeItemUnlocked(combo.id, isPermanentlyFree = combo.isCloudFree)
                            if (isLocked) {
                                onApplyVipToKeyboard()
                            } else {
                                onApplyComboTheme(combo)
                            }
                        }
                    )
                }

                StudioEffectsCategory.ANIMATIONS -> {
                    animationsLuxuryItems(
                        settings = settings,
                        licenseState = licenseState,
                        timeProvider = { timeSec },
                        onSelectStaticClean = { onSelectEffect(RgbEffect.NORMAL_DEFAULT) },
                        onSelectAmbientEffect = { fx ->
                            val isLocked = !licenseState.isThemeItemUnlocked(fx.id, isPermanentlyFree = !fx.isVipEffect)
                            if (isLocked) {
                                onApplyVipToKeyboard()
                            } else {
                                onSelectEffect(fx)
                            }
                        },
                        onApplyCloudAnimation = { anim ->
                            val isLocked = !licenseState.isThemeItemUnlocked(anim.id, isPermanentlyFree = anim.isFree)
                            if (isLocked) onApplyVipToKeyboard() else onApplyCloudAnimation(anim)
                        }
                    )
                }

                StudioEffectsCategory.PHOTOS -> {
                    photosLuxuryItems(
                        settings = settings,
                        customPhotoTheme = customPhotoTheme,
                        licenseState = licenseState,
                        onSelectDefaultBlack = onApplyDefaultBlackPreservingTouch,
                        onPickPhotoClick = {
                            val photoLocked = !licenseState.isThemeItemUnlocked("custom_gallery_photo", isPermanentlyFree = true)
                            if (photoLocked) {
                                onApplyVipToKeyboard()
                            } else {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        onRemoveCustomPhoto = onRemoveCustomPhoto,
                        onSelectHdPhoto = { preset ->
                            val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
                            if (isLocked) {
                                onApplyVipToKeyboard()
                            } else {
                                backdropEditTarget = BackdropEditTarget.BannerPreset(preset)
                            }
                        }
                    )
                }

                StudioEffectsCategory.BANNERS -> {
                    bannersLuxuryItems(
                        settings = settings,
                        customPhotoTheme = customPhotoTheme,
                        licenseState = licenseState,
                        onOpenBannerCustomizer = { preset ->
                            val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
                            if (isLocked) {
                                customizerTarget = CustomizerTarget(
                                    title = preset.title,
                                    subtitle = preset.subtitle.ifBlank { "Featured Banner Wallpaper" },
                                    badge = "BANNER",
                                    bannerPresetId = preset.id,
                                    effect = RgbEffect.NORMAL_DEFAULT,
                                    touchEffect = settings.touchEffect,
                                    palette = settings.palette,
                                    previewColors = preset.previewColors,
                                    isLocked = true
                                )
                            } else {
                                backdropEditTarget = BackdropEditTarget.BannerPreset(preset)
                            }
                        },
                        onQuickApplyBanner = { preset ->
                            val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
                            if (isLocked) {
                                onApplyVipToKeyboard()
                            } else {
                                onApplyBannerPhotoPreservingTouch(preset.id)
                            }
                        },
                        onOpenRemoteBannerCustomizer = { title, subtitle, url ->
                            if (themesLocked) {
                                onApplyVipToKeyboard()
                            } else {
                                backdropEditTarget = BackdropEditTarget.RemoteBanner(
                                    title = title,
                                    subtitle = subtitle,
                                    imageSource = url
                                )
                            }
                        }
                    )
                }
            }

            // 5. Collapsible Color Palette, Keycap Style & Gboard Optical Tuning Studio
            item(key = "advanced_studio") {
                Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .luminousGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        borderBrush = FlagshipGlassRimGoldSubtle,
                        borderWidth = 1.2.dp,
                        glowColor = NeonAmber,
                        glowElevation = 10.dp
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { showAdvancedStudio = !showAdvancedStudio }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Palette, Keycaps & Optical Tuning",
                                fontFamily = SpaceGroteskFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Customize RGB palettes, keycap shapes, corner radius & glow",
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showAdvancedStudio) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                AnimatedVisibility(visible = showAdvancedStudio) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionTitle(title = "Color Palettes")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(RgbPalette.entries) { palette ->
                                val selected = settings.palette == palette
                                Column(
                                    modifier = Modifier
                                        .width(142.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) GlassCardSelected else GlassCardInner)
                                        .border(
                                            width = if (selected) 1.6.dp else 1.dp,
                                            brush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onSelectPalette(palette) }
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Brush.horizontalGradient(palette.colors))
                                    )
                                    Text(
                                        text = palette.displayName,
                                        fontFamily = SpaceGroteskFamily,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        SectionTitle(title = "Keycap Style")
                        KeycapStyle.entries.forEach { style ->
                            val selected = settings.keycapStyle == style
                            val accentColor = RgbLightingEngine.samplePalette(settings.palette, timeSec * 0.2f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) GlassCardSelected else GlassCardInner)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        brush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onSelectKeycapStyle(style) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(width = 52.dp, height = 38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cr = CornerRadius(6.dp.toPx(), 6.dp.toPx())
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
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = style.displayName,
                                        fontFamily = SpaceGroteskFamily,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = style.description,
                                        fontFamily = PlusJakartaSansFamily,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.82f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        SectionTitle(title = "Optical Tuning")
                        ToggleSettingRow(
                            title = "Key borders",
                            subtitle = "Show crisp 0.8dp Gboard outlines around keys (default ON)",
                            checked = settings.showKeyBorders,
                            onCheckedChange = onShowKeyBordersChange,
                            testTag = "switch_key_borders"
                        )
                        TuningSliderRow(
                            label = stringResource(R.string.label_animation_speed),
                            valueLabel = "${String.format("%.1f", settings.animationSpeed)}x",
                            value = settings.animationSpeed,
                            valueRange = 0.3f..2.2f,
                            onValueChange = onSpeedChange,
                            testTag = "slider_speed"
                        )
                        TuningSliderRow(
                            label = stringResource(R.string.label_glow_intensity),
                            valueLabel = "${(settings.glowIntensity * 100).toInt()}%",
                            value = settings.glowIntensity,
                            valueRange = 0.25f..1.0f,
                            onValueChange = onGlowChange,
                            testTag = "slider_glow"
                        )
                        TuningSliderRow(
                            label = stringResource(R.string.label_corner_radius),
                            valueLabel = "${settings.keyCornerRadiusDp.toInt()} dp",
                            value = settings.keyCornerRadiusDp,
                            valueRange = 2f..15f,
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
                }
            }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Gboard-Style Full Keyboard Theme Customizer Modal
    customizerTarget?.let { target ->
        ThemeCustomizerModal(
            target = target,
            settings = settings,
            customPhotoTheme = customPhotoTheme,
            timeSec = timeSec,
            onDismiss = { customizerTarget = null },
            onApply = { showBorders, useDefaultBlack ->
                onApplyThemeCustomizerSelection(
                    target.bannerPresetId,
                    target.combo,
                    target.effect,
                    target.touchEffect,
                    target.palette,
                    showBorders,
                    useDefaultBlack
                )
                customizerTarget = null
            },
            onOpenVipUnlock = {
                customizerTarget = null
                onApplyVipToKeyboard()
            }
        )
    }

    backdropEditTarget?.let { editTarget ->
        BackdropBrightnessEditorDialog(
            target = editTarget,
            settings = settings,
            customPhotoTheme = customPhotoTheme,
            timeSec = timeSec,
            onDismiss = { backdropEditTarget = null },
            onApplyCroppedPhoto = { bmp, dimAlpha, showBorders ->
                coroutineScope.launch {
                    CustomBackgroundManager.getInstance(context).saveCroppedPhotoBitmap(bmp, dimAlpha)
                    onShowKeyBordersChange(showBorders)
                    if (settings.effect != RgbEffect.NORMAL_DEFAULT) {
                        onSelectEffect(RgbEffect.NORMAL_DEFAULT)
                    }
                    backdropEditTarget = null
                }
            },
            onApplyBannerPreset = { presetId, dimAlpha, showBorders ->
                CustomBackgroundManager.getInstance(context).selectPresetWithDim(presetId, dimAlpha)
                onShowKeyBordersChange(showBorders)
                onApplyBannerPhotoPreservingTouch(presetId)
                backdropEditTarget = null
            },
            onApplyRemoteBanner = { imageSource, dimAlpha, showBorders ->
                coroutineScope.launch {
                    CustomBackgroundManager.getInstance(context).saveRemoteBannerImage(imageSource, dimAlpha)
                    onShowKeyBordersChange(showBorders)
                    if (settings.effect != RgbEffect.NORMAL_DEFAULT) {
                        onSelectEffect(RgbEffect.NORMAL_DEFAULT)
                    }
                    backdropEditTarget = null
                }
            }
        )
    }
}

@Composable
private fun LuxurySectionHeader(
    title: String,
    subtitle: String,
    countBadge: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontFamily = OrbitronFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.80f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        FreeCornerPill(text = countBadge)
    }
}

private fun LazyListScope.photosLuxuryItems(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    onSelectDefaultBlack: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onRemoveCustomPhoto: () -> Unit,
    onSelectHdPhoto: (BannerThemePreset) -> Unit
) {
    val photoLocked = !licenseState.isThemeItemUnlocked("custom_gallery_photo", isPermanentlyFree = true)
    val isDefaultBlackActive = !customPhotoTheme.hasPhoto && settings.effect == RgbEffect.NORMAL_DEFAULT

    item(key = "photos_header") {
        LuxurySectionHeader(
            title = "Photos",
            subtitle = "Gallery & HD Photo Backgrounds (preserves active touch response)",
            countBadge = "Gallery + ${BannerPresetCatalog.staticBannerPresets.size} HD"
        )
    }

    item(key = "photos_gallery_hero") {
        val customBitmap = remember(customPhotoTheme.cacheKey) {
            val path = customPhotoTheme.photoPath
            if (customPhotoTheme.hasCustomGalleryPhoto && !path.isNullOrBlank() && File(path).exists()) {
                runCatching {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
                }.getOrNull()
            } else null
        }

        // Large Upload Custom Photo Hero Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 136.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    borderBrush = if (customPhotoTheme.hasCustomGalleryPhoto) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                    borderWidth = if (customPhotoTheme.hasCustomGalleryPhoto) 1.6.dp else 1.1.dp,
                    glowColor = NeonCyan,
                    glowElevation = 10.dp
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(124.dp)
                    .height(92.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF121622))
                    .border(1.dp, GlassBorderStrong, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (customBitmap != null) {
                    MiniKeyboardEffectPreview(
                        customBitmap = customBitmap,
                        dimAlpha = customPhotoTheme.dimAlpha,
                        showKeyBorders = settings.showKeyBorders,
                        showTopStrip = false,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "Gallery",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Gallery Photo",
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (photoLocked) VipCornerPill(text = "VIP") else FreeCornerPill(text = "FREE")
                }
                Text(
                    text = "Set any personal portrait or wallpaper behind frosted Gboard keys",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPickPhotoClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF061020)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_pick_custom_photo")
                    ) {
                        Text(
                            text = if (customPhotoTheme.hasCustomGalleryPhoto) "Change Photo" else "Pick Photo",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (customPhotoTheme.hasPhoto) {
                        OutlinedButton(
                            onClick = onRemoveCustomPhoto,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.action_remove_photo),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    item(key = "photos_classic_black") {
        // Large Default Black Card (Always Free)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = if (isDefaultBlackActive) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                    borderWidth = if (isDefaultBlackActive) 1.6.dp else 1.dp,
                    glowColor = NeonCyan,
                    glowElevation = 8.dp,
                    surfaceColor = if (isDefaultBlackActive) GlassCardSelected else GlassCardSurface
                )
                .clickable(onClick = onSelectDefaultBlack)
                .padding(14.dp)
                .testTag("card_classic_black_free"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MiniKeyboardEffectPreview(
                forceDefaultBlack = true,
                touchEffect = settings.touchEffect,
                palette = settings.palette,
                showKeyBorders = settings.showKeyBorders,
                showTopStrip = false,
                modifier = Modifier.size(width = 96.dp, height = 60.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Classic Black",
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Sleek Gboard dark charcoal chassis — permanently FREE",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }
            FreeCornerPill(text = if (isDefaultBlackActive) "ACTIVE ✓" else "FREE")
        }
    }

    val photoRows = BannerPresetCatalog.staticBannerPresets.chunked(2)
    items(
        items = photoRows,
        key = { row -> "photo_row_${row.first().id}" }
    ) { rowPresets ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowPresets.forEach { preset ->
                val selected = customPhotoTheme.presetId == preset.id
                val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 188.dp)
                        .luminousGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                            borderWidth = if (selected) 1.7.dp else 1.dp,
                            glowColor = NeonCyan,
                            glowElevation = if (selected) 14.dp else 6.dp,
                            surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
                        )
                        .clickable { onSelectHdPhoto(preset) }
                        .padding(12.dp)
                        .testTag("banner_card_${preset.id}"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MiniKeyboardEffectPreview(
                            bannerPresetId = preset.id,
                            gradientColors = preset.previewColors,
                            showKeyBorders = settings.showKeyBorders,
                            showTopStrip = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            when {
                                isLocked -> VipCornerPill(text = "VIP")
                                selected -> FreeCornerPill(text = "ACTIVE ✓")
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = preset.title,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.subtitle.ifBlank { "HD Wallpaper" },
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (rowPresets.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun LazyListScope.bannersLuxuryItems(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    onOpenBannerCustomizer: (BannerThemePreset) -> Unit,
    onQuickApplyBanner: (BannerThemePreset) -> Unit,
    onOpenRemoteBannerCustomizer: (title: String, subtitle: String, imageUrl: String) -> Unit
) {
    val bCfg = licenseState.adminConfig.banners
    val adminBanners = buildList {
        if (bCfg.resolvedBanner1Image.isNotBlank()) {
            add(Triple(bCfg.banner1Title, bCfg.banner1Subtitle, bCfg.resolvedBanner1Image))
        }
        if (bCfg.resolvedBanner2Image.isNotBlank()) {
            add(Triple(bCfg.banner2Title, bCfg.banner2Subtitle, bCfg.resolvedBanner2Image))
        }
    }

    item(key = "banners_header") {
        LuxurySectionHeader(
            title = "Banners",
            subtitle = "Featured Banner Wallpapers — full-bleed widescreen studio art",
            countBadge = "${BannerPresetCatalog.staticBannerPresets.size + adminBanners.size} Featured"
        )
    }

    items(
        items = adminBanners.mapIndexed { idx, triple -> idx to triple },
        key = { (idx, _) -> "admin_banner_$idx" }
    ) { (idx, itemData) ->
        val (title, subtitle, imageUrl) = itemData
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .luminousGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    borderBrush = FlagshipGoldCyanBrush,
                    borderWidth = 1.4.dp,
                    glowColor = RoyalGold,
                    glowElevation = 10.dp
                )
                .clickable { onOpenRemoteBannerCustomizer(title, subtitle, imageUrl) }
                .padding(14.dp)
                .testTag("featured_admin_banner_$idx"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(114.dp)
            ) {
                MiniKeyboardEffectPreview(
                    remoteImageUrl = imageUrl,
                    gradientColors = listOf(0xFF141826, 0xFF28193A),
                    showKeyBorders = settings.showKeyBorders,
                    showTopStrip = true,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    FreeCornerPill(text = "FEATURED")
                }
            }
            Text(
                text = title,
                fontFamily = SpaceGroteskFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    items(
        items = BannerPresetCatalog.staticBannerPresets,
        key = { "banner_preset_card_${it.id}" }
    ) { preset ->
        val selected = customPhotoTheme.presetId == preset.id
        val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 184.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimGoldSubtle,
                    borderWidth = if (selected) 1.7.dp else 1.1.dp,
                    glowColor = if (selected) NeonCyan else RoyalGold,
                    glowElevation = if (selected) 14.dp else 8.dp,
                    surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
                )
                .clickable { onOpenBannerCustomizer(preset) }
                .padding(14.dp)
                .testTag("banner_card_${preset.id}"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MiniKeyboardEffectPreview(
                    bannerPresetId = preset.id,
                    gradientColors = preset.previewColors,
                    showKeyBorders = settings.showKeyBorders,
                    showTopStrip = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    when {
                        isLocked -> VipCornerPill(text = "VIP")
                        selected -> FreeCornerPill(text = "ACTIVE ✓")
                        else -> FreeCornerPill(text = "HD BANNER")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = preset.title,
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = preset.subtitle.ifBlank { "Featured Banner Wallpaper" },
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) NeonEmerald else NeonCyan)
                        .clickable { onQuickApplyBanner(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selected) "Active ✓" else "Apply",
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF061020)
                    )
                }
            }
        }
    }
}

private fun LazyListScope.animationsLuxuryItems(
    settings: KeyboardSettings,
    licenseState: LicenseUiState,
    timeProvider: () -> Float,
    onSelectStaticClean: () -> Unit,
    onSelectAmbientEffect: (RgbEffect) -> Unit,
    onApplyCloudAnimation: (CloudAnimationConfig) -> Unit
) {
    val staticSelected = settings.effect == RgbEffect.NORMAL_DEFAULT

    item(key = "animations_header") {
        LuxurySectionHeader(
            title = "Animations",
            subtitle = "Live Ambient & Motion Effects — layer seamlessly with any photo",
            countBadge = "${RgbEffect.ambientPhysicsEffects.size} Live RGB"
        )
    }

    item(key = "animations_static_clean") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = if (staticSelected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                    borderWidth = if (staticSelected) 1.5.dp else 1.dp,
                    glowColor = NeonEmerald,
                    glowElevation = 8.dp,
                    surfaceColor = if (staticSelected) GlassCardSelected else GlassCardSurface
                )
                .clickable(onClick = onSelectStaticClean)
                .padding(14.dp)
                .testTag("ambient_effect_none"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Static Clean (No Ambient Loop)",
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Keeps keys sleek while letting your active Touch Response shine",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            FreeCornerPill(text = if (staticSelected) "ACTIVE ✓" else "FREE")
        }
    }

    if (licenseState.adminConfig.cloudAnimations.isNotEmpty()) {
        item(key = "animations_cloud_header") {
            SectionTitle(title = "Cloud Motion Effects")
        }
        items(
            items = licenseState.adminConfig.cloudAnimations,
            key = { "cloud_anim_${it.id}" }
        ) { anim ->
            val isLocked = !licenseState.isThemeItemUnlocked(anim.id, isPermanentlyFree = anim.isFree)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .luminousGlassCard(
                        shape = RoundedCornerShape(16.dp),
                        borderBrush = FlagshipCyanMagentaBrush,
                        borderWidth = 1.1.dp,
                        glowColor = NeonCyan,
                        glowElevation = 8.dp
                    )
                    .clickable { onApplyCloudAnimation(anim) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    MiniKeyboardEffectPreview(
                        effect = RgbEffect.fromSavedName(anim.baseEffectId),
                        palette = settings.palette,
                        glowIntensity = settings.glowIntensity,
                        animationSpeed = settings.animationSpeed,
                        timeProvider = timeProvider,
                        showKeyBorders = settings.showKeyBorders,
                        showTopStrip = false,
                        modifier = Modifier.size(width = 88.dp, height = 52.dp)
                    )
                    Column {
                        Text(
                            text = anim.title,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Synced Cloud RGB Animation",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
                if (isLocked) VipCornerPill(text = "VIP") else FreeCornerPill(text = "CLOUD")
            }
        }
    }

    val animRows = RgbEffect.ambientPhysicsEffects.chunked(2)
    items(
        items = animRows,
        key = { row -> "anim_row_${row.first().id}" }
    ) { pair ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEach { effect ->
                val selected = settings.effect == effect
                val isLocked = !licenseState.isThemeItemUnlocked(effect.id, isPermanentlyFree = !effect.isVipEffect)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 192.dp)
                        .luminousGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                            borderWidth = if (selected) 1.7.dp else 1.dp,
                            glowColor = NeonCyan,
                            glowElevation = if (selected) 14.dp else 8.dp,
                            surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
                        )
                        .clickable { onSelectAmbientEffect(effect) }
                        .padding(12.dp)
                        .testTag("ambient_effect_card_${effect.id}"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MiniKeyboardEffectPreview(
                            effect = effect,
                            palette = settings.palette,
                            keycapStyle = settings.keycapStyle,
                            glowIntensity = settings.glowIntensity,
                            animationSpeed = settings.animationSpeed,
                            timeProvider = timeProvider,
                            showKeyBorders = settings.showKeyBorders,
                            showTopStrip = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            when {
                                isLocked -> VipCornerPill(text = "VIP")
                                selected -> FreeCornerPill(text = "ACTIVE ✓")
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = effect.shortLabel,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = effect.description,
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun LazyListScope.touchResponsesLuxuryItems(
    settings: KeyboardSettings,
    licenseState: LicenseUiState,
    timeProvider: () -> Float,
    selectedCategory: TouchEffectCategory,
    filteredEffects: List<TouchTypingEffect>,
    onSelectCategory: (TouchEffectCategory) -> Unit,
    onTurnOffTouch: () -> Unit,
    onQuickApplyTouchEffect: (TouchTypingEffect) -> Unit,
    onOpenTouchCustomizer: (TouchTypingEffect) -> Unit
) {
    val offSelected = settings.touchEffect == TouchTypingEffect.NONE

    item(key = "touch_header") {
        LuxurySectionHeader(
            title = "Touch Responses",
            subtitle = "100+ AI & Physics Key-Tap Effects — tap any card to test live above",
            countBadge = "${TouchTypingEffect.byCategory(TouchEffectCategory.ALL).size} AI FX"
        )
    }

    item(key = "touch_off_card") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = if (offSelected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                    borderWidth = if (offSelected) 1.5.dp else 1.dp,
                    glowColor = NeonEmerald,
                    glowElevation = 8.dp,
                    surfaceColor = if (offSelected) GlassCardSelected else GlassCardSurface
                )
                .clickable(onClick = onTurnOffTouch)
                .padding(14.dp)
                .testTag("touch_effect_none"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Off (Clean Keycaps)",
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Pure Gboard keycaps with zero key-tap glow — preserves your photo",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            FreeCornerPill(text = if (offSelected) "ACTIVE ✓" else "FREE")
        }
    }

    item(key = "touch_filter_pills") {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(TouchEffectCategory.entries) { cat ->
                val active = cat == selectedCategory
                Row(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) GlassCardSelected else GlassCardSurface)
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            brush = if (active) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onSelectCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cat.label,
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) NeonCyan else Color.White
                    )
                }
            }
        }
    }

    val touchRows = filteredEffects.chunked(2)
    items(
        items = touchRows,
        key = { row -> "touch_row_${row.first().id}" }
    ) { pair ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEach { fx ->
                val selected = settings.touchEffect == fx
                val isLocked = !licenseState.isThemeItemUnlocked(fx.id, isPermanentlyFree = !fx.isVipEffect)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 212.dp)
                        .luminousGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                            borderWidth = if (selected) 1.7.dp else 1.dp,
                            glowColor = Color(fx.primaryHex),
                            glowElevation = if (selected) 14.dp else 8.dp,
                            surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
                        )
                        .clickable { onQuickApplyTouchEffect(fx) }
                        .padding(12.dp)
                        .testTag("touch_effect_card_${fx.id}"),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MiniKeyboardEffectPreview(
                            touchEffect = fx,
                            palette = settings.palette,
                            glowIntensity = settings.glowIntensity,
                            timeProvider = timeProvider,
                            showKeyBorders = settings.showKeyBorders,
                            bannerPresetId = BannerPresetCatalog.defaultBannerForTouchEffect(fx),
                            dimAlpha = 0.44f,
                            showTopStrip = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            if (isLocked) {
                                VipCornerPill(text = "VIP")
                            } else if (selected) {
                                FreeCornerPill(text = "ACTIVE ✓")
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = fx.shortLabel,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = fx.description,
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fx.badge,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(fx.primaryHex),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF132238))
                                    .border(0.9.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .clickable { onOpenTouchCustomizer(fx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Customize ${fx.shortLabel}",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) NeonEmerald else NeonCyan)
                                    .clickable { onQuickApplyTouchEffect(fx) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selected) "Active ✓" else "Apply",
                                    fontFamily = SpaceGroteskFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF061020)
                                )
                            }
                        }
                    }
                }
            }
            if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun LazyListScope.themesLuxuryItems(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    downloadedThemeIds: Set<String>,
    downloadingThemeIds: Set<String>,
    timeProvider: () -> Float,
    selectedCategory: ComboCategory,
    filteredCombos: List<ComboThemePreset>,
    onSelectCategory: (ComboCategory) -> Unit,
    onOpenComboCustomizer: (ComboThemePreset) -> Unit,
    onQuickApplyCombo: (ComboThemePreset) -> Unit
) {
    item(key = "themes_header") {
        LuxurySectionHeader(
            title = "Themes",
            subtitle = "Complete Luxury Keyboard Themes — paired HD art + ambient RGB",
            countBadge = "${filteredCombos.size} Packs"
        )
    }

    item(key = "themes_filter_pills") {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(ComboCategory.entries) { category ->
                val selected = category == selectedCategory
                Row(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) GlassCardSelected else GlassCardSurface)
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            brush = if (selected) FlagshipGoldCyanBrush else FlagshipGlassRimSubtle,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onSelectCategory(category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.label,
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) RoyalGold else Color.White
                    )
                }
            }
        }
    }

    val comboRows = filteredCombos.chunked(2)
    items(
        items = comboRows,
        key = { row -> "combo_row_${row.first().id}" }
    ) { pair ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEach { combo ->
                val isDownloading = combo.id in downloadingThemeIds
                val isActive = (customPhotoTheme.downloadedThemeId == combo.id ||
                    (!combo.bannerPresetId.isNullOrBlank() && customPhotoTheme.presetId == combo.bannerPresetId)) &&
                    settings.effect == combo.effect &&
                    settings.palette == combo.palette
                val isLocked = !licenseState.isThemeItemUnlocked(combo.id, isPermanentlyFree = combo.isCloudFree)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 216.dp)
                        .luminousGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            borderBrush = if (isActive) FlagshipGoldCyanBrush else FlagshipGlassRimSubtle,
                            borderWidth = if (isActive) 1.7.dp else 1.dp,
                            glowColor = RoyalGold,
                            glowElevation = if (isActive) 14.dp else 8.dp,
                            surfaceColor = if (isActive) GlassCardSelected else GlassCardSurface
                        )
                        .clickable { onQuickApplyCombo(combo) }
                        .padding(12.dp)
                        .testTag("combo_card_${combo.id}"),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MiniKeyboardEffectPreview(
                            effect = combo.effect,
                            palette = combo.palette,
                            keycapStyle = settings.keycapStyle,
                            glowIntensity = settings.glowIntensity,
                            animationSpeed = settings.animationSpeed,
                            timeProvider = timeProvider,
                            showKeyBorders = settings.showKeyBorders,
                            bannerPresetId = combo.bannerPresetId,
                            remoteImageUrl = combo.remoteImageUrl,
                            gradientColors = combo.previewColors,
                            showTopStrip = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            when {
                                isLocked -> VipCornerPill(text = "VIP")
                                isActive -> FreeCornerPill(text = "ACTIVE ✓")
                                combo.isCloudFree -> FreeCornerPill(text = "FREE")
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = combo.title,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = combo.subtitle,
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = combo.effect.shortLabel,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.5.sp,
                            color = NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF132238))
                                    .border(0.9.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .clickable { onOpenComboCustomizer(combo) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Customize ${combo.title}",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    color = NeonCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isActive) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Active",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
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
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xD9330B28))
            .border(0.9.dp, NeonAmber, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
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
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xD9052B1E))
            .border(0.9.dp, NeonEmerald, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontFamily = JetBrainsMonoFamily,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
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
                fontFamily = SpaceGroteskFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = valueLabel,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = NeonCyan,
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
            .heightIn(min = 48.dp)
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
                fontFamily = SpaceGroteskFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RoyalBlueDeep,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = GlassCardInner,
                uncheckedBorderColor = GlassBorderStrong
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
