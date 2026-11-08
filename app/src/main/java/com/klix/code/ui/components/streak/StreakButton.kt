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
            Image(
                painter = painterResource(id = R.drawable.widget_flame),
                contentDescription = "Streak Flame",
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

    GlassCard(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Flame + Title + Streak count + Claim action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.widget_flame),
                        contentDescription = "Streak Flame",
                        modifier = Modifier
                            .size(34.dp)
                            .scale(flamePulse)
                    )

                    Column {
                        Text(
                            text = "Daily Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "$streakCount ${if (streakCount == 1) "day" else "days"}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Claim status badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (!isClaimedToday) {
                                Brush.horizontalGradient(listOf(FlameOrange, FlameRed))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        GlassSurfaceElevatedColor,
                                        GlassSurfaceElevatedColor
                                    )
                                )
                            }
                        )
                        .glassBorder(CircleShape, strokeWidth = 0.8.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isClaimedToday) {
                        Text(
                            text = "CLAIM",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StreakActiveColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "CLAIMED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StreakActiveColor
                            )
                        }
                    }
                }
            }

            // 7-Day Activity Dots Row (M T W T F S S)
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isActive) StreakActiveColor else DotInactiveColor)
                                .then(
                                    if (isToday && !isActive) {
                                        Modifier.border(1.2.dp, FlameOrange, CircleShape)
                                    } else Modifier
                                )
                        )

                        Text(
                            text = dayLabels[i],
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) (if (isActive) StreakActiveColor else FlameOrange) else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
