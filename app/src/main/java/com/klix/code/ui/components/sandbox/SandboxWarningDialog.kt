package com.klix.code.ui.components.sandbox

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.BricolageGrotesque
import com.klix.code.ui.theme.LimeBrand
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.PlusJakartaSans

@Composable
fun SandboxWarningDialog(
    onDismiss: () -> Unit,
    onInstallSandbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val cardShape = RoundedCornerShape(28.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Scrim overlay covering whole screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.65f else 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Container accommodating peeking mascot above card
            Box(
                modifier = modifier
                    .widthIn(max = 390.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click inside dialog
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                // 1. Main Bento Card (rendered first)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = cardShape,
                            ambientColor = Color.Black.copy(alpha = 0.15f),
                            spotColor = Color.Black.copy(alpha = 0.25f)
                        )
                        .clip(cardShape)
                        .background(if (isDark) Color(0xFF141926) else Color(0xFFF1F4FA))
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF263248) else Color(0xFFE2E7F0),
                            shape = cardShape
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top drag handle / capsule indicator
                        Box(
                            modifier = Modifier
                                .size(width = 38.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF2B364C) else Color(0xFFDDE3ED))
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Title: "Relax, It's Only 5 MB."
                        Text(
                            text = "Relax, It's Only 5 MB.",
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                            fontFamily = BricolageGrotesque,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtitle
                        Text(
                            text = "Smaller than one selfie—yet gives your phone a real Linux brain to run code offline.",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bento Inner Card (Alpine Linux Row)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0xFF1E2638) else Color(0xFFDCE2EE))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0xFF131824) else Color(0xFFFFFFFF))
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Alpine Icon + Label
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_logo_alpine),
                                        contentDescription = "Alpine Linux",
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = "Alpine Linux",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                }

                                // Right: "5 MB • Unrooted" Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF222B3D) else Color(0xFFE2E8F4))
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "5 MB • Unrooted",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Action Button: "Install Linux (5 MB)"
                        Button(
                            onClick = onInstallSandbox,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeBrand,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .pressEffect()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_icon_terminal_prompt),
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Install Linux (5 MB)",
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Action Button: "Not now"
                        TextButton(
                            onClick = onDismiss,
                            shape = CircleShape,
                            modifier = Modifier.pressEffect()
                        ) {
                            Text(
                                text = "Not now",
                                fontFamily = PlusJakartaSans,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 2. Mascot peeking over the top edge of the card (hands grip the top border)
                Image(
                    painter = painterResource(id = R.drawable.mascot_peeking),
                    contentDescription = "Klix Bot Mascot",
                    modifier = Modifier
                        .size(width = 130.dp, height = 64.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}
