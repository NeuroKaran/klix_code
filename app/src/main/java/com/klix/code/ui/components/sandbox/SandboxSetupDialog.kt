package com.klix.code.ui.components.sandbox

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.components.KlixPrimaryPillButton
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixBgRootGradient
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixSurfaceSheet
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch

/**
 * Screen 4: Local Sandbox & Toolchains
 * Matches design specification:
 * - Top Header: Left column with "Local Sandbox" (screenTitle) and "Your local on-device environment" (bodySecondary).
 *   Right pill badge [Official Alpine Linux Logo] Ready.
 * - Hero Status Card (RoundedCornerShape(24.dp)):
 *   - Sky-to-mint gradient (Light) / Deep navy-to-purple gradient with 1.dp border (Dark).
 *   - Left: R.drawable.mascot_running_terminal (2D Klix Bot running with >_ prompt).
 *   - Right column: "Alpine Linux Active" (sheetHeader), "On-device unrooted runtime" (bodySecondary), and pill button "⤓ Install RootFS" / "↻ Reinstall RootFS".
 * - Pre-Configured Toolchains (2x2 Bento Grid):
 *   - Section title "Pre-Configured Toolchains".
 *   - 2x2 grid of RoundedCornerShape(18.dp) cards:
 *     1. [Official Node.js Green Hexagon Logo] Node.js / "Installed ✓"
 *     2. [Official Python Blue-Yellow Logo] Python 3 / "Installed ✓"
 *     3. [Official Git Orange Diamond Logo] Git / "Installed ✓"
 *     4. [Official C++ Blue Hexagon Logo] C/C++ / "Tap to Install"
 * - Bottom CTA: KlixPrimaryPillButton with leading [>_] lime badge + "Open Linux Terminal".
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
                .padding(vertical = 20.dp)
                .shadow(elevation = 16.dp, shape = dialogShape)
                .border(1.dp, KlixBorderHairline, dialogShape),
            shape = dialogShape,
            color = KlixSurfaceSheet
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
                            style = KlixTypography.screenTitle,
                            color = KlixInkPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your local on-device environment",
                            style = KlixTypography.bodySecondary,
                            color = KlixInkSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge: [Official Alpine Linux Logo] Ready
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(KlixSurfaceCardItem)
                                .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_logo_alpine),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (status == SandboxStatus.READY) "Ready" else "Setup",
                                    style = KlixTypography.codePill,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == SandboxStatus.READY) (if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F)) else Color(0xFFEAB308)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = KlixInkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. Hero Status Card (RoundedCornerShape(24.dp))
                val heroGradient = if (isDark) {
                    Brush.linearGradient(listOf(Color(0xFF151932), Color(0xFF231C3D)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFD8EEFE), Color(0xFFE2F9EE)))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(heroGradient)
                        .border(1.dp, if (isDark) Color(0xFF2E364F) else KlixBorderHairline, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mascot_running_terminal),
                            contentDescription = "Klix Bot Running with Terminal",
                            modifier = Modifier.size(86.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (status == SandboxStatus.READY) "Alpine Linux Active" else "Alpine Linux Setup",
                                style = KlixTypography.sheetHeader,
                                color = KlixInkPrimary
                            )
                            Text(
                                text = "On-device unrooted runtime",
                                style = KlixTypography.bodySecondary,
                                color = KlixInkSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Pill button: ⤓ Install RootFS / ↻ Reinstall RootFS
                            val isReinstall = SandboxSetupUiHelper.isReinstall(status)
                            val actionLabel = SandboxSetupUiHelper.getActionLabel(status)
                            val actionIcon = SandboxSetupUiHelper.getActionIcon(status)
                            val isInstalling = status == SandboxStatus.DOWNLOADING || status == SandboxStatus.EXTRACTING
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(KlixSurfaceCardItem)
                                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                                    .clickable(enabled = !isInstalling) {
                                        scope.launch { sandboxManager.installSandbox() }
                                    }
                                    .pressEffect(if (isInstalling) 1f else 0.92f)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = actionIcon,
                                        contentDescription = actionLabel,
                                        tint = if (isInstalling) KlixInkSecondary else KlixInkPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = actionLabel,
                                        style = KlixTypography.codePill,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isInstalling) KlixInkSecondary else KlixInkPrimary
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
                            color = Color(0xFFD5FB66),
                            trackColor = if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "$statusMsg (${(progress * 100).toInt()}%)",
                            style = KlixTypography.bodySecondary,
                            color = KlixInkSecondary,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // 3. Pre-Configured Toolchains (2x2 Grid)
                Text(
                    text = "Pre-Configured Toolchains",
                    style = KlixTypography.sectionHeader,
                    color = KlixInkPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToolchainCard(
                            name = "Node.js",
                            logoRes = R.drawable.ic_logo_nodejs,
                            isInstalled = true,
                            onClick = {
                                onRunCommandInTerminal("apk add nodejs npm")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ToolchainCard(
                            name = "Python 3",
                            logoRes = R.drawable.ic_logo_python,
                            isInstalled = true,
                            onClick = {
                                onRunCommandInTerminal("apk add python3 py3-pip")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToolchainCard(
                            name = "Git",
                            logoRes = R.drawable.ic_logo_git,
                            isInstalled = true,
                            onClick = {
                                onRunCommandInTerminal("apk add git")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ToolchainCard(
                            name = "C/C++",
                            logoRes = R.drawable.ic_logo_cpp,
                            isInstalled = false,
                            onClick = {
                                onRunCommandInTerminal("apk add build-base")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. Primary CTA: [>_] Open Linux Terminal
                KlixPrimaryPillButton(
                    text = "Open Linux Terminal",
                    onClick = {
                        onRunCommandInTerminal("clear")
                        onDismiss()
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF0C0D14) else Color(0xFFD5FB66)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ">_",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFFD5FB66) else Color(0xFF0C0D14)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolchainCard(
    name: String,
    logoRes: Int,
    isInstalled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(KlixSurfaceCardItem)
            .border(1.dp, KlixBorderHairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.95f)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = name,
                    style = KlixTypography.cardTitle,
                    color = KlixInkPrimary
                )
            }
            Text(
                text = if (isInstalled) "Installed ✓" else "Tap to Install",
                style = KlixTypography.bodySecondary,
                fontWeight = if (isInstalled) FontWeight.Bold else FontWeight.Medium,
                color = if (isInstalled) (if (isDark) Color(0xFFD5FB66) else Color(0xFF15803D)) else KlixInkSecondary
            )
        }
    }
}

internal object SandboxSetupUiHelper {
    fun isReinstall(status: SandboxStatus): Boolean =
        status == SandboxStatus.READY

    fun getActionLabel(status: SandboxStatus): String =
        if (isReinstall(status)) "Reinstall RootFS" else "Install RootFS"

    fun getActionIcon(status: SandboxStatus): ImageVector =
        if (isReinstall(status)) Icons.Default.Refresh else Icons.Default.Download
}
