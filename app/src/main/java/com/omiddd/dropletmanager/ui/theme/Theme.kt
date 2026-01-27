package com.omiddd.dropletmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = PinkAccent,
    background = BackgroundDark,
    surface = BackgroundDark,
    error = ErrorRed,
    onPrimary = OnPrimary,
    onBackground = OnBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = PinkAccent,
    background = Background,
    surface = Background,
    error = ErrorRed,
    onPrimary = OnPrimary,
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
