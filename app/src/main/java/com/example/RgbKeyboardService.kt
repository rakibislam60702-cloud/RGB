package com.example

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.example.data.ClipboardHistoryManager
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.KeyboardPreferencesRepository
import com.example.data.LanguagePackManager
import com.example.data.LicenseUiState
import com.example.data.ScreenshotClip
import com.example.data.TrialLicenseManager
import com.example.engine.BengaliTransliterator
import com.example.engine.GeneratedShayariResult
import com.example.engine.MechanicalFeedbackEngine
import com.example.engine.OnlineVoiceTypingController
import com.example.engine.RgbStickerGenerator
import com.example.engine.RgbStickerStyle
import com.example.engine.ShayariCategory
import com.example.engine.ShayariVault
import com.example.engine.SmartWordPredictor
import com.example.engine.StylishFontEngine
import com.example.engine.StylishFontStyle
import com.example.model.EmojiCatalog
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardSettings
import com.example.model.LanguageCatalog
import com.example.model.RgbEffect
import com.example.model.TouchTypingEffect
import com.example.rgbkeyboard.RgbKeyboardView
import com.example.security.AntiTamperGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full Android InputMethodService powering Rakib official (Pro RGB Keyboard) system-wide.
 * Supports:
 * - Dynamic JSON language layouts from `filesDir/layouts/` + Gboard 7-icon toolbar + Touch Spark RGB
 * - Persistent custom photo background theme synced live from SharedPreferences & `filesDir/custom_bg.jpg`
 * - Online Bengali (`bn-BD` / `bn-IN`) & English (`en-US`) SpeechRecognizer voice typing
 * - Unlimited 9-category scrollable Emoji GridView with hold-to-repeat Backspace
 */
