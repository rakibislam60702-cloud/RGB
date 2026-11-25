package com.example.rgbkeyboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.data.LanguagePackManager
import com.example.engine.RgbLightingEngine
import com.example.keyboard.KeyboardLayoutData
import com.example.model.DynamicKeyboardLayout
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.LanguageCatalog
import com.example.model.RgbEffect
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Gboard-styled custom Canvas Keyboard View fused with Samsung Keys Cafe Touch Spark RGB,
 * persistent custom photo backgrounds, proper Shift/Caps Lock, and dynamic JSON language packs.
 */
class RgbKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onKeyAction: ((String) -> Unit)? = null
    var onDeleteWordAction: (() -> Unit)? = null
    var onKeyFeedback: ((Boolean) -> Unit)? = null
    var onEmojiModeChanged: ((Boolean) -> Unit)? = null
    var onEmojiOpenRequested: (() -> Unit)? = null
    var onLanguageToggleRequested: (() -> Unit)? = null
    var onCursorMove: ((Int) -> Unit)? = null
    var allowTopOverflow: Boolean = true

    private enum class ShiftMode { OFF, ONCE, CAPS_LOCK }

    private val languagePackManager = LanguagePackManager.getInstance(context)
    private val customBgManager = CustomBackgroundManager.getInstance(context)
    private var activeLanguageId: String = LanguageCatalog.ENGLISH_ID
    private var activeDynamicLayout: DynamicKeyboardLayout? = null
    private var isKarBarActive: Boolean = false

    private var shiftMode: ShiftMode = ShiftMode.OFF
    private var lastShiftTapMs: Long = 0L
    private var isSymbols = false
    private var isNumberPad = false
    private var isEmojiMode = false
    private var isSearchAction = false
    private var settings: KeyboardSettings = KeyboardSettings()
    /**
     * When true (24h VIP Trial, Lifetime VIP Unlocked, or in-app Live Demo Preview), renders full
     * Touch Spark / continuous RGB lighting and custom photo / banner backgrounds.
     * When false (Forever Free Standard Keyboard on real system IME after 24h), renders a clean,
     * fast standard matte-dark keyboard with RGB lighting and photo/banner themes turned OFF.
     */
    private var vipVisualsEnabled: Boolean = true

    // Persistent Custom Photo & Built-in Banner Preset Background cache
    private var photoTheme: CustomPhotoTheme = customBgManager.getCurrentTheme()
    private var cachedPhotoBitmap: Bitmap? = null
    private var cachedPhotoKey: String = ""
    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val photoDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val photoSrcRect = Rect()
    private val photoDstRect = RectF()
    private val shiftArrowPath = Path()
    private val shiftFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var rgbPhase = 0f
    private var pressedKeyBox: KeyBox? = null
    private var committedOnDown: Boolean = false
    private var longPressTriggered: Boolean = false
    private var pressedEmoji: String? = null
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var lastTouchX = 0f
    private var spaceDragAccum = 0f
    private var isSpaceDragging = false
    private var isGliding = false

    private val handler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private var deleteHoldStartMs: Long = 0L
    private var lastInteractionMs: Long = SystemClock.uptimeMillis()
    private var isWindowPaused: Boolean = false

    private val particleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val particleStateLock = Any()
    private data class ParticleSnapshot(
        val glows: List<ActiveKeyGlow> = emptyList(),
        val trails: List<TrailPoint> = emptyList()
    )
    private val particleSnapshotRef = AtomicReference(ParticleSnapshot())
    @Volatile
    private var lastParticlePruneMs: Long = 0L

    private data class KeyBox(
        val id: String,
        val rawKey: String,
        val displayLabel: String,
        val subLabel: String?,
        val longPressOutput: String?,
        val rect: RectF,
        val neonColor: Int,
        val isKarBarKey: Boolean = false
    )

    private data class EmojiBox(val emoji: String, val rect: RectF)

    private data class ActiveKeyGlow(
        val keyId: String,
        var displayLabel: String,
        val rect: RectF,
        val neonColor: Int,
        val pressedAtMs: Long,
        var releasedAtMs: Long? = null,
        val showPopup: Boolean
    )

    private data class TrailPoint(
        val x: Float,
        val y: Float,
        val timeMs: Long,
        val color: Int
    )

    // Built-in lightweight 4-Row Gboard English QWERTY layout (non-English layouts load dynamically from JSON)
    private val enRows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("SHIFT", "Z", "X", "C", "V", "B", "N", "M", "DEL"),
        listOf("?123", ",", "GLOBE", "SPACE", "1234", ".", "ENTER")
    )
    private val enTopRowHints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    private val symRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "৳", "_", "&", "-", "+", "(", ")", "/"),
        listOf("=\\<", "*", "\"", "'", ":", ";", "!", "?", "DEL"),
        listOf("ABC", ",", "GLOBE", "SPACE", "1234", ".", "ENTER")
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val keyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val puddingInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#181B25")
        style = Paint.Style.FILL
    }
    private val rgbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
    }
    private val rgbFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sparkFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sparkBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
    }
    private val blueActionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4285F4")
    }
    private val vectorIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#E3E8F4")
    }
    private val normalTypeface: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val boldTypeface: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val bengaliTypeface: Typeface = runCatching {
        ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
    }.getOrNull() ?: normalTypeface

    private val popupBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val popupHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = boldTypeface
    }
    private val trailOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val trailCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F1F5FF")
        textAlign = Paint.Align.CENTER
        typeface = normalTypeface
    }
    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8C95AD")
        textAlign = Paint.Align.RIGHT
        typeface = normalTypeface
        textSize = 9.5f * scaledDensity
    }

    private val shaderMatrix = Matrix()
    private var cachedShader: SweepGradient? = null
    private var cachedShaderW = 0
    private var cachedShaderH = 0
    private var cachedPaletteOrdinal = -1

    private val keyBoxes = mutableListOf<KeyBox>()
    private val emojiBoxes = mutableListOf<EmojiBox>()
    private val activeGlows = mutableListOf<ActiveKeyGlow>()
    private val trailPoints = mutableListOf<TrailPoint>()
    private var emojiBackRect = RectF()
    private var emojiDelRect = RectF()
    private val tempRect = RectF()
    private val bubbleRect = RectF()

    init {
        if (!isInEditMode) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }
    }

    private val rgbAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3500L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val nowMs = SystemClock.uptimeMillis()
            if (isWindowPaused || !isAttachedToWindow) {
                if (isRunning) pause()
                return@addUpdateListener
            }
            val idleMs = nowMs - lastInteractionMs
            val activeTouchOrEffects = pressedKeyBox != null || isDeleting || hasActiveVisualEffects()
            // Pause animation loop after 1.5s of idle time to keep CPU/GPU cool and prevent IME stalls
            if (idleMs >= IDLE_PAUSE_MS && !activeTouchOrEffects) {
                if (isRunning) pause()
                return@addUpdateListener
            }
            rgbPhase = it.animatedValue as Float
            if (activeTouchOrEffects && nowMs - lastParticlePruneMs >= 32L) {
                scheduleParticlePrune(nowMs)
            }
            val needsContinuousRgb = vipVisualsEnabled &&
                settings.effect != RgbEffect.NORMAL_DEFAULT &&
                settings.effect != RgbEffect.TOUCH_SPARK
            if (needsContinuousRgb || activeTouchOrEffects) {
                postInvalidateOnAnimation()
            }
        }
    }

    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (!isDeleting) return
            val nowMs = SystemClock.uptimeMillis()
            lastInteractionMs = nowMs
            val heldMs = nowMs - deleteHoldStartMs
            onKeyFeedback?.invoke(true)
            if (heldMs >= DELETE_WORD_ACCEL_MS && onDeleteWordAction != null) {
                onDeleteWordAction?.invoke()
                handler.postDelayed(this, DELETE_WORD_REPEAT_INTERVAL_MS)
            } else {
                onKeyAction?.invoke("DEL")
                handler.postDelayed(this, DELETE_CHAR_REPEAT_INTERVAL_MS)
            }
        }
    }

    private val longPressRunnable = Runnable {
        val active = pressedKeyBox ?: return@Runnable
        if (isGliding || isSpaceDragging) return@Runnable

        if (active.rawKey == "SHIFT") {
            // Long-press on Shift locks Caps Lock ON immediately
            longPressTriggered = true
            shiftMode = if (shiftMode == ShiftMode.CAPS_LOCK) ShiftMode.OFF else ShiftMode.CAPS_LOCK
            if (settings.hapticEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onKeyFeedback?.invoke(true)
            if (shiftMode != ShiftMode.OFF) isKarBarActive = false
            rebuildKeyBoxes()
            invalidate()
            return@Runnable
        }

        if (active.rawKey == "," || active.rawKey == "،") {
            // Gboard Comma/Emoji long-press opens full categorized Emoji panel
            longPressTriggered = true
            if (settings.hapticEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onKeyFeedback?.invoke(true)
            releaseCurrentGlow(SystemClock.uptimeMillis())
            pressedKeyBox = null
            if (onEmojiOpenRequested != null) {
                onEmojiOpenRequested?.invoke()
            } else {
                toggleEmojiMode()
            }
            return@Runnable
        }

        val alt = active.longPressOutput ?: return@Runnable
        longPressTriggered = true
        if (settings.hapticEnabled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        // Because normal keys commit immediately on ACTION_DOWN for zero-lag typing,
        // swap the just-committed base character cleanly with its long-press alternate!
        if (committedOnDown) {
            onKeyAction?.invoke("DEL")
            onKeyAction?.invoke(alt)
        }
        updateGlowLabelAsync(active.id, alt)
        postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resumeAnimations()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pauseAnimations()
        particleScope.coroutineContext.cancelChildren()
    }

    fun pauseAnimations() {
        isWindowPaused = true
        if (rgbAnimator.isStarted || rgbAnimator.isRunning) {
            rgbAnimator.cancel()
        }
        isDeleting = false
        committedOnDown = false
        pressedKeyBox = null
        pressedEmoji = null
        handler.removeCallbacks(deleteRunnable)
        handler.removeCallbacks(longPressRunnable)
    }

    fun resumeAnimations() {
        isWindowPaused = false
        wakeAnimator()
    }

    private fun wakeAnimator() {
        lastInteractionMs = SystemClock.uptimeMillis()
        if (isWindowPaused || !isAttachedToWindow) return
        if (!rgbAnimator.isStarted) {
            rgbAnimator.start()
        } else if (rgbAnimator.isPaused) {
            rgbAnimator.resume()
        }
    }

    fun setSearchAction(isSearch: Boolean) {
        if (isSearchAction != isSearch) {
            isSearchAction = isSearch
            invalidate()
        }
    }

    /**
     * Switches between the dedicated Gboard-style 3x4 Number Pad (auto-opened for
     * `TYPE_CLASS_NUMBER` / `TYPE_CLASS_PHONE` fields or `1234` key tap) and normal keyboard.
     */
    fun setNumberPadMode(enabled: Boolean) {
        if (isNumberPad != enabled || (enabled && (isSymbols || isEmojiMode))) {
            isNumberPad = enabled
            if (enabled) {
                isSymbols = false
                isEmojiMode = false
                isKarBarActive = false
                onEmojiModeChanged?.invoke(false)
            }
            pressedKeyBox = null
            rebuildKeyBoxes()
            wakeAnimator()
            invalidate()
        }
    }

    fun isNumberPadOpen(): Boolean = isNumberPad

    fun setVipVisualsEnabled(enabled: Boolean) {
        if (vipVisualsEnabled != enabled) {
            vipVisualsEnabled = enabled
            if (!enabled) {
                clearTrailPointsAsync()
            }
            invalidate()
        }
    }

    fun updatePhotoTheme(newTheme: CustomPhotoTheme) {
        photoTheme = newTheme
        reloadPhotoBitmapIfNeeded()
        invalidate()
    }

    private fun reloadPhotoBitmapIfNeeded() {
        val key = photoTheme.cacheKey
        if (key == cachedPhotoKey) return
        cachedPhotoKey = key
        cachedPhotoBitmap?.recycle()
        cachedPhotoBitmap = if (photoTheme.hasPhoto) {
            customBgManager.decodeCachedBitmap(photoTheme)
        } else {
            null
        }
    }

    fun setActiveLanguage(langId: String) {
        val loaded = when {
            langId == LanguageCatalog.BANGLISH_ID -> languagePackManager.loadLayoutFromDisk("bn")
            LanguageCatalog.isBuiltInId(langId) -> null
            else -> languagePackManager.loadLayoutFromDisk(langId)
        }
        val resolvedId = when {
            langId == LanguageCatalog.BANGLISH_ID -> LanguageCatalog.BANGLISH_ID
            loaded != null -> loaded.id
            else -> LanguageCatalog.ENGLISH_ID
        }
        val changed = resolvedId != activeLanguageId || loaded != activeDynamicLayout
        activeLanguageId = resolvedId
        activeDynamicLayout = loaded
        if (changed) {
            isSymbols = false
            isEmojiMode = false
            shiftMode = ShiftMode.OFF
            isKarBarActive = false
            clearAllParticlesAsync()
            onEmojiModeChanged?.invoke(false)
            rebuildKeyBoxes()
            wakeAnimator()
            invalidate()
        }
    }

    fun cycleDownloadedLanguage(allowVipLanguages: Boolean = vipVisualsEnabled): String {
        val downloaded = languagePackManager.getDownloadedLanguageIds()
        val cycleList = settings.enabledLanguageIds
            .filter {
                (LanguageCatalog.isBuiltInId(it) || it in downloaded) &&
                    (allowVipLanguages || it != LanguageCatalog.BANGLISH_ID)
            }
            .ifEmpty { listOf(LanguageCatalog.ENGLISH_ID) }
        val idx = cycleList.indexOf(activeLanguageId)
        val nextId = cycleList[(idx + 1) % cycleList.size]
        setActiveLanguage(nextId)
        return activeLanguageId
    }

    fun getActiveLanguageId(): String = activeLanguageId

    fun getActiveDynamicLayout(): DynamicKeyboardLayout? = activeDynamicLayout

    fun toggleEmojiMode() {
        isEmojiMode = !isEmojiMode
        pressedKeyBox = null
        pressedEmoji = null
        onEmojiModeChanged?.invoke(isEmojiMode)
        invalidate()
    }

    fun isEmojiModeOpen(): Boolean = isEmojiMode

    fun updateSettings(newSettings: KeyboardSettings) {
        val speedChanged = newSettings.animationSpeed != settings.animationSpeed
        val paletteChanged = newSettings.palette != settings.palette
        settings = newSettings

        if (paletteChanged) cachedPaletteOrdinal = -1
        if (speedChanged) {
            val targetDuration = (3500f / newSettings.animationSpeed.coerceIn(0.2f, 2.5f)).toLong()
            rgbAnimator.duration = targetDuration.coerceIn(900L, 14000L)
        }

        // Sync custom photo theme if updated in SharedPreferences
        photoTheme = customBgManager.getCurrentTheme()
        reloadPhotoBitmapIfNeeded()

        // Always verify dynamic JSON layout matches the requested activeLanguageId (handles fresh downloads too)
        setActiveLanguage(newSettings.activeLanguageId)
        wakeAnimator()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildKeyBoxes()
    }

    private fun hasActiveVisualEffects(): Boolean {
        val snap = particleSnapshotRef.get()
        return pressedKeyBox != null || snap.glows.isNotEmpty() || snap.trails.isNotEmpty()
    }

    private val isShifted: Boolean
        get() = shiftMode != ShiftMode.OFF

    private fun numberPadRows(): List<List<String>> {
        // Strictly Bengali mode ("bn") displays & commits Bengali digits; English ("en") & Banglish ("bn_en") use English digits.
        val useBengaliDigits = activeLanguageId == "bn"
        val d1 = if (useBengaliDigits) "১" else "1"
        val d2 = if (useBengaliDigits) "২" else "2"
        val d3 = if (useBengaliDigits) "৩" else "3"
        val d4 = if (useBengaliDigits) "৪" else "4"
        val d5 = if (useBengaliDigits) "৫" else "5"
        val d6 = if (useBengaliDigits) "৬" else "6"
        val d7 = if (useBengaliDigits) "৭" else "7"
        val d8 = if (useBengaliDigits) "৮" else "8"
        val d9 = if (useBengaliDigits) "৯" else "9"
        val d0 = if (useBengaliDigits) "০" else "0"
        return listOf(
            listOf("+", d1, d2, d3, "%"),
            listOf("-", d4, d5, d6, "NUM_SPACE"),
            listOf("*", d7, d8, d9, "DEL"),
            listOf("/", "NUM_ALPHA", ",", "NUM_SYM", d0, "=", ".", "ENTER")
        )
    }

    private fun injectNumberPadKeyIntoBottomRow(rows: List<List<String>>): List<List<String>> {
        if (rows.isEmpty()) return rows
        val lastRow = rows.last()
        val spaceIdx = lastRow.indexOf("SPACE")
        if (spaceIdx < 0 || lastRow.contains("1234")) return rows
        val updatedLastRow = ArrayList<String>(lastRow.size + 1).apply {
            addAll(lastRow.subList(0, spaceIdx + 1))
            add("1234")
            addAll(lastRow.subList(spaceIdx + 1, lastRow.size))
        }
        return rows.dropLast(1) + listOf(updatedLastRow)
    }

    private fun currentRows(): List<List<String>> {
        if (isNumberPad) return numberPadRows()
        if (isSymbols) return symRows
        val dyn = activeDynamicLayout ?: return enRows
        val baseRows = when {
            isShifted -> dyn.shiftRows
            isKarBarActive && dyn.karBar.isNotEmpty() && dyn.rows.isNotEmpty() -> {
                // Dynamic Kar / Matra vowel-bar morphs Row 0 right after typing a consonant!
                listOf(dyn.karBar) + dyn.rows.drop(1)
            }
            else -> dyn.rows
        }
        return injectNumberPadKeyIntoBottomRow(baseRows)
    }

    private fun rebuildKeyBoxes() {
        keyBoxes.clear()
        if (width <= 0 || height <= 0 || isEmojiMode) return

        val rows = currentRows()
        if (rows.isEmpty()) return

        val isFiveRow = !isNumberPad && rows.size >= 5
        val gapX = (when {
            isNumberPad -> 5.5f
            isFiveRow -> 3.8f
            else -> 5f
        }) * density
        val gapY = (when {
            isNumberPad -> 6.5f
            isFiveRow -> 5.0f
            else -> 7f
        }) * density
        val padTop = (if (isFiveRow) 3f else 4f) * density
        val padBottom = (if (isFiveRow) 3f else 4f) * density
        val usableH = (height.toFloat() - padTop - padBottom - gapY * (rows.size - 1)).coerceAtLeast(1f)
        val rowH = usableH / rows.size

        val dyn = activeDynamicLayout
        val isEnglish = dyn == null

        var curY = padTop
        rows.forEachIndexed { rIndex, row ->
            // Gboard symmetric inset for 9-key home rows (English ASDF or Urdu 9-key row)
            val useHomeRowInset = !isNumberPad && !isSymbols && rIndex == 1 && row.size == 9 && rows.size == 4
            val sideInset = if (useHomeRowInset) (width * 0.048f) else 0f

            val totalWeight = row.sumOf { keyWeight(it, isFiveRow, isNumberPad).toDouble() }.toFloat().coerceAtLeast(1f)
            val totalGapX = gapX * (row.size + 1) + sideInset * 2f
            val usableW = (width.toFloat() - totalGapX).coerceAtLeast(1f)

            val isRowKarBar = !isNumberPad && !isSymbols && !isShifted && isKarBarActive && rIndex == 0 && dyn != null && dyn.karBar.isNotEmpty()

            var curX = gapX + sideInset
            row.forEachIndexed { cIndex, rawKey ->
                val weight = keyWeight(rawKey, isFiveRow, isNumberPad)
                val keyW = usableW * (weight / totalWeight)
                val rect = RectF(curX, curY, curX + keyW, curY + rowH)
                val keyId = "${activeLanguageId}_${if (isNumberPad) "num" else "kb"}_${rIndex}_${cIndex}_$rawKey"

                val displayLabel = when {
                    rawKey == "SPACE" -> when {
                        activeLanguageId == LanguageCatalog.BANGLISH_ID -> "বাংলিশ (BN-EN)"
                        else -> dyn?.spacebarLabel ?: "English"
                    }
                    rawKey == "NUM_SPACE" -> "␣"
                    rawKey == "NUM_ALPHA" -> if (activeLanguageId == "bn") "কখগ" else "ABC"
                    rawKey == "NUM_SYM" -> "!?#"
                    rawKey == "1234" -> "1234"
                    rawKey == "।" && activeLanguageId == LanguageCatalog.BANGLISH_ID -> "."
                    rawKey == "DEL" -> "⌫"
                    rawKey == "SHIFT" -> ""
                    rawKey == "ENTER" -> "↵"
                    rawKey == "GLOBE" -> ""
                    rawKey == "ABC" -> dyn?.backToAlphaLabel ?: "ABC"
                    isEnglish && !isSymbols && !isNumberPad && rawKey.length == 1 ->
                        if (isShifted) rawKey.uppercase() else rawKey.lowercase()
                    else -> rawKey
                }

                // Gboard top-right sub-label hints & long-press characters from built-in English or dynamic JSON
                val subLabel: String?
                val longPressOut: String?
                when {
                    isNumberPad -> {
                        subLabel = null
                        longPressOut = null
                    }
                    !isSymbols && rIndex == 0 && isEnglish && cIndex < enTopRowHints.size -> {
                        subLabel = enTopRowHints[cIndex]
                        longPressOut = enTopRowHints[cIndex]
                    }
                    !isSymbols && rIndex == 0 && !isShifted && !isRowKarBar && dyn != null && cIndex < dyn.topRowHints.size -> {
                        subLabel = dyn.topRowHints[cIndex]
                        longPressOut = dyn.topRowHints[cIndex]
                    }
                    !isSymbols && !isShifted && dyn != null && dyn.longPressMap.containsKey(rawKey) -> {
                        val alt = dyn.longPressMap[rawKey]
                        subLabel = alt
                        longPressOut = alt
                    }
                    rawKey == "," || rawKey == "،" -> {
                        subLabel = "☺"
                        longPressOut = null
                    }
                    else -> {
                        subLabel = null
                        longPressOut = null
                    }
                }

                val neonArgb = RgbLightingEngine.distinctNeonColorForKey(
                    label = rawKey,
                    indexHint = rIndex * 13 + cIndex
                ).toArgb()

                keyBoxes.add(
                    KeyBox(
                        id = keyId,
                        rawKey = rawKey,
                        displayLabel = displayLabel,
                        subLabel = subLabel,
                        longPressOutput = longPressOut,
                        rect = rect,
                        neonColor = neonArgb,
                        isKarBarKey = isRowKarBar
                    )
                )
                curX += keyW + gapX
            }
            curY += rowH + gapY
        }
    }

    private fun ensureSweepShader(): SweepGradient {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val existing = cachedShader
        if (existing != null && cachedShaderW == w && cachedShaderH == h && cachedPaletteOrdinal == settings.palette.ordinal) {
            return existing
        }

        val paletteInts = if (settings.palette.isFullSpectrum) {
            intArrayOf(
                Color.RED, Color.MAGENTA, Color.BLUE,
                Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED
            )
        } else {
            val cols = settings.palette.colors
            IntArray(cols.size + 1) { idx -> cols[idx % cols.size].toArgb() }
        }

        val created = SweepGradient(w / 2f, h / 2f, paletteInts, null)
        cachedShader = created
        cachedShaderW = w
        cachedShaderH = h
        cachedPaletteOrdinal = settings.palette.ordinal
        return created
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val nowMs = SystemClock.uptimeMillis()
        val snapshot = particleSnapshotRef.get()
        val renderGlows = snapshot.glows
        val renderTrails = snapshot.trails

        if (isEmojiMode) {
            drawEmojiGrid(canvas)
            return
        }

        if (keyBoxes.isEmpty()) {
            rebuildKeyBoxes()
        }

        val isNormalClassicBlack = !vipVisualsEnabled || settings.effect == RgbEffect.NORMAL_DEFAULT
        val isTouchSparkMode = isNormalClassicBlack || settings.effect == RgbEffect.TOUCH_SPARK
        val allowRgbGlow = vipVisualsEnabled && settings.effect != RgbEffect.NORMAL_DEFAULT
        val cornerRadius = (settings.keyCornerRadiusDp * density * 0.82f).coerceIn(5.5f * density, 13f * density)

        // 0. Draw Persistent Custom Photo / Banner Theme Background ONLY when VIP visuals are active
        val hasCustomPhoto = if (vipVisualsEnabled) {
            reloadPhotoBitmapIfNeeded()
            drawCustomPhotoBackground(canvas)
        } else {
            false
        }

        val continuousShader: SweepGradient? = if (allowRgbGlow && !isTouchSparkMode) {
            val shader = ensureSweepShader()
            val angle = when (settings.effect) {
                RgbEffect.STATIC_NEON -> 0f
                RgbEffect.AI_PLASMA_VORTEX -> rgbPhase * 1.85f
                RgbEffect.AURORA_FLOW -> (kotlin.math.sin(Math.toRadians(rgbPhase.toDouble())) * 95.0).toFloat()
                else -> rgbPhase
            }
            shaderMatrix.setRotate(angle, width / 2f, height / 2f)
            shader.setLocalMatrix(shaderMatrix)
            val alphaFactor = when (settings.effect) {
                RgbEffect.CYBER_BREATHING -> {
                    val breath = ((kotlin.math.sin(Math.toRadians(rgbPhase * 2.0)) + 1.0) * 0.5).toFloat()
                    (0.35f + 0.65f * breath) * settings.glowIntensity
                }
                RgbEffect.NEON_HEARTBEAT -> {
                    val beat = ((rgbPhase * 0.015f) % 1f)
                    val pulse = if (beat < 0.18f || (beat in 0.25f..0.40f)) 1f else 0.42f
                    pulse * settings.glowIntensity
                }
                RgbEffect.LIGHTNING_ARC -> {
                    val flash = ((rgbPhase * 0.03f) % 1f)
                    (if (flash < 0.16f) 1.0f else 0.46f) * settings.glowIntensity
                }
                RgbEffect.FIREFLY_SPARKS -> {
                    val glimmer = ((kotlin.math.sin(Math.toRadians(rgbPhase * 3.1)) + 1.0) * 0.5).toFloat()
                    (0.45f + 0.55f * glimmer) * settings.glowIntensity
                }
                else -> settings.glowIntensity
            }.coerceIn(0.25f, 1.0f)
            rgbBorderPaint.shader = shader
            rgbBorderPaint.alpha = (255 * alphaFactor).toInt()
            rgbFillPaint.shader = shader
            rgbFillPaint.alpha = (if (hasCustomPhoto) 135 else 175 * alphaFactor).toInt().coerceIn(50, 220)
            shader
        } else {
            null
        }

        val useBengaliFont = activeLanguageId == "bn" || activeLanguageId == LanguageCatalog.BANGLISH_ID
        val activeLegendTypeface = if (useBengaliFont) bengaliTypeface else normalTypeface
        subLabelPaint.typeface = activeLegendTypeface

        // 1. Draw Gboard Keycaps (pure matte-black in Normal Classic Black, or Touch Spark / RGB when VIP)
        for (i in keyBoxes.indices) {
            val box = keyBoxes[i]
            val rect = box.rect
            val isPressed = pressedKeyBox?.id == box.id
            var glowEntry: ActiveKeyGlow? = null
            for (gIdx in renderGlows.lastIndex downTo 0) {
                val candidate = renderGlows[gIdx]
                if (candidate.keyId == box.id) {
                    glowEntry = candidate
                    break
                }
            }
            val glowIntensity = when {
                !allowRgbGlow -> 0f
                isPressed -> 1f
                else -> computeGlowFactor(glowEntry, nowMs)
            }
            val isShiftOnce = box.rawKey == "SHIFT" && shiftMode == ShiftMode.ONCE
            val isCapsLocked = box.rawKey == "SHIFT" && shiftMode == ShiftMode.CAPS_LOCK
            val isShiftLit = isShiftOnce || isCapsLocked

            // Prominent circular blue Enter / Search button on the bottom-right corner (Gboard signature)
            if (box.rawKey == "ENTER") {
                drawGboardCircularActionKey(canvas, box, isPressed, glowIntensity)
                continue
            }

            if (isTouchSparkMode) {
                // Clean matte-black Gboard rounded keycaps (translucent when VIP custom photo is active)
                val letterAlpha = if (hasCustomPhoto) 154 else 242
                val modAlpha = if (hasCustomPhoto) 138 else 235
                val isNumberPadSideKey = isNumberPad && !isNumberPadDigit(box.rawKey)
                keyBgPaint.color = when {
                    isNormalClassicBlack && isPressed -> Color.argb(245, 42, 45, 58)
                    isCapsLocked -> Color.argb(225, 24, 62, 118)
                    isShiftOnce -> Color.argb(215, 36, 48, 72)
                    box.isKarBarKey -> Color.argb(if (hasCustomPhoto) 185 else 235, 22, 42, 58)
                    isNormalClassicBlack && box.rawKey == "SPACE" -> Color.argb(letterAlpha, 24, 25, 32)
                    isNormalClassicBlack && (isNumberPadSideKey || isModifierKey(box.rawKey) || box.rawKey == "GLOBE" || isPunctuationKey(box.rawKey)) ->
                        Color.argb(modAlpha, 16, 17, 23)
                    isNormalClassicBlack -> Color.argb(letterAlpha, 26, 28, 36)
                    box.rawKey == "SPACE" -> Color.argb(letterAlpha, 34, 37, 49)
                    isNumberPadSideKey || isModifierKey(box.rawKey) || box.rawKey == "GLOBE" || isPunctuationKey(box.rawKey) ->
                        Color.argb(modAlpha, 25, 28, 39)
                    else -> Color.argb(letterAlpha, 35, 39, 52)
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)

                if (box.isKarBarKey && glowIntensity <= 0.01f) {
                    sparkBorderPaint.color = Color.argb(120, 0, 245, 212)
                    sparkBorderPaint.strokeWidth = 1.0f * density
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, sparkBorderPaint)
                }

                // Touch Spark neon glow ignition ONLY when VIP visuals are enabled AND not Normal Classic Black
                if (allowRgbGlow) {
                    val effGlow = when {
                        isCapsLocked -> maxOf(glowIntensity, 0.92f)
                        isShiftOnce -> maxOf(glowIntensity, 0.58f)
                        else -> glowIntensity
                    }
                    if (effGlow > 0.01f) {
                        val neon = if (isCapsLocked) Color.parseColor("#38BDF8") else box.neonColor
                        sparkFillPaint.color = Color.argb(
                            (effGlow * 96 * settings.glowIntensity).toInt().coerceIn(0, 145),
                            Color.red(neon),
                            Color.green(neon),
                            Color.blue(neon)
                        )
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, sparkFillPaint)

                        sparkBorderPaint.color = Color.argb(
                            (effGlow * 255 * settings.glowIntensity).toInt().coerceIn(0, 255),
                            Color.red(neon),
                            Color.green(neon),
                            Color.blue(neon)
                        )
                        sparkBorderPaint.strokeWidth = (1.6f + effGlow * 1.4f) * density
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, sparkBorderPaint)
                    }
                }
            } else {
                // Optional continuous RGB Studio modes
                when (settings.keycapStyle) {
                    KeycapStyle.PUDDING -> {
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rgbFillPaint)
                        puddingInnerPaint.color = Color.argb(if (hasCustomPhoto) 168 else 245, 24, 27, 37)
                        tempRect.set(rect.left + 2.2f * density, rect.top + 1.5f * density, rect.right - 2.2f * density, rect.bottom - 4f * density)
                        val innerR = (cornerRadius - 2f * density).coerceAtLeast(3f)
                        canvas.drawRoundRect(tempRect, innerR, innerR, puddingInnerPaint)
                    }
                    KeycapStyle.NEON_WIREFRAME -> {
                        keyBgPaint.color = Color.argb(if (hasCustomPhoto) 128 else 240, 17, 19, 26)
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)
                        rgbBorderPaint.strokeWidth = 1.9f * density
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rgbBorderPaint)
                    }
                    KeycapStyle.SOLID_MECH -> {
                        keyBgPaint.color = Color.argb(if (hasCustomPhoto) 165 else 245, 34, 38, 52)
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)
                        tempRect.set(rect.centerX() - rect.width() * 0.28f, rect.bottom - 4.5f * density, rect.centerX() + rect.width() * 0.28f, rect.bottom - 2f * density)
                        canvas.drawRoundRect(tempRect, 3f * density, 3f * density, rgbFillPaint)
                    }
                    KeycapStyle.FLOATING_GLOW -> {
                        keyBgPaint.color = Color.argb(if (hasCustomPhoto) 152 else 230, 31, 34, 46)
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)
                        rgbBorderPaint.strokeWidth = 1.6f * density
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rgbBorderPaint)
                    }
                }

                if (glowIntensity > 0.01f || isShiftLit) {
                    val neon = if (isCapsLocked) Color.parseColor("#38BDF8") else box.neonColor
                    val factor = when {
                        isCapsLocked -> maxOf(glowIntensity, 0.85f)
                        isShiftOnce -> maxOf(glowIntensity, 0.50f)
                        else -> glowIntensity
                    }
                    sparkFillPaint.color = Color.argb(
                        (factor * 105).toInt().coerceIn(0, 145),
                        Color.red(neon),
                        Color.green(neon),
                        Color.blue(neon)
                    )
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, sparkFillPaint)
                }
            }

            // Draw crisp Shift / Caps Lock arrow icon (with underline bar when Caps Lock is ON)
            if (box.rawKey == "SHIFT") {
                drawShiftArrowIcon(canvas, rect, shiftMode, glowIntensity, box.neonColor, isTouchSparkMode)
                continue
            }

            // Draw Globe icon cleanly for the language switch key
            if (box.rawKey == "GLOBE") {
                val iconColor = if (glowIntensity > 0.08f && isTouchSparkMode) {
                    blendColor(Color.parseColor("#D5DCEF"), box.neonColor, glowIntensity)
                } else {
                    Color.parseColor("#D5DCEF")
                }
                drawGlobeIcon(canvas, rect.centerX(), rect.centerY(), min(rect.width(), rect.height()) * 0.26f, iconColor)
                continue
            }

            // Subtle Gboard top-right sub-label hint (1-0 on top row, etc.)
            if (box.subLabel != null) {
                subLabelPaint.color = if (glowIntensity > 0.1f && isTouchSparkMode) {
                    blendColor(Color.parseColor("#8C95AD"), box.neonColor, glowIntensity)
                } else {
                    Color.parseColor("#8891A8")
                }
                subLabelPaint.textSize = if (box.subLabel == "☺") 8.5f * scaledDensity else 9.5f * scaledDensity
                canvas.drawText(
                    box.subLabel,
                    rect.right - 3.5f * density,
                    rect.top + 10.5f * density,
                    subLabelPaint
                )
            }

            // Crisp centered keycap letter / label (cached Typeface — zero allocations per frame)
            val rowH = rect.height()
            val isNumDigit = isNumberPad && isNumberPadDigit(box.rawKey)
            textPaint.typeface = when {
                isNumDigit && activeLanguageId != "bn" -> boldTypeface
                else -> activeLegendTypeface
            }
            textPaint.textSize = when {
                box.rawKey == "SPACE" -> (rowH * 0.29f).coerceIn(11f * scaledDensity, 13.5f * scaledDensity)
                box.rawKey == "1234" -> (rowH * 0.27f).coerceIn(10f * scaledDensity, 12.5f * scaledDensity)
                box.rawKey == "NUM_ALPHA" || box.rawKey == "NUM_SYM" -> (rowH * 0.28f).coerceIn(10.5f * scaledDensity, 13f * scaledDensity)
                box.rawKey == "NUM_SPACE" -> (rowH * 0.44f).coerceIn(16f * scaledDensity, 21f * scaledDensity)
                isNumDigit -> (rowH * 0.50f).coerceIn(18f * scaledDensity, 25f * scaledDensity)
                box.displayLabel.length > 2 -> (rowH * 0.30f).coerceIn(10.5f * scaledDensity, 13.5f * scaledDensity)
                box.rawKey == "SHIFT" || box.rawKey == "DEL" -> (rowH * 0.43f).coerceIn(15f * scaledDensity, 20f * scaledDensity)
                box.isKarBarKey -> (rowH * 0.50f).coerceIn(15f * scaledDensity, 21f * scaledDensity)
                else -> (rowH * 0.45f).coerceIn(14f * scaledDensity, 21f * scaledDensity)
            }

            if (isTouchSparkMode) {
                textPaint.shader = null
                textPaint.color = when {
                    isPressed -> Color.WHITE
                    glowIntensity > 0.08f -> blendColor(Color.parseColor("#EEF2FC"), box.neonColor, glowIntensity)
                    isShiftLit -> box.neonColor
                    box.isKarBarKey -> Color.parseColor("#7CEEFF")
                    box.rawKey == "SPACE" -> Color.parseColor("#C2CBE0")
                    else -> Color.parseColor("#EEF2FC")
                }
            } else {
                textPaint.shader = if (settings.illuminatedLegends && !isPressed) continuousShader else null
                textPaint.color = Color.WHITE
            }

            val yShift = if (box.subLabel != null && box.rawKey != ",") 2.5f * density else 0f
            val textY = rect.centerY() + yShift - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(box.displayLabel, rect.centerX(), textY, textPaint)
        }
        textPaint.shader = null

        // 2. Smooth fading multicolor Glide / Swipe trail (VIP + non-Normal mode only)
        if (allowRgbGlow && renderTrails.size >= 2) {
            drawGlideTrail(canvas, renderTrails, nowMs)
        }

        // 3. Key pop-up bubbles (glowing multicolor rising bubbles when VIP RGB, clean matte dark popup when Normal/Free)
        if (renderGlows.isNotEmpty()) {
            drawRisingSparkBubbles(canvas, renderGlows, nowMs, useBengaliFont)
        }
    }

    private fun drawGboardCircularActionKey(
        canvas: Canvas,
        box: KeyBox,
        isPressed: Boolean,
        glowIntensity: Float
    ) {
        val r = box.rect
        val radius = (min(r.width(), r.height()) * 0.48f)
        val cx = r.centerX()
        val cy = r.centerY()

        // Outer neon touch halo when pressed/fading
        if (glowIntensity > 0.02f) {
            val haloR = radius * 1.32f
            popupHaloPaint.shader = RadialGradient(
                cx, cy, haloR,
                intArrayOf(
                    Color.argb((glowIntensity * 165).toInt().coerceIn(0, 200), 66, 133, 244),
                    Color.TRANSPARENT
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, haloR, popupHaloPaint)
            popupHaloPaint.shader = null
        }

        blueActionPaint.color = if (isPressed) Color.parseColor("#5C9CFF") else Color.parseColor("#3B82F6")
        canvas.drawCircle(cx, cy, radius, blueActionPaint)

        // Draw crisp white Search magnifying glass or Return arrow inside the blue circle
        vectorIconPaint.color = Color.WHITE
        vectorIconPaint.strokeWidth = 2.0f * density
        if (isSearchAction) {
            val lensR = radius * 0.30f
            val lensCx = cx - radius * 0.08f
            val lensCy = cy - radius * 0.08f
            canvas.drawCircle(lensCx, lensCy, lensR, vectorIconPaint)
            val handleStart = lensR * 0.72f
            canvas.drawLine(
                lensCx + handleStart,
                lensCy + handleStart,
                cx + radius * 0.36f,
                cy + radius * 0.36f,
                vectorIconPaint
            )
        } else {
            // Crisp Gboard return arrow (↵)
            val rightX = cx + radius * 0.30f
            val leftX = cx - radius * 0.32f
            val topY = cy - radius * 0.22f
            val midY = cy + radius * 0.08f
            canvas.drawLine(rightX, topY, rightX, midY, vectorIconPaint)
            canvas.drawLine(rightX, midY, leftX, midY, vectorIconPaint)
            val head = radius * 0.20f
            canvas.drawLine(leftX, midY, leftX + head, midY - head, vectorIconPaint)
            canvas.drawLine(leftX, midY, leftX + head, midY + head, vectorIconPaint)
        }
    }

    private fun drawCustomPhotoBackground(canvas: Canvas): Boolean {
        val bmp = cachedPhotoBitmap ?: return false
        if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) return false

        // CENTER_CROP source rect so any portrait/landscape gallery photo fills cleanly without stretching
        val viewAspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
        if (bmpAspect > viewAspect) {
            val scaledW = (bmp.height * viewAspect).toInt().coerceIn(1, bmp.width)
            val left = (bmp.width - scaledW) / 2
            photoSrcRect.set(left, 0, left + scaledW, bmp.height)
        } else {
            val scaledH = (bmp.width / viewAspect).toInt().coerceIn(1, bmp.height)
            val top = (bmp.height - scaledH) / 2
            photoSrcRect.set(0, top, bmp.width, top + scaledH)
        }
        photoDstRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bmp, photoSrcRect, photoDstRect, photoPaint)

        // Configurable dim overlay so keys & Touch Spark RGB glow pop cleanly over bright photos
        val dimAlphaInt = (photoTheme.dimAlpha.coerceIn(0.15f, 0.85f) * 255f).toInt()
        photoDimPaint.color = Color.argb(dimAlphaInt, 8, 10, 18)
        canvas.drawRect(photoDstRect, photoDimPaint)
        return true
    }

    private fun drawShiftArrowIcon(
        canvas: Canvas,
        rect: RectF,
        mode: ShiftMode,
        glowIntensity: Float,
        neonColor: Int,
        isTouchSparkMode: Boolean
    ) {
        val cx = rect.centerX()
        val cy = rect.centerY() - if (mode == ShiftMode.CAPS_LOCK) 1.5f * density else 0f
        val size = min(rect.width(), rect.height()) * 0.27f

        shiftArrowPath.reset()
        // Crisp upward arrow geometry
        shiftArrowPath.moveTo(cx, cy - size)                       // top tip
        shiftArrowPath.lineTo(cx + size, cy + size * 0.08f)        // right wing
        shiftArrowPath.lineTo(cx + size * 0.42f, cy + size * 0.08f)
        shiftArrowPath.lineTo(cx + size * 0.42f, cy + size * 0.82f) // bottom right stem
        shiftArrowPath.lineTo(cx - size * 0.42f, cy + size * 0.82f) // bottom left stem
        shiftArrowPath.lineTo(cx - size * 0.42f, cy + size * 0.08f)
        shiftArrowPath.lineTo(cx - size, cy + size * 0.08f)        // left wing
        shiftArrowPath.close()

        when (mode) {
            ShiftMode.OFF -> {
                val strokeCol = if (glowIntensity > 0.08f && isTouchSparkMode) {
                    blendColor(Color.parseColor("#E3E8F4"), neonColor, glowIntensity)
                } else {
                    Color.parseColor("#E3E8F4")
                }
                vectorIconPaint.color = strokeCol
                vectorIconPaint.strokeWidth = 1.75f * density
                canvas.drawPath(shiftArrowPath, vectorIconPaint)
            }
            ShiftMode.ONCE -> {
                shiftFillPaint.color = Color.WHITE
                shiftFillPaint.strokeWidth = 1.2f * density
                canvas.drawPath(shiftArrowPath, shiftFillPaint)
            }
            ShiftMode.CAPS_LOCK -> {
                val lockAccent = Color.parseColor("#60E5FF")
                shiftFillPaint.color = Color.WHITE
                shiftFillPaint.strokeWidth = 1.2f * density
                canvas.drawPath(shiftArrowPath, shiftFillPaint)

                // Prominent glowing underline bar directly beneath arrow for Caps Lock ON
                vectorIconPaint.color = lockAccent
                vectorIconPaint.strokeWidth = 2.4f * density
                val barY = cy + size * 1.25f
                canvas.drawLine(cx - size * 0.68f, barY, cx + size * 0.68f, barY, vectorIconPaint)
            }
        }
    }

    private fun drawGlobeIcon(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        vectorIconPaint.color = color
        vectorIconPaint.strokeWidth = 1.45f * density
        canvas.drawCircle(cx, cy, radius, vectorIconPaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, vectorIconPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, vectorIconPaint)
        tempRect.set(cx - radius * 0.52f, cy - radius, cx + radius * 0.52f, cy + radius)
        canvas.drawOval(tempRect, vectorIconPaint)
    }

    private fun computeGlowFactor(entry: ActiveKeyGlow?, nowMs: Long): Float {
        if (entry == null) return 0f
        val releasedAt = entry.releasedAtMs ?: return 1f
        val elapsed = (nowMs - releasedAt).toFloat()
        val linear = (1f - elapsed / SPARK_FADE_MS).coerceIn(0f, 1f)
        return linear * linear
    }

    private fun drawRisingSparkBubbles(
        canvas: Canvas,
        renderGlows: List<ActiveKeyGlow>,
        nowMs: Long,
        useBengaliFont: Boolean
    ) {
        popupTextPaint.typeface = if (useBengaliFont) bengaliTypeface else boldTypeface
        for (i in renderGlows.indices) {
            val glow = renderGlows[i]
            if (!glow.showPopup) continue

            // In Free Standard Mode or explicit Normal / Classic Black mode, show clean matte-dark popup only while pressed
            if (!vipVisualsEnabled || settings.effect == RgbEffect.NORMAL_DEFAULT) {
                if (glow.releasedAtMs != null) continue
                val bubbleW = maxOf(glow.rect.width() * 1.38f, 40f * density)
                val bubbleH = maxOf(glow.rect.height() * 1.12f, 44f * density)
                val cx = glow.rect.centerX().coerceIn(bubbleW / 2f + 4f * density, width - bubbleW / 2f - 4f * density)
                val minTop = if (allowTopOverflow) -30f * density else 3f * density
                val top = (glow.rect.top - 8f * density - bubbleH).coerceAtLeast(minTop)
                bubbleRect.set(cx - bubbleW / 2f, top, cx + bubbleW / 2f, top + bubbleH)
                val bubbleCorner = 12f * density

                popupBgPaint.color = Color.parseColor("#2E3344")
                canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, popupBgPaint)
                sparkBorderPaint.color = Color.parseColor("#475069")
                sparkBorderPaint.strokeWidth = 1.2f * density
                canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, sparkBorderPaint)

                popupTextPaint.textSize = 22f * scaledDensity
                popupTextPaint.color = Color.WHITE
                val fm = popupTextPaint.fontMetrics
                val ty = bubbleRect.centerY() - (fm.ascent + fm.descent) / 2f
                canvas.drawText(glow.displayLabel, bubbleRect.centerX(), ty, popupTextPaint)
                continue
            }

            val fade = computeGlowFactor(glow, nowMs)
            if (fade <= 0.02f) continue

            val pressProgress = ((nowMs - glow.pressedAtMs).toFloat() / 90f).coerceIn(0f, 1f)
            val easeOutRise = 1f - (1f - pressProgress) * (1f - pressProgress)
            val releaseLift = if (glow.releasedAtMs != null) (1f - fade) * 14f * density else 0f

            val bubbleW = maxOf(glow.rect.width() * 1.48f, 44f * density)
            val bubbleH = maxOf(glow.rect.height() * 1.20f, 48f * density)
            val cx = glow.rect.centerX().coerceIn(bubbleW / 2f + 4f * density, width - bubbleW / 2f - 4f * density)
            val baseBottom = glow.rect.top - 4f * density
            val risePx = (10f * density * easeOutRise) + releaseLift
            val minTop = if (allowTopOverflow) -36f * density else 3f * density
            val top = (baseBottom - risePx - bubbleH).coerceAtLeast(minTop)
            val bottom = top + bubbleH
            bubbleRect.set(cx - bubbleW / 2f, top, cx + bubbleW / 2f, bottom)

            val neon = glow.neonColor
            val r = Color.red(neon)
            val g = Color.green(neon)
            val b = Color.blue(neon)

            // Hardware-accelerated layered neon halo (zero RadialGradient/shadowLayer allocations!)
            val haloRadius = maxOf(bubbleW, bubbleH) * 0.78f
            popupHaloPaint.shader = null
            popupHaloPaint.color = Color.argb((fade * 52 * settings.glowIntensity).toInt().coerceIn(0, 95), r, g, b)
            canvas.drawCircle(bubbleRect.centerX(), bubbleRect.centerY(), haloRadius, popupHaloPaint)
            popupHaloPaint.color = Color.argb((fade * 92 * settings.glowIntensity).toInt().coerceIn(0, 140), r, g, b)
            canvas.drawCircle(bubbleRect.centerX(), bubbleRect.centerY(), haloRadius * 0.66f, popupHaloPaint)

            val bubbleCorner = 14f * density
            popupBgPaint.color = Color.argb((fade * 240).toInt().coerceIn(0, 245), 16, 19, 30)
            canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, popupBgPaint)

            popupBgPaint.color = Color.argb((fade * 80).toInt().coerceIn(0, 120), r, g, b)
            canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, popupBgPaint)

            sparkBorderPaint.color = Color.argb((fade * 255).toInt().coerceIn(0, 255), r, g, b)
            sparkBorderPaint.strokeWidth = 2.2f * density
            canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, sparkBorderPaint)

            popupTextPaint.textSize = (24f * scaledDensity) * (0.92f + 0.12f * easeOutRise)
            popupTextPaint.color = Color.argb((fade * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            val fm = popupTextPaint.fontMetrics
            val ty = bubbleRect.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(glow.displayLabel, bubbleRect.centerX(), ty, popupTextPaint)
        }
    }

    private fun drawGlideTrail(canvas: Canvas, renderTrails: List<TrailPoint>, nowMs: Long) {
        if (renderTrails.size < 2) return

        for (i in 1 until renderTrails.size) {
            val p0 = renderTrails[i - 1]
            val p1 = renderTrails[i]
            val age = (nowMs - p1.timeMs).toFloat()
            val life = (1f - age / TRAIL_LIFETIME_MS).coerceIn(0f, 1f)
            if (life <= 0.02f) continue

            val progress = i.toFloat() / renderTrails.size.toFloat()
            val widthFactor = life * (0.35f + 0.65f * progress)
            val c = p1.color
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            trailOuterPaint.color = Color.argb((life * 115 * settings.glowIntensity).toInt().coerceIn(0, 160), r, g, b)
            trailOuterPaint.strokeWidth = 14f * density * widthFactor
            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, trailOuterPaint)

            trailCorePaint.color = Color.argb((life * 245).toInt().coerceIn(0, 255), minOf(255, r + 90), minOf(255, g + 90), minOf(255, b + 90))
            trailCorePaint.strokeWidth = 4.2f * density * widthFactor
            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, trailCorePaint)
        }
    }

    private fun drawEmojiGrid(canvas: Canvas) {
        emojiBoxes.clear()
        val cols = 8
        val emojiRows = 4
        val pad = 5f * density
        val bottomBarH = 40f * density
        val gridH = (height - bottomBarH - pad * 2f).coerceAtLeast(1f)
        val cellW = (width - pad * (cols + 1)) / cols
        val cellH = (gridH - pad * (emojiRows - 1)) / emojiRows

        textPaint.shader = null
        textPaint.color = Color.WHITE
        textPaint.textSize = (cellH * 0.56f).coerceIn(18f * scaledDensity, 24f * scaledDensity)

        val emojis = KeyboardLayoutData.curatedEmojis
        var idx = 0
        for (r in 0 until emojiRows) {
            for (c in 0 until cols) {
                if (idx >= emojis.size) break
                val left = pad + c * (cellW + pad)
                val top = pad + r * (cellH + pad)
                val rect = RectF(left, top, left + cellW, top + cellH)
                val emoji = emojis[idx]
                emojiBoxes.add(EmojiBox(emoji, rect))

                keyBgPaint.color = Color.parseColor("#1D202C")
                canvas.drawRoundRect(rect, 9f * density, 9f * density, keyBgPaint)
                if (pressedEmoji == emoji) {
                    sparkFillPaint.color = Color.argb(110, 66, 133, 244)
                    canvas.drawRoundRect(rect, 9f * density, 9f * density, sparkFillPaint)
                }
                val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(emoji, rect.centerX(), textY, textPaint)
                idx++
            }
        }

        val barTop = height - bottomBarH - pad
        val barBottom = height - pad
        emojiBackRect = RectF(pad, barTop, pad + 88f * density, barBottom)
        emojiDelRect = RectF(width - pad - 72f * density, barTop, width - pad, barBottom)

        keyBgPaint.color = Color.parseColor("#1E212E")
        canvas.drawRoundRect(emojiBackRect, 10f * density, 10f * density, keyBgPaint)
        canvas.drawRoundRect(emojiDelRect, 10f * density, 10f * density, keyBgPaint)

        textPaint.textSize = 13f * scaledDensity
        textPaint.color = Color.parseColor("#4285F4")
        val backText = activeDynamicLayout?.backToAlphaLabel ?: "ABC"
        val fm = textPaint.fontMetrics
        val yText = emojiBackRect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(backText, emojiBackRect.centerX(), yText, textPaint)

        textPaint.color = Color.parseColor("#EEF2FC")
        canvas.drawText("⌫", emojiDelRect.centerX(), yText, textPaint)
    }

    private fun isNumberPadDigit(key: String): Boolean {
        if (key.length != 1) return false
        val ch = key[0]
        return ch in '0'..'9' || ch in '০'..'৯'
    }

    private fun keyWeight(key: String, isFiveRow: Boolean, isNumPad: Boolean = false): Float {
        if (isNumPad) {
            return when (key) {
                "+", "-", "*" -> 0.95f
                "%", "NUM_SPACE", "DEL" -> 1.12f
                "/" -> 0.74f
                "NUM_ALPHA" -> 1.14f
                "," -> 0.62f
                "NUM_SYM" -> 0.92f
                "0", "০" -> 1.08f
                "=" -> 0.66f
                "." -> 0.62f
                "ENTER" -> 1.12f
                else -> if (isNumberPadDigit(key)) 1.55f else 1.0f
            }
        }
        return when (key) {
            "SPACE" -> 3.65f
            "1234" -> 1.12f
            "?123", "?১২৩", "?१२३", "?١٢٣", "?۱۲۳", "ABC" -> 1.38f
            "ENTER" -> 1.44f
            "SHIFT", "DEL", "=\\<" -> if (isFiveRow) 1.25f else 1.45f
            "GLOBE", ",", "،", ".", "।", "۔" -> 0.88f
            else -> 1.0f
        }
    }

    private fun isSymbolSwitchKey(key: String): Boolean =
        key == "?123" || key == "?১২৩" || key == "?१२३" || key == "?١٢٣" || key == "?۱۲۳"

    private fun isPunctuationKey(key: String): Boolean =
        key == "," || key == "،" || key == "." || key == "।" || key == "۔"

    private fun isModifierKey(key: String): Boolean =
        key == "SHIFT" ||
            key == "DEL" ||
            isSymbolSwitchKey(key) ||
            key == "ABC" ||
            key == "=\\<" ||
            key == "ENTER" ||
            key == "1234" ||
            key == "NUM_ALPHA" ||
            key == "NUM_SYM" ||
            key == "NUM_SPACE"

    private fun shouldCommitImmediatelyOnDown(rawKey: String): Boolean {
        if (isNumberPad) {
            return rawKey != "DEL"
        }
        return rawKey != "DEL" &&
            rawKey != "SHIFT" &&
            rawKey != "SPACE" &&
            rawKey != "," &&
            rawKey != "،"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEmojiMode) {
            return handleEmojiTouch(event)
        }

        val x = event.x
        val y = event.y
        val nowMs = SystemClock.uptimeMillis()
        wakeAnimator()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                touchDownX = x
                touchDownY = y
                lastTouchX = x
                spaceDragAccum = 0f
                isSpaceDragging = false
                isGliding = false
                longPressTriggered = false
                committedOnDown = false
                handler.removeCallbacks(longPressRunnable)

                val hit = findKeyAt(x, y)
                if (hit != null) {
                    pressedKeyBox = hit
                    activateKeyGlow(hit, nowMs)
                    if (settings.hapticEnabled) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    if (hit.rawKey == "DEL") {
                        committedOnDown = true
                        onKeyFeedback?.invoke(true)
                        if (isKarBarActive) {
                            isKarBarActive = false
                            rebuildKeyBoxes()
                        }
                        onKeyAction?.invoke("DEL")
                        isDeleting = true
                        deleteHoldStartMs = nowMs
                        handler.removeCallbacks(deleteRunnable)
                        handler.postDelayed(deleteRunnable, DELETE_INITIAL_DELAY_MS)
                    } else if (shouldCommitImmediatelyOnDown(hit.rawKey)) {
                        // Instant zero-lag commit directly on ACTION_DOWN!
                        committedOnDown = true
                        onKeyFeedback?.invoke(isModifierKey(hit.rawKey) || hit.rawKey == "GLOBE")
                        handleKeyPress(hit.rawKey)
                        if (hit.longPressOutput != null) {
                            handler.postDelayed(longPressRunnable, LONG_PRESS_DELAY_MS)
                        }
                    } else {
                        // SHIFT, SPACE, comma wait for ACTION_UP (to support cursor swipe / Caps Lock / Emoji long-press)
                        if (hit.rawKey == "SHIFT" || hit.rawKey == "," || hit.rawKey == "،") {
                            handler.postDelayed(longPressRunnable, LONG_PRESS_DELAY_MS)
                        }
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Multi-touch rollover for lightning-fast two-thumb typing!
                val pointerIndex = event.actionIndex
                val px = event.getX(pointerIndex)
                val py = event.getY(pointerIndex)
                val hit = findKeyAt(px, py) ?: return true

                handler.removeCallbacks(deleteRunnable)
                handler.removeCallbacks(longPressRunnable)
                isDeleting = false

                // If previous deferred key (e.g. SPACE / SHIFT / comma) hasn't committed yet, flush it cleanly before the new thumb's key!
                val prev = pressedKeyBox
                if (prev != null && !committedOnDown && !longPressTriggered && !(prev.rawKey == "SPACE" && isSpaceDragging)) {
                    onKeyFeedback?.invoke(isModifierKey(prev.rawKey) || prev.rawKey == "GLOBE")
                    handleKeyPress(prev.rawKey)
                }

                releaseCurrentGlow(nowMs)
                pressedKeyBox = hit
                touchDownX = px
                touchDownY = py
                lastTouchX = px
                spaceDragAccum = 0f
                isSpaceDragging = false
                isGliding = false
                longPressTriggered = false
                committedOnDown = false

                activateKeyGlow(hit, nowMs)
                if (settings.hapticEnabled) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                if (hit.rawKey == "DEL") {
                    committedOnDown = true
                    onKeyFeedback?.invoke(true)
                    if (isKarBarActive) {
                        isKarBarActive = false
                        rebuildKeyBoxes()
                    }
                    onKeyAction?.invoke("DEL")
                    isDeleting = true
                    deleteHoldStartMs = nowMs
                    handler.postDelayed(deleteRunnable, DELETE_INITIAL_DELAY_MS)
                } else if (shouldCommitImmediatelyOnDown(hit.rawKey)) {
                    committedOnDown = true
                    onKeyFeedback?.invoke(isModifierKey(hit.rawKey) || hit.rawKey == "GLOBE")
                    handleKeyPress(hit.rawKey)
                    if (hit.longPressOutput != null) {
                        handler.postDelayed(longPressRunnable, LONG_PRESS_DELAY_MS)
                    }
                } else if (hit.rawKey == "SHIFT" || hit.rawKey == "," || hit.rawKey == "،") {
                    handler.postDelayed(longPressRunnable, LONG_PRESS_DELAY_MS)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val active = pressedKeyBox
                if (active?.rawKey == "SPACE") {
                    val dx = x - lastTouchX
                    lastTouchX = x
                    spaceDragAccum += dx
                    val stepThreshold = 26f * density
                    if (abs(spaceDragAccum) >= stepThreshold) {
                        val steps = (spaceDragAccum / stepThreshold).toInt()
                        if (steps != 0) {
                            isSpaceDragging = true
                            onCursorMove?.invoke(steps)
                            spaceDragAccum -= steps * stepThreshold
                        }
                    }
                    return true
                }

                val movedDist = hypot(x - touchDownX, y - touchDownY)
                if (movedDist > 18f * density && !longPressTriggered) {
                    isGliding = true
                    isDeleting = false
                    handler.removeCallbacks(deleteRunnable)
                    handler.removeCallbacks(longPressRunnable)

                    val hue = ((x / width.coerceAtLeast(1).toFloat()) * 320f + (nowMs % 3600L) * 0.1f) % 360f
                    val trailColor = RgbLightingEngine.hsvToColor(hue, 0.96f, 1.0f).toArgb()
                    appendTrailPointAsync(x, y, nowMs, trailColor)

                    val hovered = findKeyAt(x, y)
                    if (hovered != null && hovered.id != pressedKeyBox?.id) {
                        releaseCurrentGlow(nowMs)
                        pressedKeyBox = hovered
                        activateKeyGlow(hovered, nowMs)
                    }
                    postInvalidateOnAnimation()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                handler.removeCallbacks(deleteRunnable)
                handler.removeCallbacks(longPressRunnable)
                isDeleting = false
                val released = pressedKeyBox
                releaseCurrentGlow(nowMs)
                pressedKeyBox = null

                if (!committedOnDown && released != null && released.rawKey != "DEL") {
                    if (longPressTriggered && released.rawKey == "SHIFT") {
                        // Already locked Caps Lock on long-press
                    } else if (longPressTriggered && released.longPressOutput != null) {
                        onKeyFeedback?.invoke(false)
                        onKeyAction?.invoke(released.longPressOutput)
                    } else if (released.rawKey == "SPACE" && isSpaceDragging) {
                        // Cursor swipe only
                    } else if (!isGliding) {
                        onKeyFeedback?.invoke(isModifierKey(released.rawKey) || released.rawKey == "GLOBE")
                        handleKeyPress(released.rawKey)
                    }
                }
                committedOnDown = false
                longPressTriggered = false
                isSpaceDragging = false
                isGliding = false
                postInvalidateOnAnimation()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                handler.removeCallbacks(deleteRunnable)
                handler.removeCallbacks(longPressRunnable)
                isDeleting = false
                committedOnDown = false
                longPressTriggered = false
                releaseCurrentGlow(nowMs)
                pressedKeyBox = null
                isSpaceDragging = false
                isGliding = false
                postInvalidateOnAnimation()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun activateKeyGlow(box: KeyBox, nowMs: Long) {
        val showPopup = !isModifierKey(box.rawKey) && box.rawKey != "SPACE" && box.rawKey != "GLOBE"
        val newGlow = ActiveKeyGlow(
            keyId = box.id,
            displayLabel = box.displayLabel,
            rect = RectF(box.rect),
            neonColor = box.neonColor,
            pressedAtMs = nowMs,
            releasedAtMs = null,
            showPopup = showPopup
        )
        particleScope.launch {
            synchronized(particleStateLock) {
                for (i in activeGlows.lastIndex downTo 0) {
                    if (activeGlows[i].keyId == box.id) {
                        activeGlows.removeAt(i)
                    }
                }
                activeGlows.add(newGlow)
                pruneAndCapLocked(nowMs)
                publishParticleSnapshotLocked()
            }
            postInvalidateOnAnimation()
        }
    }

    private fun appendTrailPointAsync(x: Float, y: Float, nowMs: Long, color: Int) {
        particleScope.launch {
            synchronized(particleStateLock) {
                trailPoints.add(TrailPoint(x, y, nowMs, color))
                pruneAndCapLocked(nowMs)
                publishParticleSnapshotLocked()
            }
        }
    }

    private fun updateGlowLabelAsync(keyId: String, altLabel: String) {
        particleScope.launch {
            synchronized(particleStateLock) {
                for (i in activeGlows.lastIndex downTo 0) {
                    if (activeGlows[i].keyId == keyId) {
                        activeGlows[i].displayLabel = altLabel
                        break
                    }
                }
                publishParticleSnapshotLocked()
            }
            postInvalidateOnAnimation()
        }
    }

    private fun scheduleParticlePrune(nowMs: Long) {
        lastParticlePruneMs = nowMs
        particleScope.launch {
            val changed: Boolean
            synchronized(particleStateLock) {
                changed = pruneAndCapLocked(nowMs)
                if (changed) {
                    publishParticleSnapshotLocked()
                }
            }
            if (changed) {
                postInvalidateOnAnimation()
            }
        }
    }

    private fun clearTrailPointsAsync() {
        particleScope.launch {
            synchronized(particleStateLock) {
                trailPoints.clear()
                publishParticleSnapshotLocked()
            }
        }
    }

    private fun clearAllParticlesAsync() {
        particleScope.launch {
            synchronized(particleStateLock) {
                activeGlows.clear()
                trailPoints.clear()
                publishParticleSnapshotLocked()
            }
        }
    }

    /**
     * Runs strictly off the UI thread on Dispatchers.Default under [particleStateLock].
     * Hard-caps live RGB particles to <= 25 items and prunes expired animations with zero lambdas.
     */
    private fun pruneAndCapLocked(nowMs: Long): Boolean {
        var mutated = false
        for (i in activeGlows.lastIndex downTo 0) {
            val rel = activeGlows[i].releasedAtMs
            if (rel != null && (nowMs - rel) > SPARK_FADE_MS) {
                activeGlows.removeAt(i)
                mutated = true
            }
        }
        for (i in trailPoints.lastIndex downTo 0) {
            if (nowMs - trailPoints[i].timeMs > TRAIL_LIFETIME_MS) {
                trailPoints.removeAt(i)
                mutated = true
            }
        }
        while (activeGlows.size > MAX_ACTIVE_GLOWS) {
            activeGlows.removeAt(0)
            mutated = true
        }
        while (trailPoints.size > MAX_TRAIL_POINTS) {
            trailPoints.removeAt(0)
            mutated = true
        }
        while (activeGlows.size + trailPoints.size > MAX_RGB_PARTICLES) {
            if (trailPoints.isNotEmpty()) {
                trailPoints.removeAt(0)
            } else if (activeGlows.isNotEmpty()) {
                activeGlows.removeAt(0)
            } else {
                break
            }
            mutated = true
        }
        return mutated
    }

    private fun publishParticleSnapshotLocked() {
        particleSnapshotRef.set(
            ParticleSnapshot(
                glows = ArrayList(activeGlows),
                trails = ArrayList(trailPoints)
            )
        )
    }

    private fun releaseCurrentGlow(nowMs: Long) {
        val activeId = pressedKeyBox?.id ?: return
        particleScope.launch {
            synchronized(particleStateLock) {
                for (i in activeGlows.indices) {
                    val glow = activeGlows[i]
                    if (glow.keyId == activeId && glow.releasedAtMs == null) {
                        glow.releasedAtMs = nowMs
                    }
                }
                publishParticleSnapshotLocked()
            }
        }
    }

    private fun findKeyAt(x: Float, y: Float): KeyBox? {
        keyBoxes.firstOrNull { it.rect.contains(x, y) }?.let { return it }
        return keyBoxes.minByOrNull { kb ->
            val dx = kb.rect.centerX() - x
            val dy = kb.rect.centerY() - y
            dx * dx + dy * dy
        }
    }

    private fun handleEmojiTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                when {
                    emojiBackRect.contains(event.x, event.y) -> {
                        if (settings.hapticEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onKeyFeedback?.invoke(true)
                        toggleEmojiMode()
                    }
                    emojiDelRect.contains(event.x, event.y) -> {
                        if (settings.hapticEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onKeyFeedback?.invoke(true)
                        onKeyAction?.invoke("DEL")
                        isDeleting = true
                        deleteHoldStartMs = SystemClock.uptimeMillis()
                        handler.removeCallbacks(deleteRunnable)
                        handler.postDelayed(deleteRunnable, DELETE_INITIAL_DELAY_MS)
                    }
                    else -> {
                        val hit = emojiBoxes.find { it.rect.contains(event.x, event.y) }
                        if (hit != null) {
                            pressedEmoji = hit.emoji
                            if (settings.hapticEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKeyFeedback?.invoke(false)
                            onKeyAction?.invoke(hit.emoji)
                            invalidate()
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                pressedEmoji = null
                isDeleting = false
                handler.removeCallbacks(deleteRunnable)
                invalidate()
                return true
            }
        }
        return true
    }

    private fun handleKeyPress(key: String) {
        when {
            key == "SHIFT" -> {
                val now = SystemClock.uptimeMillis()
                shiftMode = when (shiftMode) {
                    ShiftMode.OFF -> {
                        lastShiftTapMs = now
                        ShiftMode.ONCE
                    }
                    ShiftMode.ONCE -> {
                        if (now - lastShiftTapMs <= DOUBLE_TAP_SHIFT_MS) {
                            lastShiftTapMs = 0L
                            ShiftMode.CAPS_LOCK
                        } else {
                            lastShiftTapMs = 0L
                            ShiftMode.OFF
                        }
                    }
                    ShiftMode.CAPS_LOCK -> {
                        lastShiftTapMs = 0L
                        ShiftMode.OFF
                    }
                }
                if (shiftMode != ShiftMode.OFF) isKarBarActive = false
                rebuildKeyBoxes()
            }
            key == "1234" -> {
                isNumberPad = true
                isSymbols = false
                isKarBarActive = false
                rebuildKeyBoxes()
            }
            key == "NUM_ALPHA" -> {
                isNumberPad = false
                isSymbols = false
                isKarBarActive = false
                rebuildKeyBoxes()
            }
            key == "NUM_SYM" -> {
                isNumberPad = false
                isSymbols = true
                isKarBarActive = false
                rebuildKeyBoxes()
            }
            key == "NUM_SPACE" -> {
                onKeyAction?.invoke(" ")
            }
            isSymbolSwitchKey(key) -> {
                isNumberPad = false
                isSymbols = true
                isKarBarActive = false
                rebuildKeyBoxes()
            }
            key == "ABC" || key == "=\\<" -> {
                isNumberPad = false
                isSymbols = false
                isKarBarActive = false
                rebuildKeyBoxes()
            }
            key == "GLOBE" -> {
                isKarBarActive = false
                if (onLanguageToggleRequested != null) {
                    onLanguageToggleRequested?.invoke()
                } else {
                    cycleDownloadedLanguage()
                }
            }
            key == "SPACE" -> {
                if (isKarBarActive) {
                    isKarBarActive = false
                    rebuildKeyBoxes()
                }
                onKeyAction?.invoke(" ")
            }
            key == "DEL" || key == "ENTER" -> {
                if (isKarBarActive) {
                    isKarBarActive = false
                    rebuildKeyBoxes()
                }
                onKeyAction?.invoke(key)
            }
            else -> {
                val dyn = activeDynamicLayout
                val out = if (dyn == null && !isSymbols && !isNumberPad && !isShifted) key.lowercase() else key
                onKeyAction?.invoke(out)

                var needsRebuild = false
                // Only single-shot Shift (ONCE) resets to lowercase; CAPS_LOCK stays locked ON!
                if (shiftMode == ShiftMode.ONCE) {
                    shiftMode = ShiftMode.OFF
                    needsRebuild = true
                }

                // Dynamic Kar / Matra vowel-bar state transitions for Indic alphabetical layouts
                if (dyn != null && dyn.karBar.isNotEmpty() && !isSymbols) {
                    val nextKarState = when {
                        out == "্" || out == "्" -> false // Hasanta/Virama readies next consonant for conjunct
                        dyn.isConsonant(out) -> true      // Consonant triggers dynamic Kar vowel-bar on Row 0!
                        else -> false                     // Selecting a Kar/vowel/punctuation restores vowel row
                    }
                    if (nextKarState != isKarBarActive) {
                        isKarBarActive = nextKarState
                        needsRebuild = true
                    }
                }

                if (needsRebuild) {
                    rebuildKeyBoxes()
                }
            }
        }
    }

    private fun blendColor(from: Int, to: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r, g, b)
    }

    companion object {
        private const val SPARK_FADE_MS = 460L
        private const val TRAIL_LIFETIME_MS = 380L
        private const val LONG_PRESS_DELAY_MS = 350L
        private const val DOUBLE_TAP_SHIFT_MS = 380L
        private const val IDLE_PAUSE_MS = 1500L
        private const val MAX_RGB_PARTICLES = 25
        private const val MAX_ACTIVE_GLOWS = 10
        private const val MAX_TRAIL_POINTS = 15
        private const val DELETE_INITIAL_DELAY_MS = 300L
        private const val DELETE_CHAR_REPEAT_INTERVAL_MS = 40L
        private const val DELETE_WORD_ACCEL_MS = 1200L
        private const val DELETE_WORD_REPEAT_INTERVAL_MS = 85L
    }
}
