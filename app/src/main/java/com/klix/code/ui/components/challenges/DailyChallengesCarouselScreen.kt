package com.klix.code.ui.components.challenges

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.klix.code.R
import com.klix.code.data.challenges.ChallengeDifficulty
import com.klix.code.data.challenges.ChallengeLanguage
import com.klix.code.data.challenges.ChallengeProfileState
import com.klix.code.data.challenges.ChallengeProgress
import com.klix.code.data.challenges.CodingChallenge
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixBgRootGradient
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme
import kotlin.math.absoluteValue

/**
 * Daily Challenges Carousel Screen (Design 2):
 * - Soft pastel gradient background
 * - Top-left Points pill + Top-right circular Close (X) button
 * - Centered "Daily Challenges" title and "Swipe to pick • X of Y" subtitle
 * - HorizontalPager with adjacent peeking pastel cards, active white elevated card,
 *   peeking Klix Bot mascot, language logo, difficulty & time pill, code preview block,
 *   and full-width lime "Solve Now" CTA pill
 * - Animated progress dot strip & "Swipe cards to explore" footer
 */
@Composable
fun DailyChallengesCarouselScreen(
    challenges: List<CodingChallenge>,
    profileState: ChallengeProfileState,
    initialChallengeIndex: Int = 0,
    onSolveChallenge: (CodingChallenge) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val safeInitial = initialChallengeIndex.coerceIn(0, (challenges.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { challenges.size }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KlixBgRootGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Row: Points Pill on Left + Circular Close Button on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Points Badge Pill
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(21.dp),
                            ambientColor = Color(0x180F172A),
                            spotColor = Color(0x140F172A)
                        )
                        .clip(RoundedCornerShape(21.dp))
                        .background(if (isDark) Color(0xFF1E2638) else Color.White)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Points",
                            tint = if (isDark) Color(0xFFD5FB66) else Color(0xFFEAB308),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "${profileState.totalPoints} pts",
                            style = KlixTypography.cardTitle,
                            fontWeight = FontWeight.Bold,
                            color = KlixInkPrimary
                        )
                    }
                }

                // Close (X) Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x180F172A),
                            spotColor = Color(0x140F172A)
                        )
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E2638) else Color.White)
                        .clickable(onClick = onClose)
                        .pressEffect(0.92f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Daily Challenges",
                        tint = KlixInkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Centered Header
            Text(
                text = "Daily Challenges",
                style = KlixTypography.displayHero.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.7).sp
                ),
                color = KlixInkPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            val total = challenges.size.coerceAtLeast(1)
            val currentHumanIndex = (pagerState.currentPage + 1).coerceAtMost(total)
            Text(
                text = "Swipe to pick • $currentHumanIndex of $total",
                style = KlixTypography.bodyPrimary.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = KlixInkSecondary,
                textAlign = TextAlign.Center
            )

            // 3. Swipeable Pager with Peeking Side Cards & Mascot
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (challenges.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 42.dp),
                        pageSpacing = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val challenge = challenges[page]
                        val progress = profileState.progressById[challenge.id]
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)

                        ChallengeSwipeCard(
                            challenge = challenge,
                            progress = progress,
                            pageIndex = page,
                            pageOffset = pageOffset,
                            onSolveClick = { onSolveChallenge(challenge) }
                        )
                    }
                }
            }

            // 4. Bottom Page Indicator Dots + Hint
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                challenges.indices.forEach { idx ->
                    val isSelected = idx == pagerState.currentPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 7.dp,
                        animationSpec = tween(durationMillis = 220),
                        label = "challenge_dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    if (isDark) Color(0xFFD5FB66) else Color(0xFF0C0D14)
                                } else {
                                    if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Swipe cards to explore",
                style = KlixTypography.bodySecondary.copy(fontSize = 13.sp),
                color = KlixInkSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChallengeSwipeCard(
    challenge: CodingChallenge,
    progress: ChallengeProgress?,
    pageIndex: Int,
    pageOffset: Float,
    onSolveClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    val activeCardColor = if (isDark) Color(0xFF1E2538) else Color(0xFFFFFFFF)
    val peekingTint = when (pageIndex % 3) {
        0 -> if (isDark) Color(0xFF1A332C) else Color(0xFFD6F4E2)
        1 -> if (isDark) Color(0xFF1B243D) else Color(0xFFD8E4FC)
        else -> if (isDark) Color(0xFF26213D) else Color(0xFFE3DFFC)
    }
    val cardColor = lerp(activeCardColor, peekingTint, pageOffset)
    val scale = 1f - (pageOffset * 0.065f)
    val mascotAlpha = (1f - pageOffset * 2.2f).coerceIn(0f, 1f)

    val (diffBg, diffFg) = when (challenge.difficulty) {
        ChallengeDifficulty.EASY -> if (isDark) Color(0xFF133A2E) to Color(0xFF6EE7B7) else Color(0xFFD5F5E3) to Color(0xFF0F766E)
        ChallengeDifficulty.MEDIUM -> if (isDark) Color(0xFF3A2C12) to Color(0xFFFCD34D) else Color(0xFFFEF3C7) to Color(0xFFB45309)
        ChallengeDifficulty.HARD -> if (isDark) Color(0xFF2E1E44) to Color(0xFFC4B5FD) else Color(0xFFEDE9FE) to Color(0xFF6D28D9)
    }

    val logoRes = when (challenge.language) {
        ChallengeLanguage.PYTHON -> R.drawable.ic_logo_python
        ChallengeLanguage.NODEJS -> R.drawable.ic_logo_nodejs
        ChallengeLanguage.CPP -> R.drawable.ic_logo_cpp
        ChallengeLanguage.KOTLIN -> R.drawable.ic_logo_kotlin
    }

    val isSolved = progress?.isSolved == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // Peeking Mascot above top-right of the active card
        if (mascotAlpha > 0.02f) {
            Image(
                painter = painterResource(id = R.drawable.mascot_corner_headphones),
                contentDescription = "Klix Bot Mascot",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset(y = (-49).dp)
                    .size(width = 98.dp, height = 73.dp)
                    .graphicsLayer { alpha = mascotAlpha }
                    .zIndex(2f)
            )
        }

        // Main Challenge Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .shadow(
                    elevation = (16f - pageOffset * 10f).dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color(0x240F172A),
                    spotColor = Color(0x180F172A)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(cardColor)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Language Logo + Difficulty & Points Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = challenge.language.displayName,
                    modifier = Modifier.size(30.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(diffBg)
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${challenge.difficulty.label} • ${challenge.estimatedMinutes} min",
                            style = KlixTypography.caption.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = diffFg
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSolved) Color(0xFFD5FB66)
                                else if (isDark) Color(0xFF28324D) else Color(0xFFEEF4FF)
                            )
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (isSolved) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Solved",
                                    tint = Color(0xFF0C0D14),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "+${challenge.points} pts",
                                style = KlixTypography.caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSolved) Color(0xFF0C0D14)
                                else if (isDark) Color(0xFFD5FB66) else Color(0xFF1D4ED8)
                            )
                        }
                    }
                }
            }

            // Challenge Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = challenge.title,
                    style = KlixTypography.screenTitle.copy(
                        fontSize = 23.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = KlixInkPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = challenge.description,
                    style = KlixTypography.bodySecondary.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = KlixInkSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Code Preview Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF131826) else Color(0xFFE7EEFC))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Text(
                    text = challenge.previewSnippet,
                    style = KlixTypography.codeEditor.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    ),
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                )
            }

            // Optional Live Tracking Summary if user attempted or edited
            if (progress != null && (progress.attemptCount > 0 || progress.trackedCode.trim() != challenge.starterCode.trim())) {
                val summaryText = when {
                    progress.isSolved -> "Solved • ${progress.passedTests}/${progress.totalTests} tests passed • +${progress.pointsEarned} pts"
                    progress.attemptCount > 0 -> "Last run: ${progress.passedTests}/${progress.totalTests} tests passed • Draft saved"
                    else -> "Draft saved • ${progress.trackedCode.lines().size} lines tracked"
                }
                Text(
                    text = summaryText,
                    style = KlixTypography.caption.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (progress.isSolved) {
                        if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    } else {
                        KlixInkSecondary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Full-Width Bright Lime CTA Pill: ">_ Solve Now"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD5FB66))
                    .clickable(onClick = onSolveClick)
                    .pressEffect(0.96f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_icon_terminal_prompt),
                        contentDescription = null,
                        tint = Color(0xFF0C0D14),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSolved) "Solved • View Code" else "Solve Now",
                        style = KlixTypography.buttonText.copy(fontSize = 15.5.sp),
                        color = Color(0xFF0C0D14)
                    )
                }
            }
        }
    }
}
