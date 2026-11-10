package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ChromeBlueDark,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = KopiCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9EEFFD),
    tertiary = KopiPurple,
    background = ChromeDarkBg,
    onBackground = ChromeDarkText,
    surface = ChromeDarkSurface,
    onSurface = ChromeDarkText,
    surfaceVariant = ChromeDarkSurfaceVariant,
    onSurfaceVariant = ChromeDarkTextSecondary,
    outline = ChromeDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ChromeBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = KopiPurple,
    background = ChromeLightBg,
    onBackground = ChromeLightText,
    surface = ChromeLightSurface,
    onSurface = ChromeLightText,
    surfaceVariant = Color(0xFFE8EAED),
    onSurfaceVariant = ChromeLightTextSecondary,
    outline = ChromeLightBorder
)

@Composable
fun KopiBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
