package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.data.ClipboardHistoryManager
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.KeyboardPreferencesRepository
import com.example.data.LanguagePackManager
import com.example.data.LicenseUiState
import com.example.data.TrialLicenseManager
import com.example.engine.MechanicalFeedbackEngine
import com.example.engine.OnlineVoiceTypingController
import com.example.engine.RgbStickerGenerator
import com.example.engine.SmartWordPredictor
import com.example.model.EmojiCatalog
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardSettings
import com.example.model.LanguageCatalog
import com.example.model.RgbEffect
import com.example.rgbkeyboard.RgbKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private var topBar: View? = null
    private var voiceBar: View? = null
    private var tvVoiceLangBadge: TextView? = null
    private var tvVoiceStatus: TextView? = null
    private var btnVoiceStop: Button? = null
    private var suggestionBar: HorizontalScrollView? = null
    private var suggestionContainer: LinearLayout? = null

    private var keyboardView: RgbKeyboardView? = null
    private var trialLockPanel: View? = null
    private var tvImeLockDeviceId: TextView? = null
    private var clipboardPanel: View? = null
    private var clipboardContainer: LinearLayout? = null
    private var tvClipboardEmpty: TextView? = null
    private var reactionsPanel: View? = null
    private var reactionsContainer: LinearLayout? = null

    private var emojiPanel: View? = null
    private var emojiCategoryTabs: LinearLayout? = null
    private var emojiGridView: GridView? = null
    private var tvEmojiCategoryTitle: TextView? = null
    private var btnEmojiClose: Button? = null
    private var btnEmojiBackspace: Button? = null
    private var emojiGridAdapter: EmojiGridAdapter? = null
    private var selectedEmojiCategoryIndex: Int = 0
    private var isEmojiBackspaceRepeating: Boolean = false

    private var btnMenu: ImageButton? = null
    private var btnEmoji: ImageButton? = null
    private var btnGif: ImageButton? = null
    private var btnTheme: ImageButton? = null
    private var btnMic: ImageButton? = null

    private var currentSettings: KeyboardSettings = KeyboardSettings()
    private var currentPhotoTheme: CustomPhotoTheme = CustomPhotoTheme()
    private var currentLicenseState: LicenseUiState = LicenseUiState()
    private var activeOverlay: OverlayMode = OverlayMode.NONE
    private var lastSpaceTapTimeMs: Long = 0L
    private var latestVoiceState = OnlineVoiceTypingController.VoiceListeningState()
    private var hasActiveVoiceComposing: Boolean = false
    private var isGeneratingSticker: Boolean = false
    private var bengaliTypeface: Typeface? = null

    private enum class OverlayMode { NONE, CLIPBOARD, REACTIONS, EMOJI }

    private val quickReactions = listOf(
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

    private val emojiBackspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isEmojiBackspaceRepeating) {
                handleBackspace()
                feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
                uiHandler.postDelayed(this, 65L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = KeyboardPreferencesRepository.getInstance(applicationContext)
        languagePackManager = LanguagePackManager.getInstance(applicationContext)
        customBackgroundManager = CustomBackgroundManager.getInstance(applicationContext)
        trialLicenseManager = TrialLicenseManager.getInstance(applicationContext)
        clipboardManager = ClipboardHistoryManager.getInstance(applicationContext)
        feedbackEngine = MechanicalFeedbackEngine(applicationContext)
        currentPhotoTheme = customBackgroundManager.getCurrentTheme()
        currentLicenseState = trialLicenseManager.refreshState()

        bengaliTypeface = runCatching {
            ResourcesCompat.getFont(applicationContext, R.font.noto_sans_bengali)
        }.getOrNull()

        voiceController = OnlineVoiceTypingController(
            context = applicationContext,
            onStateChanged = { state ->
                latestVoiceState = state
                uiHandler.post { renderVoiceState(state) }
            },
            onPartialText = { partial ->
                uiHandler.post {
                    val ic = currentInputConnection
                    if (ic != null && partial.isNotBlank()) {
                        hasActiveVoiceComposing = true
                        ic.setComposingText(partial, 1)
                        refreshSuggestions()
                    }
                }
            },
            onFinalText = { spokenText ->
                uiHandler.post {
                    hasActiveVoiceComposing = false
                    handleCommitText("$spokenText ")
                }
            }
        )

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
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)

        topBar = root.findViewById(R.id.topBar)
        voiceBar = root.findViewById(R.id.voiceBar)
        tvVoiceLangBadge = root.findViewById(R.id.tvVoiceLangBadge)
        tvVoiceStatus = root.findViewById(R.id.tvVoiceStatus)
        btnVoiceStop = root.findViewById(R.id.btnVoiceStop)
        suggestionBar = root.findViewById(R.id.suggestionBar)
        suggestionContainer = root.findViewById(R.id.suggestionContainer)

        btnMenu = root.findViewById(R.id.btnMenu)
        btnEmoji = root.findViewById(R.id.btnEmoji)
        btnGif = root.findViewById(R.id.btnGif)
        val btnSettings: ImageButton? = root.findViewById(R.id.btnSettings)
        btnTheme = root.findViewById(R.id.btnTheme)
        btnMic = root.findViewById(R.id.btnMic)

        clipboardPanel = root.findViewById(R.id.clipboardPanel)
        clipboardContainer = root.findViewById(R.id.clipboardContainer)
        tvClipboardEmpty = root.findViewById(R.id.tvClipboardEmpty)
        val btnClipboardClear: Button? = root.findViewById(R.id.btnClipboardClear)
        val btnClipboardClose: Button? = root.findViewById(R.id.btnClipboardClose)

        reactionsPanel = root.findViewById(R.id.reactionsPanel)
        reactionsContainer = root.findViewById(R.id.reactionsContainer)
        val btnReactionsClose: Button? = root.findViewById(R.id.btnReactionsClose)

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
            when (key) {
                "DEL" -> handleBackspace()
                "ENTER" -> handleEnter()
                " " -> handleSpace()
                else -> handleCommitText(key)
            }
        }
        rgbView.onCursorMove = { steps -> handleCursorMove(steps) }
        rgbView.onKeyFeedback = { isModifier ->
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier)
        }
        rgbView.onLanguageToggleRequested = {
            handleToggleLanguage()
        }
        rgbView.onEmojiOpenRequested = {
            setOverlay(OverlayMode.EMOJI)
        }

        // 1. Feature Menu (4-dots grid) -> Opens Clipboard Manager
        btnMenu?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            clipboardManager.captureCurrentPrimaryClip()
            setOverlay(if (activeOverlay == OverlayMode.CLIPBOARD) OverlayMode.NONE else OverlayMode.CLIPBOARD)
        }

        // 2. Emoji / Sticker icon -> Opens Unlimited Categorized Emoji Panel
        btnEmoji?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(if (activeOverlay == OverlayMode.EMOJI) OverlayMode.NONE else OverlayMode.EMOJI)
        }

        // 3. GIF / Quick Reactions icon
        btnGif?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(if (activeOverlay == OverlayMode.REACTIONS) OverlayMode.NONE else OverlayMode.REACTIONS)
        }

        // 4. Settings gear
        btnSettings?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            handleOpenSettings()
        }

        // 5. Theme Palette icon: cycles RGB lighting when VIP is active, or opens ₹20 VIP Unlock when in free standard mode
        btnTheme?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            val latestLicense = trialLicenseManager.refreshState()
            currentLicenseState = latestLicense
            if (latestLicense.isVipActive) {
                serviceScope.launch { preferencesRepository.cycleEffect() }
            } else {
                handleOpenUnlockScreen()
            }
        }

        // 6. Voice Mic icon -> Direct Same-Language SpeechRecognizer (bn-BD / en-IN / hi-IN)
        btnMic?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            handleOnlineVoiceTyping()
        }

        btnVoiceStop?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            if (hasActiveVoiceComposing) {
                currentInputConnection?.setComposingText("", 1)
                currentInputConnection?.finishComposingText()
                hasActiveVoiceComposing = false
            }
            voiceController.stopListening()
            voiceBar?.visibility = View.GONE
            updateToolbarTints()
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
        btnReactionsClose?.setOnClickListener {
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
                    uiHandler.postDelayed(emojiBackspaceRepeatRunnable, 380L)
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
        populateReactionsPanel()
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
        clipboardManager.captureCurrentPrimaryClip()
        syncSystemSubtype()
        val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        keyboardView?.setSearchAction(actionId == EditorInfo.IME_ACTION_SEARCH)
        keyboardView?.updatePhotoTheme(customBackgroundManager.getCurrentTheme())
        currentLicenseState = trialLicenseManager.refreshState()
        applyTrialLockState()
        refreshSuggestions()
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
        refreshSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (hasActiveVoiceComposing) {
            currentInputConnection?.finishComposingText()
            hasActiveVoiceComposing = false
        }
        if (::voiceController.isInitialized && voiceController.isCurrentlyListening()) {
            voiceController.stopListening()
        }
        voiceBar?.visibility = View.GONE
        isEmojiBackspaceRepeating = false
        uiHandler.removeCallbacks(emojiBackspaceRepeatRunnable)
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        val locale = newSubtype?.locale
        if (!locale.isNullOrBlank()) {
            val langId = LanguageCatalog.idFromLocale(locale)
            if (langId == LanguageCatalog.ENGLISH_ID || languagePackManager.isDownloaded(langId)) {
                if (langId != currentSettings.activeLanguageId) {
                    serviceScope.launch { preferencesRepository.updateActiveLanguage(langId) }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isEmojiBackspaceRepeating = false
        uiHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        if (::voiceController.isInitialized) {
            voiceController.destroy()
        }
        if (::feedbackEngine.isInitialized) {
            feedbackEngine.release()
        }
        topBar = null
        voiceBar = null
        tvVoiceLangBadge = null
        tvVoiceStatus = null
        btnVoiceStop = null
        suggestionBar = null
        suggestionContainer = null
        keyboardView = null
        trialLockPanel = null
        tvImeLockDeviceId = null
        clipboardPanel = null
        clipboardContainer = null
        tvClipboardEmpty = null
        reactionsPanel = null
        reactionsContainer = null
        emojiPanel = null
        emojiCategoryTabs = null
        emojiGridView = null
        tvEmojiCategoryTitle = null
        btnEmojiClose = null
        btnEmojiBackspace = null
        emojiGridAdapter = null
        btnMenu = null
        btnEmoji = null
        btnGif = null
        btnTheme = null
        btnMic = null
    }

    /**
     * Never locks out basic typing! During the 7-day VIP trial or after ₹20 Lifetime VIP unlock,
     * enables Touch Spark RGB, continuous animations, custom photo/banner backgrounds, and RGB Text Stickers.
     * After 7 days without unlock, seamlessly switches to Classic Black (No RGB) free mode
     * while keeping Bangla/English/Hindi typing, Voice Typing, Smart Suggestions, Emojis, and Caps Lock active.
     */
    private fun applyTrialLockState() {
        trialLockPanel?.visibility = View.GONE
        topBar?.visibility = if (currentSettings.showToolbar) View.VISIBLE else View.GONE
        suggestionBar?.visibility = if (activeOverlay == OverlayMode.NONE) View.VISIBLE else View.GONE
        keyboardView?.setVipVisualsEnabled(currentLicenseState.isVipActive)
        updateToolbarTints()
        refreshSuggestions()
    }

    private fun setOverlay(mode: OverlayMode) {
        activeOverlay = mode
        trialLockPanel?.visibility = View.GONE
        clipboardPanel?.visibility = if (mode == OverlayMode.CLIPBOARD) View.VISIBLE else View.GONE
        reactionsPanel?.visibility = if (mode == OverlayMode.REACTIONS) View.VISIBLE else View.GONE
        emojiPanel?.visibility = if (mode == OverlayMode.EMOJI) View.VISIBLE else View.GONE
        keyboardView?.visibility = if (mode == OverlayMode.NONE) View.VISIBLE else View.GONE
        suggestionBar?.visibility = if (mode == OverlayMode.NONE) View.VISIBLE else View.GONE
        if (mode == OverlayMode.EMOJI) {
            btnEmojiClose?.text = if (currentSettings.activeLanguageId == "bn") "কখগ" else "ABC"
        }
        updateToolbarTints()
    }

    private fun updateToolbarTints() {
        val defaultTint = Color.parseColor("#D5DCEF")
        val activePink = Color.parseColor("#FF2A85")
        val activeCyan = Color.parseColor("#00F5D4")
        val activeVoice = Color.parseColor("#00F59B")

        btnMenu?.setColorFilter(if (activeOverlay == OverlayMode.CLIPBOARD) activeCyan else defaultTint, PorterDuff.Mode.SRC_IN)
        btnEmoji?.setColorFilter(if (activeOverlay == OverlayMode.EMOJI) activePink else defaultTint, PorterDuff.Mode.SRC_IN)
        btnGif?.setColorFilter(if (activeOverlay == OverlayMode.REACTIONS) activeCyan else defaultTint, PorterDuff.Mode.SRC_IN)
        btnTheme?.setColorFilter(
            if (currentLicenseState.isVipActive &&
                currentSettings.effect != RgbEffect.NORMAL_DEFAULT &&
                currentSettings.effect != RgbEffect.TOUCH_SPARK
            ) activePink else defaultTint,
            PorterDuff.Mode.SRC_IN
        )
        btnMic?.setColorFilter(if (latestVoiceState.isListening) activeVoice else defaultTint, PorterDuff.Mode.SRC_IN)
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
                textSize = 23f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                minHeight = (38 * density).toInt()
                setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
            }
            val emoji = items[position]
            tv.text = emoji
            tv.setOnClickListener { onEmojiTap(emoji) }
            return tv
        }
    }

    private fun handleOnlineVoiceTyping() {
        syncVipVisualsQuietly()
        if (voiceController.isCurrentlyListening()) {
            if (hasActiveVoiceComposing) {
                currentInputConnection?.finishComposingText()
                hasActiveVoiceComposing = false
            }
            voiceController.stopListening()
            voiceBar?.visibility = View.GONE
            updateToolbarTints()
            return
        }

        if (!voiceController.hasAudioPermission()) {
            voiceBar?.visibility = View.VISIBLE
            tvVoiceStatus?.text = getString(R.string.voice_permission_needed)
            tvVoiceStatus?.setTextColor(Color.parseColor("#FFB300"))
            launchAudioPermissionRequest()
            return
        }

        val activeLocale = OnlineVoiceTypingController.localeForLanguageId(currentSettings.activeLanguageId)
        voiceBar?.visibility = View.VISIBLE
        voiceController.startListening(activeLocale)
    }

    private fun renderVoiceState(state: OnlineVoiceTypingController.VoiceListeningState) {
        updateToolbarTints()
        tvVoiceLangBadge?.text = state.locale.badge

        when {
            state.isListening -> {
                voiceBar?.visibility = View.VISIBLE
                tvVoiceStatus?.setTextColor(Color.parseColor("#00F5D4"))
                tvVoiceStatus?.text = when {
                    state.partialTranscript.isNotBlank() -> "🎙 ${state.partialTranscript}"
                    state.locale.isBengali -> "🎙 ${getString(R.string.voice_listening_bn)}"
                    state.locale.isHindi -> "🎙 ${getString(R.string.voice_listening_hi)}"
                    else -> "🎙 ${getString(R.string.voice_listening_en)}"
                }
            }
            !state.errorMessage.isNullOrBlank() -> {
                if (hasActiveVoiceComposing) {
                    currentInputConnection?.finishComposingText()
                    hasActiveVoiceComposing = false
                }
                voiceBar?.visibility = View.VISIBLE
                tvVoiceStatus?.setTextColor(Color.parseColor("#FFB300"))
                tvVoiceStatus?.text = state.errorMessage
            }
            else -> {
                if (hasActiveVoiceComposing) {
                    currentInputConnection?.finishComposingText()
                    hasActiveVoiceComposing = false
                }
                voiceBar?.visibility = View.GONE
            }
        }
    }

    private fun launchAudioPermissionRequest() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_REQUEST_AUDIO_PERMISSION, true)
        }
        runCatching { startActivity(intent) }
    }

    private fun refreshSuggestions() {
        val container = suggestionContainer ?: return
        val before = runCatching {
            currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        }.getOrDefault("")

        val prediction = SmartWordPredictor.predict(before, currentSettings.activeLanguageId)
        val density = resources.displayMetrics.density
        container.removeAllViews()

        // 1. Glowing "✨ RGB Sticker" chip (or "🔒 VIP • RGB Sticker" after 7-day trial expires)
        if (prediction.stickerPhrase.isNotEmpty()) {
            val isVip = currentLicenseState.isVipActive
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

        // 2. Smart Word Prediction & Next-Word Suggestion Chips
        prediction.suggestions.forEach { suggestion ->
            val wordChip = TextView(this).apply {
                text = suggestion
                textSize = 13f
                typeface = bengaliTypeface ?: Typeface.DEFAULT
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

    private fun applyWordSuggestion(word: String, replaceTokenLength: Int) {
        syncVipVisualsQuietly()
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        try {
            if (replaceTokenLength > 0) {
                ic.deleteSurroundingText(replaceTokenLength, 0)
            }
            ic.commitText("$word ", 1)
        } finally {
            ic.endBatchEdit()
        }
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    private fun handleRgbStickerChipTap(phrase: String) {
        val latestLicense = trialLicenseManager.refreshState()
        currentLicenseState = latestLicense
        if (!latestLicense.isVipActive) {
            handleOpenUnlockScreen()
            return
        }
        if (isGeneratingSticker) return
        isGeneratingSticker = true

        serviceScope.launch {
            try {
                val artifact = withContext(Dispatchers.Default) {
                    RgbStickerGenerator.generateSticker(applicationContext, phrase)
                }
                if (artifact != null) {
                    val committedDirectly = RgbStickerGenerator.sendStickerToActiveApp(
                        context = this@RgbKeyboardService,
                        inputConnection = currentInputConnection,
                        editorInfo = currentInputEditorInfo,
                        artifact = artifact
                    )
                    if (committedDirectly) {
                        val ic = currentInputConnection
                        if (ic != null) {
                            val before = ic.getTextBeforeCursor(phrase.length + 2, 0)?.toString().orEmpty()
                            if (before.trimEnd().endsWith(phrase)) {
                                val charsToDelete = before.length - before.lastIndexOf(phrase)
                                if (charsToDelete > 0) {
                                    ic.deleteSurroundingText(charsToDelete, 0)
                                }
                            }
                        }
                        refreshSuggestions()
                    }
                    artifact.bitmap.recycle()
                }
            } finally {
                isGeneratingSticker = false
            }
        }
    }

    private fun populateReactionsPanel() {
        val container = reactionsContainer ?: return
        container.removeAllViews()
        val density = resources.displayMetrics.density

        quickReactions.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (6 * density).toInt()
                }
            }
            pair.forEachIndexed { idx, (label, output) ->
                val chip = TextView(this).apply {
                    text = label
                    setTextColor(Color.parseColor("#EFF4FF"))
                    textSize = 13f
                    gravity = Gravity.CENTER
                    val padV = (11 * density).toInt()
                    setPadding((8 * density).toInt(), padV, (8 * density).toInt(), padV)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#1D202C"))
                        cornerRadius = 10f * density
                        setStroke((1f * density).toInt(), Color.parseColor("#2B3146"))
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (idx == 0) marginEnd = (6 * density).toInt()
                    }
                    setOnClickListener {
                        feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = false)
                        handleCommitText(output)
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
                    handleCommitText(clip)
                    setOverlay(OverlayMode.NONE)
                }
            }
            container.addView(card)
        }
    }

    private fun applySettingsToViews(settings: KeyboardSettings) {
        topBar?.visibility = if (settings.showToolbar) View.VISIBLE else View.GONE
        val activeDyn = languagePackManager.loadLayoutFromDisk(settings.activeLanguageId)
        val extraRowHeightDp = if ((activeDyn?.rows?.size ?: 4) >= 5) 26 else 0
        val targetHeightDp = when (settings.heightPreset) {
            KeyboardHeightPreset.COMPACT -> 212 + extraRowHeightDp
            KeyboardHeightPreset.STANDARD -> 232 + extraRowHeightDp
            KeyboardHeightPreset.TALL -> 260 + extraRowHeightDp
        }
        val targetPx = (targetHeightDp * resources.displayMetrics.density).toInt()
        listOf(keyboardView, clipboardPanel, reactionsPanel, emojiPanel, trialLockPanel).forEach { view ->
            val lp = view?.layoutParams
            if (lp != null && lp.height != targetPx) {
                lp.height = targetPx
                view.layoutParams = lp
            }
        }
        keyboardView?.updateSettings(settings)
        applyTrialLockState()
        updateToolbarTints()
    }

    private fun syncVipVisualsQuietly() {
        val state = trialLicenseManager.refreshState()
        if (state.isVipActive != currentLicenseState.isVipActive) {
            currentLicenseState = state
            applyTrialLockState()
        } else {
            currentLicenseState = state
        }
    }

    private fun handleToggleLanguage() {
        syncVipVisualsQuietly()
        val downloaded = languagePackManager.getDownloadedLanguageIds()
        serviceScope.launch {
            val nextLangId = preferencesRepository.cycleActiveLanguage(downloaded)
            keyboardView?.setActiveLanguage(nextLangId)
            refreshSuggestions()
        }
    }

    private fun handleCommitText(text: String) {
        syncVipVisualsQuietly()
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    private fun handleBackspace() {
        syncVipVisualsQuietly()
        val ic = currentInputConnection ?: return
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
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    private fun handleSpace() {
        syncVipVisualsQuietly()
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        if (currentSettings.doubleSpacePeriod && (now - lastSpaceTapTimeMs) in 40L..420L) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before.length >= 2 && before.last() == ' ' && !before[before.length - 2].isWhitespace()) {
                val punct = keyboardView?.getActiveDynamicLayout()?.sentenceEndPunct ?: "."
                ic.deleteSurroundingText(1, 0)
                ic.commitText("$punct ", 1)
                lastSpaceTapTimeMs = 0L
                refreshSuggestions()
                return
            }
        }
        ic.commitText(" ", 1)
        lastSpaceTapTimeMs = now
        refreshSuggestions()
    }

    private fun handleEnter() {
        syncVipVisualsQuietly()
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val isNoEnterAction = (info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0

        if (!isNoEnterAction && actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        lastSpaceTapTimeMs = 0L
        refreshSuggestions()
    }

    private fun handleCursorMove(steps: Int) {
        syncVipVisualsQuietly()
        val ic = currentInputConnection ?: return
        val keyCode = if (steps > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(steps).coerceAtMost(4)) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    private fun handleOpenSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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

    private fun syncSystemSubtype() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        val activeSubtype = runCatching { imm.currentInputMethodSubtype }.getOrNull()
        val locale = activeSubtype?.locale
        if (!locale.isNullOrBlank()) {
            val detectedId = LanguageCatalog.idFromLocale(locale)
            if ((detectedId == LanguageCatalog.ENGLISH_ID || languagePackManager.isDownloaded(detectedId)) &&
                detectedId != currentSettings.activeLanguageId
            ) {
                serviceScope.launch { preferencesRepository.updateActiveLanguage(detectedId) }
            }
        }
    }
}
