package com.klix.code.ui.components.update

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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.update.UpdateInfo
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.LimeDimColor
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import com.klix.code.ui.viewmodel.UpdateUIState

@Composable
fun UpdateDialog(
    state: UpdateUIState,
    onOpenInBrowser: (String) -> Unit,
    onDismiss: (postpone: Boolean) -> Unit
) {
    val updateInfo: UpdateInfo? = when (state) {
        is UpdateUIState.Available -> state.updateInfo
        is UpdateUIState.Error -> state.updateInfo
        else -> null
    }

    if (updateInfo == null) return

    val isError = state is UpdateUIState.Error

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.forceUpdate) {
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
                            if (isError) (if (LocalIsDarkTheme.current) Color(0xFF381418) else Color(0xFFFEE2E2))
                            else LimeDimColor
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Warning else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = if (isError) AccentError else AccentPrimary,
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
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateInfo.releaseNotes.trim(),
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Error State if any
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
            Button(
                onClick = {
                    val targetUrl = updateInfo.apkUrl.ifBlank { "https://klixcode.vercel.app" }
                    onOpenInBrowser(targetUrl)
                },
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
                    text = "Download Update",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!updateInfo.forceUpdate) {
                TextButton(onClick = { onDismiss(true) }) {
                    Text("Later", fontSize = 12.sp, color = TextMuted)
                }
            }
        }
    )
}
