package com.example.engine

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.model.KeyboardSettings
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.TouchTypingEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
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
     * each ignite in a distinct bright color on touch.
     */
    private val explicitKeyColors: Map<String, Color> = mapOf(
        // QWERTY Row 1
        "q" to Color(0xFF00F0FF),
        "w" to Color(0xFFFF2A85),
        "e" to Color(0xFF39FF14),
        "r" to Color(0xFFFF6B00),
        "t" to Color(0xFFA855F7),
        "y" to Color(0xFFFFE600),
        "u" to Color(0xFFFF3B5C),
        "i" to Color(0xFF38BDF8),
        "o" to Color(0xFF00F59B),
        "p" to Color(0xFFFF4DFF),
        // QWERTY Row 2
        "a" to Color(0xFFFF4D6D),
        "s" to Color(0xFF00FFCC),
        "d" to Color(0xFFFFB703),
        "f" to Color(0xFF7C3AED),
        "g" to Color(0xFF22D3EE),
        "h" to Color(0xFF84CC16),
        "j" to Color(0xFFFF5E00),
        "k" to Color(0xFFE879F9),
        "l" to Color(0xFF00E5FF),
        // QWERTY Row 3
        "z" to Color(0xFFC084FC),
        "x" to Color(0xFF00FF88),
        "c" to Color(0xFFFF2E93),
        "v" to Color(0xFFFACC15),
        "b" to Color(0xFF3B82F6),
        "n" to Color(0xFFFF7A00),
        "m" to Color(0xFF06B6D4),
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
        Color(0xFF00F0FF),
        Color(0xFFFF2A85),
        Color(0xFF39FF14),
        Color(0xFFFF6B00),
        Color(0xFFA855F7),
        Color(0xFFFFE600),
        Color(0xFF00F59B),
        Color(0xFFFF3B5C),
        Color(0xFF38BDF8),
        Color(0xFFFF4DFF),
        Color(0xFFFFB703),
        Color(0xFF00FFCC),
        Color(0xFF818CF8),
        Color(0xFF84CC16),
        Color(0xFFF43F5E),
        Color(0xFF22D3EE)
    )

    fun distinctNeonColorForKey(label: String, indexHint: Int = 0): Color {
        val normalized = label.trim().lowercase()
        explicitKeyColors[normalized]?.let { return it }
        val hash = abs(normalized.hashCode() + indexHint * 31)
        return distinctNeonPool[hash % distinctNeonPool.size]
    }

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
     * Layer 1: Evaluates the instantaneous Ambient Physics RGB Animation color for a keycap at (nx, ny).
     * Returns Color.Transparent when effect == NORMAL_DEFAULT (Classic Black).
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

        val speed = settings.animationSpeed
        val t = timeSec * speed

        val baseColor = when (settings.effect) {
            RgbEffect.NORMAL_DEFAULT -> Color.Transparent

            RgbEffect.LIGHTNING_ARC -> {
                val boltCycle = (t * 2.8f) % 1.4f
                val boltIndex = floor(t * 2.8f / 1.4f).toInt()
                val boltX = ((abs(boltIndex * 43 + 17)) % 100) / 100f
                val boltX2 = ((abs(boltIndex * 71 + 53)) % 100) / 100f
                val jag1 = sin(ny * 18f + t * 12f) * 0.085f
                val jag2 = cos(ny * 15f - t * 10f) * 0.075f
                val d1 = abs(nx - (boltX + jag1))
                val d2 = abs(nx - (boltX2 + jag2))
                val dist = minOf(d1, d2)
                val flash = if (boltCycle < 0.45f && dist < 0.16f) {
                    ((1f - dist / 0.16f) * (1f - boltCycle / 0.45f)).coerceIn(0f, 1f)
                } else 0f
                val stormBase = samplePalette(settings.palette, nx * 0.35f + t * 0.12f).copy(alpha = 0.24f)
                if (flash > 0.04f) {
                    lerp(Color(0xFF00E5FF), Color.White, flash * 0.78f)
                        .copy(alpha = (0.48f + flash * 0.52f).coerceIn(0f, 1f))
                } else {
                    stormBase
                }
            }

            RgbEffect.LIQUID_MERCURY -> {
                val wave1 = sin((nx * 7.5f - t * 2.4f) + sin(ny * 5.2f + t * 1.6f))
                val wave2 = cos((ny * 6.8f + t * 2.1f) - cos(nx * 5.5f - t * 1.4f))
                val chrome = ((wave1 * 0.5f + wave2 * 0.5f) + 1f) * 0.5f
                val specular = (chrome * chrome * chrome).coerceIn(0f, 1f)
                val base = samplePalette(settings.palette, nx * 0.45f + chrome * 0.35f + t * 0.15f)
                lerp(base, Color(0xFFE0F7FA), specular * 0.68f)
                    .copy(alpha = (0.30f + specular * 0.70f).coerceIn(0.28f, 1f))
            }

            RgbEffect.COMET_TRAIL -> {
                val headX = (t * 0.72f) % 1.55f - 0.22f
                val headY = 0.5f + sin(t * 1.8f) * 0.36f
                val dx = headX - nx
                val dy = abs(ny - headY)
                val tail = if (dx in 0f..0.68f && dy < 0.28f) {
                    (1f - dx / 0.68f) * (1f - dy / 0.28f)
                } else 0f
                val headGlow = exp(-((nx - headX) * (nx - headX) + (ny - headY) * (ny - headY)) / 0.014f)
                val energy = max(tail, headGlow).coerceIn(0f, 1f)
                val c = samplePalette(settings.palette, nx * 0.6f - t * 0.35f)
                lerp(c, Color.White, (headGlow * 0.75f).coerceIn(0f, 1f))
                    .copy(alpha = (0.18f + energy * 0.82f).coerceIn(0.18f, 1f))
            }

            RgbEffect.NEON_LAVA_ORBS -> {
                val o1x = 0.5f + 0.36f * sin(t * 0.9f)
                val o1y = 0.5f + 0.34f * cos(t * 1.15f)
                val o2x = 0.5f + 0.38f * cos(t * 0.75f + 1.8f)
                val o2y = 0.5f + 0.32f * sin(t * 1.35f + 0.9f)
                val o3x = 0.5f + 0.30f * sin(t * 1.25f + 3.4f)
                val o3y = 0.5f + 0.35f * cos(t * 0.85f + 2.2f)
                val f1 = 0.032f / (0.015f + (nx - o1x) * (nx - o1x) + (ny - o1y) * (ny - o1y))
                val f2 = 0.032f / (0.015f + (nx - o2x) * (nx - o2x) + (ny - o2y) * (ny - o2y))
                val f3 = 0.028f / (0.015f + (nx - o3x) * (nx - o3x) + (ny - o3y) * (ny - o3y))
                val field = ((f1 + f2 + f3) * 0.36f).coerceIn(0f, 1f)
                val c = samplePalette(settings.palette, field * 0.7f + t * 0.1f)
                c.copy(alpha = (0.20f + field * 0.80f).coerceIn(0.20f, 1f))
            }

            RgbEffect.PHOENIX_EMBER -> {
                val seed = abs(keyHash * 1597334677L % 1000L).toFloat() / 1000f
                val rise = ((1f - ny) + t * (0.45f + seed * 0.45f) + seed) % 1f
                val heat = ((1f - ny) * 0.7f + sin(nx * 9f + t * 3.1f) * 0.16f + 0.25f).coerceIn(0f, 1f)
                val spark = if (rise > 0.78f) ((rise - 0.78f) / 0.22f) else 0f
                val magma = lerp(Color(0xFFD50000), Color(0xFFFFD600), (heat * 0.75f + spark * 0.45f).coerceIn(0f, 1f))
                val blended = lerp(samplePalette(settings.palette, ny * 0.5f + t * 0.18f), magma, 0.72f)
                blended.copy(alpha = (0.26f + max(heat * 0.65f, spark)).coerceIn(0.24f, 1f))
            }

            RgbEffect.DIAMOND_PRISM -> {
                val facet = abs(sin((nx + ny) * 9.5f - t * 2.1f) * cos((nx - ny) * 9.5f + t * 1.7f))
                val glint = (facet * facet * facet).coerceIn(0f, 1f)
                val rainbow = hsvToColor(((nx * 240f + ny * 160f + t * 95f) % 360f), 0.78f, 1f)
                val c = lerp(samplePalette(settings.palette, nx * 0.7f + ny * 0.4f + t * 0.2f), rainbow, 0.55f)
                lerp(c, Color.White, glint * 0.78f)
                    .copy(alpha = (0.28f + glint * 0.72f).coerceIn(0.28f, 1f))
            }

            RgbEffect.BLACK_HOLE_GRAVITY -> {
                val dx = nx - 0.5f
                val dy = (ny - 0.5f) * 1.45f
                val dist = hypot(dx, dy)
                val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat() / (2f * PI.toFloat())
                val accretionRing = exp(-((dist - 0.30f) * (dist - 0.30f)) / 0.018f)
                val horizonDark = if (dist < 0.17f) (dist / 0.17f) * 0.25f else 1f
                val swirlColor = samplePalette(settings.palette, angle * 2f - t * 0.65f + dist * 1.2f)
                lerp(swirlColor, Color(0xFFFFB703), accretionRing * 0.55f)
                    .copy(alpha = ((0.22f + accretionRing * 0.78f) * horizonDark).coerceIn(0.08f, 1f))
            }

            RgbEffect.DNA_HELIX -> {
                val strand1Y = 0.5f + 0.34f * sin(nx * 10.5f - t * 3.1f)
                val strand2Y = 0.5f + 0.34f * sin(nx * 10.5f - t * 3.1f + PI.toFloat())
                val d1 = abs(ny - strand1Y)
                val d2 = abs(ny - strand2Y)
                val s1 = exp(-(d1 * d1) / 0.012f)
                val s2 = exp(-(d2 * d2) / 0.012f)
                val rungGate = ((sin(nx * 31f - t * 3.1f) + 1f) * 0.5f)
                val between = if (ny in minOf(strand1Y, strand2Y)..maxOf(strand1Y, strand2Y) && rungGate > 0.75f) 0.55f else 0f
                val c1 = samplePalette(settings.palette, nx * 0.5f + t * 0.2f)
                val c2 = samplePalette(settings.palette, nx * 0.5f + 0.5f + t * 0.2f)
                val chosen = if (s1 >= s2) c1 else c2
                val energy = max(max(s1, s2), between).coerceIn(0f, 1f)
                chosen.copy(alpha = (0.18f + energy * 0.82f).coerceIn(0.18f, 1f))
            }

            RgbEffect.SUB_ZERO_FROST -> {
                val crystal = abs(sin(nx * 11f + cos(ny * 8f + t * 1.2f)))
                val frostWave = ((sin(nx * 4.5f - t * 1.6f + ny * 3f) + 1f) * 0.5f)
                val ice = lerp(Color(0xFF00B4D8), Color(0xFFE0FBFC), (crystal * 0.55f + frostWave * 0.45f).coerceIn(0f, 1f))
                val c = lerp(samplePalette(settings.palette, nx * 0.4f + t * 0.1f), ice, 0.75f)
                c.copy(alpha = (0.30f + frostWave * 0.68f).coerceIn(0.28f, 1f))
            }

            RgbEffect.CYBER_BASS_EQ -> {
                val col = floor(nx * 10f).toInt().coerceIn(0, 9)
                val freqA = 3.2f + (col % 4) * 1.15f
                val freqB = 2.1f + ((col * 3) % 5) * 0.75f
                val barHeight = (0.28f + 0.68f * ((sin(t * freqA + col * 0.9f) + cos(t * freqB + col * 1.4f)) * 0.25f + 0.5f))
                    .coerceIn(0.15f, 0.98f)
                val heightFromBottom = 1f - ny
                val active = heightFromBottom <= barHeight
                val eqColor = samplePalette(settings.palette, heightFromBottom * 0.75f + col * 0.08f + t * 0.12f)
                if (active) {
                    val peakBoost = if (abs(heightFromBottom - barHeight) < 0.14f) 0.45f else 0f
                    lerp(eqColor, Color.White, peakBoost).copy(alpha = 0.95f)
                } else {
                    eqColor.copy(alpha = 0.16f)
                }
            }

            RgbEffect.LASER_SPIDER_WEB -> {
                val dx = nx - 0.5f
                val dy = (ny - 0.5f) * 1.35f
                val dist = hypot(dx, dy)
                val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                val spokes = abs(sin(angle * 4f + t * 0.55f))
                val rings = abs(sin(dist * 24f - t * 3.8f))
                val webLine = max(if (spokes < 0.16f) 1f - spokes / 0.16f else 0f, if (rings < 0.22f) 1f - rings / 0.22f else 0f)
                val c = samplePalette(settings.palette, dist * 1.1f - t * 0.28f)
                lerp(c, Color.White, webLine * 0.35f)
                    .copy(alpha = (0.20f + webLine * 0.80f).coerceIn(0.20f, 1f))
            }

            RgbEffect.PLASMA_REACTOR -> {
                val dx = nx - 0.5f
                val dy = (ny - 0.5f) * 1.45f
                val dist = hypot(dx, dy)
                val ring = ((sin(dist * 15f - t * 4.2f) + 1f) * 0.5f)
                val core = exp(-(dist * dist) / 0.055f)
                val energy = max(ring * 0.78f, core).coerceIn(0f, 1f)
                val c = samplePalette(settings.palette, dist * 0.9f - t * 0.35f)
                lerp(c, Color.White, core * 0.65f)
                    .copy(alpha = (0.26f + energy * 0.74f).coerceIn(0.26f, 1f))
            }

            RgbEffect.SAKURA_DRIFT -> {
                val drift = ((sin(nx * 7f - t * 1.3f + ny * 5f) + cos(ny * 8f - t * 1.9f)) * 0.25f + 0.5f)
                val petalPulse = (drift * drift * drift).coerceIn(0f, 1f)
                val blossom = lerp(Color(0xFFFF75A0), Color(0xFFFFD1E3), petalPulse)
                val c = lerp(samplePalette(settings.palette, nx * 0.4f + t * 0.08f), blossom, 0.72f)
                c.copy(alpha = (0.26f + petalPulse * 0.74f).coerceIn(0.24f, 1f))
            }

            RgbEffect.NEON_CYBER_SNAKE -> {
                val totalCells = 40
                val row = floor(ny * 4f).toInt().coerceIn(0, 3)
                val col = floor(nx * 10f).toInt().coerceIn(0, 9)
                val cellIdx = if (row % 2 == 0) row * 10 + col else row * 10 + (9 - col)
                val headPos = ((t * 14f) % totalCells)
                val delta = ((headPos - cellIdx) % totalCells + totalCells) % totalCells
                val body = if (delta < 9f) (1f - delta / 9f) else 0f
                val c = samplePalette(settings.palette, cellIdx / 40f + t * 0.2f)
                lerp(c, Color.White, if (delta < 1.3f) 0.65f else 0f)
                    .copy(alpha = (0.16f + body * 0.84f).coerceIn(0.16f, 1f))
            }

            RgbEffect.CYBERPUNK_FIREWORKS -> {
                val cycle = (t * 0.95f) % 1.25f
                val burstIndex = floor(t * 0.95f / 1.25f).toInt()
                val bx = 0.18f + ((abs(burstIndex * 37 + 11)) % 64) / 100f
                val by = 0.22f + ((abs(burstIndex * 29 + 7)) % 56) / 100f
                val dist = hypot((nx - bx) * 1.2f, (ny - by) * 0.85f)
                val radius = cycle * 0.92f
                val ring = exp(-((dist - radius) * (dist - radius)) / 0.011f)
                val fade = (1f - cycle / 1.25f).coerceIn(0f, 1f)
                val energy = (ring * fade).coerceIn(0f, 1f)
                val burstColor = samplePalette(settings.palette, dist * 1.6f + burstIndex * 0.31f)
                burstColor.copy(alpha = (0.18f + energy * 0.82f).coerceIn(0.18f, 1f))
            }

            RgbEffect.QUANTUM_VORTEX -> {
                val dx = nx - 0.5f
                val dy = (ny - 0.5f) * 1.4f
                val dist = hypot(dx, dy)
                val swirl = atan2(dy.toDouble(), dx.toDouble()).toFloat() / (2f * PI.toFloat())
                val wave = ((sin((dist * 11f - t * 3.6f).toDouble()) + 1.0) * 0.5).toFloat()
                val c = samplePalette(settings.palette, swirl + dist * 0.85f - t * 0.42f)
                c.copy(alpha = (0.36f + 0.64f * wave).coerceIn(0.30f, 1f))
            }

            RgbEffect.HEARTBEAT_ECG -> {
                val sweepX = (t * 0.85f) % 1.35f - 0.15f
                val dx = abs(nx - sweepX)
                val ecgY = when {
                    dx < 0.04f -> 0.15f
                    dx < 0.08f -> 0.85f
                    dx < 0.12f -> 0.32f
                    else -> 0.52f
                }
                val distY = abs(ny - ecgY)
                val trace = if (nx <= sweepX + 0.04f && nx >= sweepX - 0.42f) {
                    exp(-(distY * distY) / 0.022f) * (1f - ((sweepX - nx).coerceAtLeast(0f) / 0.45f))
                } else 0f
                val pulse = trace.coerceIn(0f, 1f)
                val heart = lerp(Color(0xFFFF2A85), Color(0xFF00F5D4), pulse * 0.4f)
                val c = lerp(samplePalette(settings.palette, nx * 0.5f + t * 0.15f), heart, 0.62f)
                c.copy(alpha = (0.20f + pulse * 0.80f).coerceIn(0.20f, 1f))
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

            RgbEffect.AURORA_FLOW -> {
                val ribbon = ((sin((nx * 5.2f + t * 1.4f + sin(ny * 4.5f - t * 0.9f)).toDouble()) + 1.0) * 0.5).toFloat()
                val c = samplePalette(settings.palette, nx * 0.55f + ribbon * 0.4f - t * 0.16f)
                c.copy(alpha = (0.34f + 0.66f * ribbon).coerceIn(0.3f, 1f))
            }

            RgbEffect.KATANA_SLASH -> {
                val slashPos = ((t * 1.45f) % 1.85f) - 0.35f
                val diag = nx * 0.72f + ny * 0.52f
                val d = abs(diag - slashPos)
                val blade = exp(-(d * d) / 0.0085f)
                val c = samplePalette(settings.palette, diag + t * 0.22f)
                lerp(c, Color.White, blade * 0.82f)
                    .copy(alpha = (0.20f + blade * 0.80f).coerceIn(0.20f, 1f))
            }
        }

        val boostedAlpha = max(baseColor.alpha * settings.glowIntensity, 0.14f)
        return if (isPressed) {
            lerp(baseColor.copy(alpha = 1f), Color.White, 0.32f)
        } else {
            baseColor.copy(alpha = boostedAlpha.coerceIn(0f, 1f))
        }
    }

    /**
     * Layer 2 Preview Helper: Computes an animated simulated keypress effect color for any
     * of the 7 TouchTypingEffects inside EffectsScreen mini-grid cards and LiveMiniDeckPreview.
     */
    fun computeTouchPreviewColor(
        touchEffect: TouchTypingEffect,
        nx: Float,
        ny: Float,
        row: Int,
        col: Int,
        rows: Int,
        cols: Int,
        timeSec: Float,
        palette: RgbPalette,
        glowIntensity: Float
    ): Color {
        val step = floor(timeSec * 2.1f).toInt()
        val frac = (timeSec * 2.1f) - step
        // Simulated typing path across the keyboard grid
        val path = listOf(
            1 to 2, 0 to 4, 2 to 6, 1 to 8, 0 to 1, 2 to 3, 1 to 5, 0 to 7
        )
        val cur = path[abs(step) % path.size]
        val prev1 = path[abs(step - 1) % path.size]
        val prev2 = path[abs(step - 2) % path.size]
        val prev3 = path[abs(step - 3) % path.size]

        val curRow = cur.first.coerceIn(0, rows - 1)
        val curCol = cur.second.coerceIn(0, cols - 1)
        val targetNx = (curCol + 0.5f) / cols.coerceAtLeast(1)
        val targetNy = (curRow + 0.5f) / rows.coerceAtLeast(1)
        val fade = (1f - frac).coerceIn(0f, 1f)

        return when (touchEffect) {
            TouchTypingEffect.TOUCH_SPARK -> {
                if (row == curRow && col == curCol) {
                    val distinct = distinctNeonColorForKey("k$row$col", row * 17 + col * 31)
                    lerp(distinct, Color.White, (fade * 0.3f)).copy(alpha = (0.35f + fade * 0.65f) * glowIntensity)
                } else Color.Transparent
            }

            TouchTypingEffect.TOUCH_ELECTRIC_CROSS -> {
                val isCenter = row == curRow && col == curCol
                val isCross = row == curRow || col == curCol
                if (isCenter) {
                    Color(0xFFFFFFFF).copy(alpha = (0.6f + fade * 0.4f) * glowIntensity)
                } else if (isCross) {
                    val dist = abs(row - curRow) + abs(col - curCol)
                    val falloff = (1f - dist * 0.14f).coerceIn(0.25f, 1f)
                    Color(0xFF00E5FF).copy(alpha = fade * falloff * glowIntensity)
                } else Color.Transparent
            }

            TouchTypingEffect.TOUCH_SHOCKWAVE_RING -> {
                val dist = hypot((nx - targetNx) * 1.15f, (ny - targetNy) * 0.85f)
                val radius = frac * 0.72f
                val ring = exp(-((dist - radius) * (dist - radius)) / 0.016f)
                val energy = (ring * (1f - frac * 0.75f)).coerceIn(0f, 1f)
                if (energy > 0.06f) {
                    samplePalette(palette, dist * 1.4f + timeSec * 0.4f)
                        .copy(alpha = (energy * glowIntensity).coerceIn(0f, 1f))
                } else Color.Transparent
            }

            TouchTypingEffect.TOUCH_MAGMA_EMBER -> {
                if (row == curRow && col == curCol) {
                    lerp(Color(0xFFFF3D00), Color(0xFFFFD600), fade).copy(alpha = (0.4f + fade * 0.6f) * glowIntensity)
                } else if (col == curCol && row == (curRow - 1).coerceAtLeast(0)) {
                    Color(0xFFFF9E00).copy(alpha = (fade * 0.72f * glowIntensity).coerceIn(0f, 1f))
                } else Color.Transparent
            }

            TouchTypingEffect.TOUCH_FROST_FREEZE -> {
                val dr = abs(row - curRow)
                val dc = abs(col - curCol)
                when {
                    dr == 0 && dc == 0 -> Color(0xFFE0FBFC).copy(alpha = (0.5f + fade * 0.5f) * glowIntensity)
                    dr <= 1 && dc <= 1 -> Color(0xFF38BDF8).copy(alpha = (fade * 0.78f * glowIntensity).coerceIn(0f, 1f))
                    else -> Color.Transparent
                }
            }

            TouchTypingEffect.TOUCH_COMET_TRAIL -> {
                val trail = listOf(cur to 1.0f, prev1 to 0.72f, prev2 to 0.48f, prev3 to 0.28f)
                val match = trail.firstOrNull { (pos, _) ->
                    pos.first.coerceIn(0, rows - 1) == row && pos.second.coerceIn(0, cols - 1) == col
                }
                if (match != null) {
                    val weight = match.second
                    lerp(Color(0xFFFF2A85), Color(0xFF00F5D4), weight)
                        .copy(alpha = (weight * glowIntensity).coerceIn(0f, 1f))
                } else Color.Transparent
            }

            TouchTypingEffect.TOUCH_DIAMOND_FLASH -> {
                if (row == curRow && col == curCol) {
                    lerp(Color(0xFF00E5FF), Color.White, 0.78f)
                        .copy(alpha = (0.45f + fade * 0.55f) * glowIntensity)
                } else if ((abs(row - curRow) == 1 && col == curCol) || (row == curRow && abs(col - curCol) == 1)) {
                    Color(0xFFA855F7).copy(alpha = (fade * 0.45f * glowIntensity).coerceIn(0f, 1f))
                } else Color.Transparent
            }
        }
    }

    /**
     * Returns an ordered list of vivid colors for horizontal chassis aura bars.
     */
    fun computeBarColors(
        timeSec: Float,
        settings: KeyboardSettings,
        stops: Int = 7
    ): List<Color> {
        if (settings.effect == RgbEffect.NORMAL_DEFAULT) {
            return List(stops) { Color(0xFF1F1833) }
        }
        val t = timeSec * settings.animationSpeed
        val shift = -t * 0.35f
        return List(stops) { i ->
            val u = i.toFloat() / (stops - 1).coerceAtLeast(1)
            samplePalette(settings.palette, u + shift)
                .copy(alpha = settings.glowIntensity.coerceIn(0.25f, 1f))
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
