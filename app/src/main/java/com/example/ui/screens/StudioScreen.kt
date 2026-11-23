package com.example.ui.screens

import android.Manifest
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.CustomPhotoTheme
import com.example.data.LicenseUiState
import com.example.engine.BanglaGuideScript
import com.example.engine.OnlineVoiceTypingController
import com.example.engine.RgbStickerGenerator
import com.example.engine.RgbStickerStyle
import com.example.engine.SmartWordPredictor
import com.example.engine.StylishFontEngine
import com.example.engine.StylishFontStyle
import com.example.engine.VoiceGuideState
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
import com.example.rgbkeyboard.RgbKeyboardView as NativeRgbKeyboardView
import com.example.ui.components.FeatureTourCoordinates
import com.example.ui.components.FeatureTourTarget
import com.example.ui.components.StudioAiHubPanel
import com.example.ui.components.StudioStylishFontPanel
import com.example.ui.theme.BrandObsidianPill
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
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RoyalGoldAccent
import com.example.ui.theme.RoyalGoldPillBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class StudioOverlayMode { NONE, CLIPBOARD, FONTS, EMOJI, STICKER_SELECTOR, AI_HUB }

@Composable
fun StudioScreen(
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    imeStatus: ImeStatus,
    licenseState: LicenseUiState,
    voiceGuideState: VoiceGuideState,
    languagePacks: List<LanguagePackUiState>,
    playgroundText: String,
    clipboardClips: List<String>,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenUnlockScreen: () -> Unit,
    onSpeakBanglaGuide: (BanglaGuideScript) -> Unit,
    onToggleVoiceGuideMute: () -> Unit,
    onSelectLanguageById: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectEffect: (RgbEffect) -> Unit,
    onSelectPalette: (RgbPalette) -> Unit,
    onCycleEffect: () -> Unit,
    onPlaygroundCommit: (String) -> Unit,
    onPlaygroundBackspace: () -> Unit,
    onPlaygroundSpace: () -> Unit,
    onPlaygroundEnter: () -> Unit,
    onClearPlayground: () -> Unit,
    onCopyPlayground: () -> Unit,
    onRefreshClipboard: () -> Unit,
    onRemoveClipboardItem: (String) -> Unit,
    onClearClipboard: () -> Unit,
    onKeyFeedback: (Boolean) -> Unit,
    onOpenHardwareSettings: () -> Unit,
    onOpenLanguagesTab: () -> Unit,
    isFeatureTourActive: Boolean = false,
    featureTourCoordinates: FeatureTourCoordinates? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showSystemImeTester by remember { mutableStateOf(false) }
    var showEasyGuideDialog by remember { mutableStateOf(false) }
    var systemTestText by remember { mutableStateOf("") }
    var activeOverlay by remember { mutableStateOf(StudioOverlayMode.NONE) }
    var activeStylishFont by remember { mutableStateOf(StylishFontStyle.NORMAL) }
    var voiceState by remember { mutableStateOf(OnlineVoiceTypingController.VoiceListeningState()) }
    var showVoiceBanner by remember { mutableStateOf(false) }
    var pendingStickerPhrase by remember { mutableStateOf("") }
    var previewSticker by remember { mutableStateOf<RgbStickerGenerator.StickerArtifact?>(null) }

    LaunchedEffect(isFeatureTourActive) {
        if (isFeatureTourActive) {
            activeOverlay = StudioOverlayMode.NONE
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val latestCommit by rememberUpdatedState(onPlaygroundCommit)
    val latestBackspace by rememberUpdatedState(onPlaygroundBackspace)
    val latestSpace by rememberUpdatedState(onPlaygroundSpace)
    val latestEnter by rememberUpdatedState(onPlaygroundEnter)
    val latestFeedback by rememberUpdatedState(onKeyFeedback)
    val latestToggleLanguage by rememberUpdatedState(onToggleLanguage)
    val latestActiveLangId by rememberUpdatedState(settings.activeLanguageId)
    val latestStylishFont by rememberUpdatedState(activeStylishFont)

    val voiceController = remember(context) {
        OnlineVoiceTypingController(
            context = context.applicationContext,
            onStateChanged = { next ->
                voiceState = next
                if (next.isListening || !next.errorMessage.isNullOrBlank()) {
                    showVoiceBanner = true
                }
            },
            onPartialText = { _ -> },
            onFinalText = { spoken ->
                latestCommit("$spoken ")
                showVoiceBanner = false
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
            voiceController.startListening(locale)
        } else {
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
            voiceGuideState = voiceGuideState,
            onSpeakScript = onSpeakBanglaGuide,
            onToggleMuteStop = onToggleVoiceGuideMute,
            onDismiss = { showEasyGuideDialog = false }
        )
    }

    previewSticker?.let { artifact ->
        AlertDialog(
            onDismissRequest = { previewSticker = null },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = stringResource(R.string.sticker_preview_title),
                    fontFamily = OrbitronFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
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
            // Ultra-Clean Minimalist Dark Glassmorphism Hero Card
            HeroStatusCard(
                imeStatus = imeStatus,
                licenseState = licenseState,
                voiceGuideState = voiceGuideState,
                onOpenImeSettings = onOpenImeSettings,
                onShowImePicker = onShowImePicker,
                onOpenUnlockScreen = onOpenUnlockScreen,
                onToggleVoiceGuideMute = onToggleVoiceGuideMute,
                onOpenEasyGuide = { showEasyGuideDialog = true }
            )

            val appStrings = LocalAppStrings.current
            val uiBodyFont = if (appStrings.language == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default

            // Quick Active Language Strip (Built-in English + Downloaded JSON Packs + Download Shortcut)
            val enabledLanguages = languagePacks.filter { it.isEnabled }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = appStrings.quickLanguageLabel,
                    fontFamily = uiBodyFont,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(enabledLanguages, key = { it.catalog.id }) { pack ->
                        val selected = pack.isActive
                        val pillLabel = when (pack.catalog.id) {
                            LanguageCatalog.ENGLISH_ID -> "EN"
                            else -> pack.catalog.nativeName
                        }
                        val pillFont = when (pack.catalog.id) {
                            LanguageCatalog.ENGLISH_ID -> JetBrainsMonoFamily
                            "bn" -> NotoSansBengaliFamily
                            else -> FontFamily.Default
                        }
                        Row(
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) Color(0xFF142230) else ObsidianSurface
                                )
                                .border(
                                    width = if (selected) 1.25.dp else 1.dp,
                                    color = if (selected) LuxuryCyanStroke.copy(alpha = 0.85f) else ObsidianBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectLanguageById(pack.catalog.id) }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                                .testTag("studio_lang_${pack.catalog.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = pillLabel,
                                fontFamily = pillFont,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    item(key = "add_more_languages") {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, LuxuryCyanStroke.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                                .clickable(onClick = onOpenLanguagesTab)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("studio_btn_more_languages"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appStrings.addLanguagesChip,
                                fontFamily = uiBodyFont,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = LuxuryCyanStroke,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Quick Lighting Mode Cards (Elevated #111520 studio surfaces with 16dp corners & subtle 1dp glass borders)
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = appStrings.quickEffectsLabel,
                    fontFamily = uiBodyFont,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(RgbEffect.entries) { effect ->
                        val selected = effect == settings.effect
                        val isLockedPreview = licenseState.isFreeStandardMode && effect.isVipEffect
                        Row(
                            modifier = Modifier
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) Color(0xFF171D2E) else ObsidianSurface
                                )
                                .border(
                                    width = if (selected) 1.25.dp else 1.dp,
                                    brush = if (selected) {
                                        Brush.horizontalGradient(listOf(LuxuryPinkStroke, LuxuryCyanStroke))
                                    } else {
                                        Brush.horizontalGradient(listOf(ObsidianBorder, ObsidianBorder))
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectEffect(effect) }
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                                .testTag("quick_effect_${effect.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = when {
                                    selected -> LuxuryCyanStroke
                                    !effect.isVipEffect -> TextMuted
                                    else -> LuxuryPinkStroke.copy(alpha = 0.8f)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isLockedPreview) "🔒 ${effect.shortLabel}" else effect.shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else TextSecondary,
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

        // Embedded Gboard-Layout Live Keyboard Deck with Touch Spark RGB, Custom Photo BG & Dynamic JSON Layouts
        val isFiveRowLayout = settings.activeLanguageId == "bn" || settings.activeLanguageId == "hi"
        val extraRowHeight = if (isFiveRowLayout) 26.dp else 0.dp
        val deckHeightDp = when (settings.heightPreset) {
            KeyboardHeightPreset.COMPACT -> 212.dp + extraRowHeight
            KeyboardHeightPreset.STANDARD -> 232.dp + extraRowHeight
            KeyboardHeightPreset.TALL -> 260.dp + extraRowHeight
        }

        // Free In-App Live VIP Demo Preview + Save / Apply to Keyboard Strip (shown after 7-day VIP trial expires)
        if (licenseState.isFreeStandardMode) {
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
            color = Color(0xFF101218),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                    .padding(4.dp)
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

                        // 3. Stylish Fonts (Aa)
                        val fontsActive = activeOverlay == StudioOverlayMode.FONTS || activeStylishFont != StylishFontStyle.NORMAL
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
                                    activeOverlay = if (activeOverlay == StudioOverlayMode.FONTS) {
                                        StudioOverlayMode.NONE
                                    } else {
                                        StudioOverlayMode.FONTS
                                    }
                                }
                                .testTag("studio_kb_fonts"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.toolbar_stylish_fonts),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (fontsActive) NeonCyan else TextPrimary
                            )
                        }

                        // 4. Voice Mic icon
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_mic,
                            contentDescription = stringResource(R.string.cd_voice_mic),
                            active = voiceState.isListening,
                            activeColor = NeonEmerald,
                            testTag = "studio_kb_mic",
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    featureTourCoordinates?.register(FeatureTourTarget.VOICE_AND_GLOBE, coords.boundsInRoot())
                                },
                            onClick = {
                                onKeyFeedback(true)
                                if (voiceController.isCurrentlyListening()) {
                                    voiceController.stopListening()
                                    showVoiceBanner = false
                                } else if (!voiceController.hasAudioPermission()) {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    val locale = OnlineVoiceTypingController.localeForLanguageId(settings.activeLanguageId)
                                    showVoiceBanner = true
                                    voiceController.startListening(locale)
                                }
                            }
                        )

                        // 5. Single Unified ✨ AI Hub Button (7-Day VIP Trial / Lifetime VIP)
                        val isVip = licenseState.isVipActive
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
                            tint = if (voiceState.isListening) NeonEmerald else NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
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
                            color = if (voiceState.isListening) NeonCyan else NeonAmber,
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

                // Scrollable Smart Word Prediction & Glowing "✨ RGB Sticker" Strip inside Live Playground
                if (activeOverlay == StudioOverlayMode.NONE) {
                    val prediction = remember(playgroundText, settings.activeLanguageId) {
                        SmartWordPredictor.predict(playgroundText, settings.activeLanguageId, context)
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .testTag("studio_suggestion_strip"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val effectiveStickerPhrase = prediction.stickerPhrase.ifEmpty {
                            if (isFeatureTourActive) "ভালোবাসা" else ""
                        }
                        if (effectiveStickerPhrase.isNotEmpty()) {
                            item(key = "rgb_sticker_chip") {
                                val isVip = licenseState.isVipActive
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

                        if (settings.smartSuggestionsEnabled) {
                            items(prediction.suggestions, key = { it }) { suggestion ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF19132B))
                                        .border(1.dp, Color(0xFF2F2352), RoundedCornerShape(999.dp))
                                        .clickable {
                                            onKeyFeedback(false)
                                            repeat(prediction.currentTokenLength) {
                                                onPlaygroundBackspace()
                                            }
                                            onPlaygroundCommit("$suggestion ")
                                        }
                                        .padding(horizontal = 11.dp, vertical = 5.dp)
                                        .testTag("studio_suggestion_$suggestion"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontFamily = if (settings.activeLanguageId == "bn") NotoSansBengaliFamily else FontFamily.Default,
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
                                onPlaygroundCommit(clip)
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
                            isBengaliActive = settings.activeLanguageId == "bn",
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

        Spacer(modifier = Modifier.height(16.dp))
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
            items(RgbStickerStyle.entries.chunked(2)) { pair ->
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

        // Scrollable 8-column Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
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
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
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
    voiceGuideState: VoiceGuideState,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenUnlockScreen: () -> Unit,
    onToggleVoiceGuideMute: () -> Unit,
    onOpenEasyGuide: () -> Unit
) {
    val glassShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(glassShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131826),
                        Color(0xFF0D111C)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFF262F47),
                shape = glassShape
            )
    ) {
        // Subtle luxury studio ambient rim sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LuxuryPinkStroke.copy(alpha = 0.06f),
                            LuxuryCyanStroke.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        radius = 720f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Luxury Dark Obsidian "Rakib official" Brand Pill + Royal Amber-Gold VIP Trial Pill + Circular Mute Button
            val voiceTint = when {
                voiceGuideState.isSpeaking -> NeonError
                voiceGuideState.isMuted -> TextMuted
                else -> LuxuryCyanStroke
            }
            val pillBorderColor = when {
                licenseState.isLifetimeUnlocked -> NeonEmerald.copy(alpha = 0.7f)
                licenseState.isFreeStandardMode -> LuxuryPinkStroke.copy(alpha = 0.65f)
                else -> RoyalGoldAccent.copy(alpha = 0.72f)
            }
            val pillTextColor = when {
                licenseState.isLifetimeUnlocked -> NeonEmerald
                licenseState.isFreeStandardMode -> Color(0xFFFF7AA8)
                else -> RoyalGoldAccent
            }
            val pillBg = when {
                licenseState.isLifetimeUnlocked -> Color(0xFF0E221C)
                licenseState.isFreeStandardMode -> Color(0xFF231320)
                else -> RoyalGoldPillBg
            }
            val strings = LocalAppStrings.current
            val heroBodyFont = if (strings.language == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default

            val pillText = when {
                licenseState.isLifetimeUnlocked -> strings.badgeLifetimeUnlocked
                licenseState.isFreeStandardMode -> strings.badgeFreeStandard
                else -> "VIP Trial • ${licenseState.formattedCompactEnglish}"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refined Luxury Dark Obsidian Glass Pill (#141824) with Pink-to-Cyan hairline stroke & bold white typography
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandObsidianPill)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(LuxuryPinkStroke, LuxuryCyanStroke)
                            ),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                        .testTag("hero_brand_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.studio_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = OrbitronFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.45.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Royal Amber-Gold VIP Trial Glass Pill + separate sleek circular Sound Mute Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(pillBg)
                            .border(1.dp, pillBorderColor, RoundedCornerShape(999.dp))
                            .clickable(enabled = !licenseState.isLifetimeUnlocked, onClick = onOpenUnlockScreen)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag(
                                when {
                                    licenseState.isLifetimeUnlocked -> "badge_lifetime_unlocked"
                                    licenseState.isFreeStandardMode -> "badge_free_standard_mode"
                                    else -> "badge_free_trial_timer"
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (licenseState.isLifetimeUnlocked) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = pillTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                        } else if (!licenseState.isFreeStandardMode) {
                            Text(
                                text = "👑",
                                fontSize = 11.5.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = pillTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = pillText,
                            fontFamily = if (!licenseState.isLifetimeUnlocked && !licenseState.isFreeStandardMode) {
                                FontFamily.Default
                            } else {
                                heroBodyFont
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp,
                            color = pillTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (!(imeStatus.isEnabled && imeStatus.isSelected)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ObsidianSurface)
                                .border(1.dp, ObsidianBorder, CircleShape)
                                .clickable(onClick = onToggleVoiceGuideMute)
                                .testTag("btn_voice_guide_mute_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    voiceGuideState.isSpeaking -> Icons.Filled.Stop
                                    voiceGuideState.isMuted -> Icons.Filled.VolumeOff
                                    else -> Icons.Filled.VolumeUp
                                },
                                contentDescription = stringResource(R.string.cd_voice_guide_toggle),
                                tint = voiceTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Row 2: Sleek refined Lifetime VIP status strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            if (licenseState.isLifetimeUnlocked) {
                                listOf(NeonEmerald.copy(alpha = 0.55f), LuxuryCyanStroke.copy(alpha = 0.45f))
                            } else {
                                listOf(LuxuryPinkStroke.copy(alpha = 0.48f), LuxuryCyanStroke.copy(alpha = 0.48f))
                            }
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = !licenseState.isLifetimeUnlocked, onClick = onOpenUnlockScreen)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("hero_lifetime_vip_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (licenseState.isLifetimeUnlocked) {
                        strings.vipBannerUnlocked
                    } else {
                        strings.vipBannerLocked
                    },
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = if (licenseState.isLifetimeUnlocked) NeonEmerald else Color(0xFFE4ECF7),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Elevated Flagship 2-Step Setup Cards (#111520 with 16dp corners & subtle 1dp glass borders)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MinimalistStepCard(
                    title = strings.setupStep1Title,
                    subtitle = if (imeStatus.isEnabled) strings.setupStep1Done else strings.setupStep1Subtitle,
                    isComplete = imeStatus.isEnabled,
                    completeBadgeText = strings.badgeEnabledShort,
                    completeColor = NeonEmerald,
                    completeContainer = NeonEmeraldContainer,
                    idleAccent = LuxuryPinkStroke,
                    icon = Icons.Filled.Settings,
                    onClick = onOpenImeSettings,
                    testTag = "btn_enable_ime"
                )

                MinimalistStepCard(
                    title = strings.setupStep2Title,
                    subtitle = if (imeStatus.isSelected) strings.setupStep2Done else strings.setupStep2Subtitle,
                    isComplete = imeStatus.isSelected,
                    completeBadgeText = strings.badgeActiveShort,
                    completeColor = LuxuryCyanStroke,
                    completeContainer = Color(0xFF0C222B),
                    idleAccent = LuxuryCyanStroke,
                    icon = Icons.Filled.SwapHoriz,
                    onClick = onShowImePicker,
                    testTag = "btn_select_ime"
                )
            }

            // Clean Studio-Grade Easy Guide Pill Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, LuxuryCyanStroke.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenEasyGuide)
                    .padding(horizontal = 16.dp, vertical = 11.dp)
                    .testTag("btn_bangla_easy_guide"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = null,
                    tint = LuxuryCyanStroke,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.btnEasyGuide,
                    fontFamily = heroBodyFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }
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
    testTag: String
) {
    val activeAccent = if (isComplete) completeColor else idleAccent
    val bgColor = if (isComplete) completeContainer.copy(alpha = 0.72f) else ObsidianSurface
    val borderColor = if (isComplete) completeColor.copy(alpha = 0.65f) else ObsidianBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(activeAccent.copy(alpha = 0.14f))
                .border(1.dp, activeAccent.copy(alpha = 0.36f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                tint = activeAccent,
                modifier = Modifier.size(20.dp)
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
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontFamily = NotoSansBengaliFamily,
                fontSize = 11.5.sp,
                color = if (isComplete) completeColor else TextSecondary,
                maxLines = 2
            )
        }

        if (isComplete) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(completeColor.copy(alpha = 0.16f))
                    .border(1.dp, completeColor.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = completeBadgeText,
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = completeColor
                )
            }
        }
    }
}

@Composable
private fun EasyBanglaGuideDialog(
    voiceGuideState: VoiceGuideState,
    onSpeakScript: (BanglaGuideScript) -> Unit,
    onToggleMuteStop: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bn_guide_dialog_title),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleMuteStop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (voiceGuideState.isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.cd_voice_guide_toggle),
                        tint = if (voiceGuideState.isSpeaking) NeonError else NeonCyan
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Full voice guide listen button
                Button(
                    onClick = { onSpeakScript(BanglaGuideScript.USAGE_GUIDE) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF001B22)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_guide_listen_full")
                ) {
                    Text(
                        text = stringResource(R.string.bn_btn_listen_voice),
                        fontFamily = NotoSansBengaliFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Quick Step Voice Replays
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSpeakScript(BanglaGuideScript.WELCOME_STEP_1) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔊 ধাপ ১",
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }
                    OutlinedButton(
                        onClick = { onSpeakScript(BanglaGuideScript.STEP_2) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔊 ধাপ ২",
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 12.sp,
                            color = NeonMagenta
                        )
                    }
                }

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
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF001B22)
                )
            ) {
                Text(
                    text = stringResource(R.string.bn_guide_close),
                    fontFamily = NotoSansBengaliFamily,
                    fontWeight = FontWeight.Bold
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
            .background(ObsidianCanvas)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            fontFamily = NotoSansBengaliFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )
        Text(
            text = desc,
            fontFamily = NotoSansBengaliFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = TextPrimary
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
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
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
                    color = NeonCyan
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
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (showSystemImeTester) NeonCyan.copy(alpha = 0.18f)
                            else ObsidianSurfaceVariant
                        )
                        .clickable(onClick = onToggleSystemTester)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_system_ime_field"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = null,
                        tint = if (showSystemImeTester) NeonCyan else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = playgroundStrings.playgroundSystemIme,
                        fontFamily = playgroundBodyFont,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showSystemImeTester) NeonCyan else TextSecondary
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
                            tint = TextSecondary,
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
                .background(ObsidianCanvas)
                .border(1.dp, Color(0xFF1C2133), RoundedCornerShape(10.dp))
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
