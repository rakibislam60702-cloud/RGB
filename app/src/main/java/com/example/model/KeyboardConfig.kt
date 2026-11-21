package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class RgbEffect(
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    TOUCH_SPARK(
        displayName = "Touch Spark",
        shortLabel = "Spark",
        description = "Stealth matte keys at rest; each key rises in its own vibrant neon color on touch"
    ),
    RAINBOW_WAVE(
        displayName = "Spectrum Wave",
        shortLabel = "Wave",
        description = "Smooth diagonal neon flow sweeping across every keycap"
    ),
    REACTIVE_RIPPLE(
        displayName = "Reactive Ripple",
        shortLabel = "Ripple",
        description = "Every keypress launches a radial neon shockwave outward"
    ),
    CYBER_BREATHING(
        displayName = "Cyber Breathing",
        shortLabel = "Breath",
        description = "Harmonic dual-tone glow pulsing across the entire deck"
    ),
    MATRIX_RAIN(
        displayName = "Matrix Cascade",
        shortLabel = "Matrix",
        description = "Digital light streams cascading vertically down key columns"
    ),
    STARLIGHT(
        displayName = "Starlight Embers",
        shortLabel = "Stars",
        description = "Sparkling per-key constellations shimmering organically"
    ),
    STATIC_NEON(
        displayName = "Static Prism",
        shortLabel = "Static",
        description = "Crisp locked multi-zone studio lighting with zero motion"
    );

    fun next(): RgbEffect {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

enum class RgbPalette(
    val displayName: String,
    val colors: List<Color>,
    val isFullSpectrum: Boolean = false
) {
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
        displayName = "Samsung Soft",
        actuationLabel = "Flagship · Soft Pop",
        description = "Smooth, rounded flagship keypress pop tuned for effortless daily typing",
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
    val effect: RgbEffect = RgbEffect.TOUCH_SPARK,
    val palette: RgbPalette = RgbPalette.SPECTRUM_PRISM,
    val keycapStyle: KeycapStyle = KeycapStyle.PUDDING,
    val switchSound: SwitchSoundProfile = SwitchSoundProfile.SAMSUNG_SOFT,
    val animationSpeed: Float = 1.0f,
    val glowIntensity: Float = 0.90f,
    val keyCornerRadiusDp: Float = 9f,
    val illuminatedLegends: Boolean = true,
    val chassisAura: Boolean = true,
    val hapticEnabled: Boolean = true,
    val hapticStrength: Float = 0.45f,
    val heightPreset: KeyboardHeightPreset = KeyboardHeightPreset.STANDARD,
    val showNumberRow: Boolean = false,
    val showToolbar: Boolean = true,
    val autoCapitalize: Boolean = true,
    val doubleSpacePeriod: Boolean = true
)

@Immutable
data class ImeStatus(
    val isEnabled: Boolean = false,
    val isSelected: Boolean = false
)
