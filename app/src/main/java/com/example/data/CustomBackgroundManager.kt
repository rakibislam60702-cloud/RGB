package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.R
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Immutable
data class BannerThemePreset(
    val id: String,
    val title: String,
    @DrawableRes val drawableRes: Int? = null,
    val previewColors: List<Long>
)

object BannerPresetCatalog {
    const val PRESET_VICE_SUNSET_PALMS = "vice_sunset_palms"
    const val PRESET_VICE_NEON_DRIVE = "vice_neon_drive"
    const val PRESET_NEON_CYBER = "neon_cyber"
    const val PRESET_GALAXY_NEBULA = "galaxy_nebula"
    const val PRESET_DARK_CARBON = "dark_carbon"
    const val PRESET_SUNSET_GLOW = "sunset_glow"
    const val PRESET_ROYAL_BLUE = "royal_blue"
    const val PRESET_ABSTRACT_RGB = "abstract_rgb"
    const val PRESET_PRO_DECK = "pro_deck"
    const val PRESET_EMERALD_MATRIX = "emerald_matrix"

    val allPresets: List<BannerThemePreset> = listOf(
        BannerThemePreset(
            id = PRESET_VICE_SUNSET_PALMS,
            title = "Vice Sunset",
            drawableRes = R.drawable.img_preset_vice_sunset_palms_1795352797734,
            previewColors = listOf(0xFFFF2A85, 0xFFFF9E00, 0xFF00F5D4)
        ),
        BannerThemePreset(
            id = PRESET_VICE_NEON_DRIVE,
            title = "Vice Drive",
            drawableRes = R.drawable.img_preset_vice_neon_drive_1795352788244,
            previewColors = listOf(0xFF00F5D4, 0xFFFF2A85, 0xFF090612)
        ),
        BannerThemePreset(
            id = PRESET_NEON_CYBER,
            title = "Neon Cyber",
            drawableRes = R.drawable.img_preset_neon_cyber_1795322814680,
            previewColors = listOf(0xFF00E5FF, 0xFFFF00E5, 0xFF0D1020)
        ),
        BannerThemePreset(
            id = PRESET_GALAXY_NEBULA,
            title = "Galaxy Nebula",
            drawableRes = R.drawable.img_preset_galaxy_nebula_1795322814318,
            previewColors = listOf(0xFF7B2FFF, 0xFF00D4FF, 0xFF090816)
        ),
        BannerThemePreset(
            id = PRESET_DARK_CARBON,
            title = "Dark Carbon",
            drawableRes = null,
            previewColors = listOf(0xFF282D3A, 0xFF151822, 0xFF0B0D13)
        ),
        BannerThemePreset(
            id = PRESET_SUNSET_GLOW,
            title = "Sunset Glow",
            drawableRes = R.drawable.img_preset_sunset_glow_1795322815499,
            previewColors = listOf(0xFFFFB74D, 0xFFFF4D6D, 0xFF1D1135)
        ),
        BannerThemePreset(
            id = PRESET_ROYAL_BLUE,
            title = "Royal Blue",
            drawableRes = null,
            previewColors = listOf(0xFF2979FF, 0xFF0D47A1, 0xFF06132E)
        ),
        BannerThemePreset(
            id = PRESET_ABSTRACT_RGB,
            title = "Abstract RGB",
            drawableRes = R.drawable.img_preset_abstract_rgb_1795322814858,
            previewColors = listOf(0xFF00FFAA, 0xFF00D4FF, 0xFFD500F9)
        ),
        BannerThemePreset(
            id = PRESET_PRO_DECK,
            title = "Pro Deck",
            drawableRes = R.drawable.img_rgb_hero_banner_1795266996963,
            previewColors = listOf(0xFF00E5FF, 0xFFFF2A85, 0xFF121624)
        ),
        BannerThemePreset(
            id = PRESET_EMERALD_MATRIX,
            title = "Emerald Glow",
            drawableRes = null,
            previewColors = listOf(0xFF00E676, 0xFF00695C, 0xFF051914)
        )
    )

    fun findById(id: String?): BannerThemePreset? = allPresets.firstOrNull { it.id == id }
}

