package com.klix.code.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Static Color Definitions - Dark Palette
// ==========================================
val DarkBgValue = Color(0xFF121418)
val DarkSurfaceValue = Color(0xFF181B20)
val DarkSurfaceVariantValue = Color(0xFF22262E)
val DarkBorderValue = Color(0xFF2E3440)

val DarkAccentPrimaryValue = Color(0xFF3B82F6)
val DarkAccentSecondaryValue = Color(0xFF10B981)
val DarkAccentWarningValue = Color(0xFFF59E0B)
val DarkAccentErrorValue = Color(0xFFEF4444)

val DarkTextPrimaryValue = Color(0xFFE6EDF3)
val DarkTextSecondaryValue = Color(0xFF8B949E)
val DarkTextMutedValue = Color(0xFF6E7681)

val DarkDiffAddedBgValue = Color(0x2E10B981)
val DarkDiffAddedTextValue = Color(0xFF34D399)
val DarkDiffRemovedBgValue = Color(0x2EEF4444)
val DarkDiffRemovedTextValue = Color(0xFFF87171)

val DarkTabActiveBgValue = Color(0xFF1E232B)
val DarkTabInactiveBgValue = Color(0xFF14171C)

// ==========================================
// Static Color Definitions - Light Palette
// ==========================================
val LightBgValue = Color(0xFFF6F8FA)
val LightSurfaceValue = Color(0xFFFFFFFF)
val LightSurfaceVariantValue = Color(0xFFEEF2F6)
val LightBorderValue = Color(0xFFD0D7DE)

val LightAccentPrimaryValue = Color(0xFF0969DA)
val LightAccentSecondaryValue = Color(0xFF1A7F37)
val LightAccentWarningValue = Color(0xFF9A6700)
val LightAccentErrorValue = Color(0xFFCF222E)

val LightTextPrimaryValue = Color(0xFF1F2328)
val LightTextSecondaryValue = Color(0xFF57606A)
val LightTextMutedValue = Color(0xFF8C959F)

val LightDiffAddedBgValue = Color(0x262EA043)
val LightDiffAddedTextValue = Color(0xFF1A7F37)
val LightDiffRemovedBgValue = Color(0x26CF222E)
val LightDiffRemovedTextValue = Color(0xFFCF222E)

val LightTabActiveBgValue = Color(0xFFFFFFFF)
val LightTabInactiveBgValue = Color(0xFFEAEEF2)

// ==========================================
// Theme Color Container & Composition Locals
// ==========================================
class KlixColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentWarning: Color,
    val accentError: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val diffAddedBg: Color,
    val diffAddedText: Color,
    val diffRemovedBg: Color,
    val diffRemovedText: Color,
    val tabActiveBg: Color,
    val tabInactiveBg: Color,
    val isDark: Boolean
)

val DarkKlixColors = KlixColors(
    background = DarkBgValue,
    surface = DarkSurfaceValue,
    surfaceVariant = DarkSurfaceVariantValue,
    border = DarkBorderValue,
    accentPrimary = DarkAccentPrimaryValue,
    accentSecondary = DarkAccentSecondaryValue,
    accentWarning = DarkAccentWarningValue,
    accentError = DarkAccentErrorValue,
    textPrimary = DarkTextPrimaryValue,
    textSecondary = DarkTextSecondaryValue,
    textMuted = DarkTextMutedValue,
    diffAddedBg = DarkDiffAddedBgValue,
    diffAddedText = DarkDiffAddedTextValue,
    diffRemovedBg = DarkDiffRemovedBgValue,
    diffRemovedText = DarkDiffRemovedTextValue,
    tabActiveBg = DarkTabActiveBgValue,
    tabInactiveBg = DarkTabInactiveBgValue,
    isDark = true
)

val LightKlixColors = KlixColors(
    background = LightBgValue,
    surface = LightSurfaceValue,
    surfaceVariant = LightSurfaceVariantValue,
    border = LightBorderValue,
    accentPrimary = LightAccentPrimaryValue,
    accentSecondary = LightAccentSecondaryValue,
    accentWarning = LightAccentWarningValue,
    accentError = LightAccentErrorValue,
    textPrimary = LightTextPrimaryValue,
    textSecondary = LightTextSecondaryValue,
    textMuted = LightTextMutedValue,
    diffAddedBg = LightDiffAddedBgValue,
    diffAddedText = LightDiffAddedTextValue,
    diffRemovedBg = LightDiffRemovedBgValue,
    diffRemovedText = LightDiffRemovedTextValue,
    tabActiveBg = LightTabActiveBgValue,
    tabInactiveBg = LightTabInactiveBgValue,
    isDark = false
)

val LocalKlixColors = staticCompositionLocalOf { LightKlixColors }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

// ==========================================
// Dynamic Theme Token Accessors
// (Seamless backward-compatible drop-in for all UI composables)
// ==========================================
val DarkBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.background

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.surface

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.surfaceVariant

val DarkBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.border

val AccentPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.accentPrimary

val AccentSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.accentSecondary

val AccentWarning: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.accentWarning

val AccentError: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.accentError

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.textMuted

val DiffAddedBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.diffAddedBg

val DiffAddedText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.diffAddedText

val DiffRemovedBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.diffRemovedBg

val DiffRemovedText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.diffRemovedText

val TabActiveBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.tabActiveBg

val TabInactiveBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.tabInactiveBg
