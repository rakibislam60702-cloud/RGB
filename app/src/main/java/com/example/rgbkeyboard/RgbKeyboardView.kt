package com.example.rgbkeyboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
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
import com.example.data.LanguagePackManager
import com.example.engine.RgbLightingEngine
import com.example.keyboard.KeyboardLayoutData
import com.example.model.DynamicKeyboardLayout
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.LanguageCatalog
import com.example.model.RgbEffect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Gboard-styled custom Canvas Keyboard View fused with Samsung Keys Cafe Touch Spark RGB
 * and dynamic on-demand JSON language packs loaded from `filesDir/layouts/`:
 * - Built-in lightweight 4-row English QWERTY with 1-0 long-press sub-labels.
 * - Dynamically loads downloaded JSON language layouts (5-row Bengali alphabetical + Kar bar,
 *   5-row Hindi alphabetical + Matra bar, 4-row Arabic, 4-row Urdu) from local storage.
 * - Gboard bottom row: ?123 | , (long-press emoji) | Globe | wide Spacebar | . | Circular Blue Enter/Search.
 * - Default "Touch Spark" mode: clean stealth semi-transparent dark keycaps at rest; touching any key
 *   triggers its own distinct vibrant neon glow + rising luminous pop-up bubble with smooth fade-out.
 */
class RgbKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onKeyAction: ((String) -> Unit)? = null
    var onKeyFeedback: ((Boolean) -> Unit)? = null
    var onEmojiModeChanged: ((Boolean) -> Unit)? = null
    var onLanguageToggleRequested: (() -> Unit)? = null
    var onCursorMove: ((Int) -> Unit)? = null
    var allowTopOverflow: Boolean = true

    private val languagePackManager = LanguagePackManager.getInstance(context)
    private var activeLanguageId: String = LanguageCatalog.ENGLISH_ID
    private var activeDynamicLayout: DynamicKeyboardLayout? = null
    private var isKarBarActive: Boolean = false

    private var isShift = false
    private var isSymbols = false
    private var isEmojiMode = false
    private var isSearchAction = false
    private var settings: KeyboardSettings = KeyboardSettings()

    private var rgbPhase = 0f
    private var pressedKeyBox: KeyBox? = null
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
        listOf("?123", ",", "GLOBE", "SPACE", ".", "ENTER")
    )
    private val enTopRowHints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    private val symRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "৳", "_", "&", "-", "+", "(", ")", "/"),
        listOf("=\\<", "*", "\"", "'", ":", ";", "!", "?", "DEL"),
        listOf("ABC", ",", "GLOBE", "SPACE", ".", "ENTER")
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
    private val popupBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val popupHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
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
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8C95AD")
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
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

    private val rgbAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3500L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rgbPhase = it.animatedValue as Float
            if (settings.effect != RgbEffect.TOUCH_SPARK || hasActiveVisualEffects()) {
                invalidate()
            }
        }
    }

    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeleting) {
                onKeyFeedback?.invoke(true)
                onKeyAction?.invoke("DEL")
                handler.postDelayed(this, 60L)
            }
        }
    }

    private val longPressRunnable = Runnable {
        val active = pressedKeyBox ?: return@Runnable
        if (isGliding || isSpaceDragging) return@Runnable

        if (active.rawKey == "," || active.rawKey == "،") {
            // Gboard Comma/Emoji long-press opens Emoji panel
            longPressTriggered = true
            if (settings.hapticEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onKeyFeedback?.invoke(true)
            releaseCurrentGlow(SystemClock.uptimeMillis())
            pressedKeyBox = null
            toggleEmojiMode()
            return@Runnable
        }

        val alt = active.longPressOutput ?: return@Runnable
        longPressTriggered = true
        if (settings.hapticEnabled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        // Swap the rising glowing pop-up bubble to show the long-pressed number immediately!
        activeGlows.lastOrNull { it.keyId == active.id }?.let { glow ->
            glow.displayLabel = alt
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!rgbAnimator.isStarted) {
            rgbAnimator.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rgbAnimator.cancel()
        isDeleting = false
        handler.removeCallbacks(deleteRunnable)
        handler.removeCallbacks(longPressRunnable)
    }

    fun setSearchAction(isSearch: Boolean) {
        if (isSearchAction != isSearch) {
            isSearchAction = isSearch
            invalidate()
        }
    }

    fun setActiveLanguage(langId: String) {
        val loaded = if (langId == LanguageCatalog.ENGLISH_ID) {
            null
        } else {
            languagePackManager.loadLayoutFromDisk(langId)
        }
        val resolvedId = if (loaded != null) loaded.id else LanguageCatalog.ENGLISH_ID
        val changed = resolvedId != activeLanguageId || loaded != activeDynamicLayout
        activeLanguageId = resolvedId
        activeDynamicLayout = loaded
        if (changed) {
            isSymbols = false
            isEmojiMode = false
            isShift = false
            isKarBarActive = false
            activeGlows.clear()
            onEmojiModeChanged?.invoke(false)
            rebuildKeyBoxes()
            invalidate()
        }
    }

    fun cycleDownloadedLanguage(): String {
        val downloaded = languagePackManager.getDownloadedLanguageIds()
        val cycleList = settings.enabledLanguageIds
            .filter { it == LanguageCatalog.ENGLISH_ID || it in downloaded }
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

        // Always verify dynamic JSON layout matches the requested activeLanguageId (handles fresh downloads too)
        setActiveLanguage(newSettings.activeLanguageId)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildKeyBoxes()
    }

    private fun hasActiveVisualEffects(): Boolean {
        return pressedKeyBox != null || activeGlows.isNotEmpty() || trailPoints.isNotEmpty()
    }

    private fun currentRows(): List<List<String>> {
        if (isSymbols) return symRows
        val dyn = activeDynamicLayout ?: return enRows
        if (isShift) return dyn.shiftRows
        if (isKarBarActive && dyn.karBar.isNotEmpty() && dyn.rows.isNotEmpty()) {
            // Dynamic Kar / Matra vowel-bar morphs Row 0 right after typing a consonant!
            return listOf(dyn.karBar) + dyn.rows.drop(1)
        }
        return dyn.rows
    }

    private fun rebuildKeyBoxes() {
        keyBoxes.clear()
        if (width <= 0 || height <= 0 || isEmojiMode) return

        val rows = currentRows()
        if (rows.isEmpty()) return

        val isFiveRow = rows.size >= 5
        val gapX = (if (isFiveRow) 3.8f else 5f) * density
        val gapY = (if (isFiveRow) 5.0f else 7f) * density
        val padTop = (if (isFiveRow) 3f else 4f) * density
        val padBottom = (if (isFiveRow) 3f else 4f) * density
        val usableH = (height.toFloat() - padTop - padBottom - gapY * (rows.size - 1)).coerceAtLeast(1f)
        val rowH = usableH / rows.size

        val dyn = activeDynamicLayout
        val isEnglish = dyn == null

        var curY = padTop
        rows.forEachIndexed { rIndex, row ->
            // Gboard symmetric inset for 9-key home rows (English ASDF or Urdu 9-key row)
            val useHomeRowInset = !isSymbols && rIndex == 1 && row.size == 9 && rows.size == 4
            val sideInset = if (useHomeRowInset) (width * 0.048f) else 0f

            val totalWeight = row.sumOf { keyWeight(it, isFiveRow).toDouble() }.toFloat().coerceAtLeast(1f)
            val totalGapX = gapX * (row.size + 1) + sideInset * 2f
            val usableW = (width.toFloat() - totalGapX).coerceAtLeast(1f)

            val isRowKarBar = !isSymbols && !isShift && isKarBarActive && rIndex == 0 && dyn != null && dyn.karBar.isNotEmpty()

            var curX = gapX + sideInset
            row.forEachIndexed { cIndex, rawKey ->
                val weight = keyWeight(rawKey, isFiveRow)
                val keyW = usableW * (weight / totalWeight)
                val rect = RectF(curX, curY, curX + keyW, curY + rowH)
                val keyId = "${activeLanguageId}_${rIndex}_${cIndex}_$rawKey"

                val displayLabel = when {
                    rawKey == "SPACE" -> dyn?.spacebarLabel ?: "English"
                    rawKey == "DEL" -> "⌫"
                    rawKey == "SHIFT" -> "⇧"
                    rawKey == "ENTER" -> "↵"
                    rawKey == "GLOBE" -> ""
                    rawKey == "ABC" -> dyn?.backToAlphaLabel ?: "ABC"
                    isEnglish && !isSymbols && rawKey.length == 1 ->
                        if (isShift) rawKey.uppercase() else rawKey.lowercase()
                    else -> rawKey
                }

                // Gboard top-right sub-label hints & long-press characters from built-in English or dynamic JSON
                val subLabel: String?
                val longPressOut: String?
                when {
                    !isSymbols && rIndex == 0 && isEnglish && cIndex < enTopRowHints.size -> {
                        subLabel = enTopRowHints[cIndex]
                        longPressOut = enTopRowHints[cIndex]
                    }
                    !isSymbols && rIndex == 0 && !isShift && !isRowKarBar && dyn != null && cIndex < dyn.topRowHints.size -> {
                        subLabel = dyn.topRowHints[cIndex]
                        longPressOut = dyn.topRowHints[cIndex]
                    }
                    !isSymbols && !isShift && dyn != null && dyn.longPressMap.containsKey(rawKey) -> {
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

        // Prune expired touch spark glows (smooth 460ms release fade-out)
        activeGlows.removeAll { glow ->
            val rel = glow.releasedAtMs
            rel != null && (nowMs - rel) > SPARK_FADE_MS
        }
        trailPoints.removeAll { nowMs - it.timeMs > TRAIL_LIFETIME_MS }

        if (isEmojiMode) {
            drawEmojiGrid(canvas)
            return
        }

        if (keyBoxes.isEmpty()) {
            rebuildKeyBoxes()
        }

        val isTouchSparkMode = settings.effect == RgbEffect.TOUCH_SPARK
        val cornerRadius = (settings.keyCornerRadiusDp * density * 0.82f).coerceIn(5.5f * density, 13f * density)

        val continuousShader: SweepGradient? = if (!isTouchSparkMode) {
            val shader = ensureSweepShader()
            val angle = if (settings.effect == RgbEffect.STATIC_NEON) 0f else rgbPhase
            shaderMatrix.setRotate(angle, width / 2f, height / 2f)
            shader.setLocalMatrix(shaderMatrix)
            val alphaFactor = if (settings.effect == RgbEffect.CYBER_BREATHING) {
                val breath = ((kotlin.math.sin(Math.toRadians(rgbPhase * 2.0)) + 1.0) * 0.5).toFloat()
                (0.35f + 0.65f * breath) * settings.glowIntensity
            } else {
                settings.glowIntensity
            }.coerceIn(0.25f, 1.0f)
            rgbBorderPaint.shader = shader
            rgbBorderPaint.alpha = (255 * alphaFactor).toInt()
            rgbFillPaint.shader = shader
            rgbFillPaint.alpha = (175 * alphaFactor).toInt()
            shader
        } else {
            null
        }

        // 1. Draw Gboard Keycaps (clean stealth dark semi-transparent at rest in Touch Spark)
        for (i in keyBoxes.indices) {
            val box = keyBoxes[i]
            val rect = box.rect
            val isPressed = pressedKeyBox?.id == box.id
            val glowEntry = activeGlows.lastOrNull { it.keyId == box.id }
            val glowIntensity = computeGlowFactor(glowEntry, nowMs)
            val isShiftLit = box.rawKey == "SHIFT" && isShift

            // Prominent circular blue Enter / Search button on the bottom-right corner (Gboard signature)
            if (box.rawKey == "ENTER") {
                drawGboardCircularActionKey(canvas, box, isPressed, glowIntensity)
                continue
            }

            if (isTouchSparkMode) {
                // Clean semi-transparent Gboard dark rounded keycaps (subtle cyan accent when Dynamic Kar bar is active)
                keyBgPaint.color = when {
                    box.isKarBarKey -> Color.argb(235, 22, 42, 58)
                    box.rawKey == "SPACE" -> Color.argb(232, 34, 37, 49)
                    isModifierKey(box.rawKey) || box.rawKey == "GLOBE" || isPunctuationKey(box.rawKey) ->
                        Color.argb(215, 25, 28, 39)
                    else -> Color.argb(230, 35, 39, 52)
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)

                if (box.isKarBarKey && glowIntensity <= 0.01f) {
                    sparkBorderPaint.color = Color.argb(120, 0, 229, 255)
                    sparkBorderPaint.strokeWidth = 1.0f * density
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, sparkBorderPaint)
                }

                // Touch Spark glow ignition in this key's own distinct vibrant neon color
                val effGlow = if (isShiftLit) maxOf(glowIntensity, 0.72f) else glowIntensity
                if (effGlow > 0.01f) {
                    val neon = box.neonColor
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
            } else {
                // Optional continuous RGB Studio modes
                when (settings.keycapStyle) {
                    KeycapStyle.PUDDING -> {
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rgbFillPaint)
                        tempRect.set(rect.left + 2.2f * density, rect.top + 1.5f * density, rect.right - 2.2f * density, rect.bottom - 4f * density)
                        val innerR = (cornerRadius - 2f * density).coerceAtLeast(3f)
                        canvas.drawRoundRect(tempRect, innerR, innerR, puddingInnerPaint)
                    }
                    KeycapStyle.NEON_WIREFRAME -> {
                        keyBgPaint.color = Color.parseColor("#11131A")
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)
                        rgbBorderPaint.strokeWidth = 1.9f * density
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rgbBorderPaint)
                    }
                    KeycapStyle.SOLID_MECH -> {
                        keyBgPaint.color = Color.parseColor("#222634")
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)
                        tempRect.set(rect.centerX() - rect.width() * 0.28f, rect.bottom - 4.5f * density, rect.centerX() + rect.width() * 0.28f, rect.bottom - 2f * density)
                        canvas.drawRoundRect(tempRect, 3f * density, 3f * density, rgbFillPaint)
                    }
                    KeycapStyle.FLOATING_GLOW -> {
                        keyBgPaint.color = Color.argb(230, 31, 34, 46)
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyBgPaint)
                        rgbBorderPaint.strokeWidth = 1.6f * density
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rgbBorderPaint)
                    }
                }

                if (glowIntensity > 0.01f || isShiftLit) {
                    val neon = box.neonColor
                    val factor = if (isShiftLit) maxOf(glowIntensity, 0.55f) else glowIntensity
                    sparkFillPaint.color = Color.argb(
                        (factor * 105).toInt().coerceIn(0, 145),
                        Color.red(neon),
                        Color.green(neon),
                        Color.blue(neon)
                    )
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, sparkFillPaint)
                }
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

            // Crisp centered keycap letter / label
            val rowH = rect.height()
            textPaint.typeface = when {
                box.rawKey == "SPACE" || isModifierKey(box.rawKey) ->
                    Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                else -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            textPaint.textSize = when {
                box.rawKey == "SPACE" -> (rowH * 0.29f).coerceIn(11f * scaledDensity, 13.5f * scaledDensity)
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

        // 2. Smooth fading multicolor Glide / Swipe trail
        drawGlideTrail(canvas, nowMs)

        // 3. Bright glowing rising pop-up bubbles above pressed / fading keys
        drawRisingSparkBubbles(canvas, nowMs)
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

    private fun drawRisingSparkBubbles(canvas: Canvas, nowMs: Long) {
        for (i in activeGlows.indices) {
            val glow = activeGlows[i]
            if (!glow.showPopup) continue

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
            val bubbleRect = RectF(cx - bubbleW / 2f, top, cx + bubbleW / 2f, bottom)

            val neon = glow.neonColor
            val r = Color.red(neon)
            val g = Color.green(neon)
            val b = Color.blue(neon)

            // Outer radial neon glow halo rising around the pop-up bubble
            val haloRadius = maxOf(bubbleW, bubbleH) * 0.92f
            popupHaloPaint.shader = RadialGradient(
                bubbleRect.centerX(),
                bubbleRect.centerY(),
                haloRadius,
                intArrayOf(
                    Color.argb((fade * 180 * settings.glowIntensity).toInt().coerceIn(0, 215), r, g, b),
                    Color.argb((fade * 65 * settings.glowIntensity).toInt().coerceIn(0, 110), r, g, b),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(bubbleRect.centerX(), bubbleRect.centerY(), haloRadius, popupHaloPaint)
            popupHaloPaint.shader = null

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
            popupTextPaint.setShadowLayer(12f * fade, 0f, 0f, neon)
            val fm = popupTextPaint.fontMetrics
            val ty = bubbleRect.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(glow.displayLabel, bubbleRect.centerX(), ty, popupTextPaint)
            popupTextPaint.clearShadowLayer()
        }
    }

    private fun drawGlideTrail(canvas: Canvas, nowMs: Long) {
        if (trailPoints.size < 2) return

        for (i in 1 until trailPoints.size) {
            val p0 = trailPoints[i - 1]
            val p1 = trailPoints[i]
            val age = (nowMs - p1.timeMs).toFloat()
            val life = (1f - age / TRAIL_LIFETIME_MS).coerceIn(0f, 1f)
            if (life <= 0.02f) continue

            val progress = i.toFloat() / trailPoints.size.toFloat()
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

    private fun keyWeight(key: String, isFiveRow: Boolean): Float = when (key) {
        "SPACE" -> 4.35f
        "?123", "?১২৩", "?१२३", "?١٢٣", "?۱۲۳", "ABC" -> 1.42f
        "ENTER" -> 1.48f
        "SHIFT", "DEL", "=\\<" -> if (isFiveRow) 1.25f else 1.45f
        "GLOBE", ",", "،", ".", "।", "।" , "۔" -> 0.92f
        else -> 1.0f
    }

    private fun isSymbolSwitchKey(key: String): Boolean =
        key == "?123" || key == "?১২৩" || key == "?१२३" || key == "?١٢٣" || key == "?۱۲۳"

    private fun isPunctuationKey(key: String): Boolean =
        key == "," || key == "،" || key == "." || key == "।" || key == "۔"

    private fun isModifierKey(key: String): Boolean =
        key == "SHIFT" || key == "DEL" || isSymbolSwitchKey(key) || key == "ABC" || key == "=\\<" || key == "ENTER"

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEmojiMode) {
            return handleEmojiTouch(event)
        }

        val x = event.x
        val y = event.y
        val nowMs = SystemClock.uptimeMillis()

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
                handler.removeCallbacks(longPressRunnable)

                val hit = findKeyAt(x, y)
                if (hit != null) {
                    pressedKeyBox = hit
                    activateKeyGlow(hit, nowMs)
                    if (settings.hapticEnabled) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    if (hit.rawKey == "DEL") {
                        onKeyFeedback?.invoke(true)
                        if (isKarBarActive) {
                            isKarBarActive = false
                            rebuildKeyBoxes()
                        }
                        onKeyAction?.invoke("DEL")
                        isDeleting = true
                        handler.postDelayed(deleteRunnable, 380L)
                    } else if (hit.longPressOutput != null || hit.rawKey == "," || hit.rawKey == "،") {
                        handler.postDelayed(longPressRunnable, LONG_PRESS_DELAY_MS)
                    }
                    invalidate()
                }
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
                if (movedDist > 15f * density && !longPressTriggered) {
                    isGliding = true
                    isDeleting = false
                    handler.removeCallbacks(deleteRunnable)
                    handler.removeCallbacks(longPressRunnable)

                    val hue = ((x / width.coerceAtLeast(1).toFloat()) * 320f + (nowMs % 3600L) * 0.1f) % 360f
                    val trailColor = RgbLightingEngine.hsvToColor(hue, 0.96f, 1.0f).toArgb()
                    trailPoints.add(TrailPoint(x, y, nowMs, trailColor))
                    if (trailPoints.size > 42) {
                        trailPoints.removeAt(0)
                    }

                    val hovered = findKeyAt(x, y)
                    if (hovered != null && hovered.id != pressedKeyBox?.id) {
                        releaseCurrentGlow(nowMs)
                        pressedKeyBox = hovered
                        activateKeyGlow(hovered, nowMs)
                    }
                    invalidate()
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

                if (released != null && released.rawKey != "DEL") {
                    if (longPressTriggered && released.longPressOutput != null) {
                        onKeyFeedback?.invoke(false)
                        onKeyAction?.invoke(released.longPressOutput)
                    } else if (released.rawKey == "SPACE" && isSpaceDragging) {
                        // Cursor swipe only
                    } else if (!isGliding) {
                        onKeyFeedback?.invoke(isModifierKey(released.rawKey) || released.rawKey == "GLOBE")
                        handleKeyPress(released.rawKey)
                    } else {
                        val landed = findKeyAt(x, y) ?: released
                        if (!isModifierKey(landed.rawKey) && landed.rawKey != "SPACE" && landed.rawKey != "GLOBE") {
                            onKeyFeedback?.invoke(false)
                            handleKeyPress(landed.rawKey)
                        }
                    }
                }
                longPressTriggered = false
                isSpaceDragging = false
                isGliding = false
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                handler.removeCallbacks(deleteRunnable)
                handler.removeCallbacks(longPressRunnable)
                isDeleting = false
                longPressTriggered = false
                releaseCurrentGlow(nowMs)
                pressedKeyBox = null
                isSpaceDragging = false
                isGliding = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun activateKeyGlow(box: KeyBox, nowMs: Long) {
        val showPopup = !isModifierKey(box.rawKey) && box.rawKey != "SPACE" && box.rawKey != "GLOBE"
        activeGlows.removeAll { it.keyId == box.id }
        activeGlows.add(
            ActiveKeyGlow(
                keyId = box.id,
                displayLabel = box.displayLabel,
                rect = RectF(box.rect),
                neonColor = box.neonColor,
                pressedAtMs = nowMs,
                releasedAtMs = null,
                showPopup = showPopup
            )
        )
        if (activeGlows.size > 14) {
            activeGlows.removeAt(0)
        }
    }

    private fun releaseCurrentGlow(nowMs: Long) {
        val activeId = pressedKeyBox?.id ?: return
        activeGlows.forEach { glow ->
            if (glow.keyId == activeId && glow.releasedAtMs == null) {
                glow.releasedAtMs = nowMs
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
                        handler.postDelayed(deleteRunnable, 380L)
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
                isShift = !isShift
                if (isShift) isKarBarActive = false
                rebuildKeyBoxes()
            }
            isSymbolSwitchKey(key) -> {
                isSymbols = true
                isKarBarActive = false
                rebuildKeyBoxes()
            }
            key == "ABC" || key == "=\\<" -> {
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
                val out = if (dyn == null && !isSymbols && !isShift) key.lowercase() else key
                onKeyAction?.invoke(out)

                var needsRebuild = false
                if (isShift) {
                    isShift = false
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
    }
}
