package com.klix.code.ui.components

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.model.ProjectItem
import com.klix.code.data.workspace.SandboxWorkspace
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.components.workspace.CreateWorkspaceDialog
import com.klix.code.ui.components.workspace.WorkspaceDropdownSelector
import com.klix.code.ui.theme.KlixBgRootGradient
import com.klix.code.ui.theme.KlixBorderActiveLime
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme

/**
 * Screen 1: Sandbox Workspace Home
 * Matches design specification:
 * - Top Header: Left column with "Hey, Prasanjit" (screenTitle) and "Sandbox Workspace" (bodySecondary).
 *   Right pill badge with flame icon + streakCount (RoundedCornerShape(20.dp)).
 * - Mascot Hero Bento Card (RoundedCornerShape(24.dp)):
 *   - Soft sky-to-mint gradient fill (Light) / dark indigo-to-olive glow fill with 1.dp neon-lime border (Dark).
 *   - Left side: R.drawable.mascot_coding_laptop (2D Klix Bot typing on laptop).
 *   - Top-right speech bubble: "What shall we build today?".
 *   - Bottom row inside card: Two side-by-side pill chips:
 *     1. [Vector CPU Icon] Thinking: 4K
 *     2. [Official Alpine Linux Hexagon Logo] Alpine: Ready
 * - Primary CTA: KlixPrimaryPillButton(text = "+ Open Sandbox Workspace").
 * - Recent Workspaces Section:
 *   - Section title "Recent Workspaces"
 *   - 3 stacked 56.dp height rounded cards (RoundedCornerShape(16.dp)):
 *     1. [Official Kotlin Logo] orbit-ui
 *     2. [Official Python Logo] palindrome.py
 *     3. [Official Node.js Hexagon Logo] study-buddy
 * - Bottom Bar: KlixFloatingBottomDock(selected = Home).
 */
