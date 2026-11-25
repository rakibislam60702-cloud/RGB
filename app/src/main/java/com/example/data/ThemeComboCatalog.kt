package com.example.data

import androidx.compose.runtime.Immutable
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.TouchTypingEffect

enum class ComboCategory(val id: String, val label: String) {
    ALL("all", "✨ All (52)"),
    CYBER_GAMING("cyber", "⚡ Cyber & Gaming"),
    ROYAL_LUXURY("royal", "👑 Royal Luxury"),
    SPACE_SCIFI("space", "🌌 Space & Sci-Fi"),
    ELEMENTAL("elemental", "🔥 Elemental"),
    AESTHETIC("aesthetic", "💖 Aesthetic");

    companion object {
        fun fromId(raw: String?): ComboCategory {
            return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: CYBER_GAMING
        }
    }
}

@Immutable
data class ComboThemePreset(
    val id: String,
    val title: String,
    val badge: String,
    val subtitle: String,
    val category: ComboCategory,
    val bannerPresetId: String,
    val effect: RgbEffect,
    val touchEffect: TouchTypingEffect,
    val palette: RgbPalette,
    val previewColors: List<Long>,
    val remoteImageUrl: String? = null,
    val isCloudFree: Boolean = false
)

/**
 * 52 Unique Categorized On-Demand Downloadable 1-Tap Flagship Combos across 5 curated categories.
 * Downloading saves rendered HD artwork locally to `filesDir/themes/<id>.jpg` on demand.
 */
object ComboThemeCatalog {

