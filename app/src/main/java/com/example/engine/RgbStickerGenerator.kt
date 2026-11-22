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

/**
 * Generates high-resolution GTA VI Vice City RGB neon glass text stickers as PNGs
 * inside `cacheDir/stickers/` and commits them directly to WhatsApp, Messenger,
 * Telegram, Instagram, etc. via `InputConnectionCompat.commitContent()` (with an
 * `ACTION_SEND` fallback when an editor does not declare `image/png` support).
 */
object RgbStickerGenerator {

    private const val STICKER_DIR = "stickers"
    private const val MAX_CACHED_STICKERS = 10
    private const val STICKER_WIDTH = 512
    private const val STICKER_HEIGHT = 512

    data class StickerArtifact(
        val file: File,
        val uri: Uri,
        val bitmap: Bitmap,
        val phrase: String
    )

    fun generateSticker(context: Context, rawPhrase: String): StickerArtifact? {
        val phrase = rawPhrase.trim().take(64)
        if (phrase.isEmpty()) return null

        val bitmap = renderStickerBitmap(context, phrase)
        val stickersDir = File(context.cacheDir, STICKER_DIR).apply {
            if (!exists()) mkdirs()
        }
        pruneOldStickers(stickersDir)

        val outFile = File(stickersDir, "rgb_sticker_${System.currentTimeMillis()}.png")
        val saved = runCatching {
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
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
            phrase = phrase
        )
    }

    /**
     * Commits the generated PNG sticker directly to the active chat app via
     * `InputConnectionCompat.commitContent()`. Returns `true` if committed directly,
     * or launches a system share sheet fallback when the target field does not accept PNGs.
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

        val supportsPng = mimeTypes.any { mime ->
            ClipDescription.compareMimeTypes(mime, "image/png") ||
                ClipDescription.compareMimeTypes(mime, "image/*")
        }

        if (supportsPng && inputConnection != null && editorInfo != null) {
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

            val description = ClipDescription(artifact.phrase, arrayOf("image/png"))
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

        // Fallback: open share sheet so user can still send sticker in apps without commitContent support
        shareStickerFallback(context, artifact)
        return false
    }

    fun shareStickerFallback(context: Context, artifact: StickerArtifact) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
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

    private fun renderStickerBitmap(context: Context, phrase: String): Bitmap {
        val bitmap = Bitmap.createBitmap(STICKER_WIDTH, STICKER_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT_BOLD

        // Measure dynamic text block first so the rounded glass card hugs the phrase cleanly
        val horizontalCardMargin = 42f
        val innerTextPaddingX = 36f
        val maxTextWidth = (STICKER_WIDTH - (horizontalCardMargin + innerTextPaddingX) * 2).toInt().coerceAtLeast(180)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.WHITE
        }

        val chosenSize = pickOptimalTextSize(phrase, textPaint, maxTextWidth)
        textPaint.textSize = chosenSize
        val textLayout = buildCenteredLayout(phrase, textPaint, maxTextWidth)

        val minCardHeight = 200f
        val maxCardHeight = STICKER_HEIGHT - 84f
        val cardHeight = (textLayout.height + 116f).coerceIn(minCardHeight, maxCardHeight)
        val cardTop = (STICKER_HEIGHT - cardHeight) / 2f
        val cardRect = RectF(
            horizontalCardMargin,
            cardTop,
            STICKER_WIDTH - horizontalCardMargin,
            cardTop + cardHeight
        )
        val cornerRadius = 46f

        val neonBorderShader = LinearGradient(
            cardRect.left,
            cardRect.top,
            cardRect.right,
            cardRect.bottom,
            intArrayOf(
                Color.parseColor("#FF2A85"), // Vice Sunset Pink
                Color.parseColor("#FF9E00"), // Miami Sunset Gold
                Color.parseColor("#00F5D4"), // Electric Cyan
                Color.parseColor("#B5179E")  // Deep Vice Violet
            ),
            floatArrayOf(0f, 0.34f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )

        // 1. Wide ambient outer RGB neon halo
        val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 18f
            shader = neonBorderShader
            maskFilter = BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, outerGlowPaint)

        // 2. Frosted dark obsidian glass card fill
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.right,
                cardRect.bottom,
                intArrayOf(
                    Color.parseColor("#EC150B28"),
                    Color.parseColor("#F00A0614"),
                    Color.parseColor("#EC1B0E2E")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardFillPaint)

        // 3. Top glass specular highlight wash
        val glassSheenRect = RectF(
            cardRect.left + 8f,
            cardRect.top + 8f,
            cardRect.right - 8f,
            cardRect.top + cardHeight * 0.44f
        )
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f,
                glassSheenRect.top,
                0f,
                glassSheenRect.bottom,
                Color.parseColor("#2AFFFFFF"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(glassSheenRect, cornerRadius - 6f, cornerRadius - 6f, sheenPaint)

        // 4. Crisp glowing RGB neon border stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            shader = neonBorderShader
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 5. Subtle sparkle accents in corners
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            color = Color.parseColor("#CC00F5D4")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦", cardRect.left + 32f, cardRect.top + 38f, sparklePaint)
        sparklePaint.color = Color.parseColor("#CCFF2A85")
        canvas.drawText("✦", cardRect.right - 32f, cardRect.bottom - 20f, sparklePaint)

        // 6. Glowing neon gradient phrase text centered inside card
        val textOriginX = cardRect.left + (cardRect.width() - maxTextWidth) / 2f
        val textOriginY = cardRect.centerY() - textLayout.height / 2f - 4f

        // Soft neon halo pass behind text
        val textHaloPaint = TextPaint(textPaint).apply {
            shader = null
            color = Color.parseColor("#AAFF2A85")
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }
        val haloLayout = buildCenteredLayout(phrase, textHaloPaint, maxTextWidth)

        canvas.save()
        canvas.translate(textOriginX, textOriginY)
        haloLayout.draw(canvas)

        // Primary crisp gradient text pass
        textPaint.shader = LinearGradient(
            0f,
            0f,
            maxTextWidth.toFloat(),
            textLayout.height.toFloat(),
            intArrayOf(
                Color.parseColor("#FF74B8"),
                Color.parseColor("#FFF07C"),
                Color.parseColor("#00F5D4")
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        val gradientLayout = buildCenteredLayout(phrase, textPaint, maxTextWidth)
        gradientLayout.draw(canvas)
        canvas.restore()

        // 7. Tiny subtle bottom brand pill inside glass card
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f
            this.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.parseColor("#8800F5D4")
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.14f
        }
        canvas.drawText("RAKIB OFFICIAL", cardRect.centerX(), cardRect.bottom - 18f, brandPaint)

        return bitmap
    }

    private fun pickOptimalTextSize(phrase: String, paint: TextPaint, maxWidth: Int): Float {
        val candidates = floatArrayOf(68f, 60f, 52f, 46f, 40f, 34f, 29f)
        for (size in candidates) {
            paint.textSize = size
            val layout = buildCenteredLayout(phrase, paint, maxWidth)
            if (layout.lineCount <= 3 && layout.height <= 270) {
                return size
            }
        }
        return 26f
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
