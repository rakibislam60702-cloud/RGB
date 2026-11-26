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
import com.example.data.CloudAnimationConfig
import com.example.data.ComboThemePreset
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.KeyboardPreferencesRepository
import com.example.data.LanguagePackManager
import com.example.data.LicenseUiState
import com.example.data.TrialLicenseManager
import com.example.data.UpiVerificationResult
import com.example.engine.BengaliTransliterator
import com.example.engine.MechanicalFeedbackEngine
import com.example.engine.SmartWordPredictor
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
import com.example.model.TouchTypingEffect
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

    val customPhotoTheme: StateFlow<CustomPhotoTheme> = customBackgroundManager.theme
    val downloadedThemeIds: StateFlow<Set<String>> = customBackgroundManager.downloadedThemeIds
    val licenseState: StateFlow<LicenseUiState> = trialLicenseManager.licenseState

    private val _downloadingThemeIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingThemeIds: StateFlow<Set<String>> = _downloadingThemeIds.asStateFlow()

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
    val hasFreshTextClip: StateFlow<Boolean> = clipboardManager.hasFreshTextClip

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

    private val _showStartupStepper = MutableStateFlow(
        !onboardingPrefs.getBoolean(KEY_SETUP_COMPLETED, false)
    )
    val showStartupStepper: StateFlow<Boolean> = _showStartupStepper.asStateFlow()

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
    private val playgroundBanglishRawBuffer = StringBuilder()
    private var playgroundBanglishBaseText: String = ""

    init {
        SmartWordPredictor.init(application)
        viewModelScope.launch { repository.ensureSoundDefaultMigrated() }
        trialLicenseManager.syncStorageInBackground(forcePersistToDocuments = false)
        refreshImeStatus()
        startTrialCountdownTicker()
    }

    private fun startTrialCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val state = trialLicenseManager.refreshState()
                enforceBanglishVipLockIfExpired(state)
                delay(15_000L)
            }
        }
    }

    fun refreshLicenseState() {
        val state = trialLicenseManager.refreshState()
        enforceBanglishVipLockIfExpired(state)
    }

    private fun enforceBanglishVipLockIfExpired(state: LicenseUiState) {
        if (!state.isBanglishUnlocked && settings.value.activeLanguageId == LanguageCatalog.BANGLISH_ID) {
            viewModelScope.launch {
                repository.updateActiveLanguage(LanguageCatalog.ENGLISH_ID)
            }
        }
    }

    fun generateDiscountedUniqueAmount(baseAmountInr: Int): String {
        return trialLicenseManager.generateDiscountedUniqueAmount(baseAmountInr)
    }

    fun registerPendingPayment(
        planScope: String,
        baseAmountInr: Int,
        uniqueAmountFormatted: String,
        durationDays: Int
    ): String {
        return trialLicenseManager.registerPendingPayment(
            planScope = planScope,
            baseAmountInr = baseAmountInr,
            uniqueAmountFormatted = uniqueAmountFormatted,
            durationDays = durationDays
        )
    }

    fun createOfficialUpiIntent(
        planScope: String = TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
        amountInr: Int? = null,
        uniqueAmountFormatted: String? = null,
        durationDays: Int? = null
    ): Pair<String, Intent> {
        return trialLicenseManager.createOfficialUpiIntent(
            planScope = planScope,
            overrideAmountInr = amountInr,
            uniqueAmountFormatted = uniqueAmountFormatted,
            overrideDurationDays = durationDays
        )
    }

    fun verifyUpiPaymentResult(
        resultCode: Int,
        data: Intent?,
        expectedTxnRef: String?,
        planScope: String = TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
        purchasedDurationDays: Int? = null
    ): UpiVerificationResult {
        return trialLicenseManager.verifyUpiIntentResult(
            resultCode = resultCode,
            data = data,
            expectedTxnRef = expectedTxnRef,
            planScope = planScope,
            purchasedDurationDays = purchasedDurationDays
        )
    }

    fun buildUpiQrPayload(
        planScope: String = TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
        amountInr: Int? = null,
        uniqueAmountFormatted: String? = null,
        durationDays: Int? = null
    ): String {
        return trialLicenseManager.buildUpiQrPayload(
            planScope = planScope,
            overrideAmountInr = amountInr,
            uniqueAmountFormatted = uniqueAmountFormatted,
            overrideDurationDays = durationDays
        )
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

        val updated = ImeStatus(
            isEnabled = isEnabled,
            isSelected = isSelected
        )
        _imeStatus.value = updated

        // 100% SILENT onboarding walkthrough (zero voice): launches once after startup stepper is done
        if (!hasInitializedImeStatus) {
            hasInitializedImeStatus = true
            if (!_showStartupStepper.value) {
                maybeLaunchSilentFeatureTour()
            }
        }
    }

    fun completeStartupStepper() {
        _showStartupStepper.value = false
        onboardingPrefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
        refreshImeStatus()
        maybeLaunchSilentFeatureTour()
    }

    private fun maybeLaunchSilentFeatureTour() {
        if (!hasCompletedFeatureTour && !_showFeatureTour.value && !_showFinalVipBanner.value) {
            _showFeatureTour.value = true
        }
    }

    /**
     * Called when the final arrow step is tapped OR top-right "Skip ✕" is tapped.
     * Saves `has_completed_feature_tour = true`, shows the glowing center VIP banner,
     * and automatically fades it out completely after 3 seconds.
     */
    fun finishOrSkipFeatureTour() {
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
     * Replays the 100% silent bouncing-arrow interactive guide on demand.
     */
    fun replayFeatureTour() {
        finalVipBannerJob?.cancel()
        _showFinalVipBanner.value = false
        _showFeatureTour.value = true
    }

    fun setAppUiLanguage(language: AppUiLanguage) {
        _appUiLanguage.value = language
        uiLanguagePrefs.edit().putString(KEY_APP_UI_LANGUAGE, language.code).apply()
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

    private fun isBanglishVipActive(): Boolean {
        return settings.value.activeLanguageId == LanguageCatalog.BANGLISH_ID &&
            licenseState.value.isBanglishUnlocked
    }

    private fun resetPlaygroundBanglishBuffer() {
        playgroundBanglishRawBuffer.clear()
        playgroundBanglishBaseText = ""
    }

    private fun autoTransliteratePlaygroundTail(current: String): String {
        if (!isBanglishVipActive()) return current
        if (playgroundBanglishRawBuffer.isNotEmpty()) {
            val rawWord = playgroundBanglishRawBuffer.toString()
            val romanized = BengaliTransliterator.transliterateWordWithContext(
                bengaliWord = rawWord,
                textBeforeWord = playgroundBanglishBaseText
            )
            val finalized = if (romanized.isNotEmpty()) "$playgroundBanglishBaseText$romanized" else current
            resetPlaygroundBanglishBuffer()
            return finalized
        }
        val trailingBn = BengaliTransliterator.extractTrailingBengaliWord(current)
        if (trailingBn.isEmpty()) return current
        val prefix = current.dropLast(trailingBn.length)
        val romanized = BengaliTransliterator.transliterateWordWithContext(trailingBn, prefix)
        return if (romanized.isEmpty()) current else "$prefix$romanized"
    }

    // Playground typing handlers (mirrors RgbKeyboardService behavior for live studio testing)
    fun onPlaygroundCommit(text: String) {
        val current = _playgroundText.value
        if (isBanglishVipActive()) {
            when {
                BengaliTransliterator.isBengaliWordText(text) -> {
                    if (playgroundBanglishRawBuffer.isEmpty()) {
                        playgroundBanglishBaseText = current
                    }
                    playgroundBanglishRawBuffer.append(text)
                    val liveRomanized = BengaliTransliterator.transliterateWordWithContext(
                        bengaliWord = playgroundBanglishRawBuffer.toString(),
                        textBeforeWord = playgroundBanglishBaseText
                    )
                    if (liveRomanized.isNotEmpty()) {
                        _playgroundText.value = playgroundBanglishBaseText + liveRomanized
                    }
                }
                BengaliTransliterator.isBengaliDigitText(text) -> {
                    val flushed = autoTransliteratePlaygroundTail(current)
                    _playgroundText.value = flushed + BengaliTransliterator.convertBengaliDigits(text)
                }
                BengaliTransliterator.isAutoTransliterateTriggerPunctuation(text) -> {
                    val normalizedPunct = BengaliTransliterator.normalizeBanglishPunctuation(text)
                    val converted = autoTransliteratePlaygroundTail(current)
                    if (settings.value.smartSuggestionsEnabled && converted.isNotBlank()) {
                        SmartWordPredictor.learnFromCompletedInput(getApplication(), converted, LanguageCatalog.BANGLISH_ID)
                    }
                    _playgroundText.value = converted + normalizedPunct
                }
                else -> {
                    val flushed = autoTransliteratePlaygroundTail(current)
                    val cleanText = if (BengaliTransliterator.containsBengaliScript(text)) {
                        BengaliTransliterator.transliterate(
                            text,
                            capitalizeSentences = BengaliTransliterator.isSentenceStartContext(flushed)
                        )
                    } else {
                        text
                    }
                    _playgroundText.value = flushed + cleanText
                }
            }
        } else {
            resetPlaygroundBanglishBuffer()
            _playgroundText.value = current + text
        }
        lastSpaceTapMs = 0L
        if (_playgroundShift.value == ShiftState.ONCE) {
            _playgroundShift.value = ShiftState.OFF
        }
    }

    fun onPlaygroundApplySuggestion(suggestion: String, replaceTokenLength: Int) {
        val current = _playgroundText.value
        val updated = if (playgroundBanglishRawBuffer.isNotEmpty()) {
            val base = playgroundBanglishBaseText
            resetPlaygroundBanglishBuffer()
            "$base$suggestion "
        } else {
            val trimmed = if (replaceTokenLength > 0 && current.length >= replaceTokenLength) {
                current.dropLast(replaceTokenLength)
            } else {
                current
            }
            "$trimmed$suggestion "
        }
        _playgroundText.value = updated
        if (settings.value.smartSuggestionsEnabled && updated.isNotBlank()) {
            SmartWordPredictor.learnFromCompletedInput(getApplication(), updated.trimEnd(), settings.value.activeLanguageId)
        }
        lastSpaceTapMs = 0L
    }

    fun onPlaygroundBackspace() {
        if (isBanglishVipActive() && playgroundBanglishRawBuffer.isNotEmpty()) {
            playgroundBanglishRawBuffer.deleteCharAt(playgroundBanglishRawBuffer.length - 1)
            if (playgroundBanglishRawBuffer.isNotEmpty()) {
                val liveRomanized = BengaliTransliterator.transliterateWordWithContext(
                    bengaliWord = playgroundBanglishRawBuffer.toString(),
                    textBeforeWord = playgroundBanglishBaseText
                )
                _playgroundText.value = playgroundBanglishBaseText + liveRomanized
            } else {
                _playgroundText.value = playgroundBanglishBaseText
                resetPlaygroundBanglishBuffer()
            }
            lastSpaceTapMs = 0L
            return
        }
        resetPlaygroundBanglishBuffer()
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

    fun onPlaygroundDeleteWord() {
        if (isBanglishVipActive() && playgroundBanglishRawBuffer.isNotEmpty()) {
            _playgroundText.value = playgroundBanglishBaseText
            resetPlaygroundBanglishBuffer()
            lastSpaceTapMs = 0L
            return
        }
        resetPlaygroundBanglishBuffer()
        val current = _playgroundText.value
        if (current.isEmpty()) return
        var idx = current.length
        while (idx > 0 && current[idx - 1].isWhitespace()) {
            idx--
        }
        while (idx > 0 && !current[idx - 1].isWhitespace()) {
            idx--
        }
        val updated = current.substring(0, idx)
        _playgroundText.value = updated
        if (updated.isEmpty() && settings.value.autoCapitalize && _playgroundShift.value != ShiftState.CAPS_LOCK) {
            _playgroundShift.value = ShiftState.ONCE
        }
        lastSpaceTapMs = 0L
    }

    fun onPlaygroundSpace() {
        val now = System.currentTimeMillis()
        val current = _playgroundText.value
        val activeId = settings.value.activeLanguageId
        val isLatinBuiltIn = activeId == LanguageCatalog.ENGLISH_ID
        if (settings.value.doubleSpacePeriod &&
            (now - lastSpaceTapMs) in 40L..420L &&
            current.length >= 2 &&
            current.last() == ' ' &&
            !current[current.length - 2].isWhitespace()
        ) {
            val endPunct = if (activeId == LanguageCatalog.BANGLISH_ID) {
                "."
            } else {
                languagePackManager.loadLayoutFromDisk(activeId)?.sentenceEndPunct ?: "."
            }
            _playgroundText.value = current.dropLast(1) + "$endPunct "
            lastSpaceTapMs = 0L
            if (isLatinBuiltIn && settings.value.autoCapitalize && _playgroundShift.value == ShiftState.OFF) {
                _playgroundShift.value = ShiftState.ONCE
            }
            return
        }
        val converted = autoTransliteratePlaygroundTail(current)
        if (settings.value.smartSuggestionsEnabled && converted.isNotBlank()) {
            SmartWordPredictor.learnFromCompletedInput(getApplication(), converted, activeId)
        }
        _playgroundText.value = "$converted "
        lastSpaceTapMs = now
        if (isLatinBuiltIn &&
            settings.value.autoCapitalize &&
            _playgroundShift.value == ShiftState.OFF &&
            (converted.endsWith(".") || converted.endsWith("!") || converted.endsWith("?"))
        ) {
            _playgroundShift.value = ShiftState.ONCE
        }
    }

    fun onPlaygroundEnter() {
        val current = autoTransliteratePlaygroundTail(_playgroundText.value)
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
        resetPlaygroundBanglishBuffer()
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

    fun markClipboardPasted(text: String) {
        clipboardManager.markClipboardPasted(text)
    }

    // Dynamic Language Pack & Switching Actions
    fun setLanguage(language: KeyboardLanguage) {
        val id = if (language == KeyboardLanguage.BENGALI) "bn" else LanguageCatalog.ENGLISH_ID
        selectLanguageById(id)
    }

    fun selectLanguageById(langId: String): Boolean {
        val isBanglishAllowed = trialLicenseManager.refreshState().isBanglishUnlocked
        if (langId == LanguageCatalog.BANGLISH_ID && !isBanglishAllowed) {
            enforceBanglishVipLockIfExpired(licenseState.value)
            return false
        }
        if (!LanguageCatalog.isBuiltInId(langId) && !languagePackManager.isDownloaded(langId)) {
            downloadAndEnableLanguage(langId, activateImmediately = true)
            return true
        }
        _playgroundShift.value = if (langId != LanguageCatalog.ENGLISH_ID) {
            ShiftState.OFF
        } else if (settings.value.autoCapitalize && _playgroundText.value.isEmpty()) {
            ShiftState.ONCE
        } else {
            ShiftState.OFF
        }
        viewModelScope.launch { repository.updateActiveLanguage(langId) }
        return true
    }

    fun toggleLanguage(onVipLocked: () -> Unit = {}) {
        val isBanglishAllowed = trialLicenseManager.refreshState().isBanglishUnlocked
        viewModelScope.launch {
            val downloaded = languagePackManager.getDownloadedLanguageIds()
            val fullCycle = settings.value.enabledLanguageIds
                .filter { LanguageCatalog.isBuiltInId(it) || it in downloaded }
                .ifEmpty { listOf(LanguageCatalog.ENGLISH_ID) }
            val currentIdx = fullCycle.indexOf(settings.value.activeLanguageId)
            val candidateNext = fullCycle[(currentIdx + 1) % fullCycle.size]

            val chosen = if (candidateNext == LanguageCatalog.BANGLISH_ID && !isBanglishAllowed) {
                onVipLocked()
                repository.cycleActiveLanguage(downloaded, includeVipLanguages = false)
            } else {
                repository.cycleActiveLanguage(downloaded, includeVipLanguages = isBanglishAllowed)
            }
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
        if (LanguageCatalog.isBuiltInId(langId)) return
        if (langId in _downloadingIds.value) return
        viewModelScope.launch {
            _downloadingIds.update { it + langId }
            val result = languagePackManager.downloadLanguagePack(langId)
            _downloadingIds.update { it - langId }
            if (result.isSuccess) {
                repository.setLanguageEnabled(langId, true)
                val canActivateNow = activateImmediately &&
                    (langId != LanguageCatalog.BANGLISH_ID || trialLicenseManager.refreshState().isBanglishUnlocked)
                if (canActivateNow) {
                    repository.updateActiveLanguage(langId)
                }
            }
        }
    }

    fun setLanguageEnabled(langId: String, enabled: Boolean) {
        if (LanguageCatalog.isBuiltInId(langId)) return
        viewModelScope.launch {
            if (enabled && !languagePackManager.isDownloaded(langId)) {
                downloadAndEnableLanguage(langId, activateImmediately = false)
            } else {
                repository.setLanguageEnabled(langId, enabled)
            }
        }
    }

    fun deleteLanguagePack(langId: String) {
        if (LanguageCatalog.isBuiltInId(langId)) return
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

    fun setTouchEffect(touchEffect: TouchTypingEffect) {
        viewModelScope.launch { repository.updateTouchEffect(touchEffect) }
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

    fun setShowKeyBorders(show: Boolean) {
        viewModelScope.launch { repository.updateShowKeyBorders(show) }
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

    /**
     * On-demand download & apply for any of the 52 Combos or remote Cloud Themes (`filesDir/themes/`).
     * Clean Isolation Rule: keeps Package Themes crisp without turning on Touch Effects unless the user enables one!
     */
    fun downloadComboTheme(combo: ComboThemePreset, applyAfterDownload: Boolean = true) {
        if (applyAfterDownload) {
            if (!combo.bannerPresetId.isNullOrBlank() && combo.remoteImageUrl.isNullOrBlank()) {
                customBackgroundManager.selectPreset(combo.bannerPresetId)
            }
            viewModelScope.launch {
                repository.updateEffect(combo.effect)
                repository.updateTouchEffect(TouchTypingEffect.NONE)
                repository.updatePalette(combo.palette)
            }
        }
        if (combo.id in _downloadingThemeIds.value) return
        viewModelScope.launch {
            _downloadingThemeIds.update { it + combo.id }
            val result = customBackgroundManager.downloadComboTheme(combo)
            _downloadingThemeIds.update { it - combo.id }
            if (result.isSuccess && applyAfterDownload) {
                customBackgroundManager.selectDownloadedCombo(combo.id)
                repository.updateEffect(combo.effect)
                repository.updateTouchEffect(TouchTypingEffect.NONE)
                repository.updatePalette(combo.palette)
            }
        }
    }

    /** 1-Tap Combo apply: activates live immediately and caches `filesDir/themes/` artwork cleanly. */
    fun applyComboTheme(combo: ComboThemePreset) {
        if (customBackgroundManager.selectDownloadedCombo(combo.id)) {
            viewModelScope.launch {
                repository.updateEffect(combo.effect)
                repository.updateTouchEffect(TouchTypingEffect.NONE)
                repository.updatePalette(combo.palette)
            }
        } else {
            downloadComboTheme(combo, applyAfterDownload = true)
        }
    }

    /**
     * Dual-layer Ambient Physics Animation selector:
     * - Selecting Classic Black (`NORMAL_DEFAULT`) clears any background photo for clean Classic Black.
     * - Selecting any of the 20 Ambient Physics Animations layers it cleanly while preserving the active Touch Effect!
     */
    fun applyAnimationOnly(effect: RgbEffect) {
        if (effect == RgbEffect.NORMAL_DEFAULT) {
            customBackgroundManager.clearCustomPhoto()
        }
        viewModelScope.launch {
            repository.updateEffect(effect)
        }
    }

    /** Applies a remote Cloud Animation synced from Firebase `/admin_config/cloudAnimations`. */
    fun applyCloudAnimation(anim: CloudAnimationConfig) {
        val mappedEffect = RgbEffect.fromSavedName(anim.baseEffectId)
        val mappedPalette = RgbPalette.entries.find { it.name.equals(anim.paletteName, ignoreCase = true) }
            ?: settings.value.palette
        viewModelScope.launch {
            repository.updateEffect(mappedEffect)
            repository.updatePalette(mappedPalette)
        }
    }

    /** Static HD Photo Banner — applies static banner cleanly without unwanted overlapping touch effects. */
    fun applyStaticBannerOnly(presetId: String) {
        customBackgroundManager.selectPreset(presetId)
        viewModelScope.launch {
            repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
            repository.updateTouchEffect(TouchTypingEffect.NONE)
        }
    }

    /** Switches Typing Effect backdrop to pure Default Black cleanly while ensuring an active Touch Response is ready. */
    fun applyTypingEffectDefaultBlack() {
        customBackgroundManager.clearCustomPhoto()
        viewModelScope.launch {
            repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
            if (settings.value.touchEffect == TouchTypingEffect.NONE) {
                repository.updateTouchEffect(TouchTypingEffect.AI_NEURAL_PULSE)
            }
        }
    }

    // --- Side-by-Side Independent Layer Controls (Left Photos / Right Animations) ---

    /** Sets Default Black backdrop ONLY, preserving any active Touch Response or Ambient Animation. */
    fun applyDefaultBlackBackdropPreservingTouch() {
        customBackgroundManager.clearCustomPhoto()
        viewModelScope.launch {
            if (settings.value.effect != RgbEffect.NORMAL_DEFAULT) {
                repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
            }
        }
    }

    /** Sets HD Banner Photo backdrop ONLY, preserving the user's active Touch Response. */
    fun applyBannerPhotoPreservingTouch(presetId: String) {
        customBackgroundManager.selectPreset(presetId)
        viewModelScope.launch {
            if (settings.value.effect != RgbEffect.NORMAL_DEFAULT) {
                repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
            }
        }
    }

    /** Downloads & applies a Theme Pack artwork as background photo ONLY, preserving active Touch Response. */
    fun applyComboPhotoPreservingTouch(combo: ComboThemePreset) {
        if (customBackgroundManager.selectDownloadedCombo(combo.id)) {
            viewModelScope.launch {
                repository.updatePalette(combo.palette)
                if (settings.value.effect != RgbEffect.NORMAL_DEFAULT) {
                    repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
                }
            }
            return
        }
        if (combo.id in _downloadingThemeIds.value) return
        viewModelScope.launch {
            _downloadingThemeIds.update { it + combo.id }
            val result = customBackgroundManager.downloadComboTheme(combo)
            _downloadingThemeIds.update { it - combo.id }
            if (result.isSuccess) {
                customBackgroundManager.selectDownloadedCombo(combo.id)
                repository.updatePalette(combo.palette)
                if (settings.value.effect != RgbEffect.NORMAL_DEFAULT) {
                    repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
                }
            }
        }
    }

    /** Turns OFF active Touch Response & Ambient RGB animations while preserving the active background photo. */
    fun clearAnimationsPreservingBackdrop() {
        viewModelScope.launch {
            repository.updateTouchEffect(TouchTypingEffect.NONE)
            repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
        }
    }

    /** Atomic 1-tap Apply from the Full Keyboard Theme Customizer Sheet (supports Key Borders toggle). */
    fun applyThemeCustomizerSelection(
        bannerPresetId: String?,
        combo: ComboThemePreset?,
        effect: RgbEffect,
        touchEffect: TouchTypingEffect,
        palette: RgbPalette,
        showKeyBorders: Boolean,
        clearPhotoToBlack: Boolean
    ) {
        viewModelScope.launch {
            repository.updateShowKeyBorders(showKeyBorders)
            repository.updateTouchEffect(touchEffect)
            repository.updatePalette(palette)
            when {
                combo != null -> {
                    if (customBackgroundManager.selectDownloadedCombo(combo.id)) {
                        repository.updateEffect(combo.effect)
                    } else {
                        downloadComboTheme(combo, applyAfterDownload = true)
                    }
                }
                !bannerPresetId.isNullOrBlank() -> {
                    customBackgroundManager.selectPreset(bannerPresetId)
                    repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
                }
                clearPhotoToBlack -> {
                    customBackgroundManager.clearCustomPhoto()
                    repository.updateEffect(effect)
                }
                else -> {
                    repository.updateEffect(effect)
                }
            }
        }
    }

    fun saveCustomPhoto(uri: Uri) {
        viewModelScope.launch {
            customBackgroundManager.savePhotoFromUri(uri)
        }
    }

    fun saveCroppedPhotoBitmap(bitmap: android.graphics.Bitmap, dimAlpha: Float, showKeyBorders: Boolean) {
        viewModelScope.launch {
            customBackgroundManager.saveCroppedPhotoBitmap(bitmap, dimAlpha)
            repository.updateShowKeyBorders(showKeyBorders)
            if (settings.value.effect != RgbEffect.NORMAL_DEFAULT) {
                repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
            }
        }
    }

    fun applyBannerWithDim(presetId: String, dimAlpha: Float, showKeyBorders: Boolean) {
        customBackgroundManager.selectPresetWithDim(presetId, dimAlpha)
        viewModelScope.launch {
            repository.updateShowKeyBorders(showKeyBorders)
            if (settings.value.effect != RgbEffect.NORMAL_DEFAULT) {
                repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
            }
        }
    }

    fun applyRemoteBannerWithDim(imageSource: String, dimAlpha: Float, showKeyBorders: Boolean) {
        viewModelScope.launch {
            customBackgroundManager.saveRemoteBannerImage(imageSource, dimAlpha)
            repository.updateShowKeyBorders(showKeyBorders)
            if (settings.value.effect != RgbEffect.NORMAL_DEFAULT) {
                repository.updateEffect(RgbEffect.NORMAL_DEFAULT)
            }
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
    }

    companion object {
        private const val PREFS_ONBOARDING = "ai_keyboard_onboarding_prefs"
        private const val KEY_HAS_COMPLETED_FEATURE_TOUR = "has_completed_feature_tour"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val PREFS_UI_LANGUAGE = "ai_keyboard_ui_lang_prefs"
        private const val KEY_APP_UI_LANGUAGE = "app_ui_language"
    }
}
