package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 7 Next-Gen Touch-Reactive Typing Effects that layer SIMULTANEOUSLY on top of any
 * active Background Theme or Ambient Physics RGB Animation.
 */
enum class TouchTypingEffect(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val badge: String,
    val description: String,
    val isVipEffect: Boolean = true
) {
    TOUCH_SPARK(
        id = "touch_spark",
        displayName = "✨ Touch Spark",
        shortLabel = "Touch Spark",
        badge = "✨ Spark",
        description = "Stealth matte keys at rest; pressed key rises in vivid neon glow"
    ),
    TOUCH_ELECTRIC_CROSS(
        id = "touch_electric_cross",
        displayName = "⚡ Touch Electric Cross",
        shortLabel = "Electric Cross",
        badge = "⚡ Cross",
        description = "Pressing a key illuminates its horizontal row & vertical column in a neon laser cross"
    ),
    TOUCH_SHOCKWAVE_RING(
        id = "touch_shockwave_ring",
        displayName = "💥 Touch Shockwave Ring",
        shortLabel = "Shockwave Ring",
        badge = "💥 Ring",
        description = "Tapping any key expands a glowing radial neon shockwave ring outward"
    ),
    TOUCH_MAGMA_EMBER(
        id = "touch_magma_ember",
        displayName = "🔥 Touch Magma Ember",
        shortLabel = "Magma Ember",
        badge = "🔥 Ember",
        description = "Tapped keys ignite in molten gold-crimson glow and launch rising micro-sparks"
    ),
    TOUCH_FROST_FREEZE(
        id = "touch_frost_freeze",
        displayName = "❄️ Touch Frost Freeze",
        shortLabel = "Frost Freeze",
        badge = "❄️ Frost",
        description = "Tapping a key freezes it and immediate neighbors in icy cyan-white crystal light"
    ),
    TOUCH_COMET_TRAIL(
        id = "touch_comet_trail",
        displayName = "🌠 Touch Comet Trail",
        shortLabel = "Comet Trail",
        badge = "🌠 Comet",
        description = "Rapid typing draws a luminous neon comet tail connecting the last 4 pressed keys"
    ),
    TOUCH_DIAMOND_FLASH(
        id = "touch_diamond_flash",
        displayName = "💎 Touch Diamond Flash",
        shortLabel = "Diamond Flash",
        badge = "💎 Glint",
        description = "Every keypress triggers a crisp 4-point diamond star glint across beveled key edges"
    );

    companion object {
        fun fromSavedName(name: String?): TouchTypingEffect {
            if (name.isNullOrBlank()) return TOUCH_SPARK
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true)
            } ?: TOUCH_SPARK
        }
    }
}

/**
 * Classic Black (ONLY permanently FREE default) + 20 Next-Gen Ambient Physics RGB Animations.
 * All 20 Ambient Physics Animations are marked VIP (free during the 7-day new-device trial,
 * or unlocked via VIP pass / Firebase `/admin_config/freeThemeIds` / `globalAllFree` / device overrides).
 */
