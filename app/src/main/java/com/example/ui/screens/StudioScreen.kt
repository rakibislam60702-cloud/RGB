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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.engine.VoiceGuideState
import com.example.model.EmojiCatalog
import com.example.model.ImeStatus
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardSettings
import com.example.model.LanguageCatalog
import com.example.model.LanguagePackUiState
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.rgbkeyboard.RgbKeyboardView as NativeRgbKeyboardView
import com.example.ui.theme.JetBrainsMonoFamily
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class StudioOverlayMode { NONE, CLIPBOARD, REACTIONS, EMOJI }

private val studioQuickReactions = listOf(
    "🔥 Lit!" to "🔥",
    "⚡ GG WP" to "⚡ GG WP!",
    "✨ Nice!" to "✨ Nice!",
    "🚀 Let's Go" to "🚀 Let's go!",
    "¯\\_(ツ)_/¯" to "¯\\_(ツ)_/¯",
    "(ง •̀_•́)ง" to "(ง •̀_•́)ง",
    "❤️ Love it" to "❤️ Love it!",
    "🎉 Congrats" to "🎉 Congratulations!",
    "😎 Cool" to "😎",
    "💯 100%" to "💯",
    "(♥‿♥)" to "(♥‿♥)",
    "✨ দারুণ!" to "✨ দারুণ!"
)

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSystemImeTester by remember { mutableStateOf(false) }
    var showEasyGuideDialog by remember { mutableStateOf(false) }
    var systemTestText by remember { mutableStateOf("") }
    var activeOverlay by remember { mutableStateOf(StudioOverlayMode.NONE) }
    var voiceState by remember { mutableStateOf(OnlineVoiceTypingController.VoiceListeningState()) }
    var showVoiceBanner by remember { mutableStateOf(false) }

    val latestCommit by rememberUpdatedState(onPlaygroundCommit)
    val latestBackspace by rememberUpdatedState(onPlaygroundBackspace)
    val latestSpace by rememberUpdatedState(onPlaygroundSpace)
    val latestEnter by rememberUpdatedState(onPlaygroundEnter)
    val latestFeedback by rememberUpdatedState(onKeyFeedback)
    val latestToggleLanguage by rememberUpdatedState(onToggleLanguage)
    val latestActiveLangId by rememberUpdatedState(settings.activeLanguageId)

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

            // Quick Active Language Strip (Built-in English + Downloaded JSON Packs + Download Shortcut)
            val enabledLanguages = languagePacks.filter { it.isEnabled }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.quick_language_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selected) NeonCyan.copy(alpha = 0.16f) else ObsidianSurface
                                )
                                .border(
                                    width = if (selected) 1.4.dp else 1.dp,
                                    color = if (selected) NeonCyan else ObsidianBorder,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { onSelectLanguageById(pack.catalog.id) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("studio_lang_${pack.catalog.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = pillLabel,
                                fontFamily = pillFont,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) NeonCyan else TextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    item(key = "add_more_languages") {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(ObsidianSurfaceVariant)
                                .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                                .clickable(onClick = onOpenLanguagesTab)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("studio_btn_more_languages"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.add_languages_chip),
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Quick Lighting Mode Chips (Touch Spark default + optional continuous RGB modes)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.quick_effects_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(RgbEffect.entries) { effect ->
                        val selected = effect == settings.effect
                        val isLockedPreview = licenseState.isFreeStandardMode && effect.isVipEffect
                        Row(
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selected) ObsidianSurfaceVariant else ObsidianSurface
                                )
                                .border(
                                    width = if (selected) 1.35.dp else 1.dp,
                                    brush = if (selected) {
                                        Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan))
                                    } else {
                                        Brush.horizontalGradient(listOf(ObsidianBorder, ObsidianBorder))
                                    },
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { onSelectEffect(effect) }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .testTag("quick_effect_${effect.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = when {
                                    selected -> NeonCyan
                                    !effect.isVipEffect -> TextSecondary
                                    else -> NeonMagenta.copy(alpha = 0.8f)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isLockedPreview) "🔒 ${effect.shortLabel}" else effect.shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) TextPrimary else TextSecondary,
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
                Text(
                    text = stringResource(R.string.bn_demo_preview_tag),
                    fontFamily = NotoSansBengaliFamily,
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
                        text = stringResource(R.string.bn_btn_apply_to_keyboard),
                        fontFamily = NotoSansBengaliFamily,
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
                // Clean Gboard-style 6-icon evenly spaced top toolbar (Globe key switches Bangla/English)
                if (settings.showToolbar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Feature Menu (4-dots grid) -> Clipboard Manager
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_grid_menu,
                            contentDescription = stringResource(R.string.cd_clipboard),
                            active = activeOverlay == StudioOverlayMode.CLIPBOARD,
                            testTag = "studio_kb_menu",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                onRefreshClipboard()
                                activeOverlay = if (activeOverlay == StudioOverlayMode.CLIPBOARD) {
                                    StudioOverlayMode.NONE
                                } else {
                                    StudioOverlayMode.CLIPBOARD
                                }
                            }
                        )

                        // 2. Emoji / Sticker icon -> Unlimited Categorized Emoji Panel
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_emoji,
                            contentDescription = stringResource(R.string.cd_emoji),
                            active = activeOverlay == StudioOverlayMode.EMOJI,
                            testTag = "studio_kb_emoji",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                activeOverlay = if (activeOverlay == StudioOverlayMode.EMOJI) {
                                    StudioOverlayMode.NONE
                                } else {
                                    StudioOverlayMode.EMOJI
                                }
                            }
                        )

                        // 3. GIF / Quick Reactions icon
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_gif,
                            contentDescription = stringResource(R.string.cd_gif_reactions),
                            active = activeOverlay == StudioOverlayMode.REACTIONS,
                            testTag = "studio_kb_gif",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                activeOverlay = if (activeOverlay == StudioOverlayMode.REACTIONS) {
                                    StudioOverlayMode.NONE
                                } else {
                                    StudioOverlayMode.REACTIONS
                                }
                            }
                        )

                        // 4. Settings gear -> Companion Hardware & Feel settings tab
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_settings,
                            contentDescription = stringResource(R.string.action_open_settings),
                            active = false,
                            testTag = "studio_kb_settings",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                onOpenHardwareSettings()
                            }
                        )

                        // 5. Theme Palette icon -> Cycles Touch Spark & RGB studio effects
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_palette,
                            contentDescription = stringResource(R.string.cd_cycle_effect),
                            active = settings.effect != RgbEffect.TOUCH_SPARK,
                            testTag = "studio_kb_theme",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                onCycleEffect()
                            }
                        )

                        // 6. Voice Mic icon -> Online Bengali & English SpeechRecognizer
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_mic,
                            contentDescription = stringResource(R.string.cd_voice_mic),
                            active = voiceState.isListening,
                            activeColor = NeonEmerald,
                            testTag = "studio_kb_mic",
                            modifier = Modifier.weight(1f),
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
                    }
                }

                // Inline Online Voice Typing Bar (Bengali & English Live SpeechRecognizer)
                AnimatedVisibility(visible = showVoiceBanner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF172033))
                            .border(
                                1.dp,
                                if (voiceState.isListening) NeonCyan.copy(alpha = 0.65f) else NeonAmber.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = if (voiceState.isListening) NeonEmerald else NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                voiceState.partialTranscript.isNotBlank() -> voiceState.partialTranscript
                                voiceState.isListening && voiceState.locale.isBengali ->
                                    stringResource(R.string.voice_listening_bn)
                                voiceState.isListening ->
                                    stringResource(R.string.voice_listening_en)
                                !voiceState.errorMessage.isNullOrBlank() ->
                                    voiceState.errorMessage.orEmpty()
                                else -> stringResource(R.string.voice_tap_to_speak)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (voiceState.isListening) NeonCyan else NeonAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (voiceState.locale.isBengali) "বাংলা ⇄ EN" else "EN ⇄ বাংলা",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF243552))
                                .clickable {
                                    onKeyFeedback(true)
                                    if (!voiceController.hasAudioPermission()) {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        voiceController.switchLocaleAndRestart()
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("studio_voice_lang_toggle")
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

                    StudioOverlayMode.REACTIONS -> {
                        StudioReactionsPanel(
                            height = deckHeightDp,
                            onSelectReaction = { text ->
                                onKeyFeedback(false)
                                onPlaygroundCommit(text)
                                activeOverlay = StudioOverlayMode.NONE
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

                    StudioOverlayMode.NONE -> {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(deckHeightDp)
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
                                            else -> latestCommit(key)
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
private fun StudioReactionsPanel(
    height: androidx.compose.ui.unit.Dp,
    onSelectReaction: (String) -> Unit,
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
                text = stringResource(R.string.reactions_title),
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF60A5FA)
            )
            Text(
                text = "⌨",
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1D2030))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("studio_reactions_close")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(studioQuickReactions.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { (label, payload) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1A1E2B))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectReaction(payload) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
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
    val glassShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(glassShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF170D2B),
                        Color(0xFF0E081C),
                        Color(0xFF1E0D2E)
                    )
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(
                        NeonMagenta.copy(alpha = 0.85f),
                        NeonAmber.copy(alpha = 0.7f),
                        NeonCyan.copy(alpha = 0.85f)
                    )
                ),
                shape = glassShape
            )
    ) {
        // GTA VI Vice City neon sunset ambient glassmorphism sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonMagenta.copy(alpha = 0.14f),
                            NeonCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 780f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clean, Spacious 2-Row Header inside the Glassmorphism Card
            // Row 1: Left-aligned glowing gradient "Rakib official" title + right-aligned VIP Trial pill with integrated mute/unmute icon
            val voiceTint = when {
                voiceGuideState.isSpeaking -> NeonError
                voiceGuideState.isMuted -> TextMuted
                else -> NeonCyan
            }
            val pillColor = when {
                licenseState.isLifetimeUnlocked -> NeonEmerald
                licenseState.isFreeStandardMode -> NeonMagenta
                else -> NeonAmber
            }
            val pillBg = when {
                licenseState.isLifetimeUnlocked -> NeonEmeraldContainer.copy(alpha = 0.7f)
                licenseState.isFreeStandardMode -> Color(0xFF2D0D25)
                else -> NeonAmberContainer.copy(alpha = 0.65f)
            }
            val pillText = when {
                licenseState.isLifetimeUnlocked -> stringResource(R.string.bn_lifetime_unlocked_badge)
                licenseState.isFreeStandardMode -> stringResource(R.string.bn_free_standard_badge)
                else -> stringResource(
                    R.string.bn_trial_badge_format,
                    licenseState.formattedCompactPillBangla
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.studio_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = OrbitronFamily,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(
                            listOf(NeonMagenta, NeonAmber, NeonCyan)
                        )
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Unified sleek right-aligned VIP Trial pill with integrated mute/unmute icon (no overlap or crowding)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(pillBg)
                        .border(1.dp, pillColor.copy(alpha = 0.65f), RoundedCornerShape(999.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(enabled = !licenseState.isLifetimeUnlocked, onClick = onOpenUnlockScreen)
                            .padding(start = 9.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                            .testTag(
                                when {
                                    licenseState.isLifetimeUnlocked -> "badge_lifetime_unlocked"
                                    licenseState.isFreeStandardMode -> "badge_free_standard_mode"
                                    else -> "badge_free_trial_timer"
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (licenseState.isLifetimeUnlocked) Icons.Filled.Verified else Icons.Filled.Timer,
                            contentDescription = null,
                            tint = pillColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = pillText,
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = pillColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(pillColor.copy(alpha = 0.3f))
                    )

                    Box(
                        modifier = Modifier
                            .clickable(onClick = onToggleVoiceGuideMute)
                            .padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
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
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Row 2: Full-width glowing cyan-magenta pill banner: "LIFETIME VIP • ONE-TIME PAYMENT (₹20)"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(NeonCyan.copy(alpha = 0.16f), NeonMagenta.copy(alpha = 0.18f))
                        )
                    )
                    .border(
                        1.1.dp,
                        Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.7f), NeonMagenta.copy(alpha = 0.7f))),
                        RoundedCornerShape(999.dp)
                    )
                    .clickable(enabled = !licenseState.isLifetimeUnlocked, onClick = onOpenUnlockScreen)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("hero_lifetime_vip_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (licenseState.isLifetimeUnlocked) {
                        "✓ LIFETIME VIP UNLOCKED · ALL EFFECTS ACTIVE"
                    } else {
                        stringResource(R.string.vip_one_time_badge)
                    },
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (licenseState.isLifetimeUnlocked) NeonEmerald else NeonCyan,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Modern 2-Step Minimalist Action Cards
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                // Step 1: ১. কিবোর্ড চালু করুন ("তালিকার নিচে 'Rakib official' অন করুন")
                MinimalistStepCard(
                    title = stringResource(R.string.bn_setup_step_1_title),
                    subtitle = stringResource(
                        if (imeStatus.isEnabled) R.string.bn_setup_step_1_done
                        else R.string.bn_setup_step_1_subtitle
                    ),
                    isComplete = imeStatus.isEnabled,
                    completeBadgeText = stringResource(R.string.bn_badge_enabled_short),
                    completeColor = NeonEmerald,
                    completeContainer = NeonEmeraldContainer,
                    idleAccent = NeonMagenta,
                    icon = Icons.Filled.Settings,
                    onClick = onOpenImeSettings,
                    testTag = "btn_enable_ime"
                )

                // Step 2: ২. কিবোর্ড সিলেক্ট করুন (glowing electric cyan "সক্রিয় ✓" state once selected)
                MinimalistStepCard(
                    title = stringResource(R.string.bn_setup_step_2_title),
                    subtitle = stringResource(
                        if (imeStatus.isSelected) R.string.bn_setup_step_2_done
                        else R.string.bn_setup_step_2_subtitle
                    ),
                    isComplete = imeStatus.isSelected,
                    completeBadgeText = stringResource(R.string.bn_badge_active_short),
                    completeColor = NeonCyan,
                    completeContainer = Color(0xFF072B27),
                    idleAccent = NeonCyan,
                    icon = Icons.Filled.SwapHoriz,
                    onClick = onShowImePicker,
                    testTag = "btn_select_ime"
                )
            }

            // Compact, stylish Rounded Cyan Easy Guide Button below the 2 cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(NeonCyan.copy(alpha = 0.12f))
                    .border(1.2.dp, NeonCyan.copy(alpha = 0.58f), RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenEasyGuide)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("btn_bangla_easy_guide"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.bn_btn_easy_guide),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 12.5.sp,
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
    val bgColor = if (isComplete) completeContainer.copy(alpha = 0.62f) else Color(0xCC18102C)
    val borderColor = if (isComplete) completeColor.copy(alpha = 0.82f) else Color(0xFF342258)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(activeAccent.copy(alpha = 0.17f))
                .border(1.dp, activeAccent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                tint = activeAccent,
                modifier = Modifier.size(19.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.playground_title),
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
                        text = stringResource(R.string.playground_system_ime),
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
