package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorTokenSurfaceHighest = androidx.compose.ui.graphics.Color(0xFF262C42)

private val ProRgbDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = OnNeonCyan,
    primaryContainer = NeonCyanContainer,
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = OnNeonMagenta,
    secondaryContainer = NeonMagentaContainer,
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonViolet,
    onTertiary = TextPrimary,
    tertiaryContainer = NeonVioletContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianCanvas,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ColorTokenSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
    error = NeonError,
    onError = TextPrimary
)

private val ProRgbShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