enum class RgbEffect(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val isVipEffect: Boolean = true
) {
    NORMAL_DEFAULT(
        id = "normal_default",
        displayName = "Normal / Classic Black",
        shortLabel = "Classic Black",
        description = "Pure matte-black keys with zero RGB lights — 100% FREE forever & battery friendly",
        isVipEffect = false
    ),
    LIGHTNING_ARC(
        id = "lightning_arc",
        displayName = "⚡ Lightning Arc Chain",
        shortLabel = "Lightning Arc",
        description = "High-voltage electric cyan lightning arcs branching across the key matrix"
    ),
    LIQUID_MERCURY(
        id = "liquid_mercury",
        displayName = "🌊 Liquid Mercury Shockwave",
        shortLabel = "Liquid Mercury",
        description = "Fluid chrome-neon ripples undulating across keys like molten liquid metal"
    ),
    COMET_TRAIL(
        id = "comet_trail",
        displayName = "☄️ Comet Trail",
        shortLabel = "Comet Trail",
        description = "Blazing shooting-star comets streaking diagonally with glowing stardust tails"
    ),
    NEON_LAVA_ORBS(
        id = "neon_lava_orbs",
        displayName = "🌋 Neon Lava Orbs",
        shortLabel = "Lava Orbs",
        description = "Buoyant glowing plasma blobs merging and drifting smoothly like a lava lamp"
    ),
    PHOENIX_EMBER(
        id = "phoenix_ember",
        displayName = "🔥 Phoenix Ember Sparks",
        shortLabel = "Phoenix Ember",
        description = "Rising golden-crimson embers floating upward across dark volcanic keys"
    ),
    DIAMOND_PRISM(
        id = "diamond_prism",
        displayName = "💎 Diamond Prism",
        shortLabel = "Diamond Prism",
        description = "Refracted crystalline rainbow light facets glinting sharply across every key"
    ),
    BLACK_HOLE_GRAVITY(
        id = "black_hole_gravity",
        displayName = "🕳️ Black Hole Gravity",
        shortLabel = "Black Hole",
        description = "Accretion-disk spiral pulling violet-gold energy inward toward a dark singularity"
    ),
    DNA_HELIX(
        id = "dna_helix",
        displayName = "🧬 DNA Helix",
        shortLabel = "DNA Helix",
        description = "Twin intertwined cyan and magenta sine-wave strands pulsing across rows"
    ),
    SUB_ZERO_FROST(
        id = "sub_zero_frost",
        displayName = "❄️ Sub-Zero Frost",
        shortLabel = "Sub-Zero Frost",
        description = "Shimmering arctic glacial waves and crystalline frost glimmers"
    ),
    CYBER_BASS_EQ(
        id = "cyber_bass_eq",
        displayName = "🎵 Cyber Bass Equalizer",
        shortLabel = "Bass EQ",
        description = "Rhythmic multi-band neon equalizer bars bouncing upward from the bottom row"
    ),
    LASER_SPIDER_WEB(
        id = "laser_spider_web",
        displayName = "🕸️ Laser Spider-Web",
        shortLabel = "Laser Web",
        description = "Radial neon laser filaments weaving outward from center in cyber-grid pulses"
    ),
    PLASMA_REACTOR(
        id = "plasma_reactor",
        displayName = "⚛️ Plasma Reactor",
        shortLabel = "Plasma Reactor",
        description = "High-energy fusion core rings expanding and pulsing with electric harmonics"
    ),
    SAKURA_DRIFT(
        id = "sakura_drift",
        displayName = "🌸 Sakura Drift",
        shortLabel = "Sakura Drift",
        description = "Soft neon cherry-blossom petals drifting diagonally in moonlit breeze"
    ),
    NEON_CYBER_SNAKE(
        id = "neon_cyber_snake",
        displayName = "🐍 Neon Cyber Snake",
        shortLabel = "Cyber Snake",
        description = "Luminous arcade light-snake weaving smoothly row by row across the entire deck"
    ),
    CYBERPUNK_FIREWORKS(
        id = "cyberpunk_fireworks",
        displayName = "🎆 Cyberpunk Fireworks",
        shortLabel = "Fireworks",
        description = "Periodic multi-color neon firework bursts blooming in radiant stardust rings"
    ),
    QUANTUM_VORTEX(
        id = "quantum_vortex",
        displayName = "🌀 Quantum Vortex",
        shortLabel = "Quantum Vortex",
        description = "Swirling holographic wormhole spiral warping spectrum energy across every key"
    ),
    HEARTBEAT_ECG(
        id = "heartbeat_ecg",
        displayName = "💓 Heartbeat ECG",
        shortLabel = "Heartbeat ECG",
        description = "Neon rose-cyan ECG pulse wave sweeping left to right with twin heartbeat spikes"
    ),
    MATRIX_RAIN(
        id = "matrix_rain",
        displayName = "💚 Matrix Waterfall",
        shortLabel = "Matrix Fall",
        description = "Digital emerald phosphor streams cascading vertically down key columns"
    ),
    AURORA_FLOW(
        id = "aurora_flow",
        displayName = "🌌 Aurora Ribbon",
        shortLabel = "Aurora Ribbon",
        description = "Northern lights silk ribbons undulating fluidly across the keyboard deck"
    ),
    KATANA_SLASH(
        id = "katana_slash",
        displayName = "⚔️ Katana Slash",
        shortLabel = "Katana Slash",
        description = "Razor-sharp neon blade arcs slicing diagonally with trailing afterglow"
    );

    fun next(): RgbEffect {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        val ambientPhysicsEffects: List<RgbEffect>
            get() = entries.filter { it != NORMAL_DEFAULT }

        /** Clean migration mapper so previously saved effect names never break. */
        fun fromSavedName(name: String?): RgbEffect {
            if (name.isNullOrBlank()) return QUANTUM_VORTEX
            entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true)
            }?.let { return it }
            return when (name.uppercase()) {
                "TOUCH_SPARK" -> NORMAL_DEFAULT
                "RAINBOW_WAVE" -> AURORA_FLOW
                "REACTIVE_RIPPLE" -> LIQUID_MERCURY
                "AI_PLASMA_VORTEX" -> QUANTUM_VORTEX
                "CYBER_BREATHING" -> PLASMA_REACTOR
                "FIREFLY_SPARKS" -> PHOENIX_EMBER
                "STARLIGHT" -> CYBERPUNK_FIREWORKS
                "NEON_HEARTBEAT" -> HEARTBEAT_ECG
                "STATIC_NEON" -> DIAMOND_PRISM
                else -> QUANTUM_VORTEX
            }
        }
    }
}

