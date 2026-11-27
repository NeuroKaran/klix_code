package com.klix.code.ui.components.challenges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.klix.code.R
import com.klix.code.data.challenges.ChallengeLanguage
import com.klix.code.data.challenges.ChallengeProfileState
import com.klix.code.data.challenges.CodingChallenge
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme

/**
 * Interactive Daily Challenges Folder Card matching the signature folder design:
 * - Soft pastel periwinkle folder silhouette with rounded top-left tab
 * - Peeking Klix Bot mascot gripping the top-right edge of the folder
 * - Three fanned preview challenge cards tucked inside the folder
 * - Elevated white/dark pocket card in front showing "DAILY CHALLENGES", "3 Ready",
 *   "Pick a challenge", "Tap folder to pop open", and circular arrow button.
 */
@Composable
fun DailyChallengesFolderCard(
    challenges: List<CodingChallenge>,
    profileState: ChallengeProfileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val interactionSource = remember { MutableInteractionSource() }

    val folderBackBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF232C46), Color(0xFF1A2238)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFDFEAFC), Color(0xFFD2E0FA)))
    }

    val frontCardBg = if (isDark) Color(0xFF161D2E) else Color(0xFFFFFFFF)
    val leftCardBg = if (isDark) Color(0xFF1D3B32) else Color(0xFFC5F2D6)
    val centerCardBg = Color(0xFFD5FB66)
    val rightCardBg = if (isDark) Color(0xFF27304A) else Color(0xFFFFFFFF)

    val firstChallenge = challenges.getOrNull(0)
    val secondChallenge = challenges.getOrNull(1)
    val thirdChallenge = challenges.getOrNull(2)

    val readyCount = profileState.remainingCount
    val statusBadgeText = when {
        challenges.isEmpty() -> "Ready"
        readyCount > 0 -> "$readyCount Ready"
        else -> "All Solved"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 42.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressEffect(0.97f)
    ) {
        // Folder main container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(268.dp)
        ) {
            // 1. Folder Back Silhouette with Top-Left Tab
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            ) {
                val w = size.width
                val h = size.height
                val tabHeight = 20.dp.toPx()
                val tabWidth = w * 0.37f
                val slopeWidth = 22.dp.toPx()
                val r = 24.dp.toPx()
                val tabR = 16.dp.toPx()

                val path = Path().apply {
                    moveTo(0f, tabR)
                    quadraticTo(0f, 0f, tabR, 0f)
                    lineTo(tabWidth - tabR, 0f)
                    quadraticTo(tabWidth, 0f, tabWidth + slopeWidth * 0.45f, tabHeight * 0.55f)
                    quadraticTo(tabWidth + slopeWidth * 0.85f, tabHeight, tabWidth + slopeWidth + 6.dp.toPx(), tabHeight)
                    lineTo(w - r, tabHeight)
                    quadraticTo(w, tabHeight, w, tabHeight + r)
                    lineTo(w, h - r)
                    quadraticTo(w, h, w - r, h)
                    lineTo(r, h)
                    quadraticTo(0f, h, 0f, h - r)
                    close()
                }
                drawPath(path = path, brush = folderBackBrush)
            }

            // 2. Three Fanned Preview Cards tucked inside the upper pocket
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 34.dp, start = 18.dp, end = 18.dp)
                    .height(132.dp)
                    .zIndex(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                // Left Mint Card
                Box(
                    modifier = Modifier
                        .offset(x = (-74).dp, y = 12.dp)
                        .rotate(-10f)
                        .size(width = 114.dp, height = 104.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x18000000))
                        .clip(RoundedCornerShape(16.dp))
                        .background(leftCardBg)
                        .padding(12.dp)
                ) {
                    Text(
                        text = secondChallenge?.shortTitle ?: "Rate Limiter",
                        style = KlixTypography.caption,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF0F766E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 18.dp)
                    )
                }

                // Right White Card with Python Logo
                Box(
                    modifier = Modifier
                        .offset(x = 74.dp, y = 12.dp)
                        .rotate(10f)
                        .size(width = 116.dp, height = 104.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x18000000))
                        .clip(RoundedCornerShape(16.dp))
                        .background(rightCardBg)
                        .padding(12.dp)
                ) {
                    val rightLogo = when (thirdChallenge?.language) {
                        ChallengeLanguage.NODEJS -> R.drawable.ic_logo_nodejs
                        ChallengeLanguage.CPP -> R.drawable.ic_logo_cpp
                        ChallengeLanguage.KOTLIN -> R.drawable.ic_logo_kotlin
                        else -> R.drawable.ic_logo_python
                    }
                    Image(
                        painter = painterResource(id = rightLogo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.TopEnd)
                    )
                    Text(
                        text = thirdChallenge?.shortTitle ?: "LRU Cache",
                        style = KlixTypography.cardTitle,
                        fontSize = 13.sp,
                        color = KlixInkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 18.dp)
                    )
                }

                // Center Lime Card (Two-Sum)
                Box(
                    modifier = Modifier
                        .offset(x = (-6).dp, y = 0.dp)
                        .rotate(-2.5f)
                        .size(width = 136.dp, height = 116.dp)
                        .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x22000000))
                        .clip(RoundedCornerShape(18.dp))
                        .background(centerCardBg)
                        .padding(13.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1A0C0D14)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_icon_terminal_prompt),
                                contentDescription = null,
                                tint = Color(0xFF0C0D14),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = firstChallenge?.shortTitle ?: "Two-Sum",
                            style = KlixTypography.sectionHeader,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0C0D14),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Peeking Mascot gripping top-right edge of the folder back
            Image(
                painter = painterResource(id = R.drawable.mascot_corner_headphones),
                contentDescription = "Klix Bot Peeking",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 18.dp)
                    .offset(y = (-40).dp)
                    .size(width = 104.dp, height = 77.dp)
                    .zIndex(2f)
            )

            // 4. Front White Pocket Card
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .zIndex(3f)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0x260F172A),
                        spotColor = Color(0x1A0F172A)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(frontCardBg)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header: "DAILY CHALLENGES" + points / ready badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DAILY CHALLENGES",
                            style = KlixTypography.caption.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp
                            ),
                            color = KlixInkSecondary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (profileState.totalPoints > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF232C46) else Color(0xFFEEF4FF))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${profileState.totalPoints} pts",
                                        style = KlixTypography.caption.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isDark) Color(0xFFD5FB66) else Color(0xFF1D4ED8)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFD5FB66))
                                    .padding(horizontal = 11.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusBadgeText,
                                    style = KlixTypography.caption.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF0C0D14)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pick a challenge",
                        style = KlixTypography.screenTitle.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = KlixInkPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (profileState.solvedCount > 0) {
                                "${profileState.solvedCount}/${profileState.totalCount} solved • Tap folder to open"
                            } else {
                                "Tap folder to pop open"
                            },
                            style = KlixTypography.bodySecondary.copy(fontSize = 13.5.sp),
                            color = KlixInkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFFD5FB66) else Color(0xFF0C0D14)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Daily Challenges",
                                tint = if (isDark) Color(0xFF0C0D14) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
