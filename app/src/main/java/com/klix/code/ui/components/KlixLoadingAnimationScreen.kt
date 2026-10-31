package com.klix.code.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import kotlinx.coroutines.delay

private val SplashBackgroundColor = Color(0xFF0E1118)
private val AccentLime = Color(0xFF00E676)
private val ProgressTrackColor = Color(0xFF1B2230)

/**
 * Native Jetpack Compose startup loading screen displaying the circular Klix app icon,
 * ambient pulsing glow, progressive environment initialization status, and instant tap-to-skip.
 */
@Composable
fun KlixLoadingAnimationScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFinished by remember { mutableStateOf(false) }

    val safeFinish = remember(onAnimationFinished) {
        {
            if (!isFinished) {
                isFinished = true
                onAnimationFinished()
            }
        }
    }

    // Progress animation from 0% to 100%
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
        delay(120)
        safeFinish()
    }

    // Subtle pulsing ambient animation for the circular logo
    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    val currentProgress = progress.value
    val statusText = when {
        currentProgress < 0.35f -> "Initializing Alpine Linux sandbox..."
        currentProgress < 0.70f -> "Mounting /workspace storage..."
        currentProgress < 0.95f -> "Preparing code workspace..."
        else -> "Klix Code Ready"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                safeFinish()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Circular Logo with subtle ambient glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                // Outer ambient radial glow
                Box(
                    modifier = Modifier
                        .size((128 * glowScale).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AccentLime.copy(alpha = 0.32f),
                                    AccentLime.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // App's circular icon
                Image(
                    painter = painterResource(id = R.drawable.klix_logo),
                    contentDescription = "Klix App Icon",
                    modifier = Modifier
                        .size(108.dp)
                        .scale(logoScale)
                        .clip(CircleShape)
                        .border(2.dp, AccentLime.copy(alpha = 0.5f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "Klix code",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "IDE ANYWHERE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentLime,
                letterSpacing = 2.5.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Progress Bar
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AccentLime,
                trackColor = ProgressTrackColor,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Status message and percentage
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${(currentProgress * 100).toInt()}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = AccentLime.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }

        // Tap-to-skip button in the top corner for instant IDE access
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = { safeFinish() },
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33FFFFFF),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Loading Animation",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
