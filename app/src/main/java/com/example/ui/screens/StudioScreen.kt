package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class StudioOverlayMode { NONE, CLIPBOARD, REACTIONS }

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
    "বাংলাদেশ 🇧🇩" to "বাংলাদেশ 🇧🇩"
)

@Composable
fun StudioScreen(
    settings: KeyboardSettings,
    imeStatus: ImeStatus,
    languagePacks: List<LanguagePackUiState>,
    playgroundText: String,
    clipboardClips: List<String>,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
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
    var showSystemImeTester by remember { mutableStateOf(false) }
    var systemTestText by remember { mutableStateOf("") }
    var activeOverlay by remember { mutableStateOf(StudioOverlayMode.NONE) }
    var isEmojiOpen by remember { mutableStateOf(false) }
    var nativeKeyboardRef by remember { mutableStateOf<NativeRgbKeyboardView?>(null) }

    val latestCommit by rememberUpdatedState(onPlaygroundCommit)
    val latestBackspace by rememberUpdatedState(onPlaygroundBackspace)
    val latestSpace by rememberUpdatedState(onPlaygroundSpace)
    val latestEnter by rememberUpdatedState(onPlaygroundEnter)
    val latestFeedback by rememberUpdatedState(onKeyFeedback)
    val latestToggleLanguage by rememberUpdatedState(onToggleLanguage)

    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                latestCommit(spoken)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Hardware Banner + Live IME Status
            HeroStatusCard(
                imeStatus = imeStatus,
                settings = settings,
                onOpenImeSettings = onOpenImeSettings,
                onShowImePicker = onShowImePicker
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
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) NeonCyan.copy(alpha = 0.14f) else ObsidianSurface
                                )
                                .border(
                                    width = if (selected) 1.4.dp else 1.dp,
                                    color = if (selected) NeonCyan else ObsidianBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectLanguageById(pack.catalog.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("studio_lang_${pack.catalog.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = pack.catalog.shortBadge,
                                fontFamily = if (pack.catalog.id == LanguageCatalog.ENGLISH_ID) JetBrainsMonoFamily else FontFamily.Default,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) NeonCyan else TextMuted
                            )
                            Text(
                                text = pack.catalog.nativeName,
                                fontFamily = FontFamily.Default,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) TextPrimary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    item(key = "add_more_languages") {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianSurfaceVariant)
                                .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenLanguagesTab)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("studio_btn_more_languages"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.add_languages_chip),
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan
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
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) ObsidianSurfaceVariant else ObsidianSurface
                                )
                                .border(
                                    width = if (selected) 1.25.dp else 1.dp,
                                    brush = if (selected) {
                                        Brush.horizontalGradient(settings.palette.colors)
                                    } else {
                                        Brush.horizontalGradient(listOf(ObsidianBorder, ObsidianBorder))
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectEffect(effect) }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("quick_effect_${effect.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = if (selected) NeonCyan else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = effect.shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) TextPrimary else TextSecondary
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

        // Embedded Gboard-Layout Live Keyboard Deck with Touch Spark RGB & Dynamic JSON Layouts
        val isFiveRowLayout = settings.activeLanguageId == "bn" || settings.activeLanguageId == "hi"
        val extraRowHeight = if (isFiveRowLayout) 26.dp else 0.dp
        val deckHeightDp = when (settings.heightPreset) {
            KeyboardHeightPreset.COMPACT -> 212.dp + extraRowHeight
            KeyboardHeightPreset.STANDARD -> 232.dp + extraRowHeight
            KeyboardHeightPreset.TALL -> 260.dp + extraRowHeight
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
                // Clean Gboard-style 7-icon evenly spaced top toolbar
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
                                if (isEmojiOpen) nativeKeyboardRef?.toggleEmojiMode()
                                activeOverlay = if (activeOverlay == StudioOverlayMode.CLIPBOARD) {
                                    StudioOverlayMode.NONE
                                } else {
                                    StudioOverlayMode.CLIPBOARD
                                }
                            }
                        )

                        // 2. Emoji / Sticker icon
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_emoji,
                            contentDescription = stringResource(R.string.cd_emoji),
                            active = isEmojiOpen,
                            testTag = "studio_kb_emoji",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                activeOverlay = StudioOverlayMode.NONE
                                nativeKeyboardRef?.toggleEmojiMode()
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
                                if (isEmojiOpen) nativeKeyboardRef?.toggleEmojiMode()
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

                        // 5. Language / Translate icon -> Cycles all enabled dynamic JSON languages
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_translate,
                            contentDescription = stringResource(R.string.cd_switch_language),
                            active = settings.activeLanguageId != LanguageCatalog.ENGLISH_ID,
                            testTag = "studio_kb_lang",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                activeOverlay = StudioOverlayMode.NONE
                                onToggleLanguage()
                            }
                        )

                        // 6. Theme Palette icon -> Cycles Touch Spark & RGB studio effects
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

                        // 7. Voice Mic icon -> Launches Android Speech Recognizer
                        GboardToolbarIcon(
                            iconRes = R.drawable.ic_kb_mic,
                            contentDescription = stringResource(R.string.cd_voice_mic),
                            active = false,
                            testTag = "studio_kb_mic",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onKeyFeedback(true)
                                val activeLocale = LanguageCatalog.findById(settings.activeLanguageId)?.locale
                                    ?.replace('_', '-')
                                    ?: "en-US"
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLocale)
                                }
                                runCatching { voiceInputLauncher.launch(intent) }
                            }
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
                                    onEmojiModeChanged = { open -> isEmojiOpen = open }
                                    updateSettings(settings)
                                    nativeKeyboardRef = this
                                }
                            },
                            update = { view ->
                                nativeKeyboardRef = view
                                view.updateSettings(settings)
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
private fun GboardToolbarIcon(
    iconRes: Int,
    contentDescription: String,
    active: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
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
            tint = if (active) Color(0xFF60A5FA) else Color(0xFFC9D1DE),
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
    settings: KeyboardSettings,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianSurface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        settings.palette.colors.first().copy(alpha = 0.65f),
                        ObsidianBorder,
                        settings.palette.colors.last().copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_rgb_hero_banner_1795266996963),
            contentDescription = stringResource(R.string.cd_hero_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(22.dp))
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to ObsidianCanvas.copy(alpha = 0.96f),
                        0.56f to ObsidianCanvas.copy(alpha = 0.84f),
                        1.0f to ObsidianCanvas.copy(alpha = 0.42f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.studio_tagline),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan
                )
                ImeStatusBadge(imeStatus = imeStatus)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.studio_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.studio_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!imeStatus.isEnabled) {
                    Button(
                        onClick = onOpenImeSettings,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF001B22)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_enable_ime")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_enable_ime),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else if (!imeStatus.isSelected) {
                    Button(
                        onClick = onShowImePicker,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMagenta,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_select_ime")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_select_ime),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenImeSettings,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_open_ime_settings_secondary")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_open_settings),
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonEmeraldContainer.copy(alpha = 0.85f))
                            .border(1.dp, NeonEmerald.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_active_ready),
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonEmerald
                        )
                    }
                    OutlinedButton(
                        onClick = onShowImePicker,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_switch_ime_active")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = stringResource(R.string.action_select_ime),
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImeStatusBadge(imeStatus: ImeStatus) {
    val (bg, fg, label) = when {
        imeStatus.isSelected -> Triple(
            NeonEmeraldContainer,
            NeonEmerald,
            stringResource(R.string.status_active)
        )
        imeStatus.isEnabled -> Triple(
            NeonAmberContainer,
            NeonAmber,
            stringResource(R.string.status_enabled)
        )
        else -> Triple(
            ObsidianSurfaceVariant,
            TextSecondary,
            stringResource(R.string.status_not_enabled)
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg.copy(alpha = 0.9f))
            .border(1.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("ime_status_badge"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg
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
