package com.klix.code.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.LocalIsDarkTheme
import kotlin.math.sin

/**
 * Shared Minimal Drifting Ambient Background across all 4 pages.
 * 2 ultra-soft radial gradient orbs (#D5FB66 at 16% alpha, #7DD3FC at 14% alpha)
 * drifting smoothly with infiniteTransition.
 */
@Composable
fun DriftingAmbientBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambientDrift")

    val driftPhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val driftPhase2 by infiniteTransition.animateFloat(
        initialValue = 3.14159f,
        targetValue = 9.42477f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val orb1Color = Color(0xFFD5FB66).copy(alpha = if (isDark) 0.12f else 0.18f)
    val orb2Color = Color(0xFF7DD3FC).copy(alpha = if (isDark) 0.10f else 0.16f)

    Box(modifier = modifier.fillMaxSize()) {
        // Base linear gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OnboardingColors.backgroundGradient(isDark))
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Lime orb drifting in top-left quadrant
            val orb1X = width * 0.28f + (kotlin.math.cos(driftPhase1.toDouble()) * 48).toFloat()
            val orb1Y = height * 0.32f + (sin(driftPhase1.toDouble()) * 36).toFloat()
            val orb1Radius = width * 0.55f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1Color, Color.Transparent),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius
            )

            // Orb 2: Sky-blue orb drifting in bottom-right quadrant
            val orb2X = width * 0.72f + (sin(driftPhase2.toDouble()) * 52).toFloat()
            val orb2Y = height * 0.65f + (kotlin.math.cos(driftPhase2.toDouble()) * 40).toFloat()
            val orb2Radius = width * 0.60f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2Color, Color.Transparent),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            )
        }
    }
}

/**
 * Top App Bar for onboarding screens.
 * Features optional back arrow on the left and optional underlined "Skip" on the right.
 * Applies statusBarsPadding to cleanly clear the notification bar and notch/cutout.
 */
@Composable
fun OnboardingTopBar(
    isDark: Boolean,
    onBack: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnboardingColors.textPrimary(isDark),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        if (onSkip != null) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClick = onSkip,
                        role = Role.Button
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Skip",
                    fontFamily = OnboardingFonts.PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textDecoration = TextDecoration.Underline,
                    color = OnboardingColors.textPrimary(isDark).copy(alpha = 0.85f)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

/**
 * 4-dot Progress Rail.
 * Active dot expands into a 32.dp pill, inactive dots are 8.dp circles.
 */
@Composable
fun OnboardingProgressRail(
    currentPage: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val isActive = i == currentPage
            val width: Dp by animateDpAsState(
                targetValue = if (isActive) 30.dp else 7.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "dotWidth"
            )

            val color = if (isActive) {
                if (isDark) OnboardingColors.LimeAccent else OnboardingColors.ObsidianInk
            } else {
                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
            }

            Box(
                modifier = Modifier
                    .size(width = width, height = 7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Primary Pill Call-To-Action Button with smooth press bounce and chevron.
 */
@Composable
fun OnboardingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = OnboardingColors.ObsidianInk,
    contentColor: Color = OnboardingColors.WhitePure,
    chevronAccentColor: Color = OnboardingColors.LimeAccent,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 400f
        ),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(58.dp)
            .clip(CircleShape)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = text,
                    fontFamily = OnboardingFonts.PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contentColor
                )

                // Double Chevrons »
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = chevronAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = chevronAccentColor,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = (-10).dp)
                    )
                }
            }
        }
    }
}

/**
 * Trust Vault Card on Page 3.
 * Borderless soft green container highlighting on-device Keystore encryption.
 */
@Composable
fun TrustVaultCard(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = OnboardingColors.trustVaultBg(isDark)
    val inkColor = OnboardingColors.trustVaultInk(isDark)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_icon_shield_check),
            contentDescription = "Shield",
            tint = inkColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "AES-256 On-Device Vault • Zero Server Proxy",
            fontFamily = OnboardingFonts.PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = inkColor
        )
    }
}

/**
 * Secure Dialog to enter API Key for chosen AI provider.
 * Follows the borderless soft-tonal Bento Card design language:
 * - RoundedCornerShape(26.dp) elevated sheet with no border
 * - Official provider vector brand badge
 * - Inner borderless bento group container for API key input with JetBrains Mono font
 * - AES-256 On-Device Keystore Vault trust pill
 * - Thumb-friendly tactile pill action buttons with pressEffect
 */
