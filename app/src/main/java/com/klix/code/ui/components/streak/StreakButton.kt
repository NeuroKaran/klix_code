package com.klix.code.ui.components.streak

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.ui.components.glass.GlassCard
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.components.glass.tintedGlassBorder
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.LimeBrand
import com.klix.code.ui.theme.LimeGlow
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

private val FlameOrange = Color(0xFFFF9800)
private val FlameRed = Color(0xFFFF5722)
private val DotInactiveColor: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF2B313E) else Color(0xFFD0D7DE)

private val StreakActiveColor: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) LimeBrand else Color(0xFF15803D)

/**
 * Compact Glass Streak Pill Button for the TopAppBar.
 * Displays the flame and streak count, with a glowing pulse when today's streak is ready to claim.
 */
@Composable
fun StreakPillButton(
    streakCount: Int,
    isClaimedToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_pill_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (!isClaimedToday) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (!isClaimedToday) 0.95f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_glow"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(GlassSurfaceColor)
            .then(
                if (!isClaimedToday) {
                    Modifier.tintedGlassBorder(FlameOrange, CircleShape, strokeWidth = 1.dp, highlightAlpha = borderGlow)
                } else {
                    Modifier.glassBorder(CircleShape, strokeWidth = 1.dp, highlightAlpha = 0.28f, midAlpha = 0.10f)
                }
            )
            .pressEffect(targetScale = 0.92f)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak Flame",
                tint = FlameOrange,
                modifier = Modifier
                    .size(16.dp)
                    .scale(if (!isClaimedToday) pulseScale else 1.0f)
            )

            Text(
                text = "$streakCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isClaimedToday) TextPrimary else FlameOrange
            )

            if (!isClaimedToday) {
                // Subtle glowing notification dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FlameOrange)
                )
            }
        }
    }
}

/**
 * Modular Glass Streak Card for the Sidebar / File Tree (Reference 1 style).
 */
@Composable
fun StreakSidebarCard(
    streakCount: Int,
    isClaimedToday: Boolean,
    weeklyActive: BooleanArray,
    todayIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    val infiniteTransition = rememberInfiniteTransition(label = "sidebar_streak_flame")
    val flamePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (!isClaimedToday) 1.14f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(com.klix.code.ui.theme.KlixStreakCardGradient)
            .border(1.dp, com.klix.code.ui.theme.KlixBorderHairline, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.96f)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mascot streak fire (cheering 2D Klix Bot in vector flame aura)
            Image(
                painter = painterResource(id = R.drawable.mascot_streak_fire),
                contentDescription = "Streak Fire Mascot",
                modifier = Modifier
                    .size(76.dp)
                    .scale(flamePulse)
            )

            // Center Title: "7 Days Streak" (20.sp ExtraBold)
            Text(
                text = "$streakCount Days Streak",
                style = com.klix.code.ui.theme.KlixTypography.sheetHeader,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = com.klix.code.ui.theme.KlixInkPrimary,
                letterSpacing = (-0.4).sp
            )

            // 7-Column weekly strip (M T W T F S S) with #D5FB66 circular checkmark badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 7) {
                    val isActive = if (i < weeklyActive.size) weeklyActive[i] else false
                    val isToday = i == todayIndex

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = dayLabels[i],
                            style = com.klix.code.ui.theme.KlixTypography.codePill,
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) com.klix.code.ui.theme.KlixInkPrimary else com.klix.code.ui.theme.KlixInkSecondary
                        )

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFFD5FB66) else Color(0x33000000))
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) Color(0xFFD5FB66) else Color(0x44CBD5E1),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0C0D14),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
