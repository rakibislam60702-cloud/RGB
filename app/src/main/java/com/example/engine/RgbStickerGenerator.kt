package com.example.engine

import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * RGB Sticker styles selectable inline inside AI Keyboard.
 * #1 Default option: [TEXT_ONLY_TRANSPARENT] renders ONLY glowing multi-color RGB neon text
 * on a 100% transparent background (NO border box, NO frame, NO dark background).
 */
enum class RgbStickerStyle(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val badgeIcon: String,
    val borderColors: IntArray,
    val textColors: IntArray,
    val cardBgColors: IntArray,
    val haloColor: Int,
    val accentPrimary: Int,
    val accentSecondary: Int,
    val accentSymbol: String,
    val isTransparentTextOnly: Boolean = false
) {
    TEXT_ONLY_TRANSPARENT(
        id = "text_only_transparent",
        displayName = "Text Only (Transparent)",
        subtitle = "শুধুমাত্র রঙিন লেখা • No Box",
        badgeIcon = "✨",
        borderColors = intArrayOf(
            Color.parseColor("#FF2A85"),
            Color.parseColor("#FFD60A"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#B565FF")
        ),
        textColors = intArrayOf(
            Color.parseColor("#FF2A85"),
            Color.parseColor("#FFD60A"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#B565FF")
        ),
        cardBgColors = intArrayOf(
            Color.parseColor("#FF181428"),
            Color.parseColor("#FF101929")
        ),
        haloColor = Color.parseColor("#DD00F5D4"),
        accentPrimary = Color.parseColor("#00F5D4"),
        accentSecondary = Color.parseColor("#FF2A85"),
        accentSymbol = "✨",
        isTransparentTextOnly = true
    ),
    VICE_CITY_SUNSET(
        id = "vice_city_sunset",
        displayName = "Vice City Sunset",
        subtitle = "Pink-Orange Neon Glow",
        badgeIcon = "🌴",
        borderColors = intArrayOf(
            Color.parseColor("#FF2A85"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#FF9E00"),
            Color.parseColor("#FF2A85")
        ),
        textColors = intArrayOf(
            Color.parseColor("#FF74B8"),
            Color.parseColor("#FFD166"),
            Color.parseColor("#FF9E00")
        ),
        cardBgColors = intArrayOf(
            Color.parseColor("#F01C0B2A"),
            Color.parseColor("#F40E0618"),
            Color.parseColor("#F02A0C26")
        ),
        haloColor = Color.parseColor("#CCFF2A85"),
        accentPrimary = Color.parseColor("#FF2A85"),
        accentSecondary = Color.parseColor("#FF9E00"),
        accentSymbol = "✦"
    ),
    ELECTRIC_CYAN(
        id = "electric_cyan",
        displayName = "Electric Cyan",
        subtitle = "Cyber Blue Glow",
        badgeIcon = "⚡",
        borderColors = intArrayOf(
            Color.parseColor("#00F5D4"),
            Color.parseColor("#00B4D8"),
            Color.parseColor("#38BDF8"),
            Color.parseColor("#00F5D4")
        ),
        textColors = intArrayOf(
            Color.parseColor("#E0FBFC"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#38BDF8")
        ),
        cardBgColors = intArrayOf(
            Color.parseColor("#F0071A2B"),
            Color.parseColor("#F4050E1B"),
            Color.parseColor("#F00A2238")
        ),
        haloColor = Color.parseColor("#CC00E5FF"),
        accentPrimary = Color.parseColor("#00F5D4"),
        accentSecondary = Color.parseColor("#38BDF8"),
        accentSymbol = "⚡"
    ),
    MATRIX_PULSE(
        id = "matrix_pulse",
        displayName = "Matrix Pulse",
        subtitle = "Neon Green Frame",
        badgeIcon = "🟩",
        borderColors = intArrayOf(
            Color.parseColor("#00FF66"),
            Color.parseColor("#00D68F"),
            Color.parseColor("#70FFBA"),
            Color.parseColor("#00C853")
        ),
        textColors = intArrayOf(
            Color.parseColor("#D8FFE8"),
            Color.parseColor("#00FF66"),
            Color.parseColor("#00E676")
        ),
        cardBgColors = intArrayOf(
            Color.parseColor("#F0061B12"),
            Color.parseColor("#F4030E09"),
            Color.parseColor("#F0092619")
        ),
        haloColor = Color.parseColor("#CC00FF66"),
        accentPrimary = Color.parseColor("#00FF66"),
        accentSecondary = Color.parseColor("#70FFBA"),
        accentSymbol = "◈"
    ),
    ROYAL_GOLD(
        id = "royal_gold",
        displayName = "Royal Gold",
        subtitle = "Luxury Amber Glow",
        badgeIcon = "👑",
        borderColors = intArrayOf(
            Color.parseColor("#FFF3B0"),
            Color.parseColor("#FFD60A"),
            Color.parseColor("#FF9E00"),
            Color.parseColor("#FFD60A")
        ),
        textColors = intArrayOf(
            Color.parseColor("#FFF8D6"),
            Color.parseColor("#FFD60A"),
            Color.parseColor("#FF9E00")
        ),
        cardBgColors = intArrayOf(
            Color.parseColor("#F21C1408"),
            Color.parseColor("#F5100B05"),
            Color.parseColor("#F224190A")
        ),
        haloColor = Color.parseColor("#CCFFB703"),
        accentPrimary = Color.parseColor("#FFD60A"),
        accentSecondary = Color.parseColor("#FF9E00"),
        accentSymbol = "♛"
    ),
    FIRE_WAVE(
        id = "fire_wave",
        displayName = "Fire Wave",
        subtitle = "Multi-Color Gradient",
        badgeIcon = "🔥",
        borderColors = intArrayOf(
            Color.parseColor("#FF2A85"),
            Color.parseColor("#FF5E00"),
            Color.parseColor("#FFD60A"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#9D4EDD")
        ),
        textColors = intArrayOf(
            Color.parseColor("#FF4D6D"),
            Color.parseColor("#FFD60A"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#FF74B8")
        ),
        cardBgColors = intArrayOf(
            Color.parseColor("#F0220A1E"),
            Color.parseColor("#F4120718"),
            Color.parseColor("#F0141C2B")
        ),
        haloColor = Color.parseColor("#CCFF5E00"),
        accentPrimary = Color.parseColor("#FF5E00"),
        accentSecondary = Color.parseColor("#00F5D4"),
        accentSymbol = "🔥"
    ),
    GALAXY_NEON(
        id = "galaxy_neon",
        displayName = "Galaxy Neon",
        subtitle = "Purple-Cyan Starlight",
        badgeIcon = "🌌",
        borderColors = intArrayOf(
            Color.parseColor("#C77DFF"),
            Color.parseColor("#9D4EDD"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#7209B7")
        ),
        textColors = intArrayOf(
            Color.parseColor("#F3D9FF"),
            Color.parseColor("#C77DFF"),
            Color.parseColor("#00F5D4")
        ),
        cardBgColors = intArrayOf(
            Color.parseColor("#F2140A2C"),
            Color.parseColor("#F50A0619"),
            Color.parseColor("#F2111834")
        ),
        haloColor = Color.parseColor("#CC9D4EDD"),
        accentPrimary = Color.parseColor("#C77DFF"),
        accentSecondary = Color.parseColor("#00F5D4"),
        accentSymbol = "★"
    )
}

/**
 * Generates high-resolution box-free transparent or framed RGB neon text stickers as
 * lossless WebP / PNG inside `cacheDir/stickers/` and commits them directly to WhatsApp,
 * Messenger, Telegram, Instagram, etc. via `InputConnectionCompat.commitContent()`
 * (with an `ACTION_SEND` fallback when an editor does not declare sticker MIME support).
 */
object RgbStickerGenerator {

    private const val STICKER_DIR = "stickers"
    private const val MAX_CACHED_STICKERS = 12
    private const val STICKER_WIDTH = 512
    private const val STICKER_HEIGHT = 512

    data class StickerArtifact(
        val file: File,
        val uri: Uri,
        val bitmap: Bitmap,
        val phrase: String,
        val style: RgbStickerStyle,
        val mimeType: String = "image/png"
    )

    fun generateSticker(
        context: Context,
        rawPhrase: String,
        style: RgbStickerStyle = RgbStickerStyle.TEXT_ONLY_TRANSPARENT,
        editorInfo: EditorInfo? = null
    ): StickerArtifact? {
        val phrase = rawPhrase.trim().take(64)
        if (phrase.isEmpty()) return null

        val bitmap = renderStickerBitmap(context, phrase, style)
        val stickersDir = File(context.cacheDir, STICKER_DIR).apply {
            if (!exists()) mkdirs()
        }
        pruneOldStickers(stickersDir)

        // Prefer transparent WebP when the active chat app explicitly declares image/webp sticker support
        val preferWebp = shouldUseWebp(editorInfo)
        val ext = if (preferWebp) "webp" else "png"
        val mimeType = if (preferWebp) "image/webp" else "image/png"
        val outFile = File(stickersDir, "rgb_sticker_${style.id}_${System.currentTimeMillis()}.$ext")

        val saved = runCatching {
            FileOutputStream(outFile).use { fos ->
                if (preferWebp) {
                    val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    bitmap.compress(fmt, 100, fos)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                fos.flush()
            }
            true
        }.getOrDefault(false)

        if (!saved) {
            bitmap.recycle()
            return null
        }

        val authority = "${context.packageName}.stickerprovider"
        val contentUri = runCatching {
            FileProvider.getUriForFile(context, authority, outFile)
        }.getOrNull() ?: run {
            bitmap.recycle()
            return null
        }

        return StickerArtifact(
            file = outFile,
            uri = contentUri,
            bitmap = bitmap,
            phrase = phrase,
            style = style,
            mimeType = mimeType
        )
    }

    private fun shouldUseWebp(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        return mimeTypes.any { mime ->
            ClipDescription.compareMimeTypes(mime, "image/webp")
        }
    }

    /**
     * Commits the generated WebP/PNG sticker directly to the active chat app via
     * `InputConnectionCompat.commitContent()`. Returns `true` if committed directly,
     * or launches a system share sheet fallback when the target field does not accept image content.
     */
    fun sendStickerToActiveApp(
        context: Context,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        artifact: StickerArtifact
    ): Boolean {
        val mimeTypes = if (editorInfo != null) {
            EditorInfoCompat.getContentMimeTypes(editorInfo)
        } else {
            emptyArray()
        }

        val supportsArtifactMime = mimeTypes.any { mime ->
            ClipDescription.compareMimeTypes(mime, artifact.mimeType) ||
                ClipDescription.compareMimeTypes(mime, "image/webp") ||
                ClipDescription.compareMimeTypes(mime, "image/png") ||
                ClipDescription.compareMimeTypes(mime, "image/*")
        }

        if (supportsArtifactMime && inputConnection != null && editorInfo != null) {
            val targetPackage = editorInfo.packageName
            if (!targetPackage.isNullOrBlank()) {
                runCatching {
                    context.grantUriPermission(
                        targetPackage,
                        artifact.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            val description = ClipDescription(artifact.phrase, arrayOf(artifact.mimeType))
            val inputContentInfo = InputContentInfoCompat(artifact.uri, description, null)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            } else {
                0
            }

            val committed = runCatching {
                InputConnectionCompat.commitContent(
                    inputConnection,
                    editorInfo,
                    inputContentInfo,
                    flags,
                    null
                )
            }.getOrDefault(false)

            if (committed) {
                return true
            }
        }

        shareStickerFallback(context, artifact)
        return false
    }

    fun shareStickerFallback(context: Context, artifact: StickerArtifact) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = artifact.mimeType
            putExtra(Intent.EXTRA_STREAM, artifact.uri)
            putExtra(Intent.EXTRA_TEXT, artifact.phrase)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Send RGB Neon Sticker").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    private fun renderStickerBitmap(
        context: Context,
        phrase: String,
        style: RgbStickerStyle
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(STICKER_WIDTH, STICKER_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)

        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT_BOLD

        // #1 Default Box-Free Transparent RGB Text Sticker (শুধুমাত্র রঙিন লেখা):
        // 100% transparent background (Color.TRANSPARENT) — ZERO box, ZERO frame, ZERO dark backdrop!
        if (style.isTransparentTextOnly) {
            renderBoxFreeTransparentRgbText(canvas, phrase, typeface, style)
            return bitmap
        }

        val horizontalCardMargin = 42f
        val innerTextPaddingX = 36f
        val maxTextWidth = (STICKER_WIDTH - (horizontalCardMargin + innerTextPaddingX) * 2).toInt().coerceAtLeast(180)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.WHITE
        }

        val chosenSize = pickOptimalTextSize(phrase, textPaint, maxTextWidth, isTransparent = false)
        textPaint.textSize = chosenSize
        val textLayout = buildCenteredLayout(phrase, textPaint, maxTextWidth)

        val minCardHeight = 204f
        val maxCardHeight = STICKER_HEIGHT - 84f
        val cardHeight = (textLayout.height + 120f).coerceIn(minCardHeight, maxCardHeight)
        val cardTop = (STICKER_HEIGHT - cardHeight) / 2f
        val cardRect = RectF(
            horizontalCardMargin,
            cardTop,
            STICKER_WIDTH - horizontalCardMargin,
            cardTop + cardHeight
        )
        val cornerRadius = when (style) {
            RgbStickerStyle.MATRIX_PULSE -> 26f
            RgbStickerStyle.ROYAL_GOLD -> 34f
            else -> 46f
        }

        val neonBorderShader = LinearGradient(
            cardRect.left,
            cardRect.top,
            cardRect.right,
            cardRect.bottom,
            style.borderColors,
            null,
            Shader.TileMode.CLAMP
        )

        // 1. Wide ambient outer RGB neon halo
        val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 20f
            shader = neonBorderShader
            maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, outerGlowPaint)

        // 2. Frosted dark glass card fill styled for this theme
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.right,
                cardRect.bottom,
                style.cardBgColors,
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardFillPaint)

        // 3. Style-specific decorative backdrop inside clipped glass card
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        drawStyleBackdropArt(canvas, cardRect, style)

        // Top glass specular sheen
        val glassSheenRect = RectF(
            cardRect.left + 8f,
            cardRect.top + 8f,
            cardRect.right - 8f,
            cardRect.top + cardHeight * 0.42f
        )
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            shader = LinearGradient(
                0f,
                glassSheenRect.top,
                0f,
                glassSheenRect.bottom,
                Color.parseColor("#26FFFFFF"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(glassSheenRect, cornerRadius - 6f, cornerRadius - 6f, sheenPaint)
        canvas.restore()

        // 4. Crisp glowing RGB neon border stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 7f
            shader = neonBorderShader
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // Optional luxury double inner hairline for Royal Gold & Matrix Pulse
        if (style == RgbStickerStyle.ROYAL_GOLD || style == RgbStickerStyle.MATRIX_PULSE) {
            val innerRect = RectF(
                cardRect.left + 12f,
                cardRect.top + 12f,
                cardRect.right - 12f,
                cardRect.bottom - 12f
            )
            val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = 2f
                color = style.accentPrimary
                alpha = 115
            }
            canvas.drawRoundRect(innerRect, (cornerRadius - 8f).coerceAtLeast(10f), (cornerRadius - 8f).coerceAtLeast(10f), innerStroke)
        }

        // 5. Corner sparkle / theme accents
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 23f
            color = style.accentPrimary
            alpha = 215
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(style.accentSymbol, cardRect.left + 32f, cardRect.top + 38f, sparklePaint)
        sparklePaint.color = style.accentSecondary
        canvas.drawText(style.accentSymbol, cardRect.right - 32f, cardRect.bottom - 20f, sparklePaint)

        // 6. Glowing neon gradient phrase text centered inside card
        val textOriginX = cardRect.left + (cardRect.width() - maxTextWidth) / 2f
        val textOriginY = cardRect.centerY() - textLayout.height / 2f - 4f

        val textHaloPaint = TextPaint(textPaint).apply {
            shader = null
            color = style.haloColor
            maskFilter = BlurMaskFilter(13f, BlurMaskFilter.Blur.NORMAL)
        }
        val haloLayout = buildCenteredLayout(phrase, textHaloPaint, maxTextWidth)

        canvas.save()
        canvas.translate(textOriginX, textOriginY)
        haloLayout.draw(canvas)

        textPaint.shader = LinearGradient(
            0f,
            0f,
            maxTextWidth.toFloat(),
            textLayout.height.toFloat(),
            style.textColors,
            null,
            Shader.TileMode.CLAMP
        )
        val gradientLayout = buildCenteredLayout(phrase, textPaint, maxTextWidth)
        gradientLayout.draw(canvas)
        canvas.restore()

        // 7. Signature "RAKIB OFFICIAL" neon footer inside card
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14.5f
            this.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = style.accentSecondary
            alpha = 165
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.16f
        }
        canvas.drawText("RAKIB OFFICIAL", cardRect.centerX(), cardRect.bottom - 18f, brandPaint)

        return bitmap
    }

    /**
     * Renders ONLY crisp, glowing multi-color RGB neon text on a 100% transparent canvas.
     * No border box, no frame, no dark rectangle — floats cleanly over any chat wallpaper!
     */
    private fun renderBoxFreeTransparentRgbText(
        canvas: Canvas,
        phrase: String,
        typeface: Typeface,
        style: RgbStickerStyle
    ) {
        val horizontalMargin = 28
        val maxTextWidth = (STICKER_WIDTH - horizontalMargin * 2).coerceAtLeast(220)

        val basePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.WHITE
        }

        val chosenSize = pickOptimalTextSize(phrase, basePaint, maxTextWidth, isTransparent = true)
        basePaint.textSize = chosenSize
        val measureLayout = buildCenteredLayout(phrase, basePaint, maxTextWidth)

        val originX = (STICKER_WIDTH - maxTextWidth) / 2f
        val originY = (STICKER_HEIGHT - measureLayout.height) / 2f

        val rgbShader = LinearGradient(
            0f,
            0f,
            maxTextWidth.toFloat(),
            measureLayout.height.toFloat().coerceAtLeast(chosenSize),
            style.textColors,
            null,
            Shader.TileMode.CLAMP
        )

        canvas.save()
        canvas.translate(originX, originY)

        // Layer 1: Wide multi-color RGB outer neon glow floating freely in transparent space
        val outerGlowPaint = TextPaint(basePaint).apply {
            this.style = Paint.Style.FILL_AND_STROKE
            strokeWidth = (chosenSize * 0.14f).coerceIn(5f, 12f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            shader = rgbShader
            alpha = 215
            maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
        }
        buildCenteredLayout(phrase, outerGlowPaint, maxTextWidth).draw(canvas)

        // Layer 2: Tight crisp dark edge stroke directly on letter contours for legibility on light & dark chat themes
        val contourPaint = TextPaint(basePaint).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = (chosenSize * 0.10f).coerceIn(4f, 8.5f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            shader = null
            color = Color.parseColor("#E6090B14")
        }
        buildCenteredLayout(phrase, contourPaint, maxTextWidth).draw(canvas)

        // Layer 3: Razor-sharp vibrant multi-color RGB neon gradient fill
        val fillPaint = TextPaint(basePaint).apply {
            this.style = Paint.Style.FILL
            shader = rgbShader
        }
        buildCenteredLayout(phrase, fillPaint, maxTextWidth).draw(canvas)

        canvas.restore()
    }

    private fun drawStyleBackdropArt(canvas: Canvas, rect: RectF, style: RgbStickerStyle) {
        when (style) {
            RgbStickerStyle.TEXT_ONLY_TRANSPARENT -> Unit
            RgbStickerStyle.GALAXY_NEON -> {
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                }
                val coords = listOf(
                    0.14f to 0.24f, 0.28f to 0.18f, 0.72f to 0.20f, 0.86f to 0.30f,
                    0.18f to 0.78f, 0.36f to 0.84f, 0.68f to 0.82f, 0.84f to 0.72f,
                    0.50f to 0.14f, 0.90f to 0.52f, 0.10f to 0.50f
                )
                coords.forEachIndexed { idx, (fx, fy) ->
                    starPaint.color = if (idx % 2 == 0) style.accentSecondary else Color.WHITE
                    starPaint.alpha = if (idx % 3 == 0) 200 else 120
                    val radius = if (idx % 3 == 0) 3.6f else 2.2f
                    canvas.drawCircle(rect.left + rect.width() * fx, rect.top + rect.height() * fy, radius, starPaint)
                }
            }
            RgbStickerStyle.MATRIX_PULSE -> {
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    color = style.accentPrimary
                    alpha = 32
                }
                var x = rect.left + 32f
                while (x < rect.right) {
                    canvas.drawLine(x, rect.top, x, rect.bottom, gridPaint)
                    x += 36f
                }
            }
            RgbStickerStyle.FIRE_WAVE -> {
                val wavePath = Path()
                val baseY = rect.bottom - 34f
                wavePath.moveTo(rect.left, baseY)
                val steps = 24
                for (i in 0..steps) {
                    val ratio = i.toFloat() / steps
                    val x = rect.left + rect.width() * ratio
                    val y = baseY + (sin(ratio * 4f * PI) * 9f).toFloat()
                    wavePath.lineTo(x, y)
                }
                val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 5f
                    shader = LinearGradient(
                        rect.left, baseY, rect.right, baseY,
                        style.borderColors, null, Shader.TileMode.CLAMP
                    )
                    alpha = 140
                }
                canvas.drawPath(wavePath, wavePaint)
            }
            RgbStickerStyle.ELECTRIC_CYAN -> {
                val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 1.4f
                    color = style.accentPrimary
                    alpha = 26
                }
                var y = rect.top + 22f
                while (y < rect.bottom) {
                    canvas.drawLine(rect.left, y, rect.right, y, scanPaint)
                    y += 18f
                }
            }
            RgbStickerStyle.VICE_CITY_SUNSET, RgbStickerStyle.ROYAL_GOLD -> {
                val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = style.accentPrimary
                    alpha = 28
                    maskFilter = BlurMaskFilter(36f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawCircle(rect.left + rect.width() * 0.22f, rect.top + rect.height() * 0.25f, 68f, orbPaint)
                orbPaint.color = style.accentSecondary
                canvas.drawCircle(rect.right - rect.width() * 0.22f, rect.bottom - rect.height() * 0.25f, 68f, orbPaint)
            }
        }
    }

    private fun pickOptimalTextSize(
        phrase: String,
        paint: TextPaint,
        maxWidth: Int,
        isTransparent: Boolean
    ): Float {
        val candidates = if (isTransparent) {
            floatArrayOf(92f, 80f, 70f, 62f, 54f, 46f, 38f, 32f)
        } else {
            floatArrayOf(68f, 60f, 52f, 46f, 40f, 34f, 29f)
        }
        val maxHeight = if (isTransparent) 380 else 270
        for (size in candidates) {
            paint.textSize = size
            val layout = buildCenteredLayout(phrase, paint, maxWidth)
            if (layout.lineCount <= 3 && layout.height <= maxHeight) {
                return size
            }
        }
        return if (isTransparent) 28f else 26f
    }

    private fun buildCenteredLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(6f, 1.08f)
            .setIncludePad(false)
            .build()
    }

    private fun pruneOldStickers(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size > MAX_CACHED_STICKERS) {
            files.drop(MAX_CACHED_STICKERS).forEach { runCatching { it.delete() } }
        }
    }
}