@Composable
fun ApiKeyInputDialog(
    provider: ModelProvider,
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    isDark: Boolean = LocalIsDarkTheme.current
) {
    var keyText by remember(initialKey) { mutableStateOf(initialKey) }
    var passwordVisible by remember { mutableStateOf(false) }

    val providerLogoRes = when (provider) {
        ModelProvider.GEMINI -> R.drawable.ic_logo_gemini
        ModelProvider.ANTHROPIC -> R.drawable.ic_logo_claude
        ModelProvider.OPENAI -> R.drawable.ic_logo_openai
        ModelProvider.CUSTOM_OPENAI_COMPATIBLE -> R.drawable.ic_logo_openrouter
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Bento Card Container (NO BORDER)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(OnboardingColors.elevatedSheet(isDark))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume inner click
                    )
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row: Brand Logo, Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Official Provider Logo in soft bento container
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(OnboardingColors.bentoGroup(isDark)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = providerLogoRes),
                                    contentDescription = provider.displayName,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Connect ${provider.displayName}",
                                    fontFamily = OnboardingFonts.BricolageGrotesque,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = (-0.4).sp,
                                    color = OnboardingColors.textPrimary(isDark)
                                )
                                Text(
                                    text = "Bring Your Own Key (BYOK)",
                                    fontFamily = OnboardingFonts.JetBrainsMono,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = if (isDark) OnboardingColors.LimeAccent else Color(0xFF10A37F)
                                )
                            }
                        }

                        // Close icon button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OnboardingColors.bentoGroup(isDark))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = OnboardingColors.textSecondary(isDark),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Description
                    Text(
                        text = "Enter your API key below. It will be encrypted into Android Keystore and never leaves your device.",
                        fontFamily = OnboardingFonts.PlusJakartaSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.5.sp,
                        lineHeight = 19.5.sp,
                        color = OnboardingColors.textSecondary(isDark)
                    )

                    // Inner Bento Input Box (NO BORDER)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(OnboardingColors.bentoGroup(isDark))
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = keyText,
                                onValueChange = { keyText = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = OnboardingFonts.JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = OnboardingColors.textPrimary(isDark)
                                ),
                                cursorBrush = SolidColor(if (isDark) OnboardingColors.LimeAccent else OnboardingColors.ObsidianInk),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (keyText.isEmpty()) {
                                            Text(
                                                text = "Paste key here...",
                                                fontFamily = OnboardingFonts.PlusJakartaSans,
                                                fontSize = 14.sp,
                                                color = if (isDark) OnboardingColors.DarkTextMuted else OnboardingColors.LightTextMuted
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide key" else "Show key",
                                    tint = if (isDark) OnboardingColors.DarkTextMuted else OnboardingColors.LightTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Trust Vault Badge (Bento Pill)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OnboardingColors.trustVaultBg(isDark))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_icon_shield_check),
                            contentDescription = "Shield",
                            tint = OnboardingColors.trustVaultInk(isDark),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "AES-256 On-Device Keystore • Zero Server Proxy",
                            fontFamily = OnboardingFonts.PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = OnboardingColors.trustVaultInk(isDark)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Bottom Action Buttons: Cancel and Save & Connect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Pill Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(CircleShape)
                                .background(OnboardingColors.bentoGroup(isDark))
                                .pressEffect(0.96f)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = OnboardingFonts.PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp,
                                color = OnboardingColors.textPrimary(isDark)
                            )
                        }

                        // Save & Connect Pill Button
                        val saveBgColor = if (isDark) OnboardingColors.LimeAccent else OnboardingColors.ObsidianInk
                        val saveTextColor = if (isDark) OnboardingColors.ObsidianInk else OnboardingColors.WhitePure
                        val saveEnabled = keyText.trim().isNotEmpty()

                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .clip(CircleShape)
                                .background(if (saveEnabled) saveBgColor else saveBgColor.copy(alpha = 0.5f))
                                .pressEffect(0.96f)
                                .clickable(enabled = saveEnabled) {
                                    onSave(keyText.trim())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save & Connect",
                                fontFamily = OnboardingFonts.PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = saveTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

