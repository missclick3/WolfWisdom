package ru.missclick.wolfwisdom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Figma defines one mode, Moonlight, independent of the device theme/wallpaper.
private val MoonlightColorScheme = darkColorScheme(
    primary = WolfColors.Accent,
    onPrimary = WolfColors.Background,
    primaryContainer = WolfColors.Surface,
    onPrimaryContainer = WolfColors.Accent,
    secondary = WolfColors.Secondary,
    onSecondary = WolfColors.Background,
    secondaryContainer = WolfColors.Surface,
    onSecondaryContainer = WolfColors.Secondary,
    tertiary = WolfColors.Muted,
    onTertiary = WolfColors.Background,
    tertiaryContainer = WolfColors.Surface,
    onTertiaryContainer = WolfColors.Text,
    background = WolfColors.Background,
    onBackground = WolfColors.Text,
    surface = WolfColors.Surface,
    onSurface = WolfColors.Text,
    surfaceVariant = WolfPalette.Night900,
    onSurfaceVariant = WolfColors.Secondary,
    surfaceTint = WolfColors.Accent,
    inverseSurface = WolfColors.Text,
    inverseOnSurface = WolfColors.Background,
    inversePrimary = WolfPalette.Night800,
    outline = WolfColors.Border,
    outlineVariant = WolfColors.GlassBorder,
    scrim = WolfColors.Background,
    surfaceBright = WolfColors.Surface,
    surfaceDim = WolfColors.Background,
    surfaceContainerLowest = WolfColors.Background,
    surfaceContainerLow = WolfPalette.Night900,
    surfaceContainer = WolfColors.Surface,
    surfaceContainerHigh = WolfColors.Surface,
    surfaceContainerHighest = WolfColors.Surface
)

@Composable
fun WolfWisdomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MoonlightColorScheme,
        typography = Typography,
        shapes = WolfShapes,
        content = content
    )
}
