package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Legendary Flagship Obsidian & Cyber-Royal Nebula Aura Palette
val ObsidianNebulaTop = Color(0xFF060812)
val CyberRoyalMid = Color(0xFF111633)
val CyberVioletBottom = Color(0xFF1D1138)

// Backwards-compatible aliases mapped to Obsidian & Cyber-Royal Nebula tokens
val MidnightNavyTop = ObsidianNebulaTop
val MidnightNavyMid = CyberRoyalMid
val RoyalIndigoBottom = CyberVioletBottom
val RoyalBlueMid = CyberRoyalMid
val RoyalBlueDeep = ObsidianNebulaTop

// Luminous Translucent Dark-Glass Card & Navigation Surfaces (#12172E at 85% opacity)
val GlassCardSurface = Color(0xFF12172E).copy(alpha = 0.85f)
val GlassCardSelected = Color(0xFF1C244A).copy(alpha = 0.92f)
val GlassCardInner = Color(0xFF090D20).copy(alpha = 0.88f)
val GlassBorder = Color.White.copy(alpha = 0.18f)
val GlassBorderStrong = Color.White.copy(alpha = 0.48f)
val GlassNavSurface = Color(0xFF070A18).copy(alpha = 0.95f)
val GlassDialogSurface = Color(0xFF111630)

// Backwards-compatible semantic surface aliases mapped to Luminous Dark-Glass tokens
val ObsidianCanvas = GlassCardInner
val ObsidianSurface = GlassCardSurface
val ObsidianSurfaceVariant = GlassCardSelected
val ObsidianSurfaceHigh = Color(0xFF232C58).copy(alpha = 0.92f)
val ObsidianBorder = GlassBorder
val ObsidianBorderSubtle = Color.White.copy(alpha = 0.12f)

// Luxury Brand Signature & Royal Gold Metallic VIP Tokens
val BrandObsidianPill = Color(0xFF0D1122)
val LuxuryPinkStroke = Color(0xFFFF2E63)
val LuxuryCyanStroke = Color(0xFF00F5D4)
val RoyalGold = Color(0xFFFFB800)
val RoyalGoldPillBg = Color(0xFF1F180A)
val RoyalGoldAccent = Color(0xFFFFD166)

// Vibrant Flagship Neon Accents
val NeonCyan = Color(0xFF00F5D4)
val NeonCyanContainer = Color(0xFF13293D)
val OnNeonCyan = Color(0xFF00201C)

val NeonMagenta = Color(0xFFFF2A85)
val NeonMagentaContainer = Color(0x55FF2A85)
val OnNeonMagenta = Color(0xFFFFFFFF)

val NeonViolet = Color(0xFF8B5CF6)
val NeonVioletContainer = Color(0x458B5CF6)

val NeonEmerald = Color(0xFF00F59B)
val NeonEmeraldContainer = Color(0x3300F59B)

val NeonAmber = Color(0xFFFFB800)
val NeonAmberContainer = Color(0x401E170A)

val NeonError = Color(0xFFFF5C7C)

// Crisp Pure White Typography Hierarchy across all flagship screens
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFFFFFFF)
val TextMuted = Color(0xE6FFFFFF)

// Actual Keyboard Chassis Tokens (UNTOUCHED dark keycaps for crisp typing contrast)
val KeyChassisDark = Color(0xFF0B0814)
val KeyCapSurface = Color(0xFF161224)
val KeyCapModifierSurface = Color(0xFF1E1832)
val KeyCapPressedSurface = Color(0xFF2A2146)
