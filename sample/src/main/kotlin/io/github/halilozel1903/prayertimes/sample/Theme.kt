package io.github.halilozel1903.prayertimes.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F6E5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBE2),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF8A6A1F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E3B5),
    onSecondaryContainer = Color(0xFF2B1F00),
    tertiary = Color(0xFF3F5F90),
    background = Color(0xFFF8F6F1),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFF8F6F1),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFE6EAE5),
    onSurfaceVariant = Color(0xFF434944),
    surfaceContainer = Color(0xFFEEEDE7),
    surfaceContainerHigh = Color(0xFFE8E7E1),
    outline = Color(0xFF737973),
    outlineVariant = Color(0xFFC3C8C2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6BF),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFFA2F2DB),
    secondary = Color(0xFFE6C36F),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5B4400),
    onSecondaryContainer = Color(0xFFFFDF9C),
    tertiary = Color(0xFFA8C8FF),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDEE4E0),
    surface = Color(0xFF0F1513),
    onSurface = Color(0xFFDEE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C3),
    surfaceContainer = Color(0xFF1B211F),
    surfaceContainerHigh = Color(0xFF252B29),
    outline = Color(0xFF89938E),
    outlineVariant = Color(0xFF3F4945),
)

@Composable
fun PrayerTimesTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
