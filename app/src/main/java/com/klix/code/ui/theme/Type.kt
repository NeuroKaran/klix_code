package com.klix.code.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.klix.code.R

/**
 * Klix Code Core Font Families.
 *
 * Senior Design System Typography Pairing:
 * - Bricolage Grotesque: Expressive, mechanical-grotesque display typeface for headers, titles, and hero metrics.
 * - Plus Jakarta Sans: Clean, humanist-geometric sans-serif for UI copy, dialogs, buttons, and high-density readability.
 * - JetBrains Mono: Developer-first monospace for code, syntax blocks, keyboard shortcuts, and terminal logs.
 */
object KlixFontFamilies {
    val BricolageGrotesque = FontFamily(
        Font(R.font.bricolage_grotesque, FontWeight.Light),
        Font(R.font.bricolage_grotesque, FontWeight.Normal),
        Font(R.font.bricolage_grotesque, FontWeight.Medium),
        Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
        Font(R.font.bricolage_grotesque, FontWeight.Bold),
        Font(R.font.bricolage_grotesque, FontWeight.ExtraBold),
        Font(R.font.bricolage_grotesque, FontWeight.Black)
    )

    val PlusJakartaSans = FontFamily(
        Font(R.font.plus_jakarta_sans, FontWeight.Light),
        Font(R.font.plus_jakarta_sans, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans, FontWeight.Bold),
        Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
    )

    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Light),
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono, FontWeight.Medium),
        Font(R.font.jetbrains_mono, FontWeight.SemiBold),
        Font(R.font.jetbrains_mono, FontWeight.Bold),
        Font(R.font.jetbrains_mono, FontWeight.ExtraBold)
    )
}

val BricolageGrotesque = KlixFontFamilies.BricolageGrotesque
val PlusJakartaSans = KlixFontFamilies.PlusJakartaSans
val JetBrainsMono = KlixFontFamilies.JetBrainsMono

val Typography = Typography(
    // --- DISPLAY STYLES (Bricolage Grotesque) ---
    displayLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp
    ),
    displaySmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),

    // --- HEADLINE STYLES (Bricolage Grotesque) ---
    headlineLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp
    ),

    // --- TITLE STYLES (Bricolage Grotesque for section headers, Plus Jakarta Sans for UI components) ---
    titleLarge = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.25).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.15).sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 18.5.sp,
        letterSpacing = 0.sp
    ),

    // --- BODY STYLES (Plus Jakarta Sans) ---
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.5.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),

    // --- LABEL STYLES (Plus Jakarta Sans & JetBrains Mono) ---
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)
