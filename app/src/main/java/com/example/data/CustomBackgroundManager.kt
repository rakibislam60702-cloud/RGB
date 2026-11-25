package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.R
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Immutable
data class BannerThemePreset(
    val id: String,
    val title: String,
    val subtitle: String = "",
    @DrawableRes val drawableRes: Int? = null,
    val previewColors: List<Long>,
    val isStaticGalleryBanner: Boolean = true
)

@Immutable
data class ComboThemePreset(
    val id: String,
    val title: String,
    val badge: String,
    val subtitle: String,
    val bannerPresetId: String,
    val effect: RgbEffect,
    val palette: RgbPalette,
    val previewColors: List<Long>
)

object BannerPresetCatalog {
    // --- Existing & Static HD Photo Banner IDs (10+ pure static banners) ---
    const val PRESET_SUNSET_GLOW = "sunset_glow"
    const val PRESET_VICE_NEON_DRIVE = "vice_neon_drive"
    const val PRESET_DARK_CARBON = "dark_carbon"
    const val PRESET_ROSE_GOLD_MARBLE = "rose_gold_marble"
    const val PRESET_CUTE_CARTOON = "cute_cartoon"
    const val PRESET_ABSTRACT_RGB = "abstract_rgb"
    const val PRESET_ROYAL_BLUE = "royal_blue"
    const val PRESET_PRO_DECK = "pro_deck"
    const val PRESET_SAKURA_NIGHT = "sakura_night"
    const val PRESET_MIDNIGHT_MOUNTAINS = "midnight_mountains"

    // --- Combo-Specific Rich HD Artwork IDs ---
    const val PRESET_CYBER_DRAGON_FIRE = "cyber_dragon_fire"
    const val PRESET_VICE_SUNSET_PALMS = "vice_sunset_palms"
    const val PRESET_GALAXY_NEBULA = "galaxy_nebula"
    const val PRESET_GOLDEN_ROYAL_TIGER = "golden_royal_tiger"
    const val PRESET_NEON_CYBER = "neon_cyber"
    const val PRESET_LOVE_HEART_GLOW = "love_heart_glow"
    const val PRESET_THUNDER_STORM = "thunder_storm"
    const val PRESET_EMERALD_MATRIX = "emerald_matrix"
    const val PRESET_AURORA_LIGHTS = "aurora_lights"
    const val PRESET_FIREFLY_NIGHT = "firefly_night"

    /** Section 3: 10 Pure Static HD Photo Banners (No Animation). */
    val staticBannerPresets: List<BannerThemePreset> = listOf(
        BannerThemePreset(
            id = PRESET_SUNSET_GLOW,
            title = "Sunset Beach",
            subtitle = "Golden twilight horizon",
            drawableRes = R.drawable.img_preset_sunset_glow_1795322815499,
            previewColors = listOf(0xFFFF9E00, 0xFFFF4D6D, 0xFF1D1135)
        ),
        BannerThemePreset(
            id = PRESET_VICE_NEON_DRIVE,
            title = "Neon Supercar",
            subtitle = "Outrun cyber highway",
            drawableRes = R.drawable.img_preset_vice_neon_drive_1795352788244,
            previewColors = listOf(0xFF00F5D4, 0xFFFF2A85, 0xFF090612)
        ),
        BannerThemePreset(
            id = PRESET_DARK_CARBON,
            title = "Dark Luxury Carbon",
            subtitle = "Woven matte fiber",
            drawableRes = null,
            previewColors = listOf(0xFF282D3A, 0xFF151822, 0xFF0B0D13)
        ),
        BannerThemePreset(
            id = PRESET_ROSE_GOLD_MARBLE,
            title = "Rose Gold Marble",
            subtitle = "Luxury veined stone",
            drawableRes = null,
            previewColors = listOf(0xFFF6C2C2, 0xFFD49A89, 0xFF3E242C)
        ),
        BannerThemePreset(
            id = PRESET_CUTE_CARTOON,
            title = "Cute Cartoon Sky",
            subtitle = "Pastel clouds & stars",
            drawableRes = null,
            previewColors = listOf(0xFFFF85B3, 0xFFA2D2FF, 0xFF5A4FCF)
        ),
        BannerThemePreset(
            id = PRESET_ABSTRACT_RGB,
            title = "Abstract 3D",
            subtitle = "Fluid chromatic glass",
            drawableRes = R.drawable.img_preset_abstract_rgb_1795322814858,
            previewColors = listOf(0xFF00FFAA, 0xFF00D4FF, 0xFFD500F9)
        ),
        BannerThemePreset(
            id = PRESET_ROYAL_BLUE,
            title = "Royal Sapphire",
            subtitle = "Deep luxury silk glow",
            drawableRes = null,
            previewColors = listOf(0xFF2979FF, 0xFF0D47A1, 0xFF06132E)
        ),
        BannerThemePreset(
            id = PRESET_PRO_DECK,
            title = "Pro Studio Deck",
            subtitle = "Gaming neon hardware",
            drawableRes = R.drawable.img_rgb_hero_banner_1795266996963,
            previewColors = listOf(0xFF00E5FF, 0xFFFF2A85, 0xFF121624)
        ),
        BannerThemePreset(
            id = PRESET_SAKURA_NIGHT,
            title = "Sakura Moon Night",
            subtitle = "Blossom moonlit dusk",
            drawableRes = null,
            previewColors = listOf(0xFFFF75A0, 0xFF6A2C70, 0xFF1A1034)
        ),
        BannerThemePreset(
            id = PRESET_MIDNIGHT_MOUNTAINS,
            title = "Midnight Peaks",
            subtitle = "Alpine twilight ridge",
            drawableRes = null,
            previewColors = listOf(0xFFFF6B00, 0xFF2B3A67, 0xFF0D1B3E)
        )
    )

