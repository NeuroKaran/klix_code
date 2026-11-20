package com.klix.code.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.agent.model.ModelProvider

@Composable
fun OnboardingPage3(
    isDark: Boolean,
    selectedProvider: ModelProvider,
    onSelectProvider: (ModelProvider) -> Unit,
    onConnectKey: () -> Unit,
    onOfflineMode: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasEntered = true
    }

    // Peeking mascot entrance animation
    val mascotOffset by animateDpAsState(
        targetValue = if (hasEntered) 0.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "peekingOffsetP3"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        OnboardingTopBar(
            isDark = isDark,
            onBack = onBack,
            onSkip = onSkip
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Peeking Mascot resting on top of the sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Image(
                painter = painterResource(id = R.drawable.mascot_peeking),
                contentDescription = "Klix Bot Peeking",
                modifier = Modifier
                    .size(width = 110.dp, height = 48.dp)
                    .offset(y = mascotOffset)
            )
        }

        // Elevated Sheet Container (28.dp rounded top corners, 0.dp border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(OnboardingColors.elevatedSheet(isDark))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Headline with Inline Pill: "Paying Rent [ AI Copilot ✦ ] \nFor Your Own AI?"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Paying Rent ",
                        style = OnboardingTypography.HeadlineMedium.copy(
                            color = OnboardingColors.textPrimary(isDark)
                        )
                    )

                    // Inline Pill: [ AI Copilot ✦ ]
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(OnboardingColors.ObsidianInk)
                            .padding(start = 12.dp, end = 6.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AI Copilot",
                                fontFamily = OnboardingFonts.BricolageGrotesque,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = OnboardingColors.LimeAccent,
                                letterSpacing = (-0.4).sp
                            )

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(OnboardingColors.LimeAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✦",
                                    fontSize = 14.sp,
                                    color = OnboardingColors.ObsidianInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "For Your Own AI?",
                    style = OnboardingTypography.HeadlineMedium.copy(
                        color = OnboardingColors.textPrimary(isDark)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Centered Subtitle
                Text(
                    text = "Plug in your own key. Encrypted locally\non-device—zero proxy, zero middleman tax.",
                    style = OnboardingTypography.SubtitleSmall.copy(
                        color = OnboardingColors.textSecondary(isDark)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3 Stacked Borderless Provider Pills
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProviderPillRow(
                        title = "Google Gemini",
                        logoRes = R.drawable.ic_logo_gemini,
                        isSelected = selectedProvider == ModelProvider.GEMINI,
                        isDark = isDark,
                        onClick = { onSelectProvider(ModelProvider.GEMINI) }
                    )

                    ProviderPillRow(
                        title = "Anthropic Claude",
                        logoRes = R.drawable.ic_logo_claude,
                        isSelected = selectedProvider == ModelProvider.ANTHROPIC,
                        isDark = isDark,
                        onClick = { onSelectProvider(ModelProvider.ANTHROPIC) }
                    )

                    ProviderPillRow(
                        title = "OpenAI",
                        logoRes = R.drawable.ic_logo_openai,
                        isSelected = selectedProvider == ModelProvider.OPENAI,
                        isDark = isDark,
                        onClick = { onSelectProvider(ModelProvider.OPENAI) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Trust Vault Card: Shield + AES-256 On-Device Vault • Zero Server Proxy
                TrustVaultCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Rail
        OnboardingProgressRail(
            currentPage = 2,
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Actions: Stacked "Connect My Key »" + "I'll Code Offline First"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OnboardingPillButton(
                text = "Connect My Key",
                onClick = onConnectKey,
                backgroundColor = OnboardingColors.ObsidianInk,
                contentColor = OnboardingColors.WhitePure,
                chevronAccentColor = OnboardingColors.LimeAccent
            )

            // Secondary Action: "I'll Code Offline First"
            Text(
                text = "I'll Code Offline First",
                fontFamily = OnboardingFonts.PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textDecoration = TextDecoration.Underline,
                color = OnboardingColors.textPrimary(isDark).copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOfflineMode() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Stacked Provider Pill Row on Page 3.
 * Features official provider logo, provider title, and status pill ("Selected" vs "Add Key").
 */
@Composable
fun ProviderPillRow(
    title: String,
    logoRes: Int,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "pillScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            OnboardingColors.LimeAccent
        } else {
            OnboardingColors.unselectedCard(isDark)
        },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "pillBgColor"
    )

    val textColor = if (isSelected) OnboardingColors.ObsidianInk else OnboardingColors.textPrimary(isDark)

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Official Vector Provider Logo
                Icon(
                    painter = painterResource(id = logoRes),
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )

                Text(
                    text = title,
                    fontFamily = OnboardingFonts.PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textColor
                )
            }

            // Right Status Pill: "Selected" (white pill) vs "Add Key" (subtle pill)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(OnboardingColors.WhitePure)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Selected",
                        fontFamily = OnboardingFonts.PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = OnboardingColors.ObsidianInk
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1B2030) else Color(0xFFF1F5F9))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Add Key",
                        fontFamily = OnboardingFonts.PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

