package com.klix.code.ui.components.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.klix.code.ui.theme.LocalKlixColors

// Smooth physics specifications for high-frame, natural animations
val NaturalSpringFloat = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val NaturalSpringOffset = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Specular rim highlight border simulating liquid glass refraction.
 * Adapts automatically to dark and light modes for crisp definition.
 */
@Composable
fun Modifier.glassBorder(
    shape: Shape,
    strokeWidth: Dp = 1.dp,
    highlightAlpha: Float = 0.28f,
    midAlpha: Float = 0.10f,
    shadowAlpha: Float = 0.03f
): Modifier {
    val isDark = LocalKlixColors.current.isDark
    val brush = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.White.copy(alpha = highlightAlpha),
            0.4f to Color.White.copy(alpha = midAlpha),
            1.0f to Color.White.copy(alpha = shadowAlpha)
        )
    } else {
        Brush.linearGradient(
            0.0f to Color.White.copy(alpha = 0.85f),
            0.4f to Color(0x3564748B),
            1.0f to Color(0x1F0F172A)
        )
    }
    return this.border(
        width = strokeWidth,
        brush = brush,
        shape = shape
    )
}

/**
 * Colored specular rim highlight border with custom glow tint.
 */
fun Modifier.tintedGlassBorder(
    tintColor: Color,
    shape: Shape,
    strokeWidth: Dp = 1.dp,
    highlightAlpha: Float = 0.45f,
    baseAlpha: Float = 0.18f
): Modifier = this.border(
    width = strokeWidth,
    brush = Brush.linearGradient(
        0.0f to tintColor.copy(alpha = highlightAlpha),
        0.5f to tintColor.copy(alpha = baseAlpha),
        1.0f to Color.Transparent
    ),
    shape = shape
)

/**
 * Tactile spring scale physics effect that reacts naturally to pointer presses.
 */
@Composable
fun Modifier.pressEffect(targetScale: Float = 0.96f): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = NaturalSpringFloat,
        label = "pressEffectScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

