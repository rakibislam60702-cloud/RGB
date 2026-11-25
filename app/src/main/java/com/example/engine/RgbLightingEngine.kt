package com.example.engine

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.model.KeyboardSettings
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

@Immutable
data class RipplePulse(
    val x: Float,
    val y: Float,
    val createdAtSec: Float,
    val id: Long
)

object RgbLightingEngine {

    /**
     * Curated vivid per-key neon spectrum so adjacent QWERTY, numeric, and Bengali keys
     * each ignite in a distinct bright color on touch (Samsung Keys Cafe Touch Spark).
     */
    private val explicitKeyColors: Map<String, Color> = mapOf(
        // QWERTY Row 1
        "q" to Color(0xFF00F0FF), // Cyan
        "w" to Color(0xFFFF2A85), // Magenta
        "e" to Color(0xFF39FF14), // Electric Lime
        "r" to Color(0xFFFF6B00), // Neon Orange
        "t" to Color(0xFFA855F7), // Electric Violet
        "y" to Color(0xFFFFE600), // Laser Yellow
        "u" to Color(0xFFFF3B5C), // Hot Coral
        "i" to Color(0xFF38BDF8), // Glacier Blue
        "o" to Color(0xFF00F59B), // Mint Emerald
        "p" to Color(0xFFFF4DFF), // Neon Pink
        // QWERTY Row 2
        "a" to Color(0xFFFF4D6D), // Crimson Pink
        "s" to Color(0xFF00FFCC), // Aqua Neon
        "d" to Color(0xFFFFB703), // Amber Gold
        "f" to Color(0xFF7C3AED), // Deep Violet
        "g" to Color(0xFF22D3EE), // Sky Cyan
        "h" to Color(0xFF84CC16), // Acid Green
        "j" to Color(0xFFFF5E00), // Solar Flare
        "k" to Color(0xFFE879F9), // Orchid Neon
        "l" to Color(0xFF00E5FF), // Electric Cyan
        // QWERTY Row 3
        "z" to Color(0xFFC084FC), // Lavender Glow
        "x" to Color(0xFF00FF88), // Phosphor Green
        "c" to Color(0xFFFF2E93), // Hot Magenta
        "v" to Color(0xFFFACC15), // Canary Neon
        "b" to Color(0xFF3B82F6), // Cobalt Glow
        "n" to Color(0xFFFF7A00), // Tangerine
        "m" to Color(0xFF06B6D4), // Teal Surge
        // Numbers
        "1" to Color(0xFFFF2E93),
        "2" to Color(0xFFFF6B00),
        "3" to Color(0xFFFFE600),
        "4" to Color(0xFF39FF14),
        "5" to Color(0xFF00F59B),
        "6" to Color(0xFF00F0FF),
        "7" to Color(0xFF38BDF8),
        "8" to Color(0xFF818CF8),
        "9" to Color(0xFFA855F7),
        "0" to Color(0xFFFF4DFF),
        // Modifiers
        "shift" to Color(0xFF00E5FF),
        "del" to Color(0xFFFF3B5C),
        "enter" to Color(0xFF00F59B),
        "space" to Color(0xFF00F0FF),
        "?123" to Color(0xFFA855F7),
        "abc" to Color(0xFF00E5FF)
    )

    private val distinctNeonPool: List<Color> = listOf(
        Color(0xFF00F0FF), // Cyan
        Color(0xFFFF2A85), // Magenta
        Color(0xFF39FF14), // Electric Lime
        Color(0xFFFF6B00), // Vivid Orange
        Color(0xFFA855F7), // Neon Violet
        Color(0xFFFFE600), // Laser Yellow
        Color(0xFF00F59B), // Mint Emerald
        Color(0xFFFF3B5C), // Hot Coral
        Color(0xFF38BDF8), // Glacier Blue
        Color(0xFFFF4DFF), // Neon Pink
        Color(0xFFFFB703), // Amber Gold
        Color(0xFF00FFCC), // Aqua
        Color(0xFF818CF8), // Indigo Glow
        Color(0xFF84CC16), // Acid Green
        Color(0xFFF43F5E), // Rose Neon
        Color(0xFF22D3EE)  // Electric Sky
    )

    /**
     * Returns a deterministic distinct vibrant neon color for any key label / identifier.
     */
    fun distinctNeonColorForKey(label: String, indexHint: Int = 0): Color {
        val normalized = label.trim().lowercase()
        explicitKeyColors[normalized]?.let { return it }
        val hash = abs(normalized.hashCode() + indexHint * 31)
        return distinctNeonPool[hash % distinctNeonPool.size]
    }

    /**
     * Samples a smooth looping color along the selected palette at t in (-inf, +inf).
     */
    fun samplePalette(palette: RgbPalette, t: Float): Color {
        val wrapped = ((t % 1f) + 1f) % 1f
        if (palette.isFullSpectrum) {
            return hsvToColor(wrapped * 360f, 0.94f, 1.0f)
        }
        val colors = palette.colors
        val count = colors.size
        if (count == 0) return Color.Cyan
        if (count == 1) return colors[0]
        val scaled = wrapped * count
        val idx0 = floor(scaled).toInt() % count
        val idx1 = (idx0 + 1) % count
        val frac = scaled - floor(scaled)
        return lerp(colors[idx0], colors[idx1], frac)
    }

