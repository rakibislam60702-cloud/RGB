package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class TouchEffectCategory(val id: String, val label: String) {
    ALL("all", "✨ All (56)"),
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
    );

    companion object {
        val activeEffects: List<TouchTypingEffect>
            get() = entries.filter { it != NONE }

        fun byCategory(category: TouchEffectCategory): List<TouchTypingEffect> {
            return if (category == TouchEffectCategory.ALL) activeEffects
            else activeEffects.filter { it.category == category }
        }

        fun fromSavedName(name: String?): TouchTypingEffect {
            if (name.isNullOrBlank()) return AI_NEURAL_PULSE
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true)
            } ?: AI_NEURAL_PULSE
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
    val touchEffect: TouchTypingEffect = TouchTypingEffect.AI_NEURAL_PULSE,
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