class RgbKeyboardService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val uiHandler = Handler(Looper.getMainLooper())

    private lateinit var preferencesRepository: KeyboardPreferencesRepository
    private lateinit var languagePackManager: LanguagePackManager
    private lateinit var customBackgroundManager: CustomBackgroundManager
    private lateinit var trialLicenseManager: TrialLicenseManager
    private lateinit var clipboardManager: ClipboardHistoryManager
    private lateinit var feedbackEngine: MechanicalFeedbackEngine
    private lateinit var voiceController: OnlineVoiceTypingController

    private var callBannerBar: View? = null
    private var tvCallStatus: TextView? = null
    private var btnCallReceive: TextView? = null
    private var btnCallEnd: TextView? = null
    private var currentCallState: Int = TelephonyManager.CALL_STATE_IDLE
    private var callOffhookStartMs: Long = 0L
    private var callListenerRegistered: Boolean = false
    private var telephonyCallbackObj: Any? = null
    @Suppress("DEPRECATION")
    private var legacyPhoneStateListener: PhoneStateListener? = null

    private var topBar: View? = null
    private var tvBrandSignature: TextView? = null
    private var voiceBar: View? = null
    private var tvVoiceLangBadge: TextView? = null
    private var tvVoiceStatus: TextView? = null
    private var btnVoiceStop: Button? = null
    private var aiTopicTypingBar: View? = null
    private var tvAiTopicDraft: TextView? = null
    private var btnAiTopicGenerateNow: TextView? = null
    private var btnAiTopicCancel: TextView? = null
    private var suggestionBar: HorizontalScrollView? = null
    private var suggestionContainer: LinearLayout? = null

    private var keyboardView: RgbKeyboardView? = null
    private var trialLockPanel: View? = null
    private var tvImeLockDeviceId: TextView? = null
    private var clipboardPanel: View? = null
    private var clipboardContainer: LinearLayout? = null
    private var tvClipboardEmpty: TextView? = null
    private var stylishFontPanel: View? = null
    private var stylishFontContainer: LinearLayout? = null
    private var aiHubPanel: View? = null
    private var tvAiTopicBox: TextView? = null
    private var btnAiGenerate: TextView? = null
    private var btnAiHubClose: TextView? = null
    private var aiCategoryTabs: LinearLayout? = null
    private var aiQuickTopicsContainer: LinearLayout? = null
    private var aiShayariScroll: ScrollView? = null
    private var aiShayariContainer: LinearLayout? = null

    private var stickerPanel: View? = null
    private var tvStickerPhrasePreview: TextView? = null
    private var stickerStyleGridContainer: LinearLayout? = null
    private var stickerPulseAnimator: ValueAnimator? = null
    private val stickerCardViews = mutableListOf<View>()
    private var pendingStickerPhrase: String = ""

    private var emojiPanel: View? = null
    private var emojiCategoryTabs: LinearLayout? = null
    private var emojiGridView: GridView? = null
    private var tvEmojiCategoryTitle: TextView? = null
    private var btnEmojiClose: Button? = null
    private var btnEmojiBackspace: Button? = null
    private var emojiGridAdapter: EmojiGridAdapter? = null
    private var selectedEmojiCategoryIndex: Int = 0
    private var isEmojiBackspaceRepeating: Boolean = false

    private var btnEmoji: TextView? = null
    private var btnClipboard: TextView? = null
    private var btnStylishFonts: TextView? = null
    private var btnMic: ImageButton? = null
    private var btnKeyboardSettings: TextView? = null
    private var btnAiHub: TextView? = null
    private var isTypingWindowVisible: Boolean = false
    private var lastKeyboardCallAcceptMs: Long = 0L

    private var currentSettings: KeyboardSettings = KeyboardSettings()
    private var currentPhotoTheme: CustomPhotoTheme = CustomPhotoTheme()
    private var currentLicenseState: LicenseUiState = LicenseUiState()
    private var activeOverlay: OverlayMode = OverlayMode.NONE
    private var activeStylishFont: StylishFontStyle = StylishFontStyle.NORMAL
    private var selectedAiCategory: ShayariCategory = ShayariCategory.ROMANTIC
    private var generatedAiResult: GeneratedShayariResult? = null
    private var expandedShayariIndex: Int? = null
    private var aiTopicDraft: String = ""
    private var isAiTopicTypingMode: Boolean = false
    private var aiGenerationSeed: Int = 0

    private var lastSpaceTapTimeMs: Long = 0L
    private var emojiBackspaceHoldStartMs: Long = 0L
    private var micLockTriggeredDuringPress: Boolean = false
    private var micWasListeningOnDown: Boolean = false
    private var latestVoiceState = OnlineVoiceTypingController.VoiceListeningState()
    private var hasActiveVoiceComposing: Boolean = false
    private val banglishRawBengaliBuffer = StringBuilder()
    private var banglishWordPrefixContext: String = ""
    private var isGeneratingSticker: Boolean = false
    private var bengaliTypeface: Typeface? = null

    // Zero-lag typing caches & background jobs
    private var lastVipSyncMs: Long = 0L
    private var suggestionJob: Job? = null
    private var lastRenderedSuggestions: List<String> = emptyList()
    private var lastRenderedStickerPhrase: String = ""
    private var lastRenderedVipState: Boolean? = null
    private var lastRenderedSmartEnabled: Boolean? = null
    private var lastRenderedBanglaChipLabel: String = ""
    private var lastRenderedLangId: String = ""
    private var lastRenderedScreenshotUri: String = ""
    private var lastRenderedFreshPasteClip: String = ""
    private var lastRenderedSuggestedEmojis: List<String> = emptyList()

    private enum class OverlayMode { NONE, CLIPBOARD, FONTS, EMOJI, STICKER_SELECTOR, AI_HUB }

    private val emojiBackspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (!isEmojiBackspaceRepeating) return
            val heldMs = SystemClock.uptimeMillis() - emojiBackspaceHoldStartMs
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            if (heldMs >= BACKSPACE_WORD_ACCEL_MS) {
                handleDeletePreviousWord()
                uiHandler.postDelayed(this, BACKSPACE_WORD_INTERVAL_MS)
            } else {
                handleBackspace()
                uiHandler.postDelayed(this, BACKSPACE_CHAR_INTERVAL_MS)
            }
        }
    }

    private val micHoldLockRunnable = Runnable {
        if (!micWasListeningOnDown) {
            micLockTriggeredDuringPress = true
            btnMic?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            startVoiceTyping(lockedContinuous = true)
        }
    }

    private val callTimerRunnable = object : Runnable {
        override fun run() {
            if (currentCallState == TelephonyManager.CALL_STATE_OFFHOOK &&
                isInputViewShown &&
                currentLicenseState.isCallBarUnlocked
            ) {
                val elapsedSec = ((SystemClock.elapsedRealtime() - callOffhookStartMs) / 1000L).coerceAtLeast(0L)
                val mins = elapsedSec / 60L
                val secs = elapsedSec % 60L
                tvCallStatus?.text = String.format(java.util.Locale.US, "📞 Active Call • %02d:%02d", mins, secs)
                uiHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AntiTamperGuard.verifyOrTerminate(this)
        preferencesRepository = KeyboardPreferencesRepository.getInstance(applicationContext)
        languagePackManager = LanguagePackManager.getInstance(applicationContext)
        customBackgroundManager = CustomBackgroundManager.getInstance(applicationContext)
        trialLicenseManager = TrialLicenseManager.getInstance(applicationContext)
        clipboardManager = ClipboardHistoryManager.getInstance(applicationContext)
        feedbackEngine = MechanicalFeedbackEngine(applicationContext)
        currentSettings = preferencesRepository.getCachedSettingsSnapshot()
        currentPhotoTheme = customBackgroundManager.reloadFromPrefs()
        currentLicenseState = trialLicenseManager.syncNowOnLaunchOrServiceStart()

        SmartWordPredictor.init(applicationContext)
        bengaliTypeface = runCatching {
            ResourcesCompat.getFont(applicationContext, R.font.noto_sans_bengali)
        }.getOrNull()

        voiceController = OnlineVoiceTypingController(
            context = this,
            onStateChanged = { state ->
                latestVoiceState = state
                uiHandler.post { renderVoiceState(state) }
            },
            onPartialText = { partial ->
                uiHandler.post {
                    if (partial.isNotBlank()) {
                        clearBanglishBuffer(finishComposing = false)
                        val cleanPartial = if (currentSettings.activeLanguageId == LanguageCatalog.BANGLISH_ID &&
                            BengaliTransliterator.containsBengaliScript(partial)
                        ) {
                            BengaliTransliterator.transliterate(partial, capitalizeSentences = true)
                        } else {
                            partial
                        }
                        withSafeInputConnection { ic ->
                            hasActiveVoiceComposing = true
                            ic.setComposingText(cleanPartial, 1)
                        }
                        refreshSuggestions()
                    }
                }
            },
            onFinalText = { spokenText ->
                uiHandler.post {
                    hasActiveVoiceComposing = false
                    clearBanglishBuffer(finishComposing = false)
                    val langId = currentSettings.activeLanguageId
                    val cleanSpoken = if (langId == LanguageCatalog.BANGLISH_ID &&
                        BengaliTransliterator.containsBengaliScript(spokenText)
                    ) {
                        BengaliTransliterator.transliterate(spokenText, capitalizeSentences = true)
                    } else {
                        spokenText
                    }
                    serviceScope.launch(Dispatchers.Default) {
                        SmartWordPredictor.learnFromCompletedInput(
                            applicationContext,
                            cleanSpoken,
                            langId
                        )
                    }
                    handleCommitText("$cleanSpoken ")
                }
            }
        )

        serviceScope.launch {
            preferencesRepository.ensureSoundDefaultMigrated()
        }

        serviceScope.launch {
            preferencesRepository.settingsFlow.collect { latest ->
                currentSettings = latest
                applySettingsToViews(latest)
            }
        }

        serviceScope.launch {
            languagePackManager.downloadedFiles.collect {
                applySettingsToViews(currentSettings)
            }
        }

        serviceScope.launch {
            customBackgroundManager.theme.collect { photoTheme ->
                currentPhotoTheme = photoTheme
                keyboardView?.updatePhotoTheme(photoTheme)
            }
        }

        serviceScope.launch {
            trialLicenseManager.licenseState.collect { state ->
                currentLicenseState = state
                applyTrialLockState()
            }
        }

        serviceScope.launch {
            clipboardManager.clips.collect { snippets ->
                renderClipboardSnippets(snippets)
            }
        }

        serviceScope.launch {
            clipboardManager.hasFreshTextClip.collect {
                refreshSuggestions(immediate = true)
            }
        }

        serviceScope.launch {
            clipboardManager.latestScreenshotClip.collect {
                refreshSuggestions(immediate = true)
            }
        }

        registerCallStateListenerIfPermitted()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)

        callBannerBar = root.findViewById(R.id.callBannerBar)
        tvCallStatus = root.findViewById(R.id.tvCallStatus)
        btnCallReceive = root.findViewById(R.id.btnCallReceive)
        btnCallEnd = root.findViewById(R.id.btnCallEnd)
        styleCallBannerBar()
        btnCallReceive?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            answerIncomingCall()
        }
        btnCallEnd?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            endActiveCall()
        }

        topBar = root.findViewById(R.id.topBar)
        tvBrandSignature = root.findViewById(R.id.tvBrandSignature)
        styleBrandSignatureBadge(tvBrandSignature)
        voiceBar = root.findViewById(R.id.voiceBar)
        tvVoiceLangBadge = root.findViewById(R.id.tvVoiceLangBadge)
        tvVoiceStatus = root.findViewById(R.id.tvVoiceStatus)
        btnVoiceStop = root.findViewById(R.id.btnVoiceStop)
        aiTopicTypingBar = root.findViewById(R.id.aiTopicTypingBar)
        tvAiTopicDraft = root.findViewById(R.id.tvAiTopicDraft)
        btnAiTopicGenerateNow = root.findViewById(R.id.btnAiTopicGenerateNow)
        btnAiTopicCancel = root.findViewById(R.id.btnAiTopicCancel)
        suggestionBar = root.findViewById(R.id.suggestionBar)
        suggestionContainer = root.findViewById(R.id.suggestionContainer)
        styleVoiceStatusBar()
        styleAiTopicTypingBar()

        btnEmoji = root.findViewById(R.id.btnEmoji)
        btnClipboard = root.findViewById(R.id.btnClipboard)
        btnStylishFonts = root.findViewById(R.id.btnStylishFonts)
        btnMic = root.findViewById(R.id.btnMic)
        btnKeyboardSettings = root.findViewById(R.id.btnKeyboardSettings)
        btnAiHub = root.findViewById(R.id.btnAiHub)

        tvBrandSignature?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            handleOpenThemeCustomizer()
        }
        btnKeyboardSettings?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            handleOpenSettings()
        }

        clipboardPanel = root.findViewById(R.id.clipboardPanel)
        clipboardContainer = root.findViewById(R.id.clipboardContainer)
        tvClipboardEmpty = root.findViewById(R.id.tvClipboardEmpty)
        val btnClipboardClear: Button? = root.findViewById(R.id.btnClipboardClear)
        val btnClipboardClose: Button? = root.findViewById(R.id.btnClipboardClose)

        stylishFontPanel = root.findViewById(R.id.stylishFontPanel)
        stylishFontContainer = root.findViewById(R.id.stylishFontContainer)
        val btnFontsClose: Button? = root.findViewById(R.id.btnFontsClose)

        aiHubPanel = root.findViewById(R.id.aiHubPanel)
        tvAiTopicBox = root.findViewById(R.id.tvAiTopicBox)
        btnAiGenerate = root.findViewById(R.id.btnAiGenerate)
        btnAiHubClose = root.findViewById(R.id.btnAiHubClose)
        aiCategoryTabs = root.findViewById(R.id.aiCategoryTabs)
        aiQuickTopicsContainer = root.findViewById(R.id.aiQuickTopicsContainer)
        aiShayariScroll = root.findViewById(R.id.aiShayariScroll)
        aiShayariContainer = root.findViewById(R.id.aiShayariContainer)
        styleAiHubChrome()

        stickerPanel = root.findViewById(R.id.stickerPanel)
        tvStickerPhrasePreview = root.findViewById(R.id.tvStickerPhrasePreview)
        stickerStyleGridContainer = root.findViewById(R.id.stickerStyleGridContainer)
        val btnStickerClose: Button? = root.findViewById(R.id.btnStickerClose)
        btnStickerClose?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(OverlayMode.NONE)
        }

        emojiPanel = root.findViewById(R.id.emojiPanel)
        emojiCategoryTabs = root.findViewById(R.id.emojiCategoryTabs)
        emojiGridView = root.findViewById(R.id.emojiGridView)
        tvEmojiCategoryTitle = root.findViewById(R.id.tvEmojiCategoryTitle)
        btnEmojiClose = root.findViewById(R.id.btnEmojiClose)
        btnEmojiBackspace = root.findViewById(R.id.btnEmojiBackspace)

        val rgbView = root.findViewById<RgbKeyboardView>(R.id.rgbKeyboardView)
        keyboardView = rgbView
        trialLockPanel = root.findViewById(R.id.trialLockPanel)
        tvImeLockDeviceId = root.findViewById(R.id.tvImeLockDeviceId)
        val btnImeUnlockNow: Button? = root.findViewById(R.id.btnImeUnlockNow)
        tvImeLockDeviceId?.text = "Device ID: ${trialLicenseManager.deviceId}"
        btnImeUnlockNow?.setOnClickListener {
            handleOpenUnlockScreen()
        }

        rgbView.onKeyAction = { key ->
            if (isAiTopicTypingMode) {
                handleAiTopicKeyInput(key)
            } else {
                when (key) {
                    "DEL" -> handleBackspace()
                    "ENTER" -> handleEnter()
                    " " -> handleSpace()
                    else -> handleCommitText(StylishFontEngine.transform(key, activeStylishFont))
                }
            }
        }
        rgbView.onDeleteWordAction = {
            if (isAiTopicTypingMode) {
                handleAiTopicKeyInput("DEL")
            } else {
                handleDeletePreviousWord()
            }
        }
        rgbView.onCursorMove = { steps ->
            if (!isAiTopicTypingMode) handleCursorMove(steps)
        }
        rgbView.onKeyFeedback = { isModifier ->
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier)
        }
        rgbView.onLanguageToggleRequested = {
            handleToggleLanguage()
        }
        rgbView.onEmojiOpenRequested = {
            isAiTopicTypingMode = false
            setOverlay(OverlayMode.EMOJI)
        }

        // 1. Emoji (😊) -> Unlimited Categorized Emoji Panel
        btnEmoji?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            isAiTopicTypingMode = false
            setOverlay(if (activeOverlay == OverlayMode.EMOJI) OverlayMode.NONE else OverlayMode.EMOJI)
        }

        // 2. Clipboard (📋) -> Clipboard History Manager
        btnClipboard?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            isAiTopicTypingMode = false
            clipboardManager.captureCurrentPrimaryClip()
            setOverlay(if (activeOverlay == OverlayMode.CLIPBOARD) OverlayMode.NONE else OverlayMode.CLIPBOARD)
        }

        // 3. Stylish Fonts (Aa) -> Unicode Stylish Font Picker (VIP-gated after trial; respects remote overrides)
        btnStylishFonts?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            val latestLicense = trialLicenseManager.refreshState()
            currentLicenseState = latestLicense
            if (!latestLicense.isStylishFontsUnlocked) {
                activeStylishFont = StylishFontStyle.NORMAL
                handleOpenUnlockScreen()
                return@setOnClickListener
            }
            isAiTopicTypingMode = false
            setOverlay(if (activeOverlay == OverlayMode.FONTS) OverlayMode.NONE else OverlayMode.FONTS)
        }

        // 4. Voice Mic -> Tap (< 600ms) for single-sentence voice typing, Hold (>= 600ms) for Hands-Free Continuous Lock
        btnMic?.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isAiTopicTypingMode = false
                    micLockTriggeredDuringPress = false
                    micWasListeningOnDown = voiceController.isCurrentlyListening()
                    uiHandler.removeCallbacks(micHoldLockRunnable)
                    if (!micWasListeningOnDown) {
                        uiHandler.postDelayed(micHoldLockRunnable, VOICE_LOCK_HOLD_MS)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    uiHandler.removeCallbacks(micHoldLockRunnable)
                    v.performClick()
                    if (micWasListeningOnDown) {
                        feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                        stopVoiceTyping()
                    } else if (!micLockTriggeredDuringPress) {
                        feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                        startVoiceTyping(lockedContinuous = false)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    uiHandler.removeCallbacks(micHoldLockRunnable)
                    true
                }
                else -> false
            }
        }

        // 5. Single Unified ✨ AI Hub Button (Respects remote aiHub free/device overrides)
        btnAiHub?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            val latestLicense = trialLicenseManager.refreshState()
            currentLicenseState = latestLicense
            if (!latestLicense.isAiHubUnlocked) {
                handleOpenUnlockScreen()
                return@setOnClickListener
            }
            if (isAiTopicTypingMode) {
                isAiTopicTypingMode = false
                setOverlay(OverlayMode.AI_HUB)
            } else {
                setOverlay(if (activeOverlay == OverlayMode.AI_HUB) OverlayMode.NONE else OverlayMode.AI_HUB)
            }
        }

        tvAiTopicBox?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            enterAiTopicTypingMode()
        }

        btnAiGenerate?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            triggerAiShayariGeneration()
        }

        btnAiHubClose?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            isAiTopicTypingMode = false
            setOverlay(OverlayMode.NONE)
        }

        btnAiTopicGenerateNow?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            isAiTopicTypingMode = false
            triggerAiShayariGeneration()
            setOverlay(OverlayMode.AI_HUB)
        }

        btnAiTopicCancel?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            isAiTopicTypingMode = false
            setOverlay(OverlayMode.AI_HUB)
        }

        btnVoiceStop?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            if (hasActiveVoiceComposing) {
                withSafeInputConnection { ic ->
                    ic.setComposingText("", 1)
                    ic.finishComposingText()
                }
                hasActiveVoiceComposing = false
            }
            stopVoiceTyping()
            refreshSuggestions()
        }

        btnClipboardClear?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            clipboardManager.clearAll()
        }
        btnClipboardClose?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(OverlayMode.NONE)
        }
        btnFontsClose?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(OverlayMode.NONE)
        }

        btnEmojiClose?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(OverlayMode.NONE)
        }

        btnEmojiBackspace?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    handleBackspace()
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    isEmojiBackspaceRepeating = true
                    emojiBackspaceHoldStartMs = SystemClock.uptimeMillis()
                    uiHandler.removeCallbacks(emojiBackspaceRepeatRunnable)
                    uiHandler.postDelayed(emojiBackspaceRepeatRunnable, BACKSPACE_INITIAL_DELAY_MS)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isEmojiBackspaceRepeating = false
                    uiHandler.removeCallbacks(emojiBackspaceRepeatRunnable)
                    true
                }
                else -> false
            }
        }

        setupEmojiPanel()
        populateStylishFontsPanel()
        populateStickerStyleSelector()
        renderAiHubPanel()
        applySettingsToViews(currentSettings)
        rgbView.updatePhotoTheme(currentPhotoTheme)
        renderClipboardSnippets(clipboardManager.clips.value)
        currentLicenseState = trialLicenseManager.refreshState()
        applyTrialLockState()
        updateToolbarTints()
        refreshSuggestions()

        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        isTypingWindowVisible = true
        clearBanglishBuffer(finishComposing = false)
        clipboardManager.pruneExpiredClips()
        clipboardManager.captureCurrentPrimaryClip()
        // Synchronously load saved settings, language, and photo wallpaper before drawing so IME switches never flash defaults
        currentLicenseState = trialLicenseManager.refreshState()
        currentSettings = preferencesRepository.getCachedSettingsSnapshot()
        currentPhotoTheme = customBackgroundManager.reloadFromPrefs()
        applySettingsToViews(currentSettings)
        keyboardView?.setActiveLanguage(currentSettings.activeLanguageId)
        val rawInputType = info?.inputType ?: 0
        val inputClass = rawInputType and InputType.TYPE_MASK_CLASS
        val isNumericField = inputClass == InputType.TYPE_CLASS_NUMBER ||
            inputClass == InputType.TYPE_CLASS_PHONE ||
            inputClass == InputType.TYPE_CLASS_DATETIME
        if (isNumericField && activeOverlay != OverlayMode.NONE) {
            setOverlay(OverlayMode.NONE)
        }
        keyboardView?.setNumberPadMode(isNumericField)
        val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        keyboardView?.setSearchAction(actionId == EditorInfo.IME_ACTION_SEARCH)
        keyboardView?.updatePhotoTheme(currentPhotoTheme)
        keyboardView?.updateSettings(currentSettings)
        keyboardView?.resumeAnimations()
        lastVipSyncMs = SystemClock.uptimeMillis()
        syncTrialVisualStateFromService(currentLicenseState)
        com.example.notifications.VipNotificationHelper.evaluateAndNotify(this, currentLicenseState)
        registerCallStateListenerIfPermitted()
        applyTrialLockState()
        updateCallBannerVisibility()
        refreshSuggestions(immediate = true)
    }

    private fun syncTrialVisualStateFromService(state: LicenseUiState) {
        if (!state.isDeviceBlocked && state.isVipActive) {
            serviceScope.launch(Dispatchers.IO) {
                preferencesRepository.resetPostTrialRevertFlag()
                customBackgroundManager.resetPostTrialVisualRevertFlag()
                if (!state.isTrialExpired) {
                    preferencesRepository.seedInitialTrialTouchDefaultsIfNeeded()
                    customBackgroundManager.seedTrialDefaultPresetIfUnset(
                        com.example.data.BannerPresetCatalog.PRESET_CUTE_CARTOON
                    )
                }
            }
        } else if (state.isFreeStandardMode) {
            var revertedSettings = currentSettings
            var didModifySettings = false
            if (revertedSettings.effect != RgbEffect.NORMAL_DEFAULT &&
                !state.isThemeItemUnlocked(revertedSettings.effect.id, isPermanentlyFree = !revertedSettings.effect.isVipEffect)
            ) {
                revertedSettings = revertedSettings.copy(effect = RgbEffect.NORMAL_DEFAULT)
                didModifySettings = true
            }
            if (revertedSettings.touchEffect != TouchTypingEffect.NONE &&
                !state.isThemeItemUnlocked(revertedSettings.touchEffect.id, isPermanentlyFree = !revertedSettings.touchEffect.isVipEffect)
            ) {
                revertedSettings = revertedSettings.copy(touchEffect = TouchTypingEffect.NONE)
                didModifySettings = true
            }
            if (didModifySettings) {
                currentSettings = revertedSettings
                keyboardView?.updateSettings(revertedSettings)
            }
            if (!currentPhotoTheme.hasCustomGalleryPhoto && currentPhotoTheme.hasPhoto) {
                val itemId = currentPhotoTheme.downloadedThemeId ?: currentPhotoTheme.presetId.orEmpty()
                if (itemId.isBlank() || !state.isThemeItemUnlocked(itemId, isPermanentlyFree = false)) {
                    customBackgroundManager.clearBuiltInPresetOrComboPreservingGalleryPhoto()
                    currentPhotoTheme = customBackgroundManager.getCurrentTheme()
                    keyboardView?.updatePhotoTheme(currentPhotoTheme)
                }
            }
            serviceScope.launch(Dispatchers.IO) {
                preferencesRepository.revertToFreeStandardDefaultsOnce()
                customBackgroundManager.revertPaidVisualsOnTrialExpiryPreservingGalleryPhoto()
                if (didModifySettings) {
                    preferencesRepository.updateEffect(revertedSettings.effect)
                    preferencesRepository.updateTouchEffect(revertedSettings.touchEffect)
                }
            }
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        isTypingWindowVisible = true
        keyboardView?.resumeAnimations()
        registerCallStateListenerIfPermitted()
        updateCallBannerVisibility()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        val elapsedSinceCallAccept = SystemClock.elapsedRealtime() - lastKeyboardCallAcceptMs
        val keepFocusAfterAccept = elapsedSinceCallAccept in 0L..3_500L &&
            currentCallState == TelephonyManager.CALL_STATE_OFFHOOK &&
            currentInputConnection != null
        isTypingWindowVisible = false
        keyboardView?.pauseAnimations()
        isEmojiBackspaceRepeating = false
        uiHandler.removeCallbacks(emojiBackspaceRepeatRunnable)
        uiHandler.removeCallbacks(micHoldLockRunnable)
        uiHandler.removeCallbacks(callTimerRunnable)
        if (keepFocusAfterAccept) {
            uiHandler.post { ensureKeyboardStaysFocusedAfterCallAccept() }
        } else {
            callBannerBar?.visibility = View.GONE
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
        if (banglishRawBengaliBuffer.isNotEmpty() &&
            candidatesStart >= 0 &&
            (newSelStart < candidatesStart || newSelEnd > candidatesEnd)
        ) {
            clearBanglishBuffer(finishComposing = true)
        }
        refreshSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        isTypingWindowVisible = false
        stopStickerPulseAnimation()
        keyboardView?.pauseAnimations()
        clearBanglishBuffer(finishComposing = true)
        if (hasActiveVoiceComposing) {
            withSafeInputConnection { it.finishComposingText() }
            hasActiveVoiceComposing = false
        }
        if (::voiceController.isInitialized && voiceController.isCurrentlyListening()) {
            voiceController.stopListening()
        }
        voiceBar?.visibility = View.GONE
        callBannerBar?.visibility = View.GONE
        isEmojiBackspaceRepeating = false
        uiHandler.removeCallbacks(emojiBackspaceRepeatRunnable)
        uiHandler.removeCallbacks(micHoldLockRunnable)
        uiHandler.removeCallbacks(callTimerRunnable)
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        val locale = newSubtype?.locale
        if (!locale.isNullOrBlank()) {
            val langId = LanguageCatalog.idFromLocale(locale)
            // Only honor explicit non-English system subtype switches; never let default en_US overwrite Bangla/Banglish/Hindi
            if (langId != LanguageCatalog.ENGLISH_ID &&
                (LanguageCatalog.isBuiltInId(langId) || languagePackManager.isDownloaded(langId)) &&
                langId != currentSettings.activeLanguageId
            ) {
                currentSettings = currentSettings.copy(activeLanguageId = langId)
                updateKeyboardHeightForLanguage(langId)
                keyboardView?.setActiveLanguage(langId)
                serviceScope.launch(Dispatchers.IO) { preferencesRepository.updateActiveLanguage(langId) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterCallStateListener()
        stopStickerPulseAnimation()
        stickerCardViews.clear()
        isEmojiBackspaceRepeating = false
        uiHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        if (::voiceController.isInitialized) {
            voiceController.destroy()
        }
        if (::feedbackEngine.isInitialized) {
            feedbackEngine.release()
        }
        callBannerBar = null
        tvCallStatus = null
        btnCallReceive = null
        btnCallEnd = null
        topBar = null
        tvBrandSignature = null
        voiceBar = null
        tvVoiceLangBadge = null
        tvVoiceStatus = null
        btnVoiceStop = null
        aiTopicTypingBar = null
        tvAiTopicDraft = null
        btnAiTopicGenerateNow = null
        btnAiTopicCancel = null
        suggestionBar = null
        suggestionContainer = null
        keyboardView = null
        trialLockPanel = null
        tvImeLockDeviceId = null
        clipboardPanel = null
        clipboardContainer = null
        tvClipboardEmpty = null
        stylishFontPanel = null
        stylishFontContainer = null
        aiHubPanel = null
        tvAiTopicBox = null
        btnAiGenerate = null
        btnAiHubClose = null
        aiCategoryTabs = null
        aiQuickTopicsContainer = null
        aiShayariScroll = null
        aiShayariContainer = null
        stickerPanel = null
        tvStickerPhrasePreview = null
        stickerStyleGridContainer = null
        emojiPanel = null
        emojiCategoryTabs = null
        emojiGridView = null
        tvEmojiCategoryTitle = null
        btnEmojiClose = null
        btnEmojiBackspace = null
        emojiGridAdapter = null
        btnEmoji = null
        btnClipboard = null
        btnStylishFonts = null
        btnMic = null
        btnKeyboardSettings = null
        btnAiHub = null
    }

    /**
     * Never locks out basic typing! During the 7-day VIP trial or after ₹20 Lifetime VIP unlock,
     * enables Touch Spark RGB, continuous animations, custom photo/banner backgrounds, RGB Stickers,
     * and the unified ✨ AI Shayari & Smart Reply Hub.
     * The glowing "Rakib official" signature badge stays permanently visible and 100% free.
     */
    private fun applyTrialLockState() {
        trialLockPanel?.visibility = View.GONE
        topBar?.visibility = if (currentSettings.showToolbar) View.VISIBLE else View.GONE
        if (!currentLicenseState.isStylishFontsUnlocked) {
            activeStylishFont = StylishFontStyle.NORMAL
            if (activeOverlay == OverlayMode.FONTS) {
                activeOverlay = OverlayMode.NONE
                stylishFontPanel?.visibility = View.GONE
                keyboardView?.visibility = View.VISIBLE
            }
        }
        // Strictly lock Banglish (BN-EN) when expired & not overridden: auto-revert to free English typing
        if (!currentLicenseState.isBanglishUnlocked &&
            currentSettings.activeLanguageId == LanguageCatalog.BANGLISH_ID
        ) {
            currentSettings = currentSettings.copy(activeLanguageId = LanguageCatalog.ENGLISH_ID)
            updateKeyboardHeightForLanguage(LanguageCatalog.ENGLISH_ID)
            keyboardView?.setActiveLanguage(LanguageCatalog.ENGLISH_ID)
            serviceScope.launch(Dispatchers.IO) {
                preferencesRepository.updateActiveLanguage(LanguageCatalog.ENGLISH_ID)
            }
        }
        if (!currentLicenseState.isAiHubUnlocked &&
            (activeOverlay == OverlayMode.AI_HUB || isAiTopicTypingMode)
        ) {
            isAiTopicTypingMode = false
            activeOverlay = OverlayMode.NONE
            aiHubPanel?.visibility = View.GONE
            aiTopicTypingBar?.visibility = View.GONE
            keyboardView?.visibility = View.VISIBLE
        }
        if (!currentLicenseState.isRgbStickerUnlocked && activeOverlay == OverlayMode.STICKER_SELECTOR) {
            activeOverlay = OverlayMode.NONE
            stickerPanel?.visibility = View.GONE
            keyboardView?.visibility = View.VISIBLE
        }
        suggestionBar?.visibility = if (activeOverlay == OverlayMode.NONE && !isAiTopicTypingMode) View.VISIBLE else View.GONE
        keyboardView?.setVipVisualsEnabled(currentLicenseState.isRgbThemesUnlocked)
        updateCallBannerVisibility()
        updateToolbarTints()
        refreshSuggestions()
    }

    private fun setOverlay(mode: OverlayMode) {
        activeOverlay = mode
        if (mode != OverlayMode.NONE) {
            isAiTopicTypingMode = false
        }
        trialLockPanel?.visibility = View.GONE
        aiTopicTypingBar?.visibility = if (isAiTopicTypingMode && mode == OverlayMode.NONE) View.VISIBLE else View.GONE
        clipboardPanel?.visibility = if (mode == OverlayMode.CLIPBOARD) View.VISIBLE else View.GONE
        stylishFontPanel?.visibility = if (mode == OverlayMode.FONTS) View.VISIBLE else View.GONE
        emojiPanel?.visibility = if (mode == OverlayMode.EMOJI) View.VISIBLE else View.GONE
        stickerPanel?.visibility = if (mode == OverlayMode.STICKER_SELECTOR) View.VISIBLE else View.GONE
        aiHubPanel?.visibility = if (mode == OverlayMode.AI_HUB) View.VISIBLE else View.GONE
        keyboardView?.visibility = if (mode == OverlayMode.NONE) View.VISIBLE else View.GONE
        suggestionBar?.visibility = if (mode == OverlayMode.NONE && !isAiTopicTypingMode) View.VISIBLE else View.GONE
        if (mode == OverlayMode.EMOJI) {
            btnEmojiClose?.text = if (currentSettings.activeLanguageId == "bn" ||
                currentSettings.activeLanguageId == LanguageCatalog.BANGLISH_ID
            ) "কখগ" else "ABC"
        }
        if (mode == OverlayMode.AI_HUB) {
            renderAiHubPanel()
        }
        if (mode == OverlayMode.FONTS) {
            populateStylishFontsPanel()
        }
        if (mode == OverlayMode.STICKER_SELECTOR) {
            startStickerPulseAnimation()
        } else {
            stopStickerPulseAnimation()
        }
        updateToolbarTints()
    }

    private fun updateToolbarTints() {
        val density = resources.displayMetrics.density
        val defaultTint = Color.parseColor("#D5DCEF")
        val activeCyan = Color.parseColor("#00F5D4")
        val activePink = Color.parseColor("#FF2A85")
        val activeVoice = Color.parseColor("#00F59B")

        btnEmoji?.background = if (activeOverlay == OverlayMode.EMOJI) {
            GradientDrawable().apply {
                setColor(Color.parseColor("#2A1636"))
                cornerRadius = 8f * density
                setStroke((1f * density).toInt(), activePink)
            }
        } else {
            null
        }
        btnClipboard?.background = if (activeOverlay == OverlayMode.CLIPBOARD) {
            GradientDrawable().apply {
                setColor(Color.parseColor("#132838"))
                cornerRadius = 8f * density
                setStroke((1f * density).toInt(), activeCyan)
            }
        } else {
            null
        }
        val isFontsVip = currentLicenseState.isStylishFontsUnlocked
        val fontsHighlighted = isFontsVip && (activeOverlay == OverlayMode.FONTS || activeStylishFont != StylishFontStyle.NORMAL)
        btnStylishFonts?.text = if (isFontsVip) {
            getString(R.string.toolbar_stylish_fonts)
        } else {
            getString(R.string.toolbar_stylish_fonts_locked)
        }
        btnStylishFonts?.setTextColor(
            when {
                !isFontsVip -> Color.parseColor("#FFCF70")
                fontsHighlighted -> activeCyan
                else -> Color.parseColor("#E2E8F0")
            }
        )
        btnStylishFonts?.background = if (fontsHighlighted) {
            GradientDrawable().apply {
                setColor(Color.parseColor("#132838"))
                cornerRadius = 8f * density
                setStroke((1f * density).toInt(), activeCyan)
            }
        } else {
            null
        }
        val micTint = when {
            latestVoiceState.isListening && latestVoiceState.isLockedContinuous -> Color.parseColor("#FFB800")
            latestVoiceState.isListening -> activeVoice
            else -> defaultTint
        }
        btnMic?.setColorFilter(micTint, PorterDuff.Mode.SRC_IN)
        val isAiVip = currentLicenseState.isAiHubUnlocked
        val aiActive = activeOverlay == OverlayMode.AI_HUB || isAiTopicTypingMode
        btnAiHub?.apply {
            text = if (isAiVip) getString(R.string.toolbar_ai_hub) else getString(R.string.toolbar_ai_hub_locked)
            setTextColor(if (isAiVip) Color.WHITE else Color.parseColor("#FFCF70"))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                when {
                    !isAiVip -> intArrayOf(Color.parseColor("#2E1528"), Color.parseColor("#2A1D12"))
                    aiActive -> intArrayOf(Color.parseColor("#FF2A85"), Color.parseColor("#00B4D8"))
                    else -> intArrayOf(Color.parseColor("#D81B60"), Color.parseColor("#FF8F00"))
                }
            ).apply {
                cornerRadius = 999f * density
                setStroke(
                    (1.2f * density).toInt(),
                    when {
                        !isAiVip -> Color.parseColor("#FF9E00")
                        aiActive -> activeCyan
                        else -> Color.parseColor("#FF79B0")
                    }
                )
            }
        }
    }

    private fun setupEmojiPanel() {
        val adapter = EmojiGridAdapter(EmojiCatalog.categories[selectedEmojiCategoryIndex].emojis) { emoji ->
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
            handleCommitText(emoji)
        }
        emojiGridAdapter = adapter
        emojiGridView?.adapter = adapter
        renderEmojiCategoryTabs()
        selectEmojiCategory(selectedEmojiCategoryIndex)
    }

    private fun renderEmojiCategoryTabs() {
        val container = emojiCategoryTabs ?: return
        container.removeAllViews()
        val density = resources.displayMetrics.density

        EmojiCatalog.categories.forEachIndexed { index, category ->
            val isSelected = index == selectedEmojiCategoryIndex
            val tab = TextView(this).apply {
                text = category.icon
                textSize = 18f
                gravity = Gravity.CENTER
                contentDescription = category.title
                val padH = (10 * density).toInt()
                setPadding(padH, (2 * density).toInt(), padH, (2 * density).toInt())
                background = GradientDrawable().apply {
                    setColor(if (isSelected) Color.parseColor("#24314A") else Color.TRANSPARENT)
                    cornerRadius = 8f * density
                    if (isSelected) {
                        setStroke((1.2f * density).toInt(), Color.parseColor("#00E5FF"))
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (30 * density).toInt()
                ).apply {
                    marginEnd = (4 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    selectEmojiCategory(index)
                }
            }
            container.addView(tab)
        }
    }

    private fun selectEmojiCategory(index: Int) {
        val safeIdx = index.coerceIn(0, EmojiCatalog.categories.lastIndex)
        selectedEmojiCategoryIndex = safeIdx
        val category = EmojiCatalog.categories[safeIdx]
        tvEmojiCategoryTitle?.text = "${category.title} (${category.emojis.size})"
        emojiGridView?.numColumns = if (category.id == "ai_unique") 5 else 8
        emojiGridAdapter?.updateEmojis(category.emojis)
        emojiGridView?.smoothScrollToPosition(0)
        renderEmojiCategoryTabs()
    }

    private inner class EmojiGridAdapter(
        private var items: List<String>,
        private val onEmojiTap: (String) -> Unit
    ) : BaseAdapter() {
        private val density = resources.displayMetrics.density

        fun updateEmojis(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): String = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = (convertView as? TextView) ?: TextView(this@RgbKeyboardService).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                maxLines = 1
                minHeight = (38 * density).toInt()
                setPadding((1 * density).toInt(), (4 * density).toInt(), (1 * density).toInt(), (4 * density).toInt())
            }
            val emoji = items[position]
            tv.textSize = when {
                emoji.length > 4 -> 15f
                emoji.length > 2 -> 17f
                else -> 23f
            }
            tv.text = emoji
            tv.setOnClickListener { onEmojiTap(emoji) }
            return tv
        }
    }

    private fun startVoiceTyping(lockedContinuous: Boolean) {
        syncVipVisualsQuietly()
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMicPermission) {
            voiceBar?.visibility = View.VISIBLE
            tvVoiceStatus?.text = getString(R.string.voice_permission_needed)
            tvVoiceStatus?.setTextColor(Color.parseColor("#FFB300"))
            launchAudioPermissionRequest()
            return
        }

        val activeLocale = OnlineVoiceTypingController.localeForLanguageId(currentSettings.activeLanguageId)
        voiceBar?.visibility = View.VISIBLE
        voiceController.startListening(activeLocale, lockedContinuous = lockedContinuous)
    }

    private fun stopVoiceTyping() {
        if (hasActiveVoiceComposing) {
            withSafeInputConnection { it.finishComposingText() }
            hasActiveVoiceComposing = false
        }
        voiceController.stopListening()
        voiceBar?.visibility = View.GONE
        updateToolbarTints()
    }

    private fun renderVoiceState(state: OnlineVoiceTypingController.VoiceListeningState) {
        updateToolbarTints()
        tvVoiceLangBadge?.text = state.locale.badge
        tvVoiceStatus?.typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD

        when {
            state.isListening && state.isLockedContinuous -> {
                voiceBar?.visibility = View.VISIBLE
                tvVoiceStatus?.setTextColor(Color.parseColor("#FFD166"))
                tvVoiceStatus?.text = if (state.partialTranscript.isNotBlank()) {
                    "🔒 ${state.partialTranscript}"
                } else {
                    getString(R.string.voice_locked_continuous_bn)
                }
            }
            state.isListening -> {
                voiceBar?.visibility = View.VISIBLE
                tvVoiceStatus?.setTextColor(Color.parseColor("#00F5D4"))
                tvVoiceStatus?.text = if (state.partialTranscript.isNotBlank()) {
                    "🎙️ শোনা যাচ্ছে... • ${state.partialTranscript}"
                } else {
                    getString(R.string.voice_listening_bn)
                }
            }
            !state.errorMessage.isNullOrBlank() -> {
                if (hasActiveVoiceComposing) {
                    withSafeInputConnection { it.finishComposingText() }
                    hasActiveVoiceComposing = false
                }
                voiceBar?.visibility = View.VISIBLE
                tvVoiceStatus?.setTextColor(Color.parseColor("#FFB300"))
                tvVoiceStatus?.text = state.errorMessage
            }
            else -> {
                if (hasActiveVoiceComposing) {
                    withSafeInputConnection { it.finishComposingText() }
                    hasActiveVoiceComposing = false
                }
                voiceBar?.visibility = View.GONE
            }
        }
    }

    /**
     * Launches a tiny transparent PermissionRequestActivity ONLY ONCE when RECORD_AUDIO is missing.
     * Never re-prompts or redirects to system settings once prompted.
     */
    private fun launchAudioPermissionRequest() {
        val permPrefs = getSharedPreferences(PREFS_PERMISSIONS, Context.MODE_PRIVATE)
        if (permPrefs.getBoolean(KEY_MIC_PROMPTED_ONCE, false)) {
            voiceBar?.visibility = View.VISIBLE
            tvVoiceStatus?.setTextColor(Color.parseColor("#FFB300"))
            tvVoiceStatus?.text = "🎙️ মাইক পারমিশন নেই (১ বার অনুরোধ করা হয়েছে)"
            return
        }
        permPrefs.edit().putBoolean(KEY_MIC_PROMPTED_ONCE, true).apply()
        val intent = Intent(this, PermissionRequestActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        runCatching { startActivity(intent) }
    }

    private fun refreshSuggestions(immediate: Boolean = false) {
        if (suggestionContainer == null) return
        val beforeRaw = runCatching {
            currentInputConnection?.getTextBeforeCursor(140, 0)?.toString().orEmpty()
        }.getOrDefault("")
        val before = if (currentSettings.activeLanguageId == LanguageCatalog.BANGLISH_ID &&
            banglishRawBengaliBuffer.isNotEmpty()
        ) {
            banglishWordPrefixContext + banglishRawBengaliBuffer.toString()
        } else {
            beforeRaw
        }
        val selected = runCatching {
            currentInputConnection?.getSelectedText(0)?.toString().orEmpty()
        }.getOrDefault("")
        val langId = currentSettings.activeLanguageId
        val smartEnabled = currentSettings.smartSuggestionsEnabled
        val isVip = currentLicenseState.isRgbStickerUnlocked
        val topClip = if (langId == LanguageCatalog.BANGLISH_ID) {
            clipboardManager.clips.value.firstOrNull().orEmpty()
        } else {
            ""
        }
        val freshPasteClip = if (clipboardManager.hasFreshTextClip.value) {
            clipboardManager.clips.value.firstOrNull().orEmpty()
        } else {
            ""
        }
        val activeScreenshot = clipboardManager.latestScreenshotClip.value?.takeIf {
            (System.currentTimeMillis() - it.timestampMs) <= ClipboardHistoryManager.SCREENSHOT_FRESH_WINDOW_MS
        }

        suggestionJob?.cancel()
        suggestionJob = serviceScope.launch(Dispatchers.Default) {
            if (!immediate) {
                delay(16L)
            }
            val prediction = SmartWordPredictor.predict(
                textBeforeCursor = before,
                languageId = langId,
                context = applicationContext
            )
            val selectedConverted = if (langId == LanguageCatalog.BANGLISH_ID &&
                BengaliTransliterator.containsBengaliScript(selected)
            ) {
                BengaliTransliterator.transliterate(selected, capitalizeSentences = true)
            } else {
                ""
            }
            val clipConverted = if (langId == LanguageCatalog.BANGLISH_ID &&
                selectedConverted.isEmpty() &&
                prediction.banglaConvertPreview.isEmpty() &&
                BengaliTransliterator.containsBengaliScript(topClip)
            ) {
                BengaliTransliterator.transliterate(topClip, capitalizeSentences = true)
            } else {
                ""
            }
            withContext(Dispatchers.Main) {
                renderSuggestionChips(
                    prediction = prediction,
                    langId = langId,
                    smartEnabled = smartEnabled,
                    isVip = isVip,
                    selectedBanglaConverted = selectedConverted,
                    clipBanglaConverted = clipConverted,
                    freshPasteClip = freshPasteClip,
                    screenshotClip = activeScreenshot
                )
            }
        }
    }

    private fun renderSuggestionChips(
        prediction: SmartWordPredictor.PredictionResult,
        langId: String,
        smartEnabled: Boolean,
        isVip: Boolean,
        selectedBanglaConverted: String,
        clipBanglaConverted: String,
        freshPasteClip: String,
        screenshotClip: ScreenshotClip?
    ) {
        val container = suggestionContainer ?: return
        val effectiveSuggestions = if (smartEnabled) prediction.suggestions else emptyList()
        val isBanglishMode = langId == LanguageCatalog.BANGLISH_ID && currentLicenseState.isBanglishUnlocked

        val activeConvertPreview = when {
            selectedBanglaConverted.isNotEmpty() -> selectedBanglaConverted
            prediction.banglaConvertPreview.isNotEmpty() -> prediction.banglaConvertPreview
            else -> ""
        }
        val banglaChipLabel = when {
            !isBanglishMode -> ""
            activeConvertPreview.isNotEmpty() -> {
                val shortPreview = if (activeConvertPreview.length > 18) {
                    activeConvertPreview.take(18).trimEnd() + "…"
                } else {
                    activeConvertPreview
                }
                getString(R.string.chip_bangla_to_banglish_preview, shortPreview)
            }
            clipBanglaConverted.isNotEmpty() -> getString(R.string.chip_bangla_to_banglish_paste)
            else -> getString(R.string.chip_bangla_to_banglish)
        }
        val screenshotUriStr = screenshotClip?.uri?.toString().orEmpty()

        if (effectiveSuggestions == lastRenderedSuggestions &&
            prediction.stickerPhrase == lastRenderedStickerPhrase &&
            isVip == lastRenderedVipState &&
            smartEnabled == lastRenderedSmartEnabled &&
            banglaChipLabel == lastRenderedBanglaChipLabel &&
            langId == lastRenderedLangId &&
            screenshotUriStr == lastRenderedScreenshotUri &&
            freshPasteClip == lastRenderedFreshPasteClip &&
            prediction.suggestedEmojis == lastRenderedSuggestedEmojis
        ) {
            return
        }
        lastRenderedSuggestions = effectiveSuggestions
        lastRenderedStickerPhrase = prediction.stickerPhrase
        lastRenderedVipState = isVip
        lastRenderedSmartEnabled = smartEnabled
        lastRenderedBanglaChipLabel = banglaChipLabel
        lastRenderedLangId = langId
        lastRenderedScreenshotUri = screenshotUriStr
        lastRenderedFreshPasteClip = freshPasteClip
        lastRenderedSuggestedEmojis = prediction.suggestedEmojis

        val density = resources.displayMetrics.density
        container.removeAllViews()

        // -2. One-Tap Screenshot Quick-Paste Chip (zero-permission MediaStore / Clipboard URI)
        if (screenshotClip != null) {
            val screenshotChip = TextView(this).apply {
                text = "📸 Screenshot"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#00FF9D"))
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(
                    (11 * density).toInt(),
                    (4 * density).toInt(),
                    (11 * density).toInt(),
                    (4 * density).toInt()
                )
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.parseColor("#0C2B26"), Color.parseColor("#102238"))
                ).apply {
                    cornerRadius = 999f * density
                    setStroke((1.3f * density).toInt(), Color.parseColor("#00FF9D"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (28 * density).toInt()
                ).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    handleScreenshotQuickPaste(screenshotClip)
                }
                setOnLongClickListener {
                    clipboardManager.dismissScreenshotClip()
                    refreshSuggestions(immediate = true)
                    true
                }
            }
            container.addView(screenshotChip)
        }

        // -1. One-Tap Clipboard Auto-Paste Chip when fresh copied text exists
        if (freshPasteClip.isNotEmpty()) {
            val preview = if (freshPasteClip.length > 16) {
                freshPasteClip.take(16).trimEnd() + "…"
            } else {
                freshPasteClip
            }
            val pasteChip = TextView(this).apply {
                text = "📋 Paste: $preview"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#6CF3FF"))
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(
                    (11 * density).toInt(),
                    (4 * density).toInt(),
                    (11 * density).toInt(),
                    (4 * density).toInt()
                )
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#102938"))
                    cornerRadius = 999f * density
                    setStroke((1.2f * density).toInt(), Color.parseColor("#00E5FF"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (28 * density).toInt()
                ).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                    val outClip = if (isBanglishMode &&
                        BengaliTransliterator.containsBengaliScript(freshPasteClip)
                    ) {
                        BengaliTransliterator.transliterate(freshPasteClip, capitalizeSentences = true)
                    } else {
                        freshPasteClip
                    }
                    clipboardManager.markClipboardPasted(freshPasteClip)
                    handleCommitText(outClip)
                }
            }
            container.addView(pasteChip)
        }

        // 0. Strict One-Way "বাংলা → বাংলিশ" Converter Chip (active in Banglish BN-EN mode)
        if (isBanglishMode && banglaChipLabel.isNotEmpty()) {
            val hasPendingConvert = activeConvertPreview.isNotEmpty() || clipBanglaConverted.isNotEmpty()
            val converterChip = TextView(this).apply {
                text = banglaChipLabel
                textSize = 12f
                typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (hasPendingConvert) Color.parseColor("#00FF88") else Color.parseColor("#7CEEFF"))
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(
                    (11 * density).toInt(),
                    (4 * density).toInt(),
                    (11 * density).toInt(),
                    (4 * density).toInt()
                )
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    if (hasPendingConvert) {
                        intArrayOf(Color.parseColor("#0D2F26"), Color.parseColor("#10293B"))
                    } else {
                        intArrayOf(Color.parseColor("#132035"), Color.parseColor("#1A1734"))
                    }
                ).apply {
                    cornerRadius = 999f * density
                    setStroke(
                        (1.3f * density).toInt(),
                        if (hasPendingConvert) Color.parseColor("#00FF88") else Color.parseColor("#00D4FF")
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (28 * density).toInt()
                ).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    handleBanglaToBanglishChipTap(
                        selectedConverted = selectedBanglaConverted,
                        cursorSource = prediction.banglaConvertSource,
                        cursorConverted = prediction.banglaConvertPreview,
                        clipConverted = clipBanglaConverted
                    )
                }
            }
            container.addView(converterChip)
        }

        // 1. Glowing "✨ RGB Sticker" chip (or "🔒 VIP • RGB Sticker" after 7-day trial expires)
        if (prediction.stickerPhrase.isNotEmpty()) {
            val stickerChip = TextView(this).apply {
                text = if (isVip) {
                    getString(R.string.chip_rgb_sticker)
                } else {
                    getString(R.string.chip_rgb_sticker_vip_locked)
                }
                textSize = 12f
                typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (isVip) Color.parseColor("#00F5D4") else Color.parseColor("#FF9E00"))
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(
                    (12 * density).toInt(),
                    (4 * density).toInt(),
                    (12 * density).toInt(),
                    (4 * density).toInt()
                )
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    if (isVip) {
                        intArrayOf(Color.parseColor("#3D1039"), Color.parseColor("#112D3C"))
                    } else {
                        intArrayOf(Color.parseColor("#2E1426"), Color.parseColor("#2A1D12"))
                    }
                ).apply {
                    cornerRadius = 999f * density
                    setStroke(
                        (1.3f * density).toInt(),
                        if (isVip) Color.parseColor("#FF2A85") else Color.parseColor("#FF9E00")
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (28 * density).toInt()
                ).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    handleRgbStickerChipTap(prediction.stickerPhrase)
                }
            }
            container.addView(stickerChip)
        }

        // 1.5 Contextual Emoji Suggestion Chips
        if (prediction.suggestedEmojis.isNotEmpty()) {
            prediction.suggestedEmojis.forEach { emoji ->
                val emojiChip = TextView(this).apply {
                    text = emoji
                    textSize = 15f
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setPadding(
                        (10 * density).toInt(),
                        (2 * density).toInt(),
                        (10 * density).toInt(),
                        (2 * density).toInt()
                    )
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#231738"))
                        cornerRadius = 999f * density
                        setStroke((1f * density).toInt(), Color.parseColor("#88FF2A85"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (28 * density).toInt()
                    ).apply {
                        marginEnd = (6 * density).toInt()
                    }
                    setOnClickListener {
                        feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                        handleCommitText("$emoji ")
                    }
                }
                container.addView(emojiChip)
            }
        }

        // 2. Smart Word Prediction & Next-Word Suggestion Chips (only when Smart Auto-Suggestions enabled)
        if (smartEnabled) {
            effectiveSuggestions.forEach { suggestion ->
                val wordChip = TextView(this).apply {
                    text = suggestion
                    textSize = 13f
                    typeface = if (langId == "bn") (bengaliTypeface ?: Typeface.DEFAULT) else Typeface.DEFAULT
                    setTextColor(Color.parseColor("#F1F5FF"))
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setPadding(
                        (11 * density).toInt(),
                        (4 * density).toInt(),
                        (11 * density).toInt(),
                        (4 * density).toInt()
                    )
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#19132B"))
                        cornerRadius = 999f * density
                        setStroke((1f * density).toInt(), Color.parseColor("#2F2352"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (28 * density).toInt()
                    ).apply {
                        marginEnd = (6 * density).toInt()
                    }
                    setOnClickListener {
                        feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                        applyWordSuggestion(suggestion, prediction.currentTokenLength)
                    }
                }
                container.addView(wordChip)
            }
        }
    }

    private fun handleScreenshotQuickPaste(clip: ScreenshotClip) {
        val ic = currentInputConnection
        val editorInfo = currentInputEditorInfo
        val mimeTypes = if (editorInfo != null) {
            EditorInfoCompat.getContentMimeTypes(editorInfo)
        } else {
            emptyArray()
        }
        val acceptsImage = mimeTypes.any { mime ->
            ClipDescription.compareMimeTypes(mime, clip.mimeType) ||
                ClipDescription.compareMimeTypes(mime, "image/png") ||
                ClipDescription.compareMimeTypes(mime, "image/jpeg") ||
                ClipDescription.compareMimeTypes(mime, "image/*")
        }

        var committedDirectly = false
        if (acceptsImage && ic != null && editorInfo != null) {
            val targetPackage = editorInfo.packageName
            if (!targetPackage.isNullOrBlank()) {
                runCatching {
                    grantUriPermission(
                        targetPackage,
                        clip.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            val description = ClipDescription("Screenshot", arrayOf(clip.mimeType))
            val contentInfo = InputContentInfoCompat(clip.uri, description, null)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            } else {
                0
            }
            committedDirectly = runCatching {
                InputConnectionCompat.commitContent(ic, editorInfo, contentInfo, flags, null)
            }.getOrDefault(false)
        }

        if (!committedDirectly) {
            runCatching {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = clip.mimeType
                    putExtra(Intent.EXTRA_STREAM, clip.uri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Paste Screenshot").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(chooser)
            }
        }
        clipboardManager.dismissScreenshotClip()
        refreshSuggestions(immediate = true)
    }

    /**
     * Strict one-way Bengali -> Romanized English converter action for "বাংলা → বাংলিশ" chip:
     * 1. Converts selected Bengali text in-place -> Romanized English
     * 2. Converts pasted/typed Bengali text before cursor -> Romanized English
     * 3. Converts latest copied Bengali clipboard text -> inserts Romanized English
     * 4. Otherwise launches direct Bengali (bn-IN) Voice-to-Banglish input!
     */
    private fun handleBanglaToBanglishChipTap(
        selectedConverted: String,
        cursorSource: String,
        cursorConverted: String,
        clipConverted: String
    ) {
        val latestLicense = trialLicenseManager.refreshState()
        currentLicenseState = latestLicense
        if (!latestLicense.isBanglishUnlocked) {
            handleOpenUnlockScreen()
            return
        }
        if (selectedConverted.isNotEmpty()) {
            withSafeInputConnection { ic ->
                ic.commitText(selectedConverted, 1)
            }
            refreshSuggestions(immediate = true)
            return
        }
        if (cursorSource.isNotEmpty() && cursorConverted.isNotEmpty()) {
            withSafeInputConnection { ic ->
                ic.beginBatchEdit()
                try {
                    ic.deleteSurroundingText(cursorSource.length, 0)
                    ic.commitText(cursorConverted, 1)
                } finally {
                    runCatching { ic.endBatchEdit() }
                }
            }
            refreshSuggestions(immediate = true)
            return
        }
        if (clipConverted.isNotEmpty()) {
            handleCommitText("$clipConverted ")
            return
        }
        clipboardManager.captureCurrentPrimaryClip()
        val freshClip = clipboardManager.clips.value.firstOrNull().orEmpty()
        if (BengaliTransliterator.containsBengaliScript(freshClip)) {
            val converted = BengaliTransliterator.transliterate(freshClip, capitalizeSentences = true)
            handleCommitText("$converted ")
            return
        }
        startVoiceTyping(lockedContinuous = false)
    }

    private fun applyWordSuggestion(word: String, replaceTokenLength: Int) {
        syncVipVisualsQuietly()
        var before = ""
        val hadLiveBanglishComposing = banglishRawBengaliBuffer.isNotEmpty()
        val livePrefix = banglishWordPrefixContext
        withSafeInputConnection { ic ->
            before = runCatching {
                ic.getTextBeforeCursor(60, 0)?.toString().orEmpty()
            }.getOrDefault("")
            ic.beginBatchEdit()
            try {
                if (hadLiveBanglishComposing) {
                    banglishRawBengaliBuffer.clear()
                    banglishWordPrefixContext = ""
                    ic.commitText("$word ", 1)
                } else {
                    if (replaceTokenLength > 0) {
                        ic.deleteSurroundingText(replaceTokenLength, 0)
                    }
                    ic.commitText("$word ", 1)
                }
            } finally {
                runCatching { ic.endBatchEdit() }
            }
        }
        val prefixWithoutToken = if (hadLiveBanglishComposing) {
            livePrefix
        } else if (replaceTokenLength > 0 && before.length >= replaceTokenLength) {
            before.dropLast(replaceTokenLength)
        } else {
            before
        }
        if (currentSettings.smartSuggestionsEnabled) {
            val langId = currentSettings.activeLanguageId
            serviceScope.launch(Dispatchers.Default) {
                SmartWordPredictor.learnFromCompletedInput(
                    applicationContext,
                    "$prefixWithoutToken$word",
                    langId
                )
            }
        }
        lastSpaceTapTimeMs = 0L
        refreshSuggestions(immediate = true)
    }

    private fun handleRgbStickerChipTap(phrase: String) {
        val latestLicense = trialLicenseManager.refreshState()
        currentLicenseState = latestLicense
        if (!latestLicense.isRgbStickerUnlocked) {
            handleOpenUnlockScreen()
            return
        }
        pendingStickerPhrase = phrase
        tvStickerPhrasePreview?.text = "✨ \"$phrase\""
        tvStickerPhrasePreview?.typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD
        setOverlay(OverlayMode.STICKER_SELECTOR)
    }

    private fun styleBrandSignatureBadge(view: TextView?) {
        val tv = view ?: return
        val density = resources.displayMetrics.density
        tv.paint.shader = null
        tv.setTextColor(Color.WHITE)
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        tv.elevation = 2f * density

        val strokePx = (1.1f * density).toInt().coerceAtLeast(1)
        val gradientRing = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#FF2E63"),
                Color.parseColor("#00D4FF")
            )
        ).apply {
            cornerRadius = 999f * density
        }
        val obsidianFill = GradientDrawable().apply {
            setColor(Color.parseColor("#141824"))
            cornerRadius = 999f * density
        }
        val pillDrawable = LayerDrawable(arrayOf(gradientRing, obsidianFill)).apply {
            setLayerInset(1, strokePx, strokePx, strokePx, strokePx)
        }
        tv.background = pillDrawable
    }

    private fun styleVoiceStatusBar() {
        val density = resources.displayMetrics.density
        voiceBar?.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#1D0D2E"), Color.parseColor("#0E1D2F"))
        ).apply {
            cornerRadius = 10f * density
            setStroke((1.15f * density).toInt(), Color.parseColor("#00F5D4"))
        }
        tvVoiceLangBadge?.background = GradientDrawable().apply {
            setColor(Color.parseColor("#2B1648"))
            cornerRadius = 6f * density
            setStroke((1f * density).toInt(), Color.parseColor("#FF2A85"))
        }
    }

    private fun populateStickerStyleSelector() {
        val container = stickerStyleGridContainer ?: return
        container.removeAllViews()
        stickerCardViews.clear()
        val density = resources.displayMetrics.density

        // #1 Default Featured Option: Box-Free Transparent RGB Text Sticker (শুধুমাত্র রঙিন লেখা)
        val transparentStyle = RgbStickerStyle.TEXT_ONLY_TRANSPARENT
        val featuredCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (12 * density).toInt()
            val padV = (9 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#18122B"), Color.parseColor("#0D1F2C"))
            ).apply {
                cornerRadius = 12f * density
                setStroke((1.8f * density).toInt(), Color.parseColor("#00F5D4"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            setOnClickListener {
                feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                sendStyledRgbSticker(transparentStyle)
            }
        }
        val featuredTextCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val featuredTitle = TextView(this).apply {
            text = "${transparentStyle.badgeIcon} ${transparentStyle.displayName}"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#00F5D4"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val featuredSub = TextView(this).apply {
            text = transparentStyle.subtitle
            textSize = 11f
            typeface = bengaliTypeface ?: Typeface.DEFAULT
            setTextColor(Color.parseColor("#E2E8F0"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        featuredTextCol.addView(featuredTitle)
        featuredTextCol.addView(featuredSub)

        val defaultPill = TextView(this).apply {
            text = "DEFAULT"
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0A0D18"))
            gravity = Gravity.CENTER
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#00F5D4"), Color.parseColor("#FF2A85"))
            ).apply {
                cornerRadius = 999f * density
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * density).toInt()
            }
        }
        featuredCard.addView(featuredTextCol)
        featuredCard.addView(defaultPill)
        stickerCardViews.add(featuredCard)
        container.addView(featuredCard)

        // Optional 6 Framed Neon Box Styles in a clean 2-column grid
        val framedStyles = RgbStickerStyle.entries.filter { !it.isTransparentTextOnly }
        framedStyles.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (7 * density).toInt()
                }
            }
            pair.forEachIndexed { idx, style ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    val padH = (10 * density).toInt()
                    val padV = (9 * density).toInt()
                    setPadding(padH, padV, padH, padV)
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(style.cardBgColors.first(), style.cardBgColors.last())
                    ).apply {
                        cornerRadius = 12f * density
                        setStroke((1.6f * density).toInt(), style.accentPrimary)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (idx == 0) marginEnd = (7 * density).toInt()
                    }
                    setOnClickListener {
                        feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                        sendStyledRgbSticker(style)
                    }
                }

                val titleTv = TextView(this).apply {
                    text = "${style.badgeIcon} ${style.displayName}"
                    textSize = 12.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(style.accentPrimary)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                val subTv = TextView(this).apply {
                    text = style.subtitle
                    textSize = 10f
                    setTextColor(Color.parseColor("#CBD5E1"))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                card.addView(titleTv)
                card.addView(subTv)
                stickerCardViews.add(card)
                row.addView(card)
            }
            container.addView(row)
        }
    }

    private fun startStickerPulseAnimation() {
        stopStickerPulseAnimation()
        if (stickerCardViews.isEmpty()) return
        stickerPulseAnimator = ValueAnimator.ofFloat(0.84f, 1.0f).apply {
            duration = 950L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                stickerCardViews.forEachIndexed { index, card ->
                    val phaseAlpha = if (index % 2 == 0) value else (1.84f - value).coerceIn(0.84f, 1f)
                    card.alpha = phaseAlpha
                }
            }
            start()
        }
    }

    private fun stopStickerPulseAnimation() {
        stickerPulseAnimator?.cancel()
        stickerPulseAnimator = null
        stickerCardViews.forEach { it.alpha = 1f }
    }

    private fun sendStyledRgbSticker(style: RgbStickerStyle) {
        val phrase = pendingStickerPhrase.trim()
        if (phrase.isEmpty() || isGeneratingSticker) {
            setOverlay(OverlayMode.NONE)
            return
        }
        isGeneratingSticker = true
        setOverlay(OverlayMode.NONE)
        val editorInfoSnapshot = currentInputEditorInfo

        serviceScope.launch {
            try {
                val artifact = withContext(Dispatchers.Default) {
                    RgbStickerGenerator.generateSticker(
                        context = applicationContext,
                        rawPhrase = phrase,
                        style = style,
                        editorInfo = editorInfoSnapshot
                    )
                }
                if (artifact != null) {
                    val committedDirectly = RgbStickerGenerator.sendStickerToActiveApp(
                        context = this@RgbKeyboardService,
                        inputConnection = currentInputConnection,
                        editorInfo = currentInputEditorInfo,
                        artifact = artifact
                    )
                    if (committedDirectly) {
                        withSafeInputConnection { ic ->
                            val before = ic.getTextBeforeCursor(phrase.length + 2, 0)?.toString().orEmpty()
                            if (before.trimEnd().endsWith(phrase)) {
                                val charsToDelete = before.length - before.lastIndexOf(phrase)
                                if (charsToDelete > 0) {
                                    ic.deleteSurroundingText(charsToDelete, 0)
                                }
                            }
                        }
                        refreshSuggestions(immediate = true)
                    }
                    artifact.bitmap.recycle()
                }
            } finally {
                isGeneratingSticker = false
            }
        }
    }

    private fun styleAiTopicTypingBar() {
        val density = resources.displayMetrics.density
        aiTopicTypingBar?.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#1D0E30"), Color.parseColor("#0F1D30"))
        ).apply {
            cornerRadius = 10f * density
            setStroke((1.2f * density).toInt(), Color.parseColor("#FF2A85"))
        }
        btnAiTopicGenerateNow?.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#FF2A85"), Color.parseColor("#FF9E00"))
        ).apply {
            cornerRadius = 8f * density
        }
        btnAiTopicCancel?.background = GradientDrawable().apply {
            setColor(Color.parseColor("#33152A"))
            cornerRadius = 8f * density
        }
        tvAiTopicDraft?.typeface = bengaliTypeface ?: Typeface.DEFAULT
    }

    private fun styleAiHubChrome() {
        val density = resources.displayMetrics.density
        tvAiTopicBox?.apply {
            typeface = bengaliTypeface ?: Typeface.DEFAULT
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161028"))
                cornerRadius = 10f * density
                setStroke((1f * density).toInt(), Color.parseColor("#35285E"))
            }
        }
        btnAiGenerate?.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#FF2A85"), Color.parseColor("#FF9E00"))
        ).apply {
            cornerRadius = 10f * density
        }
        btnAiHubClose?.background = GradientDrawable().apply {
            setColor(Color.parseColor("#1E2235"))
            cornerRadius = 10f * density
        }
    }

    private fun enterAiTopicTypingMode() {
        isAiTopicTypingMode = true
        activeOverlay = OverlayMode.NONE
        aiHubPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.GONE
        stylishFontPanel?.visibility = View.GONE
        emojiPanel?.visibility = View.GONE
        stickerPanel?.visibility = View.GONE
        suggestionBar?.visibility = View.GONE
        aiTopicTypingBar?.visibility = View.VISIBLE
        keyboardView?.visibility = View.VISIBLE
        refreshAiTopicDraftViews()
        updateToolbarTints()
    }

    private fun handleAiTopicKeyInput(key: String) {
        when (key) {
            "DEL" -> {
                if (aiTopicDraft.isNotEmpty()) {
                    val lastCp = aiTopicDraft.codePointBefore(aiTopicDraft.length)
                    aiTopicDraft = aiTopicDraft.substring(0, aiTopicDraft.length - Character.charCount(lastCp))
                }
            }
            "ENTER" -> {
                isAiTopicTypingMode = false
                triggerAiShayariGeneration()
                setOverlay(OverlayMode.AI_HUB)
                return
            }
            " " -> {
                if (aiTopicDraft.isNotEmpty() && !aiTopicDraft.endsWith(" ") && aiTopicDraft.length < 28) {
                    aiTopicDraft += " "
                }
            }
            else -> {
                if (aiTopicDraft.length + key.length <= 28) {
                    aiTopicDraft += key
                }
            }
        }
        refreshAiTopicDraftViews()
    }

    private fun refreshAiTopicDraftViews() {
        val hint = getString(R.string.ai_hub_topic_hint)
        val hasDraft = aiTopicDraft.isNotBlank()
        tvAiTopicBox?.text = if (hasDraft) aiTopicDraft else hint
        tvAiTopicBox?.setTextColor(
            if (hasDraft) Color.parseColor("#F1F5FF") else Color.parseColor("#9AA4C4")
        )
        tvAiTopicDraft?.text = if (hasDraft) {
            "${getString(R.string.ai_hub_topic_mode_label)} $aiTopicDraft|"
        } else {
            "${getString(R.string.ai_hub_topic_mode_label)} $hint"
        }
    }

    private fun triggerAiShayariGeneration() {
        aiGenerationSeed += 1
        generatedAiResult = ShayariVault.generatePoeticShayaris(aiTopicDraft, aiGenerationSeed)
        expandedShayariIndex = null
        renderAiHubPanel()
        aiShayariScroll?.scrollTo(0, 0)
    }

    private fun renderAiHubPanel() {
        refreshAiTopicDraftViews()
        renderAiCategoryTabs()
        renderAiQuickTopicsOrBanner()
        renderAiShayariCards()
    }

    private fun renderAiCategoryTabs() {
        val container = aiCategoryTabs ?: return
        container.removeAllViews()
        val density = resources.displayMetrics.density

        ShayariCategory.entries.forEach { category ->
            val isSelected = generatedAiResult == null && category == selectedAiCategory
            val tab = TextView(this).apply {
                text = "${category.emoji} ${category.tabTitle}"
                textSize = 11.5f
                typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD
                if (isSelected) setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (isSelected) category.accentHex else Color.parseColor("#C4CCE0"))
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding((10 * density).toInt(), (3 * density).toInt(), (10 * density).toInt(), (3 * density).toInt())
                background = GradientDrawable().apply {
                    setColor(
                        if (isSelected) Color.argb(
                            54,
                            Color.red(category.accentHex),
                            Color.green(category.accentHex),
                            Color.blue(category.accentHex)
                        ) else Color.parseColor("#161028")
                    )
                    cornerRadius = 999f * density
                    setStroke(
                        (1.2f * density).toInt(),
                        if (isSelected) category.accentHex else Color.parseColor("#2B2248")
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (28 * density).toInt()
                ).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    selectedAiCategory = category
                    generatedAiResult = null
                    expandedShayariIndex = null
                    renderAiHubPanel()
                    aiShayariScroll?.scrollTo(0, 0)
                }
            }
            container.addView(tab)
        }
    }

    private fun renderAiQuickTopicsOrBanner() {
        val container = aiQuickTopicsContainer ?: return
        container.removeAllViews()
        val density = resources.displayMetrics.density

        val gen = generatedAiResult
        if (gen != null) {
            val backChip = TextView(this).apply {
                text = getString(R.string.ai_hub_back_to_vault)
                textSize = 10.5f
                typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#FF2A85"))
                gravity = Gravity.CENTER
                setPadding((9 * density).toInt(), (2 * density).toInt(), (9 * density).toInt(), (2 * density).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#2B112B"))
                    cornerRadius = 999f * density
                    setStroke((1f * density).toInt(), Color.parseColor("#FF2A85"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (24 * density).toInt()
                ).apply {
                    marginEnd = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    generatedAiResult = null
                    expandedShayariIndex = null
                    renderAiHubPanel()
                }
            }
            container.addView(backChip)

            val labelChip = TextView(this).apply {
                text = if (gen.usedFallbackTheme) {
                    "✨ কিউরেটেড থিম: ${gen.resolvedTopic}"
                } else {
                    "✨ বিষয়: ${gen.resolvedTopic} (৪টি শায়ারি)"
                }
                textSize = 11f
                typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#00F5D4"))
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (24 * density).toInt()
                )
            }
            container.addView(labelChip)
            return
        }

        ShayariVault.quickTopicSuggestions.forEach { topic ->
            val chip = TextView(this).apply {
                text = "✨ $topic"
                textSize = 10.5f
                typeface = bengaliTypeface ?: Typeface.DEFAULT
                setTextColor(Color.parseColor("#9AE6FF"))
                gravity = Gravity.CENTER
                setPadding((9 * density).toInt(), (2 * density).toInt(), (9 * density).toInt(), (2 * density).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#14192E"))
                    cornerRadius = 999f * density
                    setStroke((1f * density).toInt(), Color.parseColor("#273A5C"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (24 * density).toInt()
                ).apply {
                    marginEnd = (5 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                    aiTopicDraft = topic
                    triggerAiShayariGeneration()
                }
            }
            container.addView(chip)
        }
    }

    private fun renderAiShayariCards() {
        val container = aiShayariContainer ?: return
        container.removeAllViews()
        val density = resources.displayMetrics.density

        val gen = generatedAiResult
        val effectiveCategory = if (gen != null) ShayariCategory.ROMANTIC else selectedAiCategory
        val lines = gen?.lines ?: ShayariVault.linesForCategory(selectedAiCategory)

        lines.forEachIndexed { idx, line ->
            val isExpanded = expandedShayariIndex == idx
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val padH = (11 * density).toInt()
                val padV = (8 * density).toInt()
                setPadding(padH, padV, padH, padV)
                background = GradientDrawable().apply {
                    setColor(if (isExpanded) Color.parseColor("#1B1333") else Color.parseColor("#140F24"))
                    cornerRadius = 11f * density
                    setStroke(
                        ((if (isExpanded) 1.3f else 1f) * density).toInt().coerceAtLeast(1),
                        if (isExpanded) effectiveCategory.accentHex else Color.parseColor("#281F44")
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                    expandedShayariIndex = if (isExpanded) null else idx
                    renderAiShayariCards()
                }
            }

            val lineTv = TextView(this).apply {
                text = line
                textSize = 12.5f
                typeface = bengaliTypeface ?: Typeface.DEFAULT
                setTextColor(Color.parseColor("#F1F5FF"))
                setLineSpacing(2f * density, 1f)
            }
            card.addView(lineTv)

            if (isExpanded) {
                val variations = ShayariVault.buildVariations(line, effectiveCategory)
                val gridWrapper = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (7 * density).toInt()
                    }
                }
                variations.chunked(2).forEachIndexed { rowIdx, pair ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            if (rowIdx == 0) bottomMargin = (5 * density).toInt()
                        }
                    }
                    pair.forEachIndexed { colIdx, variation ->
                        val isDirect = variation.chipLabel == "সরাসরি পাঠান"
                        val chipTv = TextView(this).apply {
                            text = variation.chipLabel
                            textSize = 11f
                            typeface = bengaliTypeface ?: Typeface.DEFAULT_BOLD
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(if (isDirect) Color.WHITE else Color.parseColor("#00F5D4"))
                            gravity = Gravity.CENTER
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            setPadding(
                                (8 * density).toInt(),
                                (6 * density).toInt(),
                                (8 * density).toInt(),
                                (6 * density).toInt()
                            )
                            background = GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                if (isDirect) {
                                    intArrayOf(Color.parseColor("#FF2A85"), Color.parseColor("#FF9E00"))
                                } else {
                                    intArrayOf(Color.parseColor("#231842"), Color.parseColor("#182A3D"))
                                }
                            ).apply {
                                cornerRadius = 8f * density
                                if (!isDirect) {
                                    setStroke((1f * density).toInt(), Color.parseColor("#00F5D4"))
                                }
                            }
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {
                                if (colIdx == 0) marginEnd = (6 * density).toInt()
                            }
                            setOnClickListener {
                                feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                                handleCommitText("${variation.text} ")
                                expandedShayariIndex = null
                                renderAiShayariCards()
                            }
                        }
                        row.addView(chipTv)
                    }
                    gridWrapper.addView(row)
                }
                card.addView(gridWrapper)
            }

            container.addView(card)
        }
    }

    private fun populateStylishFontsPanel() {
        val container = stylishFontContainer ?: return
        container.removeAllViews()
        val density = resources.displayMetrics.density

        StylishFontStyle.entries.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (7 * density).toInt()
                }
            }
            pair.forEachIndexed { idx, style ->
                val selected = style == activeStylishFont
                val chip = TextView(this).apply {
                    text = style.label
                    setTextColor(if (selected) Color.parseColor("#00F5D4") else Color.parseColor("#EFF4FF"))
                    textSize = 13.5f
                    gravity = Gravity.CENTER
                    val padV = (10 * density).toInt()
                    setPadding((8 * density).toInt(), padV, (8 * density).toInt(), padV)
                    background = GradientDrawable().apply {
                        setColor(if (selected) Color.parseColor("#152D3D") else Color.parseColor("#1D202C"))
                        cornerRadius = 10f * density
                        setStroke(
                            ((if (selected) 1.4f else 1f) * density).toInt().coerceAtLeast(1),
                            if (selected) Color.parseColor("#00F5D4") else Color.parseColor("#2B3146")
                        )
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (idx == 0) marginEnd = (7 * density).toInt()
                    }
                    setOnClickListener {
                        feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                        activeStylishFont = style
                        updateToolbarTints()
                        setOverlay(OverlayMode.NONE)
                    }
                }
                row.addView(chip)
            }
            container.addView(row)
        }
    }

    private fun renderClipboardSnippets(snippets: List<String>) {
        val container = clipboardContainer ?: return
        val emptyView = tvClipboardEmpty
        container.removeAllViews()

        if (snippets.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
            return
        }
        emptyView?.visibility = View.GONE

        val density = resources.displayMetrics.density
        snippets.forEach { clip ->
            val card = TextView(this).apply {
                text = clip
                setTextColor(Color.parseColor("#EFF4FF"))
                textSize = 13f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                val padH = (12 * density).toInt()
                val padV = (10 * density).toInt()
                setPadding(padH, padV, padH, padV)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1D202C"))
                    cornerRadius = 10f * density
                    setStroke((1f * density).toInt(), Color.parseColor("#2B3146"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (6 * density).toInt()
                }
                setOnClickListener {
                    feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                    val outText = if (currentSettings.activeLanguageId == LanguageCatalog.BANGLISH_ID &&
                        BengaliTransliterator.containsBengaliScript(clip)
                    ) {
                        BengaliTransliterator.transliterate(clip, capitalizeSentences = true)
                    } else {
                        clip
                    }
                    handleCommitText(outText)
                    setOverlay(OverlayMode.NONE)
                }
            }
            container.addView(card)
        }
    }

    private fun updateKeyboardHeightForLanguage(langId: String) {
        val layoutLookupId = if (langId == LanguageCatalog.BANGLISH_ID) "bn" else langId
        val activeDyn = languagePackManager.loadLayoutFromDisk(layoutLookupId)
        val extraRowHeightDp = if ((activeDyn?.rows?.size ?: 4) >= 5) 26 else 0
        val targetHeightDp = when (currentSettings.heightPreset) {
            KeyboardHeightPreset.COMPACT -> 212 + extraRowHeightDp
            KeyboardHeightPreset.STANDARD -> 232 + extraRowHeightDp
            KeyboardHeightPreset.TALL -> 260 + extraRowHeightDp
        }
        val targetPx = (targetHeightDp * resources.displayMetrics.density).toInt()
        listOf(keyboardView, clipboardPanel, stylishFontPanel, emojiPanel, stickerPanel, trialLockPanel).forEach { view ->
            val lp = view?.layoutParams
            if (lp != null && lp.height != targetPx) {
                lp.height = targetPx
                view.layoutParams = lp
            }
        }
        val aiHubPx = (maxOf(targetHeightDp, 268) * resources.displayMetrics.density).toInt()
        aiHubPanel?.layoutParams?.let { lp ->
            if (lp.height != aiHubPx) {
                lp.height = aiHubPx
                aiHubPanel?.layoutParams = lp
            }
        }
    }

    private fun applySettingsToViews(settings: KeyboardSettings) {
        topBar?.visibility = if (settings.showToolbar) View.VISIBLE else View.GONE
        val effectiveSettings = if (!currentLicenseState.isBanglishUnlocked &&
            settings.activeLanguageId == LanguageCatalog.BANGLISH_ID
        ) {
            settings.copy(activeLanguageId = LanguageCatalog.ENGLISH_ID)
        } else {
            settings
        }
        currentSettings = effectiveSettings
        updateKeyboardHeightForLanguage(effectiveSettings.activeLanguageId)
        keyboardView?.updateSettings(effectiveSettings)
        applyTrialLockState()
        updateToolbarTints()
        refreshSuggestions(immediate = true)
    }

    private fun syncVipVisualsQuietly() {
        val now = SystemClock.uptimeMillis()
        if (now - lastVipSyncMs < VIP_SYNC_INTERVAL_MS) return
        lastVipSyncMs = now
        serviceScope.launch(Dispatchers.Default) {
            val state = trialLicenseManager.refreshState()
            withContext(Dispatchers.Main) {
                val changed = state.isRgbThemesUnlocked != currentLicenseState.isRgbThemesUnlocked ||
                    state.isBanglishUnlocked != currentLicenseState.isBanglishUnlocked ||
                    state.isAiHubUnlocked != currentLicenseState.isAiHubUnlocked ||
                    state.isStylishFontsUnlocked != currentLicenseState.isStylishFontsUnlocked ||
                    state.isRgbStickerUnlocked != currentLicenseState.isRgbStickerUnlocked ||
                    state.isCallBarUnlocked != currentLicenseState.isCallBarUnlocked
                currentLicenseState = state
                if (changed) {
                    syncTrialVisualStateFromService(state)
                    applyTrialLockState()
                }
            }
        }
    }

    private fun dismissOneTimeClipChips() {
        if (clipboardManager.hasFreshTextClip.value) {
            clipboardManager.dismissFreshTextClip()
        }
        if (clipboardManager.latestScreenshotClip.value != null) {
            clipboardManager.dismissScreenshotClip()
        }
    }

    private fun handleToggleLanguage() {
        val latestLicense = trialLicenseManager.refreshState()
        currentLicenseState = latestLicense
        val downloaded = languagePackManager.getDownloadedLanguageIds()
        val fullCycle = currentSettings.enabledLanguageIds
            .filter { LanguageCatalog.isBuiltInId(it) || it in downloaded }
            .ifEmpty { listOf(LanguageCatalog.ENGLISH_ID, LanguageCatalog.BANGLISH_ID, "bn", "hi") }
        val idx = fullCycle.indexOf(currentSettings.activeLanguageId)
        var nextLangId = fullCycle[(idx + 1) % fullCycle.size]

        // Banglish (BN-EN) is VIP-exclusive after trial unless overridden: open VIP Unlock and cycle free languages
        if (nextLangId == LanguageCatalog.BANGLISH_ID && !latestLicense.isBanglishUnlocked) {
            handleOpenUnlockScreen()
            val freeCycle = fullCycle.filter { it != LanguageCatalog.BANGLISH_ID }
                .ifEmpty { listOf(LanguageCatalog.ENGLISH_ID) }
            val freeIdx = freeCycle.indexOf(currentSettings.activeLanguageId)
            nextLangId = if (freeIdx >= 0) {
                freeCycle[(freeIdx + 1) % freeCycle.size]
            } else {
                freeCycle.first()
            }
        }

        // Switch active keyboard layout synchronously on the spot (zero lag waiting for DataStore disk I/O)
        currentSettings = currentSettings.copy(activeLanguageId = nextLangId)
        preferencesRepository.cacheActiveLanguageSync(nextLangId)
        updateKeyboardHeightForLanguage(nextLangId)
        keyboardView?.setActiveLanguage(nextLangId)
        refreshSuggestions(immediate = true)

        serviceScope.launch(Dispatchers.IO) {
            preferencesRepository.updateActiveLanguage(nextLangId)
        }
    }

    /**
     * Safely executes any InputConnection IPC call inside a null-checked try-catch guard
     * so dead/detached editor connections can NEVER crash or auto-close the keyboard.
     */
    private inline fun withSafeInputConnection(block: (InputConnection) -> Unit) {
        val ic = currentInputConnection ?: return
        try {
            block(ic)
        } catch (_: Throwable) {
            // Gracefully ignore detached InputConnection IPC failures
        }
    }

    private fun isBanglishVipActive(): Boolean {
        return currentSettings.activeLanguageId == LanguageCatalog.BANGLISH_ID &&
            currentLicenseState.isBanglishUnlocked
    }

    private fun clearBanglishBuffer(finishComposing: Boolean = false) {
        val hadBuffer = banglishRawBengaliBuffer.isNotEmpty()
        banglishRawBengaliBuffer.clear()
        banglishWordPrefixContext = ""
        if (finishComposing && hadBuffer) {
            withSafeInputConnection { it.finishComposingText() }
        }
    }

    /**
     * Flushes the live Bengali composing buffer (or any raw Bengali word right before the cursor)
     * into final natural Indian Romanized English (e.g., `তুমি` -> `Tumi`, `কেমন` -> `kemon`,
     * `আছো` -> `acho`) + optional [trailingText] (Space or normalized punctuation).
     * Guarantees raw Bengali script is NEVER left behind in Banglish (BN-EN) mode.
     */
    private fun flushBanglishBuffer(
        ic: InputConnection,
        trailingText: String = ""
    ): String? {
        if (!isBanglishVipActive()) {
            banglishRawBengaliBuffer.clear()
            banglishWordPrefixContext = ""
            return null
        }
        if (banglishRawBengaliBuffer.isNotEmpty()) {
            val rawBengali = banglishRawBengaliBuffer.toString()
            val prefix = banglishWordPrefixContext
            banglishRawBengaliBuffer.clear()
            banglishWordPrefixContext = ""

            val romanized = BengaliTransliterator.transliterateWordWithContext(
                bengaliWord = rawBengali,
                textBeforeWord = prefix
            )
            if (romanized.isNotEmpty()) {
                ic.commitText(romanized + trailingText, 1)
                return "$prefix$romanized"
            } else {
                ic.commitText(trailingText, 1)
                return null
            }
        }
        return autoTransliteratePendingBanglishWord(ic, trailingText)
    }

    /**
     * Fallback safety net: if any contiguous Bengali word sits right before the cursor
     * (e.g., pasted or pre-existing Bengali text), atomically replaces it with Romanized English.
     */
    private fun autoTransliteratePendingBanglishWord(
        ic: InputConnection,
        trailingText: String = ""
    ): String? {
        if (!isBanglishVipActive()) {
            return null
        }
        val before = runCatching {
            ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        }.getOrDefault("")
        val trailingBengali = BengaliTransliterator.extractTrailingBengaliWord(before)
        if (trailingBengali.isEmpty()) return null

        val prefixBeforeWord = before.dropLast(trailingBengali.length)
        val romanized = BengaliTransliterator.transliterateWordWithContext(
            bengaliWord = trailingBengali,
            textBeforeWord = prefixBeforeWord
        )
        if (romanized.isEmpty()) return null

        ic.beginBatchEdit()
        try {
            ic.deleteSurroundingText(trailingBengali.length, 0)
            ic.commitText(romanized + trailingText, 1)
        } finally {
            runCatching { ic.endBatchEdit() }
        }
        return "$prefixBeforeWord$romanized"
    }

    private fun handleCommitText(text: String) {
        syncVipVisualsQuietly()
        dismissOneTimeClipChips()
        val isBanglishVip = isBanglishVipActive()
        var learnInput: String? = null

        withSafeInputConnection { ic ->
            if (isBanglishVip) {
                when {
                    BengaliTransliterator.isBengaliWordText(text) -> {
                        if (banglishRawBengaliBuffer.isEmpty()) {
                            banglishWordPrefixContext = runCatching {
                                ic.getTextBeforeCursor(40, 0)?.toString().orEmpty()
                            }.getOrDefault("")
                        }
                        banglishRawBengaliBuffer.append(text)
                        val liveRomanized = BengaliTransliterator.transliterateWordWithContext(
                            bengaliWord = banglishRawBengaliBuffer.toString(),
                            textBeforeWord = banglishWordPrefixContext
                        )
                        if (liveRomanized.isNotEmpty()) {
                            ic.setComposingText(liveRomanized, 1)
                        }
                    }
                    BengaliTransliterator.isBengaliDigitText(text) -> {
                        flushBanglishBuffer(ic, trailingText = "")
                        ic.commitText(BengaliTransliterator.convertBengaliDigits(text), 1)
                    }
                    BengaliTransliterator.isAutoTransliterateTriggerPunctuation(text) -> {
                        val normalizedPunct = BengaliTransliterator.normalizeBanglishPunctuation(text)
                        val convertedContext = flushBanglishBuffer(ic, trailingText = normalizedPunct)
                        if (convertedContext != null) {
                            learnInput = convertedContext
                        } else {
                            ic.commitText(normalizedPunct, 1)
                        }
                    }
                    else -> {
                        flushBanglishBuffer(ic, trailingText = "")
                        val cleanText = if (BengaliTransliterator.containsBengaliScript(text)) {
                            val before = runCatching {
                                ic.getTextBeforeCursor(40, 0)?.toString().orEmpty()
                            }.getOrDefault("")
                            BengaliTransliterator.transliterate(
                                text,
                                capitalizeSentences = BengaliTransliterator.isSentenceStartContext(before)
                            )
                        } else {
                            text
                        }
                        ic.commitText(cleanText, 1)
                    }
                }
            } else {
                clearBanglishBuffer(finishComposing = false)
                ic.commitText(text, 1)
            }
        }
        learnInput?.let { textToLearn ->
            if (currentSettings.smartSuggestionsEnabled) {
                serviceScope.launch(Dispatchers.Default) {
                    SmartWordPredictor.learnFromCompletedInput(
                        applicationContext,
                        textToLearn,
                        LanguageCatalog.BANGLISH_ID
                    )
                }
            }
        }
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    private fun handleBackspace() {
        syncVipVisualsQuietly()
        dismissOneTimeClipChips()
        withSafeInputConnection { ic ->
            if (isBanglishVipActive() && banglishRawBengaliBuffer.isNotEmpty()) {
                banglishRawBengaliBuffer.deleteCharAt(banglishRawBengaliBuffer.length - 1)
                if (banglishRawBengaliBuffer.isNotEmpty()) {
                    val liveRomanized = BengaliTransliterator.transliterateWordWithContext(
                        bengaliWord = banglishRawBengaliBuffer.toString(),
                        textBeforeWord = banglishWordPrefixContext
                    )
                    ic.setComposingText(liveRomanized, 1)
                } else {
                    banglishWordPrefixContext = ""
                    ic.commitText("", 1)
                }
                return@withSafeInputConnection
            }
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
                if (before.length >= 2 && Character.isSurrogatePair(before[before.length - 2], before[before.length - 1])) {
                    ic.deleteSurroundingText(2, 0)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
        }
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    /**
     * Whole-word accelerated backspace triggered when holding `⌫` for >= 1.2s.
     * Deletes trailing whitespace plus the previous word in one clean batch.
     */
    private fun handleDeletePreviousWord() {
        syncVipVisualsQuietly()
        dismissOneTimeClipChips()
        withSafeInputConnection { ic ->
            if (isBanglishVipActive() && banglishRawBengaliBuffer.isNotEmpty()) {
                banglishRawBengaliBuffer.clear()
                banglishWordPrefixContext = ""
                ic.commitText("", 1)
                return@withSafeInputConnection
            }
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
                if (before.isEmpty()) return@withSafeInputConnection
                var idx = before.length
                // Skip trailing spaces first
                while (idx > 0 && before[idx - 1].isWhitespace()) {
                    idx--
                }
                // Then consume the preceding word characters
                while (idx > 0 && !before[idx - 1].isWhitespace()) {
                    idx--
                }
                val countToDelete = (before.length - idx).coerceAtLeast(1)
                ic.deleteSurroundingText(countToDelete, 0)
            }
        }
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    private fun handleSpace() {
        syncVipVisualsQuietly()
        dismissOneTimeClipChips()
        var learnInput: String? = null
        withSafeInputConnection { ic ->
            val now = System.currentTimeMillis()
            if (currentSettings.doubleSpacePeriod &&
                banglishRawBengaliBuffer.isEmpty() &&
                (now - lastSpaceTapTimeMs) in 40L..420L
            ) {
                val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
                if (before.length >= 2 && before.last() == ' ' && !before[before.length - 2].isWhitespace()) {
                    val punct = if (currentSettings.activeLanguageId == LanguageCatalog.BANGLISH_ID) {
                        "."
                    } else {
                        keyboardView?.getActiveDynamicLayout()?.sentenceEndPunct ?: "."
                    }
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText("$punct ", 1)
                    lastSpaceTapTimeMs = 0L
                    refreshSuggestions()
                    return@withSafeInputConnection
                }
            }

            // In Banglish (BN-EN) mode, commit live Romanized English word + Space (NEVER leaves raw Bengali behind)
            val convertedContext = flushBanglishBuffer(ic, trailingText = " ")
            if (convertedContext != null) {
                if (currentSettings.smartSuggestionsEnabled) {
                    learnInput = convertedContext
                }
                lastSpaceTapTimeMs = now
                return@withSafeInputConnection
            }

            val beforeContext = runCatching {
                ic.getTextBeforeCursor(60, 0)?.toString().orEmpty()
            }.getOrDefault("")
            if (currentSettings.smartSuggestionsEnabled && beforeContext.isNotBlank() && !beforeContext.last().isWhitespace()) {
                learnInput = beforeContext
            }
            ic.commitText(" ", 1)
            lastSpaceTapTimeMs = now
        }
        learnInput?.let { textToLearn ->
            val langId = currentSettings.activeLanguageId
            serviceScope.launch(Dispatchers.Default) {
                SmartWordPredictor.learnFromCompletedInput(applicationContext, textToLearn, langId)
            }
        }
        refreshSuggestions()
    }

    private fun handleEnter() {
        syncVipVisualsQuietly()
        dismissOneTimeClipChips()
        var learnInput: String? = null
        withSafeInputConnection { ic ->
            // Flush & transliterate any live or pending Bengali word in Banglish (BN-EN) mode before Enter/Send
            val convertedContext = flushBanglishBuffer(ic, trailingText = "")
            val beforeContext = convertedContext ?: runCatching {
                ic.getTextBeforeCursor(60, 0)?.toString().orEmpty()
            }.getOrDefault("")
            if (currentSettings.smartSuggestionsEnabled && beforeContext.isNotBlank()) {
                learnInput = beforeContext
            }
            val info = currentInputEditorInfo
            val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
            val isNoEnterAction = (info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0

            if (!isNoEnterAction && actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(actionId)
            } else {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }
        learnInput?.let { textToLearn ->
            val langId = currentSettings.activeLanguageId
            serviceScope.launch(Dispatchers.Default) {
                SmartWordPredictor.learnFromCompletedInput(applicationContext, textToLearn, langId)
            }
        }
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    private fun handleCursorMove(steps: Int) {
        syncVipVisualsQuietly()
        withSafeInputConnection { ic ->
            val keyCode = if (steps > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
            repeat(kotlin.math.abs(steps).coerceAtMost(4)) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }
    }

    private fun handleOpenSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_KEYBOARD_SETTINGS, true)
        }
        runCatching { startActivity(intent) }
    }

    private fun handleOpenThemeCustomizer() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_THEME_CUSTOMIZER, true)
        }
        runCatching { startActivity(intent) }
    }

    private fun handleOpenUnlockScreen() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_UNLOCK_SCREEN, true)
        }
        runCatching { startActivity(intent) }
    }

    private fun styleCallBannerBar() {
        val density = resources.displayMetrics.density
        callBannerBar?.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#0D1530"), Color.parseColor("#1D1036"))
        ).apply {
            cornerRadius = 10f * density
            setStroke((1.2f * density).toInt(), Color.parseColor("#00F5D4"))
        }
        btnCallReceive?.background = GradientDrawable().apply {
            setColor(Color.parseColor("#0D9458"))
            cornerRadius = 999f * density
            setStroke((1f * density).toInt(), Color.parseColor("#34D399"))
        }
        btnCallEnd?.background = GradientDrawable().apply {
            setColor(Color.parseColor("#C81E4C"))
            cornerRadius = 999f * density
            setStroke((1f * density).toInt(), Color.parseColor("#FF6B8A"))
        }
    }

    @Suppress("DEPRECATION")
    private fun registerCallStateListenerIfPermitted() {
        if (callListenerRegistered) return
        val hasPhoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return

        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleTelephonyCallStateChanged(state)
                    }
                }
                tm.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallbackObj = callback
            } else {
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleTelephonyCallStateChanged(state)
                    }
                }
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
            }
            callListenerRegistered = true
        }
    }

    @Suppress("DEPRECATION")
    private fun unregisterCallStateListener() {
        if (!callListenerRegistered) return
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallbackObj as? TelephonyCallback)?.let { cb ->
                    tm?.unregisterTelephonyCallback(cb)
                }
                telephonyCallbackObj = null
            } else {
                legacyPhoneStateListener?.let { listener ->
                    tm?.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
                legacyPhoneStateListener = null
            }
        }
        callListenerRegistered = false
    }

    private fun handleTelephonyCallStateChanged(state: Int) {
        uiHandler.post {
            val prevState = currentCallState
            currentCallState = state
            if (state == TelephonyManager.CALL_STATE_OFFHOOK && prevState != TelephonyManager.CALL_STATE_OFFHOOK) {
                callOffhookStartMs = SystemClock.elapsedRealtime()
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                callOffhookStartMs = 0L
            }
            if (state == TelephonyManager.CALL_STATE_OFFHOOK &&
                (SystemClock.elapsedRealtime() - lastKeyboardCallAcceptMs) in 0L..3_500L
            ) {
                uiHandler.post { ensureKeyboardStaysFocusedAfterCallAccept() }
            }
            updateCallBannerVisibility()
        }
    }

    /**
     * Strict Only-While-Typing Guard:
     * Never shows call bar or opens keyboard when the user is on the Home screen / lock screen / outside an active editor.
     */
    private fun isActivelyTyping(): Boolean {
        val editorInfo = currentInputEditorInfo ?: return false
        if (currentInputConnection == null) return false
        val inputClass = editorInfo.inputType and InputType.TYPE_MASK_CLASS
        return isInputViewShown && isTypingWindowVisible && inputClass != InputType.TYPE_NULL
    }

    private fun updateCallBannerVisibility() {
        val bar = callBannerBar ?: return
        uiHandler.removeCallbacks(callTimerRunnable)

        val shouldShow = isActivelyTyping() &&
            currentLicenseState.isCallBarUnlocked &&
            currentCallState != TelephonyManager.CALL_STATE_IDLE

        if (!shouldShow) {
            bar.visibility = View.GONE
            return
        }

        bar.visibility = View.VISIBLE
        when (currentCallState) {
            TelephonyManager.CALL_STATE_RINGING -> {
                tvCallStatus?.text = "📞 Incoming Call..."
                tvCallStatus?.setTextColor(Color.parseColor("#00F5D4"))
                btnCallReceive?.visibility = View.VISIBLE
                btnCallEnd?.visibility = View.VISIBLE
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (callOffhookStartMs <= 0L) {
                    callOffhookStartMs = SystemClock.elapsedRealtime()
                }
                tvCallStatus?.setTextColor(Color.parseColor("#34D399"))
                btnCallReceive?.visibility = View.GONE
                btnCallEnd?.visibility = View.VISIBLE
                uiHandler.post(callTimerRunnable)
            }
            else -> {
                bar.visibility = View.GONE
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun answerIncomingCall() {
        if (!isActivelyTyping()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
        lastKeyboardCallAcceptMs = SystemClock.elapsedRealtime()
        val accepted = runCatching { telecomManager.acceptRingingCall() }.isSuccess
        if (accepted) {
            // Immediately transition keyboard call bar to Active Timer and keep typing editor focused
            currentCallState = TelephonyManager.CALL_STATE_OFFHOOK
            if (callOffhookStartMs <= 0L) {
                callOffhookStartMs = SystemClock.elapsedRealtime()
            }
            updateCallBannerVisibility()
            uiHandler.postDelayed({ ensureKeyboardStaysFocusedAfterCallAccept() }, 120L)
            uiHandler.postDelayed({ ensureKeyboardStaysFocusedAfterCallAccept() }, 350L)
            uiHandler.postDelayed({ ensureKeyboardStaysFocusedAfterCallAccept() }, 700L)
            uiHandler.postDelayed({ ensureKeyboardStaysFocusedAfterCallAccept() }, 1200L)
            uiHandler.postDelayed({ ensureKeyboardStaysFocusedAfterCallAccept() }, 1900L)
            uiHandler.postDelayed({ ensureKeyboardStaysFocusedAfterCallAccept() }, 2600L)
        }
    }

    private fun ensureKeyboardStaysFocusedAfterCallAccept() {
        val elapsedSinceAccept = SystemClock.elapsedRealtime() - lastKeyboardCallAcceptMs
        if (elapsedSinceAccept > 3_500L) return
        val editorInfo = currentInputEditorInfo ?: return
        val inputClass = editorInfo.inputType and InputType.TYPE_MASK_CLASS
        if (currentInputConnection != null && inputClass != InputType.TYPE_NULL) {
            isTypingWindowVisible = true
            if (!isInputViewShown) {
                val shown = runCatching {
                    @Suppress("DEPRECATION")
                    requestShowSelf(android.view.inputmethod.InputMethodManager.SHOW_FORCED)
                    true
                }.getOrDefault(false)
                if (!shown) {
                    runCatching { requestShowSelf(0) }
                }
            }
            updateCallBannerVisibility()
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun endActiveCall() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
        runCatching { telecomManager.endCall() }
    }

    companion object {
        private const val PREFS_PERMISSIONS = "rakib_permissions_prefs"
        private const val KEY_MIC_PROMPTED_ONCE = "mic_permission_prompted_once"
        private const val VOICE_LOCK_HOLD_MS = 600L
        private const val VIP_SYNC_INTERVAL_MS = 30_000L
        private const val BACKSPACE_INITIAL_DELAY_MS = 300L
        private const val BACKSPACE_CHAR_INTERVAL_MS = 40L
        private const val BACKSPACE_WORD_ACCEL_MS = 1200L
        private const val BACKSPACE_WORD_INTERVAL_MS = 85L
    }
}
