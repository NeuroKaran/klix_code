package com.klix.code.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R

@Composable
fun OnboardingPage1(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "page1Motion")

    // Gentle floating idle animation for mascot (0.dp -> -7.dp)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotFloat"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Headline & Subtitle Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // First Headline line with inline pill: "Still Carrying [ </> A Brick? ]"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Still Carrying ",
                    style = OnboardingTypography.HeadlineLarge.copy(
                        color = OnboardingColors.textPrimary(isDark)
                    )
                )

                // Inline Obsidian Pill with Lime circle & text
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(OnboardingColors.ObsidianInk)
                        .padding(start = 4.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Lime circle with </>
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(OnboardingColors.LimeAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "</>",
                                fontFamily = OnboardingFonts.JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = OnboardingColors.ObsidianInk
                            )
                        }

                        Text(
                            text = "A Brick?",
                            fontFamily = OnboardingFonts.BricolageGrotesque,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = OnboardingColors.LimeAccent,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }

            // Second Headline line: "Just To Code?"
            Text(
                text = "Just To Code?",
                style = OnboardingTypography.HeadlineLarge.copy(
                    color = OnboardingColors.textPrimary(isDark)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle
            Text(
                text = "Your laptop is still booting. Meanwhile,\nyour pocket already runs real Linux and AI.",
                style = OnboardingTypography.Subtitle.copy(
                    color = OnboardingColors.textSecondary(isDark)
                )
            )
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // Center Hero Mascot with floating animation & sparkles
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(y = floatOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            // 4 Accent Sparkle stars around mascot
            // Top-left sparkle
            Text(
                text = "✦",
                fontSize = 18.sp,
                color = if (isDark) OnboardingColors.LimeAccent else Color(0xFF84CC16),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 28.dp, y = 40.dp)
                    .size(24.dp)
            )

            // Top-right sparkle
            Text(
                text = "✦",
                fontSize = 16.sp,
                color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF38BDF8),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-32).dp, y = 48.dp)
                    .size(24.dp)
            )

            // Bottom-left sparkle
            Text(
                text = "✦",
                fontSize = 14.sp,
                color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 36.dp, y = (-46).dp)
                    .size(24.dp)
            )

            // Bottom-right sparkle
            Text(
                text = "✦",
                fontSize = 20.sp,
                color = if (isDark) OnboardingColors.LimeAccent else Color(0xFF84CC16),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-28).dp, y = (-38).dp)
                    .size(24.dp)
            )

            // Mascot Typing Image
            Image(
                painter = painterResource(id = R.drawable.mascot_typing),
                contentDescription = "Klix Bot Typing on Laptop",
                modifier = Modifier.size(220.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

