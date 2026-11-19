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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * - Top Header: Left column with dynamic greeting and active workspace dropdown.
 *   Right pill badge with flame icon + streakCount (RoundedCornerShape(20.dp)).
 * - Mascot Hero Bento Card (RoundedCornerShape(24.dp)):
 *   - Soft sky-to-mint gradient fill (Light) / dark indigo-to-olive glow fill with 1.dp neon-lime border (Dark).
 *   - Left side: R.drawable.mascot_coding_laptop (2D Klix Bot typing on laptop).
 *   - Top-right speech bubble: "What shall we build today?".
 *   - Bottom row inside card: Alpine Status Pill Chip: [Official Alpine Linux Hexagon Logo] Alpine: Ready (Non-clickable status indicator)
 * - Primary CTA: KlixPrimaryPillButton(text = "+ Open Sandbox Workspace").
 * - Recent Workspaces Section:
 *   - Section title "Recent Workspaces" with "+ New" button.
 *   - Realtime workspace cards displaying the user's active & recent workspaces.
 * - Bottom Bar: KlixFloatingBottomDock(selected = Home).
 */
@Composable
fun HomeDashboard(
    userName: String = "",
    onUpdateUserName: (String) -> Unit = {},
    currentWorkspaceName: String = "Sandbox Workspace",
    activeWorkspace: SandboxWorkspace? = null,
    workspaces: List<SandboxWorkspace> = emptyList(),
    onSelectWorkspace: (String) -> Unit = {},
    onCreateWorkspace: (String) -> Unit = {},
    onDeleteWorkspace: (String) -> Unit = {},
    streakCount: Int = 1,
    isStreakClaimedToday: Boolean = true,
    onOpenStreak: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    isSandboxInstalled: Boolean = true,
    activeModelName: String = "",
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
    var showEditNameDialog by remember { mutableStateOf(false) }

    val displayWorkspaces = remember(workspaces, recentProjects) {
        if (workspaces.isNotEmpty()) {
            workspaces
        } else {
            recentProjects.map { project ->
                SandboxWorkspace(
                    id = project.name,
                    name = project.name,
                    directoryName = project.name,
                    relativePath = "/workspaces/${project.name}",
                    rootUri = project.uri,
                    lastOpened = project.lastOpened
                )
            }
        }
    }

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
                                    painter = painterResource(id = R.drawable.ic_icon_menu),
                                    contentDescription = "Open Workspace Files",
                                    tint = KlixInkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showEditNameDialog = true }
                                    .pressEffect(0.95f)
                            ) {
                                Text(
                                    text = if (userName.isNotBlank()) "Hey, $userName" else "Welcome back",
                                    style = KlixTypography.screenTitle,
                                    color = KlixInkPrimary
                                )
                            }
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
                                painter = painterResource(id = R.drawable.ic_icon_flame),
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

                        // Bottom row inside card: Alpine Status Pill Chip (Non-clickable status indicator)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF1F2639) else Color(0xFFFFFFFF))
                                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            // Realtime Recent Workspaces Section
            if (displayWorkspaces.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(KlixSurfaceCardItem)
                            .border(1.dp, KlixBorderHairline, RoundedCornerShape(16.dp))
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent workspaces yet",
                            style = KlixTypography.bodySecondary,
                            color = KlixInkSecondary
                        )
                    }
                }
            } else {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        displayWorkspaces.forEach { ws ->
                            val isActive = activeWorkspace?.id == ws.id || (activeWorkspace == null && ws.name == currentWorkspaceName)
                            RecentWorkspaceRow(
                                name = ws.name,
                                logoRes = getWorkspaceIconRes(ws.name),
                                isActive = isActive,
                                onClick = {
                                    val match = workspaces.find { it.id == ws.id }
                                    if (match != null) {
                                        onSelectWorkspace(match.id)
                                    } else {
                                        onSelectProject(ws.rootUri, ws.name)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }

        // Dialog for setting user name
        if (showEditNameDialog) {
            var inputName by remember { mutableStateOf(userName) }
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = {
                    Text(
                        text = "Your Name",
                        style = KlixTypography.cardTitle,
                        color = KlixInkPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Set your name to personalize your greeting.",
                            style = KlixTypography.bodySecondary,
                            color = KlixInkSecondary
                        )
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            placeholder = { Text("Enter your name", color = KlixInkSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F),
                                unfocusedBorderColor = KlixBorderHairline,
                                focusedTextColor = KlixInkPrimary,
                                unfocusedTextColor = KlixInkPrimary,
                                cursorColor = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onUpdateUserName(inputName.trim())
                            showEditNameDialog = false
                        }
                    ) {
                        Text(
                            text = "Save",
                            color = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("Cancel", color = KlixInkSecondary)
                    }
                },
                containerColor = if (isDark) Color(0xFF1E2638) else Color(0xFFFFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
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
    logoRes: Int? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KlixSurfaceCardItem)
            .border(
                width = 1.dp,
                color = if (isActive) {
                    if (isDark) Color(0xFFD5FB66).copy(alpha = 0.5f) else Color(0xFF4D7C0F).copy(alpha = 0.4f)
                } else KlixBorderHairline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .pressEffect(0.97f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (logoRes != null) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) {
                            if (isDark) Color(0xFFD5FB66).copy(alpha = 0.15f) else Color(0xFF4D7C0F).copy(alpha = 0.15f)
                        } else {
                            if (isDark) Color(0xFF262C40) else Color(0xFFE2EDF7)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_icon_folder),
                    contentDescription = null,
                    tint = if (isActive) {
                        if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F)
                    } else {
                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = name,
            style = KlixTypography.cardTitle,
            color = KlixInkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isActive) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFFD5FB66).copy(alpha = 0.15f) else Color(0xFF4D7C0F).copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Active",
                    style = KlixTypography.bodySecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F)
                )
            }
        }
    }
}

private fun getWorkspaceIconRes(name: String): Int? {
    val lower = name.lowercase()
    return when {
        lower.endsWith(".py") || lower.contains("python") -> R.drawable.ic_logo_python
        lower.endsWith(".kt") || lower.endsWith(".kts") || lower.contains("kotlin") -> R.drawable.ic_logo_kotlin
        lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".jsx") || lower.endsWith(".tsx") || lower.contains("node") -> R.drawable.ic_logo_nodejs
        lower.endsWith(".cpp") || lower.endsWith(".c") || lower.contains("cpp") -> R.drawable.ic_logo_cpp
        lower.endsWith(".sh") || lower.endsWith(".bash") || lower.contains("alpine") -> R.drawable.ic_logo_alpine
        else -> null
    }
}
