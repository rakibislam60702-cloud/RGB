package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import com.example.ui.components.BlueVerifiedBadge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.BannerPresetCatalog
import com.example.data.BannerThemePreset
import com.example.data.ComboCategory
import com.example.data.ComboThemeCatalog
import com.example.data.ComboThemePreset
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.LicenseUiState
import com.example.engine.BengaliTransliterator
import com.example.engine.OnlineVoiceTypingController
import com.example.engine.RgbStickerGenerator
import com.example.engine.RgbStickerStyle
import com.example.engine.SmartWordPredictor
import com.example.engine.StylishFontEngine
import com.example.engine.StylishFontStyle
import com.example.model.AppUiLanguage
import com.example.model.EmojiCatalog
import com.example.model.ImeStatus
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardSettings
import com.example.model.LanguageCatalog
import com.example.model.LanguagePackUiState
import com.example.model.LocalAppStrings
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.TouchEffectCategory
import com.example.model.TouchTypingEffect
import com.example.rgbkeyboard.RgbKeyboardView as NativeRgbKeyboardView
import com.example.ui.components.BackdropBrightnessEditorDialog
import com.example.ui.components.BackdropEditTarget
import com.example.ui.components.FeatureTourCoordinates
import com.example.ui.components.FeatureTourTarget
import com.example.ui.components.LuxuryStudioCategoryGrid
import com.example.ui.components.MiniKeyboardEffectPreview
import com.example.ui.components.PromoBannerCarousel
import com.example.ui.components.StudioAiHubPanel
import com.example.ui.components.StudioEffectsCategory
import com.example.ui.components.StudioStylishFontPanel
import com.example.ui.theme.BrandObsidianPill
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipEmeraldCyanBrush
import com.example.ui.theme.FlagshipGlassRimGoldSubtle
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.FlagshipGoldAmberBrush
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.GlassCardSelected
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.GlassDialogSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuxuryCyanStroke
import com.example.ui.theme.LuxuryPinkStroke
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonAmberContainer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldContainer
import com.example.ui.theme.NeonError
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldAccent
import com.example.ui.theme.RoyalGoldPillBg
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.luminousGlassCard

private enum class StudioOverlayMode { NONE, CLIPBOARD, FONTS, EMOJI, STICKER_SELECTOR, AI_HUB }

private enum class HomeTopSection(
    val title: String,
    val subtitle: String,
    val testTag: String
) {
    KEYBOARD("Keyboard", "Live Deck & Languages", "home_top_tab_keyboard"),
    THEMES_RGB("Themes & RGB", "Touch • Themes • RGB • Photos", "home_top_tab_themes_rgb"),
    STYLES("Photos & Art", "Photos • Banners • Packs", "home_top_tab_styles"),
    SETUP("Setup & Guide", "2-Step Setup & Tips", "home_top_tab_setup")
}

