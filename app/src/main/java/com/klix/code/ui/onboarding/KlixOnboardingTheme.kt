package com.klix.code.ui.onboarding

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.klix.code.R

object OnboardingFonts {
    val BricolageGrotesque = FontFamily(
        Font(R.font.bricolage_grotesque, FontWeight.Normal),
        Font(R.font.bricolage_grotesque, FontWeight.Medium),
        Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
        Font(R.font.bricolage_grotesque, FontWeight.Bold),
        Font(R.font.bricolage_grotesque, FontWeight.ExtraBold)
    )

    val PlusJakartaSans = FontFamily(
        Font(R.font.plus_jakarta_sans, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans, FontWeight.Bold)
    )

    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono, FontWeight.Medium),
        Font(R.font.jetbrains_mono, FontWeight.SemiBold),
        Font(R.font.jetbrains_mono, FontWeight.Bold)
    )
}

object OnboardingColors {
    // Shared Brand Accent
    val LimeAccent = Color(0xFFD5FB66)
    val ObsidianInk = Color(0xFF0C0D14)
    val WhitePure = Color(0xFFFFFFFF)

    // Light Theme
    val LightBgTop = Color(0xFFD8EEFE)
    val LightBgMid = Color(0xFFE8FBF3)
    val LightBgBottom = Color(0xFFF6F9FC)
    val LightElevatedSheet = Color(0xFFF6F9FC)
    val LightBentoGroup = Color(0xFFE7EEF5)
    val LightUnselectedCard = Color(0xFFFFFFFF)
    val LightTrustVaultBg = Color(0xFFE6F7ED)
    val LightTrustVaultInk = Color(0xFF14532D)
    val LightTextMuted = Color(0xFF6B7280)
    val LightTextSecondary = Color(0xFF4B5563)

    // Dark Theme
    val DarkBgTop = Color(0xFF0B0E1A)
    val DarkBgMid = Color(0xFF141933)
    val DarkBgBottom = Color(0xFF121521)
    val DarkElevatedSheet = Color(0xFF121521)
    val DarkBentoGroup = Color(0xFF1B2030)
    val DarkUnselectedCard = Color(0xFF23293D)
    val DarkTrustVaultBg = Color(0xFF122923)
    val DarkTrustVaultInk = Color(0xFF86EFAC)
    val DarkTextMuted = Color(0xFF94A3B8)
    val DarkTextSecondary = Color(0xFFCBD5E1)

    fun backgroundGradient(isDark: Boolean): Brush {
        return if (isDark) {
            Brush.verticalGradient(listOf(DarkBgTop, DarkBgMid, DarkBgBottom))
        } else {
            Brush.verticalGradient(listOf(LightBgTop, LightBgMid, LightBgBottom))
        }
    }

    fun elevatedSheet(isDark: Boolean) = if (isDark) DarkElevatedSheet else LightElevatedSheet
    fun bentoGroup(isDark: Boolean) = if (isDark) DarkBentoGroup else LightBentoGroup
    fun unselectedCard(isDark: Boolean) = if (isDark) DarkUnselectedCard else LightUnselectedCard
    fun trustVaultBg(isDark: Boolean) = if (isDark) DarkTrustVaultBg else LightTrustVaultBg
    fun trustVaultInk(isDark: Boolean) = if (isDark) DarkTrustVaultInk else LightTrustVaultInk
    fun textPrimary(isDark: Boolean) = if (isDark) WhitePure else ObsidianInk
    fun textSecondary(isDark: Boolean) = if (isDark) DarkTextSecondary else LightTextSecondary
}

object OnboardingTypography {
    val HeadlineLarge = TextStyle(
        fontFamily = OnboardingFonts.BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    )

    val HeadlineMedium = TextStyle(
        fontFamily = OnboardingFonts.BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.8).sp
    )

    val Subtitle = TextStyle(
        fontFamily = OnboardingFonts.PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    )

    val SubtitleSmall = TextStyle(
        fontFamily = OnboardingFonts.PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    )

    val CardTitle = TextStyle(
        fontFamily = OnboardingFonts.PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    )

    val CardSubtitle = TextStyle(
        fontFamily = OnboardingFonts.JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    val ButtonText = TextStyle(
        fontFamily = OnboardingFonts.PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    )

    val InlinePillText = TextStyle(
        fontFamily = OnboardingFonts.BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.5).sp
    )

    val CodeTag = TextStyle(
        fontFamily = OnboardingFonts.JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    )
}

