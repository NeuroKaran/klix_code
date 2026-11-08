package com.klix.code.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccentPrimaryValue,
    secondary = DarkAccentSecondaryValue,
    background = DarkBgValue,
    surface = DarkSurfaceValue,
    surfaceVariant = DarkSurfaceVariantValue,
    onPrimary = ObsidianBase,
    onSecondary = ObsidianBase,
    onBackground = DarkTextPrimaryValue,
    onSurface = DarkTextPrimaryValue,
    onSurfaceVariant = DarkTextSecondaryValue,
    outline = DarkBorderValue,
    error = DarkAccentErrorValue
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentPrimaryValue,
    secondary = LightAccentSecondaryValue,
    background = LightBgValue,
    surface = LightSurfaceValue,
    surfaceVariant = LightSurfaceVariantValue,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightTextPrimaryValue,
    onSurface = LightTextPrimaryValue,
    onSurfaceVariant = LightTextSecondaryValue,
    outline = LightBorderValue,
    error = LightAccentErrorValue
)

@Composable
fun KlixTheme(
    themeMode: ThemeMode = ThemeManager.themeMode.collectAsState().value,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val klixColors = if (isDark) DarkKlixColors else LightKlixColors
    val materialColors = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
                window.statusBarColor = klixColors.background.toArgb()
                window.navigationBarColor = klixColors.background.toArgb()
            }
        }
    }

    CompositionLocalProvider(
        LocalKlixColors provides klixColors,
        LocalIsDarkTheme provides isDark
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