@Composable
fun StudioScreen(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    imeStatus: ImeStatus,
    licenseState: LicenseUiState,
    languagePacks: List<LanguagePackUiState>,
    playgroundText: String,
    clipboardClips: List<String>,
    hasFreshTextClip: Boolean = false,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenUnlockScreen: () -> Unit,
    onOpenBanglishVip: () -> Unit = onOpenUnlockScreen,
    onSelectLanguageById: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectEffect: (RgbEffect) -> Unit,
    onSelectPalette: (RgbPalette) -> Unit,
    onCycleEffect: () -> Unit,
    onPlaygroundCommit: (String) -> Unit,
    onPlaygroundBackspace: () -> Unit,
    onPlaygroundApplySuggestion: (String, Int) -> Unit = { suggestion, len ->
        repeat(len) { onPlaygroundBackspace() }
        onPlaygroundCommit("$suggestion ")
    },
    onPlaygroundDeleteWord: () -> Unit = onPlaygroundBackspace,
    onPlaygroundSpace: () -> Unit,
    onPlaygroundEnter: () -> Unit,
    onClearPlayground: () -> Unit,
    onCopyPlayground: () -> Unit,
    onRefreshClipboard: () -> Unit,
    onRemoveClipboardItem: (String) -> Unit,
    onClearClipboard: () -> Unit,
    onMarkClipboardPasted: (String) -> Unit = {},
    onKeyFeedback: (Boolean) -> Unit,
    onOpenHardwareSettings: () -> Unit,
    onOpenLanguagesTab: () -> Unit,
    onOpenEffectsCategory: (StudioEffectsCategory) -> Unit = {},
    downloadedThemeIds: Set<String> = emptySet(),
    downloadingThemeIds: Set<String> = emptySet(),
    onSelectTouchEffect: (TouchTypingEffect) -> Unit = {},
    onApplyDefaultBlack: () -> Unit = {},
    onApplyComboTheme: (ComboThemePreset) -> Unit = {},
    onDownloadComboTheme: (ComboThemePreset) -> Unit = {},
    onApplyBannerPhotoPreservingTouch: (String) -> Unit = {},
    onSaveCroppedPhotoBitmap: (android.graphics.Bitmap, Float, Boolean) -> Unit = { _, _, _ -> },
    isFeatureTourActive: Boolean = false,
    activeTourTarget: FeatureTourTarget? = null,
    featureTourCoordinates: FeatureTourCoordinates? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var selectedHomeSectionName by rememberSaveable { mutableStateOf(HomeTopSection.KEYBOARD.name) }
    val selectedHomeSection = remember(selectedHomeSectionName) {
        HomeTopSection.entries.firstOrNull { it.name == selectedHomeSectionName } ?: HomeTopSection.KEYBOARD
    }
    val homeRgbPagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    var homeBackdropEditTarget by remember { mutableStateOf<BackdropEditTarget?>(null) }
    val homePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri: Uri? ->
        if (selectedUri != null) {
            homeBackdropEditTarget = BackdropEditTarget.PhotoUri(selectedUri)
        }
    }
    var homeTimeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(selectedHomeSection) {
        if (selectedHomeSection == HomeTopSection.THEMES_RGB) {
            val startNanos = System.nanoTime()
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    homeTimeSec = (frameTimeNanos - startNanos) / 1_000_000_000f
                }
            }
        }
    }

    var showSystemImeTester by remember { mutableStateOf(false) }
    var showEasyGuideDialog by remember { mutableStateOf(false) }
    var systemTestText by remember { mutableStateOf("") }
    var activeOverlay by remember { mutableStateOf(StudioOverlayMode.NONE) }
    var activeStylishFont by remember { mutableStateOf(StylishFontStyle.NORMAL) }
    var voiceState by remember { mutableStateOf(OnlineVoiceTypingController.VoiceListeningState()) }
    var showVoiceBanner by remember { mutableStateOf(false) }
    var pendingStickerPhrase by remember { mutableStateOf("") }
    var previewSticker by remember { mutableStateOf<RgbStickerGenerator.StickerArtifact?>(null) }

    // Step-by-step hierarchical Back navigation inside Home
    BackHandler(enabled = selectedHomeSection != HomeTopSection.KEYBOARD) {
        selectedHomeSectionName = HomeTopSection.KEYBOARD.name
    }
    BackHandler(enabled = selectedHomeSection == HomeTopSection.THEMES_RGB && homeRgbPagerState.currentPage != 0) {
        coroutineScope.launch { homeRgbPagerState.animateScrollToPage(0) }
    }
    BackHandler(enabled = selectedHomeSection == HomeTopSection.KEYBOARD && activeOverlay != StudioOverlayMode.NONE) {
        activeOverlay = StudioOverlayMode.NONE
    }
    BackHandler(enabled = showEasyGuideDialog || previewSticker != null) {
        showEasyGuideDialog = false
        previewSticker = null
    }

    LaunchedEffect(isFeatureTourActive, activeTourTarget) {
        if (isFeatureTourActive) {
            activeOverlay = StudioOverlayMode.NONE
            when (activeTourTarget) {
                FeatureTourTarget.ENABLE_KEYBOARD,
                FeatureTourTarget.SELECT_KEYBOARD -> {
                    selectedHomeSectionName = HomeTopSection.SETUP.name
                    scrollState.animateScrollTo(0)
                }
                FeatureTourTarget.ADD_LANGUAGES -> {
                    selectedHomeSectionName = HomeTopSection.KEYBOARD.name
                    scrollState.animateScrollTo(0)
                }
                else -> {
                    selectedHomeSectionName = HomeTopSection.KEYBOARD.name
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }
    }

    LaunchedEffect(
        licenseState.isStylishFontsUnlocked,
        licenseState.isAiHubUnlocked,
        licenseState.isRgbStickerUnlocked
    ) {
        if (!licenseState.isStylishFontsUnlocked) {
            activeStylishFont = StylishFontStyle.NORMAL
            if (activeOverlay == StudioOverlayMode.FONTS) {
                activeOverlay = StudioOverlayMode.NONE
            }
        }
        if (!licenseState.isAiHubUnlocked && activeOverlay == StudioOverlayMode.AI_HUB) {
            activeOverlay = StudioOverlayMode.NONE
        }
        if (!licenseState.isRgbStickerUnlocked && activeOverlay == StudioOverlayMode.STICKER_SELECTOR) {
            activeOverlay = StudioOverlayMode.NONE
        }
    }

    val latestCommit by rememberUpdatedState(onPlaygroundCommit)
    val latestBackspace by rememberUpdatedState(onPlaygroundBackspace)
    val latestDeleteWord by rememberUpdatedState(onPlaygroundDeleteWord)
    val latestSpace by rememberUpdatedState(onPlaygroundSpace)
    val latestEnter by rememberUpdatedState(onPlaygroundEnter)
    val latestFeedback by rememberUpdatedState(onKeyFeedback)
    val latestToggleLanguage by rememberUpdatedState(onToggleLanguage)
    val latestActiveLangId by rememberUpdatedState(settings.activeLanguageId)
    val latestStylishFont by rememberUpdatedState(activeStylishFont)
    val haptic = LocalHapticFeedback.current
    var pendingLockedVoiceAfterPermission by remember { mutableStateOf(false) }

    val voiceController = remember(context) {
        OnlineVoiceTypingController(
            context = context,
            onStateChanged = { next ->
                voiceState = next
                if (next.isListening || !next.errorMessage.isNullOrBlank()) {
                    showVoiceBanner = true
                } else {
                    showVoiceBanner = false
                }
            },
            onPartialText = { _ -> },
            onFinalText = { spoken ->
                val cleanSpoken = if (latestActiveLangId == LanguageCatalog.BANGLISH_ID &&
                    BengaliTransliterator.containsBengaliScript(spoken)
                ) {
                    BengaliTransliterator.transliterate(spoken, capitalizeSentences = true)
                } else {
                    spoken
                }
                latestCommit("$cleanSpoken ")
                if (!voiceState.isLockedContinuous) {
                    showVoiceBanner = false
                }
            }
        )
    }

    DisposableEffect(voiceController) {
        onDispose { voiceController.destroy() }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val locale = OnlineVoiceTypingController.localeForLanguageId(latestActiveLangId)
            showVoiceBanner = true
            voiceController.startListening(locale, lockedContinuous = pendingLockedVoiceAfterPermission)
            pendingLockedVoiceAfterPermission = false
        } else {
            pendingLockedVoiceAfterPermission = false
            showVoiceBanner = true
            voiceState = OnlineVoiceTypingController.VoiceListeningState(
                isListening = false,
                locale = OnlineVoiceTypingController.localeForLanguageId(latestActiveLangId),
                errorMessage = context.getString(R.string.voice_permission_needed)
            )
        }
    }

    if (showEasyGuideDialog) {
        EasyBanglaGuideDialog(
            onDismiss = { showEasyGuideDialog = false }
        )
    }

    previewSticker?.let { artifact ->
        AlertDialog(
            onDismissRequest = { previewSticker = null },
            containerColor = GlassDialogSurface,
            title = {
                Text(
                    text = stringResource(R.string.sticker_preview_title),
                    fontFamily = OrbitronFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        bitmap = artifact.bitmap.asImageBitmap(),
                        contentDescription = artifact.phrase,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0A0614))
                    )
                    if (artifact.style.isTransparentTextOnly) {
                        Text(
                            text = "✨ শুধুমাত্র রঙিন লেখা • 100% Transparent (No Box)",
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RgbStickerGenerator.shareStickerFallback(context, artifact)
                        previewSticker = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonMagenta,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.sticker_share_btn),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { previewSticker = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sticker_close_btn),
                        color = TextSecondary
                    )
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Flagship Elevated Dark-Glassmorphic "Rakib official" + VIP Status Card
            HeroStatusCard(
                imeStatus = imeStatus,
                licenseState = licenseState,
                onOpenUnlockScreen = onOpenUnlockScreen,
                onOpenBanglishVip = onOpenBanglishVip,
                onOpenSetupSection = { selectedHomeSectionName = HomeTopSection.SETUP.name }
            )

            // Large Top Selector Tabs (Tapping an option displays ONLY that selected section below)
            HomeTopSelectorBar(
                selectedSection = selectedHomeSection,
                imeReady = imeStatus.isEnabled && imeStatus.isSelected,
                onSelectSection = { nextSection ->
                    selectedHomeSectionName = nextSection.name
                }
            )
        }

        AnimatedContent(
            targetState = selectedHomeSection,
            label = "home_top_section_content",
            modifier = Modifier.fillMaxWidth()
        ) { activeSection ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (activeSection) {
                    HomeTopSection.KEYBOARD -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val appStrings = LocalAppStrings.current
                            val uiBodyFont = if (appStrings.language == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default

                            // Quick Active Language Strip (Built-in English + Downloaded JSON Packs + Download Shortcut)
                            val enabledLanguages = languagePacks.filter { it.isEnabled }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = appStrings.quickLanguageLabel,
                                    fontFamily = uiBodyFont,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(end = 8.dp)
                                ) {
                                    items(enabledLanguages, key = { it.catalog.id }) { pack ->
                                        val isVipLocked = pack.catalog.isVipOnly && !licenseState.isBanglishUnlocked
                                        val selected = pack.isActive && !isVipLocked
                                        val pillLabel = when (pack.catalog.id) {
                                            LanguageCatalog.ENGLISH_ID -> "EN"
                                            else -> pack.catalog.nativeName
                                        }
                                        val pillFont = when (pack.catalog.id) {
                                            LanguageCatalog.ENGLISH_ID -> JetBrainsMonoFamily
                                            "bn", LanguageCatalog.BANGLISH_ID -> NotoSansBengaliFamily
                                            else -> FontFamily.Default
                                        }
                                        val pillShape = RoundedCornerShape(16.dp)
                                        val pillBorderBrush = when {
                                            selected -> FlagshipCyanMagentaBrush
                                            isVipLocked -> FlagshipGoldAmberBrush
                                            else -> FlagshipGlassRimSubtle
                                        }
                                        val pillGlowColor = when {
                                            selected -> NeonCyan
                                            isVipLocked -> RoyalGold
                                            else -> NeonViolet
                                        }
                                        Row(
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .shadow(
                                                    elevation = if (selected || isVipLocked) 10.dp else 4.dp,
                                                    shape = pillShape,
                                                    ambientColor = pillGlowColor.copy(alpha = 0.35f),
                                                    spotColor = pillGlowColor.copy(alpha = 0.45f)
                                                )
                                                .clip(pillShape)
                                                .background(
                                                    if (selected) GlassCardSelected else GlassCardSurface
                                                )
                                                .border(
                                                    width = if (selected || isVipLocked) 1.5.dp else 1.dp,
                                                    brush = pillBorderBrush,
                                                    shape = pillShape
                                                )
                                                .clickable {
                                                    if (isVipLocked) {
                                                        if (pack.catalog.id == LanguageCatalog.BANGLISH_ID) {
                                                            onOpenBanglishVip()
                                                        } else {
                                                            onOpenUnlockScreen()
                                                        }
                                                    } else {
                                                        onSelectLanguageById(pack.catalog.id)
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                                .testTag("studio_lang_${pack.catalog.id}"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = pillLabel,
                                                fontFamily = pillFont,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            if (isVipLocked) {
                                                Text(
                                                    text = "🔒 VIP",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RoyalGold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(RoyalGold.copy(alpha = 0.18f))
                                                        .border(0.9.dp, RoyalGold.copy(alpha = 0.78f), RoundedCornerShape(999.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    item(key = "add_more_languages") {
                                        Row(
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .onGloballyPositioned { coords ->
                                                    featureTourCoordinates?.register(FeatureTourTarget.ADD_LANGUAGES, coords.boundsInRoot())
                                                }
                                                .luminousGlassCard(
                                                    shape = RoundedCornerShape(16.dp),
                                                    borderBrush = FlagshipGlassRimGoldSubtle,
                                                    borderWidth = 1.dp,
                                                    glowColor = NeonCyan,
                                                    glowElevation = 6.dp
                                                )
                                                .clickable(onClick = onOpenLanguagesTab)
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                                .testTag("studio_btn_more_languages"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = appStrings.addLanguagesChip,
                                                fontFamily = uiBodyFont,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            // Live Playground OLED Output Display
                            PlaygroundReadoutCard(
                                playgroundText = playgroundText,
                                showSystemImeTester = showSystemImeTester,
                                systemTestText = systemTestText,
                                onSystemTestTextChange = { systemTestText = it },
                                onToggleSystemTester = { showSystemImeTester = !showSystemImeTester },
                                onCopyPlayground = onCopyPlayground,
                                onClearPlayground = onClearPlayground
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

        // Embedded Gboard-Layout Live Keyboard Deck with Touch Spark RGB, Custom Photo BG & Dynamic JSON Layouts
        val isFiveRowLayout = settings.activeLanguageId == "bn" ||
            settings.activeLanguageId == "hi" ||
            settings.activeLanguageId == LanguageCatalog.BANGLISH_ID
        val extraRowHeight = if (isFiveRowLayout) 26.dp else 0.dp
        val deckHeightDp = when (settings.heightPreset) {
            KeyboardHeightPreset.COMPACT -> 212.dp + extraRowHeight
            KeyboardHeightPreset.STANDARD -> 232.dp + extraRowHeight
            KeyboardHeightPreset.TALL -> 260.dp + extraRowHeight
        }

        // Free In-App Live VIP Demo Preview + Save / Apply to Keyboard Strip (shown when RGB Themes are locked)
        if (!licenseState.isRgbThemesUnlocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurfaceVariant)
                    .border(
                        width = 1.25.dp,
                        brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan)),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("studio_live_demo_apply_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val demoStrings = LocalAppStrings.current
                val demoBodyFont = if (demoStrings.language == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default
                Text(
                    text = demoStrings.demoPreviewTag,
                    fontFamily = demoBodyFont,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber)))
                        .clickable(onClick = onOpenUnlockScreen)
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("btn_studio_apply_vip_to_keyboard"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = demoStrings.btnApplyToKeyboard,
                        fontFamily = demoBodyFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF121418),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF282C34), RoundedCornerShape(18.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                // Ultra-Clean Single-Button Top Toolbar: Rakib official (Permanent Free) | 😊 | 📋 | Aa | 🎙 | ✨ AI
                if (settings.showToolbar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(BrandObsidianPill)
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(LuxuryPinkStroke, LuxuryCyanStroke)
                                    ),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("studio_kb_brand_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.brand_header_title),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = OrbitronFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.35.sp,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // 1. Emoji (😊)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (activeOverlay == StudioOverlayMode.EMOJI) Color(0xFF2A1636) else Color.Transparent
                                )
                                .border(
                                    width = if (activeOverlay == StudioOverlayMode.EMOJI) 1.dp else 0.dp,
                                    color = if (activeOverlay == StudioOverlayMode.EMOJI) NeonMagenta else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable {
                                    onKeyFeedback(true)
                                    activeOverlay = if (activeOverlay == StudioOverlayMode.EMOJI) {
                                        StudioOverlayMode.NONE
                                    } else {
                                        StudioOverlayMode.EMOJI
                                    }
                                }
                                .testTag("studio_kb_emoji"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "😊", fontSize = 18.sp)
                        }

                        // 2. Clipboard (📋)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .onGloballyPositioned { coords ->
                                    featureTourCoordinates?.registerClipboard(coords.boundsInRoot())
                                }
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (activeOverlay == StudioOverlayMode.CLIPBOARD) Color(0xFF132838) else Color.Transparent
                                )
                                .border(
                                    width = if (activeOverlay == StudioOverlayMode.CLIPBOARD) 1.dp else 0.dp,
                                    color = if (activeOverlay == StudioOverlayMode.CLIPBOARD) NeonCyan else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable {
                                    onKeyFeedback(true)
                                    onRefreshClipboard()
                                    activeOverlay = if (activeOverlay == StudioOverlayMode.CLIPBOARD) {
                                        StudioOverlayMode.NONE
                                    } else {
                                        StudioOverlayMode.CLIPBOARD
                                    }
                                }
                                .testTag("studio_kb_menu"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📋", fontSize = 17.sp)
                        }

                        // 3. Stylish Fonts (Aa — VIP-gated after trial)
                        val isVipFonts = licenseState.isStylishFontsUnlocked
                        val fontsActive = isVipFonts && (activeOverlay == StudioOverlayMode.FONTS || activeStylishFont != StylishFontStyle.NORMAL)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .onGloballyPositioned { coords ->
                                    featureTourCoordinates?.registerFonts(coords.boundsInRoot())
                                }
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (fontsActive) Color(0xFF132838) else Color.Transparent)
                                .border(
                                    width = if (fontsActive) 1.dp else 0.dp,
                                    color = if (fontsActive) NeonCyan else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable {
                                    onKeyFeedback(true)
                                    if (!isVipFonts) {
                                        activeStylishFont = StylishFontStyle.NORMAL
                                        onOpenUnlockScreen()
                                    } else {
                                        activeOverlay = if (activeOverlay == StudioOverlayMode.FONTS) {
                                            StudioOverlayMode.NONE
                                        } else {
                                            StudioOverlayMode.FONTS
                                        }
                                    }
                                }
                                .testTag("studio_kb_fonts"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isVipFonts) {
                                    stringResource(R.string.toolbar_stylish_fonts)
                                } else {
                                    stringResource(R.string.toolbar_stylish_fonts_locked)
                                },
                                fontSize = if (isVipFonts) 14.5.sp else 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    !isVipFonts -> Color(0xFFFFCF70)
                                    fontsActive -> NeonCyan
                                    else -> Color.White
                                }
                            )
                        }

                        // 4. Voice Mic icon — Tap (< 600ms) for single sentence, Hold (>= 600ms) for Hands-Free Lock
                        val micActiveColor = if (voiceState.isLockedContinuous) RoyalGoldAccent else NeonEmerald
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .onGloballyPositioned { coords ->
                                    featureTourCoordinates?.register(FeatureTourTarget.VOICE_AND_GLOBE, coords.boundsInRoot())
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (voiceState.isListening) Color(0xFF1D2434) else Color.Transparent)
                                .pointerInput(voiceController) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        val wasListening = voiceController.isCurrentlyListening()
                                        if (wasListening) {
                                            val up = waitForUpOrCancellation()
                                            if (up != null) {
                                                latestFeedback(true)
                                                voiceController.stopListening()
                                                showVoiceBanner = false
                                            }
                                            return@awaitEachGesture
                                        }
                                        val upBeforeLock = withTimeoutOrNull(600L) {
                                            waitForUpOrCancellation()
                                        }
                                        if (upBeforeLock != null) {
                                            // Quick Single Tap (< 600ms): single-sentence voice typing
                                            latestFeedback(true)
                                            if (!voiceController.hasAudioPermission()) {
                                                pendingLockedVoiceAfterPermission = false
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                val locale = OnlineVoiceTypingController.localeForLanguageId(latestActiveLangId)
                                                showVoiceBanner = true
                                                voiceController.startListening(locale, lockedContinuous = false)
                                            }
                                        } else {
                                            // Hold (>= 600ms): Hands-Free Continuous Voice Lock
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            latestFeedback(true)
                                            if (!voiceController.hasAudioPermission()) {
                                                pendingLockedVoiceAfterPermission = true
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                val locale = OnlineVoiceTypingController.localeForLanguageId(latestActiveLangId)
                                                showVoiceBanner = true
                                                voiceController.startListening(locale, lockedContinuous = true)
                                            }
                                            waitForUpOrCancellation()
                                        }
                                    }
                                }
                                .testTag("studio_kb_mic"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_kb_mic),
                                contentDescription = stringResource(R.string.cd_voice_mic),
                                tint = if (voiceState.isListening) micActiveColor else Color(0xFFD5DCEF),
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // 5. Single Unified ✨ AI Hub Button (Respects remote aiHub free/device overrides)
                        val isVip = licenseState.isAiHubUnlocked
                        val aiActive = activeOverlay == StudioOverlayMode.AI_HUB
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .height(30.dp)
                                .onGloballyPositioned { coords ->
                                    featureTourCoordinates?.register(FeatureTourTarget.AI_HUB, coords.boundsInRoot())
                                }
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        when {
                                            !isVip -> listOf(Color(0xFF2E1528), Color(0xFF2A1D12))
                                            aiActive -> listOf(NeonMagenta, Color(0xFF00B4D8))
                                            else -> listOf(Color(0xFFD81B60), NeonAmber)
                                        }
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = when {
                                        !isVip -> NeonAmber
                                        aiActive -> NeonCyan
                                        else -> Color(0xFFFF79B0)
                                    },
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable {
                                    onKeyFeedback(true)
                                    if (!isVip) {
                                        onOpenUnlockScreen()
                                    } else {
                                        activeOverlay = if (aiActive) StudioOverlayMode.NONE else StudioOverlayMode.AI_HUB
                                    }
                                }
                                .padding(horizontal = 11.dp)
                                .testTag("studio_kb_ai_hub"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isVip) {
                                    stringResource(R.string.toolbar_ai_hub)
                                } else {
                                    stringResource(R.string.toolbar_ai_hub_locked)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isVip) Color.White else Color(0xFFFFCF70),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Inline Direct Same-Language Voice Typing Bar (bn-BD / en-IN / hi-IN — no auto-translation toggle)
                AnimatedVisibility(visible = showVoiceBanner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF171128))
                            .border(
                                1.dp,
                                if (voiceState.isListening) NeonCyan.copy(alpha = 0.65f) else NeonAmber.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = voiceState.locale.badge,
                            fontFamily = if (voiceState.locale.isBengali) NotoSansBengaliFamily else JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF261B47))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = when {
                                voiceState.isListening && voiceState.isLockedContinuous -> RoyalGoldAccent
                                voiceState.isListening -> NeonEmerald
                                else -> NeonAmber
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                voiceState.isListening && voiceState.isLockedContinuous && voiceState.partialTranscript.isNotBlank() ->
                                    "🔒 ${voiceState.partialTranscript}"
                                voiceState.isListening && voiceState.isLockedContinuous ->
                                    stringResource(R.string.voice_locked_continuous_bn)
                                voiceState.isListening && voiceState.partialTranscript.isNotBlank() ->
                                    "🎙️ শোনা যাচ্ছে... • ${voiceState.partialTranscript}"
                                voiceState.isListening ->
                                    stringResource(R.string.voice_listening_bn)
                                !voiceState.errorMessage.isNullOrBlank() ->
                                    voiceState.errorMessage.orEmpty()
                                else -> stringResource(R.string.voice_tap_to_speak)
                            },
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                voiceState.isListening && voiceState.isLockedContinuous -> RoyalGoldAccent
                                voiceState.isListening -> NeonCyan
                                else -> NeonAmber
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_text),
                            tint = NeonError,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable {
                                    voiceController.stopListening()
                                    showVoiceBanner = false
                                }
                                .padding(2.dp)
                                .testTag("studio_voice_stop")
                        )
                    }
                }

                // Scrollable Smart Word Prediction, "বাংলা → বাংলিশ" Converter & Glowing "✨ RGB Sticker" Strip inside Live Playground
                if (activeOverlay == StudioOverlayMode.NONE) {
                    val prediction = remember(playgroundText, settings.activeLanguageId) {
                        SmartWordPredictor.predict(playgroundText, settings.activeLanguageId, context)
                    }
                    val isBanglishMode = settings.activeLanguageId == LanguageCatalog.BANGLISH_ID &&
                        licenseState.isBanglishUnlocked
                    val topBanglaClip = remember(clipboardClips, isBanglishMode) {
                        if (isBanglishMode) {
                            clipboardClips.firstOrNull { BengaliTransliterator.containsBengaliScript(it) }.orEmpty()
                        } else {
                            ""
                        }
                    }
                    val clipBanglishPreview = remember(topBanglaClip) {
                        if (topBanglaClip.isNotEmpty()) {
                            BengaliTransliterator.transliterate(topBanglaClip, capitalizeSentences = true)
                        } else {
                            ""
                        }
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .testTag("studio_suggestion_strip"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBanglishMode) {
                            item(key = "bangla_to_banglish_chip") {
                                val hasLiveConvert = prediction.banglaConvertPreview.isNotEmpty()
                                val hasClipConvert = !hasLiveConvert && clipBanglishPreview.isNotEmpty()
                                val chipLabel = when {
                                    hasLiveConvert -> {
                                        val shortPreview = if (prediction.banglaConvertPreview.length > 18) {
                                            prediction.banglaConvertPreview.take(18).trimEnd() + "…"
                                        } else {
                                            prediction.banglaConvertPreview
                                        }
                                        stringResource(R.string.chip_bangla_to_banglish_preview, shortPreview)
                                    }
                                    hasClipConvert -> stringResource(R.string.chip_bangla_to_banglish_paste)
                                    else -> stringResource(R.string.chip_bangla_to_banglish)
                                }
                                val isHighlighted = hasLiveConvert || hasClipConvert
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                if (isHighlighted) {
                                                    listOf(Color(0xFF0D2F26), Color(0xFF10293B))
                                                } else {
                                                    listOf(Color(0xFF132035), Color(0xFF1A1734))
                                                }
                                            )
                                        )
                                        .border(
                                            width = 1.25.dp,
                                            color = if (isHighlighted) NeonEmerald else NeonCyan,
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .clickable {
                                            onKeyFeedback(true)
                                            if (!licenseState.isBanglishUnlocked) {
                                                onOpenBanglishVip()
                                                return@clickable
                                            }
                                            when {
                                                hasLiveConvert -> {
                                                    repeat(prediction.banglaConvertSource.length) {
                                                        onPlaygroundBackspace()
                                                    }
                                                    onPlaygroundCommit(prediction.banglaConvertPreview)
                                                }
                                                hasClipConvert -> {
                                                    onPlaygroundCommit("$clipBanglishPreview ")
                                                }
                                                else -> {
                                                    if (!voiceController.hasAudioPermission()) {
                                                        pendingLockedVoiceAfterPermission = false
                                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    } else {
                                                        val locale = OnlineVoiceTypingController.localeForLanguageId(latestActiveLangId)
                                                        showVoiceBanner = true
                                                        voiceController.startListening(locale, lockedContinuous = false)
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 11.dp, vertical = 5.dp)
                                        .testTag("studio_chip_bangla_to_banglish"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chipLabel,
                                        fontFamily = NotoSansBengaliFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHighlighted) NeonEmerald else Color(0xFF7CEEFF),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        val effectiveStickerPhrase = prediction.stickerPhrase.ifEmpty {
                            if (isFeatureTourActive) "ভালোবাসা" else ""
                        }
                        if (effectiveStickerPhrase.isNotEmpty()) {
                            item(key = "rgb_sticker_chip") {
                                val isVip = licenseState.isRgbStickerUnlocked
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            featureTourCoordinates?.register(FeatureTourTarget.RGB_STICKER, coords.boundsInRoot())
                                        }
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                if (isVip) {
                                                    listOf(Color(0xFF3D1039), Color(0xFF112D3C))
                                                } else {
                                                    listOf(Color(0xFF2E1426), Color(0xFF2A1D12))
                                                }
                                            )
                                        )
                                        .border(
                                            width = 1.25.dp,
                                            brush = Brush.horizontalGradient(
                                                if (isVip) listOf(NeonMagenta, NeonAmber, NeonCyan)
                                                else listOf(NeonAmber, NeonMagenta)
                                            ),
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .clickable {
                                            onKeyFeedback(true)
                                            if (!isVip) {
                                                onOpenUnlockScreen()
                                            } else {
                                                pendingStickerPhrase = effectiveStickerPhrase
                                                activeOverlay = StudioOverlayMode.STICKER_SELECTOR
                                            }
                                        }
                                        .padding(horizontal = 11.dp, vertical = 5.dp)
                                        .testTag("studio_chip_rgb_sticker"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isVip) {
                                            stringResource(R.string.chip_rgb_sticker)
                                        } else {
                                            stringResource(R.string.chip_rgb_sticker_vip_locked)
                                        },
                                        fontFamily = NotoSansBengaliFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isVip) NeonCyan else NeonAmber,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        val topFreshClip = if (hasFreshTextClip) clipboardClips.firstOrNull().orEmpty() else ""
                        if (topFreshClip.isNotEmpty()) {
                            item(key = "fresh_clipboard_paste_chip") {
                                val shortClip = if (topFreshClip.length > 16) {
                                    topFreshClip.take(16).trimEnd() + "…"
                                } else {
                                    topFreshClip
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF102A36))
                                        .border(1.2.dp, NeonCyan, RoundedCornerShape(999.dp))
                                        .clickable {
                                            onKeyFeedback(false)
                                            val outClip = if (isBanglishMode &&
                                                BengaliTransliterator.containsBengaliScript(topFreshClip)
                                            ) {
                                                BengaliTransliterator.transliterate(topFreshClip, capitalizeSentences = true)
                                            } else {
                                                topFreshClip
                                            }
                                            onPlaygroundCommit(outClip)
                                            onMarkClipboardPasted(topFreshClip)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("studio_chip_clipboard_paste"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "📋 Paste: $shortClip",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (prediction.suggestedEmojis.isNotEmpty()) {
                            itemsIndexed(
                                items = prediction.suggestedEmojis,
                                key = { idx, emoji -> "emoji_hint_${idx}_$emoji" }
                            ) { _, emoji ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF231738))
                                        .border(1.dp, NeonMagenta.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                                        .clickable {
                                            onKeyFeedback(false)
                                            onPlaygroundCommit("$emoji ")
                                        }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                        .testTag("studio_emoji_hint_$emoji"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (settings.smartSuggestionsEnabled) {
                            items(prediction.suggestions, key = { it }) { suggestion ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF19132B))
                                        .border(1.dp, Color(0xFF2F2352), RoundedCornerShape(999.dp))
                                        .clickable {
                                            onKeyFeedback(false)
                                            onPlaygroundApplySuggestion(suggestion, prediction.currentTokenLength)
                                        }
                                        .padding(horizontal = 11.dp, vertical = 5.dp)
                                        .testTag("studio_suggestion_$suggestion"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontFamily = if (settings.activeLanguageId == "bn") {
                                            NotoSansBengaliFamily
                                        } else {
                                            FontFamily.Default
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                when (activeOverlay) {
                    StudioOverlayMode.CLIPBOARD -> {
                        StudioClipboardPanel(
                            clips = clipboardClips,
                            height = deckHeightDp,
                            onSelectClip = { clip ->
                                onKeyFeedback(false)
                                val outClip = if (settings.activeLanguageId == LanguageCatalog.BANGLISH_ID &&
                                    BengaliTransliterator.containsBengaliScript(clip)
                                ) {
                                    BengaliTransliterator.transliterate(clip, capitalizeSentences = true)
                                } else {
                                    clip
                                }
                                onPlaygroundCommit(outClip)
                                activeOverlay = StudioOverlayMode.NONE
                            },
                            onRemoveClip = onRemoveClipboardItem,
                            onClearAll = {
                                onKeyFeedback(true)
                                onClearClipboard()
                            },
                            onClose = {
                                onKeyFeedback(true)
                                activeOverlay = StudioOverlayMode.NONE
                            }
                        )
                    }

                    StudioOverlayMode.FONTS -> {
                        StudioStylishFontPanel(
                            activeStyle = activeStylishFont,
                            height = deckHeightDp,
                            onSelectStyle = { style ->
                                onKeyFeedback(false)
                                activeStylishFont = style
                                activeOverlay = StudioOverlayMode.NONE
                            },
                            onClose = {
                                onKeyFeedback(true)
                                activeOverlay = StudioOverlayMode.NONE
                            }
                        )
                    }

                    StudioOverlayMode.AI_HUB -> {
                        StudioAiHubPanel(
                            height = deckHeightDp,
                            onInsertText = { text ->
                                onKeyFeedback(false)
                                onPlaygroundCommit("$text ")
                            },
                            onClose = {
                                onKeyFeedback(true)
                                activeOverlay = StudioOverlayMode.NONE
                            }
                        )
                    }

                    StudioOverlayMode.EMOJI -> {
                        StudioEmojiPanel(
                            height = deckHeightDp,
                            isBengaliActive = settings.activeLanguageId == "bn" ||
                                settings.activeLanguageId == LanguageCatalog.BANGLISH_ID,
                            onSelectEmoji = { emoji ->
                                onKeyFeedback(false)
                                onPlaygroundCommit(emoji)
                            },
                            onBackspace = {
                                onKeyFeedback(true)
                                onPlaygroundBackspace()
                            },
                            onClose = {
                                onKeyFeedback(true)
                                activeOverlay = StudioOverlayMode.NONE
                            }
                        )
                    }

                    StudioOverlayMode.STICKER_SELECTOR -> {
                        StudioStickerSelectorPanel(
                            phrase = pendingStickerPhrase,
                            height = deckHeightDp,
                            onSelectStyle = { style ->
                                onKeyFeedback(false)
                                val artifact = RgbStickerGenerator.generateSticker(
                                    context = context,
                                    rawPhrase = pendingStickerPhrase,
                                    style = style
                                )
                                activeOverlay = StudioOverlayMode.NONE
                                if (artifact != null) {
                                    previewSticker = artifact
                                }
                            },
                            onClose = {
                                onKeyFeedback(true)
                                activeOverlay = StudioOverlayMode.NONE
                            }
                        )
                    }

                    StudioOverlayMode.NONE -> {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(deckHeightDp)
                                .onGloballyPositioned { coords ->
                                    featureTourCoordinates?.register(FeatureTourTarget.TOUCH_SPARK_DECK, coords.boundsInRoot())
                                }
                                .testTag("native_rgb_keyboard_preview"),
                            factory = { ctx ->
                                NativeRgbKeyboardView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    allowTopOverflow = false
                                    onKeyAction = { key ->
                                        when (key) {
                                            "DEL" -> latestBackspace()
                                            "ENTER" -> latestEnter()
                                            " " -> latestSpace()
                                            else -> latestCommit(StylishFontEngine.transform(key, latestStylishFont))
                                        }
                                    }
                                    this.onDeleteWordAction = { latestDeleteWord() }
                                    this.onKeyFeedback = { isMod -> latestFeedback(isMod) }
                                    this.onLanguageToggleRequested = { latestToggleLanguage() }
                                    this.onEmojiOpenRequested = { activeOverlay = StudioOverlayMode.EMOJI }
                                    // Always enable full VIP visuals in the in-app Live Playground so users can test every effect & photo freely!
                                    setVipVisualsEnabled(true)
                                    updateSettings(settings)
                                    updatePhotoTheme(customPhotoTheme)
                                }
                            },
                            update = { view ->
                                view.setVipVisualsEnabled(true)
                                view.updateSettings(settings)
                                view.updatePhotoTheme(customPhotoTheme)
                            }
                        )
                    }
                }
            }
        }
                    }

                    HomeTopSection.THEMES_RGB -> {
                        HomeUnifiedRgbThemesTouchSection(
                            settings = settings,
                            customPhotoTheme = customPhotoTheme,
                            licenseState = licenseState,
                            downloadedThemeIds = downloadedThemeIds,
                            downloadingThemeIds = downloadingThemeIds,
                            timeProvider = { homeTimeSec },
                            pagerState = homeRgbPagerState,
                            onSelectPage = { pageIdx ->
                                coroutineScope.launch { homeRgbPagerState.animateScrollToPage(pageIdx) }
                            },
                            onApplyDefaultBlack = onApplyDefaultBlack,
                            onSelectTouchEffect = { fx ->
                                val isLocked = !licenseState.isThemeItemUnlocked(fx.id, !fx.isVipEffect)
                                if (isLocked) onOpenUnlockScreen() else onSelectTouchEffect(fx)
                            },
                            onSelectEffect = { fx ->
                                val isLocked = !licenseState.isThemeItemUnlocked(fx.id, !fx.isVipEffect)
                                if (isLocked) onOpenUnlockScreen() else onSelectEffect(fx)
                            },
                            onSelectPalette = onSelectPalette,
                            onQuickApplyCombo = { combo ->
                                val isLocked = !licenseState.isThemeItemUnlocked(combo.id, combo.isCloudFree)
                                if (isLocked) {
                                    onOpenUnlockScreen()
                                } else {
                                    onApplyComboTheme(combo)
                                }
                            },
                            onPickGalleryPhoto = {
                                homePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onSelectHdPhoto = { preset ->
                                val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
                                if (isLocked) {
                                    onOpenUnlockScreen()
                                } else {
                                    onApplyBannerPhotoPreservingTouch(preset.id)
                                }
                            },
                            onOpenFullStudio = { cat -> onOpenEffectsCategory(cat) }
                        )
                    }

                    HomeTopSection.STYLES -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PromoBannerCarousel(
                                banners = licenseState.adminConfig.banners,
                                onBannerClick = {
                                    if (!licenseState.isPaidOrAdminVip) {
                                        onOpenUnlockScreen()
                                    }
                                }
                            )

                            val homeBannersCount = com.example.data.BannerPresetCatalog.staticBannerPresets.size +
                                listOf(
                                    licenseState.adminConfig.banners.resolvedBanner1Image,
                                    licenseState.adminConfig.banners.resolvedBanner2Image
                                ).count { it.isNotBlank() }

                            LuxuryStudioCategoryGrid(
                                selectedCategory = null,
                                photosBadge = "Gallery + ${com.example.data.BannerPresetCatalog.staticBannerPresets.size} HD",
                                bannersBadge = "$homeBannersCount Featured",
                                animationsBadge = "${RgbEffect.ambientPhysicsEffects.size} Live RGB",
                                touchResponsesBadge = "${TouchTypingEffect.byCategory(TouchEffectCategory.ALL).size} AI FX",
                                themesBadge = "${ComboThemeCatalog.byCategory(ComboCategory.ALL, licenseState.adminConfig.cloudThemes).size} Packs",
                                onSelectCategory = onOpenEffectsCategory
                            )
                        }
                    }

                    HomeTopSection.SETUP -> {
                        HomeSetupAndGuideSection(
                            imeStatus = imeStatus,
                            onOpenImeSettings = onOpenImeSettings,
                            onShowImePicker = onShowImePicker,
                            onOpenEasyGuide = { showEasyGuideDialog = true },
                            onOpenLanguagesTab = onOpenLanguagesTab,
                            featureTourCoordinates = featureTourCoordinates
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
    }

    homeBackdropEditTarget?.let { editTarget ->
        BackdropBrightnessEditorDialog(
            target = editTarget,
            settings = settings,
            customPhotoTheme = customPhotoTheme,
            timeSec = homeTimeSec,
            onDismiss = { homeBackdropEditTarget = null },
            onApplyCroppedPhoto = { bmp, dimAlpha, showBorders ->
                onSaveCroppedPhotoBitmap(bmp, dimAlpha, showBorders)
                homeBackdropEditTarget = null
            },
            onApplyBannerPreset = { presetId, _, _ ->
                onApplyBannerPhotoPreservingTouch(presetId)
                homeBackdropEditTarget = null
            },
            onApplyRemoteBanner = { _, _, _ ->
                homeBackdropEditTarget = null
            }
        )
    }
}

@Composable
private fun StudioStickerSelectorPanel(
    phrase: String,
    height: androidx.compose.ui.unit.Dp,
    onSelectStyle: (RgbStickerStyle) -> Unit,
    onClose: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "sticker_style_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sticker_pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C0818))
            .padding(10.dp)
            .testTag("studio_sticker_selector_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "✨ \"$phrase\"",
                fontFamily = NotoSansBengaliFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "⌨",
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1D2030))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("studio_sticker_selector_close")
            )
        }

        Text(
            text = stringResource(R.string.sticker_selector_tap_hint),
            fontFamily = NotoSansBengaliFamily,
            fontSize = 10.5.sp,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // #1 Default Featured Option: Box-Free Transparent RGB Text Sticker (শুধুমাত্র রঙিন লেখা)
            item(key = "transparent_text_only_default") {
                val style = RgbStickerStyle.TEXT_ONLY_TRANSPARENT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF18122B), Color(0xFF0D1F2C))
                            )
                        )
                        .border(
                            width = 1.6.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    NeonCyan.copy(alpha = pulseAlpha),
                                    NeonMagenta.copy(alpha = pulseAlpha),
                                    NeonAmber.copy(alpha = pulseAlpha)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectStyle(style) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("studio_sticker_style_${style.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${style.badgeIcon} ${style.displayName}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = style.subtitle,
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 10.5.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta)))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEFAULT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0A0D18)
                        )
                    }
                }
            }

            // Optional 6 Framed Neon Box Styles
            val framedStyles = RgbStickerStyle.entries.filter { !it.isTransparentTextOnly }
            items(framedStyles.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    pair.forEachIndexed { idx, style ->
                        val primaryColor = Color(style.accentPrimary)
                        val secondaryColor = Color(style.accentSecondary)
                        val borderAlpha = if (idx % 2 == 0) pulseAlpha else (1.72f - pulseAlpha).coerceIn(0.72f, 1f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(style.cardBgColors.first()),
                                            Color(style.cardBgColors.last())
                                        )
                                    )
                                )
                                .border(
                                    width = 1.4.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            primaryColor.copy(alpha = borderAlpha),
                                            secondaryColor.copy(alpha = borderAlpha)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectStyle(style) }
                                .padding(horizontal = 10.dp, vertical = 9.dp)
                                .testTag("studio_sticker_style_${style.id}"),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${style.badgeIcon} ${style.displayName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = style.subtitle,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioEmojiPanel(
    height: androidx.compose.ui.unit.Dp,
    isBengaliActive: Boolean,
    onSelectEmoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit
) {
    var selectedCategoryIdx by remember { mutableIntStateOf(0) }
    val activeCategory = EmojiCatalog.categories[selectedCategoryIdx.coerceIn(0, EmojiCatalog.categories.lastIndex)]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0F17))
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("studio_emoji_panel"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 9 Category Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(EmojiCatalog.categories, key = { _, cat -> cat.id }) { idx, category ->
                val selected = idx == selectedCategoryIdx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF24314A) else Color.Transparent)
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) NeonCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategoryIdx = idx }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("studio_emoji_tab_${category.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Scrollable Emoji Grid (5 cols for wide Unique AI Emojis combos, 8 cols for standard emojis)
        val isAiUniqueTab = activeCategory.id == "ai_unique"
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isAiUniqueTab) 5 else 8),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(activeCategory.emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectEmoji(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = if (emoji.length > 4) 15.sp else if (emoji.length > 2) 17.sp else 22.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Bottom Bar: ABC / কখগ + Category Name + Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2434))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("studio_emoji_close"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBengaliActive) "কখগ" else "ABC",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Text(
                text = "${activeCategory.title} (${activeCategory.emojis.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF252A3B))
                    .clickable(onClick = onBackspace)
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .testTag("studio_emoji_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun GboardToolbarIcon(
    iconRes: Int,
    contentDescription: String,
    active: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF60A5FA),
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0xFF1D2434) else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = if (active) activeColor else Color(0xFFC9D1DE),
            modifier = Modifier.size(21.dp)
        )
    }
}


