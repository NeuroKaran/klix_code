package com.klix.code.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.model.ProjectItem
import com.klix.code.data.stats.DayActivity
import com.klix.code.data.stats.StreakManager
import com.klix.code.data.workspace.SandboxWorkspace
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.components.workspace.CreateWorkspaceDialog
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.KlixBgRootGradient
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Screen 1: Sandbox Workspace Home (Neo-Bento Redesign)
 *
 * Visual hierarchy achieved purely through borderless soft-tonal surfaces, generous corner radii,
 * unmocked real coding activity data, and the signature peeking Klix Bot mascot.
 *
 * 1. Top Bar:
 *    - Circular Hamburger button on left
 *    - Centered Workspace Dropdown pill selector
 *    - Daily Streak flame badge pill on right
 *
 * 2. Middle Bento Card:
 *    - Borderless soft white/dark card with 26.dp corner radius
 *    - Peeking Klix Bot with headphones (R.drawable.mascot_corner_headphones) over top-right corner
 *    - "CODING ACTIVITY" label + bold Headline displaying real runs and Current Month Name
 *    - Unmocked 30/31-day activity grid for current month partitioned by weeks
 *    - Noticeable gap separating Week 1, Week 2, Week 3, Week 4, Week 5
 *    - Zero 1.dp borders anywhere
 *
 * 3. Recent Workspaces:
 *    - "Recent Workspaces" header + "+ New" action
 *    - Realtime borderless workspace cards with official tech logos and "Active" pill tag
 *
 * 4. Floating Navigation Dock:
 *    - Borderless bottom dock with active Home pill highlight
 */
