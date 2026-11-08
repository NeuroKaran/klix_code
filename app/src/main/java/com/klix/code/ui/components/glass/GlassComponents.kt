package com.klix.code.ui.components.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.ui.theme.GlassBorderColor
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfacePillColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.LocalKlixColors
import com.klix.code.ui.theme.OnAccentPrimaryColor
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

/**
 * Modular Frosted Glass Surface with specular rim lighting and subtle ambient depth.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurfaceColor,
    strokeWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalKlixColors.current.isDark
    val clickModifier = if (onClick != null) {
        Modifier
            .pressEffect(0.97f)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.05f else 0.45f),
                        Color.Transparent
                    )
                )
            )
            .glassBorder(shape = shape, strokeWidth = strokeWidth)
            .then(clickModifier),
        content = content
    )
}

/**
 * Modular Card container with generous rounded corners and breathable inner spacing.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurfaceColor,
    strokeWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        strokeWidth = strokeWidth,
        onClick = onClick,
        content = content
    )
}

/**
 * Tactile Pill Button with rounded ends, spring press physics, and modern glass / brand styling.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LimeBrandColor,
    contentColor: Color = OnAccentPrimaryColor,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.4f),
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier
            .pressEffect(targetScale = 0.95f)
            .glassBorder(shape = shape, strokeWidth = 1.dp, highlightAlpha = 0.35f, midAlpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Circular Glass Icon Button with specular rim and tactile feedback.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = GlassSurfaceColor,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .glassBorder(CircleShape, strokeWidth = 1.dp)
            .pressEffect(0.92f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Compact pill badge for statuses, counts, and tags.
 */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = GlassSurfaceColor,
    textColor: Color = TextPrimary,
    glowBorderColor: Color? = null
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (glowBorderColor != null) {
                    Modifier.tintedGlassBorder(glowBorderColor, CircleShape, strokeWidth = 1.dp)
                } else {
                    Modifier.glassBorder(CircleShape, strokeWidth = 0.8.dp)
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

/**
 * Liquid glass segmented control (Reference 2 style) with gliding specular lens.
 */
@Composable
fun <T> GlassSegmentedControl(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String
) {
    if (items.isEmpty()) return

    val isDark = LocalKlixColors.current.isDark
    val clampedIndex = selectedIndex.coerceIn(0, items.size - 1)

    val animatedIndex by animateFloatAsState(
        targetValue = clampedIndex.toFloat(),
        animationSpec = NaturalSpringFloat,
        label = "glassSliderIndex"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(GlassSurfacePillColor)
            .glassBorder(CircleShape, strokeWidth = 1.dp)
            .padding(4.dp)
    ) {
        // Gliding liquid glass lens
        val totalItems = items.size
        val fraction = 1f / totalItems

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = animatedIndex * (this.size.width)
                }
                .clip(CircleShape)
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0xFFF1F5F9)
                            )
                        )
                    }
                )
                .glassBorder(
                    CircleShape,
                    strokeWidth = 1.2.dp,
                    highlightAlpha = if (isDark) 0.45f else 0.85f,
                    midAlpha = if (isDark) 0.15f else 0.35f
                )
        )

        // Labels row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == clampedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Floating Dock capsule container (Reference 1 & 2 style) for navigation and toolbars.
 */
@Composable
fun GlassDock(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(GlassSurfacePillColor)
            .glassBorder(shape, strokeWidth = 1.dp, highlightAlpha = 0.35f, midAlpha = 0.12f)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}
