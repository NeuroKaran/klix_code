package com.klix.code.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    AGENT("Agent", Icons.Default.SmartToy),
    TERMINAL("Terminal", Icons.Default.Terminal),
    SETTINGS("Settings", Icons.Default.Settings)
}

/**
 * Signature Klix Floating Navigation Dock as designed in the modern UI overhaul.
 * Rendered as a pill floating above the bottom edge with high-contrast obsidian surface
 * and active lime pill highlights.
 */
@Composable
fun FloatingNavDock(
    selectedTab: NavTab,
    onTabSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 16.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(Color(0xE614151B))
                .glassBorder(
                    shape = CircleShape,
                    strokeWidth = 1.dp,
                    highlightAlpha = 0.22f,
                    midAlpha = 0.08f
                )
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                NavDockItem(
                    tab = tab,
                    isSelected = isSelected,
                    onClick = { onTabSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun NavDockItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) LimeBrandColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color(0xFF9EABB8),
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_color"
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressEffect(0.94f)
            .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tab.label,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

