package com.klix.code.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class KlixPalette(
    val bgRootGradient: Brush,
    val surfaceSheet: Color,
    val surfaceBentoGroup: Color,
    val surfaceCardItem: Color,
    val surfaceMacroTint: Brush,
    val streakCardGradient: Brush,
    val primaryCtaBg: Color,
    val primaryCtaText: Color,
    val accentLime: Color,
    val accentLimeText: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val borderHairline: Color,
    val borderActiveLime: Color,
    val isDark: Boolean
)

val LightKlixPalette = KlixPalette(
    bgRootGradient = Brush.verticalGradient(
        listOf(Color(0xFFD8EEFE), Color(0xFFE8FBF3), Color(0xFFF8FAFC))
    ),
    surfaceSheet = Color(0xF5F4F8FB),
    surfaceBentoGroup = Color(0xFFE6EEF6),
    surfaceCardItem = Color(0xFFFFFFFF),
    surfaceMacroTint = Brush.linearGradient(
        listOf(Color(0xFFE2F9C8), Color(0xFFD4F5E9))
    ),
    streakCardGradient = Brush.linearGradient(
        listOf(Color(0xFFFFB36B), Color(0xFFD4F87A), Color(0xFF7CE8C8))
    ),
    primaryCtaBg = Color(0xFF0C0D14),
    primaryCtaText = Color(0xFFFFFFFF),
    accentLime = Color(0xFFD5FB66),
    accentLimeText = Color(0xFF4D7C0F),
    inkPrimary = Color(0xFF0F172A),
    inkSecondary = Color(0xFF475569),
    borderHairline = Color(0x99CBD5E1),
    borderActiveLime = Color(0xFF84CC16),
    isDark = false
)

val DarkKlixPalette = KlixPalette(
    bgRootGradient = Brush.verticalGradient(
        listOf(Color(0xFF0B0E1A), Color(0xFF151932), Color(0xFF2A164D))
    ),
    surfaceSheet = Color(0xF5121521),
    surfaceBentoGroup = Color(0xFF1B2030),
    surfaceCardItem = Color(0xFF23293D),
    surfaceMacroTint = Brush.linearGradient(
        listOf(Color(0xFF16302B), Color(0xFF1E2638))
    ),
    streakCardGradient = Brush.linearGradient(
        listOf(Color(0xFF9A521E), Color(0xFF4E6E24), Color(0xFF2B5836))
    ),
    primaryCtaBg = Color(0xFFD5FB66),
    primaryCtaText = Color(0xFF0C0D14),
    accentLime = Color(0xFFD5FB66),
    accentLimeText = Color(0xFF0C0D14),
    inkPrimary = Color(0xFFF8FAFC),
    inkSecondary = Color(0xFF94A3B8),
    borderHairline = Color(0xCC2E364F),
    borderActiveLime = Color(0xFFD5FB66),
    isDark = true
)

val LocalKlixPalette = staticCompositionLocalOf { LightKlixPalette }

val KlixBgRootGradient: Brush
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.bgRootGradient

val KlixSurfaceSheet: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.surfaceSheet

val KlixSurfaceBentoGroup: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.surfaceBentoGroup

val KlixSurfaceCardItem: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.surfaceCardItem

val KlixSurfaceMacroTint: Brush
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.surfaceMacroTint

val KlixStreakCardGradient: Brush
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.streakCardGradient

val KlixPrimaryCtaBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.primaryCtaBg

val KlixPrimaryCtaText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.primaryCtaText

val KlixAccentLime: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.accentLime

val KlixAccentLimeText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.accentLimeText

val KlixInkPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.inkPrimary

val KlixInkSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.inkSecondary

val KlixBorderHairline: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.borderHairline

val KlixBorderActiveLime: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalKlixPalette.current.borderActiveLime
