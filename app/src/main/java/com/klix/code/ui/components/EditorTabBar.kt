package com.klix.code.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.model.EditorTab
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LimeBrandColor

/**
 * Screen 7: Top Tab Bar
 * Displays active and inactive file pills with official vector logos (Python, Kotlin, Node.js, etc.)
 * Zero Unicode emojis.
 */
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
            .background(KlixSurfaceBentoGroup)
            .border(0.5.dp, KlixBorderHairline)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.uri == activeTabUri
            val tabShape = RoundedCornerShape(12.dp)

            val logoRes = when (tab.extension.lowercase()) {
                "py" -> R.drawable.ic_logo_python
                "kt", "kts", "java" -> R.drawable.ic_logo_kotlin
                "js", "ts", "jsx", "tsx", "mjs" -> R.drawable.ic_logo_nodejs
                "cpp", "c", "h", "hpp" -> R.drawable.ic_logo_cpp
                "sh", "bash" -> R.drawable.ic_logo_alpine
                else -> null
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(tabShape)
                    .background(if (isActive) KlixSurfaceCardItem else Color.Transparent)
                    .then(
                        if (isActive) {
                            Modifier.border(1.dp, KlixBorderHairline, tabShape)
                        } else {
                            Modifier
                        }
                    )
                    .pressEffect(targetScale = 0.97f)
                    .clickable { onSelectTab(tab.uri) }
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Official Vector Logo
                if (logoRes != null) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = tab.name,
                        modifier = Modifier.size(15.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (isActive) KlixInkPrimary else KlixInkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = tab.name,
                    color = if (isActive) KlixInkPrimary else KlixInkSecondary,
                    style = KlixTypography.codePill,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Unsaved modified indicator
                if (tab.isModified) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LimeBrandColor)
                    )
                }

                IconButton(
                    onClick = { onCloseTab(tab.uri) },
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close ${tab.name}",
                        tint = KlixInkSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
