package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class TouchEffectCategory(val id: String, val label: String) {
    ALL("all", "✨ All (106)"),
    AI_NEURAL("ai", "🧠 AI & Neural"),
    QUANTUM_SPACE("quantum", "🌌 Quantum & Space"),
    CYBER_LASER("cyber", "⚡ Cyber & Laser"),
    PLASMA_ELEMENTAL("plasma", "🔥 Plasma & Cryo"),
    PRISM_CHRONO("prism", "💎 Prism & Chrono")
}

enum class TouchEffectVisualStyle {
    NONE,
    NEURAL_SYNAPSE,
    RADIAL_SHOCKWAVE,
    ELECTRIC_CROSS,
    PRISM_SHATTER,
    RISING_EMBER,
    GRAVITY_IMPLOSION,
    CHRONO_ORBIT,
    COMET_STREAK,
    FROST_CRYSTAL,
    GLITCH_PULSE
}

/**
 * 56 Original AI & Physics Touch-Reactive Typing Effects + NONE (Off/Clean).
 * Layer cleanly on top of any active Background Theme, Banner, or Ambient RGB Animation.
 */
enum class TouchTypingEffect(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val badge: String,
    val description: String,
    val category: TouchEffectCategory = TouchEffectCategory.AI_NEURAL,
    val visualStyle: TouchEffectVisualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
    val primaryHex: Long = 0xFF00F0FF,
    val secondaryHex: Long = 0xFFFF2A85,
    val isVipEffect: Boolean = true
) {
    NONE(
        id = "touch_none",
        displayName = "অফ (Clean Keycaps)",
        shortLabel = "Off (Clean)",
        badge = "🖤 Off",
        description = "Clean keycaps with zero typing glow — keeps Banners & Package Themes crisp",
        category = TouchEffectCategory.ALL,
        visualStyle = TouchEffectVisualStyle.NONE,
        primaryHex = 0xFF000000,
        secondaryHex = 0xFF000000,
        isVipEffect = false
    ),

    // --- Preserved Core 7 Touch Effects (1..7) ---
    TOUCH_SPARK(
        id = "touch_spark",
        displayName = "✨ Touch Spark",
        shortLabel = "Touch Spark",
        badge = "✨ Spark",
        description = "Stealth matte keys at rest; pressed key rises in vivid neon glow",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFF00F0FF,
        secondaryHex = 0xFFFF2A85
    ),
    TOUCH_ELECTRIC_CROSS(
        id = "touch_electric_cross",
        displayName = "⚡ Touch Electric Cross",
        shortLabel = "Electric Cross",
        badge = "⚡ Cross",
        description = "Pressing a key illuminates its horizontal row & vertical column in a neon laser cross",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFFFFFFFF
    ),
    TOUCH_SHOCKWAVE_RING(
        id = "touch_shockwave_ring",
        displayName = "💥 Touch Shockwave Ring",
        shortLabel = "Shockwave Ring",
        badge = "💥 Ring",
        description = "Tapping any key expands a glowing radial neon shockwave ring outward",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFFFF2A85
    ),
    TOUCH_MAGMA_EMBER(
        id = "touch_magma_ember",
        displayName = "🔥 Touch Magma Ember",
        shortLabel = "Magma Ember",
        badge = "🔥 Ember",
        description = "Tapped keys ignite in molten gold-crimson glow and launch rising micro-sparks",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF3D00,
        secondaryHex = 0xFFFFD600
    ),
    TOUCH_FROST_FREEZE(
        id = "touch_frost_freeze",
        displayName = "❄️ Touch Frost Freeze",
        shortLabel = "Frost Freeze",
        badge = "❄️ Frost",
        description = "Tapping a key freezes it and immediate neighbors in icy cyan-white crystal light",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFE0FBFC,
        secondaryHex = 0xFF38BDF8
    ),
    TOUCH_COMET_TRAIL(
        id = "touch_comet_trail",
        displayName = "🌠 Touch Comet Trail",
        shortLabel = "Comet Trail",
        badge = "🌠 Comet",
        description = "Rapid typing draws a luminous neon comet tail connecting the last 4 pressed keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF00F5D4
    ),
    TOUCH_DIAMOND_FLASH(
        id = "touch_diamond_flash",
        displayName = "💎 Touch Diamond Flash",
        shortLabel = "Diamond Flash",
        badge = "💎 Glint",
        description = "Every keypress triggers a crisp 4-point diamond star glint across beveled key edges",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFFA855F7
    ),

    // --- 🧠 AI & NEURAL TOUCH RESPONSES (8..18) ---
    AI_NEURAL_PULSE(
        id = "ai_neural_pulse",
        displayName = "🧠 AI Neural Pulse",
        shortLabel = "AI Neural",
        badge = "🧠 Neural",
        description = "Synaptic cyan-violet neural signal fires from pressed key to neighboring nodes",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFF7B2FFF
    ),
    PLASMA_SYNAPSE(
        id = "plasma_synapse",
        displayName = "⚡ Plasma Synapse",
        shortLabel = "Synapse",
        badge = "⚡ Synapse",
        description = "Bio-electric neon dendrites arc brightly between keycaps on every strike",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFF00E5FF
    ),
    SONIC_ECHO(
        id = "sonic_echo",
        displayName = "🔊 Sonic Echo",
        shortLabel = "Sonic Echo",
        badge = "🔊 Echo",
        description = "Acoustic neon soundwave rings reverberate outward across the keybed",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFF818CF8
    ),
    CORTEX_SURGE(
        id = "cortex_surge",
        displayName = "🧬 Cortex Surge",
        shortLabel = "Cortex Surge",
        badge = "🧬 Cortex",
        description = "Smart emerald-teal cortex wave cascades instantaneously across adjacent keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF00F59B,
        secondaryHex = 0xFF00D4FF
    ),
    SYNAPTIC_BLOOM(
        id = "synaptic_bloom",
        displayName = "🌺 Synaptic Bloom",
        shortLabel = "Synapse Bloom",
        badge = "🌺 Bloom",
        description = "Rose-gold neural petals expand radially from the pressed key center",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF4D6D,
        secondaryHex = 0xFFFFB703
    ),
    SENTIENT_GLINT(
        id = "sentient_glint",
        displayName = "🤖 Sentient Glint",
        shortLabel = "Sentient",
        badge = "🤖 AI",
        description = "Precision cyber-cyan glint sharpens the pressed key and diagonal corners",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF22D3EE,
        secondaryHex = 0xFFFFFFFF
    ),
    MIND_LINK_BEAM(
        id = "mind_link_beam",
        displayName = "🔗 Mind Link Beam",
        shortLabel = "Mind Link",
        badge = "🔗 Link",
        description = "Connects consecutive typed letters with luminous holographic neural threads",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF818CF8,
        secondaryHex = 0xFF00F5D4
    ),
    MATRIX_CODE_BURST(
        id = "matrix_code_burst",
        displayName = "🟩 Matrix Code Burst",
        shortLabel = "Matrix Burst",
        badge = "🟩 Code",
        description = "Phosphor emerald digital glitch sparks flash across the tapped column",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF00FF66,
        secondaryHex = 0xFF70FFBA
    ),
    BIOMETRIC_SCAN(
        id = "biometric_scan",
        displayName = "👁️ Biometric Scan",
        shortLabel = "Bio Scan",
        badge = "👁️ Scan",
        description = "Crisp sapphire scanner reticle sweeps horizontally and vertically on tap",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF3B82F6,
        secondaryHex = 0xFF00F5D4
    ),
    AXIOM_FLASH(
        id = "axiom_flash",
        displayName = "⚛️ Axiom Flash",
        shortLabel = "Axiom",
        badge = "⚛️ Axiom",
        description = "Twin orbital electrons spin cleanly around every pressed keycap",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFFFF2E93
    ),
    DOPAMINE_DROP(
        id = "dopamine_drop",
        displayName = "💧 Dopamine Drop",
        shortLabel = "Dopamine",
        badge = "💧 Drop",
        description = "Liquid neon droplet ripples gently outward with soft ultraviolet shimmer",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFC084FC,
        secondaryHex = 0xFF00F5D4
    ),

    // --- 🌌 QUANTUM & SPACE TOUCH RESPONSES (19..29) ---
    QUANTUM_GRAVITY_RIPPLE(
        id = "quantum_gravity_ripple",
        displayName = "🌌 Quantum Gravity Ripple",
        shortLabel = "Gravity Ripple",
        badge = "🌌 Quantum",
        description = "Spacetime shockwave bends indigo-cyan light around the tapped keycap",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF7B2FFF,
        secondaryHex = 0xFF00E5FF
    ),
    AURA_SINGULARITY(
        id = "aura_singularity",
        displayName = "🕳️ Aura Singularity",
        shortLabel = "Singularity",
        badge = "🕳️ Void",
        description = "Gravitational ring pulls violet-amber glow inward onto the active key",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF9D4EDD,
        secondaryHex = 0xFFFFB703
    ),
    NEBULA_FLARE(
        id = "nebula_flare",
        displayName = "🌠 Nebula Flare",
        shortLabel = "Nebula Flare",
        badge = "🌠 Nebula",
        description = "Cosmic magenta-cyan stardust cloud erupts upward from every key press",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF38BDF8
    ),
    VOID_PULL(
        id = "void_pull",
        displayName = "🌑 Void Pull",
        shortLabel = "Void Pull",
        badge = "🌑 Pull",
        description = "Dark-violet accretion wave draws energy cleanly toward the key center",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF560BAD,
        secondaryHex = 0xFF00F5D4
    ),
    STARLIGHT_DRIFT(
        id = "starlight_drift",
        displayName = "✨ Starlight Drift",
        shortLabel = "Starlight",
        badge = "✨ Stars",
        description = "Silvery gold starlight motes float softly above pressed keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFACC15,
        secondaryHex = 0xFFE0F2FE
    ),
    NOVA_SPARK(
        id = "nova_spark",
        displayName = "💥 Nova Spark",
        shortLabel = "Nova Spark",
        badge = "💥 Nova",
        description = "Supernova white-orange burst fractures outward across surrounding keycaps",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFF9E00,
        secondaryHex = 0xFFFFFFFF
    ),
    GRAVITY_WELL(
        id = "gravity_well",
        displayName = "🌀 Gravity Well",
        shortLabel = "Gravity Well",
        badge = "🌀 Well",
        description = "Contracting orbital vortex snaps neon energy to the keycap perimeter",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF00B4D8,
        secondaryHex = 0xFF9D4EDD
    ),
    ECLIPSE_HALO(
        id = "eclipse_halo",
        displayName = "🌘 Eclipse Halo",
        shortLabel = "Eclipse",
        badge = "🌘 Eclipse",
        description = "Solar-corona amber rim glows fiercely around an obsidian key center",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFFF8800,
        secondaryHex = 0xFFFFE600
    ),
    PULSAR_BEACON(
        id = "pulsar_beacon",
        displayName = "📡 Pulsar Beacon",
        shortLabel = "Pulsar",
        badge = "📡 Pulsar",
        description = "Rapid neutron-star cross beams flash cleanly along row and column axes",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFE879F9
    ),
    ORBITAL_DUST(
        id = "orbital_dust",
        displayName = "🪐 Orbital Dust",
        shortLabel = "Orbital Dust",
        badge = "🪐 Orbit",
        description = "Saturn-ring luminous dust sweeps gracefully around typed keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFFFACC15
    ),
    WORMHOLE_TRAIL(
        id = "wormhole_trail",
        displayName = "🕳️ Wormhole Trail",
        shortLabel = "Wormhole",
        badge = "🕳️ Warp",
        description = "Hyper-violet warp streak links rapid consecutive key taps in space",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFBC13FE,
        secondaryHex = 0xFF00F0FF
    ),

    // --- ⚡ CYBER & LASER TOUCH RESPONSES (30..38) ---
    PHOTON_LASER_ARC(
        id = "photon_laser_arc",
        displayName = "⚡ Photon Laser Arc",
        shortLabel = "Laser Arc",
        badge = "⚡ Laser",
        description = "High-energy crimson-cyan laser lines slice across row and column",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF00F0FF
    ),
    CYBERNETIC_SHOCKWAVE(
        id = "cybernetic_shockwave",
        displayName = "🤖 Cybernetic Shockwave",
        shortLabel = "Cyber Wave",
        badge = "🤖 Wave",
        description = "Neon magenta-aqua cyber ring pulses outward across the keycap matrix",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF2E93,
        secondaryHex = 0xFF00E5FF
    ),
    MAGNET_FLUX(
        id = "magnet_flux",
        displayName = "🧲 Magnet Flux",
        shortLabel = "Magnet Flux",
        badge = "🧲 Flux",
        description = "Magnetic cobalt-cyan field snaps inward onto adjacent keys",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF2563EB,
        secondaryHex = 0xFF00F5D4
    ),
    CYBER_PULSE(
        id = "cyber_pulse",
        displayName = "💠 Cyber Pulse",
        shortLabel = "Cyber Pulse",
        badge = "💠 Cyber",
        description = "Crisp electric strobe flickers cleanly over the pressed keycap",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFF00F0FF,
        secondaryHex = 0xFFBC13FE
    ),
    FLUX_WAVE(
        id = "flux_wave",
        displayName = "🌊 Flux Wave",
        shortLabel = "Flux Wave",
        badge = "🌊 Flux",
        description = "Liquid neon capacitor wave rolls outward from every tapped letter",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF00F59B,
        secondaryHex = 0xFF00E5FF
    ),
    VOLT_STRIKE(
        id = "volt_strike",
        displayName = "🌩️ Volt Strike",
        shortLabel = "Volt Strike",
        badge = "🌩️ Volt",
        description = "High-voltage electric yellow-white bolt illuminates the active key column",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFFFE600,
        secondaryHex = 0xFF00E5FF
    ),
    GLITCH_OVERDRIVE(
        id = "glitch_overdrive",
        displayName = "👾 Glitch Overdrive",
        shortLabel = "Glitch",
        badge = "👾 Glitch",
        description = "Chromatic RGB pixel jitter flashes sharply around the pressed keycap",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF39FF14
    ),
    RADAR_SWEEP(
        id = "radar_sweep",
        displayName = "📡 Radar Sweep",
        shortLabel = "Radar Sweep",
        badge = "📡 Radar",
        description = "Emerald tactical sonar ping expands smoothly across neighboring keys",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF39FF14,
        secondaryHex = 0xFF00F5D4
    ),
    HYPER_TRACER(
        id = "hyper_tracer",
        displayName = "🏎️ Hyper Tracer",
        shortLabel = "Tracer",
        badge = "🏎️ Tracer",
        description = "Outrun turbo streak trails fast typing across the QWERTY deck",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFF5E00,
        secondaryHex = 0xFF00F0FF
    ),

    // --- 🔥 PLASMA & CRYO TOUCH RESPONSES (39..47) ---
    SOLAR_FLARE(
        id = "solar_flare",
        displayName = "☀️ Solar Flare",
        shortLabel = "Solar Flare",
        badge = "☀️ Solar",
        description = "Blazing amber corona sparks burst skyward above every tapped key",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF6B00,
        secondaryHex = 0xFFFFE600
    ),
    CRYO_SHARD(
        id = "cryo_shard",
        displayName = "🧊 Cryo Shard",
        shortLabel = "Cryo Shard",
        badge = "🧊 Cryo",
        description = "Sub-zero ice crystals lock frost edges onto surrounding keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFF7DD3FC,
        secondaryHex = 0xFFFFFFFF
    ),
    INFERNO_RING(
        id = "inferno_ring",
        displayName = "🔥 Inferno Ring",
        shortLabel = "Inferno",
        badge = "🔥 Inferno",
        description = "Molten crimson shockwave ring scorches outward from each strike",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFE63946,
        secondaryHex = 0xFFFFB703
    ),
    ARCTIC_GLACIER(
        id = "arctic_glacier",
        displayName = "❄️ Arctic Glacier",
        shortLabel = "Glacier",
        badge = "❄️ Glacier",
        description = "Cool polar cyan prism glaze sweeps cleanly across adjacent keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFFE0F2FE
    ),
    PHOENIX_FEATHER(
        id = "phoenix_feather",
        displayName = "🪶 Phoenix Feather",
        shortLabel = "Phoenix",
        badge = "🪶 Phoenix",
        description = "Golden scarlet ash particles drift gently upward on keypress",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF2E93,
        secondaryHex = 0xFFFFD60A
    ),
    THUNDER_SPARK(
        id = "thunder_spark",
        displayName = "⚡ Thunder Spark",
        shortLabel = "Thunder",
        badge = "⚡ Thunder",
        description = "Crackling ice-blue thunder flashes across row and keycap edges",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF60A5FA,
        secondaryHex = 0xFFFFFFFF
    ),
    AURORA_MIST(
        id = "aurora_mist",
        displayName = "🌌 Aurora Mist",
        shortLabel = "Aurora Mist",
        badge = "🌌 Aurora",
        description = "Polar emerald-violet shimmer lights up tapped keys cleanly",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF00F59B,
        secondaryHex = 0xFFA855F7
    ),
    LAVA_CORE(
        id = "lava_core",
        displayName = "🌋 Lava Core",
        shortLabel = "Lava Core",
        badge = "🌋 Lava",
        description = "Glowing obsidian magma rim pulses warmly around active letters",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFFFF4500,
        secondaryHex = 0xFFFFB703
    ),
    BIO_TOXIC_GLOW(
        id = "bio_toxic_glow",
        displayName = "☣️ Bio Toxic Glow",
        shortLabel = "Bio Glow",
        badge = "☣️ Toxic",
        description = "Acid-lime plasma trail connects swift key taps in neon green",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF84CC16,
        secondaryHex = 0xFF00F59B
    ),

    // --- 💎 PRISM & CHRONO TOUCH RESPONSES (48..56) ---
    HOLOGRAM_PRISM_BURST(
        id = "hologram_prism_burst",
        displayName = "🌈 Hologram Prism Burst",
        shortLabel = "Hologram",
        badge = "🌈 Holo",
        description = "Iridescent hologram facets glint brightly over pressed keycaps",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFFFF2A85
    ),
    CHRONO_WARP(
        id = "chrono_warp",
        displayName = "⏳ Chrono Warp",
        shortLabel = "Chrono Warp",
        badge = "⏳ Chrono",
        description = "Golden-cyan time ring orbits swiftly around the tapped letter",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFFFD60A,
        secondaryHex = 0xFF00E5FF
    ),
    PRISM_SHATTER(
        id = "prism_shatter",
        displayName = "💎 Prism Shatter",
        shortLabel = "Prism Shatter",
        badge = "💎 Prism",
        description = "Crystalline diamond shards refract spectrum light on adjacent keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFE879F9,
        secondaryHex = 0xFF38BDF8
    ),
    ROYAL_CROWN_GLINT(
        id = "royal_crown_glint",
        displayName = "👑 Royal Crown Glint",
        shortLabel = "Royal Glint",
        badge = "👑 Royal",
        description = "24K gold luxury sparkle illuminates every keycap corner",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFFD700,
        secondaryHex = 0xFFFF9E00
    ),
    ROSE_QUARTZ_SHINE(
        id = "rose_quartz_shine",
        displayName = "🌸 Rose Quartz Shine",
        shortLabel = "Rose Quartz",
        badge = "🌸 Rose",
        description = "Soft rose-pink crystal glow blooms outward in gentle ripples",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF75A0,
        secondaryHex = 0xFFF6C2C2
    ),
    EMERALD_FACET(
        id = "emerald_facet",
        displayName = "✳️ Emerald Facet",
        shortLabel = "Emerald",
        badge = "✳️ Emerald",
        description = "Sharp emerald jewel glint cuts diagonally across pressed keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF00F59B,
        secondaryHex = 0xFFFFFFFF
    ),
    SPECTRUM_RIPPLE(
        id = "spectrum_ripple",
        displayName = "🎨 Spectrum Ripple",
        shortLabel = "Spectrum",
        badge = "🎨 Spectrum",
        description = "Full rainbow spectrum wave expands cleanly on every touch",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF2E93,
        secondaryHex = 0xFF00E5FF
    ),
    TEMPORAL_ECHO(
        id = "temporal_echo",
        displayName = "🕰️ Temporal Echo",
        shortLabel = "Temporal",
        badge = "🕰️ Echo",
        description = "Past 4 keys stay connected in a fading golden-violet time ribbon",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFFB703,
        secondaryHex = 0xFFA855F7
    ),
    CRYSTAL_ORBIT(
        id = "crystal_orbit",
        displayName = "🔮 Crystal Orbit",
        shortLabel = "Crystal Orbit",
        badge = "🔮 Crystal",
        description = "Violet-aqua crystal ring orbits smoothly around each pressed key",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFC084FC,
        secondaryHex = 0xFF22D3EE
    ),

    // --- ✨ 50 EXPANDED PRO AI & PHYSICS TOUCH ENGINES (57..106) ---
    NEURAL_SPARK_CORE(
        id = "neural_spark_core",
        displayName = "🧠 Neural Spark Core",
        shortLabel = "Spark Core",
        badge = "🧠 Core",
        description = "High-speed cyan neural pulse with electric magenta feedback node",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFFFF2A85
    ),
    CORTEX_BEAM(
        id = "cortex_beam",
        displayName = "🔮 Cortex Beam",
        shortLabel = "Cortex Beam",
        badge = "🔮 Cortex",
        description = "Focused violet-teal synapse beams link adjacent typed keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF7B2FFF,
        secondaryHex = 0xFF00F5D4
    ),
    SYNAPTIC_GLITCH(
        id = "synaptic_glitch",
        displayName = "⚡ Synaptic Glitch",
        shortLabel = "Syn Glitch",
        badge = "⚡ Glitch",
        description = "Cyberpunk neural micro-flicker sparks around pressed keycaps",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF00FFAA
    ),
    MINDWAVE_HALO(
        id = "mindwave_halo",
        displayName = "🌊 Mindwave Halo",
        shortLabel = "Mindwave",
        badge = "🌊 Wave",
        description = "Expanding electric sapphire ring syncs across your finger taps",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFF818CF8
    ),
    DEEP_MATRIX_TAP(
        id = "deep_matrix_tap",
        displayName = "🟢 Deep Matrix Tap",
        shortLabel = "Deep Matrix",
        badge = "🟢 Matrix",
        description = "Digital emerald phosphor burst ripples across neighboring code keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF00FF88,
        secondaryHex = 0xFF00D4FF
    ),
    QUANTUM_SPARK_X(
        id = "quantum_spark_x",
        displayName = "💫 Quantum Spark X",
        shortLabel = "Quantum X",
        badge = "💫 Quark",
        description = "Twin aqua-violet shock rings expand cleanly with soft afterglow",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFFC084FC
    ),
    PHOTON_SYNAPSE(
        id = "photon_synapse",
        displayName = "✨ Photon Synapse",
        shortLabel = "Photon Syn",
        badge = "✨ Photon",
        description = "Ultra-crisp white-gold neural flare flashes at peak key travel",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFFD166,
        secondaryHex = 0xFF00E5FF
    ),
    ALPHA_WAVE_PULSE(
        id = "alpha_wave_pulse",
        displayName = "💠 Alpha Wave Pulse",
        shortLabel = "Alpha Pulse",
        badge = "💠 Alpha",
        description = "Precision orbital shimmer activates smoothly on every letter stroke",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF22D3EE,
        secondaryHex = 0xFF6366F1
    ),
    BRAIN_BRIDGE(
        id = "brain_bridge",
        displayName = "🔗 Brain Bridge",
        shortLabel = "Brain Bridge",
        badge = "🔗 Link",
        description = "Luminous neon comet filaments connect consecutive fast keystrokes",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF00FFAA,
        secondaryHex = 0xFF7B2FFF
    ),
    ECHO_NODE_MINT(
        id = "echo_node_mint",
        displayName = "🍃 Echo Node Mint",
        shortLabel = "Echo Mint",
        badge = "🍃 Mint",
        description = "Silky mint-ice neural ring fades effortlessly without visual clutter",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF34D399,
        secondaryHex = 0xFF67E8F9
    ),

    SUPERNOVA_BURST(
        id = "supernova_burst",
        displayName = "🌟 Supernova Burst",
        shortLabel = "Supernova",
        badge = "🌟 Nova",
        description = "Golden-cyan stellar shockwave illuminates nearby keycaps",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFFB703,
        secondaryHex = 0xFF00E5FF
    ),
    VOID_GRAVITY(
        id = "void_gravity",
        displayName = "🕳️ Void Gravity",
        shortLabel = "Void Pull",
        badge = "🕳️ Void",
        description = "Deep indigo gravitational implosion pulls neon light inward",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF8B5CF6,
        secondaryHex = 0xFFEC4899
    ),
    METEOR_DASH(
        id = "meteor_dash",
        displayName = "☄️ Meteor Dash",
        shortLabel = "Meteor Dash",
        badge = "☄️ Meteor",
        description = "Blazing amber-rose meteor trail streaks across swipe and tap keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFF6B00,
        secondaryHex = 0xFFFF2A85
    ),
    ORBITAL_ECLIPSE(
        id = "orbital_eclipse",
        displayName = "🌑 Orbital Eclipse",
        shortLabel = "Eclipse",
        badge = "🌑 Eclipse",
        description = "Solar corona gold ring orbits around sleek shadowed key edges",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFFFD600,
        secondaryHex = 0xFF7B2FFF
    ),
    NEBULA_RIPPLE(
        id = "nebula_ripple",
        displayName = "🌌 Nebula Ripple",
        shortLabel = "Nebula Wave",
        badge = "🌌 Nebula",
        description = "Violet-cyan cosmic gas ripple diffuses smoothly from tap center",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFF06B6D4
    ),
    STARLIGHT_TRACE(
        id = "starlight_trace",
        displayName = "🌠 Starlight Trace",
        shortLabel = "Starlight",
        badge = "🌠 Star",
        description = "Silver-cyan starlight comet thread follows high-speed typing bursts",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFE0F2FE,
        secondaryHex = 0xFF38BDF8
    ),
    COSMIC_SINGULARITY(
        id = "cosmic_singularity",
        displayName = "🌀 Cosmic Singularity",
        shortLabel = "Singularity",
        badge = "🌀 Vortex",
        description = "Concentrated magenta-cyan gravity well snaps crisply on touch",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF00F5D4
    ),
    PULSAR_COSMOS(
        id = "pulsar_cosmos",
        displayName = "📡 Pulsar Cosmos",
        shortLabel = "Pulsar Nova",
        badge = "📡 Pulsar",
        description = "Twin rhythmic aqua shockwaves pulse cleanly like a deep-space star",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFF3B82F6
    ),
    ASTRAL_DRIFT(
        id = "astral_drift",
        displayName = "✨ Astral Drift",
        shortLabel = "Astral Drift",
        badge = "✨ Astral",
        description = "Weightless aurora dust sparkles float gently around tapped keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFF5EEAD4,
        secondaryHex = 0xFFC084FC
    ),
    QUASAR_RING(
        id = "quasar_ring",
        displayName = "🪐 Quasar Ring",
        shortLabel = "Quasar",
        badge = "🪐 Ring",
        description = "Planetary neon hoop tilts and glows along each key bevel",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFF472B6,
        secondaryHex = 0xFF22D3EE
    ),

    LASER_GRID_X(
        id = "laser_grid_x",
        displayName = "⚡ Laser Grid X",
        shortLabel = "Laser Grid",
        badge = "⚡ Grid",
        description = "Sharp crimson-cyan laser crosshairs lock onto each typed letter",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF00E5FF
    ),
    OVERCLOCK_GLITCH(
        id = "overclock_glitch",
        displayName = "💥 Overclock Glitch",
        shortLabel = "Overclock",
        badge = "💥 Turbo",
        description = "Esports RGB micro-glitch slices across keycaps under rapid strokes",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFF00FF66,
        secondaryHex = 0xFFFF00E5
    ),
    VOLT_SABER(
        id = "volt_saber",
        displayName = "⚔️ Volt Saber",
        shortLabel = "Volt Saber",
        badge = "⚔️ Saber",
        description = "High-voltage sapphire plasma cross-beam slices row & column",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF2979FF,
        secondaryHex = 0xFF00F5D4
    ),
    HYPER_SCAN_PRO(
        id = "hyper_scan_pro",
        displayName = "📡 Hyper Scan Pro",
        shortLabel = "Hyper Scan",
        badge = "📡 Scan",
        description = "Dual telemetry laser streaks sweep smoothly across active keys",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFF00FF88
    ),
    NEON_KATANA(
        id = "neon_katana",
        displayName = "🗡️ Neon Katana",
        shortLabel = "Katana",
        badge = "🗡️ Slash",
        description = "Razor-sharp neon magenta shock-slash ignites pressed key borders",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFF007F,
        secondaryHex = 0xFF00F0FF
    ),
    TURBO_ARC(
        id = "turbo_arc",
        displayName = "🏎️ Turbo Arc",
        shortLabel = "Turbo Arc",
        badge = "🏎️ Nitro",
        description = "Nitromethane cyan-amber spark ring bursts at racing speed",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF00D4FF,
        secondaryHex = 0xFFFF9E00
    ),
    CYBER_CIRCUIT_TAP(
        id = "cyber_circuit_tap",
        displayName = "🔌 Cyber Circuit",
        shortLabel = "Circuit",
        badge = "🔌 PCB",
        description = "Golden motherboard trace signal lights adjacent PCB nodes",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFFFFD600,
        secondaryHex = 0xFF00FF88
    ),
    RAILGUN_PULSE(
        id = "railgun_pulse",
        displayName = "🔫 Railgun Pulse",
        shortLabel = "Railgun",
        badge = "🔫 Rail",
        description = "Electromagnetic cobalt shock ring discharges radially outward",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF60A5FA,
        secondaryHex = 0xFFF43F5E
    ),
    RGB_MATRIX_SPIKE(
        id = "rgb_matrix_spike",
        displayName = "🟩 Matrix Spike",
        shortLabel = "Matrix Spike",
        badge = "🟩 Code",
        description = "Terminal neon green pulse flickers cleanly across key legends",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFFA3E635
    ),
    MECHA_BEAM_GOLD(
        id = "mecha_beam_gold",
        displayName = "🛡️ Mecha Beam Gold",
        shortLabel = "Mecha Gold",
        badge = "🛡️ Gold",
        description = "Titanium gold cross-laser highlights precision mechanical presses",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFFFB703,
        secondaryHex = 0xFFFFFFFF
    ),

    SOLAR_FLARE_EMBER(
        id = "solar_flare_ember",
        displayName = "☀️ Solar Flare",
        shortLabel = "Solar Flare",
        badge = "☀️ Solar",
        description = "Sun-core gold embers drift upward from every warm keystroke",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF9E00,
        secondaryHex = 0xFFFF3D00
    ),
    ARCTIC_DIAMOND_ICE(
        id = "arctic_diamond_ice",
        displayName = "🧊 Arctic Diamond",
        shortLabel = "Arctic Ice",
        badge = "🧊 Ice",
        description = "Sub-zero sapphire ice crystals shimmer over frosted keycaps",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFBAE6FD,
        secondaryHex = 0xFF0284C7
    ),
    VOLCANIC_LAVA_POP(
        id = "volcanic_lava_pop",
        displayName = "🌋 Volcanic Lava",
        shortLabel = "Lava Pop",
        badge = "🌋 Lava",
        description = "Molten obsidian crimson sparks rise warmly on each tap",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFEF4444,
        secondaryHex = 0xFFF59E0B
    ),
    BIO_TOXIC_PLASMA(
        id = "bio_toxic_plasma",
        displayName = "☣️ Bio Plasma",
        shortLabel = "Bio Plasma",
        badge = "☣️ Acid",
        description = "Radioactive lime-emerald plasma shockwave pulses softly outward",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFA3E635,
        secondaryHex = 0xFF00E676
    ),
    GLACIER_CRACK(
        id = "glacier_crack",
        displayName = "❄️ Glacier Crack",
        shortLabel = "Glacier",
        badge = "❄️ Frost",
        description = "Crisp white-mint frost fractures flash brightly along key outlines",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFE0FBFC,
        secondaryHex = 0xFF2DD4BF
    ),
    DRAGON_BREATH_SPARK(
        id = "dragon_breath_spark",
        displayName = "🐉 Dragon Breath",
        shortLabel = "Dragon Fire",
        badge = "🐉 Flame",
        description = "Fiery crimson-gold phoenix embers swirl upward during fast typing",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF2A55,
        secondaryHex = 0xFFFFB703
    ),
    THUNDER_BOLT_CLOUD(
        id = "thunder_bolt_cloud",
        displayName = "🌩️ Thunder Cloud",
        shortLabel = "Thunder",
        badge = "🌩️ Storm",
        description = "Electric storm-blue lightning arcs crackle across row & column",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFFFFFFF
    ),
    AURORA_VEIL(
        id = "aurora_veil",
        displayName = "🌌 Aurora Veil",
        shortLabel = "Aurora Veil",
        badge = "🌌 Mist",
        description = "Polar teal-violet aurora glow washes softly over pressed keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFF8B5CF6
    ),
    PHOENIX_ASH(
        id = "phoenix_ash",
        displayName = "🔥 Phoenix Ash",
        shortLabel = "Phoenix",
        badge = "🔥 Ember",
        description = "Rose-amber glowing embers float upward with silky fadeout",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFB7185,
        secondaryHex = 0xFFFBBF24
    ),
    OCEAN_HYDRO_WAVE(
        id = "ocean_hydro_wave",
        displayName = "🌊 Hydro Wave",
        shortLabel = "Hydro Wave",
        badge = "🌊 Hydro",
        description = "Deep aqua water ripple expands cleanly from fingertip impact",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF06B6D4,
        secondaryHex = 0xFF3B82F6
    ),

    PRISM_KALEIDOSCOPE(
        id = "prism_kaleidoscope",
        displayName = "🌈 Prism Spectrum",
        shortLabel = "Prism Spec",
        badge = "🌈 Prism",
        description = "Faceted rainbow crystal glint refracts cleanly around key edges",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF00E5FF
    ),
    ROYAL_CROWN_AURA(
        id = "royal_crown_aura",
        displayName = "👑 Royal Aura",
        shortLabel = "Royal Aura",
        badge = "👑 Royal",
        description = "Luxury 24K gold diamond facet sparkles smoothly on tap",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFFD166,
        secondaryHex = 0xFFFF9E00
    ),
    CHRONO_GOLD_DIAL(
        id = "chrono_gold_dial",
        displayName = "⏱️ Chrono Gold",
        shortLabel = "Chrono Gold",
        badge = "⏱️ Dial",
        description = "Precision Swiss watch-hand gold orbit sweeps around the active key",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFFBBF24,
        secondaryHex = 0xFF22D3EE
    ),
    EMERALD_PRISM(
        id = "emerald_prism",
        displayName = "💚 Emerald Prism",
        shortLabel = "Emerald Prism",
        badge = "💚 Gem",
        description = "Precious emerald crystal edges refract glowing mint light",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF00FF88,
        secondaryHex = 0xFF10B981
    ),
    RUBY_SHARD(
        id = "ruby_shard",
        displayName = "♦️ Ruby Shard",
        shortLabel = "Ruby Shard",
        badge = "♦️ Ruby",
        description = "Crimson ruby prism shards gleam across luxury dark keycaps",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFF43F5E,
        secondaryHex = 0xFFFFB703
    ),
    SAPPHIRE_HALO(
        id = "sapphire_halo",
        displayName = "🔷 Sapphire Halo",
        shortLabel = "Sapphire",
        badge = "🔷 Gem",
        description = "Royal blue orbital gem halo spins around each tapped letter",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF3B82F6,
        secondaryHex = 0xFF00F5D4
    ),
    ROSE_QUARTZ_PULSE(
        id = "rose_quartz_pulse",
        displayName = "🌸 Rose Quartz",
        shortLabel = "Rose Quartz",
        badge = "🌸 Rose",
        description = "Delicate rose-gold crystal ripple blooms smoothly on every tap",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFF9A8D4,
        secondaryHex = 0xFFFDE047
    ),
    AMETHYST_ORBIT(
        id = "amethyst_orbit",
        displayName = "🍇 Amethyst Orbit",
        shortLabel = "Amethyst",
        badge = "🍇 Violet",
        description = "Lustrous royal purple crystal ring glides around pressed keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFFF472B6
    ),
    PLATINUM_BEVEL(
        id = "platinum_bevel",
        displayName = "🪙 Platinum Bevel",
        shortLabel = "Platinum",
        badge = "🪙 Silver",
        description = "Minimalist brushed-silver diamond glint highlights clean keycaps",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFE2E8F0,
        secondaryHex = 0xFF38BDF8
    ),
    INFINITE_MIRROR(
        id = "infinite_mirror",
        displayName = "🪞 Infinite Mirror",
        shortLabel = "Mirror FX",
        badge = "🪞 Mirror",
        description = "Twin neon cyan-gold orbital reflections echo rapid keystrokes",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFFFFD166
    ),

    // --- 200+ SMOOTH & REFINED TOUCH RESPONSES EXPANSION (107..208) ---
    // AI & Neural Smooth Collection (107..126)
    SERENE_SYNAPSE(
        id = "serene_synapse",
        displayName = "🌊 Serene Synapse",
        shortLabel = "Serene Wave",
        badge = "🌊 Calm",
        description = "Silky teal-sky neural wave glides smoothly across neighboring keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF2DD4BF,
        secondaryHex = 0xFF38BDF8
    ),
    LUMEN_WHISPER(
        id = "lumen_whisper",
        displayName = "✨ Lumen Whisper",
        shortLabel = "Lumen Glow",
        badge = "✨ Soft",
        description = "Gentle warm pearl glow blooms effortlessly on every keystroke",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFDE68A,
        secondaryHex = 0xFF38BDF8
    ),
    VELVET_PULSE(
        id = "velvet_pulse",
        displayName = "🪻 Velvet Pulse",
        shortLabel = "Velvet Pulse",
        badge = "🪻 Smooth",
        description = "Soft lavender-rose neural glow ripples delicately around pressed keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFFC084FC,
        secondaryHex = 0xFFF472B6
    ),
    CYAN_DREAM_FLOW(
        id = "cyan_dream_flow",
        displayName = "💠 Cyan Dream Flow",
        shortLabel = "Cyan Flow",
        badge = "💠 Flow",
        description = "Liquid aqua comet trace connects rapid typing with silky smoothness",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFF00FFAA
    ),
    MIND_HARMONY(
        id = "mind_harmony",
        displayName = "🔮 Mind Harmony",
        shortLabel = "Harmony",
        badge = "🔮 Zen",
        description = "Balanced indigo-mint halo orbits gently on each tap",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF818CF8,
        secondaryHex = 0xFF34D399
    ),
    SILK_ECHO(
        id = "silk_echo",
        displayName = "🫧 Silk Echo",
        shortLabel = "Silk Echo",
        badge = "🫧 Ripple",
        description = "Clean pastel cyan ripple expands smoothly with zero harsh glare",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF67E8F9,
        secondaryHex = 0xFFA5B4FC
    ),
    IRIS_SYNAPSE(
        id = "iris_synapse",
        displayName = "👁️ Iris Synapse",
        shortLabel = "Iris Glow",
        badge = "👁️ Neural",
        description = "Electric violet-cobalt neural ring illuminates adjacent keys softly",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF6366F1,
        secondaryHex = 0xFF22D3EE
    ),
    AZURE_SPARKLET(
        id = "azure_sparklet",
        displayName = "💎 Azure Sparklet",
        shortLabel = "Azure Spark",
        badge = "💎 Azure",
        description = "Crisp sky-blue spark softens into a serene frosted key border",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFE0F2FE
    ),
    NEURAL_BREEZE(
        id = "neural_breeze",
        displayName = "🍃 Neural Breeze",
        shortLabel = "Breeze FX",
        badge = "🍃 Mint",
        description = "Fresh mint-emerald touch halo drifts softly across pressed keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFF6EE7B7
    ),
    TWILIGHT_GLIDE(
        id = "twilight_glide",
        displayName = "🌆 Twilight Glide",
        shortLabel = "Twilight",
        badge = "🌆 Glide",
        description = "Dusk rose-indigo trail connects letters in fluid continuous motion",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFB7185,
        secondaryHex = 0xFF6366F1
    ),
    OPAL_STREAM(
        id = "opal_stream",
        displayName = "🦪 Opal Stream",
        shortLabel = "Opal Glow",
        badge = "🦪 Opal",
        description = "Iridescent pearl-aqua sheen shimmers calmly on keypress",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFFE0E7FF,
        secondaryHex = 0xFF5EEAD4
    ),
    ZEN_LOTUS_RING(
        id = "zen_lotus_ring",
        displayName = "🪷 Zen Lotus Ring",
        shortLabel = "Zen Lotus",
        badge = "🪷 Calm",
        description = "Blossom-pink lotus shockwave blooms evenly across the keybed",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFF472B6,
        secondaryHex = 0xFFFDE047
    ),
    ORCHID_SYNAPSE(
        id = "orchid_synapse",
        displayName = "🌸 Orchid Synapse",
        shortLabel = "Orchid FX",
        badge = "🌸 Soft",
        description = "Magenta-lavender synaptic glow radiates softly from tapped keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFFE879F9,
        secondaryHex = 0xFF818CF8
    ),
    HORIZON_COMET(
        id = "horizon_comet",
        displayName = "🌅 Horizon Comet",
        shortLabel = "Horizon",
        badge = "🌅 Trail",
        description = "Golden-cyan horizon ribbon traces every word you type",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFBBF24,
        secondaryHex = 0xFF22D3EE
    ),
    MOONSTONE_TAP(
        id = "moonstone_tap",
        displayName = "🌙 Moonstone Tap",
        shortLabel = "Moonstone",
        badge = "🌙 Glow",
        description = "Cool silver-ice ring gently orbits tapped keys without glare",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFCBD5E1,
        secondaryHex = 0xFF7DD3FC
    ),
    COBALT_RIPPLE(
        id = "cobalt_ripple",
        displayName = "🔵 Cobalt Ripple",
        shortLabel = "Cobalt",
        badge = "🔵 Wave",
        description = "Deep royal cobalt ring expands smoothly across matte keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF3B82F6,
        secondaryHex = 0xFF00E5FF
    ),
    MINT_VELVET_TAP(
        id = "mint_velvet_tap",
        displayName = "🧊 Mint Velvet",
        shortLabel = "Mint Velvet",
        badge = "🧊 Crisp",
        description = "Frosted seafoam-cyan glow hugs key edges smoothly",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFF5EEAD4,
        secondaryHex = 0xFF0EA5E9
    ),
    AMBER_SYNAPSE_FLOW(
        id = "amber_synapse_flow",
        displayName = "🍯 Amber Synapse",
        shortLabel = "Amber Flow",
        badge = "🍯 Warm",
        description = "Warm honey-amber glow flows gracefully between neighboring letters",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFFF59E0B,
        secondaryHex = 0xFFFDE68A
    ),
    NEON_MIST_PULSE(
        id = "neon_mist_pulse",
        displayName = "🌫️ Neon Mist",
        shortLabel = "Neon Mist",
        badge = "🌫️ Mist",
        description = "Soft dual-tone cyan-violet mist settles calmly on tapped keys",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFF22D3EE,
        secondaryHex = 0xFFA855F7
    ),
    JADE_HARMONIC(
        id = "jade_harmonic",
        displayName = "💚 Jade Harmonic",
        shortLabel = "Jade Wave",
        badge = "💚 Jade",
        description = "Harmonious emerald-teal orbital ring glides smoothly on keypress",
        category = TouchEffectCategory.AI_NEURAL,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF00FF88,
        secondaryHex = 0xFF14B8A6
    ),

    // Quantum & Space Smooth Collection (127..146)
    NEBULA_SILK(
        id = "nebula_silk",
        displayName = "🌌 Nebula Silk",
        shortLabel = "Nebula Silk",
        badge = "🌌 Silk",
        description = "Deep cosmic plum-cyan nebula ring expands across dark keycaps",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFF00E5FF
    ),
    STARDUST_COMET(
        id = "stardust_comet",
        displayName = "🌠 Stardust Comet",
        shortLabel = "Stardust",
        badge = "🌠 Star",
        description = "Silvery-gold stardust streak links your keystrokes cleanly",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFDE68A,
        secondaryHex = 0xFF60A5FA
    ),
    ECLIPSE_ORBIT_PRO(
        id = "eclipse_orbit_pro",
        displayName = "🌒 Eclipse Orbit",
        shortLabel = "Eclipse Pro",
        badge = "🌒 Orbit",
        description = "Solar gold halo circles gently around dark lunar keycaps",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFFFB703,
        secondaryHex = 0xFF818CF8
    ),
    GRAVITY_WELL_SOFT(
        id = "gravity_well_soft",
        displayName = "🕳️ Soft Singularity",
        shortLabel = "Soft Pull",
        badge = "🕳️ Space",
        description = "Velvet indigo implosion ring draws inward smoothly on tap",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF6366F1,
        secondaryHex = 0xFF00F5D4
    ),
    CELESTIAL_RIPPLE(
        id = "celestial_ripple",
        displayName = "✨ Celestial Ripple",
        shortLabel = "Celestial",
        badge = "✨ Sky",
        description = "Starlight sky-blue ripple glides effortlessly over midnight keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF7DD3FC,
        secondaryHex = 0xFFF472B6
    ),
    GALAXY_RIBBON(
        id = "galaxy_ribbon",
        displayName = "🪐 Galaxy Ribbon",
        shortLabel = "Galaxy",
        badge = "🪐 Ribbon",
        description = "Vibrant violet-aqua galaxy ribbon flows smoothly while typing",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFC026D3,
        secondaryHex = 0xFF22D3EE
    ),
    SOLAR_RING_GOLD(
        id = "solar_ring_gold",
        displayName = "☀️ Solar Ring",
        shortLabel = "Solar Ring",
        badge = "☀️ Gold",
        description = "Warm champagne-gold ring radiates smoothly from tapped keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFBBF24,
        secondaryHex = 0xFFFF7A00
    ),
    LUNAR_FROST_HALO(
        id = "lunar_frost_halo",
        displayName = "🌕 Lunar Frost",
        shortLabel = "Lunar Ice",
        badge = "🌕 Frost",
        description = "Moonlit icy crystal glow softens across adjacent keycaps",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFE2E8F0,
        secondaryHex = 0xFF38BDF8
    ),
    ANDROMEDA_GLOW(
        id = "andromeda_glow",
        displayName = "🌠 Andromeda Glow",
        shortLabel = "Andromeda",
        badge = "🌠 Glow",
        description = "Cosmic magenta-cyan orbital spark glides gently on each stroke",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFEC4899,
        secondaryHex = 0xFF00E5FF
    ),
    AETHER_IMPLODE(
        id = "aether_implode",
        displayName = "🌀 Aether Vortex",
        shortLabel = "Aether",
        badge = "🌀 Vortex",
        description = "Smooth aqua-violet gravity ring converges softly on keypress",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF2DD4BF,
        secondaryHex = 0xFF8B5CF6
    ),
    METEOR_BREEZE(
        id = "meteor_breeze",
        displayName = "☄️ Meteor Breeze",
        shortLabel = "Meteor",
        badge = "☄️ Smooth",
        description = "Soft tangerine-violet meteor arc follows fast typing seamlessly",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFB923C,
        secondaryHex = 0xFFA855F7
    ),
    ORION_SPARK(
        id = "orion_spark",
        displayName = "⭐ Orion Spark",
        shortLabel = "Orion",
        badge = "⭐ Star",
        description = "Refined diamond starlight glint twinkles softly on key edges",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF93C5FD,
        secondaryHex = 0xFFFDE047
    ),
    COSMIC_TIDE(
        id = "cosmic_tide",
        displayName = "🌊 Cosmic Tide",
        shortLabel = "Cosmic Tide",
        badge = "🌊 Tide",
        description = "Oceanic star-teal wave ripples cleanly across the whole deck",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFF2563EB
    ),
    VOID_PEARL(
        id = "void_pearl",
        displayName = "🖤 Void Pearl",
        shortLabel = "Void Pearl",
        badge = "🖤 Stealth",
        description = "Ultra-clean stealth pearl-cyan implosion ring for dark keyboards",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFFE2E8F0,
        secondaryHex = 0xFF0EA5E9
    ),
    SATURN_ORBIT(
        id = "saturn_orbit",
        displayName = "🪐 Saturn Orbit",
        shortLabel = "Saturn",
        badge = "🪐 Ring",
        description = "Warm amber-rose planetary ring revolves around tapped keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFF59E0B,
        secondaryHex = 0xFFF472B6
    ),
    NOVA_BLOSSOM(
        id = "nova_blossom",
        displayName = "🌸 Nova Blossom",
        shortLabel = "Nova Pink",
        badge = "🌸 Nova",
        description = "Rose-gold stellar shockwave blooms with silky soft edges",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF85B3,
        secondaryHex = 0xFFFFD166
    ),
    QUASAR_TAIL(
        id = "quasar_tail",
        displayName = "💫 Quasar Tail",
        shortLabel = "Quasar",
        badge = "💫 Tail",
        description = "Bright turquoise-lime comet ribbon links rapid key presses",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFFA3E635
    ),
    SUPERNOVA_SOFT(
        id = "supernova_soft",
        displayName = "🌟 Supernova Silk",
        shortLabel = "Supernova",
        badge = "🌟 Silk",
        description = "Radiant coral-gold expansion wave softens gently after impact",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF6B6B,
        secondaryHex = 0xFFFBBF24
    ),
    ASTRO_SYNAPSE(
        id = "astro_synapse",
        displayName = "🛰️ Astro Synapse",
        shortLabel = "Astro",
        badge = "🛰️ Orbit",
        description = "Electric sky-indigo neural cluster lights nearby keys evenly",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFF6366F1
    ),
    STARFALL_GLOW(
        id = "starfall_glow",
        displayName = "✨ Starfall Glow",
        shortLabel = "Starfall",
        badge = "✨ Fall",
        description = "Soft twilight gold-violet sparks drift calmly across tapped keys",
        category = TouchEffectCategory.QUANTUM_SPACE,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFDE047,
        secondaryHex = 0xFFA855F7
    ),

    // Cyber & Laser Smooth Collection (147..166)
    CYBER_SILK_BEAM(
        id = "cyber_silk_beam",
        displayName = "⚡ Cyber Silk Cross",
        shortLabel = "Silk Cross",
        badge = "⚡ Beam",
        description = "Refined cyan laser crosshairs illuminate row and column smoothly",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFFBAE6FD
    ),
    LASER_MINT_AXIS(
        id = "laser_mint_axis",
        displayName = "🔋 Mint Laser Axis",
        shortLabel = "Mint Axis",
        badge = "🔋 Axis",
        description = "Emerald-mint crosshair beam glides softly along pressed row/column",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF00FF88,
        secondaryHex = 0xFF2DD4BF
    ),
    VIOLET_TRACE_LINE(
        id = "violet_trace_line",
        displayName = "🟣 Violet Tracer",
        shortLabel = "Violet Trace",
        badge = "🟣 Trace",
        description = "Smooth neon orchid comet ribbon follows swift cyber typing",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFD946EF,
        secondaryHex = 0xFF38BDF8
    ),
    SYNTH_WAVE_RING(
        id = "synth_wave_ring",
        displayName = "🌆 Synthwave Ring",
        shortLabel = "Synth Ring",
        badge = "🌆 Synth",
        description = "Outrun pink-cyan neon shockwave pulses smoothly across keycaps",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF2A85,
        secondaryHex = 0xFF00F0FF
    ),
    STEALTH_TRACER(
        id = "stealth_tracer",
        displayName = "🕶️ Stealth Tracer",
        shortLabel = "Stealth FX",
        badge = "🕶️ Clean",
        description = "Low-glare titanium-cyan glow accents only the active pressed key",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFF94A3B8
    ),
    PHOTON_CROSS_GOLD(
        id = "photon_cross_gold",
        displayName = "✨ Photon Gold Cross",
        shortLabel = "Gold Cross",
        badge = "✨ Gold",
        description = "Luxury 24K gold laser axis highlights row & column cleanly",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFFFD166,
        secondaryHex = 0xFFFF9E00
    ),
    NEON_KATANA_BLUE(
        id = "neon_katana_blue",
        displayName = "🗡️ Katana Azure",
        shortLabel = "Katana Blue",
        badge = "🗡️ Blade",
        description = "Sharp cobalt-ice trail slices cleanly across rapid keystrokes",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF00D4FF,
        secondaryHex = 0xFF2563EB
    ),
    CIRCUIT_PULSE_TEAL(
        id = "circuit_pulse_teal",
        displayName = "🔌 Circuit Teal",
        shortLabel = "Circuit Teal",
        badge = "🔌 PCB",
        description = "Microchip teal synapse arcs smoothly across neighboring key nodes",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF14B8A6,
        secondaryHex = 0xFF67E8F9
    ),
    ARC_REACTOR_HALO(
        id = "arc_reactor_halo",
        displayName = "💠 Arc Reactor",
        shortLabel = "Arc Halo",
        badge = "💠 Core",
        description = "Clean glowing ice-blue energy orbit spins steadily around taps",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFE0FBFC
    ),
    MAGENTA_GRID_CROSS(
        id = "magenta_grid_cross",
        displayName = "🟪 Magenta Axis",
        shortLabel = "Pink Axis",
        badge = "🟪 Laser",
        description = "Soft hot-pink cyber axis illuminates row & column cleanly",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFEC4899,
        secondaryHex = 0xFFF9A8D4
    ),
    HYPER_FLOW_LIME(
        id = "hyper_flow_lime",
        displayName = "⚡ Hyper Lime Flow",
        shortLabel = "Hyper Lime",
        badge = "⚡ Lime",
        description = "Clean neon lime-cyan glow pulses evenly on every key tap",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFF4ADE80,
        secondaryHex = 0xFF00E5FF
    ),
    CYBER_PRISM_EDGE(
        id = "cyber_prism_edge",
        displayName = "🔷 Cyber Prism",
        shortLabel = "Cyber Prism",
        badge = "🔷 Edge",
        description = "Crisp holographic cyan glint sparkles smoothly along beveled keys",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF00F0FF,
        secondaryHex = 0xFFA855F7
    ),
    TURBO_VECTOR_TRAIL(
        id = "turbo_vector_trail",
        displayName = "🏎️ Turbo Vector",
        shortLabel = "Turbo Line",
        badge = "🏎️ Fast",
        description = "Sleek crimson-cyan vector tail links fast typing smoothly",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFF43F5E,
        secondaryHex = 0xFF00E5FF
    ),
    QUANTUM_CROSS_VIOLET(
        id = "quantum_cross_violet",
        displayName = "💜 Violet Cross",
        shortLabel = "Violet Cross",
        badge = "💜 Axis",
        description = "Amethyst-violet laser beams cross cleanly at the tapped key",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFF00F5D4
    ),
    CYBER_WAVE_CLEAN(
        id = "cyber_wave_clean",
        displayName = "📡 Cyber Wave",
        shortLabel = "Cyber Wave",
        badge = "📡 Wave",
        description = "Radar-clean aqua shockwave sweeps outward with balanced fade",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFF38BDF8
    ),
    MATRIX_SILK_TAP(
        id = "matrix_silk_tap",
        displayName = "🟩 Matrix Silk",
        shortLabel = "Matrix Silk",
        badge = "🟩 Code",
        description = "Smooth emerald digital pulse highlights keys without harsh flicker",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFFA7F3D0
    ),
    CHRONO_CYBER_RING(
        id = "chrono_cyber_ring",
        displayName = "⏱️ Cyber Chrono",
        shortLabel = "Cyber Ring",
        badge = "⏱️ Tech",
        description = "Precision electric blue orbital dot tracks your typing cadence",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF0EA5E9,
        secondaryHex = 0xFF00FFAA
    ),
    NEON_SHIELD_IMPLODE(
        id = "neon_shield_implode",
        displayName = "🛡️ Neon Shield",
        shortLabel = "Neon Shield",
        badge = "🛡️ Focus",
        description = "Focused cyan-amber energy ring converges cleanly onto pressed keys",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFF22D3EE,
        secondaryHex = 0xFFFBBF24
    ),
    TITANIUM_BEAM(
        id = "titanium_beam",
        displayName = "🩶 Titanium Beam",
        shortLabel = "Titanium",
        badge = "🩶 Pro",
        description = "Brushed silver-cyan laser axis pairs cleanly with dark keycaps",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFFCBD5E1,
        secondaryHex = 0xFF38BDF8
    ),
    VAPOR_SYNTH_TAIL(
        id = "vapor_synth_tail",
        displayName = "🌴 Vaporwave Tail",
        shortLabel = "Vapor Tail",
        badge = "🌴 Retro",
        description = "Pastel sunset pink-teal vapor trail flows smoothly across keys",
        category = TouchEffectCategory.CYBER_LASER,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFFFF7EB3,
        secondaryHex = 0xFF2DD4BF
    ),

    // Plasma, Fire/Flame & Elemental Smooth Collection (167..188)
    CRIMSON_FLAME_DRIFT(
        id = "crimson_flame_drift",
        displayName = "🔥 Crimson Flame",
        shortLabel = "Crimson Fire",
        badge = "🔥 Flame",
        description = "Smooth ruby-amber flames drift upward gracefully on every keypress",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF2A55,
        secondaryHex = 0xFFFFB703
    ),
    BLUE_TORCH_FLAME(
        id = "blue_torch_flame",
        displayName = "🩵 Blue Plasma Flame",
        shortLabel = "Blue Flame",
        badge = "🩵 Fire",
        description = "Clean cobalt-cyan plasma flames rise smoothly above tapped letters",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFF00E5FF,
        secondaryHex = 0xFF3B82F6
    ),
    VIOLET_EMBER_FLAME(
        id = "violet_ember_flame",
        displayName = "🟣 Violet Mystic Fire",
        shortLabel = "Violet Fire",
        badge = "🟣 Flame",
        description = "Enchanted orchid-magenta embers float gently with zero harsh flicker",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFFF472B6
    ),
    GOLDEN_PHOENIX_FLAME(
        id = "golden_phoenix_flame",
        displayName = "🦅 Golden Phoenix Fire",
        shortLabel = "Gold Flame",
        badge = "🦅 Fire",
        description = "Warm liquid-gold embers ascend smoothly from glowing keycaps",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFFB703,
        secondaryHex = 0xFFFF5E00
    ),
    EMERALD_DRAGON_FLAME(
        id = "emerald_dragon_flame",
        displayName = "🐉 Emerald Dragon Fire",
        shortLabel = "Jade Fire",
        badge = "🐉 Flame",
        description = "Mystic emerald-teal dragon sparks rise gently from tapped keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFF00FF88,
        secondaryHex = 0xFF00E5FF
    ),
    ROSE_CANDLE_GLOW(
        id = "rose_candle_glow",
        displayName = "🕯️ Rose Candle Glow",
        shortLabel = "Rose Flame",
        badge = "🕯️ Warm",
        description = "Soft rose-coral candlelight embers rise calmly on keystrokes",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFB7185,
        secondaryHex = 0xFFFDE68A
    ),
    GLACIER_CRYSTAL_MIST(
        id = "glacier_crystal_mist",
        displayName = "❄️ Glacier Crystal",
        shortLabel = "Glacier",
        badge = "❄️ Ice",
        description = "Crisp arctic ice facets chill pressed keys and close neighbors",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFBAE6FD,
        secondaryHex = 0xFF0EA5E9
    ),
    AURORA_EMERALD_WAVE(
        id = "aurora_emerald_wave",
        displayName = "🌌 Aurora Emerald",
        shortLabel = "Aurora Green",
        badge = "🌌 Wave",
        description = "Northern lights emerald-cyan wave ripples smoothly across keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF34D399,
        secondaryHex = 0xFF38BDF8
    ),
    TANGERINE_SPARK_GLOW(
        id = "tangerine_spark_glow",
        displayName = "🍊 Tangerine Ember",
        shortLabel = "Citrus Fire",
        badge = "🍊 Glow",
        description = "Warm sunset orange-gold embers drift upward with silky motion",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFFF7A00,
        secondaryHex = 0xFFFDE047
    ),
    FROST_SAPPHIRE_VEIL(
        id = "frost_sapphire_veil",
        displayName = "💎 Sapphire Frost",
        shortLabel = "Blue Frost",
        badge = "💎 Ice",
        description = "Deep sapphire ice crystal shimmer coats adjacent keys gently",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFF60A5FA,
        secondaryHex = 0xFFE0F2FE
    ),
    CORAL_SUNSET_RIPPLE(
        id = "coral_sunset_ripple",
        displayName = "🌺 Coral Sunset",
        shortLabel = "Coral Wave",
        badge = "🌺 Warm",
        description = "Tropical coral-amber shockwave ripples evenly across the deck",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFFF6B6B,
        secondaryHex = 0xFFFF9E00
    ),
    SNOWFLAKE_HALO(
        id = "snowflake_halo",
        displayName = "🌨️ Snowflake Halo",
        shortLabel = "Snowflake",
        badge = "🌨️ Chill",
        description = "Pure white-aqua frost border sparkles cleanly on winter keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFF1F5F9,
        secondaryHex = 0xFF67E8F9
    ),
    AMBER_SOLAR_FLARE(
        id = "amber_solar_flare",
        displayName = "☀️ Amber Solar Fire",
        shortLabel = "Solar Fire",
        badge = "☀️ Ember",
        description = "Radiant solar amber-crimson flames rise gracefully per tap",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFF59E0B,
        secondaryHex = 0xFFEF4444
    ),
    AQUA_SPRING_RIPPLE(
        id = "aqua_spring_ripple",
        displayName = "💧 Aqua Spring",
        shortLabel = "Aqua Drop",
        badge = "💧 Water",
        description = "Crystal-clear water drop ring expands smoothly from tapped keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF22D3EE,
        secondaryHex = 0xFF5EEAD4
    ),
    MAGMA_RUBY_CORE(
        id = "magma_ruby_core",
        displayName = "🌋 Magma Ruby",
        shortLabel = "Ruby Core",
        badge = "🌋 Lava",
        description = "Smoldering ruby-gold embers float smoothly above pressed keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFE11D48,
        secondaryHex = 0xFFFF9E00
    ),
    MINT_ICE_CRYSTAL(
        id = "mint_ice_crystal",
        displayName = "🧊 Mint Ice Crystal",
        shortLabel = "Mint Ice",
        badge = "🧊 Frost",
        description = "Refreshing mint-turquoise frost edges light adjacent keys gently",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFA7F3D0,
        secondaryHex = 0xFF2DD4BF
    ),
    LIGHTNING_SILK_ARC(
        id = "lightning_silk_arc",
        displayName = "🌩️ Storm Silk Arc",
        shortLabel = "Storm Arc",
        badge = "🌩️ Volt",
        description = "Smooth electric sky-cyan cross beam echoes thunder strikes",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.ELECTRIC_CROSS,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFE0E7FF
    ),
    SPRING_PETAL_EMBER(
        id = "spring_petal_ember",
        displayName = "🌸 Sakura Sparks",
        shortLabel = "Sakura FX",
        badge = "🌸 Drift",
        description = "Delicate cherry-blossom pink sparks float upward smoothly",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFF9A8D4,
        secondaryHex = 0xFFFDA4AF
    ),
    OCEAN_PEARL_WAVE(
        id = "ocean_pearl_wave",
        displayName = "🐚 Ocean Pearl",
        shortLabel = "Pearl Wave",
        badge = "🐚 Ocean",
        description = "Serene seafoam-cobalt ring ripples smoothly across dark keys",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF5EEAD4,
        secondaryHex = 0xFF3B82F6
    ),
    COPPER_FORGE_SPARK(
        id = "copper_forge_spark",
        displayName = "⚒️ Copper Forge",
        shortLabel = "Copper",
        badge = "⚒️ Forge",
        description = "Warm molten bronze sparks rise evenly from tactile keystrokes",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.RISING_EMBER,
        primaryHex = 0xFFD97706,
        secondaryHex = 0xFFFDE68A
    ),
    WHITE_STAR_FROST(
        id = "white_star_frost",
        displayName = "🤍 Pure Ice Sheen",
        shortLabel = "Pure Ice",
        badge = "🤍 Frost",
        description = "Minimalist frosted silver-white crystal halo for clean keyboards",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFF8FAFC,
        secondaryHex = 0xFF94A3B8
    ),
    TEAL_PHOENIX_TRAIL(
        id = "teal_phoenix_trail",
        displayName = "🦚 Peacock Flame",
        shortLabel = "Peacock",
        badge = "🦚 Trail",
        description = "Iridescent teal-emerald comet streak flows smoothly while typing",
        category = TouchEffectCategory.PLASMA_ELEMENTAL,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF14B8A6,
        secondaryHex = 0xFF38BDF8
    ),

    // Prism, Luxury Gems & Chrono Smooth Collection (189..208)
    CHAMPAGNE_DIAMOND(
        id = "champagne_diamond",
        displayName = "🥂 Champagne Glint",
        shortLabel = "Champagne",
        badge = "🥂 Luxe",
        description = "Soft ivory-gold diamond glint shimmers on luxury dark keycaps",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFDE68A,
        secondaryHex = 0xFFF59E0B
    ),
    AQUAMARINE_PRISM(
        id = "aquamarine_prism",
        displayName = "🩵 Aquamarine Prism",
        shortLabel = "Aquamarine",
        badge = "🩵 Gem",
        description = "Clear aquamarine crystal glint illuminates key corners softly",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF67E8F9,
        secondaryHex = 0xFF2DD4BF
    ),
    ROYAL_TOURMALINE(
        id = "royal_tourmaline",
        displayName = "💖 Tourmaline Orbit",
        shortLabel = "Tourmaline",
        badge = "💖 Gem",
        description = "Watermelon pink-mint crystal ring glides gently around keycaps",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFF472B6,
        secondaryHex = 0xFF34D399
    ),
    OBSIDIAN_CROWN(
        id = "obsidian_crown",
        displayName = "👑 Obsidian Crown",
        shortLabel = "Crown Gold",
        badge = "👑 Royal",
        description = "Regal gold-platinum halo gleams smoothly on matte obsidian keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFFFD166,
        secondaryHex = 0xFFE2E8F0
    ),
    PEARL_SILK_RIPPLE(
        id = "pearl_silk_ripple",
        displayName = "🤍 Pearl Ripple",
        shortLabel = "Pearl Ring",
        badge = "🤍 Pearl",
        description = "Soft iridescent mother-of-pearl ring expands gracefully on tap",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFF1F5F9,
        secondaryHex = 0xFFF9A8D4
    ),
    TOPAZ_SPARKLE(
        id = "topaz_sparkle",
        displayName = "🔶 Mystic Topaz",
        shortLabel = "Topaz FX",
        badge = "🔶 Gem",
        description = "Honey-cyan mystic topaz glint sparkles evenly along key rims",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFBBF24,
        secondaryHex = 0xFF22D3EE
    ),
    CRYSTAL_PENDULUM(
        id = "crystal_pendulum",
        displayName = "🕰️ Crystal Pendulum",
        shortLabel = "Pendulum",
        badge = "🕰️ Chrono",
        description = "Smooth cyan-violet chrono orbit keeps rhythm with every word",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFC084FC
    ),
    GARNET_SILK_WAVE(
        id = "garnet_silk_wave",
        displayName = "♦️ Garnet Wave",
        shortLabel = "Garnet",
        badge = "♦️ Ruby",
        description = "Deep velvet garnet ripple radiates cleanly across tapped keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFFE11D48,
        secondaryHex = 0xFFFDA4AF
    ),
    JADEITE_HALO(
        id = "jadeite_halo",
        displayName = "🟢 Jadeite Halo",
        shortLabel = "Jadeite",
        badge = "🟢 Luxe",
        description = "Imperial jade orbital gem glow highlights pressed letters",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFFFDE68A
    ),
    MORGANITE_BLUSH(
        id = "morganite_blush",
        displayName = "🌷 Morganite Blush",
        shortLabel = "Morganite",
        badge = "🌷 Blush",
        description = "Soft peach-pink crystal glow cushions every keystroke gently",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFFFECDD3,
        secondaryHex = 0xFFF472B6
    ),
    TANZANITE_COMET(
        id = "tanzanite_comet",
        displayName = "🔮 Tanzanite Trail",
        shortLabel = "Tanzanite",
        badge = "🔮 Gem",
        description = "Indigo-violet gemstone ribbon links fast taps smoothly",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.COMET_STREAK,
        primaryHex = 0xFF818CF8,
        secondaryHex = 0xFFD946EF
    ),
    DIAMOND_DEW_FROST(
        id = "diamond_dew_frost",
        displayName = "💧 Diamond Dew",
        shortLabel = "Dew Frost",
        badge = "💧 Pure",
        description = "Clear morning-dew crystal shimmer softly outlines nearby keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.FROST_CRYSTAL,
        primaryHex = 0xFFE0F2FE,
        secondaryHex = 0xFF7DD3FC
    ),
    CHRONO_AMETHYST_PULSE(
        id = "chrono_amethyst_pulse",
        displayName = "💜 Chrono Amethyst",
        shortLabel = "Amethyst FX",
        badge = "💜 Ring",
        description = "Lustrous amethyst-gold ring converges smoothly on every tap",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.GRAVITY_IMPLOSION,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFFFFD166
    ),
    PRISM_AURORA_GLINT(
        id = "prism_aurora_glint",
        displayName = "🌈 Prism Aurora",
        shortLabel = "Prism Aura",
        badge = "🌈 Glint",
        description = "Refined mint-magenta diamond sparkle accents pressed keycaps",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFF00F5D4,
        secondaryHex = 0xFFEC4899
    ),
    VELVET_ONYX_EDGE(
        id = "velvet_onyx_edge",
        displayName = "🖤 Velvet Onyx",
        shortLabel = "Onyx Edge",
        badge = "🖤 Minimal",
        description = "Ultra-subtle silver-slate highlight for distraction-free typing",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.GLITCH_PULSE,
        primaryHex = 0xFFCBD5E1,
        secondaryHex = 0xFF64748B
    ),
    SOLSTICE_CHRONO_ORB(
        id = "solstice_chrono_orb",
        displayName = "🌞 Solstice Orb",
        shortLabel = "Solstice",
        badge = "🌞 Orbit",
        description = "Warm sunset gold-coral orbital glow spins gently around taps",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFFFF9E00,
        secondaryHex = 0xFFFB7185
    ),
    CELADON_RIPPLE(
        id = "celadon_ripple",
        displayName = "🍃 Celadon Ripple",
        shortLabel = "Celadon",
        badge = "🍃 Calm",
        description = "Pale ceramic jade shockwave blooms smoothly across dark keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.RADIAL_SHOCKWAVE,
        primaryHex = 0xFF6EE7B7,
        secondaryHex = 0xFF93C5FD
    ),
    RUBELLITE_SYNAPSE(
        id = "rubellite_synapse",
        displayName = "🌺 Rubellite Glow",
        shortLabel = "Rubellite",
        badge = "🌺 Gem",
        description = "Crimson-orchid crystal glow spreads gently to adjacent keys",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.NEURAL_SYNAPSE,
        primaryHex = 0xFFF43F5E,
        secondaryHex = 0xFFE879F9
    ),
    AURIC_BEVEL_GLINT(
        id = "auric_bevel_glint",
        displayName = "✨ Auric Bevel",
        shortLabel = "Auric Gold",
        badge = "✨ Bevel",
        description = "Fine gold-ivory diamond glint graces beveled corners cleanly",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.PRISM_SHATTER,
        primaryHex = 0xFFFFD166,
        secondaryHex = 0xFFFFFBEB
    ),
    SERENITY_MIRROR_ORBIT(
        id = "serenity_mirror_orbit",
        displayName = "🪞 Serenity Mirror",
        shortLabel = "Serenity",
        badge = "🪞 Calm",
        description = "Calm icy-cyan and soft lavender twin orbit completes 200+ FX",
        category = TouchEffectCategory.PRISM_CHRONO,
        visualStyle = TouchEffectVisualStyle.CHRONO_ORBIT,
        primaryHex = 0xFF7DD3FC,
        secondaryHex = 0xFFC4B5FD
    );

    companion object {
        val activeEffects: List<TouchTypingEffect>
            get() = entries.filter { it != NONE }

        fun byCategory(category: TouchEffectCategory): List<TouchTypingEffect> {
            return if (category == TouchEffectCategory.ALL) activeEffects
            else activeEffects.filter { it.category == category }
        }

        fun fromSavedName(name: String?): TouchTypingEffect {
            if (name.isNullOrBlank()) return TOUCH_SHOCKWAVE_RING
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true)
            } ?: TOUCH_SHOCKWAVE_RING
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
            if (name.isNullOrBlank()) return NORMAL_DEFAULT
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
                else -> NORMAL_DEFAULT
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

enum class SoundCategory(val displayName: String) {
    ALL("All (53+)"),
    MECHANICAL("Mechanical"),
    GAMING("Gaming"),
    SCIFI("Sci-Fi"),
    RETRO("Retro"),
    ASMR("Soft & ASMR")
}

enum class SwitchSoundProfile(
    val displayName: String,
    val actuationLabel: String,
    val description: String,
    val accentHex: Long,
    val category: SoundCategory
) {
    SILENT(
        displayName = "Silent (OFF)",
        actuationLabel = "0 dB · Default OFF",
        description = "Keypress sound disabled for silent typing",
        accentHex = 0xFF8B95B5,
        category = SoundCategory.ALL
    ),

    // Mechanical & Enthusiast Switches (12)
    SAMSUNG_SOFT(
        displayName = "Soft Modern",
        actuationLabel = "35g · Subtle Tap",
        description = "Gentle modern keypress tap tuned for clean daily typing",
        accentHex = 0xFF00F59B,
        category = SoundCategory.MECHANICAL
    ),
    CLICKY_BLUE(
        displayName = "Clicky Blue",
        actuationLabel = "60g · Crisp Snap",
        description = "Audible tactile jacket click with bright high-frequency snap",
        accentHex = 0xFF00E5FF,
        category = SoundCategory.MECHANICAL
    ),
    TACTILE_BROWN(
        displayName = "Tactile Brown",
        actuationLabel = "55g · Warm Bump",
        description = "Balanced tactile feedback with a refined mid-tone thock",
        accentHex = 0xFFFFB703,
        category = SoundCategory.MECHANICAL
    ),
    LINEAR_RED(
        displayName = "Linear Red",
        actuationLabel = "45g · Deep Thock",
        description = "Smooth fast travel with a deep PBT bottom-out thock",
        accentHex = 0xFFFF2E93,
        category = SoundCategory.MECHANICAL
    ),
    HOLY_PANDA_THOCK(
        displayName = "Holy Panda Thock",
        actuationLabel = "62g · Creamy Bump",
        description = "Cult-favorite deep resonant polycarb tactile thock",
        accentHex = 0xFFFF8800,
        category = SoundCategory.MECHANICAL
    ),
    BLACK_INK_V2(
        displayName = "Black Ink V2",
        actuationLabel = "60g · Heavy Linear",
        description = "Thick lubed smoky nylon bottom-out punch",
        accentHex = 0xFF60A5FA,
        category = SoundCategory.MECHANICAL
    ),
    GATERON_MILKY_YELLOW(
        displayName = "Milky Yellow",
        actuationLabel = "50g · Creamy Smooth",
        description = "Silky milky-housing linear pop for rapid bursts",
        accentHex = 0xFFFDE047,
        category = SoundCategory.MECHANICAL
    ),
    BOX_JADE_CRYSTAL(
        displayName = "Box Jade Clickbar",
        actuationLabel = "55g · Thick Click",
        description = "Razor-sharp dual-stage clickbar snap",
        accentHex = 0xFF10B981,
        category = SoundCategory.MECHANICAL
    ),
    TOPRE_EC_THOCK(
        displayName = "Topre EC Cup",
        actuationLabel = "45g · Electro-Capacitive",
        description = "Signature Japanese electrostatic rain-on-drum thock",
        accentHex = 0xFFA78BFA,
        category = SoundCategory.MECHANICAL
    ),
    CHERRY_SILVER_SPEED(
        displayName = "Silver Speed",
        actuationLabel = "40g · Rapid Trigger",
        description = "Ultra-short actuation snap built for fast combos",
        accentHex = 0xFFE2E8F0,
        category = SoundCategory.MECHANICAL
    ),
    IBM_BUCKLING_SPRING(
        displayName = "Buckling Spring",
        actuationLabel = "70g · Metallic Ping",
        description = "Classic Model-M heavy spring ring and mechanical clack",
        accentHex = 0xFFF59E0B,
        category = SoundCategory.MECHANICAL
    ),
    OIL_KING_DEEP(
        displayName = "Oil King Deep",
        actuationLabel = "55g · Sub-Bass",
        description = "Factory-lubed deep low-end thock with zero rattle",
        accentHex = 0xFF38BDF8,
        category = SoundCategory.MECHANICAL
    ),

    // Gaming & Esports FX (12)
    ESPORTS_PRO_TRIGGER(
        displayName = "Esports Trigger",
        actuationLabel = "0.1ms · Optical Snap",
        description = "Crisp tournament optical switch actuation punch",
        accentHex = 0xFF00E5FF,
        category = SoundCategory.GAMING
    ),
    RAZOR_OPTO_PURPLE(
        displayName = "Opto Clicky Pro",
        actuationLabel = "48g · Purple Beam",
        description = "Sharp high-velocity optical click engineered for FPS",
        accentHex = 0xFFC084FC,
        category = SoundCategory.GAMING
    ),
    RAPID_FIRE_FPS(
        displayName = "Rapid Fire Burst",
        actuationLabel = "SMG · Micro Snap",
        description = "Tight, punchy tactical rifle trigger impulse",
        accentHex = 0xFFFB7185,
        category = SoundCategory.GAMING
    ),
    SNIPER_BOLT_LOCK(
        displayName = "Sniper Bolt Lock",
        actuationLabel = "Precision · Metallic",
        description = "Clean crisp chamber lock snap on every key hit",
        accentHex = 0xFFFACC15,
        category = SoundCategory.GAMING
    ),
    SHOTGUN_RELOAD_SNAP(
        displayName = "Tactical Pump",
        actuationLabel = "Heavy · Impact",
        description = "Heavy dual-transient mechanical pump impact",
        accentHex = 0xFFEF4444,
        category = SoundCategory.GAMING
    ),
    HEADSHOT_PING(
        displayName = "Headshot Ping",
        actuationLabel = "Crit · Crisp Ring",
        description = "Bright competitive armor-break dink chime",
        accentHex = 0xFFFFD600,
        category = SoundCategory.GAMING
    ),
    KILLSTREAK_COMBO(
        displayName = "Combo Surge",
        actuationLabel = "Dual Pitch · Hype",
        description = "Dual-tone rising streak confirmation click",
        accentHex = 0xFFFF2A85,
        category = SoundCategory.GAMING
    ),
    APEX_SHIELD_POP(
        displayName = "Shield Battery Pop",
        actuationLabel = "BR · Crisp Pulse",
        description = "Bright shield-cell charge click with clean tail",
        accentHex = 0xFF38BDF8,
        category = SoundCategory.GAMING
    ),
    VALO_TAP_CRISP(
        displayName = "Valo One-Tap",
        actuationLabel = "Clutch · Glassy",
        description = "Glassy high-clarity confirmation tap for crisp aim typing",
        accentHex = 0xFF2DD4BF,
        category = SoundCategory.GAMING
    ),
    NITRO_BOOST_TURBO(
        displayName = "Nitro Turbo Pop",
        actuationLabel = "Racing · Whoosh",
        description = "Compressed turbo wastegate chirp on fast inputs",
        accentHex = 0xFFFB923C,
        category = SoundCategory.GAMING
    ),
    MECHANICAL_PARRY(
        displayName = "Blade Parry Clang",
        actuationLabel = "Action · Steel",
        description = "Compact metallic katana parry shimmer",
        accentHex = 0xFF93C5FD,
        category = SoundCategory.GAMING
    ),
    CRITICAL_HIT_DING(
        displayName = "Critical Hit Ding",
        actuationLabel = "RPG · Precision",
        description = "Bright crystal chime for critical combo strikes",
        accentHex = 0xFFF472B6,
        category = SoundCategory.GAMING
    ),

    // Sci-Fi & Cyber FX (11)
    CYBER_LASER_PULSE(
        displayName = "Cyber Laser Pulse",
        actuationLabel = "Synth · Downsweep",
        description = "Futuristic neon laser beam downward frequency sweep",
        accentHex = 0xFF00F0FF,
        category = SoundCategory.SCIFI
    ),
    QUANTUM_BLASTER(
        displayName = "Quantum Blaster",
        actuationLabel = "Sci-Fi · Bolt",
        description = "Punchy starfighter plasma blaster pulse",
        accentHex = 0xFFFF007F,
        category = SoundCategory.SCIFI
    ),
    PLASMA_SPARK(
        displayName = "Plasma Arc Spark",
        actuationLabel = "Arc · Electric",
        description = "High-voltage micro plasma discharge crackle",
        accentHex = 0xFF3B82F6,
        category = SoundCategory.SCIFI
    ),
    HOLOGRAM_TAP(
        displayName = "Hologram Glass Tap",
        actuationLabel = "HUD · Pure Tone",
        description = "Clean sci-fi holographic interface confirmation tone",
        accentHex = 0xFF22D3EE,
        category = SoundCategory.SCIFI
    ),
    NEURAL_SYNAPSE(
        displayName = "AI Neural Synapse",
        actuationLabel = "Neural · Shimmer",
        description = "Subtle cybernetic impulse tuned for AI keyboards",
        accentHex = 0xFFA855F7,
        category = SoundCategory.SCIFI
    ),
    MECH_SERVO_MICRO(
        displayName = "Mech Exo Servo",
        actuationLabel = "Robotics · Actuator",
        description = "Compact titanium exo-suit micro-servo click",
        accentHex = 0xFF94A3B8,
        category = SoundCategory.SCIFI
    ),
    STARSHIP_CONSOLE(
        displayName = "Starship Bridge",
        actuationLabel = "LCARS · Chirp",
        description = "Iconic command-deck tactile touch-panel beep",
        accentHex = 0xFFF59E0B,
        category = SoundCategory.SCIFI
    ),
    ION_CANNON_CLICK(
        displayName = "Ion Capacitor",
        actuationLabel = "Energy · Discharge",
        description = "Deep sub-harmonic ion charge release thump",
        accentHex = 0xFF06B6D4,
        category = SoundCategory.SCIFI
    ),
    MATRIX_DECRYPT(
        displayName = "Matrix Decrypt Bit",
        actuationLabel = "Cyber · Bitstream",
        description = "Tight digital cipher tick for hacker-style typing",
        accentHex = 0xFF00FF66,
        category = SoundCategory.SCIFI
    ),
    CYBER_SONAR_PING(
        displayName = "Deep Sonar Ping",
        actuationLabel = "Radar · Echo",
        description = "Pure resonant submarine tactical radar ping",
        accentHex = 0xFF34D399,
        category = SoundCategory.SCIFI
    ),
    TRON_LIGHT_DISC(
        displayName = "Light Disc Snap",
        actuationLabel = "Neon · Ring",
        description = "Glassy neon grid disc ricochet impulse",
        accentHex = 0xFF60A5FA,
        category = SoundCategory.SCIFI
    ),

    // Retro Arcade & Vintage (9)
    RETRO_8BIT_BLIP(
        displayName = "8-Bit Pixel Blip",
        actuationLabel = "Square · Chiptune",
        description = "Authentic 8-bit handheld console square wave blip",
        accentHex = 0xFF4ADE80,
        category = SoundCategory.RETRO
    ),
    ARCADE_COIN_PING(
        displayName = "Arcade Coin Ping",
        actuationLabel = "Cabinet · Classic",
        description = "Crisp retro arcade token chime micro-hit",
        accentHex = 0xFFFACC15,
        category = SoundCategory.RETRO
    ),
    PIXEL_JUMP_TAP(
        displayName = "Pixel Power Jump",
        actuationLabel = "Platformer · Up",
        description = "Bouncy upward pitch chirp inspired by 90s classics",
        accentHex = 0xFFFF6B6B,
        category = SoundCategory.RETRO
    ),
    NES_MENU_TICK(
        displayName = "Console Menu Tick",
        actuationLabel = "8-Bit · Cursor",
        description = "Ultra-short crisp inventory selection blip",
        accentHex = 0xFF38BDF8,
        category = SoundCategory.RETRO
    ),
    CHIPTUNE_PLUCK(
        displayName = "Chiptune Synth Pluck",
        actuationLabel = "FM · Retro",
        description = "Warm FM-synthesis arcade bass pluck",
        accentHex = 0xFFE879F9,
        category = SoundCategory.RETRO
    ),
    GAMEBOY_DPAD(
        displayName = "Pocket D-Pad Snap",
        actuationLabel = "Handheld · Membrane",
        description = "Nostalgic pocket handheld rubber-dome thump",
        accentHex = 0xFFA3E635,
        category = SoundCategory.RETRO
    ),
    SYNTHWAVE_TOM(
        displayName = "80s Synthwave Tom",
        actuationLabel = "Retrowave · Drum",
        description = "Gated 80s electronic drum tom drop",
        accentHex = 0xFFFF2E93,
        category = SoundCategory.RETRO
    ),
    TYPEWRITER_VINTAGE(
        displayName = "Vintage Typewriter",
        actuationLabel = "Steel · Hammer",
        description = "Mechanical ribbon strike & carriage clack",
        accentHex = 0xFFD97706,
        category = SoundCategory.RETRO
    ),
    TELETYPE_TERMINAL(
        displayName = "CRT Terminal Key",
        actuationLabel = "1980s · Phosphor",
        description = "Crisp green-screen mainframe terminal click",
        accentHex = 0xFF22C55E,
        category = SoundCategory.RETRO
    ),

    // Soft, Organic & ASMR (9)
    GLASS_WATER_DROP(
        displayName = "Water Droplet Pop",
        actuationLabel = "Liquid · Up-Chirp",
        description = "Smooth rising bubble waterdrop pop for relaxing typing",
        accentHex = 0xFF38BDF8,
        category = SoundCategory.ASMR
    ),
    CRYSTAL_ICE_CHIME(
        displayName = "Crystal Ice Chime",
        actuationLabel = "Glass · Pure",
        description = "Delicate frozen crystal micro-bell resonance",
        accentHex = 0xFFBAE6FD,
        category = SoundCategory.ASMR
    ),
    WOOD_MARIMBA_TAP(
        displayName = "Rosewood Marimba",
        actuationLabel = "Organic · Warm",
        description = "Warm acoustic rosewood mallet tap",
        accentHex = 0xFFF59E0B,
        category = SoundCategory.ASMR
    ),
    CREAMY_PEBBLE_POP(
        displayName = "Creamy Pebble Pop",
        actuationLabel = "ASMR · Mahjong",
        description = "Satisfying rounded creamy ceramic tile click",
        accentHex = 0xFFF9A8D4,
        category = SoundCategory.ASMR
    ),
    BAMBOO_SHISHI_TAP(
        displayName = "Zen Bamboo Knock",
        actuationLabel = "Hollow · Calm",
        description = "Peaceful hollow bamboo garden water knock",
        accentHex = 0xFF86EFAC,
        category = SoundCategory.ASMR
    ),
    SILK_CLOUD_THOCK(
        displayName = "Silk Cloud Thock",
        actuationLabel = "Feather · Muted",
        description = "Airy ultra-soft cushioned low-frequency thock",
        accentHex = 0xFFE2E8F0,
        category = SoundCategory.ASMR
    ),
    VELVET_RUBBER_DOME(
        displayName = "Velvet Dome",
        actuationLabel = "Quiet · Plush",
        description = "Muted studio silicone dome press",
        accentHex = 0xFFC4B5FD,
        category = SoundCategory.ASMR
    ),
    RAINDROP_RIPPLE(
        displayName = "Raindrop Leaf Tap",
        actuationLabel = "Lo-Fi · Soft",
        description = "Gentle lo-fi raindrop splash micro-transient",
        accentHex = 0xFF67E8F9,
        category = SoundCategory.ASMR
    ),
    KALIMBA_THUMB_PLUCK(
        displayName = "Kalimba Thumb Tine",
        actuationLabel = "Acoustic · Tine",
        description = "Soft African thumb-piano steel tine pluck",
        accentHex = 0xFFFDBA74,
        category = SoundCategory.ASMR
    );

    companion object {
        val playableEntries: List<SwitchSoundProfile>
            get() = entries.filter { it != SILENT }
    }
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
    val effect: RgbEffect = RgbEffect.NORMAL_DEFAULT,
    val touchEffect: TouchTypingEffect = TouchTypingEffect.TOUCH_SHOCKWAVE_RING,
    val palette: RgbPalette = RgbPalette.VICE_CITY_NEON,
    val keycapStyle: KeycapStyle = KeycapStyle.PUDDING,
    val switchSound: SwitchSoundProfile = SwitchSoundProfile.SILENT,
    val animationSpeed: Float = 1.0f,
    val glowIntensity: Float = 0.90f,
    val keyCornerRadiusDp: Float = 6f,
    val showKeyBorders: Boolean = true,
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