@Composable
fun HomeDashboard(
    userName: String = "",
    onUpdateUserName: (String) -> Unit = {},
    currentWorkspaceName: String = "Default Workspace",
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
    onConfigureSandbox: () -> Unit = {},
    recentProjects: List<ProjectItem> = emptyList(),
    onSelectProject: (Uri, String) -> Unit = { _, _ -> },
    onOpenFolder: () -> Unit = {},
    selectedTab: NavTab = NavTab.HOME,
    onSelectTab: (NavTab) -> Unit = {},
    isNavDockVisible: Boolean = true,
    monthActivities: List<DayActivity> = emptyList(),
    totalMonthRuns: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var showCreateWorkspaceDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var isWorkspaceMenuOpen by remember { mutableStateOf(false) }
    var workspaceToDelete by remember { mutableStateOf<SandboxWorkspace?>(null) }

    val liveStreakState by StreakManager.streakState.collectAsState()

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

    // Date & Month calculations (unmocked)
    val calendar = remember { Calendar.getInstance() }
    val currentMonthName = remember(calendar) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
    }
    val daysInMonth = remember(calendar) {
        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Real activities for current month
    val activities = remember(monthActivities, liveStreakState, daysInMonth) {
        if (monthActivities.isNotEmpty() && monthActivities.size == daysInMonth) {
            monthActivities
        } else if (liveStreakState.monthActivities.isNotEmpty() && liveStreakState.monthActivities.size == daysInMonth) {
            liveStreakState.monthActivities
        } else {
            StreakManager.getMonthActivities(context, calendar)
        }
    }

    val runsSum = remember(totalMonthRuns, liveStreakState.totalMonthRuns, activities) {
        if (totalMonthRuns > 0) {
            totalMonthRuns
        } else if (liveStreakState.totalMonthRuns > 0) {
            liveStreakState.totalMonthRuns
        } else {
            activities.sumOf { it.runCount }
        }
    }

    // Headline with Current Month Name
    val monthHeadline = remember(runsSum, currentMonthName) {
        when (runsSum) {
            1 -> "1 run in $currentMonthName"
            else -> "$runsSum runs in $currentMonthName"
        }
    }

    // Partition days into weeks (Week 1, Week 2, Week 3, Week 4, Week 5)
    val weeks = remember(activities) {
        activities.chunked(7)
    }

    val displayStreakCount = if (streakCount > 0) streakCount else liveStreakState.streakCount

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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Top Header Row (Hamburger, Centered Workspace Dropdown Pill, Streak Pill)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Circular Hamburger Menu Button (Borderless)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E2638) else Color(0xFFFFFFFF))
                            .clickable(onClick = onOpenDrawer)
                            .pressEffect(0.92f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_icon_menu),
                            contentDescription = "Open Drawer",
                            tint = KlixInkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Center: Centered Workspace Dropdown Pill (Borderless)
                    Box {
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isDark) Color(0xFF1E2638) else Color(0xFFFFFFFF))
                                .clickable { isWorkspaceMenuOpen = true }
                                .pressEffect(0.94f)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_icon_folder),
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = activeWorkspace?.name ?: currentWorkspaceName,
                                    style = KlixTypography.cardTitle,
                                    color = KlixInkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = KlixInkSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Workspace Selection Dropdown Menu
                        DropdownMenu(
                            expanded = isWorkspaceMenuOpen,
                            onDismissRequest = { isWorkspaceMenuOpen = false },
                            modifier = Modifier
                                .width(260.dp)
                                .background(DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "WORKSPACES (${workspaces.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = TextMuted
                                )
                            }

                            HorizontalDivider(color = DarkBorder)

                            workspaces.forEach { ws ->
                                val isActive = ws.id == activeWorkspace?.id
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = if (isActive) AccentSecondary else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ws.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) AccentSecondary else TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = ws.relativePath,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (workspaces.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        isWorkspaceMenuOpen = false
                                                        workspaceToDelete = ws
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete",
                                                        tint = AccentError.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        isWorkspaceMenuOpen = false
                                        onSelectWorkspace(ws.id)
                                    },
                                    modifier = Modifier.background(if (isActive) DarkSurfaceVariant else DarkSurface)
                                )
                            }

                            HorizontalDivider(color = DarkBorder)

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Workspace",
                                            tint = AccentPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "New Workspace",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentPrimary
                                        )
                                    }
                                },
                                onClick = {
                                    isWorkspaceMenuOpen = false
                                    showCreateWorkspaceDialog = true
                                }
                            )
                        }
                    }

                    // Right: Streak Flame Badge Pill (Borderless)
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isDark) Color(0xFF1E2638) else Color(0xFFFFFFFF))
                            .clickable(onClick = onOpenStreak)
                            .pressEffect(0.92f)
                            .padding(horizontal = 14.dp),
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
                                text = "$displayStreakCount",
                                style = KlixTypography.cardTitle,
                                fontWeight = FontWeight.Bold,
                                color = KlixInkPrimary
                            )
                        }
                    }
                }
            }

            // 2. Middle Bento Card with Peeking Mascot and Unmocked Current Month Activity Grid
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                ) {
                    // Peeking Klix Bot Mascot with Headphones behind top-right corner
                    Image(
                        painter = painterResource(id = R.drawable.mascot_corner_headphones),
                        contentDescription = "Klix Bot Mascot with Headphones",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 18.dp)
                            .offset(y = (-58).dp)
                            .size(width = 96.dp, height = 71.dp)
                    )

                    // Card Container (Borderless 26.dp soft surface rendered in front of mascot)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(if (isDark) Color(0xFF1E2638) else Color(0xFFFFFFFF))
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Texts
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "CODING ACTIVITY",
                                    style = KlixTypography.caption.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = KlixInkSecondary
                                )

                                Text(
                                    text = monthHeadline,
                                    style = KlixTypography.screenTitle.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = KlixInkPrimary
                                )
                            }

                            // 30/31-day Activity Grid separated per week with noticeable gap
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                weeks.forEach { weekDays ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        weekDays.forEach { dayActivity ->
                                            val tileColor = when (dayActivity.intensityLevel) {
                                                1 -> if (isDark) Color(0xFF4E6E24) else Color(0xFFD4F87A)
                                                2 -> if (isDark) Color(0xFF65A30D) else Color(0xFFA3E635)
                                                3 -> if (isDark) Color(0xFF84CC16) else Color(0xFF4D7C0F)
                                                else -> if (isDark) Color(0xFF262C40) else Color(0xFFE2EDF7)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(tileColor)
                                                    .clickable {
                                                        val label = if (dayActivity.isFuture) {
                                                            "${dayActivity.dateString}: Upcoming"
                                                        } else {
                                                            "${dayActivity.dateString}: ${dayActivity.runCount} ${if (dayActivity.runCount == 1) "run" else "runs"}"
                                                        }
                                                        Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                                                    }
                                                    .pressEffect(0.92f)
                                            )
                                        }

                                        // Balance spacing for incomplete week (e.g. Week 5 with 2 or 3 days)
                                        val emptySlots = 7 - weekDays.size
                                        repeat(emptySlots) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Recent Workspaces Section
            item {
                Spacer(modifier = Modifier.height(2.dp))
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

            // Realtime Recent Workspaces List
            if (displayWorkspaces.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(KlixSurfaceCardItem)
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

            item { Spacer(modifier = Modifier.height(96.dp)) }
        }

        // Dialog for setting user name (preserved for personalization)
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
                                unfocusedBorderColor = Color.Transparent,
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

        // Delete Confirmation Dialog
        workspaceToDelete?.let { ws ->
            AlertDialog(
                onDismissRequest = { workspaceToDelete = null },
                title = { Text("Delete Workspace?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to delete '${ws.name}'? This will permanently remove its directory, terminal tabs, and AI chat history.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteWorkspace(ws.id)
                            workspaceToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentError)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { workspaceToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
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
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(KlixSurfaceCardItem)
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
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isActive) {
                            if (isDark) Color(0xFFD5FB66).copy(alpha = 0.15f) else Color(0xFF4D7C0F).copy(alpha = 0.12f)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFFD5FB66).copy(alpha = 0.18f) else Color(0xFF4D7C0F).copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Active",
                    style = KlixTypography.bodySecondary,
                    fontSize = 11.5.sp,
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeDashboardPreviewLight() {
    com.klix.code.ui.theme.KlixTheme(themeMode = com.klix.code.ui.theme.ThemeMode.LIGHT) {
        HomeDashboard(
            currentWorkspaceName = "Default Workspace",
            streakCount = 7,
            totalMonthRuns = 28
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeDashboardPreviewDark() {
    com.klix.code.ui.theme.KlixTheme(themeMode = com.klix.code.ui.theme.ThemeMode.DARK) {
        HomeDashboard(
            currentWorkspaceName = "Default Workspace",
            streakCount = 7,
            totalMonthRuns = 28
        )
    }
}