@Immutable
data class CustomPhotoTheme(
    val photoPath: String? = null,
    val presetId: String? = null,
    val dimAlpha: Float = 0.48f,
    val updatedAtMs: Long = 0L
) {
    val hasPreset: Boolean
        get() = BannerPresetCatalog.findById(presetId) != null

    val hasCustomGalleryPhoto: Boolean
        get() = !hasPreset && !photoPath.isNullOrBlank() && File(photoPath).exists()

    val hasPhoto: Boolean
        get() = hasPreset || (!photoPath.isNullOrBlank() && File(photoPath).exists())

    val cacheKey: String
        get() = when {
            hasPreset -> "preset:${presetId.orEmpty()}"
            !photoPath.isNullOrBlank() -> "file:$photoPath:$updatedAtMs"
            else -> "none"
        }
}

/**
 * Persists both built-in aesthetic banner presets and user-selected gallery photos
 * (`context.filesDir/custom_bg.jpg`) plus background dim/opacity in SharedPreferences so
 * themes survive restarts and sync live between MainActivity and RgbKeyboardService.
 */
class CustomBackgroundManager private constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readCurrentState())
    val theme: StateFlow<CustomPhotoTheme> = _theme.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _theme.value = readCurrentState()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    private fun readCurrentState(): CustomPhotoTheme {
        val savedPreset = prefs.getString(KEY_PRESET_ID, null)
            ?.takeIf { BannerPresetCatalog.findById(it) != null }
        val savedPath = prefs.getString(KEY_BG_PATH, null)
        val validPath = savedPath?.takeIf { it.isNotBlank() && File(it).exists() }
        val dim = prefs.getFloat(KEY_BG_DIM, DEFAULT_DIM_ALPHA).coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        val updatedAt = prefs.getLong(KEY_BG_UPDATED_AT, 0L)
        return CustomPhotoTheme(
            photoPath = if (savedPreset != null) null else validPath,
            presetId = savedPreset,
            dimAlpha = dim,
            updatedAtMs = updatedAt
        )
    }

    fun getCurrentTheme(): CustomPhotoTheme = readCurrentState()

    fun selectPreset(presetId: String) {
        if (BannerPresetCatalog.findById(presetId) == null) return
        val now = System.currentTimeMillis()
        val currentDim = prefs.getFloat(KEY_BG_DIM, DEFAULT_DIM_ALPHA).coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        prefs.edit()
            .putString(KEY_PRESET_ID, presetId)
            .putFloat(KEY_BG_DIM, currentDim)
            .putLong(KEY_BG_UPDATED_AT, now)
            .apply()
        _theme.value = readCurrentState()
    }

    /**
     * Copies and safely downscales the picked gallery image into `context.filesDir/custom_bg.jpg`,
     * clears any active built-in preset, and saves the path + current dim level into SharedPreferences.
     */
    suspend fun savePhotoFromUri(uri: Uri): Result<CustomPhotoTheme> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = File(context.filesDir, BG_FILE_NAME)
            val tempFile = File(context.filesDir, "$BG_FILE_NAME.tmp")

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }

            var inSample = 1
            val maxDim = 1280
            if (boundsOpts.outWidth > maxDim || boundsOpts.outHeight > maxDim) {
                while ((boundsOpts.outWidth / inSample) > maxDim || (boundsOpts.outHeight / inSample) > maxDim) {
                    inSample *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
            val decoded: Bitmap? = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }

            if (decoded != null) {
                FileOutputStream(tempFile).use { out ->
                    decoded.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    out.flush()
                }
                decoded.recycle()
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: error("Unable to open selected image stream")
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val now = System.currentTimeMillis()
            val currentDim = prefs.getFloat(KEY_BG_DIM, DEFAULT_DIM_ALPHA).coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
            prefs.edit()
                .remove(KEY_PRESET_ID)
                .putString(KEY_BG_PATH, targetFile.absolutePath)
                .putFloat(KEY_BG_DIM, currentDim)
                .putLong(KEY_BG_UPDATED_AT, now)
                .apply()

            val updated = CustomPhotoTheme(
                photoPath = targetFile.absolutePath,
                presetId = null,
                dimAlpha = currentDim,
                updatedAtMs = now
            )
            _theme.value = updated
            updated
        }
    }

    fun setDimAlpha(dimAlpha: Float) {
        val clamped = dimAlpha.coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        prefs.edit()
            .putFloat(KEY_BG_DIM, clamped)
            .apply()
        _theme.value = readCurrentState()
    }

    fun clearCustomPhoto() {
        val file = File(context.filesDir, BG_FILE_NAME)
        if (file.exists()) file.delete()
        prefs.edit()
            .remove(KEY_BG_PATH)
            .remove(KEY_PRESET_ID)
            .putLong(KEY_BG_UPDATED_AT, System.currentTimeMillis())
            .apply()
        _theme.value = readCurrentState()
    }

    fun decodeCachedBitmap(theme: CustomPhotoTheme): Bitmap? {
        val preset = BannerPresetCatalog.findById(theme.presetId)
        if (preset != null) {
            return if (preset.drawableRes != null) {
                runCatching {
                    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
                    BitmapFactory.decodeResource(context.resources, preset.drawableRes, opts)
                }.getOrNull()
            } else {
                renderProceduralPresetBitmap(preset.id)
            }
        }

        val path = theme.photoPath ?: return null
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    private fun renderProceduralPresetBitmap(presetId: String): Bitmap {
        val w = 960
        val h = 480
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (presetId) {
            BannerPresetCatalog.PRESET_DARK_CARBON -> {
                paint.shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(
                        Color.parseColor("#181C26"),
                        Color.parseColor("#0D1018"),
                        Color.parseColor("#1D2230")
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                paint.shader = null

                // Crisp metallic carbon-fiber weave pattern
                val cell = 24f
                val lightFiber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B3244") }
                val darkFiber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#131722") }
                val highlightFiber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3B455E") }
                var y = 0f
                var row = 0
                while (y < h) {
                    var x = if (row % 2 == 0) 0f else cell / 2f
                    while (x < w) {
                        canvas.drawRect(x, y, x + cell * 0.48f, y + cell * 0.44f, lightFiber)
                        canvas.drawRect(x, y, x + cell * 0.48f, y + 3f, highlightFiber)
                        canvas.drawRect(x + cell * 0.5f, y + cell * 0.5f, x + cell * 0.96f, y + cell * 0.94f, darkFiber)
                        x += cell
                    }
                    y += cell / 2f
                    row++
                }
                // Subtle diagonal specular sheen
                paint.shader = RadialGradient(
                    w * 0.25f, h * 0.2f, w * 0.65f,
                    intArrayOf(Color.argb(60, 0, 229, 255), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }

            BannerPresetCatalog.PRESET_ROYAL_BLUE -> {
                paint.shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(
                        Color.parseColor("#051233"),
                        Color.parseColor("#0D3B9E"),
                        Color.parseColor("#1A73E8"),
                        Color.parseColor("#081B47")
                    ),
                    floatArrayOf(0f, 0.35f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                // Luminous sapphire radial aurora orbs
                paint.shader = RadialGradient(
                    w * 0.78f, h * 0.28f, w * 0.45f,
                    intArrayOf(Color.argb(175, 64, 196, 255), Color.argb(55, 41, 121, 255), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                paint.shader = RadialGradient(
                    w * 0.22f, h * 0.82f, w * 0.5f,
                    intArrayOf(Color.argb(140, 83, 109, 254), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }

            else -> {
                // Emerald Glow / Matrix aesthetic
                paint.shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(
                        Color.parseColor("#031510"),
                        Color.parseColor("#063B2C"),
                        Color.parseColor("#041914")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                paint.shader = RadialGradient(
                    w * 0.5f, h * 0.3f, w * 0.52f,
                    intArrayOf(Color.argb(150, 0, 230, 118), Color.argb(45, 0, 229, 255), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(42, 0, 255, 160)
                    strokeWidth = 2f
                }
                var gx = 0f
                while (gx <= w) {
                    canvas.drawLine(gx, 0f, gx, h.toFloat(), gridPaint)
                    gx += 48f
                }
                var gy = 0f
                while (gy <= h) {
                    canvas.drawLine(0f, gy, w.toFloat(), gy, gridPaint)
                    gy += 48f
                }
            }
        }
        return bmp
    }

    companion object {
        private const val PREFS_NAME = "rakib_custom_theme_prefs"
        private const val KEY_BG_PATH = "custom_bg_path"
        private const val KEY_PRESET_ID = "custom_bg_preset_id"
        private const val KEY_BG_DIM = "custom_bg_dim"
        private const val KEY_BG_UPDATED_AT = "custom_bg_updated_at"
        private const val BG_FILE_NAME = "custom_bg.jpg"
        const val DEFAULT_DIM_ALPHA = 0.48f
        const val MIN_DIM_ALPHA = 0.15f
        const val MAX_DIM_ALPHA = 0.85f

        @Volatile
        private var instance: CustomBackgroundManager? = null

        fun getInstance(context: Context): CustomBackgroundManager {
            return instance ?: synchronized(this) {
                instance ?: CustomBackgroundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
