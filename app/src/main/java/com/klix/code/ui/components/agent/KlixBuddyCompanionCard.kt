package com.klix.code.ui.components.agent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.LilacCardBgColor
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

/**
 * Klix Buddy Proactive Companion Card as displayed in Gemini Mockup 7.
 * Provides contextual AI suggestions (e.g. /test, /optimize) for the currently active file.
 */
@Composable
fun KlixBuddyCompanionCard(
    activeFileName: String,
    onTriggerCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LilacCardBgColor)
            .glassBorder(
                RoundedCornerShape(20.dp),
                strokeWidth = 1.dp,
                highlightAlpha = if (isDark) 0.18f else 0.35f,
                midAlpha = 0.08f
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.klix_mascot_buddy),
                contentDescription = "Klix Buddy Avatar",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Klix Buddy says:",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Ready to test ${activeFileName.ifEmpty { "your code" }}?",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    BuddyActionChip(
                        text = "🧪 /test",
                        onClick = { onTriggerCommand("/test") }
                    )
                    BuddyActionChip(
                        text = "⚡ /optimize",
                        onClick = { onTriggerCommand("/optimize") }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuddyActionChip(
    text: String,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isDark) Color(0xFF131722) else Color.White)
            .glassBorder(CircleShape, strokeWidth = 0.8.dp)
            .clickable(onClick = onClick)
            .pressEffect(0.92f)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color.White else Color(0xFF18191D)
        )
    }
}

