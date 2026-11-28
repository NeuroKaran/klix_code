package com.klix.code.ui.components.sandbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.sandbox.DownloadPhase
import com.klix.code.data.sandbox.OnDemandToolchainSpec
import com.klix.code.data.sandbox.ToolchainDownloadState
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.BricolageGrotesque
import com.klix.code.ui.theme.JetBrainsMono
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixSurfaceSheet
import com.klix.code.ui.theme.LimeBrand
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.PlusJakartaSans
import java.util.Locale

internal fun formatDownloadBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / 1_000_000.0
    return if (mb >= 10.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.2f MB", mb)
    }
}

internal fun logoResForTool(toolId: String): Int = when (toolId.lowercase()) {
    "nodejs", "node" -> R.drawable.ic_logo_nodejs
    "c/c++", "c", "cpp", "c++" -> R.drawable.ic_logo_cpp
    "python3", "python" -> R.drawable.ic_logo_python
    else -> R.drawable.ic_icon_terminal_prompt
}

/**
 * Borderless Soft-Tonal Bento Modal Card for on-demand runtime downloads.
 * Shown when running a file whose runtime (Node.js or C/C++) is not installed yet,
 * or when extracting the bundled Python 3 runtime on first run.
 */
@Composable
fun ToolchainDownloadDialog(
    toolId: String,
    spec: OnDemandToolchainSpec?,
    downloadState: ToolchainDownloadState?,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val cardShape = RoundedCornerShape(28.dp)

    val activeForThisTool = downloadState?.takeIf {
        it.toolId.equals(toolId, ignoreCase = true) ||
            (toolId.lowercase() in setOf("c", "cpp", "c++", "c/c++") && it.toolId == "c/c++") ||
            (toolId.lowercase() in setOf("node", "nodejs") && it.toolId == "nodejs") ||
            (toolId.lowercase() in setOf("python", "python3") && it.toolId == "python3")
    }

    val isDownloading = activeForThisTool != null && (
        activeForThisTool.phase == DownloadPhase.PREPARING ||
            activeForThisTool.phase == DownloadPhase.DOWNLOADING ||
            activeForThisTool.phase == DownloadPhase.VERIFYING ||
            activeForThisTool.phase == DownloadPhase.EXTRACTING
        )
    val isCompleted = activeForThisTool?.phase == DownloadPhase.COMPLETED
    val isFailed = activeForThisTool?.phase == DownloadPhase.FAILED

    val displayName = spec?.displayName ?: activeForThisTool?.displayName ?: when (toolId.lowercase()) {
        "nodejs", "node" -> "Node.js"
        "c", "cpp", "c++", "c/c++" -> "C / C++"
        else -> "Python 3"
    }
    val sizeTag = spec?.approxSizeLabel?.let { "$it • 16 KB" } ?: "2.4 MB • Built-in"
    val subtitleText = if (spec != null) {
        "Klix ships lean with Python 3 built-in. Download $displayName (${spec.approxSizeLabel}) on-demand to run this file offline."
    } else {
        "Extract the built-in Python 3 Bionic runtime (2.4 MB) inside your app storage to run code offline."
    }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.65f else 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isDownloading) onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .widthIn(max = 396.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                // Main Borderless Bento Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = cardShape,
                            ambientColor = Color.Black.copy(alpha = 0.16f),
                            spotColor = Color.Black.copy(alpha = 0.24f)
                        )
                        .clip(cardShape)
                        .background(KlixSurfaceSheet)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF2B364C) else Color(0xFFDDE3ED))
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = when {
                                isCompleted -> "$displayName Ready"
                                isFailed -> "$displayName Download Failed"
                                isDownloading -> "Installing $displayName..."
                                spec != null -> "Install $displayName?"
                                else -> "Set Up $displayName"
                            },
                            color = KlixInkPrimary,
                            fontFamily = BricolageGrotesque,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isFailed) {
                                activeForThisTool?.errorMessage ?: "Network or verification error."
                            } else {
                                subtitleText
                            },
                            color = if (isFailed) Color(0xFFEF4444) else KlixInkSecondary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Inner Borderless Bento Info & Progress Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(KlixSurfaceBentoGroup)
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(KlixSurfaceCardItem)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Image(
                                            painter = painterResource(id = logoResForTool(toolId)),
                                            contentDescription = displayName,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Column {
                                            Text(
                                                text = displayName,
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = KlixInkPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = spec?.subtitle ?: "Bundled offline runtime",
                                                fontFamily = PlusJakartaSans,
                                                fontSize = 11.5.sp,
                                                color = KlixInkSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(KlixSurfaceBentoGroup)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (isDownloading) {
                                                "${(activeForThisTool!!.progress * 100).toInt()}%"
                                            } else {
                                                sizeTag
                                            },
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp,
                                            color = KlixInkPrimary
                                        )
                                    }
                                }

                                if (isDownloading) {
                                    val dl = activeForThisTool!!
                                    LinearProgressIndicator(
                                        progress = { dl.progress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(CircleShape),
                                        color = LimeBrand,
                                        trackColor = KlixSurfaceBentoGroup
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dl.statusMessage,
                                            fontFamily = PlusJakartaSans,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = KlixInkSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (dl.totalBytes > 0L) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${formatDownloadBytes(dl.downloadedBytes)} / ${formatDownloadBytes(dl.totalBytes)}",
                                                fontFamily = JetBrainsMono,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = KlixInkPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (!isDownloading) {
                            Button(
                                onClick = onStartDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LimeBrand,
                                    contentColor = Color(0xFF0C0D14)
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
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF0C0D14),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            isFailed -> "Retry Download"
                                            spec != null -> "Download & Run (${spec.approxSizeLabel})"
                                            else -> "Extract & Run"
                                        },
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0C0D14)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = onDismiss,
                                shape = CircleShape,
                                modifier = Modifier.pressEffect()
                            ) {
                                Text(
                                    text = "Not now",
                                    fontFamily = PlusJakartaSans,
                                    color = KlixInkSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            TextButton(
                                onClick = onCancelDownload,
                                shape = CircleShape,
                                modifier = Modifier.pressEffect()
                            ) {
                                Text(
                                    text = "Cancel Download",
                                    fontFamily = PlusJakartaSans,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Peeking Mascot gripping top edge
                Image(
                    painter = painterResource(id = R.drawable.mascot_peeking),
                    contentDescription = "Klix Bot Mascot",
                    modifier = Modifier
                        .size(width = 130.dp, height = 64.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-16).dp)
                )
            }
        }
    }
}