    /** Additional rich HD artwork backgrounds powering Section 1 Combos. */
    private val comboArtworkPresets: List<BannerThemePreset> = listOf(
        BannerThemePreset(
            id = PRESET_CYBER_DRAGON_FIRE,
            title = "Cyber Dragon Fire",
            drawableRes = null,
            previewColors = listOf(0xFFFF4500, 0xFFFF9E00, 0xFF1B090C),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_VICE_SUNSET_PALMS,
            title = "Vice City Sunset",
            drawableRes = R.drawable.img_preset_vice_sunset_palms_1795352797734,
            previewColors = listOf(0xFFFF2A85, 0xFFFF9E00, 0xFF00F5D4),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_GALAXY_NEBULA,
            title = "Galaxy Nebula",
            drawableRes = R.drawable.img_preset_galaxy_nebula_1795322814318,
            previewColors = listOf(0xFF7B2FFF, 0xFF00D4FF, 0xFF090816),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_GOLDEN_ROYAL_TIGER,
            title = "Golden Royal Tiger",
            drawableRes = null,
            previewColors = listOf(0xFFFFB703, 0xFFFF6B00, 0xFF1A1107),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_NEON_CYBER,
            title = "AI Quantum Circuit",
            drawableRes = R.drawable.img_preset_neon_cyber_1795322814680,
            previewColors = listOf(0xFF00E5FF, 0xFFFF00E5, 0xFF0D1020),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_LOVE_HEART_GLOW,
            title = "Love Heart Glow",
            drawableRes = null,
            previewColors = listOf(0xFFFF2A85, 0xFFFF79B0, 0xFF280922),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_THUNDER_STORM,
            title = "Thunder Storm",
            drawableRes = null,
            previewColors = listOf(0xFF00E5FF, 0xFF3B82F6, 0xFF07112B),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_EMERALD_MATRIX,
            title = "Emerald Matrix",
            drawableRes = null,
            previewColors = listOf(0xFF00FF66, 0xFF00695C, 0xFF051914),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_AURORA_LIGHTS,
            title = "Aurora Borealis",
            drawableRes = null,
            previewColors = listOf(0xFF00F5D4, 0xFF7B2FFF, 0xFF08142E),
            isStaticGalleryBanner = false
        ),
        BannerThemePreset(
            id = PRESET_FIREFLY_NIGHT,
            title = "Enchanted Fireflies",
            drawableRes = null,
            previewColors = listOf(0xFFFFB703, 0xFF00F59B, 0xFF0B162C),
            isStaticGalleryBanner = false
        )
    )

    val allPresets: List<BannerThemePreset> = staticBannerPresets + comboArtworkPresets

    fun findById(id: String?): BannerThemePreset? = allPresets.firstOrNull { it.id == id }
}

