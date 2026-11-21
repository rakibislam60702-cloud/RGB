package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Keyboard
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.LanguagePackUiState
import com.example.ui.screens.EffectsScreen
import com.example.ui.screens.HardwareScreen
import com.example.ui.screens.LanguagesScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanContainer
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ProRgbKeyboardTheme
import com.example.ui.theme.TextMuted
import com.example.viewmodel.StudioViewModel

private enum class StudioTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    STUDIO(
        labelRes = R.string.nav_studio,
        selectedIcon = Icons.Filled.Keyboard,
        unselectedIcon = Icons.Outlined.Keyboard,
        testTag = "nav_tab_studio"
    ),
    LANGUAGES(
        labelRes = R.string.nav_languages,
        selectedIcon = Icons.Filled.Translate,
        unselectedIcon = Icons.Outlined.Translate,
        testTag = "nav_tab_languages"
    ),
    EFFECTS(
        labelRes = R.string.nav_effects,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_effects"
    ),
    HARDWARE(
        labelRes = R.string.nav_feel,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_hardware"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            ProRgbKeyboardTheme {
                ProRgbKeyboardApp()
            }
        }
    }
}

@Composable
fun ProRgbKeyboardApp(
    viewModel: StudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
    val languagePacks by viewModel.languagePacks.collectAsStateWithLifecycle()
    val playgroundText by viewModel.playgroundText.collectAsStateWithLifecycle()
    val clipboardClips by viewModel.clipboardClips.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(StudioTab.STUDIO) }

    BackHandler(enabled = currentTab != StudioTab.STUDIO) {
        currentTab = StudioTab.STUDIO
    }

    // Automatically re-check Android IME enabled & default status, languages, and clipboard on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshImeStatus()
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
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
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelSmall
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
                        settings = settings,
                        imeStatus = imeStatus,
                        languagePacks = languagePacks,
                        playgroundText = playgroundText,
                        clipboardClips = clipboardClips,
                        viewModel = viewModel,
                        onOpenImeSettings = openImeSettings,
                        onShowImePicker = showImePicker,
                        onOpenHardwareSettings = { currentTab = StudioTab.HARDWARE },
                        onOpenLanguagesTab = { currentTab = StudioTab.LANGUAGES }
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ObsidianCanvas,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
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
                        settings = settings,
                        imeStatus = imeStatus,
                        languagePacks = languagePacks,
                        playgroundText = playgroundText,
                        clipboardClips = clipboardClips,
                        viewModel = viewModel,
                        onOpenImeSettings = openImeSettings,
                        onShowImePicker = showImePicker,
                        onOpenHardwareSettings = { currentTab = StudioTab.HARDWARE },
                        onOpenLanguagesTab = { currentTab = StudioTab.LANGUAGES }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    currentTab: StudioTab,
    settings: com.example.model.KeyboardSettings,
    imeStatus: com.example.model.ImeStatus,
    languagePacks: List<LanguagePackUiState>,
    playgroundText: String,
    clipboardClips: List<String>,
    viewModel: StudioViewModel,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenHardwareSettings: () -> Unit,
    onOpenLanguagesTab: () -> Unit
) {
    Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
        when (tab) {
            StudioTab.STUDIO -> StudioScreen(
                settings = settings,
                imeStatus = imeStatus,
                languagePacks = languagePacks,
                playgroundText = playgroundText,
                clipboardClips = clipboardClips,
                onOpenImeSettings = onOpenImeSettings,
                onShowImePicker = onShowImePicker,
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
                onOpenLanguagesTab = onOpenLanguagesTab
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
        }
    }
}
