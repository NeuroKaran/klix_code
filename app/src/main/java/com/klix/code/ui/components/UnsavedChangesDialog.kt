package com.klix.code.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.model.EditorTab
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.DarkAccentErrorValue
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.OnAccentPrimaryColor
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

/**
 * Klix Code Unsaved Changes Confirmation Dialog
 * Adheres strictly to Bento / Glass design language:
 * - Elevated glass modal surface
 * - Glowing branded save icon
 * - Visual file pill with vector language logo and unsaved status badge
 * - Clear thumb-friendly action buttons: "Save & Close", "Close Without Saving", "Keep Editing"
 */
@Composable
fun UnsavedChangesDialog(
    tab: EditorTab,
    onSaveAndClose: () -> Unit,
    onCloseWithoutSaving: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val dialogShape = RoundedCornerShape(26.dp)

    val logoRes = when (tab.extension.lowercase()) {
        "py" -> R.drawable.ic_logo_python
        "kt", "kts", "java" -> R.drawable.ic_logo_kotlin
        "js", "ts", "html", "css" -> R.drawable.ic_logo_nodejs
        "cpp", "c", "h", "hpp" -> R.drawable.ic_logo_cpp
        "sh", "bash" -> R.drawable.ic_logo_alpine
        else -> null
    }

    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier.glassBorder(dialogShape),
        containerColor = GlassSurfaceElevatedColor,
        shape = dialogShape,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2E2405) else Color(0xFFFEF3C7))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF784909) else Color(0xFFF59E0B),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_icon_save),
                    contentDescription = null,
                    tint = if (isDark) LimeBrandColor else Color(0xFF84CC16),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Unsaved Changes",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Do you want to save your progress in this file before closing?",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                // Themed File Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(KlixSurfaceCardItem)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = KlixInkSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = tab.name,
                        style = KlixTypography.codePill,
                        fontWeight = FontWeight.Bold,
                        color = KlixInkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Amber/Lime unsaved modification indicator
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(LimeBrandColor)
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary CTA: Save & Close
                Button(
                    onClick = onSaveAndClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .pressEffect(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeBrandColor,
                        contentColor = OnAccentPrimaryColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_icon_save),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0C0D14)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save & Close",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF0C0D14)
                    )
                }

                // Destructive: Close Without Saving
                OutlinedButton(
                    onClick = onCloseWithoutSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .pressEffect(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DarkAccentErrorValue
                    ),
                    border = BorderStroke(1.dp, DarkAccentErrorValue.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DarkAccentErrorValue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Close Without Saving",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = DarkAccentErrorValue
                    )
                }

                // Neutral: Keep Editing
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .pressEffect(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Keep Editing",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        dismissButton = {}
    )
}
