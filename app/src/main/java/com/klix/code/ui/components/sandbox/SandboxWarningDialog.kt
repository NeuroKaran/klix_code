package com.klix.code.ui.components.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.OnAccentPrimaryColor
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

@Composable
fun SandboxWarningDialog(
    onDismiss: () -> Unit,
    onInstallSandbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogShape = RoundedCornerShape(26.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.glassBorder(dialogShape),
        containerColor = GlassSurfaceElevatedColor,
        shape = dialogShape,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AccentWarning.copy(alpha = 0.15f))
                    .border(1.dp, AccentWarning.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = AccentWarning,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Sandbox Required",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "You need to first install the sandbox to use the workspace and run code locally.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onInstallSandbox,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeBrandColor,
                    contentColor = OnAccentPrimaryColor
                ),
                shape = CircleShape,
                modifier = Modifier.pressEffect()
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = OnAccentPrimaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Install Sandbox",
                    color = OnAccentPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.pressEffect()
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    )
}
