package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CustomPhotoTheme
import com.example.data.LicenseUiState
import com.example.engine.VoiceGuideState
import com.example.model.AppUiLanguage
import com.example.model.AppUiStrings
import com.example.model.LanguagePackUiState
import com.example.model.LocalAppStrings
import com.example.model.appStringsFor
import com.example.ui.components.FeatureTourCoordinates
import com.example.ui.components.FinalLifetimeVipAutoBanner
import com.example.ui.components.LifetimeUnlockScreen
import com.example.ui.components.SilentFeatureTourOverlay
import com.example.ui.screens.EffectsScreen
import com.example.ui.screens.HardwareScreen
import com.example.ui.screens.LanguagesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.NotoSansBengaliFamily
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanContainer
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ProRgbKeyboardTheme
import com.example.ui.theme.TextMuted
import com.example.viewmodel.StudioViewModel

private enum class StudioTab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    STUDIO(
        selectedIcon = Icons.Filled.Keyboard,
        unselectedIcon = Icons.Outlined.Keyboard,
        testTag = "nav_tab_studio"
    ),
    LANGUAGES(
        selectedIcon = Icons.Filled.Translate,
        unselectedIcon = Icons.Outlined.Translate,
        testTag = "nav_tab_languages"
    ),
    EFFECTS(
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_effects"
    ),
    HARDWARE(
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_hardware"
    ),
    SETTINGS(
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    );

    fun label(strings: AppUiStrings): String = when (this) {
        STUDIO -> strings.navStudio
        LANGUAGES -> strings.navLanguages
        EFFECTS -> strings.navEffects
        HARDWARE -> strings.navFeel
        SETTINGS -> strings.navSettings
    }
}

class MainActivity : ComponentActivity() {

    private var windowFocusTick by mutableIntStateOf(0)
    private var openUnlockTrigger by mutableIntStateOf(0)

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result is automatically observed by RgbKeyboardService next time mic is tapped
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        handleIncomingIntent(intent)
        requestMicPermissionOnceIfNeeded()
        setContent {
            ProRgbKeyboardTheme {
                ProRgbKeyboardApp(
                    windowFocusTick = windowFocusTick,
                    openUnlockTrigger = openUnlockTrigger
                )
            }
        }
    }

    /**
     * Requests RECORD_AUDIO once inside the companion app so tapping the Mic key
     * on the system keyboard starts direct voice typing immediately without re-opening the app.
     */
    private fun requestMicPermissionOnceIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return

        val prefs = getSharedPreferences(PREFS_PERMISSIONS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MIC_PROMPTED_ONCE, false)) {
            prefs.edit().putBoolean(KEY_MIC_PROMPTED_ONCE, true).apply()
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            windowFocusTick++
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_REQUEST_AUDIO_PERMISSION, false) == true) {
            intent.removeExtra(EXTRA_REQUEST_AUDIO_PERMISSION)
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_UNLOCK_SCREEN, false) == true) {
            intent.removeExtra(EXTRA_OPEN_UNLOCK_SCREEN)
            openUnlockTrigger++
        }
    }

    companion object {
        const val EXTRA_REQUEST_AUDIO_PERMISSION = "extra_request_audio_permission"
        const val EXTRA_OPEN_UNLOCK_SCREEN = "extra_open_unlock_screen"
        private const val PREFS_PERMISSIONS = "rakib_permissions_prefs"
        private const val KEY_MIC_PROMPTED_ONCE = "mic_permission_prompted_once"
    }
}

