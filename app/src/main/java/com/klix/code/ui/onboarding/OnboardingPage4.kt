package com.klix.code.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingPage4(
    isDark: Boolean,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "page4Celebration")

    // Joyful floating bounce
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrationFloat"
    )

    // Starburst rotation
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRotate"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar (with back arrow)
        OnboardingTopBar(
            isDark = isDark,
            onBack = onBack,
            onSkip = null
        )

        Spacer(modifier = Modifier.weight(0.3f))

        // Upper Stage: Celebration Mascot & Dynamic Star Accents
        Box(
            modifier = Modifier
                .size(310.dp)
                .offset(y = floatOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            // Top-Left Blue Bursting Star Accent
            CornerBurstStar(
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF60A5FA),
                points = 12,
                outerRadius = 42f,
                innerRadius = 22f,
                rotationDegrees = starRotation,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 10.dp, y = 20.dp)
                    .size(72.dp)
            )

            // Top-Right Lime / Emerald Bursting Star Accent
            CornerBurstStar(
                color = if (isDark) OnboardingColors.LimeAccent else Color(0xFF84CC16),
                points = 14,
                outerRadius = 52f,
                innerRadius = 26f,
                rotationDegrees = -starRotation * 0.8f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = 14.dp)
                    .size(88.dp)
            )

            // Sparkle Stars
            Text(
                text = "✦",
                fontSize = 20.sp,
                color = Color(0xFFFACC15),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 12.dp, y = (-40).dp)
            )

            Text(
                text = "✦",
                fontSize = 16.sp,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = 10.dp)
            )

            // Mascot Celebration Image
            Image(
                painter = painterResource(id = R.drawable.mascot_celebration),
                contentDescription = "Klix Bot Excuses Deleted Celebration",
                modifier = Modifier.size(230.dp)
            )
        }

        Spacer(modifier = Modifier.weight(0.4f))

        // Centered Copy: Zero Checklists, Zero Bluff
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Excuses Deleted.",
                style = OnboardingTypography.HeadlineLarge.copy(
                    color = OnboardingColors.textPrimary(isDark)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No desk? No laptop? No problem. Small steps\nbuild big things—your pocket studio is live.",
                style = OnboardingTypography.Subtitle.copy(
                    color = OnboardingColors.textSecondary(isDark)
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Progress Rail (dot 4 active)
        OnboardingProgressRail(
            currentPage = 3,
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom CTA: "Let's Ship Something »"
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            OnboardingPillButton(
                text = "Let's Ship Something",
                onClick = onComplete,
                backgroundColor = OnboardingColors.ObsidianInk,
                contentColor = OnboardingColors.WhitePure,
                chevronAccentColor = OnboardingColors.LimeAccent
            )
        }
    }
}

/**
 * Procedural Starburst graphic for corner celebration accents.
 */
@Composable
fun CornerBurstStar(
    color: Color,
    points: Int,
    outerRadius: Float,
    innerRadius: Float,
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path()

        val rotRad = (rotationDegrees * PI / 180.0).toFloat()
        val totalSteps = points * 2
        val stepAngle = (2.0 * PI / totalSteps).toFloat()

        for (i in 0 until totalSteps) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * stepAngle + rotRad
            val x = cx + (r * cos(angle.toDouble())).toFloat()
            val y = cy + (r * sin(angle.toDouble())).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        drawPath(path = path, color = color)
    }
}

