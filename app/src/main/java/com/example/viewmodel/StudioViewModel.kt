package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RgbKeyboardService
import com.example.data.ClipboardHistoryManager
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.KeyboardPreferencesRepository
import com.example.data.LanguagePackManager
import com.example.data.LicenseUiState
import com.example.data.TrialLicenseManager
import com.example.data.UpiVerificationResult
import com.example.engine.BanglaGuideScript
import com.example.engine.BanglaVoiceGuideController
import com.example.engine.MechanicalFeedbackEngine
import com.example.engine.SmartWordPredictor
import com.example.engine.VoiceGuideState
import com.example.keyboard.ShiftState
import com.example.model.AppUiLanguage
import com.example.model.ImeStatus
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardLanguage
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.LanguageCatalog
import com.example.model.LanguagePackUiState
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.SwitchSoundProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KeyboardPreferencesRepository.getInstance(application)
    private val languagePackManager = LanguagePackManager.getInstance(application)
    private val customBackgroundManager = CustomBackgroundManager.getInstance(application)
    private val trialLicenseManager = TrialLicenseManager.getInstance(application)
    private val clipboardManager = ClipboardHistoryManager.getInstance(application)
    private val feedbackEngine = MechanicalFeedbackEngine(application)
    private val voiceGuideController = BanglaVoiceGuideController(application)

    val customPhotoTheme: StateFlow<CustomPhotoTheme> = customBackgroundManager.theme
    val voiceGuideState: StateFlow<VoiceGuideState> = voiceGuideController.state
    val licenseState: StateFlow<LicenseUiState> = trialLicenseManager.licenseState

    val settings: StateFlow<KeyboardSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = KeyboardSettings()
    )

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())

    val languagePacks: StateFlow<List<LanguagePackUiState>> = combine(
        settings,
        languagePackManager.downloadedFiles,
        _downloadingIds
    ) { currentSettings, downloadedMap, downloading ->
        LanguageCatalog.allItems.map { item ->
            val downloaded = item.isBuiltIn || downloadedMap.containsKey(item.id)
            val enabled = item.isBuiltIn || (downloaded && item.id in currentSettings.enabledLanguageIds)
            LanguagePackUiState(
                catalog = item,
                isDownloaded = downloaded,
                isEnabled = enabled,
                isActive = currentSettings.activeLanguageId == item.id,
                isDownloading = item.id in downloading,
                fileSizeBytes = downloadedMap[item.id] ?: 0L
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LanguageCatalog.allItems.map { item ->
            LanguagePackUiState(
                catalog = item,
                isDownloaded = item.isBuiltIn,
                isEnabled = item.isBuiltIn,
                isActive = item.id == LanguageCatalog.ENGLISH_ID,
                isDownloading = false,
                fileSizeBytes = 0L
            )
        }
    )

    val clipboardClips: StateFlow<List<String>> = clipboardManager.clips

    private val _imeStatus = MutableStateFlow(ImeStatus())
    val imeStatus: StateFlow<ImeStatus> = _imeStatus.asStateFlow()

    private val _playgroundText = MutableStateFlow("")
    val playgroundText: StateFlow<String> = _playgroundText.asStateFlow()

    private val _playgroundShift = MutableStateFlow(ShiftState.ONCE)
    val playgroundShift: StateFlow<ShiftState> = _playgroundShift.asStateFlow()

    private val onboardingPrefs = application.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
    private val uiLanguagePrefs = application.getSharedPreferences(PREFS_UI_LANGUAGE, Context.MODE_PRIVATE)
    private var hasCompletedFeatureTour: Boolean =
        onboardingPrefs.getBoolean(KEY_HAS_COMPLETED_FEATURE_TOUR, false)

    private val _appUiLanguage = MutableStateFlow(
        AppUiLanguage.fromCode(uiLanguagePrefs.getString(KEY_APP_UI_LANGUAGE, AppUiLanguage.BENGALI.code))
    )
    val appUiLanguage: StateFlow<AppUiLanguage> = _appUiLanguage.asStateFlow()

    private val _showFeatureTour = MutableStateFlow(false)
    val showFeatureTour: StateFlow<Boolean> = _showFeatureTour.asStateFlow()

    private val _showFinalVipBanner = MutableStateFlow(false)
    val showFinalVipBanner: StateFlow<Boolean> = _showFinalVipBanner.asStateFlow()

    private var finalVipBannerJob: Job? = null

    private var hasInitializedImeStatus: Boolean = false
    private var lastShiftTapMs: Long = 0L
    private var lastSpaceTapMs: Long = 0L

    init {
        SmartWordPredictor.init(application)
        trialLicenseManager.tryAutoRestoreFromLicenseFile()
        refreshImeStatus()
        startTrialCountdownTicker()
    }

    private fun startTrialCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val state = trialLicenseManager.refreshState()
                if (state.isLifetimeUnlocked) {
                    break
                }
                delay(1_000L)
            }
        }
    }

    fun refreshLicenseState() {
        trialLicenseManager.refreshState()
    }

    fun createOfficialUpiIntent(): Pair<String, Intent> {
        return trialLicenseManager.createOfficialUpiIntent()
    }

    fun verifyUpiPaymentResult(
        resultCode: Int,
        data: Intent?,
        expectedTxnRef: String?
    ): UpiVerificationResult {
        return trialLicenseManager.verifyUpiIntentResult(
            resultCode = resultCode,
            data = data,
            expectedTxnRef = expectedTxnRef
        )
    }

    fun buildUpiQrPayload(): String {
        return trialLicenseManager.buildUpiQrPayload()
    }

    fun refreshImeStatus() {
        val context = getApplication<Application>()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val expectedComponent = ComponentName(context, RgbKeyboardService::class.java)
        val expectedId = expectedComponent.flattenToShortString()
        val expectedFullId = expectedComponent.flattenToString()

        val enabledList = runCatching { imm?.enabledInputMethodList.orEmpty() }.getOrDefault(emptyList())
        val isEnabled = enabledList.any { info ->
            info.packageName == context.packageName &&
                (info.serviceName.endsWith("RgbKeyboardService") ||
                    info.id == expectedId ||
                    info.id == expectedFullId)
        }

        val defaultIme = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        }.getOrNull().orEmpty()

        val isSelected = defaultIme == expectedId ||
            defaultIme == expectedFullId ||
            (defaultIme.startsWith(context.packageName) && defaultIme.contains("RgbKeyboardService"))

        val previous = _imeStatus.value
        val updated = ImeStatus(
            isEnabled = isEnabled,
            isSelected = isSelected
        )
        _imeStatus.value = updated

        val setupCompleted = updated.isEnabled && updated.isSelected

        if (!hasInitializedImeStatus) {
            hasInitializedImeStatus = true
            // Stage 1: Play Bengali TTS voice ONLY during the 2 initial setup steps
            if (setupCompleted) {
                voiceGuideController.stopSpeaking()
                maybeLaunchSilentFeatureTour()
            } else {
                voiceGuideController.triggerFirstLaunchWelcomeIfNeeded(isEnabled, isSelected)
            }
        } else {
            when {
                setupCompleted -> {
                    // Stage 1 complete: immediately stop all TTS voice and launch Stage 2 silent tour if pending
                    voiceGuideController.stopSpeaking()
                    maybeLaunchSilentFeatureTour()
                }
                !previous.isEnabled && updated.isEnabled && !updated.isSelected -> {
                    // Stage 1 Step 2: narrate Step 2 instruction
                    voiceGuideController.speak(BanglaGuideScript.STEP_2, forceUnmute = false)
                }
            }
        }
    }

    private fun maybeLaunchSilentFeatureTour() {
        if (!hasCompletedFeatureTour && !_showFeatureTour.value && !_showFinalVipBanner.value) {
            voiceGuideController.stopSpeaking()
            _showFeatureTour.value = true
        }
    }

    /**
     * Stage 3: Called when the final arrow step is tapped OR top-right "Skip ✕" is tapped.
     * Saves `has_completed_feature_tour = true`, shows the glowing center Lifetime VIP banner,
     * and automatically fades it out completely after 3 seconds (`delay(3000)`).
     */
    fun finishOrSkipFeatureTour() {
        voiceGuideController.stopSpeaking()
        _showFeatureTour.value = false
        if (!hasCompletedFeatureTour) {
            hasCompletedFeatureTour = true
            onboardingPrefs.edit().putBoolean(KEY_HAS_COMPLETED_FEATURE_TOUR, true).apply()
        }
        finalVipBannerJob?.cancel()
        _showFinalVipBanner.value = true
        finalVipBannerJob = viewModelScope.launch {
            delay(3_000L)
            _showFinalVipBanner.value = false
        }
    }

    /**
     * Replays Stage 2 silent bouncing-arrow interactive guide on demand from Settings.
     */
    fun replayFeatureTour() {
        voiceGuideController.stopSpeaking()
        finalVipBannerJob?.cancel()
        _showFinalVipBanner.value = false
        _showFeatureTour.value = true
    }

    fun setAppUiLanguage(language: AppUiLanguage) {
        _appUiLanguage.value = language
        uiLanguagePrefs.edit().putString(KEY_APP_UI_LANGUAGE, language.code).apply()
    }

    fun speakBanglaGuide(script: BanglaGuideScript) {
        val status = _imeStatus.value
        // Stage 1 rule: Bengali voice guide plays ONLY during initial 2-step setup
        if (status.isEnabled && status.isSelected) {
            voiceGuideController.stopSpeaking()
            return
        }
        if (script == BanglaGuideScript.WELCOME_STEP_1 || script == BanglaGuideScript.STEP_2) {
            voiceGuideController.speak(script, forceUnmute = true)
        }
    }

    fun toggleVoiceGuideStopOrMute() {
        val status = _imeStatus.value
        if (status.isEnabled && status.isSelected) {
            voiceGuideController.stopSpeaking()
            return
        }
        val recommended = if (!status.isEnabled) {
            BanglaGuideScript.WELCOME_STEP_1
        } else {
            BanglaGuideScript.STEP_2
        }
        voiceGuideController.toggleStopOrMute(recommended)
    }

    fun stopBanglaGuide() {
        voiceGuideController.stopSpeaking()
    }

    fun triggerKeyFeedback(isModifier: Boolean) {
        feedbackEngine.triggerKeyFeedback(settings.value, isModifier)
    }

    fun auditionSwitch(profile: SwitchSoundProfile) {
        val current = settings.value
        feedbackEngine.triggerSwitchAudition(
            profile = profile,
            hapticEnabled = current.hapticEnabled,
            hapticStrength = current.hapticStrength
        )
    }

    // Playground typing handlers (mirrors RgbKeyboardService behavior for live studio testing)
    fun onPlaygroundCommit(text: String) {
        _playgroundText.value += text
        lastSpaceTapMs = 0L
        if (_playgroundShift.value == ShiftState.ONCE) {
            _playgroundShift.value = ShiftState.OFF
        }
    }

    fun onPlaygroundBackspace() {
        val current = _playgroundText.value
        if (current.isNotEmpty()) {
            // Handle surrogate pairs (emojis) cleanly
            val lastCodePointIdx = current.offsetByCodePoints(current.length, -1)
            val updated = current.substring(0, lastCodePointIdx)
            _playgroundText.value = updated
            if (updated.isEmpty() && settings.value.autoCapitalize && _playgroundShift.value != ShiftState.CAPS_LOCK) {
                _playgroundShift.value = ShiftState.ONCE
            }
        }
        lastSpaceTapMs = 0L
    }

    fun onPlaygroundSpace() {
        val now = System.currentTimeMillis()
        val current = _playgroundText.value
        val activeId = settings.value.activeLanguageId
        val isEnglish = activeId == LanguageCatalog.ENGLISH_ID
        if (settings.value.doubleSpacePeriod &&
            (now - lastSpaceTapMs) in 40L..420L &&
            current.length >= 2 &&
            current.last() == ' ' &&
            !current[current.length - 2].isWhitespace()
        ) {
            val endPunct = languagePackManager.loadLayoutFromDisk(activeId)?.sentenceEndPunct ?: "."
            _playgroundText.value = current.dropLast(1) + "$endPunct "
            lastSpaceTapMs = 0L
            if (isEnglish && settings.value.autoCapitalize && _playgroundShift.value == ShiftState.OFF) {
                _playgroundShift.value = ShiftState.ONCE
            }
            return
        }
        if (settings.value.smartSuggestionsEnabled && current.isNotBlank()) {
            SmartWordPredictor.learnFromCompletedInput(getApplication(), current, activeId)
        }
        _playgroundText.value = "$current "
        lastSpaceTapMs = now
        if (isEnglish &&
            settings.value.autoCapitalize &&
            _playgroundShift.value == ShiftState.OFF &&
            (current.endsWith(".") || current.endsWith("!") || current.endsWith("?"))
        ) {
            _playgroundShift.value = ShiftState.ONCE
        }
    }

    fun onPlaygroundEnter() {
        val current = _playgroundText.value
        if (settings.value.smartSuggestionsEnabled && current.isNotBlank()) {
            SmartWordPredictor.learnFromCompletedInput(getApplication(), current, settings.value.activeLanguageId)
        }
        _playgroundText.value = "$current\n"
        lastSpaceTapMs = 0L
        if (settings.value.activeLanguageId == LanguageCatalog.ENGLISH_ID &&
            settings.value.autoCapitalize &&
            _playgroundShift.value == ShiftState.OFF
        ) {
            _playgroundShift.value = ShiftState.ONCE
        }
    }

    fun onPlaygroundShiftTap() {
        val now = System.currentTimeMillis()
        _playgroundShift.value = when (_playgroundShift.value) {
            ShiftState.OFF -> ShiftState.ONCE
            ShiftState.ONCE -> if (now - lastShiftTapMs < 380L) ShiftState.CAPS_LOCK else ShiftState.OFF
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        lastShiftTapMs = now
    }

    fun clearPlaygroundText() {
        _playgroundText.value = ""
        if (settings.value.autoCapitalize && _playgroundShift.value != ShiftState.CAPS_LOCK) {
            _playgroundShift.value = ShiftState.ONCE
        }
    }

    fun refreshClipboard() {
        clipboardManager.captureCurrentPrimaryClip()
    }

    fun copyPlaygroundToClipboard() {
        val text = _playgroundText.value.trim()
        if (text.isNotEmpty()) {
            clipboardManager.copyAndSave(text)
        }
    }

    fun removeClipboardItem(snippet: String) {
        clipboardManager.removeClip(snippet)
    }

    fun clearClipboard() {
        clipboardManager.clearAll()
    }

    // Dynamic Language Pack & Switching Actions
    fun setLanguage(language: KeyboardLanguage) {
        val id = if (language == KeyboardLanguage.BENGALI) "bn" else LanguageCatalog.ENGLISH_ID
        selectLanguageById(id)
    }

    fun selectLanguageById(langId: String) {
        if (langId != LanguageCatalog.ENGLISH_ID && !languagePackManager.isDownloaded(langId)) {
            downloadAndEnableLanguage(langId, activateImmediately = true)
            return
        }
        _playgroundShift.value = if (langId != LanguageCatalog.ENGLISH_ID) {
            ShiftState.OFF
        } else if (settings.value.autoCapitalize && _playgroundText.value.isEmpty()) {
            ShiftState.ONCE
        } else {
            ShiftState.OFF
        }
        viewModelScope.launch { repository.updateActiveLanguage(langId) }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val downloaded = languagePackManager.getDownloadedLanguageIds()
            val chosen = repository.cycleActiveLanguage(downloaded)
            _playgroundShift.value = if (chosen == LanguageCatalog.ENGLISH_ID &&
                settings.value.autoCapitalize &&
                _playgroundText.value.isEmpty()
            ) {
                ShiftState.ONCE
            } else {
                ShiftState.OFF
            }
        }
    }

    fun downloadAndEnableLanguage(langId: String, activateImmediately: Boolean = true) {
        if (langId == LanguageCatalog.ENGLISH_ID) return
        if (langId in _downloadingIds.value) return
        viewModelScope.launch {
            _downloadingIds.update { it + langId }
            val result = languagePackManager.downloadLanguagePack(langId)
            _downloadingIds.update { it - langId }
            if (result.isSuccess) {
                repository.setLanguageEnabled(langId, true)
                if (activateImmediately) {
                    repository.updateActiveLanguage(langId)
                }
            }
        }
    }

    fun setLanguageEnabled(langId: String, enabled: Boolean) {
        if (langId == LanguageCatalog.ENGLISH_ID) return
        viewModelScope.launch {
            if (enabled && !languagePackManager.isDownloaded(langId)) {
                downloadAndEnableLanguage(langId, activateImmediately = false)
            } else {
                repository.setLanguageEnabled(langId, enabled)
            }
        }
    }

    fun deleteLanguagePack(langId: String) {
        if (langId == LanguageCatalog.ENGLISH_ID) return
        viewModelScope.launch {
            languagePackManager.deleteLanguagePack(langId)
            repository.setLanguageEnabled(langId, false)
        }
    }

    fun refreshDownloadedLanguages() {
        languagePackManager.refreshDownloadedPacks()
    }

    fun setEffect(effect: RgbEffect) {
        viewModelScope.launch { repository.updateEffect(effect) }
    }

    fun setPalette(palette: RgbPalette) {
        viewModelScope.launch { repository.updatePalette(palette) }
    }

    fun cycleEffect() {
        viewModelScope.launch { repository.cycleEffect() }
    }

    fun cyclePalette() {
        viewModelScope.launch { repository.cyclePalette() }
    }

    fun setKeycapStyle(style: KeycapStyle) {
        viewModelScope.launch { repository.updateKeycapStyle(style) }
    }

    fun setSwitchSound(profile: SwitchSoundProfile) {
        viewModelScope.launch { repository.updateSwitchSound(profile) }
        auditionSwitch(profile)
    }

    fun setAnimationSpeed(speed: Float) {
        viewModelScope.launch { repository.updateAnimationSpeed(speed) }
    }

    fun setGlowIntensity(intensity: Float) {
        viewModelScope.launch { repository.updateGlowIntensity(intensity) }
    }

    fun setCornerRadius(radiusDp: Float) {
        viewModelScope.launch { repository.updateCornerRadius(radiusDp) }
    }

    fun setIlluminatedLegends(enabled: Boolean) {
        viewModelScope.launch { repository.updateIlluminatedLegends(enabled) }
    }

    fun setChassisAura(enabled: Boolean) {
        viewModelScope.launch { repository.updateChassisAura(enabled) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateHapticEnabled(enabled) }
    }

    fun setHapticStrength(strength: Float) {
        viewModelScope.launch { repository.updateHapticStrength(strength) }
    }

    fun setHeightPreset(preset: KeyboardHeightPreset) {
        viewModelScope.launch { repository.updateHeightPreset(preset) }
    }

    fun setShowNumberRow(show: Boolean) {
        viewModelScope.launch { repository.updateShowNumberRow(show) }
    }

    fun setShowToolbar(show: Boolean) {
        viewModelScope.launch { repository.updateShowToolbar(show) }
    }

    fun setAutoCapitalize(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoCapitalize(enabled) }
    }

    fun setDoubleSpacePeriod(enabled: Boolean) {
        viewModelScope.launch { repository.updateDoubleSpacePeriod(enabled) }
    }

    fun setSmartSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSmartSuggestionsEnabled(enabled) }
    }

    fun selectBannerPreset(presetId: String) {
        customBackgroundManager.selectPreset(presetId)
    }

    fun saveCustomPhoto(uri: Uri) {
        viewModelScope.launch {
            customBackgroundManager.savePhotoFromUri(uri)
        }
    }

    fun setCustomPhotoDim(dimAlpha: Float) {
        customBackgroundManager.setDimAlpha(dimAlpha)
    }

    fun clearCustomPhoto() {
        customBackgroundManager.clearCustomPhoto()
    }

    fun resetDefaults() {
        viewModelScope.launch { repository.resetDefaults() }
    }

    override fun onCleared() {
        super.onCleared()
        finalVipBannerJob?.cancel()
        feedbackEngine.release()
        voiceGuideController.release()
    }

    companion object {
        private const val PREFS_ONBOARDING = "ai_keyboard_onboarding_prefs"
        private const val KEY_HAS_COMPLETED_FEATURE_TOUR = "has_completed_feature_tour"
        private const val PREFS_UI_LANGUAGE = "ai_keyboard_ui_lang_prefs"
        private const val KEY_APP_UI_LANGUAGE = "app_ui_language"
    }
}
