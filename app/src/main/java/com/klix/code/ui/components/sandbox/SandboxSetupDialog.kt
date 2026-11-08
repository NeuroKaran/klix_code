package com.klix.code.ui.components.sandbox

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.components.glass.GlassIconButton
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.HeroCardBgColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.SoftCardBgColor
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Modern Local Sandbox Dialog as designed in Gemini Mockup 4.
 * Features:
 * - Header with status badge "🐧 Ready"
 * - Hero Card with running robot mascot and RootFS status
 * - 2x2 Grid of Pre-Configured Toolchains (Node.js, Python 3, Git, C/C++)
 * - Prominent ">_ Open Linux Terminal" primary CTA
 */
@Composable
fun SandboxSetupDialog(
    sandboxManager: SandboxManager,
    onRunCommandInTerminal: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val status by sandboxManager.status.collectAsState()
    val statusMsg by sandboxManager.statusMessage.collectAsState()
    val progress by sandboxManager.progress.collectAsState()
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current
    val dialogShape = RoundedCornerShape(26.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .glassBorder(dialogShape),
            shape = dialogShape,
            color = if (isDark) Color(0xFF14151B) else Color(0xFFF7F5EC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local Sandbox",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.4).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your local on-device environment",
                            fontSize = 12.5.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge: 🐧 Ready
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33000000) else Color.White)
                                .glassBorder(CircleShape, strokeWidth = 0.8.dp)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (status == SandboxStatus.READY) "🐧 Ready" else "🐧 Setup",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status == SandboxStatus.READY) LimeBrandColor else Color(0xFFEAB308)
                            )
                        }

                        GlassIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. Hero Card (Alpine Linux Active)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(HeroCardBgColor)
                        .glassBorder(RoundedCornerShape(22.dp), strokeWidth = 1.dp)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.klix_mascot_running),
                            contentDescription = "Running Mascot",
                            modifier = Modifier.size(86.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (status == SandboxStatus.READY) "Alpine Linux Active" else "Alpine Linux Setup",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "On-device unrooted runtime",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Reinstall / Install button
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33000000) else Color.White)
                                    .glassBorder(CircleShape, strokeWidth = 0.8.dp)
                                    .clickable {
                                        scope.launch { sandboxManager.installSandbox() }
                                    }
                                    .pressEffect(0.92f)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (status == SandboxStatus.READY) "Reinstall RootFS" else "Install RootFS",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Progress if downloading
                if (status == SandboxStatus.DOWNLOADING || status == SandboxStatus.EXTRACTING) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LimeBrandColor,
                            trackColor = Color(0x33000000)
                        )
                        Text(
                            text = "$statusMsg (${(progress * 100).toInt()}%)",
                            fontSize = 10.5.sp,
                            color = TextMuted,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // 3. Pre-Configured Toolchains (2x2 Grid)
                Text(
                    text = "Pre-Configured Toolchains",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val toolchains = listOf(
                    ToolchainItem("Node.js", "apk add nodejs npm", "nodejs", "📦"),
                    ToolchainItem("Python 3", "apk add python3 py3-pip", "python3", "🐍"),
                    ToolchainItem("Git", "apk add git", "git", "🌿"),
                    ToolchainItem("C/C++", "apk add build-base", "build-base", "⚙️")
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolchainCard(
                            item = toolchains[0],
                            isInstalled = sandboxManager.isToolInstalled(toolchains[0].id),
                            onClick = {
                                onRunCommandInTerminal(toolchains[0].command)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ToolchainCard(
                            item = toolchains[1],
                            isInstalled = sandboxManager.isToolInstalled(toolchains[1].id),
                            onClick = {
                                onRunCommandInTerminal(toolchains[1].command)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolchainCard(
                            item = toolchains[2],
                            isInstalled = sandboxManager.isToolInstalled(toolchains[2].id),
                            onClick = {
                                onRunCommandInTerminal(toolchains[2].command)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ToolchainCard(
                            item = toolchains[3],
                            isInstalled = sandboxManager.isToolInstalled(toolchains[3].id),
                            onClick = {
                                onRunCommandInTerminal(toolchains[3].command)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Primary CTA: >_ Open Linux Terminal
                Button(
                    onClick = {
                        onRunCommandInTerminal("clear")
                        onDismiss()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) LimeBrandColor else Color(0xFF18191D),
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pressEffect(0.96f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Linux Terminal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        }
    }
}

private data class ToolchainItem(
    val name: String,
    val command: String,
    val id: String,
    val emoji: String
)

@Composable
private fun ToolchainCard(
    item: ToolchainItem,
    isInstalled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCardBgColor)
            .glassBorder(RoundedCornerShape(16.dp), strokeWidth = 0.8.dp)
            .clickable(onClick = onClick)
            .pressEffect(0.95f)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = item.emoji, fontSize = 16.sp)
                Text(
                    text = item.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = if (isInstalled) "Installed ✓" else "Tap to Install",
                fontSize = 11.sp,
                fontWeight = if (isInstalled) FontWeight.Bold else FontWeight.Normal,
                color = if (isInstalled) (if (isDark) LimeBrandColor else Color(0xFF15803D)) else TextMuted
            )
        }
    }
}
