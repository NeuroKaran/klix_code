package com.klix.code.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

object ThemeManager {
    const val PREFS_NAME = "klix_theme_prefs"
    const val KEY_THEME_MODE = "app_theme_mode"

    // Default to LIGHT mode per requirement: "make our app light themed as default"
    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name)
        val mode = try {
            ThemeMode.valueOf(savedName ?: ThemeMode.LIGHT.name)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }
        _themeMode.value = mode
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun isDarkTheme(systemInDark: Boolean): Boolean {
        return when (_themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemInDark
        }
    }

    /**
     * Resets theme state to default (LIGHT). Useful for test fixtures.
     */
    fun resetForTest() {
        _themeMode.value = ThemeMode.LIGHT
    }
}

