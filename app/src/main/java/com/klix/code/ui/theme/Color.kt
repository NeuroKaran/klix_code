package com.klix.code.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Klix Electric Lime Scale (Primary Brand)
// ==========================================
val Lime50 = Color(0xFFF9FFE5)
val Lime100 = Color(0xFFF2FFB8)
val Lime200 = Color(0xFFEAFF80)
val Lime300 = Color(0xFFE0FF4D)
val Lime400 = Color(0xFFD4FF32) // Core Brand Identity
val LimeBrand = Lime400
val Lime500 = Color(0xFFB2E600)
val Lime600 = Color(0xFF88B300)
val Lime700 = Color(0xFF628000)
val Lime800 = Color(0xFF3E5200)
val Lime900 = Color(0xFF222D00)

val LimeGlow = Color(0x59D4FF32)    // 35% alpha glow
val LimeDim = Color(0x1FD4FF32)     // 12% alpha container
val LimeSubtle = Color(0x0DD4FF32)  // 5% alpha ambient tint
val LimeBorder = Color(0x40D4FF32)  // 25% alpha glass border

// ==========================================
// 2. Klix Electric Cyan Scale (Secondary Accent)
// ==========================================
val Cyan50 = Color(0xFFF0F9FF)
val Cyan100 = Color(0xFFE0F2FE)
val Cyan200 = Color(0xFFBAE6FD)
val Cyan300 = Color(0xFF7DD3FC)
val Cyan400 = Color(0xFF38BDF8) // Core Tech Accent
val CyanBrand = Cyan400
val Cyan500 = Color(0xFF0EA5E9)
val Cyan600 = Color(0xFF0284C7)
val Cyan700 = Color(0xFF0369A1)
val Cyan800 = Color(0xFF075985)
val Cyan900 = Color(0xFF0C4A6E)

val CyanGlow = Color(0x4D38BDF8)    // 30% alpha glow
val CyanDim = Color(0x1A38BDF8)     // 10% alpha container
val CyanSubtle = Color(0x0A38BDF8)  // 4% alpha ambient tint
val CyanBorder = Color(0x3338BDF8)  // 20% alpha glass border

// ==========================================
// 3. Deep Obsidian Neutrals (Modern Dark Base)
// ==========================================
val ObsidianBase = Color(0xFF07080C)       // Deep space obsidian black
val ObsidianSurface = Color(0xFF0E121A)    // Deep modular card surface
val ObsidianElevated = Color(0xFF151C28)   // Elevated sheets & modals
val ObsidianBorder = Color(0xFF1F293D)     // Crisp card edge

// ==========================================
// 4. Glass UI Tokens
// ==========================================
val GlassSurface = Color(0xB30E131E)          // 70% opacity frosted glass surface
val GlassSurfaceElevated = Color(0xCC161E2D)  // 80% opacity elevated glass sheet
val GlassSurfacePill = Color(0xE6131A26)      // 90% opacity floating dock pill
val GlassBorder = Color(0x1FFFFFFF)           // 12% specular rim border
val GlassBorderHighlight = Color(0x40FFFFFF)  // 25% specular top highlight

val LightBase = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2F6)
val LightBorder = Color(0xFFD0D7DE)
val LightGlassSurface = Color(0xCCFFFFFF)
val LightGlassElevated = Color(0xF0FFFFFF)
val LightGlassPill = Color(0xF2F1F5F9)
val LightGlassBorder = Color(0x40D0D7DE)
val LightGlassBorderHighlight = Color(0x80FFFFFF)

// Static Theme Color Definitions - Dark Palette
val DarkBgValue = ObsidianBase
val DarkSurfaceValue = ObsidianSurface
val DarkSurfaceVariantValue = ObsidianElevated
val DarkBorderValue = ObsidianBorder

val DarkAccentPrimaryValue = LimeBrand
val DarkAccentSecondaryValue = CyanBrand
val DarkAccentWarningValue = Color(0xFFF59E0B)
val DarkAccentErrorValue = Color(0xFFEF4444)

val DarkTextPrimaryValue = Color(0xFFE6EDF3)
val DarkTextSecondaryValue = Color(0xFF8B949E)
val DarkTextMutedValue = Color(0xFF6E7681)

val DarkDiffAddedBgValue = Color(0x2E10B981)
val DarkDiffAddedTextValue = Color(0xFF34D399)
val DarkDiffRemovedBgValue = Color(0x2EEF4444)
val DarkDiffRemovedTextValue = Color(0xFFF87171)

val DarkTabActiveBgValue = Color(0x331E293B)
val DarkTabInactiveBgValue = Color(0x140E131E)

// Static Theme Color Definitions - Light Palette
val LightBgValue = LightBase
val LightSurfaceValue = LightSurface
val LightSurfaceVariantValue = LightSurfaceVariant
val LightBorderValue = LightBorder

val LightAccentPrimaryValue = Color(0xFF65A30D) // Deep lime for high readability
val LightAccentSecondaryValue = Color(0xFF0284C7) // Cyan 600
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
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassSurfacePill: Color,
    val glassBorder: Color,
    val glassBorderHighlight: Color,
    val limePrimary: Color,
    val cyanAccent: Color,
    val onAccentPrimary: Color,
    val limeGlow: Color,
    val cyanGlow: Color,
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
    glassSurface = GlassSurface,
    glassSurfaceElevated = GlassSurfaceElevated,
    glassSurfacePill = GlassSurfacePill,
    glassBorder = GlassBorder,
    glassBorderHighlight = GlassBorderHighlight,
    limePrimary = LimeBrand,
    cyanAccent = CyanBrand,
    onAccentPrimary = ObsidianBase,
    limeGlow = LimeGlow,
    cyanGlow = CyanGlow,
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
    glassSurface = LightGlassSurface,
    glassSurfaceElevated = LightGlassElevated,
    glassSurfacePill = LightGlassPill,
    glassBorder = LightGlassBorder,
    glassBorderHighlight = LightGlassBorderHighlight,
    limePrimary = LightAccentPrimaryValue,
    cyanAccent = LightAccentSecondaryValue,
    onAccentPrimary = Color.White,
    limeGlow = Color(0x3365A30D),
    cyanGlow = Color(0x330284C7),
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

val GlassSurfaceColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.glassSurface

val GlassSurfaceElevatedColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.glassSurfaceElevated

val GlassSurfacePillColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.glassSurfacePill

val GlassBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.glassBorder

val GlassBorderHighlightColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.glassBorderHighlight

val LimeBrandColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.limePrimary

val CyanBrandColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.cyanAccent

val OnAccentPrimaryColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.onAccentPrimary

val LimeGlowColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.limeGlow

val CyanGlowColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixColors.current.cyanGlow

val LimeDimColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalKlixColors.current.isDark) LimeDim else Color(0x2465A30D)

val CyanDimColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalKlixColors.current.isDark) CyanDim else Color(0x1F0284C7)

val LimeBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalKlixColors.current.isDark) LimeBorder else Color(0x4D65A30D)

val CyanBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalKlixColors.current.isDark) CyanBorder else Color(0x4D0284C7)
