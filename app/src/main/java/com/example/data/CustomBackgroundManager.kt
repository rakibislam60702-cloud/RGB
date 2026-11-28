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
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.R
import com.example.model.TouchEffectCategory
import com.example.model.TouchEffectVisualStyle
import com.example.model.TouchTypingEffect
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
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

    /** Section 3: 104 Curated Static HD Photo Banners (10 flagship + 94 procedural studio scenes). */
    private val baseStaticBannerPresets: List<BannerThemePreset> = listOf(
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

    private val expandedHdPhotoPresets: List<BannerThemePreset> = listOf(
        BannerThemePreset("hd_crimson_eclipse", "Crimson Eclipse", "Deep ruby halo dusk", null, listOf(0xFFFF2A55, 0xFF7C1034, 0xFF14060F)),
        BannerThemePreset("hd_glacier_mint", "Glacier Mint", "Arctic frosted teal", null, listOf(0xFF00F5D4, 0xFF0E7490, 0xFF061923)),
        BannerThemePreset("hd_obsidian_gold", "Obsidian Gold", "24K luxury dark vein", null, listOf(0xFFFFD166, 0xFF8C6214, 0xFF0D0B09)),
        BannerThemePreset("hd_lavender_cloud", "Lavender Cloud", "Soft pastel horizon", null, listOf(0xFFC084FC, 0xFF6366F1, 0xFF1B1438)),
        BannerThemePreset("hd_tokyo_rain", "Tokyo Rain", "Neon reflections midnight", null, listOf(0xFF00E5FF, 0xFFFF2A85, 0xFF0A1024)),
        BannerThemePreset("hd_sahara_dusk", "Sahara Dusk", "Warm amber desert sky", null, listOf(0xFFFF9E00, 0xFFB45309, 0xFF1F1006)),
        BannerThemePreset("hd_emerald_velvet", "Emerald Velvet", "Royal deep forest silk", null, listOf(0xFF10B981, 0xFF065F46, 0xFF041712)),
        BannerThemePreset("hd_cobalt_wave", "Cobalt Wave", "Oceanic twilight swell", null, listOf(0xFF3B82F6, 0xFF1D4ED8, 0xFF07112B)),
        BannerThemePreset("hd_peach_sorbet", "Peach Sorbet", "Warm coral glow pastel", null, listOf(0xFFFF758F, 0xFFFFB347, 0xFF2F1426)),
        BannerThemePreset("hd_cyber_violet", "Cyber Violet", "Synthwave laser mist", null, listOf(0xFFA855F7, 0xFFEC4899, 0xFF14092B)),
        BannerThemePreset("hd_platinum_mesh", "Platinum Mesh", "Brushed titanium weave", null, listOf(0xFFCBD5E1, 0xFF475569, 0xFF0F172A)),
        BannerThemePreset("hd_arctic_aurora", "Arctic Aurora", "Polar sky emerald ribbon", null, listOf(0xFF2DD4BF, 0xFF38BDF8, 0xFF071929)),
        BannerThemePreset("hd_amethyst_silk", "Amethyst Silk", "Velvet royal gemstone", null, listOf(0xFFD946EF, 0xFF7E22CE, 0xFF180828)),
        BannerThemePreset("hd_bronze_marble", "Bronze Marble", "Warm espresso stone", null, listOf(0xFFD97706, 0xFF78350F, 0xFF170D07)),
        BannerThemePreset("hd_aqua_lagoon", "Aqua Lagoon", "Crystal reef horizon", null, listOf(0xFF22D3EE, 0xFF0284C7, 0xFF051829)),
        BannerThemePreset("hd_neon_cherry", "Neon Cherry", "Vibrant blossom night", null, listOf(0xFFF43F5E, 0xFFBE185D, 0xFF1D0818)),
        BannerThemePreset("hd_indigo_storm", "Indigo Storm", "Electric thunder sky", null, listOf(0xFF6366F1, 0xFF312E81, 0xFF090922)),
        BannerThemePreset("hd_golden_honeycomb", "Golden Hive", "Warm luxury amber", null, listOf(0xFFFBBF24, 0xFFD97706, 0xFF1C1206)),
        BannerThemePreset("hd_mint_macaron", "Mint Macaron", "Dreamy pastel jade", null, listOf(0xFF6EE7B7, 0xFF0D9488, 0xFF092422)),
        BannerThemePreset("hd_stellar_slate", "Stellar Slate", "Deep cosmos graphite", null, listOf(0xFF60A5FA, 0xFF1E293B, 0xFF080C17)),
        BannerThemePreset("hd_candy_floss", "Candy Floss", "Cotton sky pink-blue", null, listOf(0xFFF472B6, 0xFF60A5FA, 0xFF24163D)),
        BannerThemePreset("hd_jade_dragon", "Jade Dragon", "Imperial green glow", null, listOf(0xFF00FF88, 0xFF047857, 0xFF031710)),
        BannerThemePreset("hd_ruby_velvet", "Ruby Velvet", "Deep garnet luxury", null, listOf(0xFFE11D48, 0xFF881337, 0xFF19060D)),
        BannerThemePreset("hd_moonlit_sand", "Moonlit Sand", "Silver dune twilight", null, listOf(0xFFE2E8F0, 0xFF64748B, 0xFF111827)),
        BannerThemePreset("hd_cyber_lime", "Cyber Lime", "Acid neon gridglow", null, listOf(0xFFA3E635, 0xFF16A34A, 0xFF07190C)),
        BannerThemePreset("hd_royal_plum", "Royal Plum", "Majestic orchid silk", null, listOf(0xFFC026D3, 0xFF6B21A8, 0xFF160726)),
        BannerThemePreset("hd_tropical_dusk", "Tropical Dusk", "Mango teal sunset", null, listOf(0xFFFB923C, 0xFF14B8A6, 0xFF14182B)),
        BannerThemePreset("hd_frost_diamond", "Frost Diamond", "Icy white sapphire", null, listOf(0xFFBAE6FD, 0xFF0EA5E9, 0xFF091B2E)),
        BannerThemePreset("hd_espresso_gold", "Espresso Gold", "Roast velvet bronze", null, listOf(0xFFF59E0B, 0xFF451A03, 0xFF120A05)),
        BannerThemePreset("hd_pixel_dawn", "Pixel Dawn", "Retro vapor horizon", null, listOf(0xFFFF7EB3, 0xFF7B2FFF, 0xFF110A2E)),
        BannerThemePreset("hd_cyan_katana", "Cyan Katana", "Sharp samurai neon", null, listOf(0xFF00F0FF, 0xFF0EA5E9, 0xFF050E1C)),
        BannerThemePreset("hd_blush_pearl", "Blush Pearl", "Iridescent rose mist", null, listOf(0xFFFBCFE8, 0xFFF472B6, 0xFF2B1226)),
        BannerThemePreset("hd_deep_abyss", "Deep Abyss", "Mariana blue depth", null, listOf(0xFF38BDF8, 0xFF1E3A8A, 0xFF040B1A)),
        BannerThemePreset("hd_solar_citrus", "Solar Citrus", "Tangerine gold burst", null, listOf(0xFFFACC15, 0xFFEA580C, 0xFF1F0F06)),
        BannerThemePreset("hd_titanium_blue", "Titanium Blue", "Metallic midnight sheen", null, listOf(0xFF93C5FD, 0xFF1E40AF, 0xFF0A1124)),
        BannerThemePreset("hd_berry_aurora", "Berry Aurora", "Cranberry sky light", null, listOf(0xFFFB7185, 0xFF9333EA, 0xFF180A26)),
        BannerThemePreset("hd_jade_bamboo", "Jade Bamboo", "Zen emerald mist", null, listOf(0xFF34D399, 0xFF15803D, 0xFF061B11)),
        BannerThemePreset("hd_champagne_silk", "Champagne Silk", "Soft ivory gold", null, listOf(0xFFFDE68A, 0xFFB45309, 0xFF1F160B)),
        BannerThemePreset("hd_hyper_magenta", "Hyper Magenta", "Hot neon studio glow", null, listOf(0xFFFF00E5, 0xFF7C3AED, 0xFF170526)),
        BannerThemePreset("hd_ocean_foam", "Ocean Foam", "Seafoam turquoise haze", null, listOf(0xFF5EEAD4, 0xFF0369A1, 0xFF061A26)),
        BannerThemePreset("hd_copper_forge", "Copper Forge", "Molten bronze metallic", null, listOf(0xFFFB923C, 0xFF9A3412, 0xFF1A0B06)),
        BannerThemePreset("hd_lilac_dream", "Lilac Dream", "Dreamy violet pastel", null, listOf(0xFFD8B4FE, 0xFF7E22CE, 0xFF1C1033)),
        BannerThemePreset("hd_cobalt_laser", "Cobalt Laser", "Electric sapphire beams", null, listOf(0xFF00D4FF, 0xFF2563EB, 0xFF060E26)),
        BannerThemePreset("hd_coral_reef", "Coral Reef", "Warm coral & sea teal", null, listOf(0xFFFF6B6B, 0xFF0D9488, 0xFF121826)),
        BannerThemePreset("hd_onyx_silver", "Onyx Silver", "Stealth chrome gloss", null, listOf(0xFFE2E8F0, 0xFF334155, 0xFF090D16)),
        BannerThemePreset("hd_amber_horizon", "Amber Horizon", "Sunset gold reflection", null, listOf(0xFFFBBF24, 0xFFDC2626, 0xFF1E0B09)),
        BannerThemePreset("hd_orchid_night", "Orchid Night", "Moonlit magenta bloom", null, listOf(0xFFE879F9, 0xFF4C1D95, 0xFF130826)),
        BannerThemePreset("hd_teal_carbon", "Teal Carbon", "Sport fiber aqua", null, listOf(0xFF14B8A6, 0xFF115E59, 0xFF061617)),
        BannerThemePreset("hd_starlight_blue", "Starlight Blue", "Clear night constellations", null, listOf(0xFF7DD3FC, 0xFF1D4ED8, 0xFF060E24)),
        BannerThemePreset("hd_rosewood_glow", "Rosewood Glow", "Warm mahogany satin", null, listOf(0xFFF43F5E, 0xFF9F1239, 0xFF1C0810)),
        BannerThemePreset("hd_electric_teal", "Electric Teal", "High-volt aqua stream", null, listOf(0xFF00F5D4, 0xFF06B6D4, 0xFF051822)),
        BannerThemePreset("hd_sunflower_dusk", "Sunflower Dusk", "Warm honey twilight", null, listOf(0xFFFDE047, 0xFFCA8A04, 0xFF1B1506)),
        BannerThemePreset("hd_neon_orchid", "Neon Orchid", "Vivid purple-cyan mist", null, listOf(0xFFC084FC, 0xFF00E5FF, 0xFF120C2B)),
        BannerThemePreset("hd_graphite_flame", "Graphite Flame", "Charcoal ember trace", null, listOf(0xFFFF4D00, 0xFF334155, 0xFF0A0D14)),
        BannerThemePreset("hd_pastel_mint", "Pastel Mint", "Fresh spring breeze", null, listOf(0xFFA7F3D0, 0xFF059669, 0xFF082219)),
        BannerThemePreset("hd_midnight_ruby", "Midnight Ruby", "Dark crimson shimmer", null, listOf(0xFFFB7185, 0xFF7F1D1D, 0xFF17050A)),
        BannerThemePreset("hd_sky_marshmallow", "Sky Marshmallow", "Fluffy cloud daydream", null, listOf(0xFF93C5FD, 0xFFF9A8D4, 0xFF261E4A)),
        BannerThemePreset("hd_gold_circuit", "Gold Circuit", "Luxury cyber microchip", null, listOf(0xFFFFD166, 0xFF00F5D4, 0xFF0F1420)),
        BannerThemePreset("hd_aurora_rose", "Aurora Rose", "Pink northern lights", null, listOf(0xFFF472B6, 0xFF2DD4BF, 0xFF160E29)),
        BannerThemePreset("hd_sapphire_ink", "Sapphire Ink", "Rich fountain pen blue", null, listOf(0xFF60A5FA, 0xFF1E3A8A, 0xFF060D21)),
        BannerThemePreset("hd_velvet_cocoa", "Velvet Cocoa", "Silky chocolate cream", null, listOf(0xFFD6A87C, 0xFF78350F, 0xFF160C07)),
        BannerThemePreset("hd_laser_lime", "Laser Lime", "Racing neon green", null, listOf(0xFF4ADE80, 0xFF00E5FF, 0xFF071A17)),
        BannerThemePreset("hd_cosmos_pearl", "Cosmos Pearl", "Milky way opal glow", null, listOf(0xFFE0E7FF, 0xFF818CF8, 0xFF0D1029)),
        BannerThemePreset("hd_sunset_cherry", "Sunset Cherry", "Crimson tangerine glow", null, listOf(0xFFFF5E7E, 0xFFFF9E00, 0xFF1E0B1A)),
        BannerThemePreset("hd_iceberg_cyan", "Iceberg Cyan", "Crystal glacial ridge", null, listOf(0xFF67E8F9, 0xFF0E7490, 0xFF061822)),
        BannerThemePreset("hd_bronze_lotus", "Bronze Lotus", "Antique rose copper", null, listOf(0xFFFDA4AF, 0xFFB45309, 0xFF1E0F12)),
        BannerThemePreset("hd_cyber_phoenix", "Cyber Phoenix", "Neon flame feather", null, listOf(0xFFFF3D00, 0xFFFF00E5, 0xFF18071E)),
        BannerThemePreset("hd_emerald_wave", "Emerald Wave", "Deep jade ocean glow", null, listOf(0xFF00FF88, 0xFF0EA5E9, 0xFF051A1C)),
        BannerThemePreset("hd_royal_indigo", "Royal Indigo", "Velvet midnight crown", null, listOf(0xFF818CF8, 0xFF4338CA, 0xFF0A0B24)),
        BannerThemePreset("hd_peach_blossom", "Peach Blossom", "Soft sakura morning", null, listOf(0xFFFECDD3, 0xFFFB7185, 0xFF28121E)),
        BannerThemePreset("hd_neon_cobalt", "Neon Cobalt", "High contrast blue", null, listOf(0xFF00E5FF, 0xFF1D4ED8, 0xFF060E24)),
        BannerThemePreset("hd_golden_dusk", "Golden Dusk", "Autumn amber breeze", null, listOf(0xFFF59E0B, 0xFF9333EA, 0xFF1A0F22)),
        BannerThemePreset("hd_frost_violet", "Frost Violet", "Icy lavender crystal", null, listOf(0xFFC4B5FD, 0xFF06B6D4, 0xFF13112B)),
        BannerThemePreset("hd_obsidian_cyan", "Obsidian Cyan", "Matte black aqua rim", null, listOf(0xFF22D3EE, 0xFF1E293B, 0xFF070B12)),
        BannerThemePreset("hd_ruby_flare", "Ruby Flare", "Radiant scarlet prism", null, listOf(0xFFEF4444, 0xFFEC4899, 0xFF1A0710)),
        BannerThemePreset("hd_jade_eclipse", "Jade Eclipse", "Glowing green corona", null, listOf(0xFF10B981, 0xFF3B82F6, 0xFF06161E)),
        BannerThemePreset("hd_plum_horizon", "Plum Horizon", "Dusk berry gradient", null, listOf(0xFFA855F7, 0xFFF43F5E, 0xFF1A0A21)),
        BannerThemePreset("hd_silver_frost", "Silver Frost", "Clean frosted mirror", null, listOf(0xFFF1F5F9, 0xFF64748B, 0xFF0F172A)),
        BannerThemePreset("hd_tangerine_pop", "Tangerine Pop", "Vibrant citrus neon", null, listOf(0xFFFF7A00, 0xFFFF2A85, 0xFF1E0B14)),
        BannerThemePreset("hd_aqua_nebula", "Aqua Nebula", "Deep teal galaxy haze", null, listOf(0xFF2DD4BF, 0xFF6366F1, 0xFF081426)),
        BannerThemePreset("hd_velvet_gold", "Velvet Gold", "Black tie golden mist", null, listOf(0xFFFFD166, 0xFF475569, 0xFF0B0E14)),
        BannerThemePreset("hd_bubblegum_sky", "Bubblegum Sky", "Cheerful candy pastel", null, listOf(0xFFFF85B3, 0xFF38BDF8, 0xFF21163B)),
        BannerThemePreset("hd_neon_jade", "Neon Jade", "Bright cyber emerald", null, listOf(0xFF00FFAA, 0xFF10B981, 0xFF041713)),
        BannerThemePreset("hd_twilight_rose", "Twilight Rose", "Romantic evening dusk", null, listOf(0xFFFB7185, 0xFF6366F1, 0xFF19102D)),
        BannerThemePreset("hd_electric_amber", "Electric Amber", "High-volt gold flash", null, listOf(0xFFFBBF24, 0xFF00E5FF, 0xFF121520)),
        BannerThemePreset("hd_arctic_slate", "Arctic Slate", "Nordic ice charcoal", null, listOf(0xFF94A3B8, 0xFF0EA5E9, 0xFF0B1320)),
        BannerThemePreset("hd_crimson_tide", "Crimson Tide", "Ruby ocean surge", null, listOf(0xFFE11D48, 0xFF2563EB, 0xFF120B21)),
        BannerThemePreset("hd_pastel_rainbow", "Pastel Rainbow", "Soft prism cloud mist", null, listOf(0xFFF9A8D4, 0xFFA7F3D0, 0xFF312E81)),
        BannerThemePreset("hd_obsidian_rose", "Obsidian Rose", "Dark satin pink rim", null, listOf(0xFFF472B6, 0xFF1F2937, 0xFF0B0D14)),
        BannerThemePreset("hd_deep_emerald", "Deep Emerald", "Lush forest night", null, listOf(0xFF34D399, 0xFF064E3B, 0xFF041410)),
        BannerThemePreset("hd_cyber_sapphire", "Cyber Sapphire", "Holographic blue sheen", null, listOf(0xFF38BDF8, 0xFFA855F7, 0xFF0A1026)),
        BannerThemePreset("hd_honey_quartz", "Honey Quartz", "Warm amber crystal", null, listOf(0xFFFCD34D, 0xFFF472B6, 0xFF21121D)),
        BannerThemePreset("hd_midnight_teal", "Midnight Teal", "Sleek dark turquoise", null, listOf(0xFF14B8A6, 0xFF0F172A, 0xFF050B14)),
        BannerThemePreset("hd_ultra_violet", "Ultra Violet", "Pure electric purple", null, listOf(0xFF8B5CF6, 0xFFEC4899, 0xFF120726))
    )

    val staticBannerPresets: List<BannerThemePreset> = baseStaticBannerPresets + expandedHdPhotoPresets

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

    /**
     * Maps every Touch Response to a harmonious matching default built-in Banner preset so
     * selecting a Touch Response automatically pairs it with a matching backdrop cleanly.
     */
    fun defaultBannerForTouchEffect(touchEffect: TouchTypingEffect): String {
        return when (touchEffect) {
            TouchTypingEffect.NONE -> PRESET_DARK_CARBON
            TouchTypingEffect.TOUCH_SHOCKWAVE_RING -> PRESET_CUTE_CARTOON
            TouchTypingEffect.TOUCH_SPARK -> PRESET_VICE_NEON_DRIVE
            TouchTypingEffect.TOUCH_ELECTRIC_CROSS,
            TouchTypingEffect.PHOTON_LASER_ARC,
            TouchTypingEffect.THUNDER_SPARK,
            TouchTypingEffect.VOLT_STRIKE -> PRESET_THUNDER_STORM
            TouchTypingEffect.TOUCH_MAGMA_EMBER,
            TouchTypingEffect.SOLAR_FLARE,
            TouchTypingEffect.INFERNO_RING,
            TouchTypingEffect.PHOENIX_FEATHER,
            TouchTypingEffect.LAVA_CORE -> PRESET_CYBER_DRAGON_FIRE
            TouchTypingEffect.TOUCH_FROST_FREEZE,
            TouchTypingEffect.CRYO_SHARD,
            TouchTypingEffect.ARCTIC_GLACIER -> "hd_glacier_mint"
            TouchTypingEffect.TOUCH_COMET_TRAIL,
            TouchTypingEffect.NEBULA_FLARE,
            TouchTypingEffect.STARLIGHT_DRIFT -> PRESET_GALAXY_NEBULA
            TouchTypingEffect.TOUCH_DIAMOND_FLASH,
            TouchTypingEffect.HOLOGRAM_PRISM_BURST,
            TouchTypingEffect.PRISM_SHATTER -> PRESET_ABSTRACT_RGB
            TouchTypingEffect.ROSE_QUARTZ_SHINE,
            TouchTypingEffect.SYNAPTIC_BLOOM -> PRESET_ROSE_GOLD_MARBLE
            TouchTypingEffect.MATRIX_CODE_BURST,
            TouchTypingEffect.BIO_TOXIC_GLOW -> PRESET_EMERALD_MATRIX
            TouchTypingEffect.ROYAL_CROWN_GLINT -> PRESET_GOLDEN_ROYAL_TIGER
            TouchTypingEffect.AURORA_MIST -> PRESET_AURORA_LIGHTS
            else -> when (touchEffect.visualStyle) {
                TouchEffectVisualStyle.RISING_EMBER -> PRESET_CYBER_DRAGON_FIRE
                TouchEffectVisualStyle.FROST_CRYSTAL -> "hd_glacier_mint"
                TouchEffectVisualStyle.ELECTRIC_CROSS -> PRESET_THUNDER_STORM
                TouchEffectVisualStyle.COMET_STREAK -> PRESET_VICE_SUNSET_PALMS
                TouchEffectVisualStyle.PRISM_SHATTER -> PRESET_ABSTRACT_RGB
                TouchEffectVisualStyle.GRAVITY_IMPLOSION -> PRESET_GALAXY_NEBULA
                TouchEffectVisualStyle.CHRONO_ORBIT -> PRESET_AURORA_LIGHTS
                TouchEffectVisualStyle.GLITCH_PULSE -> PRESET_NEON_CYBER
                TouchEffectVisualStyle.RADIAL_SHOCKWAVE -> when (touchEffect.category) {
                    TouchEffectCategory.QUANTUM_SPACE -> PRESET_GALAXY_NEBULA
                    TouchEffectCategory.PLASMA_ELEMENTAL -> PRESET_SUNSET_GLOW
                    TouchEffectCategory.PRISM_CHRONO -> PRESET_SAKURA_NIGHT
                    else -> PRESET_CUTE_CARTOON
                }
                TouchEffectVisualStyle.NEURAL_SYNAPSE -> PRESET_NEON_CYBER
                TouchEffectVisualStyle.NONE -> PRESET_DARK_CARBON
            }
        }
    }
}

