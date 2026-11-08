package com.klix.code.ui.components.sandbox

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.components.glass.GlassIconButton
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.CyanBrandColor
import com.klix.code.ui.theme.CyanDimColor
import com.klix.code.ui.theme.GlassBorderColor
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.OnAccentPrimaryColor
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import kotlinx.coroutines.launch

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
            color = GlassSurfaceElevatedColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanDimColor)
                                .glassBorder(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = CyanBrandColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Local Sandbox",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Alpine Linux on-device runtime",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    GlassIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = GlassBorderColor)

                // Current Status Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurfaceColor)
                        .glassBorder(RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = statusMsg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (status) {
                                SandboxStatus.READY -> LimeBrandColor
                                SandboxStatus.DOWNLOADING, SandboxStatus.EXTRACTING -> CyanBrandColor
                                SandboxStatus.ERROR -> AccentError
                                SandboxStatus.NOT_INSTALLED -> AccentWarning
                            }
                        )
                    }

                    Icon(
                        imageVector = when (status) {
                            SandboxStatus.READY -> Icons.Default.CheckCircle
                            SandboxStatus.ERROR -> Icons.Default.Warning
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (status) {
                            SandboxStatus.READY -> LimeBrandColor
                            SandboxStatus.ERROR -> AccentError
                            else -> CyanBrandColor
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Progress indicator during download/extraction
                if (status == SandboxStatus.DOWNLOADING || status == SandboxStatus.EXTRACTING) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyanBrandColor,
                            trackColor = GlassSurfaceColor
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% completed",
                            fontSize = 10.sp,
                            color = TextMuted,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // Install / Reinstall button
                if (status != SandboxStatus.DOWNLOADING && status != SandboxStatus.EXTRACTING) {
                    Button(
                        onClick = {
                            scope.launch {
                                sandboxManager.installSandbox()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressEffect(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == SandboxStatus.READY) GlassSurfaceColor else LimeBrandColor,
                            contentColor = if (status == SandboxStatus.READY) TextPrimary else OnAccentPrimaryColor
                        ),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (status == SandboxStatus.READY) Icons.Default.Refresh else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (status == SandboxStatus.READY) "Reinstall Sandbox RootFS" else "Download & Install Alpine RootFS (~5MB)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Package Install Presets (available when ready)
                if (status == SandboxStatus.READY) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRE-CONFIGURED TOOLCHAINS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )

                        data class ToolchainPreset(
                            val id: String,
                            val label: String,
                            val command: String
                        )

                        val packages = listOf(
                            ToolchainPreset("nodejs", "Node.js & npm", "apk add nodejs npm"),
                            ToolchainPreset("python3", "Python 3 & pip", "apk add python3 py3-pip"),
                            ToolchainPreset("git", "Git", "apk add git"),
                            ToolchainPreset("build-base", "C/C++ Build Essentials", "apk add build-base")
                        )

                        packages.forEach { preset ->
                            val isInstalled = sandboxManager.isToolInstalled(preset.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GlassSurfaceColor)
                                    .glassBorder(RoundedCornerShape(14.dp))
                                    .pressEffect()
                                    .clickable {
                                        onRunCommandInTerminal(preset.command)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = preset.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = preset.command,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanBrandColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (isInstalled) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = LimeBrandColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Installed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LimeBrandColor
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Install",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LimeBrandColor
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = GlassBorderColor)

                // Capabilities & Limitations info per PRD Section 6.5
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurfaceColor)
                        .glassBorder(RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sandbox Capabilities & Scope:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Runs unrooted Linux userland directly on-device.\n• Ideal for node, python, git, and lightweight builds.\n• Offline capable — no server required.\n• Constrained to phone CPU/RAM. No GPU acceleration.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
