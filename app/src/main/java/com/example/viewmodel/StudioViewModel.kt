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
import com.example.data.PinVerificationResult
import com.example.data.RestoreVipResult
import com.example.data.TrialLicenseManager
import com.example.data.UpiVerificationResult
import com.example.engine.BanglaGuideScript
import com.example.engine.BanglaVoiceGuideController
import com.example.engine.MechanicalFeedbackEngine
import com.example.engine.VoiceGuideState
import com.example.keyboard.ShiftState
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

    private var hasInitializedImeStatus: Boolean = false
    private var lastShiftTapMs: Long = 0L
    private var lastSpaceTapMs: Long = 0L

    init {
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

    fun savePendingMobileNumber(rawMobile: String): String {
        return trialLicenseManager.savePendingMobileNumber(rawMobile)
    }

    fun createOfficialUpiIntent(): Pair<String, Intent> {
        return trialLicenseManager.createOfficialUpiIntent()
    }

    fun verifyUpiPaymentResult(
        resultCode: Int,
        data: Intent?,
        expectedTxnRef: String?,
        mobileNumber: String
    ): UpiVerificationResult {
        val result = trialLicenseManager.verifyUpiIntentResult(
            resultCode = resultCode,
            data = data,
            expectedTxnRef = expectedTxnRef,
            rawMobileNumber = mobileNumber
        )
        if (result is UpiVerificationResult.Success) {
            voiceGuideController.speak(BanglaGuideScript.PAYMENT_UNLOCKED, forceUnmute = true)
        }
        return result
    }

    fun restoreVipByMobile(mobileNumber: String): RestoreVipResult {
        val result = trialLicenseManager.restoreVipByMobileNumber(mobileNumber)
        if (result is RestoreVipResult.Restored) {
            voiceGuideController.speak(BanglaGuideScript.PAYMENT_UNLOCKED, forceUnmute = true)
        }
        return result
    }

    fun verifyOwnerUnlockPin(pin: String): PinVerificationResult {
        val result = trialLicenseManager.verifyOwnerPin(pin)
        if (result is PinVerificationResult.Success) {
            voiceGuideController.speak(BanglaGuideScript.PAYMENT_UNLOCKED, forceUnmute = true)
        }
        return result
    }

    fun buildWhatsAppReceiptUri(): Uri {
        return trialLicenseManager.buildWhatsAppReceiptUri()
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

        if (!hasInitializedImeStatus) {
            hasInitializedImeStatus = true
            voiceGuideController.triggerFirstLaunchWelcomeIfNeeded(isEnabled, isSelected)
        } else {
            // Automatically narrate next Bengali step when user completes Step 1 or Step 2
            when {
                !previous.isSelected && updated.isSelected -> {
                    voiceGuideController.speak(BanglaGuideScript.COMPLETE, forceUnmute = false)
                }
                !previous.isEnabled && updated.isEnabled && !updated.isSelected -> {
                    voiceGuideController.speak(BanglaGuideScript.STEP_2, forceUnmute = false)
                }
            }
        }
    }

    fun speakBanglaGuide(script: BanglaGuideScript) {
        voiceGuideController.speak(script, forceUnmute = true)
    }

    fun toggleVoiceGuideStopOrMute() {
        val status = _imeStatus.value
        val recommended = when {
            !status.isEnabled -> BanglaGuideScript.WELCOME_STEP_1
            !status.isSelected -> BanglaGuideScript.STEP_2
            else -> BanglaGuideScript.COMPLETE
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
        _playgroundText.value += "\n"
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
        feedbackEngine.release()
        voiceGuideController.release()
    }
}
