package com.klix.code.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R

@Composable
fun OnboardingPage2(
    isDark: Boolean,
    selectedTools: Set<ToolchainOption>,
    installState: SandboxInstallState,
    onToggleTool: (ToolchainOption) -> Unit,
    onInstallAndContinue: () -> Unit,
    onRetry: () -> Unit,
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
        label = "peekingOffset"
    )

    val isInstalling = installState is SandboxInstallState.DownloadingRootFs ||
        installState is SandboxInstallState.ExtractingRootFs ||
        installState is SandboxInstallState.InstallingPackages

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        OnboardingTopBar(
            isDark = isDark,
            onBack = if (!isInstalling) onBack else null,
            onSkip = if (!isInstalling) onSkip else null
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
                // Centered Headline with Inline Pill: "Phones [ No Root -> ] \nCan't Compile?"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Phones ",
                        style = OnboardingTypography.HeadlineMedium.copy(
                            color = OnboardingColors.textPrimary(isDark)
                        )
                    )

                    // Inline Pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(OnboardingColors.ObsidianInk)
                            .padding(start = 12.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No Root",
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
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = OnboardingColors.ObsidianInk,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Can't Compile?",
                    style = OnboardingTypography.HeadlineMedium.copy(
                        color = OnboardingColors.textPrimary(isDark)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Centered Subtitle
                Text(
                    text = "Cute assumption. Pick your real toolchains—\ninstalled locally, no root needed.",
                    style = OnboardingTypography.SubtitleSmall.copy(
                        color = OnboardingColors.textSecondary(isDark)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Inner Bento Container (22.dp radius, #E7EEF5 light, #1B2030 dark, 0.dp border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(OnboardingColors.bentoGroup(isDark))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Row 1: Python 3 and Node.js
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ToolchainCard(
                                tool = ToolchainOption.PYTHON,
                                isSelected = selectedTools.contains(ToolchainOption.PYTHON),
                                isDark = isDark,
                                isEnabled = !isInstalling,
                                onToggle = { onToggleTool(ToolchainOption.PYTHON) },
                                modifier = Modifier.weight(1f)
                            )
                            ToolchainCard(
                                tool = ToolchainOption.NODEJS,
                                isSelected = selectedTools.contains(ToolchainOption.NODEJS),
                                isDark = isDark,
                                isEnabled = !isInstalling,
                                onToggle = { onToggleTool(ToolchainOption.NODEJS) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2: Git and C / C++
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ToolchainCard(
                                tool = ToolchainOption.GIT,
                                isSelected = selectedTools.contains(ToolchainOption.GIT),
                                isDark = isDark,
                                isEnabled = !isInstalling,
                                onToggle = { onToggleTool(ToolchainOption.GIT) },
                                modifier = Modifier.weight(1f)
                            )
                            ToolchainCard(
                                tool = ToolchainOption.CPP,
                                isSelected = selectedTools.contains(ToolchainOption.CPP),
                                isDark = isDark,
                                isEnabled = !isInstalling,
                                onToggle = { onToggleTool(ToolchainOption.CPP) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Real-time Install Progress Banner if active
                AnimatedVisibility(
                    visible = isInstalling || installState is SandboxInstallState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (installState) {
                            is SandboxInstallState.DownloadingRootFs -> {
                                Text(
                                    text = installState.statusMessage,
                                    fontFamily = OnboardingFonts.PlusJakartaSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnboardingColors.textPrimary(isDark)
                                )
                                LinearProgressIndicator(
                                    progress = { installState.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = OnboardingColors.LimeAccent,
                                    trackColor = if (isDark) Color(0xFF23293D) else Color(0xFFCBD5E1)
                                )
                            }
                            is SandboxInstallState.ExtractingRootFs -> {
                                Text(
                                    text = installState.statusMessage,
                                    fontFamily = OnboardingFonts.PlusJakartaSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnboardingColors.textPrimary(isDark)
                                )
                                LinearProgressIndicator(
                                    progress = { installState.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = OnboardingColors.LimeAccent,
                                    trackColor = if (isDark) Color(0xFF23293D) else Color(0xFFCBD5E1)
                                )
                            }
                            is SandboxInstallState.InstallingPackages -> {
                                Text(
                                    text = installState.statusMessage,
                                    fontFamily = OnboardingFonts.PlusJakartaSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnboardingColors.textPrimary(isDark)
                                )
                                LinearProgressIndicator(
                                    progress = { installState.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = OnboardingColors.LimeAccent,
                                    trackColor = if (isDark) Color(0xFF23293D) else Color(0xFFCBD5E1)
                                )
                            }
                            is SandboxInstallState.Error -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = installState.message,
                                        fontFamily = OnboardingFonts.PlusJakartaSans,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFEF4444)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                            .clickable { onRetry() }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Retry",
                                            fontFamily = OnboardingFonts.PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Rail
        OnboardingProgressRail(
            currentPage = 1,
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom CTA Button: "Install & Continue »"
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            OnboardingPillButton(
                text = if (isInstalling) "Provisioning Alpine..." else "Install & Continue",
                onClick = onInstallAndContinue,
                isLoading = isInstalling,
                backgroundColor = OnboardingColors.ObsidianInk,
                contentColor = OnboardingColors.WhitePure,
                chevronAccentColor = OnboardingColors.LimeAccent
            )
        }
    }
}

/**
 * 2x2 Bento Toolchain Card.
 * Toggles selection with smooth scale bounce and morphs to LimeAccent with checkmark badge.
 */
@Composable
fun ToolchainCard(
    tool: ToolchainOption,
    isSelected: Boolean,
    isDark: Boolean,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "cardScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            OnboardingColors.LimeAccent
        } else {
            OnboardingColors.unselectedCard(isDark)
        },
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "cardBgColor"
    )

    val primaryTextColor = if (isSelected) OnboardingColors.ObsidianInk else OnboardingColors.textPrimary(isDark)
    val subtitleTextColor = if (isSelected) Color(0xFF263238) else OnboardingColors.textSecondary(isDark)

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(1.08f)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onToggle
            )
            .padding(12.dp)
    ) {
        // Checkmark badge in top-right when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(OnboardingColors.ObsidianInk),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = OnboardingColors.WhitePure,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official Toolchain Brand Logo
            Icon(
                painter = painterResource(id = tool.logoRes),
                contentDescription = tool.title,
                tint = Color.Unspecified, // Render official brand colors
                modifier = Modifier.size(46.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tool.title,
                fontFamily = OnboardingFonts.PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = primaryTextColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = tool.subtitle,
                fontFamily = OnboardingFonts.JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = subtitleTextColor
            )
        }
    }
}