@Composable
private fun StudioClipboardPanel(
    clips: List<String>,
    height: androidx.compose.ui.unit.Dp,
    onSelectClip: (String) -> Unit,
    onRemoveClip: (String) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0F17))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.clipboard_title),
                style = MaterialTheme.typography.titleSmall,
                color = NeonCyan
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (clips.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.clipboard_clear_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonError,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1D2030))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("studio_clipboard_clear")
                    )
                }
                Text(
                    text = "⌨",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1D2030))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("studio_clipboard_close")
                )
            }
        }

        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.clipboard_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(clips, key = { it }) { snippet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF161926))
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                            .clickable { onSelectClip(snippet) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemoveClip(snippet) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.clipboard_clear_all),
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatusCard(
    imeStatus: ImeStatus,
    licenseState: LicenseUiState,
    onOpenUnlockScreen: () -> Unit,
    onOpenBanglishVip: () -> Unit,
    onOpenSetupSection: () -> Unit
) {
    val glassShape = RoundedCornerShape(24.dp)

    val isDeviceBlocked = licenseState.isDeviceBlocked
    val isTrueLifetimeDisplay = licenseState.isTrueLifetimeDisplay
    val isAdminGrantedVip = !isDeviceBlocked && !isTrueLifetimeDisplay &&
        (licenseState.isAdminGrantedVip || licenseState.deviceOverrides.hasFullAdminFreeAccess)
    val isTimedVip = !isDeviceBlocked && !isTrueLifetimeDisplay && !isAdminGrantedVip &&
        (licenseState.isMonthlyActive || licenseState.isWeeklyActive)
    val isPaidOrAdminOrGlobalVip = isTrueLifetimeDisplay || isAdminGrantedVip || isTimedVip
    val isActiveFreeTrial = !isDeviceBlocked && !isPaidOrAdminOrGlobalVip && !licenseState.isTrialExpired
    val isExpiredFreeMode = isDeviceBlocked || (!isPaidOrAdminOrGlobalVip && licenseState.isTrialExpired)

    val pillBorderBrush = when {
        isDeviceBlocked -> FlagshipCyanMagentaBrush
        isPaidOrAdminOrGlobalVip -> FlagshipEmeraldCyanBrush
        isExpiredFreeMode -> FlagshipCyanMagentaBrush
        else -> FlagshipGoldAmberBrush
    }
    val pillBg = when {
        isDeviceBlocked -> Color(0xCC2A1026)
        isPaidOrAdminOrGlobalVip -> Color(0xCC0A2E22)
        isExpiredFreeMode -> Color(0xCC2A1026)
        else -> Color(0xCC221909)
    }
    val strings = LocalAppStrings.current
    val heroBodyFont = if (strings.language == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default
    val pillText = licenseState.formattedVipPillText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = glassShape,
                borderBrush = FlagshipGoldCyanBrush,
                borderWidth = 1.6.dp,
                glowColor = RoyalGold,
                glowElevation = 18.dp,
                surfaceColor = GlassCardSurface
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Glowing Crown Badge + "Rakib official" + Live Trial/VIP Countdown Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_brand_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Glowing Golden Crown Emblem Badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = RoyalGold.copy(alpha = 0.72f),
                                spotColor = RoyalGold.copy(alpha = 0.85f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF3A2A0A),
                                        Color(0xFF171324),
                                        Color(0xFF090D1F)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = FlagshipGoldAmberBrush,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👑",
                            fontSize = 20.sp
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.studio_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = OrbitronFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.55.sp,
                                    color = Color.White,
                                    shadow = Shadow(
                                        color = NeonCyan.copy(alpha = 0.6f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 8f
                                    )
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            BlueVerifiedBadge(
                                size = 17.dp,
                                contentDescription = "Verified Official Badge"
                            )
                        }
                        Text(
                            text = when {
                                isDeviceBlocked -> "🔒 ACCESS RESTRICTED BY ADMIN"
                                isTrueLifetimeDisplay -> strings.vipBannerUnlocked
                                isAdminGrantedVip -> "✓ ADMIN VIP UNLOCKED · ALL EFFECTS ACTIVE"
                                isTimedVip -> "✓ ${licenseState.formattedCompactEnglish} VIP ACTIVE"
                                isActiveFreeTrial -> "👑 VIP TRIAL • ${licenseState.formattedCompactEnglish}"
                                else -> "NORMAL BLACK • FREE STANDARD MODE"
                            },
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = when {
                                isDeviceBlocked -> NeonMagenta
                                isPaidOrAdminOrGlobalVip -> NeonEmerald
                                isActiveFreeTrial -> RoyalGold
                                else -> NeonCyan.copy(alpha = 0.88f)
                            },
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Live Trial / VIP Countdown Pill
                Row(
                    modifier = Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(999.dp),
                            ambientColor = RoyalGold.copy(alpha = 0.45f),
                            spotColor = NeonCyan.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(pillBg)
                        .border(1.3.dp, pillBorderBrush, RoundedCornerShape(999.dp))
                        .clickable(
                            enabled = !licenseState.isAllUnlocked && !isDeviceBlocked,
                            onClick = {
                                if (isPaidOrAdminOrGlobalVip && !licenseState.isBanglishUnlocked) {
                                    onOpenBanglishVip()
                                } else {
                                    onOpenUnlockScreen()
                                }
                            }
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                        .testTag(
                            when {
                                isTrueLifetimeDisplay || isAdminGrantedVip -> "badge_lifetime_unlocked"
                                isExpiredFreeMode -> "badge_free_standard_mode"
                                else -> "badge_free_trial_timer"
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (licenseState.isVipActive) {
                        BlueVerifiedBadge(
                            size = 15.dp,
                            contentDescription = "VIP Verified"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = pillText,
                        fontFamily = FontFamily.Default,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.25.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Slim non-intrusive status strip when IME isn't active yet
            if (!(imeStatus.isEnabled && imeStatus.isSelected)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x521C142B))
                        .border(1.dp, FlagshipGoldAmberBrush, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSetupSection)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("home_quick_setup_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚡ Finish 2-step keyboard setup to type anywhere",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Setup",
                        tint = RoyalGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Prominent Full-Width VIP Unlock / Plans CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = if (isPaidOrAdminOrGlobalVip) NeonEmerald.copy(alpha = 0.42f) else RoyalGold.copy(alpha = 0.55f),
                        spotColor = if (isPaidOrAdminOrGlobalVip) NeonCyan.copy(alpha = 0.48f) else NeonCyan.copy(alpha = 0.42f)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isPaidOrAdminOrGlobalVip) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0A3327),
                                    Color(0xFF0D4438),
                                    Color(0xFF092D3A)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFC107),
                                    Color(0xFFFF9800),
                                    Color(0xFF00E5FF)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        brush = if (isPaidOrAdminOrGlobalVip) FlagshipEmeraldCyanBrush else FlagshipGoldCyanBrush,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(
                        enabled = !licenseState.isAllUnlocked && !isDeviceBlocked,
                        onClick = {
                            if (isPaidOrAdminOrGlobalVip && !licenseState.isBanglishUnlocked) {
                                onOpenBanglishVip()
                            } else {
                                onOpenUnlockScreen()
                            }
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("hero_lifetime_vip_banner"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isDeviceBlocked -> "🔒 অ্যাক্সেস সীমিত (Admin Blocked)"
                        isTrueLifetimeDisplay -> "👑 ${strings.vipBannerUnlocked}"
                        isAdminGrantedVip -> "👑 ADMIN VIP UNLOCKED · ALL EFFECTS"
                        isTimedVip -> "👑 VIP ACTIVE • ${licenseState.formattedCompactEnglish}"
                        isActiveFreeTrial -> "👑 VIP ট্রায়াল (${licenseState.formattedCompactEnglish}) • প্ল্যান দেখুন"
                        else -> "👑 VIP আনলক / প্ল্যান দেখুন"
                    },
                    fontFamily = if (isPaidOrAdminOrGlobalVip) heroBodyFont else NotoSansBengaliFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.35.sp,
                    color = if (isPaidOrAdminOrGlobalVip) Color.White else Color(0xFF060A18),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeTopSelectorBar(
    selectedSection: HomeTopSection,
    imeReady: Boolean,
    onSelectSection: (HomeTopSection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeTopSelectorTile(
                section = HomeTopSection.KEYBOARD,
                icon = Icons.Filled.Keyboard,
                accentColor = NeonCyan,
                selected = selectedSection == HomeTopSection.KEYBOARD,
                onClick = { onSelectSection(HomeTopSection.KEYBOARD) },
                modifier = Modifier.weight(1f)
            )
            HomeTopSelectorTile(
                section = HomeTopSection.THEMES_RGB,
                icon = Icons.Filled.AutoAwesome,
                accentColor = NeonMagenta,
                selected = selectedSection == HomeTopSection.THEMES_RGB,
                onClick = { onSelectSection(HomeTopSection.THEMES_RGB) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeTopSelectorTile(
                section = HomeTopSection.STYLES,
                icon = Icons.Filled.Collections,
                accentColor = RoyalGold,
                selected = selectedSection == HomeTopSection.STYLES,
                onClick = { onSelectSection(HomeTopSection.STYLES) },
                modifier = Modifier.weight(1f)
            )
            HomeTopSelectorTile(
                section = HomeTopSection.SETUP,
                icon = if (imeReady) Icons.Filled.CheckCircle else Icons.Filled.Settings,
                accentColor = if (imeReady) NeonEmerald else NeonAmber,
                selected = selectedSection == HomeTopSection.SETUP,
                onClick = { onSelectSection(HomeTopSection.SETUP) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeTopSelectorTile(
    section: HomeTopSection,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 60.dp)
            .luminousGlassCard(
                shape = RoundedCornerShape(18.dp),
                borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                borderWidth = if (selected) 1.7.dp else 1.dp,
                glowColor = accentColor,
                glowElevation = if (selected) 14.dp else 6.dp,
                surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(section.testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accentColor.copy(alpha = if (selected) 0.24f else 0.14f))
                .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = section.title,
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = section.title,
                fontFamily = SpaceGroteskFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = section.subtitle,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeUnifiedRgbThemesTouchSection(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    licenseState: LicenseUiState,
    downloadedThemeIds: Set<String>,
    downloadingThemeIds: Set<String>,
    timeProvider: () -> Float,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onSelectPage: (Int) -> Unit,
    onApplyDefaultBlack: () -> Unit,
    onSelectTouchEffect: (TouchTypingEffect) -> Unit,
    onSelectEffect: (RgbEffect) -> Unit,
    onSelectPalette: (RgbPalette) -> Unit,
    onQuickApplyCombo: (ComboThemePreset) -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onSelectHdPhoto: (BannerThemePreset) -> Unit,
    onOpenFullStudio: (StudioEffectsCategory) -> Unit
) {
    val touchEffects = remember { TouchTypingEffect.byCategory(TouchEffectCategory.ALL).take(16) }
    val featuredCombos = remember(licenseState.adminConfig.cloudThemes) {
        ComboThemeCatalog.byCategory(ComboCategory.ALL, licenseState.adminConfig.cloudThemes).take(12)
    }
    val featuredPhotos = remember { BannerPresetCatalog.staticBannerPresets.take(12) }
    val isClassicBlack = !customPhotoTheme.hasPhoto && settings.effect == RgbEffect.NORMAL_DEFAULT

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Look Summary + Classic Black Reset + Full Studio Jump
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .luminousGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    borderBrush = FlagshipGlassRimSubtle,
                    borderWidth = 1.dp,
                    glowColor = NeonCyan,
                    glowElevation = 8.dp
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isClassicBlack) "Classic Black • ${settings.touchEffect.shortLabel}" else "${settings.effect.shortLabel} • ${settings.touchEffect.shortLabel}",
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Swipe or tap tabs below to customize live",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isClassicBlack) GlassCardSelected else GlassCardInner)
                        .border(1.dp, if (isClassicBlack) NeonCyan else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onApplyDefaultBlack)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("home_btn_classic_black"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Classic Black",
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan)
                        .clickable {
                            val targetCat = when (pagerState.currentPage) {
                                1 -> StudioEffectsCategory.THEMES
                                2 -> StudioEffectsCategory.ANIMATIONS
                                3 -> StudioEffectsCategory.PHOTOS
                                else -> StudioEffectsCategory.TOUCH_RESPONSES
                            }
                            onOpenFullStudio(targetCat)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("home_btn_open_full_studio"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All →",
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF061020)
                    )
                }
            }
        }

        // Unified 4-Way Bar: Touch FX | Themes | RGB Design | Photos
        val tabTitles = listOf("Touch FX", "Themes", "RGB Design", "Photos")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassCardInner)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabTitles.forEachIndexed { idx, label ->
                val selected = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) GlassCardSelected else Color.Transparent)
                        .border(
                            width = if (selected) 1.2.dp else 0.dp,
                            brush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectPage(idx) }
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .testTag("home_unified_tab_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) NeonCyan else Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Swipeable HorizontalPager for Touch FX / Themes / RGB Design
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) { page ->
            val shouldRender = page == pagerState.currentPage || pagerState.isScrollInProgress
            if (!shouldRender) {
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp))
            } else {
                when (page) {
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            touchEffects.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { fx ->
                                        val selected = settings.touchEffect == fx
                                        val isLocked = !licenseState.isThemeItemUnlocked(fx.id, !fx.isVipEffect)
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .luminousGlassCard(
                                                    shape = RoundedCornerShape(16.dp),
                                                    borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                                    borderWidth = if (selected) 1.6.dp else 1.dp,
                                                    glowColor = Color(fx.primaryHex),
                                                    glowElevation = if (selected) 12.dp else 6.dp,
                                                    surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
                                                )
                                                .clickable { onSelectTouchEffect(fx) }
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            MiniKeyboardEffectPreview(
                                                touchEffect = fx,
                                                palette = settings.palette,
                                                glowIntensity = settings.glowIntensity,
                                                timeProvider = timeProvider,
                                                showKeyBorders = settings.showKeyBorders,
                                                bannerPresetId = BannerPresetCatalog.defaultBannerForTouchEffect(fx),
                                                dimAlpha = 0.44f,
                                                showTopStrip = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = if (isLocked) "🔒 ${fx.shortLabel}" else fx.shortLabel,
                                                    fontFamily = SpaceGroteskFamily,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (selected) "Active ✓" else fx.badge,
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontSize = 9.5.sp,
                                                    color = if (selected) NeonEmerald else NeonCyan,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            featuredCombos.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { combo ->
                                        val isActive = (customPhotoTheme.downloadedThemeId == combo.id ||
                                            (!combo.bannerPresetId.isNullOrBlank() && customPhotoTheme.presetId == combo.bannerPresetId)) &&
                                            settings.effect == combo.effect
                                        val isLocked = !licenseState.isThemeItemUnlocked(combo.id, combo.isCloudFree)
                                        val isDownloading = combo.id in downloadingThemeIds
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .luminousGlassCard(
                                                    shape = RoundedCornerShape(16.dp),
                                                    borderBrush = if (isActive) FlagshipGoldCyanBrush else FlagshipGlassRimSubtle,
                                                    borderWidth = if (isActive) 1.6.dp else 1.dp,
                                                    glowColor = RoyalGold,
                                                    glowElevation = if (isActive) 12.dp else 6.dp,
                                                    surfaceColor = if (isActive) GlassCardSelected else GlassCardSurface
                                                )
                                                .clickable { onQuickApplyCombo(combo) }
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
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
                                                showTopStrip = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = if (isLocked) "🔒 ${combo.title}" else combo.title,
                                                    fontFamily = SpaceGroteskFamily,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = when {
                                                        isDownloading -> "Applying…"
                                                        isActive -> "Active ✓"
                                                        combo.isCloudFree -> "FREE • Tap"
                                                        else -> "Tap to Apply"
                                                    },
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontSize = 9.5.sp,
                                                    color = if (isActive) NeonEmerald else NeonCyan,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(RgbPalette.entries) { palette ->
                                    val selected = settings.palette == palette
                                    Row(
                                        modifier = Modifier
                                            .heightIn(min = 44.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (selected) GlassCardSelected else GlassCardInner)
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                brush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                                shape = RoundedCornerShape(999.dp)
                                            )
                                            .clickable { onSelectPalette(palette) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(palette.colors))
                                        )
                                        Text(
                                            text = palette.displayName,
                                            fontFamily = SpaceGroteskFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            RgbEffect.entries.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { effect ->
                                        val selected = settings.effect == effect
                                        val isLocked = !licenseState.isRgbThemesUnlocked && effect.isVipEffect
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .luminousGlassCard(
                                                    shape = RoundedCornerShape(16.dp),
                                                    borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                                    borderWidth = if (selected) 1.6.dp else 1.dp,
                                                    glowColor = NeonCyan,
                                                    glowElevation = if (selected) 12.dp else 6.dp,
                                                    surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
                                                )
                                                .clickable { onSelectEffect(effect) }
                                                .padding(10.dp)
                                                .testTag("quick_effect_${effect.name.lowercase()}"),
                                            verticalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            MiniKeyboardEffectPreview(
                                                effect = effect,
                                                palette = settings.palette,
                                                keycapStyle = settings.keycapStyle,
                                                glowIntensity = settings.glowIntensity,
                                                animationSpeed = settings.animationSpeed,
                                                timeProvider = timeProvider,
                                                showKeyBorders = settings.showKeyBorders,
                                                forceDefaultBlack = effect == RgbEffect.NORMAL_DEFAULT,
                                                showTopStrip = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = if (isLocked) "🔒 ${effect.shortLabel}" else effect.shortLabel,
                                                    fontFamily = SpaceGroteskFamily,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (selected) "Active ✓" else "Ambient RGB",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontSize = 9.5.sp,
                                                    color = if (selected) NeonEmerald else NeonCyan,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .luminousGlassCard(
                                            shape = RoundedCornerShape(14.dp),
                                            borderBrush = if (customPhotoTheme.hasCustomGalleryPhoto) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                            borderWidth = if (customPhotoTheme.hasCustomGalleryPhoto) 1.5.dp else 1.dp,
                                            glowColor = NeonCyan,
                                            glowElevation = 8.dp,
                                            surfaceColor = if (customPhotoTheme.hasCustomGalleryPhoto) GlassCardSelected else GlassCardSurface
                                        )
                                        .clickable(onClick = onPickGalleryPhoto)
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("home_btn_pick_gallery_photo"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Pick Gallery Photo",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (customPhotoTheme.hasCustomGalleryPhoto) "Change Photo" else "Pick Gallery Photo",
                                            fontFamily = SpaceGroteskFamily,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "FREE • Works with Touch FX",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 9.5.sp,
                                            color = NeonEmerald,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            featuredPhotos.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { preset ->
                                        val selected = customPhotoTheme.presetId == preset.id
                                        val isLocked = !licenseState.isThemeItemUnlocked(preset.id, isPermanentlyFree = false)
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .luminousGlassCard(
                                                    shape = RoundedCornerShape(16.dp),
                                                    borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                                    borderWidth = if (selected) 1.6.dp else 1.dp,
                                                    glowColor = NeonCyan,
                                                    glowElevation = if (selected) 12.dp else 6.dp,
                                                    surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
                                                )
                                                .clickable { onSelectHdPhoto(preset) }
                                                .padding(10.dp)
                                                .testTag("home_photo_${preset.id}"),
                                            verticalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            MiniKeyboardEffectPreview(
                                                bannerPresetId = preset.id,
                                                gradientColors = preset.previewColors,
                                                showKeyBorders = settings.showKeyBorders,
                                                showTopStrip = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = if (isLocked) "🔒 ${preset.title}" else preset.title,
                                                    fontFamily = SpaceGroteskFamily,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (selected) "Active ✓" else "HD Photo • Touch Ready",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontSize = 9.5.sp,
                                                    color = if (selected) NeonEmerald else NeonCyan,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSetupAndGuideSection(
    imeStatus: ImeStatus,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenEasyGuide: () -> Unit,
    onOpenLanguagesTab: () -> Unit,
    featureTourCoordinates: FeatureTourCoordinates? = null
) {
    val strings = LocalAppStrings.current
    val bodyFont = if (strings.language == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MinimalistStepCard(
            title = strings.setupStep1Title,
            subtitle = if (imeStatus.isEnabled) strings.setupStep1Done else strings.setupStep1Subtitle,
            isComplete = imeStatus.isEnabled,
            completeBadgeText = strings.badgeEnabledShort,
            completeColor = NeonEmerald,
            completeContainer = Color(0x520B3829),
            idleAccent = NeonCyan,
            icon = Icons.Filled.Settings,
            onClick = onOpenImeSettings,
            testTag = "btn_enable_ime",
            modifier = Modifier.onGloballyPositioned { coords ->
                featureTourCoordinates?.register(FeatureTourTarget.ENABLE_KEYBOARD, coords.boundsInRoot())
            }
        )

        MinimalistStepCard(
            title = strings.setupStep2Title,
            subtitle = if (imeStatus.isSelected) strings.setupStep2Done else strings.setupStep2Subtitle,
            isComplete = imeStatus.isSelected,
            completeBadgeText = strings.badgeActiveShort,
            completeColor = NeonEmerald,
            completeContainer = Color(0x520B3829),
            idleAccent = RoyalGold,
            icon = Icons.Filled.SwapHoriz,
            onClick = onShowImePicker,
            testTag = "btn_select_ime",
            modifier = Modifier.onGloballyPositioned { coords ->
                featureTourCoordinates?.register(FeatureTourTarget.SELECT_KEYBOARD, coords.boundsInRoot())
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    borderBrush = FlagshipCyanMagentaBrush,
                    borderWidth = 1.2.dp,
                    glowColor = NeonCyan,
                    glowElevation = 8.dp
                )
                .clickable(onClick = onOpenEasyGuide)
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .testTag("btn_bangla_easy_guide"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.HelpOutline,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.btnEasyGuide,
                fontFamily = bodyFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    borderBrush = FlagshipGoldAmberBrush,
                    borderWidth = 1.1.dp,
                    glowColor = RoyalGold,
                    glowElevation = 8.dp
                )
                .clickable(onClick = onOpenLanguagesTab)
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .testTag("btn_setup_manage_languages"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Palette,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.addLanguagesChip,
                fontFamily = bodyFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun MinimalistStepCard(
    title: String,
    subtitle: String,
    isComplete: Boolean,
    completeBadgeText: String,
    completeColor: Color,
    completeContainer: Color,
    idleAccent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isComplete) completeContainer else GlassCardSurface
    val cardShape = RoundedCornerShape(16.dp)
    val borderBrush = when {
        isComplete -> FlagshipEmeraldCyanBrush
        idleAccent == RoyalGold -> FlagshipGoldAmberBrush
        else -> FlagshipCyanMagentaBrush
    }
    val glowTint = if (isComplete) completeColor else idleAccent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .luminousGlassCard(
                shape = cardShape,
                borderBrush = borderBrush,
                borderWidth = 1.25.dp,
                glowColor = glowTint,
                glowElevation = 10.dp,
                surfaceColor = bgColor
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            glowTint.copy(alpha = 0.28f),
                            Color(0xFF0E142B).copy(alpha = 0.90f)
                        )
                    )
                )
                .border(1.2.dp, borderBrush, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                tint = if (isComplete) completeColor else Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontFamily = NotoSansBengaliFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontFamily = NotoSansBengaliFamily,
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 2
            )
        }

        if (isComplete) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(completeColor.copy(alpha = 0.24f))
                    .border(1.1.dp, FlagshipEmeraldCyanBrush, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = completeBadgeText,
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun EasyBanglaGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassDialogSurface,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = stringResource(R.string.bn_guide_dialog_title),
                fontFamily = NotoSansBengaliFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GuideTipCard(
                    title = stringResource(R.string.bn_guide_tip_globe_title),
                    desc = stringResource(R.string.bn_guide_tip_globe_desc)
                )
                GuideTipCard(
                    title = stringResource(R.string.bn_guide_tip_mic_title),
                    desc = stringResource(R.string.bn_guide_tip_mic_desc)
                )
                GuideTipCard(
                    title = stringResource(R.string.bn_guide_tip_caps_title),
                    desc = stringResource(R.string.bn_guide_tip_caps_desc)
                )
                GuideTipCard(
                    title = stringResource(R.string.bn_guide_tip_theme_title),
                    desc = stringResource(R.string.bn_guide_tip_theme_desc)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = RoyalBlueDeep
                )
            ) {
                Text(
                    text = stringResource(R.string.bn_guide_close),
                    fontFamily = NotoSansBengaliFamily,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlueDeep
                )
            }
        }
    )
}

@Composable
private fun GuideTipCard(
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassCardSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            fontFamily = NotoSansBengaliFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = desc,
            fontFamily = NotoSansBengaliFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color.White.copy(alpha = 0.90f)
        )
    }
}

@Composable
private fun PlaygroundReadoutCard(
    playgroundText: String,
    showSystemImeTester: Boolean,
    systemTestText: String,
    onSystemTestTextChange: (String) -> Unit,
    onToggleSystemTester: () -> Unit,
    onCopyPlayground: () -> Unit,
    onClearPlayground: () -> Unit
) {
    val cursorTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = RoundedCornerShape(18.dp),
                borderBrush = FlagshipGlassRimSubtle,
                borderWidth = 1.2.dp,
                glowColor = NeonCyan,
                glowElevation = 10.dp
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val playgroundStrings = LocalAppStrings.current
            val playgroundBodyFont = if (playgroundStrings.language == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = playgroundStrings.playgroundTitle,
                    fontFamily = playgroundBodyFont,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (playgroundText.isNotEmpty()) {
                    Text(
                        text = "· ${playgroundText.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (playgroundText.isNotEmpty()) {
                    IconButton(
                        onClick = onCopyPlayground,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_copy_playground")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.clipboard_add_sample),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (showSystemImeTester) GlassCardSelected
                            else GlassCardInner
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleSystemTester)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_system_ime_field"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = playgroundStrings.playgroundSystemIme,
                        fontFamily = playgroundBodyFont,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                if (playgroundText.isNotEmpty()) {
                    IconButton(
                        onClick = onClearPlayground,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_clear_playground")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_text),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp, max = 92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GlassCardInner)
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (playgroundText.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(16.dp)
                            .background(NeonCyan.copy(alpha = cursorAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.playground_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                val hasNonLatinScript = playgroundText.any { it.code > 127 }
                Text(
                    text = playgroundText + "▎",
                    fontFamily = if (hasNonLatinScript) FontFamily.Default else JetBrainsMonoFamily,
                    fontSize = if (hasNonLatinScript) 16.sp else 14.sp,
                    lineHeight = 22.sp,
                    color = TextPrimary,
                    modifier = Modifier.testTag("playground_output_text")
                )
            }
        }

        AnimatedVisibility(visible = showSystemImeTester) {
            OutlinedTextField(
                value = systemTestText,
                onValueChange = onSystemTestTextChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.playground_system_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan,
                    focusedContainerColor = ObsidianCanvas,
                    unfocusedContainerColor = ObsidianCanvas
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_ime_test_input")
            )
        }
    }
}