@Composable
fun HomeDashboard(
    userName: String = "Prasanjit",
    currentWorkspaceName: String = "Sandbox Workspace",
    activeWorkspace: SandboxWorkspace? = null,
    workspaces: List<SandboxWorkspace> = emptyList(),
    onSelectWorkspace: (String) -> Unit = {},
    onCreateWorkspace: (String) -> Unit = {},
    onDeleteWorkspace: (String) -> Unit = {},
    streakCount: Int = 7,
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
    var showCreateWorkspaceDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KlixBgRootGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // 1. Top Header Row
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E2638) else Color(0xFFE2EDF7))
                                    .border(1.dp, KlixBorderHairline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Workspace Files",
                                    tint = KlixInkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Hey, ${if (userName.isBlank()) "Prasanjit" else userName}",
                                style = KlixTypography.screenTitle,
                                color = KlixInkPrimary
                            )
                            if (workspaces.isNotEmpty()) {
                                WorkspaceDropdownSelector(
                                    activeWorkspace = activeWorkspace,
                                    workspaces = workspaces,
                                    onSelectWorkspace = onSelectWorkspace,
                                    onCreateWorkspace = onCreateWorkspace,
                                    onDeleteWorkspace = onDeleteWorkspace
                                )
                            } else {
                                Text(
                                    text = currentWorkspaceName,
                                    style = KlixTypography.bodySecondary,
                                    color = KlixInkSecondary
                                )
                            }
                        }
                    }

                    // Streak Pill Badge (🔥 7)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF1F2536) else Color(0xFFFFFFFF))
                            .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenStreak)
                            .pressEffect(0.92f)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak flame",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$streakCount",
                                style = KlixTypography.cardTitle,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // 2. Mascot Hero Bento Card (RoundedCornerShape(24.dp))
            item {
                val heroGradient = if (isDark) {
                    Brush.linearGradient(listOf(Color(0xFF151932), Color(0xFF1B2E28)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFD8EEFE), Color(0xFFE2F9EE)))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(heroGradient)
                        .border(
                            width = if (isDark) 1.dp else 1.dp,
                            color = if (isDark) Color(0xFFD5FB66).copy(alpha = 0.8f) else KlixBorderHairline,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top row: Mascot on left, speech bubble on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mascot_coding_laptop),
                                contentDescription = "Klix Bot Coding",
                                modifier = Modifier.size(width = 110.dp, height = 98.dp)
                            )

                            // Speech Bubble
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0xFF23293D) else Color(0xFFFFFFFF))
                                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "What shall we build today?",
                                    style = KlixTypography.bodyPrimary,
                                    color = KlixInkPrimary
                                )
                            }
                        }

                        // Bottom row inside card: Two side-by-side pill chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Chip 1: [Vector CPU Icon] Thinking: 4K
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF1F2639) else Color(0xFFFFFFFF))
                                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_icon_cpu),
                                        contentDescription = "CPU Icon",
                                        tint = if (isDark) Color(0xFFD5FB66) else Color(0xFF3B82F6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = activeModelName.ifEmpty { "Thinking: 4K" },
                                        style = KlixTypography.bodySecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KlixInkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Chip 2: [Official Alpine Linux Hexagon Logo] Alpine: Ready
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF1F2639) else Color(0xFFFFFFFF))
                                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                                    .clickable(onClick = onConfigureSandbox)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_logo_alpine),
                                        contentDescription = "Alpine Logo",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isSandboxInstalled) "Alpine: Ready" else "Alpine: Setup",
                                        style = KlixTypography.bodySecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KlixInkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Primary CTA: "+ Open Sandbox Workspace"
            item {
                KlixPrimaryPillButton(
                    text = "+ Open Sandbox Workspace",
                    onClick = onOpenSandboxWorkspace
                )
            }

            // 4. Recent Workspaces Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Workspaces",
                        style = KlixTypography.sectionHeader,
                        color = KlixInkPrimary
                    )

                    Text(
                        text = "+ New",
                        style = KlixTypography.bodySecondary,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F),
                        modifier = Modifier
                            .clickable(onClick = { showCreateWorkspaceDialog = true })
                            .padding(4.dp)
                    )
                }
            }

            // 3 stacked 56.dp height rounded cards
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Item 1: [Official Kotlin Logo] orbit-ui
                    RecentWorkspaceRow(
                        name = if (recentProjects.isNotEmpty()) recentProjects[0].name else "orbit-ui",
                        logoRes = R.drawable.ic_logo_kotlin,
                        onClick = {
                            if (recentProjects.isNotEmpty()) {
                                onSelectProject(recentProjects[0].uri, recentProjects[0].name)
                            } else {
                                onOpenSandboxWorkspace()
                            }
                        }
                    )

                    // Item 2: [Official Python Logo] palindrome.py
                    RecentWorkspaceRow(
                        name = if (recentProjects.size > 1) recentProjects[1].name else "palindrome.py",
                        logoRes = R.drawable.ic_logo_python,
                        onClick = {
                            if (recentProjects.size > 1) {
                                onSelectProject(recentProjects[1].uri, recentProjects[1].name)
                            } else {
                                onOpenSandboxWorkspace()
                            }
                        }
                    )

                    // Item 3: [Official Node.js Hexagon Logo] study-buddy
                    RecentWorkspaceRow(
                        name = if (recentProjects.size > 2) recentProjects[2].name else "study-buddy",
                        logoRes = R.drawable.ic_logo_nodejs,
                        onClick = {
                            if (recentProjects.size > 2) {
                                onSelectProject(recentProjects[2].uri, recentProjects[2].name)
                            } else {
                                onOpenSandboxWorkspace()
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }

        // Dialog for creating workspace
        if (showCreateWorkspaceDialog) {
            CreateWorkspaceDialog(
                onCreate = { name ->
                    onCreateWorkspace(name)
                    showCreateWorkspaceDialog = false
                },
                onDismiss = { showCreateWorkspaceDialog = false }
            )
        }

        // Floating Bottom Navigation Dock
        if (isNavDockVisible) {
            KlixFloatingBottomDock(
                selectedTab = selectedTab,
                onTabSelect = onSelectTab,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun RecentWorkspaceRow(
    name: String,
    logoRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KlixSurfaceCardItem)
            .border(1.dp, KlixBorderHairline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.97f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = name,
            style = KlixTypography.cardTitle,
            color = KlixInkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
