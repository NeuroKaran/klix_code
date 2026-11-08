package com.klix.code.ui.components

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.model.ProjectItem
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.components.streak.StreakPillButton
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.HeroCardBgColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.SoftCardBgColor
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

/**
 * Modern Home Dashboard Screen as defined in Gemini Mockup 0.
 * Features:
 * - Header with User Greeting, Workspace Subtitle & Streak Counter
 * - Hero Card with coding mascot, speech bubble, status badges and prominent CTA
 * - Recent Workspaces listing
 * - Floating Navigation Dock anchored at the bottom
 */
@Composable
fun HomeDashboard(
    userName: String = "Developer",
    currentWorkspaceName: String = "Sandbox Workspace",
    streakCount: Int = 1,
    isStreakClaimedToday: Boolean = true,
    onOpenStreak: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    isSandboxInstalled: Boolean = true,
    activeModelName: String = "Thinking: 4K",
    onOpenSandboxWorkspace: () -> Unit = {},
    recentProjects: List<ProjectItem> = emptyList(),
    onSelectProject: (Uri, String) -> Unit = { _, _ -> },
    onOpenFolder: () -> Unit = {},
    selectedTab: NavTab = NavTab.HOME,
    onSelectTab: (NavTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // 1. Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x18000000))
                                .pressEffect(0.92f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Workspace Files",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Hey, $userName",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentWorkspaceName,
                                fontSize = 12.5.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Streak Pill
                    StreakPillButton(
                        streakCount = streakCount,
                        isClaimedToday = isStreakClaimedToday,
                        onClick = onOpenStreak
                    )
                }
            }

            // 2. Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(HeroCardBgColor)
                        .glassBorder(
                            RoundedCornerShape(26.dp),
                            strokeWidth = 1.dp,
                            highlightAlpha = if (isDark) 0.18f else 0.4f,
                            midAlpha = if (isDark) 0.06f else 0.15f
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mascot + Speech bubble row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.klix_mascot_laptop),
                                contentDescription = "Klix Mascot Coding",
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(end = 8.dp)
                            )

                            // Speech Bubble
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isDark) Color(0xFF1E2129) else Color.White)
                                    .glassBorder(
                                        RoundedCornerShape(18.dp),
                                        strokeWidth = 0.8.dp,
                                        highlightAlpha = 0.3f,
                                        midAlpha = 0.1f
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "What shall we\nbuild today?",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF18191D),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Status badges row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Thinking Badge
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33000000) else Color.White.copy(alpha = 0.85f))
                                    .glassBorder(CircleShape, strokeWidth = 0.8.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🧠", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeModelName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF18191D)
                                )
                            }

                            // Alpine Badge
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33000000) else Color.White.copy(alpha = 0.85f))
                                    .glassBorder(CircleShape, strokeWidth = 0.8.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🐧", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSandboxInstalled) "Alpine: Ready" else "Alpine: Setup",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF18191D)
                                )
                            }
                        }

                        // Big CTA: + Open Sandbox Workspace
                        Button(
                            onClick = onOpenSandboxWorkspace,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) LimeBrandColor else Color(0xFF18191D),
                                contentColor = if (isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .pressEffect(0.96f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Sandbox Workspace",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            )
                        }
                    }
                }
            }

            // 3. Recent Workspaces Title
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Workspaces",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "+ Open Folder",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LimeBrandColor,
                        modifier = Modifier
                            .clickable(onClick = onOpenFolder)
                            .padding(4.dp)
                    )
                }
            }

            // 4. Recent Workspaces List
            if (recentProjects.isNotEmpty()) {
                items(recentProjects.take(6)) { project ->
                    WorkspaceCard(
                        name = project.name,
                        onClick = { onSelectProject(project.uri, project.name) }
                    )
                }
            } else {
                item {
                    WorkspaceCard(
                        name = "orbit-ui",
                        onClick = onOpenSandboxWorkspace
                    )
                }
                item {
                    WorkspaceCard(
                        name = "palindrome.py",
                        onClick = onOpenSandboxWorkspace
                    )
                }
            }

            // Bottom space for floating nav dock
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        // Floating Bottom Navigation Dock
        FloatingNavDock(
            selectedTab = selectedTab,
            onTabSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun WorkspaceCard(
    name: String,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCardBgColor)
            .glassBorder(
                RoundedCornerShape(16.dp),
                strokeWidth = 0.8.dp,
                highlightAlpha = if (isDark) 0.14f else 0.3f,
                midAlpha = 0.05f
            )
            .clickable(onClick = onClick)
            .pressEffect(0.97f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = if (isDark) TextSecondary else Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

