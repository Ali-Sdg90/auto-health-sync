package com.autohealthsync.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9DF2DC),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8DF),
    onSecondaryContainer = Color(0xFF06201A),
    tertiary = Color(0xFF416277),
    tertiaryContainer = Color(0xFFC5E7FF),
    background = Color(0xFFF6FAF7),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF6FAF7),
    surfaceContainer = Color(0xFFEAEFEC),
    surfaceContainerHigh = Color(0xFFE4EAE7),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBEC9C4),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81D5C0),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF9DF2DC),
    secondary = Color(0xFFB0CCC3),
    onSecondary = Color(0xFF1C352F),
    secondaryContainer = Color(0xFF334B45),
    onSecondaryContainer = Color(0xFFCCE8DF),
    tertiary = Color(0xFFA8CBE3),
    tertiaryContainer = Color(0xFF294A5E),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF0E1513),
    surfaceContainer = Color(0xFF1A211F),
    surfaceContainerHigh = Color(0xFF242B29),
    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4945),
    error = Color(0xFFFFB4AB),
)

@Composable
fun AutoHealthSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