/**
 * Section 1: 10 Luxury All-in-One 1-Tap Photo + AI Animation Combos.
 * Tapping 1 card applies BOTH the HD background artwork AND its matching AI RGB lighting animation together.
 */
object ComboThemeCatalog {
    val allCombos: List<ComboThemePreset> = listOf(
        ComboThemePreset(
            id = "combo_cyber_dragon_fire",
            title = "Cyber Dragon Fire",
            badge = "🔥 Plasma Vortex",
            subtitle = "Inferno dragon art + swirling plasma RGB",
            bannerPresetId = BannerPresetCatalog.PRESET_CYBER_DRAGON_FIRE,
            effect = RgbEffect.AI_PLASMA_VORTEX,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFF4500, 0xFFFF9E00, 0xFF200A0E)
        ),
        ComboThemePreset(
            id = "combo_vice_city_sunset",
            title = "Vice City Sunset",
            badge = "🌴 Neon Pulse",
            subtitle = "Miami palms HD + Cyber Breathing glow",
            bannerPresetId = BannerPresetCatalog.PRESET_VICE_SUNSET_PALMS,
            effect = RgbEffect.CYBER_BREATHING,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFF2A85, 0xFFFF9E00, 0xFF00F5D4)
        ),
        ComboThemePreset(
            id = "combo_galaxy_nebula",
            title = "Galaxy Nebula",
            badge = "✨ Starlight",
            subtitle = "Deep space cosmos + Starlight Embers",
            bannerPresetId = BannerPresetCatalog.PRESET_GALAXY_NEBULA,
            effect = RgbEffect.STARLIGHT,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFF7B2FFF, 0xFF00D4FF, 0xFF090816)
        ),
        ComboThemePreset(
            id = "combo_golden_royal_tiger",
            title = "Golden Royal Tiger",
            badge = "👑 Touch Spark",
            subtitle = "Royal gold stripes + Touch Spark keys",
            bannerPresetId = BannerPresetCatalog.PRESET_GOLDEN_ROYAL_TIGER,
            effect = RgbEffect.TOUCH_SPARK,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFFB703, 0xFFFF6B00, 0xFF1A1107)
        ),
        ComboThemePreset(
            id = "combo_ai_quantum_circuit",
            title = "AI Quantum Circuit",
            badge = "⚡ Spectrum Wave",
            subtitle = "Cyberpunk neural grid + Spectrum Wave",
            bannerPresetId = BannerPresetCatalog.PRESET_NEON_CYBER,
            effect = RgbEffect.RAINBOW_WAVE,
            palette = RgbPalette.CYBERPUNK,
            previewColors = listOf(0xFF00E5FF, 0xFFFF00E5, 0xFF0D1020)
        ),
        ComboThemePreset(
            id = "combo_love_heart_glow",
            title = "Love Heart Glow",
            badge = "❤️ Heartbeat",
            subtitle = "Neon romantic hearts + Heartbeat RGB",
            bannerPresetId = BannerPresetCatalog.PRESET_LOVE_HEART_GLOW,
            effect = RgbEffect.NEON_HEARTBEAT,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFFFF2A85, 0xFFFF79B0, 0xFF2A0823)
        ),
        ComboThemePreset(
            id = "combo_thunder_storm",
            title = "Thunder Storm",
            badge = "⚡ Lightning",
            subtitle = "Electric storm bolts + Lightning Arcs",
            bannerPresetId = BannerPresetCatalog.PRESET_THUNDER_STORM,
            effect = RgbEffect.LIGHTNING_ARC,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF00E5FF, 0xFF3B82F6, 0xFF07112B)
        ),
        ComboThemePreset(
            id = "combo_emerald_matrix",
            title = "Emerald Matrix",
            badge = "💚 Matrix Rain",
            subtitle = "Hacker cyber grid + Matrix Cascade",
            bannerPresetId = BannerPresetCatalog.PRESET_EMERALD_MATRIX,
            effect = RgbEffect.MATRIX_RAIN,
            palette = RgbPalette.EMERALD_MATRIX,
            previewColors = listOf(0xFF00FF66, 0xFF008F4C, 0xFF041914)
        ),
        ComboThemePreset(
            id = "combo_aurora_northern",
            title = "Aurora Northern",
            badge = "🌌 Aurora Flow",
            subtitle = "Polar ribbon sky + Aurora Flow RGB",
            bannerPresetId = BannerPresetCatalog.PRESET_AURORA_LIGHTS,
            effect = RgbEffect.AURORA_FLOW,
            palette = RgbPalette.SPECTRUM_PRISM,
            previewColors = listOf(0xFF00F5D4, 0xFF7B2FFF, 0xFF0A1734)
        ),
        ComboThemePreset(
            id = "combo_firefly_night",
            title = "Firefly Enchanted",
            badge = "✨ Firefly Sparks",
            subtitle = "Starlit forest dusk + Firefly Sparks",
            bannerPresetId = BannerPresetCatalog.PRESET_FIREFLY_NIGHT,
            effect = RgbEffect.FIREFLY_SPARKS,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFFB703, 0xFF00F59B, 0xFF0C1830)
        )
    )
}