enum class RgbPalette(
    val displayName: String,
    val colors: List<Color>,
    val isFullSpectrum: Boolean = false
) {
    VICE_CITY_NEON(
        displayName = "Vice City Sunset",
        colors = listOf(
            Color(0xFFFF2A85),
            Color(0xFFFF9E00),
            Color(0xFF00F5D4),
            Color(0xFFB5179E)
        )
    ),
    SPECTRUM_PRISM(
        displayName = "Spectrum Prism",
        colors = listOf(
            Color(0xFFFF2E93),
            Color(0xFF9D4EDD),
            Color(0xFF00E5FF),
            Color(0xFF00F59B),
            Color(0xFFFFD60A),
            Color(0xFFFF5E00)
        ),
        isFullSpectrum = true
    ),
    CYBERPUNK(
        displayName = "Cyberpunk 2077",
        colors = listOf(
            Color(0xFFFF2A85),
            Color(0xFF00F0FF),
            Color(0xFFFFE600),
            Color(0xFFBC13FE)
        )
    ),
    VAPORWAVE(
        displayName = "Vaporwave Sunset",
        colors = listOf(
            Color(0xFF9D4EDD),
            Color(0xFFFF4D6D),
            Color(0xFF00F5D4),
            Color(0xFF560BAD)
        )
    ),
    EMERALD_MATRIX(
        displayName = "Emerald Matrix",
        colors = listOf(
            Color(0xFF00FF66),
            Color(0xFF00D68F),
            Color(0xFF70FFBA),
            Color(0xFF008F4C)
        )
    ),
    SOLAR_FLARE(
        displayName = "Solar Flare",
        colors = listOf(
            Color(0xFFFF5E00),
            Color(0xFFFFB703),
            Color(0xFFE63946),
            Color(0xFFFF8800)
        )
    ),
    ARCTIC_FROST(
        displayName = "Arctic Frost",
        colors = listOf(
            Color(0xFF38BDF8),
            Color(0xFFE0F2FE),
            Color(0xFF3B82F6),
            Color(0xFF22D3EE)
        )
    );

    fun next(): RgbPalette {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

enum class KeycapStyle(
    val displayName: String,
    val description: String
) {
    PUDDING(
        displayName = "Pudding Skirt",
        description = "Opaque matte top with luminous glowing perimeter halo"
    ),
    FLOATING_GLOW(
        displayName = "Floating Glow",
        description = "Obsidian keycaps rimmed with razor-thin RGB underglow"
    ),
    NEON_WIREFRAME(
        displayName = "Neon Wireframe",
        description = "Ultra-dark hollow chassis with crisp glowing outlines"
    ),
    SOLID_MECH(
        displayName = "Solid Sculpted",
        description = "Matte PBT mechanical profile with illuminated top legends"
    )
}

enum class SwitchSoundProfile(
    val displayName: String,
    val actuationLabel: String,
    val description: String,
    val accentHex: Long
) {
    SAMSUNG_SOFT(
        displayName = "Soft Modern",
        actuationLabel = "Subtle · Low Volume",
        description = "Gentle, low-volume modern keypress tap tuned for quiet daily typing",
        accentHex = 0xFF00F59B
    ),
    CLICKY_BLUE(
        displayName = "Clicky Blue",
        actuationLabel = "60g · Crisp Snap",
        description = "Audible tactile jacket click with bright high-frequency snap",
        accentHex = 0xFF00E5FF
    ),
    TACTILE_BROWN(
        displayName = "Tactile Brown",
        actuationLabel = "55g · Warm Bump",
        description = "Balanced tactile feedback with a refined mid-tone thock",
        accentHex = 0xFFFFB703
    ),
    LINEAR_RED(
        displayName = "Linear Red",
        actuationLabel = "45g · Deep Thock",
        description = "Smooth fast travel with a muted, low-pitched PBT bottom-out",
        accentHex = 0xFFFF2E93
    ),
    SILENT(
        displayName = "Silent Dampened",
        actuationLabel = "0 dB · Stealth",
        description = "Zero keystroke audio for quiet spaces and late-night typing",
        accentHex = 0xFF8B95B5
    )
}

enum class KeyboardHeightPreset(
    val displayName: String,
    val rowHeightDp: Int
) {
    COMPACT("Compact", 42),
    STANDARD("Standard", 48),
    TALL("Tall", 54)
}

enum class KeyboardLanguage(
    val localeTag: String,
    val displayName: String,
    val shortBadge: String,
    val spacebarLabel: String
) {
    ENGLISH(
        localeTag = "en_US",
        displayName = "English (QWERTY)",
        shortBadge = "EN",
        spacebarLabel = "ENGLISH · PRO RGB"
    ),
    BENGALI(
        localeTag = "bn_BD",
        displayName = "বাংলা (Bengali)",
        shortBadge = "বাং",
        spacebarLabel = "বাংলা · PRO RGB"
    );

    fun toggle(): KeyboardLanguage = when (this) {
        ENGLISH -> BENGALI
        BENGALI -> ENGLISH
    }

    companion object {
        fun fromLocale(locale: String?): KeyboardLanguage {
            return if (locale != null && locale.lowercase().startsWith("bn")) {
                BENGALI
            } else {
                ENGLISH
            }
        }
    }
}

@Immutable
data class KeyboardSettings(
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val activeLanguageId: String = LanguageCatalog.ENGLISH_ID,
    val enabledLanguageIds: List<String> = listOf(LanguageCatalog.ENGLISH_ID),
    val effect: RgbEffect = RgbEffect.QUANTUM_VORTEX,
    val touchEffect: TouchTypingEffect = TouchTypingEffect.TOUCH_SPARK,
    val palette: RgbPalette = RgbPalette.VICE_CITY_NEON,
    val keycapStyle: KeycapStyle = KeycapStyle.PUDDING,
    val switchSound: SwitchSoundProfile = SwitchSoundProfile.SAMSUNG_SOFT,
    val animationSpeed: Float = 1.0f,
    val glowIntensity: Float = 0.90f,
    val keyCornerRadiusDp: Float = 9f,
    val illuminatedLegends: Boolean = true,
    val chassisAura: Boolean = true,
    val hapticEnabled: Boolean = false,
    val hapticStrength: Float = 0.45f,
    val heightPreset: KeyboardHeightPreset = KeyboardHeightPreset.STANDARD,
    val showNumberRow: Boolean = false,
    val showToolbar: Boolean = true,
    val autoCapitalize: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val smartSuggestionsEnabled: Boolean = true
)

@Immutable
data class ImeStatus(
    val isEnabled: Boolean = false,
    val isSelected: Boolean = false
)