/**
 * Floating Borderless Soft-Tonal Bento Download Card shown whenever any toolchain
 * is downloading/extracting in the background (e.g. from Settings, Sandbox Setup, or Terminal).
 */
@Composable
fun ActiveDownloadBentoCard(
    downloadState: ToolchainDownloadState?,
    visible: Boolean,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val cardShape = RoundedCornerShape(24.dp)

    AnimatedVisibility(
        visible = visible && downloadState != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        if (downloadState == null) return@AnimatedVisibility
        val isFailed = downloadState.phase == DownloadPhase.FAILED
        val isCompleted = downloadState.phase == DownloadPhase.COMPLETED
        val isInProgress = !isFailed && !isCompleted

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = cardShape,
                    ambientColor = Color.Black.copy(alpha = 0.14f),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .clip(cardShape)
                .background(KlixSurfaceSheet)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KlixSurfaceBentoGroup),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted -> Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDark) LimeBrand else Color(0xFF15803D),
                                modifier = Modifier.size(22.dp)
                            )
                            isFailed -> Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                            else -> Image(
                                painter = painterResource(id = logoResForTool(downloadState.toolId)),
                                contentDescription = downloadState.displayName,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when {
                                    isCompleted -> "${downloadState.displayName} Ready"
                                    isFailed -> "${downloadState.displayName} Failed"
                                    else -> "Downloading ${downloadState.displayName}"
                                },
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = KlixInkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isInProgress) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(LimeBrand)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${(downloadState.progress * 100).toInt()}%",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0C0D14)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isFailed) {
                                downloadState.errorMessage ?: downloadState.statusMessage
                            } else if (isInProgress && downloadState.totalBytes > 0L) {
                                "${downloadState.statusMessage} • ${formatDownloadBytes(downloadState.downloadedBytes)} / ${formatDownloadBytes(downloadState.totalBytes)}"
                            } else {
                                downloadState.statusMessage
                            },
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                            color = if (isFailed) Color(0xFFEF4444) else KlixInkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { if (isInProgress) onCancel() else onDismiss() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(KlixSurfaceBentoGroup)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isInProgress) "Cancel" else "Dismiss",
                            tint = KlixInkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isInProgress) {
                    LinearProgressIndicator(
                        progress = { downloadState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = LimeBrand,
                        trackColor = KlixSurfaceBentoGroup
                    )
                }
            }
        }
    }
}

