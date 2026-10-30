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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = DarkBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = AccentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Local Sandbox (Alpine Linux)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Offline Linux userland execution tier",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder)

                // Current Status Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .border(0.5.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sandbox Status",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = statusMsg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (status) {
                                SandboxStatus.READY -> AccentSecondary
                                SandboxStatus.DOWNLOADING, SandboxStatus.EXTRACTING -> AccentPrimary
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
                            SandboxStatus.READY -> AccentSecondary
                            SandboxStatus.ERROR -> AccentError
                            else -> AccentPrimary
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
                            color = AccentPrimary,
                            trackColor = DarkSurfaceVariant
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == SandboxStatus.READY) DarkSurfaceVariant else AccentPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
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
                            color = if (status == SandboxStatus.READY) TextPrimary else TextPrimary
                        )
                    }
                }

                // Package Install Presets (available when ready)
                if (status == SandboxStatus.READY) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRE-CONFIGURED TOOLCHAINS (TAP TO INSTALL)",
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .border(
                                        0.5.dp,
                                        if (isInstalled) AccentSecondary.copy(alpha = 0.5f) else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onRunCommandInTerminal(preset.command)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        color = AccentPrimary
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
                                            tint = AccentSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Installed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentSecondary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Install",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder)

                // Capabilities & Limitations info per PRD Section 6.5
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(12.dp),
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
                        fontSize = 10.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
