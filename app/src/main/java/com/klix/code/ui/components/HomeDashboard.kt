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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.model.ProjectItem
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.components.streak.StreakPillButton
import com.klix.code.ui.theme.AccentWarning
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
    onConfigureSandbox: () -> Unit = {},
    recentProjects: List<ProjectItem> = emptyList(),
    onSelectProject: (Uri, String) -> Unit = { _, _ -> },
    onOpenFolder: () -> Unit = {},
    selectedTab: NavTab = NavTab.HOME,
    onSelectTab: (NavTab) -> Unit = {},
    isNavDockVisible: Boolean = true,
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
                                .minimumInteractiveComponentSize()
                                .pressEffect(0.92f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Workspace Files",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                        // Mascot + Dynamic Status Column row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.klix_mascot_laptop),
                                contentDescription = "Klix Mascot Coding",
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(end = 4.dp)
                            )

                            // Factual Dynamic Status Column (replacing speculative telemetry & speech bubble)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SANDBOX STATUS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.8.sp
                                )

                                // Readiness Badge: READY vs SETUP REQUIRED
                                val isReady = isSandboxInstalled
                                val badgeColor = if (isReady) LimeBrandColor else AccentWarning
                                val badgeTextColor = if (isReady) {
                                    if (isDark) LimeBrandColor else Color(0xFF3F6212)
                                } else {
                                    AccentWarning
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = if (isDark) 0.16f else 0.12f))
                                        .border(
                                            width = 1.dp,
                                            color = badgeColor.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                    Text(
                                        text = if (isReady) "READY" else "SETUP REQUIRED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = badgeTextColor
                                    )
                                }

                                // Active Model Pill
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = activeModelName.ifEmpty { "Thinking: 4K" },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Dynamic CTA: Launch Sandbox Workspace vs Configure Sandbox
                        Button(
                            onClick = {
                                if (isSandboxInstalled) {
                                    onOpenSandboxWorkspace()
                                } else {
                                    onConfigureSandbox()
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSandboxInstalled) {
                                    if (isDark) LimeBrandColor else Color(0xFF18191D)
                                } else {
                                    AccentWarning
                                },
                                contentColor = if (isSandboxInstalled) {
                                    if (isDark) Color.Black else Color.White
                                } else {
                                    Color.Black
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .pressEffect(0.96f)
                        ) {
                            Icon(
                                imageVector = if (isSandboxInstalled) Icons.Default.Add else Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSandboxInstalled) "Launch Sandbox Workspace" else "Configure Sandbox",
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

            // 4. Dual-Branch Workspaces Section
            if (recentProjects.isNotEmpty()) {
                item {
                    RecentWorkspacesList(
                        projects = recentProjects,
                        onSelectProject = onSelectProject
                    )
                }
            } else {
                item {
                    EmptyWorkspacesCard(
                        onOpenFolder = onOpenFolder,
                        onOpenSandboxWorkspace = onOpenSandboxWorkspace
                    )
                }
            }

            // Bottom space for floating nav dock
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        // Floating Bottom Navigation Dock (only visible when on Home and no overlay/dialog is active)
        if (isNavDockVisible) {
            FloatingNavDock(
                selectedTab = selectedTab,
                onTabSelect = onSelectTab,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Actionable Empty State Card rendered when no recent workspaces exist.
 * Eliminates dummy hardcoded project items.
 */
@Composable
fun EmptyWorkspacesCard(
    onOpenFolder: () -> Unit,
    onOpenSandboxWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SoftCardBgColor)
            .glassBorder(
                RoundedCornerShape(20.dp),
                strokeWidth = 0.8.dp,
                highlightAlpha = if (isDark) 0.14f else 0.3f,
                midAlpha = 0.05f
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF222938) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = LimeBrandColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "No Recent Workspaces",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Open a local project folder or launch the Linux sandbox workspace to start coding.",
                fontSize = 12.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Button 1: Open Folder
            Button(
                onClick = onOpenFolder,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .pressEffect(0.96f),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF222938) else Color(0xFFE2E8F0),
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open Folder",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Button 2: Launch Sandbox Workspace
            Button(
                onClick = onOpenSandboxWorkspace,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .pressEffect(0.96f),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) LimeBrandColor else Color(0xFF18191D),
                    contentColor = if (isDark) Color.Black else Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Launch Sandbox",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Renders the list of recent workspaces bound to onSelectProject.
 */
@Composable
fun RecentWorkspacesList(
    projects: List<ProjectItem>,
    onSelectProject: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        projects.take(6).forEach { project ->
            WorkspaceCard(
                name = project.name,
                onClick = { onSelectProject(project.uri, project.name) }
            )
        }
    }
}

@Composable
fun WorkspaceCard(
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