@Immutable
data class CustomPhotoTheme(
    val photoPath: String? = null,
    val presetId: String? = null,
    val dimAlpha: Float = 0.44f,
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
 * Persists built-in HD combo/static banner presets and user-selected gallery photos
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

    private val proceduralBitmapCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

    /** Renders high-definition (960x480) rich vector/shader artwork for procedural themes, cached in memory. */
    fun renderProceduralPresetBitmap(presetId: String, w: Int = 960, h: Int = 480): Bitmap {
        val cacheKey = "$presetId:${w}x$h"
        proceduralBitmapCache[cacheKey]?.takeIf { !it.isRecycled }?.let { return it }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val wf = w.toFloat()
        val hf = h.toFloat()

        when (presetId) {
            BannerPresetCatalog.PRESET_CYBER_DRAGON_FIRE -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#18040B"), Color.parseColor("#6B0F1A"), Color.parseColor("#FF4500"), Color.parseColor("#1A050D")),
                    floatArrayOf(0f, 0.35f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                // Molten fire aura
                paint.shader = RadialGradient(
                    wf * 0.55f, hf * 0.45f, wf * 0.48f,
                    intArrayOf(Color.argb(220, 255, 190, 0), Color.argb(145, 255, 69, 0), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                // Stylized dragon flame arcs & embers
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                for (i in 0..7) {
                    val path = Path()
                    val yBase = hf * (0.2f + i * 0.095f)
                    path.moveTo(0f, yBase)
                    path.cubicTo(wf * 0.3f, yBase - hf * 0.22f, wf * 0.65f, yBase + hf * 0.2f, wf, yBase - hf * 0.1f)
                    paint.strokeWidth = (12f - i).coerceAtLeast(3f)
                    paint.color = if (i % 2 == 0) Color.argb(135, 255, 200, 40) else Color.argb(115, 255, 42, 133)
                    canvas.drawPath(path, paint)
                }
                paint.style = Paint.Style.FILL
                for (e in 0..38) {
                    val ex = ((e * 197) % w).toFloat()
                    val ey = ((e * 131) % h).toFloat()
                    paint.color = Color.argb(190, 255, 225, 120)
                    canvas.drawCircle(ex, ey, (2.5f + (e % 5) * 1.4f), paint)
                }
            }

            BannerPresetCatalog.PRESET_GOLDEN_ROYAL_TIGER -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#1A1004"), Color.parseColor("#B4690E"), Color.parseColor("#FFB703"), Color.parseColor("#2A1605")),
                    floatArrayOf(0f, 0.3f, 0.65f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
                // Bold dark tiger stripes sweeping diagonally
                val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(175, 18, 11, 5)
                    style = Paint.Style.FILL
                }
                for (s in 0..8) {
                    val sx = s * (wf / 8f)
                    val path = Path().apply {
                        moveTo(sx - 20f, 0f)
                        quadTo(sx + 55f, hf * 0.45f, sx - 15f, hf)
                        lineTo(sx + 26f, hf)
                        quadTo(sx + 92f, hf * 0.48f, sx + 18f, 0f)
                        close()
                    }
                    canvas.drawPath(path, stripePaint)
                }
                // Royal golden shimmer
                paint.shader = RadialGradient(
                    wf * 0.5f, hf * 0.45f, wf * 0.42f,
                    intArrayOf(Color.argb(150, 255, 230, 120), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }

            BannerPresetCatalog.PRESET_LOVE_HEART_GLOW -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#1D0620"), Color.parseColor("#7A0F4D"), Color.parseColor("#FF2A85"), Color.parseColor("#2A0835")),
                    floatArrayOf(0f, 0.38f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
                // Glowing layered vector neon hearts
                val hearts = listOf(
                    Triple(wf * 0.22f, hf * 0.36f, hf * 0.16f),
                    Triple(wf * 0.50f, hf * 0.48f, hf * 0.28f),
                    Triple(wf * 0.78f, hf * 0.34f, hf * 0.18f),
                    Triple(wf * 0.34f, hf * 0.74f, hf * 0.11f),
                    Triple(wf * 0.68f, hf * 0.72f, hf * 0.13f)
                )
                hearts.forEach { (cx, cy, r) ->
                    val heartPath = buildHeartPath(cx, cy, r)
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb(75, 255, 110, 175)
                    canvas.drawPath(heartPath, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6f
                    paint.color = Color.argb(210, 255, 190, 225)
                    canvas.drawPath(heartPath, paint)
                }
            }

            BannerPresetCatalog.PRESET_THUNDER_STORM -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#040B1F"), Color.parseColor("#0D2554"), Color.parseColor("#1E3A8A"), Color.parseColor("#050D22")),
                    floatArrayOf(0f, 0.42f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = RadialGradient(
                    wf * 0.52f, hf * 0.32f, wf * 0.44f,
                    intArrayOf(Color.argb(180, 0, 229, 255), Color.argb(75, 59, 130, 246), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
                // Crisp glowing electric lightning bolts
                drawLightningBolt(canvas, wf * 0.28f, 0f, wf * 0.36f, hf, Color.parseColor("#00F5FF"))
                drawLightningBolt(canvas, wf * 0.64f, 0f, wf * 0.54f, hf, Color.parseColor("#93C5FD"))
                drawLightningBolt(canvas, wf * 0.80f, hf * 0.08f, wf * 0.74f, hf * 0.92f, Color.parseColor("#00E5FF"))
            }

            BannerPresetCatalog.PRESET_AURORA_LIGHTS -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, hf,
                    intArrayOf(Color.parseColor("#050B24"), Color.parseColor("#0B1F3A"), Color.parseColor("#071226")),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                val ribbonColors = intArrayOf(
                    Color.argb(140, 0, 245, 212),
                    Color.argb(125, 0, 245, 155),
                    Color.argb(120, 123, 47, 255),
                    Color.argb(115, 0, 212, 255)
                )
                ribbonColors.forEachIndexed { idx, c ->
                    val path = Path()
                    val base = hf * (0.22f + idx * 0.16f)
                    path.moveTo(-40f, base)
                    path.cubicTo(wf * 0.28f, base - hf * 0.26f, wf * 0.66f, base + hf * 0.26f, wf + 40f, base - hf * 0.08f)
                    paint.color = c
                    paint.strokeWidth = 44f - idx * 6f
                    canvas.drawPath(path, paint)
                }
                paint.style = Paint.Style.FILL
                for (s in 0..45) {
                    paint.color = Color.argb(200, 255, 255, 255)
                    canvas.drawCircle(((s * 211) % w).toFloat(), ((s * 97) % h).toFloat(), 2f + (s % 3), paint)
                }
            }

            BannerPresetCatalog.PRESET_FIREFLY_NIGHT -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#061326"), Color.parseColor("#0F2A3F"), Color.parseColor("#081B29")),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                for (i in 0..22) {
                    val fx = ((i * 173 + 47) % w).toFloat()
                    val fy = ((i * 113 + 31) % h).toFloat()
                    val rad = 18f + (i % 4) * 14f
                    val glowColor = if (i % 3 == 0) Color.argb(190, 0, 245, 155) else Color.argb(210, 255, 200, 40)
                    paint.shader = RadialGradient(
                        fx, fy, rad,
                        intArrayOf(Color.WHITE, glowColor, Color.TRANSPARENT),
                        floatArrayOf(0f, 0.35f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(fx, fy, rad, paint)
                }
            }

            BannerPresetCatalog.PRESET_ROSE_GOLD_MARBLE -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(
                        Color.parseColor("#3B212A"),
                        Color.parseColor("#7D4A56"),
                        Color.parseColor("#C78B8B"),
                        Color.parseColor("#542D39")
                    ),
                    floatArrayOf(0f, 0.35f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
                paint.style = Paint.Style.STROKE
                for (v in 0..9) {
                    val p = Path()
                    val startY = (v * hf / 8f)
                    p.moveTo(0f, startY)
                    p.cubicTo(wf * 0.32f, startY + (if (v % 2 == 0) 68f else -55f), wf * 0.68f, startY - 40f, wf, startY + 28f)
                    paint.strokeWidth = if (v % 3 == 0) 5f else 2.5f
                    paint.color = if (v % 2 == 0) Color.argb(165, 255, 224, 185) else Color.argb(115, 255, 245, 240)
                    canvas.drawPath(p, paint)
                }
            }

            BannerPresetCatalog.PRESET_CUTE_CARTOON -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(
                        Color.parseColor("#FF75B5"),
                        Color.parseColor("#9B89F5"),
                        Color.parseColor("#5BC0F8")
                    ),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
                paint.style = Paint.Style.FILL
                // Fluffy kawaii clouds
                val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 255, 255, 255) }
                drawFluffyCloud(canvas, wf * 0.22f, hf * 0.74f, 68f, cloudPaint)
                drawFluffyCloud(canvas, wf * 0.76f, hf * 0.68f, 82f, cloudPaint)
                drawFluffyCloud(canvas, wf * 0.50f, hf * 0.30f, 54f, cloudPaint)
                // Pastel sparkle stars
                paint.color = Color.argb(230, 255, 246, 160)
                for (st in 0..14) {
                    val sx = ((st * 191 + 60) % w).toFloat()
                    val sy = ((st * 97 + 40) % h).toFloat()
                    drawSparkleStar(canvas, sx, sy, 10f + (st % 3) * 5f, paint)
                }
            }

            BannerPresetCatalog.PRESET_SAKURA_NIGHT -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#180E33"), Color.parseColor("#4B1F5C"), Color.parseColor("#A4386E"), Color.parseColor("#1D1038")),
                    floatArrayOf(0f, 0.38f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                // Glowing crescent/full moon
                paint.shader = RadialGradient(
                    wf * 0.76f, hf * 0.32f, 110f,
                    intArrayOf(Color.argb(235, 255, 240, 245), Color.argb(95, 255, 130, 180), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(wf * 0.76f, hf * 0.32f, 110f, paint)
                paint.shader = null
                // Floating sakura petals
                paint.color = Color.argb(195, 255, 185, 215)
                for (p in 0..28) {
                    val px = ((p * 179 + 25) % w).toFloat()
                    val py = ((p * 127 + 20) % h).toFloat()
                    canvas.save()
                    canvas.rotate((p * 33 % 360).toFloat(), px, py)
                    canvas.drawOval(px - 11f, py - 5f, px + 11f, py + 5f, paint)
                    canvas.restore()
                }
            }

            BannerPresetCatalog.PRESET_MIDNIGHT_MOUNTAINS -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, hf,
                    intArrayOf(Color.parseColor("#FF6B00"), Color.parseColor("#9B3150"), Color.parseColor("#1B264F"), Color.parseColor("#0D1B3E")),
                    floatArrayOf(0f, 0.34f, 0.68f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
                paint.style = Paint.Style.FILL
                // Back ridge
                paint.color = Color.argb(175, 20, 30, 68)
                val backPeak = Path().apply {
                    moveTo(0f, hf)
                    lineTo(0f, hf * 0.66f)
                    lineTo(wf * 0.24f, hf * 0.34f)
                    lineTo(wf * 0.48f, hf * 0.62f)
                    lineTo(wf * 0.76f, hf * 0.28f)
                    lineTo(wf, hf * 0.56f)
                    lineTo(wf, hf)
                    close()
                }
                canvas.drawPath(backPeak, paint)
                // Front deep blue ridge
                paint.color = Color.argb(225, 11, 22, 54)
                val frontPeak = Path().apply {
                    moveTo(0f, hf)
                    lineTo(0f, hf * 0.78f)
                    lineTo(wf * 0.34f, hf * 0.48f)
                    lineTo(wf * 0.62f, hf * 0.74f)
                    lineTo(wf * 0.88f, hf * 0.44f)
                    lineTo(wf, hf * 0.68f)
                    lineTo(wf, hf)
                    close()
                }
                canvas.drawPath(frontPeak, paint)
            }

            BannerPresetCatalog.PRESET_DARK_CARBON -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#181C26"), Color.parseColor("#0D1018"), Color.parseColor("#1D2230")),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null
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
                paint.shader = RadialGradient(
                    wf * 0.25f, hf * 0.2f, wf * 0.65f,
                    intArrayOf(Color.argb(60, 0, 229, 255), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }

            BannerPresetCatalog.PRESET_ROYAL_BLUE -> {
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#051233"), Color.parseColor("#0D3B9E"), Color.parseColor("#1A73E8"), Color.parseColor("#081B47")),
                    floatArrayOf(0f, 0.35f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = RadialGradient(
                    wf * 0.78f, hf * 0.28f, wf * 0.45f,
                    intArrayOf(Color.argb(175, 64, 196, 255), Color.argb(55, 41, 121, 255), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = RadialGradient(
                    wf * 0.22f, hf * 0.82f, wf * 0.5f,
                    intArrayOf(Color.argb(140, 83, 109, 254), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
            }

            else -> {
                // Emerald Matrix default procedural grid
                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(Color.parseColor("#031510"), Color.parseColor("#063B2C"), Color.parseColor("#041914")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = RadialGradient(
                    wf * 0.5f, hf * 0.3f, wf * 0.52f,
                    intArrayOf(Color.argb(150, 0, 230, 118), Color.argb(45, 0, 229, 255), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(48, 0, 255, 160)
                    strokeWidth = 2f
                }
                var gx = 0f
                while (gx <= wf) {
                    canvas.drawLine(gx, 0f, gx, hf, gridPaint)
                    gx += 48f
                }
                var gy = 0f
                while (gy <= hf) {
                    canvas.drawLine(0f, gy, wf, gy, gridPaint)
                    gy += 48f
                }
            }
        }
        proceduralBitmapCache[cacheKey] = bmp
        return bmp
    }

    private fun buildHeartPath(cx: Float, cy: Float, r: Float): Path {
        val path = Path()
        val steps = 60
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2.0 * Math.PI)
            val hx = 16.0 * sin(t) * sin(t) * sin(t)
            val hy = -(13.0 * cos(t) - 5.0 * cos(2 * t) - 2.0 * cos(3 * t) - cos(4 * t))
            val px = cx + (hx / 16.0).toFloat() * r
            val py = cy + (hy / 16.0).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }

    private fun drawLightningBolt(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, color: Int) {
        val path = Path().apply {
            moveTo(x0, y0)
            val dx = x1 - x0
            val dy = y1 - y0
            lineTo(x0 + dx * 0.25f - 28f, y0 + dy * 0.28f)
            lineTo(x0 + dx * 0.42f + 22f, y0 + dy * 0.34f)
            lineTo(x0 + dx * 0.58f - 32f, y0 + dy * 0.64f)
            lineTo(x0 + dx * 0.72f + 18f, y0 + dy * 0.70f)
            lineTo(x1, y1)
        }
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 18f
            this.color = Color.argb(95, Color.red(color), Color.green(color), Color.blue(color))
        }
        canvas.drawPath(path, glow)
        glow.strokeWidth = 5f
        glow.color = Color.WHITE
        canvas.drawPath(path, glow)
    }

    private fun drawFluffyCloud(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawCircle(cx - r * 0.85f, cy + r * 0.18f, r * 0.72f, paint)
        canvas.drawCircle(cx + r * 0.85f, cy + r * 0.18f, r * 0.72f, paint)
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.32f, r * 0.65f, paint)
        canvas.drawCircle(cx + r * 0.35f, cy - r * 0.28f, r * 0.68f, paint)
    }

    private fun drawSparkleStar(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - r)
            quadTo(cx, cy, cx + r, cy)
            quadTo(cx, cy, cx, cy + r)
            quadTo(cx, cy, cx - r, cy)
            quadTo(cx, cy, cx, cy - r)
            close()
        }
        canvas.drawPath(path, paint)
    }

    companion object {
        private const val PREFS_NAME = "rakib_custom_theme_prefs"
        private const val KEY_BG_PATH = "custom_bg_path"
        private const val KEY_PRESET_ID = "custom_bg_preset_id"
        private const val KEY_BG_DIM = "custom_bg_dim"
        private const val KEY_BG_UPDATED_AT = "custom_bg_updated_at"
        private const val BG_FILE_NAME = "custom_bg.jpg"
        const val DEFAULT_DIM_ALPHA = 0.44f
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
