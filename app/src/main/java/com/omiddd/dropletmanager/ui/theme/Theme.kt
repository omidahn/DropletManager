package com.omiddd.dropletmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = OnPrimary,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = OnBackgroundDark,
    secondary = TealAccent,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnBackgroundDark,
    tertiary = GoldAccent,
    tertiaryContainer = Color(0xFF423015),
    onTertiaryContainer = Color(0xFFF9DFC0),
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnBackgroundDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorRed,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),
    onBackground = OnBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = OnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = TealAccent,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = GoldAccent,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurface = OnBackground,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed,
    onBackground = OnBackground
)

@Composable
fun DropletManagerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
