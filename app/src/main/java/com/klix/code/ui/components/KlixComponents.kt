package com.klix.code.ui.components

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.klix.code.R
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixBgRootGradient
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixPrimaryCtaBg
import com.klix.code.ui.theme.KlixPrimaryCtaText
import com.klix.code.ui.theme.KlixSurfaceSheet
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme

/**
 * 1. KlixPeekingMascotScaffold
 * Used on Settings, Klix AI Agent, and Slash Commands.
 * Renders the top title & bgRootGradient header, positions Image(painterResource(R.drawable.mascot_peeking))
 * centered horizontally so the mascot's hands overlap the top edge of the sheet below it,
 * and draws a centered 36.dp x 4.dp drag-handle pill at the top of the sheet.
 */
@Composable
fun KlixPeekingMascotScaffold(
    title: String? = null,
    topBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val density = LocalDensity.current
    val context = LocalContext.current

    val composeNavDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeBottomPadding = remember(density, context, composeNavDp) {
        val activity = context as? Activity ?: (context as? ContextWrapper)?.baseContext as? Activity
        val rootInsets = activity?.window?.decorView?.let { ViewCompat.getRootWindowInsets(it) }
        val navInsetsPx = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val navInsetsDp = if (navInsetsPx > 0) with(density) { navInsetsPx.toDp() } else 0.dp
        val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val resPx = if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        val resDp = if (resPx > 0) with(density) { resPx.toDp() } else 0.dp
        maxOf(navInsetsDp, resDp, composeNavDp, 28.dp)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KlixBgRootGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header backdrop container (170.dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .statusBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Header Title or custom TopBar
                if (topBar != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        topBar()
                    }
                } else if (title != null) {
                    Text(
                        text = title,
                        style = KlixTypography.screenTitle,
                        color = KlixInkPrimary,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Peeking Mascot Sprite overlapping the sheet
                Image(
                    painter = painterResource(id = R.drawable.mascot_peeking),
                    contentDescription = "Klix Bot Mascot Peeking",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                        .size(width = 135.dp, height = 75.dp)
                )
            }

            // Main elevated sheet container with 28.dp top radius
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = KlixBorderHairline,
                        shape = sheetShape
                    ),
                shape = sheetShape,
                color = KlixSurfaceSheet,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 18.dp,
                            end = 18.dp,
                            top = 10.dp,
                            bottom = 100.dp
                        )
                ) {
                    // Centered 36.dp x 4.dp drag-handle pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF333D55) else Color(0xFFCBD5E1))
                        )
                    }

                    content()
                }
            }
        }
    }
}

/**
 * 2. KlixPrimaryPillButton
 * Full-width 54.dp height RoundedCornerShape(50) button.
 * Uses primaryCtaBg (#0C0D14 in Light, #D5FB66 in Dark) and primaryCtaText (#FFFFFF in Light, #0C0D14 in Dark).
 */
@Composable
fun KlixPrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = KlixPrimaryCtaBg,
            contentColor = KlixPrimaryCtaText,
            disabledContainerColor = KlixPrimaryCtaBg.copy(alpha = 0.5f),
            disabledContentColor = KlixPrimaryCtaText.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .pressEffect(0.96f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = KlixTypography.bodyPrimary,
                fontWeight = FontWeight.Bold,
                color = KlixPrimaryCtaText
            )
        }
    }
}

/**
 * 3. KlixFloatingBottomDock
 * Floating pill container (height = 64.dp, RoundedCornerShape(32.dp), #121521 dark surface with 1.dp border)
 * containing 4 destinations: Home, Agent, Terminal, Settings.
 */
@Composable
fun KlixFloatingBottomDock(
    selectedTab: NavTab,
    onTabSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF121521))
                .border(1.dp, Color(0xFF2A3449), RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                DockItem(
                    tab = tab,
                    isSelected = isSelected,
                    onClick = { onTabSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(if (isSelected) Color(0xFFD5FB66) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressEffect(0.93f)
            .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 8.dp),
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
                    tint = Color(0xFF0C0D14),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tab.label,
                    color = Color(0xFF0C0D14),
                    fontSize = 12.5.sp,
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
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
