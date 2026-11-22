package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ClipPulseDarkScheme = darkColorScheme(
    primary = AcidLime,
    onPrimary = OnAcidLime,
    primaryContainer = ColorTokens.PrimaryContainer,
    onPrimaryContainer = AcidLime,
    secondary = CyanSpark,
    onSecondary = OnCyanSpark,
    secondaryContainer = CyanSparkContainer,
    onSecondaryContainer = CyanSpark,
    tertiary = ViralCoral,
    onTertiary = OnViralCoral,
    tertiaryContainer = ViralCoralContainer,
    onTertiaryContainer = ViralCoral,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    surfaceContainerHighest = ObsidianSurfaceBright,
    outline = HairlineBorder,
    outlineVariant = HairlineBorderBright,
    error = ViralCoral,
    onError = OnViralCoral
)

private object ColorTokens {
    val PrimaryContainer = androidx.compose.ui.graphics.Color(0xFF242E00)
}

private val ClipPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClipPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipPulseDarkScheme,
        typography = ClipPulseTypography,
        shapes = ClipPulseShapes,
        content = content
    )
}