@Immutable
data class CustomPhotoTheme(
    val photoPath: String? = null,
    val presetId: String? = null,
    val downloadedThemeId: String? = null,
    val dimAlpha: Float = 0.44f,
    val updatedAtMs: Long = 0L
) {
    val hasPreset: Boolean
        get() = BannerPresetCatalog.findById(presetId) != null

    val hasDownloadedCombo: Boolean
        get() = !downloadedThemeId.isNullOrBlank() && !photoPath.isNullOrBlank() && File(photoPath).exists()

    val hasCustomGalleryPhoto: Boolean
        get() = !hasPreset && !hasDownloadedCombo && !photoPath.isNullOrBlank() && File(photoPath).exists()

    val hasPhoto: Boolean
        get() = hasPreset || (!photoPath.isNullOrBlank() && File(photoPath).exists())

    val cacheKey: String
        get() = when {
            hasDownloadedCombo -> "combo:${downloadedThemeId.orEmpty()}:$updatedAtMs"
            hasPreset -> "preset:${presetId.orEmpty()}"
            !photoPath.isNullOrBlank() -> "file:$photoPath:$updatedAtMs"
            else -> "none"
        }
}

/**
 * Persists built-in HD combo/static banner presets, on-demand downloaded themes
 * (`context.filesDir/themes/<id>.jpg`), and user-selected gallery photos
 * (`context.filesDir/custom_bg.jpg`) plus background dim/opacity in SharedPreferences so
 * themes survive restarts and sync live between MainActivity and RgbKeyboardService.
 */
