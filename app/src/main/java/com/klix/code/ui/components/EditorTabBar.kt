package com.klix.code.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.model.EditorTab
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

@Composable
fun EditorTabBar(
    tabs: List<EditorTab>,
    activeTabUri: Uri?,
    onSelectTab: (Uri) -> Unit,
    onCloseTab: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(GlassSurfaceColor)
            .glassBorder(RoundedCornerShape(0.dp), strokeWidth = 0.5.dp, highlightAlpha = 0.15f, midAlpha = 0.05f)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.uri == activeTabUri
            val tabShape = RoundedCornerShape(10.dp)

            val animatedBg by animateColorAsState(
                targetValue = if (isActive) GlassSurfaceElevatedColor else Color.Transparent,
                label = "tabBgAnimation"
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(tabShape)
                    .background(animatedBg)
                    .then(
                        if (isActive) {
                            Modifier.glassBorder(tabShape, strokeWidth = 1.dp, highlightAlpha = 0.32f, midAlpha = 0.12f)
                        } else {
                            Modifier
                        }
                    )
                    .pressEffect(targetScale = 0.97f)
                    .clickable { onSelectTab(tab.uri) }
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Tab modified indicator or active dot
                if (tab.isModified) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(LimeBrandColor)
                    )
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LimeBrandColor)
                            .glassBorder(CircleShape, strokeWidth = 1.dp, highlightAlpha = 0.5f)
                    )
                }

                Text(
                    text = tab.name,
                    color = if (isActive) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = { onCloseTab(tab.uri) },
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close ${tab.name}",
                        tint = if (isActive) TextSecondary else TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
