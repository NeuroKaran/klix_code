package com.klix.code.ui.components.agent

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
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixBorderActiveLime
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixTypography

/**
 * Screen 7: Klix Buddy Inline Assistant Card (RoundedCornerShape(20.dp))
 * - Left: 2D Klix Bot avatar (R.drawable.mascot_peeking).
 * - Right column: "Klix Buddy says:" (bold), "Ready to test palindrome.py?", and two pill buttons:
 *   - [>_ Icon] /test
 *   - [Official Gemini Star Icon] /optimize
 * - Active lime border (KlixBorderActiveLime)
 * - Zero Unicode emojis.
 */
@Composable
fun KlixBuddyCompanionCard(
    activeFileName: String,
    onTriggerCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KlixSurfaceBentoGroup)
            .border(1.dp, KlixBorderActiveLime, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 2D Klix Bot Mascot Avatar
            Image(
                painter = painterResource(id = R.drawable.mascot_peeking),
                contentDescription = "Klix Bot Mascot",
                modifier = Modifier
                    .size(width = 54.dp, height = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Klix Buddy says:",
                    style = KlixTypography.cardTitle,
                    fontWeight = FontWeight.Bold,
                    color = KlixInkPrimary
                )

                Text(
                    text = "Ready to test ${activeFileName.ifEmpty { "your code" }}?",
                    style = KlixTypography.bodySecondary,
                    color = KlixInkSecondary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pill button 1: [>_ Icon] /test
                    BuddyPillButton(
                        text = "/test",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = KlixInkPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        onClick = { onTriggerCommand("/test") }
                    )

                    // Pill button 2: [Official Gemini Star Icon] /optimize
                    BuddyPillButton(
                        text = "/optimize",
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo_gemini),
                                contentDescription = "Gemini",
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        onClick = { onTriggerCommand("/optimize") }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuddyPillButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = KlixSurfaceCardItem,
        border = androidx.compose.foundation.BorderStroke(1.dp, KlixBorderHairline),
        modifier = Modifier
            .clickable(onClick = onClick)
            .pressEffect(0.93f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon()
            Text(
                text = text,
                style = KlixTypography.codePill,
                color = KlixInkPrimary
            )
        }
    }
}
