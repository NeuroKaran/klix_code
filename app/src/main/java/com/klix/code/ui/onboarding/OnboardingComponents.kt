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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.agent.model.ModelProvider
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnboardingColors.textPrimary(isDark),
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(38.dp))
        }

        if (onSkip != null) {
            Text(
                text = "Skip",
                fontFamily = OnboardingFonts.PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textDecoration = TextDecoration.Underline,
                color = OnboardingColors.textPrimary(isDark).copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSkip() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(38.dp))
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
 */
@Composable
fun ApiKeyInputDialog(
    provider: ModelProvider,
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var keyText by remember(initialKey) { mutableStateOf(initialKey) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Connect ${provider.displayName}",
                    fontFamily = OnboardingFonts.BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your API key below. It will be encrypted into Android Keystore and never leaves your device.",
                    fontFamily = OnboardingFonts.PlusJakartaSans,
                    fontSize = 13.5.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    placeholder = { Text("Paste key here...", fontSize = 14.sp) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide key" else "Show key",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(keyText) }
            ) {
                Text(
                    text = "Save & Connect",
                    fontFamily = OnboardingFonts.PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10A37F)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = OnboardingFonts.PlusJakartaSans)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
