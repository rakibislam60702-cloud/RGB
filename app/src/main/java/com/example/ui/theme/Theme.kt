package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ProRgbDarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = ObsidianNebulaTop,
    primaryContainer = GlassCardSelected,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = ObsidianNebulaTop,
    secondaryContainer = GlassCardSurface,
    onSecondaryContainer = Color.White,
    tertiary = RoyalGold,
    onTertiary = ObsidianNebulaTop,
    tertiaryContainer = GlassCardSurface,
    onTertiaryContainer = Color.White,
    background = ObsidianNebulaTop,
    onBackground = Color.White,
    surface = GlassCardSurface,
    onSurface = Color.White,
    surfaceVariant = GlassCardSelected,
    onSurfaceVariant = Color.White,
    surfaceContainerLowest = ObsidianNebulaTop,
    surfaceContainerLow = GlassCardSurface,
    surfaceContainer = GlassCardSelected,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = GlassDialogSurface,
    outline = GlassBorder,
    outlineVariant = ObsidianBorderSubtle,
    error = NeonError,
    onError = Color.White
)

private val ProRgbShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Signature Flagship Gradient Rim Brushes
val FlagshipGoldCyanBrush = Brush.linearGradient(
    colors = listOf(RoyalGold, Color(0xFFFFE599), NeonCyan)
)

val FlagshipCyanMagentaBrush = Brush.linearGradient(
    colors = listOf(NeonCyan, NeonViolet, NeonMagenta)
)

val FlagshipGoldAmberBrush = Brush.linearGradient(
    colors = listOf(RoyalGold, Color(0xFFFF8A00), RoyalGoldAccent)
)

val FlagshipEmeraldCyanBrush = Brush.linearGradient(
    colors = listOf(NeonEmerald, NeonCyan)
)

val FlagshipGlassRimSubtle = Brush.linearGradient(
    colors = listOf(
        NeonCyan.copy(alpha = 0.48f),
        Color.White.copy(alpha = 0.22f),
        NeonMagenta.copy(alpha = 0.42f)
    )
)

val FlagshipGlassRimGoldSubtle = Brush.linearGradient(
    colors = listOf(
        RoyalGold.copy(alpha = 0.56f),
        Color.White.copy(alpha = 0.22f),
        NeonCyan.copy(alpha = 0.44f)
    )
)

/**
 * Rich Multi-Layer Flagship Aura Background:
 * Deep Obsidian & Cyber-Royal Nebula Gradient (`#060812` -> `#111633` -> `#1D1138`)
 * accented with ambient radial Neon Cyan (`#00F5D4`) & Royal Gold (`#FFB800`) light orbs
 * behind the top header plus subtle lower nebula depth so every screen feels alive and 3D.
 */
fun Modifier.midnightNavyIndigoWallpaper(): Modifier = this
    .background(
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to ObsidianNebulaTop,
                0.48f to CyberRoyalMid,
                1.00f to CyberVioletBottom
            )
        )
    )
    .drawBehind {
        // Top-left Neon Cyan (#00F5D4) ambient light orb behind header
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.20f),
                    NeonCyan.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.14f, size.height * 0.08f),
                radius = size.maxDimension * 0.46f
            )
        )
        // Top-right Royal Gold (#FFB800) ambient light orb behind header
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    RoyalGold.copy(alpha = 0.17f),
                    RoyalGold.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.88f, size.height * 0.09f),
                radius = size.maxDimension * 0.44f
            )
        )
        // Mid-left deep cyber violet volumetric depth aura
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonViolet.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.12f, size.height * 0.56f),
                radius = size.maxDimension * 0.52f
            )
        )
        // Lower-right subtle neon magenta nebula glow for 3D bottom buoyancy
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonMagenta.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.86f, size.height * 0.90f),
                radius = size.maxDimension * 0.52f
            )
        )
    }

/**
 * Luminous 3D Dark-Glassmorphic Card Modifier:
 * Applies soft colored outer glow shadow, translucent `#12172E` (85%) dark glass surface,
 * subtle top glass specular sheen, and crisp gradient neon rim border.
 */
fun Modifier.luminousGlassCard(
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderBrush: Brush = FlagshipGlassRimSubtle,
    borderWidth: Dp = 1.2.dp,
    glowColor: Color = NeonCyan,
    glowElevation: Dp = 12.dp,
    surfaceColor: Color = GlassCardSurface
): Modifier = this
    .shadow(
        elevation = glowElevation,
        shape = shape,
        ambientColor = glowColor.copy(alpha = 0.36f),
        spotColor = glowColor.copy(alpha = 0.48f)
    )
    .clip(shape)
    .background(surfaceColor)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.07f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.16f)
            )
        )
    )
    .border(
        width = borderWidth,
        brush = borderBrush,
        shape = shape
    )

@Composable
fun ProRgbKeyboardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProRgbDarkColorScheme,
        typography = ProRgbTypography,
        shapes = ProRgbShapes,
        content = content
    )
}