class CustomBackgroundManager private constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val themesDir: File = File(context.filesDir, THEMES_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    private val _theme = MutableStateFlow(readCurrentState())
    val theme: StateFlow<CustomPhotoTheme> = _theme.asStateFlow()

    private val _downloadedThemeIds = MutableStateFlow(scanDownloadedThemeIds())
    val downloadedThemeIds: StateFlow<Set<String>> = _downloadedThemeIds.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _theme.value = readCurrentState()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    private fun scanDownloadedThemeIds(): Set<String> {
        if (!themesDir.exists()) return emptySet()
        return themesDir.listFiles()
            ?.filter { it.isFile && it.length() > 0L && it.name.endsWith(".jpg") }
            ?.map { it.name.removeSuffix(".jpg") }
            ?.toSet()
            .orEmpty()
    }

    fun getDownloadedThemeFile(comboId: String): File {
        val safeId = comboId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        if (!themesDir.exists()) themesDir.mkdirs()
        return File(themesDir, "$safeId.jpg")
    }

    fun isComboDownloaded(comboId: String): Boolean {
        val f = getDownloadedThemeFile(comboId)
        return f.exists() && f.length() > 0L
    }

    private fun readCurrentState(): CustomPhotoTheme {
        val savedDownloadedId = prefs.getString(KEY_DOWNLOADED_THEME_ID, null)?.takeIf { it.isNotBlank() }
        val savedPreset = prefs.getString(KEY_PRESET_ID, null)
            ?.takeIf { BannerPresetCatalog.findById(it) != null }
        val savedPath = prefs.getString(KEY_BG_PATH, null)
        val validPath = savedPath?.takeIf { it.isNotBlank() && File(it).exists() }
        val dim = prefs.getFloat(KEY_BG_DIM, DEFAULT_DIM_ALPHA).coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        val updatedAt = prefs.getLong(KEY_BG_UPDATED_AT, 0L)
        return if (savedDownloadedId != null && validPath != null) {
            CustomPhotoTheme(
                photoPath = validPath,
                presetId = null,
                downloadedThemeId = savedDownloadedId,
                dimAlpha = dim,
                updatedAtMs = updatedAt
            )
        } else {
            CustomPhotoTheme(
                photoPath = if (savedPreset != null) null else validPath,
                presetId = savedPreset,
                downloadedThemeId = null,
                dimAlpha = dim,
                updatedAtMs = updatedAt
            )
        }
    }

    fun getCurrentTheme(): CustomPhotoTheme = readCurrentState()

    fun reloadFromPrefs(): CustomPhotoTheme {
        val fresh = readCurrentState()
        _theme.value = fresh
        _downloadedThemeIds.value = scanDownloadedThemeIds()
        return fresh
    }

    fun selectPreset(presetId: String) {
        if (BannerPresetCatalog.findById(presetId) == null) return
        val now = System.currentTimeMillis()
        val currentDim = prefs.getFloat(KEY_BG_DIM, DEFAULT_DIM_ALPHA).coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        prefs.edit()
            .remove(KEY_DOWNLOADED_THEME_ID)
            .putString(KEY_PRESET_ID, presetId)
            .putFloat(KEY_BG_DIM, currentDim)
            .putLong(KEY_BG_UPDATED_AT, now)
            .putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true)
            .apply()
        _theme.value = readCurrentState()
    }

    fun selectPresetWithDim(presetId: String, dimAlpha: Float) {
        if (BannerPresetCatalog.findById(presetId) == null) return
        val now = System.currentTimeMillis()
        val clampedDim = dimAlpha.coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        prefs.edit()
            .remove(KEY_DOWNLOADED_THEME_ID)
            .putString(KEY_PRESET_ID, presetId)
            .putFloat(KEY_BG_DIM, clampedDim)
            .putLong(KEY_BG_UPDATED_AT, now)
            .putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true)
            .apply()
        _theme.value = readCurrentState()
    }

    fun selectDownloadedCombo(comboId: String): Boolean {
        val file = getDownloadedThemeFile(comboId)
        if (!file.exists() || file.length() <= 0L) return false
        val now = System.currentTimeMillis()
        val currentDim = prefs.getFloat(KEY_BG_DIM, DEFAULT_DIM_ALPHA).coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        prefs.edit()
            .remove(KEY_PRESET_ID)
            .putString(KEY_DOWNLOADED_THEME_ID, comboId)
            .putString(KEY_BG_PATH, file.absolutePath)
            .putFloat(KEY_BG_DIM, currentDim)
            .putLong(KEY_BG_UPDATED_AT, now)
            .putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true)
            .apply()
        _theme.value = readCurrentState()
        return true
    }

    /**
     * Activates Cute Cartoon Sky on initial new-device VIP trial once if user hasn't picked a backdrop yet.
     */
    fun seedTrialDefaultPresetIfUnset(presetId: String = BannerPresetCatalog.PRESET_CUTE_CARTOON) {
        if (prefs.getBoolean(KEY_TRIAL_PRESET_SEEDED_V1, false)) return
        val current = readCurrentState()
        if (current.hasPhoto) {
            prefs.edit().putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true).apply()
            return
        }
        if (BannerPresetCatalog.findById(presetId) == null) return
        prefs.edit()
            .remove(KEY_DOWNLOADED_THEME_ID)
            .putString(KEY_PRESET_ID, presetId)
            .putFloat(KEY_BG_DIM, DEFAULT_DIM_ALPHA)
            .putLong(KEY_BG_UPDATED_AT, System.currentTimeMillis())
            .putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true)
            .apply()
        _theme.value = readCurrentState()
    }

    fun resetPostTrialVisualRevertFlag() {
        if (prefs.getBoolean(KEY_POST_TRIAL_VISUAL_REVERTED_V1, false)) {
            prefs.edit().putBoolean(KEY_POST_TRIAL_VISUAL_REVERTED_V1, false).apply()
        }
    }

    /**
     * When free trial expires without active VIP/override: reverts built-in presets/combos to Classic Black,
     * while preserving the user's own Custom Gallery Photo (`hasCustomGalleryPhoto`) permanently free.
     */
    fun revertPaidVisualsOnTrialExpiryPreservingGalleryPhoto() {
        if (prefs.getBoolean(KEY_POST_TRIAL_VISUAL_REVERTED_V1, false)) return
        val current = readCurrentState()
        if (current.hasCustomGalleryPhoto) {
            prefs.edit().putBoolean(KEY_POST_TRIAL_VISUAL_REVERTED_V1, true).apply()
            return
        }
        prefs.edit()
            .remove(KEY_BG_PATH)
            .remove(KEY_PRESET_ID)
            .remove(KEY_DOWNLOADED_THEME_ID)
            .putLong(KEY_BG_UPDATED_AT, System.currentTimeMillis())
            .putBoolean(KEY_POST_TRIAL_VISUAL_REVERTED_V1, true)
            .apply()
        _theme.value = readCurrentState()
    }

    /**
     * Immediately strips any built-in banner preset or downloaded combo theme when VIP is expired,
     * while strictly preserving the user's own Custom Gallery Photo (`hasCustomGalleryPhoto`).
     */
    fun clearBuiltInPresetOrComboPreservingGalleryPhoto() {
        val current = readCurrentState()
        if (current.hasCustomGalleryPhoto) return
        if (!current.hasPreset && !current.hasDownloadedCombo && current.photoPath.isNullOrBlank()) return
        prefs.edit()
            .remove(KEY_BG_PATH)
            .remove(KEY_PRESET_ID)
            .remove(KEY_DOWNLOADED_THEME_ID)
            .putLong(KEY_BG_UPDATED_AT, System.currentTimeMillis())
            .apply()
        _theme.value = readCurrentState()
    }

    /**
     * Downloads or renders a 1-Tap Combo Theme on demand and stores it locally inside
     * `filesDir/themes/<comboId>.jpg` so it stays offline-ready and lightweight.
     */
    suspend fun downloadComboTheme(combo: ComboThemePreset): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = getDownloadedThemeFile(combo.id)
            val tempFile = File(themesDir, "${combo.id}.tmp")

            val remoteBitmap: Bitmap? = combo.remoteImageUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { decodeRemoteOrBase64Image(it) }

            val bitmapToSave: Bitmap = remoteBitmap ?: renderComboArtworkBitmap(combo)
            try {
                FileOutputStream(tempFile).use { out ->
                    bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }
            } finally {
                if (remoteBitmap != null && !remoteBitmap.isRecycled) {
                    remoteBitmap.recycle()
                } else if (!bitmapToSave.isRecycled && bitmapToSave !in proceduralBitmapCache.values) {
                    bitmapToSave.recycle()
                }
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            _downloadedThemeIds.value = scanDownloadedThemeIds()
            targetFile
        }
    }

    private fun decodeRemoteOrBase64Image(source: String): Bitmap? {
        return runCatching {
            val trimmed = source.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                val conn = (URL(trimmed).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 9000
                    readTimeout = 9000
                    instanceFollowRedirects = true
                }
                try {
                    if (conn.responseCode in 200..299) {
                        conn.inputStream.use { stream -> BitmapFactory.decodeStream(stream) }
                    } else null
                } finally {
                    conn.disconnect()
                }
            } else {
                val base64Clean = trimmed.substringAfter("base64,", trimmed)
                val bytes = Base64.decode(base64Clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }

    private fun renderComboArtworkBitmap(combo: ComboThemePreset, w: Int = 960, h: Int = 480): Bitmap {
        val knownPreset = BannerPresetCatalog.findById(combo.bannerPresetId)
        if (knownPreset != null && knownPreset.drawableRes != null) {
            val resBmp = runCatching {
                BitmapFactory.decodeResource(context.resources, knownPreset.drawableRes)
            }.getOrNull()
            if (resBmp != null) {
                val scaled = Bitmap.createScaledBitmap(resBmp, w, h, true)
                if (scaled != resBmp) resBmp.recycle()
                return scaled
            }
        }
        if (knownPreset != null && knownPreset.drawableRes == null && combo.bannerPresetId != BannerPresetCatalog.PRESET_EMERALD_MATRIX) {
            return renderProceduralPresetBitmap(knownPreset.id, w, h)
        }
        // Bespoke high-contrast 960x480 procedural canvas tailored to combo.previewColors + combo.category
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val wf = w.toFloat()
        val hf = h.toFloat()
        val colors = combo.previewColors.map { it.toInt() }.ifEmpty {
            listOf(Color.parseColor("#00E5FF"), Color.parseColor("#FF2A85"), Color.parseColor("#090E20"))
        }
        val c0 = colors.getOrElse(0) { Color.parseColor("#00E5FF") }
        val c1 = colors.getOrElse(1) { c0 }
        val c2 = colors.getOrElse(2) { Color.parseColor("#070B19") }
        val seed = kotlin.math.abs(combo.id.hashCode())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, wf, hf,
            intArrayOf(c2, blendColor(c2, c1, 0.45f), blendColor(c2, c0, 0.35f), c2),
            floatArrayOf(0f, 0.35f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, wf, hf, paint)

        // Luminous focal nebula glow
        val fx = wf * (0.25f + (seed % 50) / 100f)
        val fy = hf * (0.28f + ((seed / 7) % 44) / 100f)
        paint.shader = RadialGradient(
            fx, fy, wf * 0.48f,
            intArrayOf(setAlpha(c0, 195), setAlpha(c1, 105), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, wf, hf, paint)

        // Secondary counter-glow
        paint.shader = RadialGradient(
            wf - fx, hf - fy, wf * 0.42f,
            intArrayOf(setAlpha(c1, 160), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, wf, hf, paint)
        paint.shader = null

        // Category-specific geometric luxury overlay
        when (combo.category) {
            ComboCategory.CYBER_GAMING -> {
                // Perspective cyber neon grid + laser diagonal slashes
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.2f
                paint.color = setAlpha(c0, 82)
                var gx = 0f
                while (gx <= wf) {
                    canvas.drawLine(gx, 0f, gx, hf, paint)
                    gx += 64f
                }
                var gy = 0f
                while (gy <= hf) {
                    canvas.drawLine(0f, gy, wf, gy, paint)
                    gy += 64f
                }
                paint.strokeWidth = 7f
                paint.color = setAlpha(c0, 175)
                canvas.drawLine(wf * 0.12f, hf, wf * 0.44f, 0f, paint)
                paint.strokeWidth = 4f
                paint.color = setAlpha(c1, 165)
                canvas.drawLine(wf * 0.62f, hf, wf * 0.92f, 0f, paint)
            }
            ComboCategory.ROYAL_LUXURY -> {
                // Velvet metallic ribbons & diamond lattice
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                for (i in 0..6) {
                    val p = Path()
                    val yBase = hf * (0.12f + i * 0.13f)
                    p.moveTo(0f, yBase)
                    p.cubicTo(wf * 0.3f, yBase - 42f, wf * 0.7f, yBase + 42f, wf, yBase - 14f)
                    paint.strokeWidth = if (i % 2 == 0) 5f else 2.5f
                    paint.color = setAlpha(if (i % 2 == 0) c0 else c1, 130)
                    canvas.drawPath(p, paint)
                }
                paint.style = Paint.Style.FILL
                for (s in 0..16) {
                    val sx = ((s * 173 + seed) % w).toFloat()
                    val sy = ((s * 97 + seed) % h).toFloat()
                    paint.color = setAlpha(Color.WHITE, 195)
                    drawSparkleStar(canvas, sx, sy, 8f + (s % 4) * 3f, paint)
                }
            }
            ComboCategory.SPACE_SCIFI -> {
                // Orbital rings + dense starfield
                paint.style = Paint.Style.STROKE
                for (r in 1..4) {
                    paint.strokeWidth = (5f - r * 0.7f).coerceAtLeast(1.5f)
                    paint.color = setAlpha(if (r % 2 == 0) c0 else c1, 115)
                    canvas.drawOval(
                        wf * 0.5f - r * 115f, hf * 0.5f - r * 48f,
                        wf * 0.5f + r * 115f, hf * 0.5f + r * 48f,
                        paint
                    )
                }
                paint.style = Paint.Style.FILL
                for (s in 0..55) {
                    val sx = ((s * 211 + seed * 3) % w).toFloat()
                    val sy = ((s * 137 + seed) % h).toFloat()
                    paint.color = setAlpha(if (s % 4 == 0) c0 else Color.WHITE, 210)
                    canvas.drawCircle(sx, sy, 1.8f + (s % 4) * 1.1f, paint)
                }
            }
            ComboCategory.ELEMENTAL -> {
                // Swirling energy arcs + rising embers / frost crystals
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                for (i in 0..7) {
                    val path = Path()
                    val yBase = hf * (0.16f + i * 0.1f)
                    path.moveTo(-20f, yBase)
                    path.cubicTo(wf * 0.3f, yBase - hf * 0.2f, wf * 0.68f, yBase + hf * 0.22f, wf + 20f, yBase - hf * 0.08f)
                    paint.strokeWidth = (13f - i * 1.1f).coerceAtLeast(3f)
                    paint.color = setAlpha(if (i % 2 == 0) c0 else c1, 140)
                    canvas.drawPath(path, paint)
                }
                paint.style = Paint.Style.FILL
                for (e in 0..34) {
                    val ex = ((e * 193 + seed) % w).toFloat()
                    val ey = ((e * 127 + seed) % h).toFloat()
                    paint.color = setAlpha(c0, 200)
                    canvas.drawCircle(ex, ey, 2.5f + (e % 5) * 1.5f, paint)
                }
            }
            else -> {
                // Aesthetic dreamy bokeh orbs & sparkles
                paint.style = Paint.Style.FILL
                for (b in 0..16) {
                    val bx = ((b * 181 + seed) % w).toFloat()
                    val by = ((b * 109 + seed) % h).toFloat()
                    val rad = 26f + (b % 5) * 18f
                    paint.color = setAlpha(if (b % 2 == 0) c0 else c1, 58)
                    canvas.drawCircle(bx, by, rad, paint)
                }
                for (s in 0..18) {
                    val sx = ((s * 167 + seed) % w).toFloat()
                    val sy = ((s * 89 + seed) % h).toFloat()
                    paint.color = setAlpha(Color.WHITE, 210)
                    drawSparkleStar(canvas, sx, sy, 7f + (s % 4) * 3f, paint)
                }
            }
        }
        return bmp
    }

    private fun setAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun blendColor(a: Int, b: Int, ratio: Float): Int {
        val inv = 1f - ratio
        return Color.argb(
            255,
            (Color.red(a) * inv + Color.red(b) * ratio).toInt().coerceIn(0, 255),
            (Color.green(a) * inv + Color.green(b) * ratio).toInt().coerceIn(0, 255),
            (Color.blue(a) * inv + Color.blue(b) * ratio).toInt().coerceIn(0, 255)
        )
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
                .remove(KEY_DOWNLOADED_THEME_ID)
                .putString(KEY_BG_PATH, targetFile.absolutePath)
                .putFloat(KEY_BG_DIM, currentDim)
                .putLong(KEY_BG_UPDATED_AT, now)
                .putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true)
                .apply()

            val updated = CustomPhotoTheme(
                photoPath = targetFile.absolutePath,
                presetId = null,
                downloadedThemeId = null,
                dimAlpha = currentDim,
                updatedAtMs = now
            )
            _theme.value = updated
            updated
        }
    }

    suspend fun saveCroppedPhotoBitmap(bitmap: Bitmap, dimAlpha: Float): Result<CustomPhotoTheme> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = File(context.filesDir, BG_FILE_NAME)
            val tempFile = File(context.filesDir, "$BG_FILE_NAME.tmp")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val now = System.currentTimeMillis()
            val clampedDim = dimAlpha.coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
            prefs.edit()
                .remove(KEY_PRESET_ID)
                .remove(KEY_DOWNLOADED_THEME_ID)
                .putString(KEY_BG_PATH, targetFile.absolutePath)
                .putFloat(KEY_BG_DIM, clampedDim)
                .putLong(KEY_BG_UPDATED_AT, now)
                .putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true)
                .apply()

            val updated = CustomPhotoTheme(
                photoPath = targetFile.absolutePath,
                presetId = null,
                downloadedThemeId = null,
                dimAlpha = clampedDim,
                updatedAtMs = now
            )
            _theme.value = updated
            updated
        }
    }

    suspend fun saveRemoteBannerImage(imageSource: String, dimAlpha: Float): Result<CustomPhotoTheme> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = imageSource.trim()
            val bytes: ByteArray = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                val conn = (URL(trimmed).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { it.readBytes() }
            } else {
                val rawB64 = trimmed.substringAfter("base64,", trimmed)
                Base64.decode(rawB64, Base64.DEFAULT)
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Unable to decode banner artwork")
            val saved = saveCroppedPhotoBitmap(bmp, dimAlpha).getOrThrow()
            bmp.recycle()
            saved
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
            .remove(KEY_DOWNLOADED_THEME_ID)
            .putLong(KEY_BG_UPDATED_AT, System.currentTimeMillis())
            .putBoolean(KEY_TRIAL_PRESET_SEEDED_V1, true)
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

            BannerPresetCatalog.PRESET_EMERALD_MATRIX -> {
                // Emerald Matrix procedural grid
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

            else -> {
                // Curated multi-stop procedural HD artwork driven by preset colors & seed
                val colors = BannerPresetCatalog.findById(presetId)?.previewColors?.map { it.toInt() }.orEmpty()
                val c0 = colors.getOrElse(0) { Color.parseColor("#00E5FF") }
                val c1 = colors.getOrElse(1) { Color.parseColor("#7B2FFF") }
                val c2 = colors.getOrElse(2) { Color.parseColor("#0A1022") }
                val seed = kotlin.math.abs(presetId.hashCode())

                paint.shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(c2, blendColor(c2, c1, 0.58f), blendColor(c1, c0, 0.42f), c2),
                    floatArrayOf(0f, 0.38f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)

                val focusX = wf * (0.22f + (seed % 56) / 100f)
                val focusY = hf * (0.25f + ((seed / 11) % 50) / 100f)
                paint.shader = RadialGradient(
                    focusX, focusY, wf * 0.56f,
                    intArrayOf(setAlpha(c0, 185), setAlpha(c1, 95), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.48f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)

                paint.shader = RadialGradient(
                    wf - focusX, hf - focusY, wf * 0.46f,
                    intArrayOf(setAlpha(c1, 155), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, wf, hf, paint)
                paint.shader = null

                when (seed % 3) {
                    0 -> {
                        // Flowing silk ribbons + subtle stars
                        paint.style = Paint.Style.STROKE
                        paint.strokeCap = Paint.Cap.ROUND
                        for (i in 0..5) {
                            val ribbon = Path()
                            val y0 = hf * (0.14f + i * 0.14f)
                            ribbon.moveTo(-16f, y0)
                            ribbon.cubicTo(wf * 0.32f, y0 - 36f, wf * 0.68f, y0 + 36f, wf + 16f, y0 - 12f)
                            paint.strokeWidth = 6f - i * 0.6f
                            paint.color = setAlpha(if (i % 2 == 0) c0 else c1, 110)
                            canvas.drawPath(ribbon, paint)
                        }
                    }
                    1 -> {
                        // Dreamy bokeh & sparkle highlights
                        paint.style = Paint.Style.FILL
                        for (b in 0..14) {
                            val bx = ((b * 179 + seed) % w).toFloat()
                            val by = ((b * 113 + seed) % h).toFloat()
                            paint.color = setAlpha(if (b % 2 == 0) c0 else c1, 56)
                            canvas.drawCircle(bx, by, 22f + (b % 5) * 16f, paint)
                        }
                    }
                    else -> {
                        // Diagonal architectural prism streaks
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 3f
                        for (d in 0..7) {
                            val xStart = ((d * 140 + seed % 90) % (w + 180)) - 90f
                            paint.color = setAlpha(if (d % 2 == 0) c0 else Color.WHITE, 64)
                            canvas.drawLine(xStart, hf, xStart + 180f, 0f, paint)
                        }
                    }
                }
                paint.style = Paint.Style.FILL
                for (s in 0..14) {
                    val sx = ((s * 197 + seed * 3) % w).toFloat()
                    val sy = ((s * 97 + seed) % h).toFloat()
                    paint.color = setAlpha(Color.WHITE, 190)
                    drawSparkleStar(canvas, sx, sy, 6f + (s % 4) * 2.5f, paint)
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
        private const val KEY_DOWNLOADED_THEME_ID = "downloaded_theme_id"
        private const val KEY_BG_DIM = "custom_bg_dim"
        private const val KEY_BG_UPDATED_AT = "custom_bg_updated_at"
        private const val KEY_TRIAL_PRESET_SEEDED_V1 = "trial_preset_seeded_v1"
        private const val KEY_POST_TRIAL_VISUAL_REVERTED_V1 = "post_trial_visual_reverted_v1"
        private const val BG_FILE_NAME = "custom_bg.jpg"
        private const val THEMES_DIR_NAME = "themes"
        const val DEFAULT_DIM_ALPHA = 0.24f
        const val MIN_DIM_ALPHA = 0.05f
        const val MAX_DIM_ALPHA = 0.85f

        /** Maps user-facing 0%..100% Brightness (`0f..1f`) to dark overlay `dimAlpha` (`0.85f..0.05f`). */
        fun brightnessToDimAlpha(brightnessFraction: Float): Float {
            val b = brightnessFraction.coerceIn(0f, 1f)
            return (MAX_DIM_ALPHA - b * (MAX_DIM_ALPHA - MIN_DIM_ALPHA)).coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
        }

        /** Maps dark overlay `dimAlpha` (`0.05f..0.85f`) to user-facing Brightness (`0f..1f`). */
        fun dimAlphaToBrightness(dimAlpha: Float): Float {
            val d = dimAlpha.coerceIn(MIN_DIM_ALPHA, MAX_DIM_ALPHA)
            return ((MAX_DIM_ALPHA - d) / (MAX_DIM_ALPHA - MIN_DIM_ALPHA)).coerceIn(0f, 1f)
        }

        @Volatile
        private var instance: CustomBackgroundManager? = null

        fun getInstance(context: Context): CustomBackgroundManager {
            return instance ?: synchronized(this) {
                instance ?: CustomBackgroundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
