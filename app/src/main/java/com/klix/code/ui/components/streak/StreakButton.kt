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
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixSurfaceCardItem
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

    val pillShape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(pillShape)
            .background(KlixSurfaceCardItem)
            .then(
                if (!isClaimedToday) {
                    Modifier.tintedGlassBorder(FlameOrange, pillShape, strokeWidth = 1.dp, highlightAlpha = borderGlow)
                } else {
                    Modifier.glassBorder(pillShape, strokeWidth = 1.dp, highlightAlpha = 0.28f, midAlpha = 0.10f)
                }
            )
            .pressEffect(targetScale = 0.92f)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_icon_flame),
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
                color = if (isClaimedToday) KlixInkPrimary else FlameOrange
            )

            if (!isClaimedToday) {
                // Subtle glowing notification dot
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(FlameOrange)
                )
            }
        }
    }
}

/**
 * Bento Streak Card for the Sidebar / File Tree matching Reference Image.
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
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 34.dp)
    ) {
        // Peeking mascot in the BACK side (rendered before card so card covers lower body)
        Image(
            painter = painterResource(id = R.drawable.mascot_peeking),
            contentDescription = "Streak Mascot",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 40.dp)
                .offset(y = (-48).dp)
                .size(64.dp)
        )

        // Main Bento Card in the FRONT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF1E2536) else Color.White)
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color(0x33CBD5E1) else Color(0x15000000),
                    shape = RoundedCornerShape(26.dp)
                )
                .clickable(onClick = onClick)
                .pressEffect(0.96f)
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header row: "CODING STREAK" on left, "Active" pill on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CODING STREAK",
                        style = com.klix.code.ui.theme.KlixTypography.codePill,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF1E293B)
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isClaimedToday || streakCount > 0) "Active" else "Claim",
                            style = com.klix.code.ui.theme.KlixTypography.codePill,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB5FA36)
                        )
                    }
                }

                // Title row: "3 Days Streak 🔥"
                Text(
                    text = "$streakCount Days Streak \uD83D\uDD25",
                    style = com.klix.code.ui.theme.KlixTypography.sheetHeader,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 7-Column weekly strip (M T W T F S S) with #B5FA36 squircle checkmark boxes
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = dayLabels[i],
                                style = com.klix.code.ui.theme.KlixTypography.codePill,
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                            )

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (isActive) Color(0xFFB5FA36)
                                        else if (isDark) Color(0xFF2B3346)
                                        else Color(0xFFBAC7DF)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF0C0D14),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