    /**
     * Evaluates the instantaneous RGB glow color for a keycap at normalized grid position (nx, ny).
     * In TOUCH_SPARK mode, returns transparent (stealth dark) unless touched/fading.
     */
    fun computeKeyColor(
        nx: Float,
        ny: Float,
        keyHash: Int,
        timeSec: Float,
        settings: KeyboardSettings,
        ripples: List<RipplePulse>,
        isPressed: Boolean,
        keyLabel: String = "",
        touchGlowStrength: Float = 0f
    ): Color {
        if (settings.effect == RgbEffect.NORMAL_DEFAULT) {
            return Color.Transparent
        }
        if (settings.effect == RgbEffect.TOUCH_SPARK) {
            val distinct = distinctNeonColorForKey(keyLabel, keyHash)
            val intensity = if (isPressed) 1f else touchGlowStrength.coerceIn(0f, 1f)
            return if (intensity <= 0.01f) {
                Color.Transparent
            } else {
                val brightened = if (isPressed) lerp(distinct, Color.White, 0.22f) else distinct
                brightened.copy(alpha = (intensity * settings.glowIntensity).coerceIn(0f, 1f))
            }
        }

        val speed = settings.animationSpeed
        val t = timeSec * speed

        val baseColor = when (settings.effect) {
            RgbEffect.NORMAL_DEFAULT,
            RgbEffect.TOUCH_SPARK -> Color.Transparent

            RgbEffect.RAINBOW_WAVE -> {
                val phase = nx * 0.85f + ny * 0.35f - t * 0.38f
                samplePalette(settings.palette, phase)
            }

            RgbEffect.REACTIVE_RIPPLE -> {
                val ambient = samplePalette(settings.palette, nx * 0.4f + t * 0.08f)
                    .copy(alpha = 0.28f)
                var energy = 0f
                var rippleColor = samplePalette(settings.palette, t * 0.3f)
                for (i in ripples.indices) {
                    val pulse = ripples[i]
                    val age = timeSec - pulse.createdAtSec
                    if (age in 0f..1.4f) {
                        val dist = hypot((nx - pulse.x) * 1.15f, (ny - pulse.y) * 0.72f)
                        val radius = age * (0.95f + speed * 0.45f)
                        val ringWidth = 0.17f
                        val ringDist = abs(dist - radius)
                        val ring = exp(-(ringDist * ringDist) / (ringWidth * ringWidth * 0.45f))
                        val fade = (1f - age / 1.4f).coerceIn(0f, 1f)
                        val contribution = ring * fade
                        if (contribution > energy) {
                            energy = contribution
                            rippleColor = samplePalette(
                                settings.palette,
                                dist * 1.4f + pulse.id * 0.17f
                            )
                        }
                    }
                }
                if (energy > 0.02f) {
                    lerp(ambient, rippleColor, energy.coerceIn(0f, 1f))
                } else {
                    ambient
                }
            }

            RgbEffect.AI_PLASMA_VORTEX -> {
                val dx = nx - 0.5f
                val dy = (ny - 0.5f) * 1.4f
                val dist = hypot(dx, dy)
                val swirl = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat() / (2f * PI.toFloat())
                val wave = ((sin((dist * 10f - t * 3.4f).toDouble()) + 1.0) * 0.5).toFloat()
                val c = samplePalette(settings.palette, swirl + dist * 0.85f - t * 0.42f)
                c.copy(alpha = (0.38f + 0.62f * wave).coerceIn(0.3f, 1f))
            }

            RgbEffect.CYBER_BREATHING -> {
                val wave = ((sin(t * 2.1f) + 1f) * 0.5f).coerceIn(0f, 1f)
                val eased = 0.28f + 0.72f * (wave * wave * (3f - 2f * wave))
                val c = samplePalette(settings.palette, t * 0.14f + nx * 0.28f)
                c.copy(alpha = eased)
            }

            RgbEffect.MATRIX_RAIN -> {
                val colIndex = floor(nx * 11f).toInt()
                val colOffset = ((colIndex * 37) % 13) / 13f
                val colSpeed = 0.65f + ((colIndex * 19) % 7) * 0.09f
                val headY = ((t * colSpeed + colOffset) % 1.45f) - 0.2f
                val delta = headY - ny
                val intensity = if (delta in 0f..0.75f) {
                    (1f - delta / 0.75f).let { it * it }
                } else {
                    0.12f
                }
                val color = if (delta in 0f..0.11f) {
                    lerp(samplePalette(settings.palette, ny * 0.5f + colOffset), Color.White, 0.45f)
                } else {
                    samplePalette(settings.palette, ny * 0.5f + colOffset)
                }
                color.copy(alpha = intensity.coerceIn(0.14f, 1f))
            }

            RgbEffect.FIREFLY_SPARKS -> {
                val seed = abs(keyHash * 1597334677L % 1000L).toFloat() / 1000f
                val drift = ((sin(t * (1.2f + seed * 1.9f) + seed * 6.28f) + 1f) * 0.5f)
                val ember = drift * drift * drift * drift
                val warmGold = lerp(Color(0xFFFF9E00), Color(0xFF00F5D4), (seed * 0.65f + nx * 0.35f) % 1f)
                val tinted = lerp(samplePalette(settings.palette, seed + t * 0.08f), warmGold, 0.45f)
                tinted.copy(alpha = (0.14f + ember * 0.86f).coerceIn(0.14f, 1f))
            }

            RgbEffect.AURORA_FLOW -> {
                val ribbon = ((sin((nx * 5.2f + t * 1.4f + sin(ny * 4.5f - t * 0.9f)).toDouble()) + 1.0) * 0.5).toFloat()
                val c = samplePalette(settings.palette, nx * 0.55f + ribbon * 0.4f - t * 0.16f)
                c.copy(alpha = (0.34f + 0.66f * ribbon).coerceIn(0.3f, 1f))
            }

            RgbEffect.STARLIGHT -> {
                val seed = abs(keyHash * 2654435761L % 1000L).toFloat() / 1000f
                val freq = 1.6f + seed * 2.4f
                val phase = seed * 2f * PI.toFloat()
                val pulse = ((sin(t * freq + phase) + 1f) * 0.5f)
                val sparkle = pulse * pulse * pulse
                val color = samplePalette(settings.palette, seed + t * 0.12f)
                color.copy(alpha = (0.18f + sparkle * 0.82f).coerceIn(0.18f, 1f))
            }

            RgbEffect.LIGHTNING_ARC -> {
                val boltPhase = (t * 2.6f) % 1.8f
                val boltX = ((floor(t * 2.6f / 1.8f).toInt() * 37) % 100) / 100f
                val jag = (sin(ny * 14f + t * 9f) * 0.08f)
                val distX = abs(nx - (boltX + jag))
                val flash = if (boltPhase < 0.38f && distX < 0.18f) {
                    ((1f - distX / 0.18f) * (1f - boltPhase / 0.38f)).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val stormBase = samplePalette(settings.palette, nx * 0.35f + t * 0.1f).copy(alpha = 0.24f)
                if (flash > 0.05f) {
                    lerp(Color(0xFF00E5FF), Color.White, flash * 0.7f).copy(alpha = (0.45f + flash * 0.55f).coerceIn(0f, 1f))
                } else {
                    stormBase
                }
            }

            RgbEffect.NEON_HEARTBEAT -> {
                val beatCycle = (t * 1.35f) % 1f
                val lub = exp(-((beatCycle - 0.18f) * (beatCycle - 0.18f)) / 0.0045f)
                val dub = exp(-((beatCycle - 0.36f) * (beatCycle - 0.36f)) / 0.0065f) * 0.75f
                val distCenter = hypot((nx - 0.5f) * 1.1f, (ny - 0.5f) * 1.2f)
                val pulse = ((lub + dub) * (1.15f - distCenter).coerceIn(0.25f, 1f)).coerceIn(0f, 1f)
                val heartColor = lerp(Color(0xFFFF2A85), Color(0xFFFF79B0), pulse)
                val blended = lerp(samplePalette(settings.palette, distCenter + t * 0.12f), heartColor, 0.65f)
                blended.copy(alpha = (0.28f + 0.72f * pulse).coerceIn(0.26f, 1f))
            }

            RgbEffect.STATIC_NEON -> {
                samplePalette(settings.palette, nx * 0.85f + ny * 0.2f)
            }
        }

        val pressedColor = if (isPressed) {
            lerp(baseColor.copy(alpha = 1f), Color.White, 0.35f)
        } else {
            val boostedAlpha = max(baseColor.alpha * settings.glowIntensity, 0.14f)
            baseColor.copy(alpha = boostedAlpha.coerceIn(0f, 1f))
        }
        return pressedColor
    }

    /**
     * Returns an ordered list of vivid colors for horizontal chassis aura bars.
     */
    fun computeBarColors(
        timeSec: Float,
        settings: KeyboardSettings,
        stops: Int = 7
    ): List<Color> {
        if (settings.effect == RgbEffect.NORMAL_DEFAULT || settings.effect == RgbEffect.TOUCH_SPARK) {
            return List(stops) { Color(0xFF1F1833) }
        }
        val t = timeSec * settings.animationSpeed
        val pulse = if (settings.effect == RgbEffect.CYBER_BREATHING) {
            0.35f + 0.65f * ((sin(t * 2.1f) + 1f) * 0.5f)
        } else {
            1f
        }
        val shift = when (settings.effect) {
            RgbEffect.STATIC_NEON -> 0f
            else -> -t * 0.35f
        }
        return List(stops) { i ->
            val u = i.toFloat() / (stops - 1).coerceAtLeast(1)
            samplePalette(settings.palette, u + shift)
                .copy(alpha = (settings.glowIntensity * pulse).coerceIn(0.2f, 1f))
        }
    }

    fun hsvToColor(hue: Float, sat: Float, value: Float): Color {
        val h = ((hue % 360f) + 360f) % 360f
        val c = value * sat
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = value - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
}
