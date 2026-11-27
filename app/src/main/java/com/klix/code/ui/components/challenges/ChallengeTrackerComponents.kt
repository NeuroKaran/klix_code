package com.klix.code.ui.components.challenges

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.challenges.ChallengeDifficulty
import com.klix.code.data.challenges.ChallengeProgress
import com.klix.code.data.challenges.ChallengeRewardEvent
import com.klix.code.data.challenges.CodingChallenge
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixSurfaceSheet
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme

/**
 * Compact Challenge Tracker Card embedded inside the Editor when solving a Daily Challenge.
 * Tracks live code line count, test pass state, output inspection, and triggers Run & Verify.
 */
@Composable
fun ChallengeTrackerEditorCard(
    challenge: CodingChallenge,
    progress: ChallengeProgress?,
    currentCode: String,
    totalPoints: Int,
    isVerifying: Boolean,
    runtimeMissing: Boolean,
    onRunAndVerify: () -> Unit,
    onSetupSandbox: () -> Unit,
    onAskBuddy: () -> Unit,
    onResetStarter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var isExpanded by remember { mutableStateOf(false) }
    val isSolved = progress?.isSolved == true
    val passedTests = progress?.passedTests ?: 0
    val totalTests = challenge.testCases.size
    val lineCount = remember(currentCode) { currentCode.lines().size }

    val (diffBg, diffFg) = when (challenge.difficulty) {
        ChallengeDifficulty.EASY -> if (isDark) Color(0xFF133A2E) to Color(0xFF6EE7B7) else Color(0xFFD5F5E3) to Color(0xFF0F766E)
        ChallengeDifficulty.MEDIUM -> if (isDark) Color(0xFF3A2C12) to Color(0xFFFCD34D) else Color(0xFFFEF3C7) to Color(0xFFB45309)
        ChallengeDifficulty.HARD -> if (isDark) Color(0xFF2E1E44) to Color(0xFFC4B5FD) else Color(0xFFEDE9FE) to Color(0xFF6D28D9)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KlixSurfaceBentoGroup)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Row: Challenge Title + Difficulty & Score + Expand Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = challenge.title,
                        style = KlixTypography.cardTitle,
                        color = KlixInkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(diffBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isSolved) "Solved +${challenge.points} pts" else "${challenge.difficulty.label} • +${challenge.points} pts",
                            style = KlixTypography.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                            color = diffFg
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val statusText = when {
                    isVerifying -> "Running & tracking output..."
                    progress != null && progress.attemptCount > 0 ->
                        "$passedTests/$totalTests tests passed • $lineCount lines tracked • Total: $totalPoints pts"
                    else -> "${challenge.description} ($lineCount lines)"
                }
                Text(
                    text = statusText,
                    style = KlixTypography.caption.copy(fontSize = 11.5.sp),
                    color = if (isSolved) {
                        if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    } else {
                        KlixInkSecondary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse tracker" else "Expand tracker",
                tint = KlixInkSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Action Row: Verify, Output Toggle, Ask Buddy, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary Run & Verify CTA
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD5FB66))
                    .clickable(enabled = !isVerifying, onClick = onRunAndVerify)
                    .pressEffect(0.94f)
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0C0D14)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0C0D14),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = if (isVerifying) "Checking..." else "Check Solution",
                        style = KlixTypography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFF0C0D14)
                    )
                }
            }

            // Tracked Output Toggle Chip
            val attemptLabel = if ((progress?.attemptCount ?: 0) > 0) {
                "Output (${progress?.attemptCount})"
            } else {
                "Output"
            }
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(KlixSurfaceCardItem)
                    .clickable { isExpanded = !isExpanded }
                    .pressEffect(0.94f)
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = KlixInkPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = attemptLabel,
                        style = KlixTypography.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = KlixInkPrimary
                    )
                }
            }

            // Ask AI Buddy
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(KlixSurfaceCardItem)
                    .clickable(onClick = onAskBuddy)
                    .pressEffect(0.94f)
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_gemini),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Hint",
                        style = KlixTypography.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = KlixInkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Reset Starter Code
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(KlixSurfaceCardItem)
                    .clickable(onClick = onResetStarter)
                    .pressEffect(0.92f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Starter Code",
                    tint = KlixInkSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        if (runtimeMissing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF3B181E) else Color(0xFFFEE2E2))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Python 3 not installed in sandbox yet.",
                    style = KlixTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFD5FB66))
                        .clickable(onClick = onSetupSandbox)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Setup",
                        style = KlixTypography.caption.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0C0D14)
                    )
                }
            }
        }

        // Expandable Tracked Output & Test Results Drawer
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(14.dp))
                    .background(KlixSurfaceCardItem)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Problem: ${challenge.description}",
                    style = KlixTypography.bodySecondary.copy(fontSize = 12.sp),
                    color = KlixInkPrimary
                )

                // Tracked Output Box
                val trackedOutput = buildString {
                    if (!progress?.lastOutput.isNullOrBlank()) {
                        append(progress?.lastOutput?.trim())
                    }
                    if (!progress?.lastStderr.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(progress?.lastStderr?.trim())
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0C0F1A))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val exitMeta = progress?.lastExitCode?.let { "exit $it • ${progress.lastExecutionTimeMs}ms" }
                            ?: "No execution output tracked yet"
                        Text(
                            text = "TRACKED OUTPUT ($exitMeta)",
                            style = KlixTypography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = trackedOutput.ifBlank { "Tap 'Check Solution' or run in Terminal to track output." },
                            style = KlixTypography.codeEditor.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                            color = if (!progress?.lastStderr.isNullOrBlank() && progress?.lastExitCode != 0) {
                                Color(0xFFF87171)
                            } else {
                                Color(0xFF86EFAC)
                            }
                        )
                    }
                }

                // Test Case Breakdown
                if (progress != null && progress.testResults.isNotEmpty()) {
                    Text(
                        text = "TEST CASES ($passedTests/$totalTests)",
                        style = KlixTypography.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = KlixInkSecondary
                    )
                    progress.testResults.forEach { tr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (tr.passed) {
                                        if (isDark) Color(0xFF132D24) else Color(0xFFE6F7ED)
                                    } else {
                                        if (isDark) Color(0xFF32181E) else Color(0xFFFEE2E2)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (tr.passed) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (tr.passed) {
                                    if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                } else {
                                    if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr.name,
                                    style = KlixTypography.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                    color = KlixInkPrimary
                                )
                                if (!tr.passed) {
                                    Text(
                                        text = "Expected ${tr.expected} • Got ${tr.actual}",
                                        style = KlixTypography.codePill.copy(fontSize = 10.5.sp),
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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

/**
 * Celebratory Points Awarded Dialog shown when a user solves a challenge for the first time.
 */
@Composable
fun ChallengeSolvedDialog(
    reward: ChallengeRewardEvent,
    onBackToChallenges: () -> Unit,
    onKeepCoding: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Dialog(
        onDismissRequest = onKeepCoding,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .shadow(20.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(KlixSurfaceSheet)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mascot_celebration),
                    contentDescription = "Challenge Solved Celebration",
                    modifier = Modifier.size(110.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFD5FB66))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF0C0D14),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+${reward.pointsAwarded} PTS • ${reward.challenge.difficulty.label.uppercase()}",
                            style = KlixTypography.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = Color(0xFF0C0D14)
                        )
                    }
                }

                Text(
                    text = "Challenge Solved!",
                    style = KlixTypography.screenTitle.copy(fontSize = 26.sp),
                    color = KlixInkPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${reward.challenge.title} passed all test cases. Total Score: ${reward.newTotalPoints} pts.",
                    style = KlixTypography.bodySecondary.copy(fontSize = 14.sp),
                    color = KlixInkSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5FB66))
                        .clickable(onClick = onBackToChallenges)
                        .pressEffect(0.96f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "More Challenges",
                        style = KlixTypography.buttonText,
                        color = Color(0xFF0C0D14)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E2638) else Color(0xFFE7EEF5))
                        .clickable(onClick = onKeepCoding)
                        .pressEffect(0.96f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keep Coding",
                        style = KlixTypography.bodyPrimary,
                        color = KlixInkPrimary
                    )
                }
            }
        }
    }
}