@Composable
fun ProRgbKeyboardApp(
    windowFocusTick: Int = 0,
    openUnlockTrigger: Int = 0,
    viewModel: StudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val appUiLanguage by viewModel.appUiLanguage.collectAsStateWithLifecycle()
    val appStrings = remember(appUiLanguage) { appStringsFor(appUiLanguage) }
    val navLabelFont = if (appUiLanguage == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val customPhotoTheme by viewModel.customPhotoTheme.collectAsStateWithLifecycle()
    val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
    val licenseState by viewModel.licenseState.collectAsStateWithLifecycle()
    val voiceGuideState by viewModel.voiceGuideState.collectAsStateWithLifecycle()
    val languagePacks by viewModel.languagePacks.collectAsStateWithLifecycle()
    val playgroundText by viewModel.playgroundText.collectAsStateWithLifecycle()
    val clipboardClips by viewModel.clipboardClips.collectAsStateWithLifecycle()
    val showFeatureTour by viewModel.showFeatureTour.collectAsStateWithLifecycle()
    val showFinalVipBanner by viewModel.showFinalVipBanner.collectAsStateWithLifecycle()
    val featureTourCoordinates = remember { FeatureTourCoordinates() }

    var currentTab by rememberSaveable { mutableStateOf(StudioTab.STUDIO) }
    var showVipUnlockModal by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showFeatureTour) {
        if (showFeatureTour) {
            currentTab = StudioTab.STUDIO
        }
    }

    LaunchedEffect(openUnlockTrigger) {
        if (openUnlockTrigger > 0) {
            viewModel.refreshLicenseState()
            if (!licenseState.isLifetimeUnlocked) {
                showVipUnlockModal = true
            }
        }
    }

    LaunchedEffect(licenseState.isLifetimeUnlocked) {
        if (licenseState.isLifetimeUnlocked) {
            showVipUnlockModal = false
        }
    }

    BackHandler(enabled = showFeatureTour) {
        viewModel.finishOrSkipFeatureTour()
    }

    BackHandler(enabled = showVipUnlockModal) {
        showVipUnlockModal = false
    }

    BackHandler(enabled = !showVipUnlockModal && !showFeatureTour && currentTab != StudioTab.STUDIO) {
        currentTab = StudioTab.STUDIO
    }

    // Re-check IME & license status immediately when system dialogs close
    LaunchedEffect(windowFocusTick) {
        if (windowFocusTick > 0) {
            viewModel.refreshImeStatus()
            viewModel.refreshLicenseState()
        }
    }

    // Automatically re-check Android IME enabled & default status, license, languages, and clipboard on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshImeStatus()
                viewModel.refreshLicenseState()
                viewModel.refreshClipboard()
                viewModel.refreshDownloadedLanguages()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openImeSettings: () -> Unit = {
        runCatching {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    val showImePicker: () -> Unit = {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        runCatching { imm?.showInputMethodPicker() }
        viewModel.refreshImeStatus()
    }

    CompositionLocalProvider(LocalAppStrings provides appStrings) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianCanvas)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NeonMagenta.copy(alpha = 0.08f),
                            ComposeColor.Transparent,
                            NeonCyan.copy(alpha = 0.06f)
                        )
                    )
                )
        ) {
            val useNavRail = maxWidth >= 600.dp

            if (useNavRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val tabLabel = tab.label(appStrings)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel,
                                        fontFamily = navLabelFont,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        TabContent(
                            currentTab = currentTab,
                            appUiLanguage = appUiLanguage,
                            settings = settings,
                            customPhotoTheme = customPhotoTheme,
                            imeStatus = imeStatus,
                            licenseState = licenseState,
                            voiceGuideState = voiceGuideState,
                            languagePacks = languagePacks,
                            playgroundText = playgroundText,
                            clipboardClips = clipboardClips,
                            viewModel = viewModel,
                            onOpenImeSettings = openImeSettings,
                            onShowImePicker = showImePicker,
                            onOpenUnlockScreen = { showVipUnlockModal = true },
                            onOpenHardwareSettings = { currentTab = StudioTab.HARDWARE },
                            onOpenLanguagesTab = { currentTab = StudioTab.LANGUAGES },
                            isFeatureTourActive = showFeatureTour,
                            featureTourCoordinates = featureTourCoordinates
                        )
                    }
                }
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ComposeColor.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            tonalElevation = 0.dp
                        ) {
                            StudioTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                val tabLabel = tab.label(appStrings)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tabLabel
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tabLabel,
                                            fontFamily = navLabelFont,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyanContainer,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        TabContent(
                            currentTab = currentTab,
                            appUiLanguage = appUiLanguage,
                            settings = settings,
                            customPhotoTheme = customPhotoTheme,
                            imeStatus = imeStatus,
                            licenseState = licenseState,
                            voiceGuideState = voiceGuideState,
                            languagePacks = languagePacks,
                            playgroundText = playgroundText,
                            clipboardClips = clipboardClips,
                            viewModel = viewModel,
                            onOpenImeSettings = openImeSettings,
                            onShowImePicker = showImePicker,
                            onOpenUnlockScreen = { showVipUnlockModal = true },
                            onOpenHardwareSettings = { currentTab = StudioTab.HARDWARE },
                            onOpenLanguagesTab = { currentTab = StudioTab.LANGUAGES },
                            isFeatureTourActive = showFeatureTour,
                            featureTourCoordinates = featureTourCoordinates
                        )
                    }
                }
            }

            // Stage 2: 100% Silent Bouncing Green/Red Arrow Feature Tour with fixed top-right "Skip ✕"
            if (showFeatureTour && !showVipUnlockModal) {
                SilentFeatureTourOverlay(
                    coordinates = featureTourCoordinates,
                    onFinishOrSkip = viewModel::finishOrSkipFeatureTour
                )
            }

            // Stage 3: Auto-Disappearing Final Lifetime VIP Banner (fades out completely after 3 seconds, non-blocking)
            FinalLifetimeVipAutoBanner(visible = showFinalVipBanner)

            // Dismissible Bengali ₹20 Lifetime VIP Unlock Modal (never locks out free standard typing or live demo preview)
            if (showVipUnlockModal && !licenseState.isLifetimeUnlocked) {
                LifetimeUnlockScreen(
                    licenseState = licenseState,
                    upiQrPayload = viewModel.buildUpiQrPayload(),
                    createUpiPaymentIntent = viewModel::createOfficialUpiIntent,
                    onVerifyUpiResult = viewModel::verifyUpiPaymentResult,
                    onDismiss = { showVipUnlockModal = false }
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    currentTab: StudioTab,
    appUiLanguage: AppUiLanguage,
    settings: com.example.model.KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    imeStatus: com.example.model.ImeStatus,
    licenseState: LicenseUiState,
    voiceGuideState: VoiceGuideState,
    languagePacks: List<LanguagePackUiState>,
    playgroundText: String,
    clipboardClips: List<String>,
    viewModel: StudioViewModel,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenUnlockScreen: () -> Unit,
    onOpenHardwareSettings: () -> Unit,
    onOpenLanguagesTab: () -> Unit,
    isFeatureTourActive: Boolean,
    featureTourCoordinates: FeatureTourCoordinates
) {
    Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
        when (tab) {
            StudioTab.STUDIO -> StudioScreen(
                settings = settings,
                customPhotoTheme = customPhotoTheme,
                imeStatus = imeStatus,
                licenseState = licenseState,
                voiceGuideState = voiceGuideState,
                languagePacks = languagePacks,
                playgroundText = playgroundText,
                clipboardClips = clipboardClips,
                onOpenImeSettings = onOpenImeSettings,
                onShowImePicker = onShowImePicker,
                onOpenUnlockScreen = onOpenUnlockScreen,
                onSpeakBanglaGuide = viewModel::speakBanglaGuide,
                onToggleVoiceGuideMute = viewModel::toggleVoiceGuideStopOrMute,
                onSelectLanguageById = viewModel::selectLanguageById,
                onToggleLanguage = viewModel::toggleLanguage,
                onSelectEffect = viewModel::setEffect,
                onSelectPalette = viewModel::setPalette,
                onCycleEffect = viewModel::cycleEffect,
                onPlaygroundCommit = viewModel::onPlaygroundCommit,
                onPlaygroundBackspace = viewModel::onPlaygroundBackspace,
                onPlaygroundSpace = viewModel::onPlaygroundSpace,
                onPlaygroundEnter = viewModel::onPlaygroundEnter,
                onClearPlayground = viewModel::clearPlaygroundText,
                onCopyPlayground = viewModel::copyPlaygroundToClipboard,
                onRefreshClipboard = viewModel::refreshClipboard,
                onRemoveClipboardItem = viewModel::removeClipboardItem,
                onClearClipboard = viewModel::clearClipboard,
                onKeyFeedback = viewModel::triggerKeyFeedback,
                onOpenHardwareSettings = onOpenHardwareSettings,
                onOpenLanguagesTab = onOpenLanguagesTab,
                isFeatureTourActive = isFeatureTourActive,
                featureTourCoordinates = featureTourCoordinates
            )

            StudioTab.LANGUAGES -> LanguagesScreen(
                languagePacks = languagePacks,
                onDownloadAndEnable = { langId ->
                    viewModel.downloadAndEnableLanguage(langId, activateImmediately = true)
                },
                onToggleEnabled = viewModel::setLanguageEnabled,
                onSelectLanguage = viewModel::selectLanguageById,
                onDeletePack = viewModel::deleteLanguagePack
            )

            StudioTab.EFFECTS -> EffectsScreen(
                settings = settings,
                customPhotoTheme = customPhotoTheme,
                licenseState = licenseState,
                onApplyVipToKeyboard = onOpenUnlockScreen,
                onSelectBannerPreset = viewModel::selectBannerPreset,
                onPickCustomPhoto = viewModel::saveCustomPhoto,
                onPhotoDimChange = viewModel::setCustomPhotoDim,
                onRemoveCustomPhoto = viewModel::clearCustomPhoto,
                onSelectEffect = viewModel::setEffect,
                onSelectPalette = viewModel::setPalette,
                onSelectKeycapStyle = viewModel::setKeycapStyle,
                onSpeedChange = viewModel::setAnimationSpeed,
                onGlowChange = viewModel::setGlowIntensity,
                onCornerRadiusChange = viewModel::setCornerRadius,
                onIlluminatedLegendsChange = viewModel::setIlluminatedLegends,
                onChassisAuraChange = viewModel::setChassisAura
            )

            StudioTab.HARDWARE -> HardwareScreen(
                settings = settings,
                imeStatus = imeStatus,
                languagePacks = languagePacks,
                onSelectSwitchSound = viewModel::setSwitchSound,
                onAuditionSwitch = viewModel::auditionSwitch,
                onHapticEnabledChange = viewModel::setHapticEnabled,
                onHapticStrengthChange = viewModel::setHapticStrength,
                onSelectLanguageById = viewModel::selectLanguageById,
                onOpenLanguagesTab = onOpenLanguagesTab,
                onHeightPresetChange = viewModel::setHeightPreset,
                onShowToolbarChange = viewModel::setShowToolbar,
                onAutoCapsChange = viewModel::setAutoCapitalize,
                onDoubleSpacePeriodChange = viewModel::setDoubleSpacePeriod,
                onOpenImeSettings = onOpenImeSettings,
                onShowImePicker = onShowImePicker,
                onResetDefaults = viewModel::resetDefaults
            )

            StudioTab.SETTINGS -> SettingsScreen(
                appUiLanguage = appUiLanguage,
                settings = settings,
                licenseState = licenseState,
                onSelectAppUiLanguage = viewModel::setAppUiLanguage,
                onSelectSwitchSound = viewModel::setSwitchSound,
                onHapticEnabledChange = viewModel::setHapticEnabled,
                onSmartSuggestionsChange = viewModel::setSmartSuggestionsEnabled,
                onReplayFeatureGuide = viewModel::replayFeatureTour,
                onOpenUnlockScreen = onOpenUnlockScreen
            )
        }
    }
}
