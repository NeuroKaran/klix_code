package com.klix.code

import android.content.Context
import android.content.SharedPreferences
import com.klix.code.ui.theme.ThemeManager
import com.klix.code.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ThemeManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)

        ThemeManager.resetForTest()
    }

    @Test
    fun testDefaultThemeModeIsLight() {
        // App must be light themed as default per user requirement
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)
    }

    @Test
    fun testThemeModeDisplayNames() {
        assertEquals("Light", ThemeMode.LIGHT.displayName)
        assertEquals("Dark", ThemeMode.DARK.displayName)
        assertEquals("System Default", ThemeMode.SYSTEM.displayName)
    }

    @Test
    fun testSetThemeModeUpdatesStateAndPersists() {
        ThemeManager.setThemeMode(mockContext, ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, ThemeManager.themeMode.value)
        verify(mockPrefs).edit()
        verify(mockEditor).putString(ThemeManager.KEY_THEME_MODE, ThemeMode.DARK.name)
        verify(mockEditor).apply()

        // Switch to SYSTEM
        ThemeManager.setThemeMode(mockContext, ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode.value)

        // Switch back to LIGHT
        ThemeManager.setThemeMode(mockContext, ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)
    }

    @Test
    fun testInitLoadsSavedThemeFromPreferences() {
        `when`(mockPrefs.getString(ThemeManager.KEY_THEME_MODE, ThemeMode.LIGHT.name))
            .thenReturn(ThemeMode.DARK.name)

        ThemeManager.init(mockContext)
        assertEquals(ThemeMode.DARK, ThemeManager.themeMode.value)
    }

    @Test
    fun testInitFallsBackToLightWhenNoSavedPreferenceOrCorrupted() {
        `when`(mockPrefs.getString(ThemeManager.KEY_THEME_MODE, ThemeMode.LIGHT.name))
            .thenReturn("INVALID_THEME_STRING")

        ThemeManager.init(mockContext)
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)

        `when`(mockPrefs.getString(ThemeManager.KEY_THEME_MODE, ThemeMode.LIGHT.name))
            .thenReturn(null)

        ThemeManager.init(mockContext)
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)
    }

    @Test
    fun testIsDarkThemeLogicAcrossModes() {
        // 1. LIGHT mode: Always false regardless of system dark mode
        ThemeManager.setThemeMode(mockContext, ThemeMode.LIGHT)
        assertFalse(ThemeManager.isDarkTheme(systemInDark = false))
        assertFalse(ThemeManager.isDarkTheme(systemInDark = true))

        // 2. DARK mode: Always true regardless of system dark mode
        ThemeManager.setThemeMode(mockContext, ThemeMode.DARK)
        assertTrue(ThemeManager.isDarkTheme(systemInDark = false))
        assertTrue(ThemeManager.isDarkTheme(systemInDark = true))

        // 3. SYSTEM mode: Follows system dark mode setting
        ThemeManager.setThemeMode(mockContext, ThemeMode.SYSTEM)
        assertFalse(ThemeManager.isDarkTheme(systemInDark = false))
        assertTrue(ThemeManager.isDarkTheme(systemInDark = true))
    }

    @Test
    fun testPaletteInvariants() {
        assertFalse(com.klix.code.ui.theme.LightKlixColors.isDark)
        assertTrue(com.klix.code.ui.theme.DarkKlixColors.isDark)

        // Light theme background must differ from dark theme background
        org.junit.Assert.assertNotEquals(
            com.klix.code.ui.theme.LightKlixColors.background,
            com.klix.code.ui.theme.DarkKlixColors.background
        )
        // Light theme text primary must differ from dark theme text primary
        org.junit.Assert.assertNotEquals(
            com.klix.code.ui.theme.LightKlixColors.textPrimary,
            com.klix.code.ui.theme.DarkKlixColors.textPrimary
        )
    }
}