    val allCombos: List<ComboThemePreset> = listOf(
        // ==================== ⚡ CYBER & GAMING (12) ====================
        ComboThemePreset(
            id = "combo_zeus_thunder_god",
            title = "Zeus Thunder God",
            badge = "⚡ Lightning Arc",
            subtitle = "Electric storm throne + Electric Cross touch",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_zeus_thunder_god",
            effect = RgbEffect.LIGHTNING_ARC,
            touchEffect = TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF00F5FF, 0xFF3B82F6, 0xFF07112B)
        ),
        ComboThemePreset(
            id = "combo_cyber_samurai_katana",
            title = "Cyber Samurai Katana",
            badge = "⚔️ Katana Slash",
            subtitle = "Crimson-cyan neon blade + Comet Trail",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_cyber_samurai_katana",
            effect = RgbEffect.KATANA_SLASH,
            touchEffect = TouchTypingEffect.TOUCH_COMET_TRAIL,
            palette = RgbPalette.CYBERPUNK,
            previewColors = listOf(0xFFFF2A85, 0xFF00F0FF, 0xFF0D081B)
        ),
        ComboThemePreset(
            id = "combo_bugatti_neon_drift",
            title = "Bugatti Neon Drift",
            badge = "☄️ Comet Trail",
            subtitle = "Hypercar speed streaks + Shockwave Ring",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = BannerPresetCatalog.PRESET_VICE_NEON_DRIVE,
            effect = RgbEffect.COMET_TRAIL,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFF00F5D4, 0xFFFF2A85, 0xFF0A0818)
        ),
        ComboThemePreset(
            id = "combo_cyber_bass_pulse",
            title = "Cyber Bass Equalizer",
            badge = "🎵 Bass EQ",
            subtitle = "Pro gaming soundwave deck + Electric Cross",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = BannerPresetCatalog.PRESET_PRO_DECK,
            effect = RgbEffect.CYBER_BASS_EQ,
            touchEffect = TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            palette = RgbPalette.CYBERPUNK,
            previewColors = listOf(0xFF00E5FF, 0xFFBC13FE, 0xFF0F1326)
        ),
        ComboThemePreset(
            id = "combo_emerald_matrix_hacker",
            title = "Emerald Matrix Pro",
            badge = "💚 Matrix Fall",
            subtitle = "Hacker phosphor rain + Touch Spark",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = BannerPresetCatalog.PRESET_EMERALD_MATRIX,
            effect = RgbEffect.MATRIX_RAIN,
            touchEffect = TouchTypingEffect.TOUCH_SPARK,
            palette = RgbPalette.EMERALD_MATRIX,
            previewColors = listOf(0xFF00FF66, 0xFF008F4C, 0xFF031712)
        ),
        ComboThemePreset(
            id = "combo_laser_spider_grid",
            title = "Laser Spider-Web",
            badge = "🕸️ Laser Web",
            subtitle = "Crimson cyber web + Electric Cross",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = BannerPresetCatalog.PRESET_NEON_CYBER,
            effect = RgbEffect.LASER_SPIDER_WEB,
            touchEffect = TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            palette = RgbPalette.CYBERPUNK,
            previewColors = listOf(0xFFFF2E93, 0xFF00E5FF, 0xFF0A0D1F)
        ),
        ComboThemePreset(
            id = "combo_neon_cyber_snake",
            title = "Neon Cyber Snake",
            badge = "🐍 Cyber Snake",
            subtitle = "Retro arcade light-snake + Comet Trail",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_neon_cyber_snake",
            effect = RgbEffect.NEON_CYBER_SNAKE,
            touchEffect = TouchTypingEffect.TOUCH_COMET_TRAIL,
            palette = RgbPalette.SPECTRUM_PRISM,
            previewColors = listOf(0xFF39FF14, 0xFF00F0FF, 0xFF08141C)
        ),
        ComboThemePreset(
            id = "combo_esports_overdrive",
            title = "Esports Overdrive",
            badge = "⚛️ Plasma Core",
            subtitle = "Arena neon reactor + Shockwave Ring",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_esports_overdrive",
            effect = RgbEffect.PLASMA_REACTOR,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.CYBERPUNK,
            previewColors = listOf(0xFFFFE600, 0xFFFF2A85, 0xFF140B24)
        ),
        ComboThemePreset(
            id = "combo_glitch_protocol",
            title = "Glitch Protocol X",
            badge = "⚡ Lightning Arc",
            subtitle = "Cyber glitch matrix + Diamond Flash",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_glitch_protocol",
            effect = RgbEffect.LIGHTNING_ARC,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFF00F5D4, 0xFFFF006E, 0xFF0D081D)
        ),
        ComboThemePreset(
            id = "combo_mecha_titan_core",
            title = "Mecha Titan Core",
            badge = "🌊 Liquid Mercury",
            subtitle = "Armored chrome alloy + Shockwave Ring",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_mecha_titan_core",
            effect = RgbEffect.LIQUID_MERCURY,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF38BDF8, 0xFFA855F7, 0xFF0B1124)
        ),
        ComboThemePreset(
            id = "combo_neon_drift_racer",
            title = "Neon Drift Racer",
            badge = "☄️ Comet Trail",
            subtitle = "Outrun turbo strip + Comet Trail",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_neon_drift_racer",
            effect = RgbEffect.COMET_TRAIL,
            touchEffect = TouchTypingEffect.TOUCH_COMET_TRAIL,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFF6B00, 0xFFFF2A85, 0xFF120926)
        ),
        ComboThemePreset(
            id = "combo_hyper_fps_hud",
            title = "Hyper FPS HUD",
            badge = "💓 Heartbeat ECG",
            subtitle = "Tactical HUD telemetry + Electric Cross",
            category = ComboCategory.CYBER_GAMING,
            bannerPresetId = "theme_hyper_fps_hud",
            effect = RgbEffect.HEARTBEAT_ECG,
            touchEffect = TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            palette = RgbPalette.EMERALD_MATRIX,
            previewColors = listOf(0xFF00F59B, 0xFF00E5FF, 0xFF06161C)
        ),

        // ==================== 👑 ROYAL LUXURY (10) ====================
        ComboThemePreset(
            id = "combo_liquid_gold_royal",
            title = "Liquid Gold Royal",
            badge = "👑 Diamond Prism",
            subtitle = "24K molten gold filigree + Diamond Flash",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = "theme_liquid_gold_royal",
            effect = RgbEffect.DIAMOND_PRISM,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFFD700, 0xFFFF9E00, 0xFF1A1104)
        ),
        ComboThemePreset(
            id = "combo_diamond_prism_glass",
            title = "Diamond Prism Glass",
            badge = "💎 Diamond Prism",
            subtitle = "Faceted crystal prism + Diamond Flash",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = BannerPresetCatalog.PRESET_ABSTRACT_RGB,
            effect = RgbEffect.DIAMOND_PRISM,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.SPECTRUM_PRISM,
            previewColors = listOf(0xFFE0FBFF, 0xFF00E5FF, 0xFF161330)
        ),
        ComboThemePreset(
            id = "combo_golden_royal_tiger",
            title = "Golden Royal Tiger",
            badge = "🔥 Phoenix Ember",
            subtitle = "Royal gold stripes + Magma Ember",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = BannerPresetCatalog.PRESET_GOLDEN_ROYAL_TIGER,
            effect = RgbEffect.PHOENIX_EMBER,
            touchEffect = TouchTypingEffect.TOUCH_MAGMA_EMBER,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFFB703, 0xFFFF6B00, 0xFF1A1107)
        ),
        ComboThemePreset(
            id = "combo_obsidian_carbon_vip",
            title = "Obsidian Carbon VIP",
            badge = "🌊 Liquid Mercury",
            subtitle = "Forged carbon weave + Diamond Flash",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = BannerPresetCatalog.PRESET_DARK_CARBON,
            effect = RgbEffect.LIQUID_MERCURY,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF94A3B8, 0xFF00E5FF, 0xFF0C0F18)
        ),
        ComboThemePreset(
            id = "combo_rose_gold_imperial",
            title = "Rose Gold Imperial",
            badge = "💎 Diamond Prism",
            subtitle = "Veined rose marble + Diamond Flash",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = BannerPresetCatalog.PRESET_ROSE_GOLD_MARBLE,
            effect = RgbEffect.DIAMOND_PRISM,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFFF6C2C2, 0xFFD49A89, 0xFF3E242C)
        ),
        ComboThemePreset(
            id = "combo_sapphire_crown_velvet",
            title = "Sapphire Crown Velvet",
            badge = "🌌 Aurora Ribbon",
            subtitle = "Royal sapphire silk + Touch Spark",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = BannerPresetCatalog.PRESET_ROYAL_BLUE,
            effect = RgbEffect.AURORA_FLOW,
            touchEffect = TouchTypingEffect.TOUCH_SPARK,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF2979FF, 0xFFFFD700, 0xFF06132E)
        ),
        ComboThemePreset(
            id = "combo_emerald_emperor",
            title = "Emerald Emperor",
            badge = "💎 Diamond Prism",
            subtitle = "Imperial jade facets + Diamond Flash",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = "theme_emerald_emperor",
            effect = RgbEffect.DIAMOND_PRISM,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.EMERALD_MATRIX,
            previewColors = listOf(0xFF10B981, 0xFFFFD700, 0xFF062018)
        ),
        ComboThemePreset(
            id = "combo_platinum_mercury",
            title = "Platinum Mercury",
            badge = "🌊 Liquid Mercury",
            subtitle = "Brushed silver chrome + Shockwave Ring",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = "theme_platinum_mercury",
            effect = RgbEffect.LIQUID_MERCURY,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFFE2E8F0, 0xFF60A5FA, 0xFF101626)
        ),
        ComboThemePreset(
            id = "combo_ruby_velvet_monarch",
            title = "Ruby Velvet Monarch",
            badge = "💓 Heartbeat ECG",
            subtitle = "Crimson gemstone glow + Diamond Flash",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = "theme_ruby_velvet_monarch",
            effect = RgbEffect.HEARTBEAT_ECG,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFE11D48, 0xFFFFD700, 0xFF220612)
        ),
        ComboThemePreset(
            id = "combo_midnight_gold_palace",
            title = "Midnight Gold Palace",
            badge = "🎆 Fireworks",
            subtitle = "Navy gilded arches + Touch Spark",
            category = ComboCategory.ROYAL_LUXURY,
            bannerPresetId = "theme_midnight_gold_palace",
            effect = RgbEffect.CYBERPUNK_FIREWORKS,
            touchEffect = TouchTypingEffect.TOUCH_SPARK,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFFD700, 0xFF38BDF8, 0xFF090F26)
        ),

        // ==================== 🌌 SPACE & SCI-FI (10) ====================
        ComboThemePreset(
            id = "combo_cosmic_black_hole",
            title = "Cosmic Black Hole",
            badge = "🕳️ Black Hole",
            subtitle = "Event horizon vortex + Shockwave Ring",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_cosmic_black_hole",
            effect = RgbEffect.BLACK_HOLE_GRAVITY,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFFFF9E00, 0xFF9D4EDD, 0xFF06040F)
        ),
        ComboThemePreset(
            id = "combo_galaxy_nebula",
            title = "Galaxy Nebula",
            badge = "🌀 Quantum Vortex",
            subtitle = "Deep space nebula + Comet Trail",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = BannerPresetCatalog.PRESET_GALAXY_NEBULA,
            effect = RgbEffect.QUANTUM_VORTEX,
            touchEffect = TouchTypingEffect.TOUCH_COMET_TRAIL,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFF7B2FFF, 0xFF00D4FF, 0xFF090816)
        ),
        ComboThemePreset(
            id = "combo_dna_biolab",
            title = "DNA Bio-Helix",
            badge = "🧬 DNA Helix",
            subtitle = "Glowing genome strands + Electric Cross",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_dna_biolab",
            effect = RgbEffect.DNA_HELIX,
            touchEffect = TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFF00F5FF, 0xFFFF2A85, 0xFF081024)
        ),
        ComboThemePreset(
            id = "combo_quantum_wormhole",
            title = "Quantum Wormhole",
            badge = "🌀 Quantum Vortex",
            subtitle = "Hyperspace tunnel + Shockwave Ring",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_quantum_wormhole",
            effect = RgbEffect.QUANTUM_VORTEX,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.SPECTRUM_PRISM,
            previewColors = listOf(0xFF00E5FF, 0xFFA855F7, 0xFF09071A)
        ),
        ComboThemePreset(
            id = "combo_supernova_burst",
            title = "Supernova Burst",
            badge = "🎆 Fireworks",
            subtitle = "Exploding star clusters + Magma Ember",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_supernova_burst",
            effect = RgbEffect.CYBERPUNK_FIREWORKS,
            touchEffect = TouchTypingEffect.TOUCH_MAGMA_EMBER,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFFB703, 0xFFFF2E93, 0xFF12061F)
        ),
        ComboThemePreset(
            id = "combo_plasma_starship",
            title = "Plasma Starship",
            badge = "⚛️ Plasma Reactor",
            subtitle = "Warp core bridge + Electric Cross",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_plasma_starship",
            effect = RgbEffect.PLASMA_REACTOR,
            touchEffect = TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF22D3EE, 0xFF6366F1, 0xFF070D21)
        ),
        ComboThemePreset(
            id = "combo_andromeda_comet",
            title = "Andromeda Comet",
            badge = "☄️ Comet Trail",
            subtitle = "Meteor shower dusk + Comet Trail",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_andromeda_comet",
            effect = RgbEffect.COMET_TRAIL,
            touchEffect = TouchTypingEffect.TOUCH_COMET_TRAIL,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFF38BDF8, 0xFFC084FC, 0xFF070B1E)
        ),
        ComboThemePreset(
            id = "combo_saturn_rings_neon",
            title = "Saturn Rings Neon",
            badge = "🌌 Aurora Ribbon",
            subtitle = "Planetary halo glow + Diamond Flash",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_saturn_rings_neon",
            effect = RgbEffect.AURORA_FLOW,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFFD166, 0xFF06B6D4, 0xFF0C0F29)
        ),
        ComboThemePreset(
            id = "combo_alien_biolum",
            title = "Alien Bioluminescence",
            badge = "🌋 Lava Orbs",
            subtitle = "Exoplanet neon spores + Touch Spark",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_alien_biolum",
            effect = RgbEffect.NEON_LAVA_ORBS,
            touchEffect = TouchTypingEffect.TOUCH_SPARK,
            palette = RgbPalette.EMERALD_MATRIX,
            previewColors = listOf(0xFF00FFCC, 0xFF7C3AED, 0xFF05141C)
        ),
        ComboThemePreset(
            id = "combo_lunar_eclipse_glow",
            title = "Lunar Eclipse Glow",
            badge = "🕳️ Black Hole",
            subtitle = "Blood moon corona + Frost Freeze",
            category = ComboCategory.SPACE_SCIFI,
            bannerPresetId = "theme_lunar_eclipse_glow",
            effect = RgbEffect.BLACK_HOLE_GRAVITY,
            touchEffect = TouchTypingEffect.TOUCH_FROST_FREEZE,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFF4D6D, 0xFFFFB703, 0xFF100612)
        ),

        // ==================== 🔥 ELEMENTAL (10) ====================
        ComboThemePreset(
            id = "combo_phoenix_inferno",
            title = "Phoenix Inferno",
            badge = "🔥 Phoenix Ember",
            subtitle = "Blazing phoenix wings + Magma Ember",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = BannerPresetCatalog.PRESET_CYBER_DRAGON_FIRE,
            effect = RgbEffect.PHOENIX_EMBER,
            touchEffect = TouchTypingEffect.TOUCH_MAGMA_EMBER,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFF4500, 0xFFFFD700, 0xFF20080C)
        ),
        ComboThemePreset(
            id = "combo_subzero_glacier",
            title = "Sub-Zero Glacier",
            badge = "❄️ Sub-Zero Frost",
            subtitle = "Arctic ice crystals + Frost Freeze",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = "theme_subzero_glacier",
            effect = RgbEffect.SUB_ZERO_FROST,
            touchEffect = TouchTypingEffect.TOUCH_FROST_FREEZE,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFFE0F7FF, 0xFF00E5FF, 0xFF06162E)
        ),
        ComboThemePreset(
            id = "combo_thunder_storm",
            title = "Thunder Storm",
            badge = "⚡ Lightning Arc",
            subtitle = "Electric storm clouds + Electric Cross",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = BannerPresetCatalog.PRESET_THUNDER_STORM,
            effect = RgbEffect.LIGHTNING_ARC,
            touchEffect = TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF00E5FF, 0xFF3B82F6, 0xFF07112B)
        ),
        ComboThemePreset(
            id = "combo_volcano_magma_orbs",
            title = "Volcano Magma Orbs",
            badge = "🌋 Lava Orbs",
            subtitle = "Molten obsidian core + Magma Ember",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = "theme_volcano_magma_orbs",
            effect = RgbEffect.NEON_LAVA_ORBS,
            touchEffect = TouchTypingEffect.TOUCH_MAGMA_EMBER,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFF3B00, 0xFFFF9E00, 0xFF1A0706)
        ),
        ComboThemePreset(
            id = "combo_aurora_northern",
            title = "Aurora Northern",
            badge = "🌌 Aurora Ribbon",
            subtitle = "Polar sky curtains + Frost Freeze",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = BannerPresetCatalog.PRESET_AURORA_LIGHTS,
            effect = RgbEffect.AURORA_FLOW,
            touchEffect = TouchTypingEffect.TOUCH_FROST_FREEZE,
            palette = RgbPalette.SPECTRUM_PRISM,
            previewColors = listOf(0xFF00F5D4, 0xFF7B2FFF, 0xFF0A1734)
        ),
        ComboThemePreset(
            id = "combo_tsunami_abyss",
            title = "Tsunami Deep Abyss",
            badge = "🌊 Liquid Mercury",
            subtitle = "Bioluminescent ocean surge + Shockwave Ring",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = "theme_tsunami_abyss",
            effect = RgbEffect.LIQUID_MERCURY,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF06B6D4, 0xFF1D4ED8, 0xFF041226)
        ),
        ComboThemePreset(
            id = "combo_firefly_forest",
            title = "Firefly Enchanted",
            badge = "🔥 Phoenix Ember",
            subtitle = "Starlit forest glimmers + Touch Spark",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = BannerPresetCatalog.PRESET_FIREFLY_NIGHT,
            effect = RgbEffect.PHOENIX_EMBER,
            touchEffect = TouchTypingEffect.TOUCH_SPARK,
            palette = RgbPalette.EMERALD_MATRIX,
            previewColors = listOf(0xFFFFB703, 0xFF00F59B, 0xFF0C1830)
        ),
        ComboThemePreset(
            id = "combo_solar_corona",
            title = "Solar Flare Corona",
            badge = "⚛️ Plasma Reactor",
            subtitle = "Sunstorm plasma waves + Magma Ember",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = "theme_solar_corona",
            effect = RgbEffect.PLASMA_REACTOR,
            touchEffect = TouchTypingEffect.TOUCH_MAGMA_EMBER,
            palette = RgbPalette.SOLAR_FLARE,
            previewColors = listOf(0xFFFFE600, 0xFFFF5E00, 0xFF210C04)
        ),
        ComboThemePreset(
            id = "combo_arctic_blizzard",
            title = "Arctic Blizzard",
            badge = "❄️ Sub-Zero Frost",
            subtitle = "Frozen twilight peaks + Frost Freeze",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = BannerPresetCatalog.PRESET_MIDNIGHT_MOUNTAINS,
            effect = RgbEffect.SUB_ZERO_FROST,
            touchEffect = TouchTypingEffect.TOUCH_FROST_FREEZE,
            palette = RgbPalette.ARCTIC_FROST,
            previewColors = listOf(0xFF7DD3FC, 0xFF3B82F6, 0xFF0B1938)
        ),
        ComboThemePreset(
            id = "combo_toxic_venom_surge",
            title = "Toxic Venom Surge",
            badge = "🐍 Cyber Snake",
            subtitle = "Radioactive acid glow + Shockwave Ring",
            category = ComboCategory.ELEMENTAL,
            bannerPresetId = "theme_toxic_venom_surge",
            effect = RgbEffect.NEON_CYBER_SNAKE,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.EMERALD_MATRIX,
            previewColors = listOf(0xFF84CC16, 0xFF00FF88, 0xFF071A0E)
        ),

        // ==================== 💖 AESTHETIC (10) ====================
        ComboThemePreset(
            id = "combo_tokyo_midnight_rain",
            title = "Tokyo Midnight Rain",
            badge = "🌸 Sakura Drift",
            subtitle = "Rain-slick neon Shibuya + Comet Trail",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = "theme_tokyo_midnight_rain",
            effect = RgbEffect.SAKURA_DRIFT,
            touchEffect = TouchTypingEffect.TOUCH_COMET_TRAIL,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFF2A85, 0xFF00F0FF, 0xFF0D0B24)
        ),
        ComboThemePreset(
            id = "combo_vice_city_sunset",
            title = "Vice City Sunset",
            badge = "🌌 Aurora Ribbon",
            subtitle = "Miami neon palms + Shockwave Ring",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = BannerPresetCatalog.PRESET_VICE_SUNSET_PALMS,
            effect = RgbEffect.AURORA_FLOW,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFF2A85, 0xFFFF9E00, 0xFF00F5D4)
        ),
        ComboThemePreset(
            id = "combo_love_heart_glow",
            title = "Love Heart Glow",
            badge = "💓 Heartbeat ECG",
            subtitle = "Romantic rose hearts + Diamond Flash",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = BannerPresetCatalog.PRESET_LOVE_HEART_GLOW,
            effect = RgbEffect.HEARTBEAT_ECG,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFFFF2A85, 0xFFFF79B0, 0xFF2A0823)
        ),
        ComboThemePreset(
            id = "combo_sakura_moon_drift",
            title = "Sakura Moon Drift",
            badge = "🌸 Sakura Drift",
            subtitle = "Moonlit cherry blossoms + Touch Spark",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = BannerPresetCatalog.PRESET_SAKURA_NIGHT,
            effect = RgbEffect.SAKURA_DRIFT,
            touchEffect = TouchTypingEffect.TOUCH_SPARK,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFFFF75A0, 0xFF9D4EDD, 0xFF1A1034)
        ),
        ComboThemePreset(
            id = "combo_pastel_dream_cloud",
            title = "Pastel Dream Cloud",
            badge = "🎆 Fireworks",
            subtitle = "Kawaii dreamy clouds + Diamond Flash",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = BannerPresetCatalog.PRESET_CUTE_CARTOON,
            effect = RgbEffect.CYBERPUNK_FIREWORKS,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.SPECTRUM_PRISM,
            previewColors = listOf(0xFFFF85B3, 0xFFA2D2FF, 0xFF5A4FCF)
        ),
        ComboThemePreset(
            id = "combo_sunset_beach_breeze",
            title = "Sunset Beach Breeze",
            badge = "🌋 Lava Orbs",
            subtitle = "Golden twilight waves + Touch Spark",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = BannerPresetCatalog.PRESET_SUNSET_GLOW,
            effect = RgbEffect.NEON_LAVA_ORBS,
            touchEffect = TouchTypingEffect.TOUCH_SPARK,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFF9E00, 0xFFFF4D6D, 0xFF1D1135)
        ),
        ComboThemePreset(
            id = "combo_lofi_lavender_mist",
            title = "Lo-Fi Lavender Mist",
            badge = "🌌 Aurora Ribbon",
            subtitle = "Chill violet vapor glow + Frost Freeze",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = "theme_lofi_lavender_mist",
            effect = RgbEffect.AURORA_FLOW,
            touchEffect = TouchTypingEffect.TOUCH_FROST_FREEZE,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFFC084FC, 0xFF818CF8, 0xFF1A1038)
        ),
        ComboThemePreset(
            id = "combo_cotton_candy_neon",
            title = "Cotton Candy Neon",
            badge = "🌋 Lava Orbs",
            subtitle = "Pink-aqua glossy pastel + Shockwave Ring",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = "theme_cotton_candy_neon",
            effect = RgbEffect.NEON_LAVA_ORBS,
            touchEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
            palette = RgbPalette.VICE_CITY_NEON,
            previewColors = listOf(0xFFFF75B5, 0xFF00F5D4, 0xFF1F1238)
        ),
        ComboThemePreset(
            id = "combo_cyber_butterfly_wings",
            title = "Neon Butterfly Wings",
            badge = "🌸 Sakura Drift",
            subtitle = "Iridescent neon wings + Diamond Flash",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = "theme_cyber_butterfly_wings",
            effect = RgbEffect.SAKURA_DRIFT,
            touchEffect = TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            palette = RgbPalette.SPECTRUM_PRISM,
            previewColors = listOf(0xFFE879F9, 0xFF22D3EE, 0xFF190C32)
        ),
        ComboThemePreset(
            id = "combo_starlight_romance",
            title = "Starlight Romance",
            badge = "☄️ Comet Trail",
            subtitle = "Moonlit stardust night + Comet Trail",
            category = ComboCategory.AESTHETIC,
            bannerPresetId = "theme_starlight_romance",
            effect = RgbEffect.COMET_TRAIL,
            touchEffect = TouchTypingEffect.TOUCH_COMET_TRAIL,
            palette = RgbPalette.VAPORWAVE,
            previewColors = listOf(0xFFFF4D6D, 0xFFA855F7, 0xFF120829)
        )
    )

    fun byCategory(category: ComboCategory, cloudThemes: List<CloudThemeConfig> = emptyList()): List<ComboThemePreset> {
        val mergedCloud = cloudThemes.map { ct ->
            ComboThemePreset(
                id = ct.id,
                title = ct.title,
                badge = "☁️ Cloud Theme",
                subtitle = ct.subtitle.ifBlank { "Remote Firebase theme" },
                category = ComboCategory.fromId(ct.category),
                bannerPresetId = ct.id,
                effect = RgbEffect.fromSavedName(ct.effectId),
                touchEffect = TouchTypingEffect.fromSavedName(ct.touchEffectId),
                palette = RgbPalette.entries.firstOrNull { it.name.equals(ct.paletteName, ignoreCase = true) }
                    ?: RgbPalette.VICE_CITY_NEON,
                previewColors = ct.previewColors,
                remoteImageUrl = ct.imageUrl.takeIf { it.isNotBlank() },
                isCloudFree = ct.isFree
            )
        }
        val combined = mergedCloud + allCombos
        return if (category == ComboCategory.ALL) combined else combined.filter { it.category == category }
    }

    fun findById(id: String?): ComboThemePreset? = allCombos.firstOrNull { it.id == id }
}
