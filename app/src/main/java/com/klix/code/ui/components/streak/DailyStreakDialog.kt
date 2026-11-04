package com.klix.code.ui.components.streak

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.stats.StreakManager
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val KlixNeonLime = Color(0xFFD4FF32)
private val FlameOrange = Color(0xFFFF9800)
private val FlameDeepOrange = Color(0xFFFF5722)
private val FlameGold = Color(0xFFFFD54F)
private val DotInactiveColor = Color(0xFF2B313E)

private enum class StreakAnimStage {
    IDLE,
    BLOW_UP,
    MORPH_TO_DOT,
    FLY_TO_STREAK,
    COMPLETED
}

private data class FireParticle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val color: Color
)

private data class ConfettiParticle(
    val xOffset: Float,
    val yOffset: Float,
    val size: Float,
    val color: Color
)

@Composable
fun DailyStreakDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val streakState by StreakManager.streakState.collectAsState()
    var displayedStreakCount by remember(streakState.streakCount) {
        mutableIntStateOf(streakState.streakCount)
    }

    // Refresh at 12 midnight timer & 1-second countdown ticker
    var millisUntilMidnight by remember {
        mutableLongStateOf(StreakManager.getMillisUntilNextMidnight())
    }

    LaunchedEffect(Unit) {
        StreakManager.refresh(context)
        while (isActive) {
            delay(1000L)
            val remaining = StreakManager.getMillisUntilNextMidnight()
            millisUntilMidnight = remaining
            if (remaining <= 1000L) {
                // Wait for midnight to pass then refresh
                delay(1200L)
                StreakManager.refresh(context)
            }
        }
    }

    var animStage by remember {
        mutableStateOf(if (streakState.isClaimedToday) StreakAnimStage.COMPLETED else StreakAnimStage.IDLE)
    }

    // Coordinates tracking for dynamic dot flight animation
    var dialogRootPosition by remember { mutableStateOf(Offset.Zero) }
    var fireCenterPosition by remember { mutableStateOf(Offset.Zero) }
    var targetSlotPosition by remember { mutableStateOf(Offset.Zero) }

    // Animatable values
    val fireScale = remember { Animatable(1.0f) }
    val fireAlpha = remember { Animatable(1.0f) }
    val fireExplosionProgress = remember { Animatable(0.0f) }
    val dotScale = remember { Animatable(0.0f) }
    val dotAlpha = remember { Animatable(0.0f) }
    val flyProgress = remember { Animatable(0.0f) }
    val slotShockwaveScale = remember { Animatable(1.0f) }
    val slotShockwaveAlpha = remember { Animatable(0.0f) }
    val slotScale = remember { Animatable(1.0f) }
    val counterScale = remember { Animatable(1.0f) }
    val confettiProgress = remember { Animatable(0.0f) }

    // Fiery blast particles
    val fireParticles = remember {
        List(30) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val dist = with(density) { (Random.nextInt(55, 120)).dp.toPx() }
            val sz = with(density) { (Random.nextInt(4, 9)).dp.toPx() }
            val colors = listOf(FlameGold, FlameOrange, FlameDeepOrange, Color.White, KlixNeonLime)
            FireParticle(angle, dist, sz, colors.random())
        }
    }

    // Celebration confetti particles
    val confettiParticles = remember {
        List(24) {
            val xOff = with(density) { (Random.nextInt(-140, 140)).dp.toPx() }
            val yOff = with(density) { (Random.nextInt(-60, 40)).dp.toPx() }
            val sz = with(density) { (Random.nextInt(3, 7)).dp.toPx() }
            val colors = listOf(KlixNeonLime, FlameGold, FlameOrange, Color.White, Color(0xFF64B5F6))
            ConfettiParticle(xOff, yOff, sz, colors.random())
        }
    }

    // Idle breathing animation for the flame
    val infiniteTransition = rememberInfiniteTransition(label = "dialog_flame_idle")
    val idleFlameScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_idle_scale"
    )
    val idleGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_idle_alpha"
    )

    // Trigger Claim Streak Sequence
    val triggerClaimSequence: () -> Unit = {
        if (animStage == StreakAnimStage.IDLE && !streakState.isClaimedToday) {
            scope.launch {
                // Phase 1: BLOW UP! The fire explodes outwards with intense particles
                animStage = StreakAnimStage.BLOW_UP
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                launch {
                    fireScale.animateTo(
                        targetValue = 2.4f,
                        animationSpec = tween(420, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    fireExplosionProgress.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    )
                }
                delay(380)

                // Phase 2: Fire shrinks/implodes into a dot!
                animStage = StreakAnimStage.MORPH_TO_DOT
                launch {
                    fireScale.animateTo(
                        targetValue = 0.0f,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    )
                    fireAlpha.snapTo(0.0f)
                }
                delay(120)

                // Dot pops into existence at center of flame
                dotAlpha.snapTo(1.0f)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                dotScale.animateTo(
                    targetValue = 1.5f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                dotScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(120, easing = LinearEasing)
                )

                // Phase 3: The Dot flies into the streak row!
                animStage = StreakAnimStage.FLY_TO_STREAK
                flyProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(520, easing = FastOutSlowInEasing)
                )

                // Phase 4: Dot lands in today's slot! Impact, shockwave, counter punch & celebration
                animStage = StreakAnimStage.COMPLETED
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                // Immediately claim in manager
                val newCount = StreakManager.claimDailyStreak(context)
                displayedStreakCount = newCount

                // Shockwave ripple at slot
                slotShockwaveAlpha.snapTo(0.85f)
                launch {
                    slotShockwaveScale.animateTo(
                        targetValue = 2.8f,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    )
                    slotShockwaveAlpha.animateTo(
                        targetValue = 0.0f,
                        animationSpec = tween(250, easing = LinearEasing)
                    )
                }

                // Slot bounce
                launch {
                    slotScale.animateTo(
                        targetValue = 1.45f,
                        animationSpec = tween(140, easing = FastOutSlowInEasing)
                    )
                    slotScale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                // Counter text punch (+1)
                launch {
                    counterScale.animateTo(
                        targetValue = 1.35f,
                        animationSpec = tween(160, easing = FastOutSlowInEasing)
                    )
                    counterScale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                // Confetti burst
                launch {
                    confettiProgress.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(700, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Main Dialog Card
            Box(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click inside dialog
                    )
                    .onGloballyPositioned { coords ->
                        dialogRootPosition = coords.boundsInRoot().topLeft
                    }
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Bar: Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = FlameOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "DAILY STREAK",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp,
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Central Fire Stage & Animation Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient Radial Glow
                        val glowScale = when (animStage) {
                            StreakAnimStage.BLOW_UP -> 2.2f * fireScale.value
                            else -> idleFlameScale
                        }
                        val glowAlpha = when (animStage) {
                            StreakAnimStage.BLOW_UP -> (1.0f - fireExplosionProgress.value * 0.4f).coerceIn(0f, 1f)
                            StreakAnimStage.MORPH_TO_DOT, StreakAnimStage.FLY_TO_STREAK -> 0f
                            else -> idleGlowAlpha
                        }

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(glowScale)
                                .alpha(glowAlpha)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            FlameOrange.copy(alpha = 0.55f),
                                            FlameDeepOrange.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        // Fiery explosion particles canvas
                        if (animStage == StreakAnimStage.BLOW_UP || fireExplosionProgress.value > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val prog = fireExplosionProgress.value
                                for (p in fireParticles) {
                                    val dist = p.distance * prog
                                    val x = center.x + cos(p.angle) * dist
                                    val y = center.y + sin(p.angle) * dist
                                    val alpha = (1.0f - prog).coerceIn(0f, 1f)
                                    val sz = (p.size * (1.0f - prog * 0.4f)).coerceAtLeast(1.5f)
                                    drawCircle(
                                        color = p.color.copy(alpha = alpha),
                                        radius = sz,
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }

                        // Central Flame
                        if (animStage != StreakAnimStage.FLY_TO_STREAK && (fireAlpha.value > 0f || animStage == StreakAnimStage.COMPLETED)) {
                            val activeScale = when (animStage) {
                                StreakAnimStage.BLOW_UP, StreakAnimStage.MORPH_TO_DOT -> fireScale.value
                                StreakAnimStage.COMPLETED -> idleFlameScale
                                else -> idleFlameScale
                            }

                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .scale(activeScale)
                                    .alpha(if (animStage == StreakAnimStage.COMPLETED) 1.0f else fireAlpha.value)
                                    .clickable(
                                        enabled = animStage == StreakAnimStage.IDLE && !streakState.isClaimedToday,
                                        onClick = triggerClaimSequence
                                    )
                                    .onGloballyPositioned { coords ->
                                        val rootBounds = coords.boundsInRoot()
                                        fireCenterPosition = Offset(
                                            rootBounds.left + rootBounds.width / 2f - dialogRootPosition.x,
                                            rootBounds.top + rootBounds.height / 2f - dialogRootPosition.y
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.widget_flame),
                                    contentDescription = "Flame",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // The Morphing Dot (appears after fire collapses)
                        if (animStage == StreakAnimStage.MORPH_TO_DOT && dotAlpha.value > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(dotScale.value)
                                    .alpha(dotAlpha.value)
                                    .clip(CircleShape)
                                    .background(KlixNeonLime)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }

                        // Flying Dot (travels from flame center to today's slot)
                        if (animStage == StreakAnimStage.FLY_TO_STREAK) {
                            val p = flyProgress.value
                            val start = fireCenterPosition
                            // If target slot wasn't measured, fallback to straight down
                            val target = if (targetSlotPosition != Offset.Zero) {
                                targetSlotPosition
                            } else {
                                Offset(start.x, start.y + with(density) { 150.dp.toPx() })
                            }

                            // Curved arc path: parabolic hump upwards
                            val arcOffset = with(density) { (-32).dp.toPx() } * sin((p * PI).toFloat())
                            val currentX = start.x + (target.x - start.x) * p
                            val currentY = start.y + (target.y - start.y) * p + arcOffset

                            // Trailing particles canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                for (i in 1..4) {
                                    val trailP = (p - i * 0.08f).coerceIn(0f, 1f)
                                    val trailArc = arcOffset * (trailP / p.coerceAtLeast(0.01f))
                                    val trailX = start.x + (target.x - start.x) * trailP
                                    val trailY = start.y + (target.y - start.y) * trailP + trailArc
                                    val trailAlpha = (1f - i * 0.22f) * 0.7f
                                    drawCircle(
                                        color = KlixNeonLime.copy(alpha = trailAlpha),
                                        radius = 6f - i * 1f,
                                        center = Offset(trailX, trailY)
                                    )
                                }
                            }

                            // The traveling dot
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (currentX - with(density) { 8.dp.toPx() }).roundToInt(),
                                            (currentY - with(density) { 8.dp.toPx() }).roundToInt()
                                        )
                                    }
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(KlixNeonLime)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }

                    // Streak Count & Motivation Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$displayedStreakCount ${if (displayedStreakCount == 1) "day" else "days"}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (animStage == StreakAnimStage.COMPLETED || streakState.isClaimedToday) {
                                KlixNeonLime
                            } else {
                                TextPrimary
                            },
                            modifier = Modifier.scale(counterScale.value)
                        )

                        Text(
                            text = if (animStage == StreakAnimStage.COMPLETED || streakState.isClaimedToday) {
                                "You're on fire! Daily login recorded."
                            } else {
                                "Tap fire or claim button to add to your streak!"
                            },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 7-Day Activity Matrix (Matching Widget_UI reference)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.75f))
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                            .padding(vertical = 14.dp, horizontal = 12.dp)
                    ) {
                        // Celebration Confetti Canvas
                        if (animStage == StreakAnimStage.COMPLETED && confettiProgress.value in 0.01f..0.99f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val prog = confettiProgress.value
                                for (c in confettiParticles) {
                                    val x = center.x + c.xOffset * prog
                                    val y = center.y + c.yOffset * prog
                                    val alpha = (1.0f - prog).coerceIn(0f, 1f)
                                    drawCircle(
                                        color = c.color.copy(alpha = alpha),
                                        radius = c.size * (1f - prog * 0.3f),
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }

                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 7) {
                                val isToday = i == streakState.todayIndex
                                val isDayActive = if (isToday) {
                                    animStage == StreakAnimStage.COMPLETED || streakState.isClaimedToday
                                } else {
                                    if (i < streakState.weeklyActive.size) streakState.weeklyActive[i] else false
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Shockwave ripple on today's slot upon impact
                                        if (isToday && slotShockwaveAlpha.value > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .scale(slotShockwaveScale.value)
                                                    .alpha(slotShockwaveAlpha.value)
                                                    .border(2.dp, KlixNeonLime, CircleShape)
                                            )
                                        }

                                        // The Dot Slot
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .then(if (isToday) Modifier.scale(slotScale.value) else Modifier)
                                                .then(
                                                    if (isToday) {
                                                        Modifier.onGloballyPositioned { coords ->
                                                            val bounds = coords.boundsInRoot()
                                                            targetSlotPosition = Offset(
                                                                bounds.left + bounds.width / 2f - dialogRootPosition.x,
                                                                bounds.top + bounds.height / 2f - dialogRootPosition.y
                                                            )
                                                        }
                                                    } else Modifier
                                                )
                                                .clip(CircleShape)
                                                .background(if (isDayActive) KlixNeonLime else DotInactiveColor)
                                                .then(
                                                    if (isToday && !isDayActive) {
                                                        Modifier.border(1.5.dp, FlameOrange, CircleShape)
                                                    } else Modifier
                                                )
                                        )
                                    }

                                    Text(
                                        text = dayLabels[i],
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isToday) {
                                            if (isDayActive) KlixNeonLime else FlameOrange
                                        } else {
                                            TextMuted
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action / Midnight Refresh Info
                    val isClaimed = animStage == StreakAnimStage.COMPLETED || streakState.isClaimedToday

                    if (!isClaimed) {
                        Button(
                            onClick = triggerClaimSequence,
                            enabled = animStage == StreakAnimStage.IDLE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlameOrange
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "CLAIM TODAY'S STREAK",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = KlixNeonLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Claimed for today!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KlixNeonLime
                                )
                            }

                            // Midnight countdown indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Refreshes at 12 midnight (${StreakManager.formatTimeRemaining(millisUntilMidnight)})",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

