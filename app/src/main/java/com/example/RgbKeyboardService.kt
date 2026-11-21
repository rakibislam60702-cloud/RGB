package com.example

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.ClipboardHistoryManager
import com.example.data.KeyboardPreferencesRepository
import com.example.data.LanguagePackManager
import com.example.engine.MechanicalFeedbackEngine
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

/**
 * Full Android InputMethodService powering Pro RGB Keyboard system-wide.
 * Supports dynamic JSON language layouts from `filesDir/layouts/` + Gboard 7-icon toolbar + Touch Spark RGB.
 */
class RgbKeyboardService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var preferencesRepository: KeyboardPreferencesRepository
    private lateinit var languagePackManager: LanguagePackManager
    private lateinit var clipboardManager: ClipboardHistoryManager
    private lateinit var feedbackEngine: MechanicalFeedbackEngine

    private var topBar: View? = null
    private var keyboardView: RgbKeyboardView? = null
    private var clipboardPanel: View? = null
    private var clipboardContainer: LinearLayout? = null
    private var tvClipboardEmpty: TextView? = null
    private var reactionsPanel: View? = null
    private var reactionsContainer: LinearLayout? = null

    private var btnMenu: ImageButton? = null
    private var btnEmoji: ImageButton? = null
    private var btnGif: ImageButton? = null
    private var btnLang: ImageButton? = null
    private var btnTheme: ImageButton? = null

    private var currentSettings: KeyboardSettings = KeyboardSettings()
    private var activeOverlay: OverlayMode = OverlayMode.NONE
    private var lastSpaceTapTimeMs: Long = 0L

    private enum class OverlayMode { NONE, CLIPBOARD, REACTIONS }

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
        "বাংলাদেশ 🇧🇩" to "বাংলাদেশ 🇧🇩"
    )

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = KeyboardPreferencesRepository.getInstance(applicationContext)
        languagePackManager = LanguagePackManager.getInstance(applicationContext)
        clipboardManager = ClipboardHistoryManager.getInstance(applicationContext)
        feedbackEngine = MechanicalFeedbackEngine(applicationContext)

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
            clipboardManager.clips.collect { snippets ->
                renderClipboardSnippets(snippets)
            }
        }
    }

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)

        topBar = root.findViewById(R.id.topBar)
        btnMenu = root.findViewById(R.id.btnMenu)
        btnEmoji = root.findViewById(R.id.btnEmoji)
        btnGif = root.findViewById(R.id.btnGif)
        val btnSettings: ImageButton? = root.findViewById(R.id.btnSettings)
        btnLang = root.findViewById(R.id.btnLang)
        btnTheme = root.findViewById(R.id.btnTheme)
        val btnMic: ImageButton? = root.findViewById(R.id.btnMic)

        clipboardPanel = root.findViewById(R.id.clipboardPanel)
        clipboardContainer = root.findViewById(R.id.clipboardContainer)
        tvClipboardEmpty = root.findViewById(R.id.tvClipboardEmpty)
        val btnClipboardClear: Button? = root.findViewById(R.id.btnClipboardClear)
        val btnClipboardClose: Button? = root.findViewById(R.id.btnClipboardClose)

        reactionsPanel = root.findViewById(R.id.reactionsPanel)
        reactionsContainer = root.findViewById(R.id.reactionsContainer)
        val btnReactionsClose: Button? = root.findViewById(R.id.btnReactionsClose)

        val rgbView = root.findViewById<RgbKeyboardView>(R.id.rgbKeyboardView)
        keyboardView = rgbView

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
        rgbView.onEmojiModeChanged = { isOpen ->
            updateToolbarTints(isEmojiOpen = isOpen)
        }

        // 1. Feature Menu (4-dots grid) -> Opens Clipboard Manager
        btnMenu?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            clipboardManager.captureCurrentPrimaryClip()
            if (rgbView.isEmojiModeOpen()) rgbView.toggleEmojiMode()
            setOverlay(if (activeOverlay == OverlayMode.CLIPBOARD) OverlayMode.NONE else OverlayMode.CLIPBOARD)
        }

        // 2. Emoji / Sticker icon
        btnEmoji?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(OverlayMode.NONE)
            rgbView.toggleEmojiMode()
        }

        // 3. GIF / Quick Reactions icon
        btnGif?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            if (rgbView.isEmojiModeOpen()) rgbView.toggleEmojiMode()
            setOverlay(if (activeOverlay == OverlayMode.REACTIONS) OverlayMode.NONE else OverlayMode.REACTIONS)
        }

        // 4. Settings gear
        btnSettings?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            handleOpenSettings()
        }

        // 5. Language / Translate icon (toggles English <-> বাংলা)
        btnLang?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            setOverlay(OverlayMode.NONE)
            handleToggleLanguage()
        }

        // 6. Theme Palette icon (cycles Touch Spark & RGB studio effects)
        btnTheme?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            serviceScope.launch { preferencesRepository.cycleEffect() }
        }

        // 7. Voice Mic icon (switches to Android Voice Typing IME or recognizer)
        btnMic?.setOnClickListener {
            feedbackEngine.triggerKeyFeedback(currentSettings, isModifier = true)
            handleVoiceTyping()
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

        populateReactionsPanel()
        applySettingsToViews(currentSettings)
        renderClipboardSnippets(clipboardManager.clips.value)
        updateToolbarTints(isEmojiOpen = rgbView.isEmojiModeOpen())

        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        clipboardManager.captureCurrentPrimaryClip()
        syncSystemSubtype()
        val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        keyboardView?.setSearchAction(actionId == EditorInfo.IME_ACTION_SEARCH)
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
        serviceScope.cancel()
        if (::feedbackEngine.isInitialized) {
            feedbackEngine.release()
        }
        topBar = null
        keyboardView = null
        clipboardPanel = null
        clipboardContainer = null
        tvClipboardEmpty = null
        reactionsPanel = null
        reactionsContainer = null
        btnMenu = null
        btnEmoji = null
        btnGif = null
        btnLang = null
        btnTheme = null
    }

    private fun setOverlay(mode: OverlayMode) {
        activeOverlay = mode
        clipboardPanel?.visibility = if (mode == OverlayMode.CLIPBOARD) View.VISIBLE else View.GONE
        reactionsPanel?.visibility = if (mode == OverlayMode.REACTIONS) View.VISIBLE else View.GONE
        keyboardView?.visibility = if (mode == OverlayMode.NONE) View.VISIBLE else View.GONE
        updateToolbarTints(isEmojiOpen = keyboardView?.isEmojiModeOpen() == true)
    }

    private fun updateToolbarTints(isEmojiOpen: Boolean) {
        val defaultTint = Color.parseColor("#D5DCEF")
        val activeBlue = Color.parseColor("#4285F4")
        val activeCyan = Color.parseColor("#00E5FF")

        btnMenu?.setColorFilter(if (activeOverlay == OverlayMode.CLIPBOARD) activeBlue else defaultTint, PorterDuff.Mode.SRC_IN)
        btnEmoji?.setColorFilter(if (isEmojiOpen) activeBlue else defaultTint, PorterDuff.Mode.SRC_IN)
        btnGif?.setColorFilter(if (activeOverlay == OverlayMode.REACTIONS) activeCyan else defaultTint, PorterDuff.Mode.SRC_IN)
        btnLang?.setColorFilter(if (currentSettings.activeLanguageId != LanguageCatalog.ENGLISH_ID) activeCyan else defaultTint, PorterDuff.Mode.SRC_IN)
        btnTheme?.setColorFilter(if (currentSettings.effect != RgbEffect.TOUCH_SPARK) activeCyan else defaultTint, PorterDuff.Mode.SRC_IN)
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
                    gravity = android.view.Gravity.CENTER
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
        listOf(keyboardView, clipboardPanel, reactionsPanel).forEach { view ->
            val lp = view?.layoutParams
            if (lp != null && lp.height != targetPx) {
                lp.height = targetPx
                view.layoutParams = lp
            }
        }
        keyboardView?.updateSettings(settings)
        updateToolbarTints(isEmojiOpen = keyboardView?.isEmojiModeOpen() == true)
    }

    private fun handleToggleLanguage() {
        val downloaded = languagePackManager.getDownloadedLanguageIds()
        serviceScope.launch {
            val nextLangId = preferencesRepository.cycleActiveLanguage(downloaded)
            keyboardView?.setActiveLanguage(nextLangId)
        }
    }

    private fun handleVoiceTyping() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        // Prefer switching to installed system Voice Typing IME if available
        val voiceIme = runCatching {
            imm?.enabledInputMethodList?.firstOrNull { info ->
                info.id.contains("voice", ignoreCase = true) ||
                    info.serviceName.contains("voice", ignoreCase = true)
            }
        }.getOrNull()

        if (voiceIme != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { switchInputMethod(voiceIme.id) }
            return
        }

        val activeLocale = LanguageCatalog.findById(currentSettings.activeLanguageId)?.locale
            ?.replace('_', '-')
            ?: "en-US"

        // Otherwise open standard Android speech recognizer intent or IME switcher
        val started = runCatching {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLocale)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        }.getOrDefault(false)

        if (!started) {
            runCatching { imm?.showInputMethodPicker() }
        }
    }

    private fun handleCommitText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        lastSpaceTapTimeMs = 0L
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        lastSpaceTapTimeMs = 0L
    }

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        if (currentSettings.doubleSpacePeriod && (now - lastSpaceTapTimeMs) in 40L..420L) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before.length >= 2 && before.last() == ' ' && !before[before.length - 2].isWhitespace()) {
                val punct = keyboardView?.getActiveDynamicLayout()?.sentenceEndPunct ?: "."
                ic.deleteSurroundingText(1, 0)
                ic.commitText("$punct ", 1)
                lastSpaceTapTimeMs = 0L
                return
            }
        }
        ic.commitText(" ", 1)
        lastSpaceTapTimeMs = now
    }

    private fun handleEnter() {
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
    }

    private fun handleCursorMove(steps: Int) {
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
