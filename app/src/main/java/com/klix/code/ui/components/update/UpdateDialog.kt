package com.klix.code.ui.components.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.update.UpdateInfo
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
import com.klix.code.ui.viewmodel.UpdateUIState
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    state: UpdateUIState,
    onStartDownload: (UpdateInfo) -> Unit,
    onInstallApk: (File) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onDismiss: (postpone: Boolean) -> Unit
) {
    val updateInfo: UpdateInfo? = when (state) {
        is UpdateUIState.Available -> state.updateInfo
        is UpdateUIState.Downloading -> state.updateInfo
        is UpdateUIState.ReadyToInstall -> state.updateInfo
        is UpdateUIState.PermissionRequired -> state.updateInfo
        is UpdateUIState.Error -> state.updateInfo
        else -> null
    }

    if (updateInfo == null) return

    val isDownloading = state is UpdateUIState.Downloading
    val isReadyToInstall = state is UpdateUIState.ReadyToInstall
    val isPermissionRequired = state is UpdateUIState.PermissionRequired
    val isError = state is UpdateUIState.Error

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.forceUpdate && !isDownloading) {
                onDismiss(true)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isError -> AccentError.copy(alpha = 0.15f)
                                isReadyToInstall -> AccentSecondary.copy(alpha = 0.15f)
                                else -> AccentPrimary.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isError -> Icons.Default.Warning
                            isReadyToInstall -> Icons.Default.SecurityUpdateGood
                            else -> Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = when {
                            isError -> AccentError
                            isReadyToInstall -> AccentSecondary
                            else -> AccentPrimary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = if (updateInfo.forceUpdate) "Mandatory Update" else "Update Available",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "v${updateInfo.currentVersionName}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "v${updateInfo.latestVersionName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Release Notes Section
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = "WHAT'S NEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateInfo.releaseNotes,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Downloading Progress
                if (isDownloading) {
                    val dlState = state as UpdateUIState.Downloading
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading update...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (dlState.progressPercent >= 0) "${dlState.progressPercent}%" else "...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (dlState.progressPercent >= 0) {
                            LinearProgressIndicator(
                                progress = { dlState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentPrimary,
                                trackColor = DarkSurfaceVariant
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentPrimary,
                                trackColor = DarkSurfaceVariant
                            )
                        }

                        if (dlState.totalBytes > 0) {
                            val currentMb = dlState.bytesRead / (1024.0 * 1024.0)
                            val totalMb = dlState.totalBytes / (1024.0 * 1024.0)
                            Text(
                                text = String.format(Locale.US, "%.1f MB / %.1f MB", currentMb, totalMb),
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Permission Warning
                if (isPermissionRequired) {
                    val permState = state as UpdateUIState.PermissionRequired
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentWarning.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, AccentWarning.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Permission Required",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentWarning
                            )
                        }
                        Text(
                            text = "To install the update, please grant Klix Code permission to install apps from this source.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Error State
                if (isError) {
                    val errState = state as UpdateUIState.Error
                    Text(
                        text = "Error: ${errState.message}",
                        fontSize = 12.sp,
                        color = AccentError,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            when {
                isDownloading -> {
                    // Downloading in progress, button disabled
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Downloading...", fontSize = 13.sp)
                    }
                }
                isPermissionRequired -> {
                    val permState = state as UpdateUIState.PermissionRequired
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentWarning),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Permission", fontSize = 13.sp, color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
                isReadyToInstall -> {
                    val readyState = state as UpdateUIState.ReadyToInstall
                    Button(
                        onClick = { onInstallApk(readyState.apkFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SecurityUpdateGood,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install Update", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    // Available or Error (retry)
                    Button(
                        onClick = { onStartDownload(updateInfo) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isError) "Retry Download" else "Update Now",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (updateInfo.apkUrl.isNotBlank()) {
                    TextButton(
                        onClick = { onOpenInBrowser(updateInfo.apkUrl) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browser", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                if (!updateInfo.forceUpdate && !isDownloading) {
                    TextButton(onClick = { onDismiss(true) }) {
                        Text("Later", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }
    )
}
