package com.klix.code.ui.components

import android.net.Uri
import com.klix.code.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import com.klix.code.data.stats.StreakManager
import com.klix.code.ui.components.streak.StreakSidebarCard
import com.klix.code.ui.theme.ThemeManager
import com.klix.code.ui.theme.ThemeMode
import androidx.compose.runtime.collectAsState
import com.klix.code.BuildConfig
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalContext
import com.klix.code.ui.theme.LocalIsDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.model.FileNode
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.GlassSurfacePillColor
import com.klix.code.ui.components.sandbox.SandboxWarningDialog
import com.klix.code.ui.components.glass.GlassCard
import com.klix.code.ui.components.glass.GlassButton
import com.klix.code.ui.components.glass.GlassBadge
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.LimeDimColor
import com.klix.code.ui.theme.CyanBrandColor
import com.klix.code.ui.theme.CyanDimColor
import com.klix.code.ui.theme.OnAccentPrimaryColor
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

@Composable
fun FileTreeView(
    rootNode: FileNode?,
    activeFileUri: Uri?,
    currentProjectName: String? = null,
    isSandboxInstalled: Boolean = true,
    isUpdateAvailable: Boolean = false,
    onShowSandboxWarning: (() -> Unit)? = null,
    onConfigureSandbox: (() -> Unit)? = null,
    onOpenSandboxWorkspace: (() -> Unit)? = null,
    onCheckForUpdates: (() -> Unit)? = null,
    onOpenStreak: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onFileClick: (FileNode) -> Unit,
    onCreateFile: (parentUri: Uri, fileName: String) -> Unit,
    onCreateDirectory: (parentUri: Uri, dirName: String) -> Unit,
    onDeleteNode: (FileNode) -> Unit,
    onRenameNode: (FileNode, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep track of which directory URIs are expanded
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    val streakState by StreakManager.streakState.collectAsState()

    var showCreateFileDialog by remember { mutableStateOf<Uri?>(null) }
    var showCreateDirDialog by remember { mutableStateOf<Uri?>(null) }
    var showSandboxWarningDialog by remember { mutableStateOf(false) }
    var nodeToDelete by remember { mutableStateOf<FileNode?>(null) }
    var nodeToRename by remember { mutableStateOf<FileNode?>(null) }

    var dialogInputText by remember { mutableStateOf("") }

    val triggerWarning = {
        if (onShowSandboxWarning != null) {
            onShowSandboxWarning()
        } else {
            showSandboxWarningDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlassSurfaceColor)
    ) {
        // Workspace Tree Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurfaceElevatedColor)
                .glassBorder(RoundedCornerShape(0.dp), strokeWidth = 0.5.dp, highlightAlpha = 0.18f, midAlpha = 0.05f)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Klix Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LimeBrandColor)
                        .clickable(enabled = onCheckForUpdates != null) { onCheckForUpdates?.invoke() }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rootNode?.let { root ->
                    IconButton(
                        onClick = {
                            if (!isSandboxInstalled) {
                                triggerWarning()
                            } else {
                                dialogInputText = ""
                                showCreateFileDialog = root.uri
                            }
                        },
                        modifier = Modifier.size(28.dp).pressEffect(0.9f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = "New File",
                            tint = TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!isSandboxInstalled) {
                                triggerWarning()
                            } else {
                                dialogInputText = ""
                                showCreateDirDialog = root.uri
                            }
                        },
                        modifier = Modifier.size(28.dp).pressEffect(0.9f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp).pressEffect(0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Active Workspace sub-bar
        val displayName = currentProjectName ?: rootNode?.name ?: "No Workspace"
        val isSandbox = rootNode?.uri?.toString()?.contains("sandbox") == true

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurfaceColor)
                .glassBorder(RoundedCornerShape(0.dp), strokeWidth = 0.5.dp, highlightAlpha = 0.12f, midAlpha = 0.04f)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = if (isSandbox) Icons.Default.Terminal else Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isSandbox) LimeBrandColor else CyanBrandColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = displayName.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isSandbox && onOpenSandboxWorkspace != null) {
                GlassBadge(
                    text = "SANDBOX",
                    containerColor = LimeDimColor,
                    textColor = LimeBrandColor,
                    modifier = Modifier
                        .pressEffect(0.92f)
                        .clickable(onClick = onOpenSandboxWorkspace)
                )
            }
        }

        HorizontalDivider(color = DarkBorder, thickness = 1.dp)

        if (onOpenStreak != null) {
            StreakSidebarCard(
                streakCount = streakState.streakCount,
                isClaimedToday = streakState.isClaimedToday,
                weeklyActive = streakState.weeklyActive,
                todayIndex = streakState.todayIndex,
                onClick = onOpenStreak,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Workspace Files",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        if (rootNode == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GlassSurfaceElevatedColor)
                                .glassBorder(CircleShape, strokeWidth = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = CyanBrandColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Folder Open",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open the sandbox workspace or pick a device folder to start coding.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (onOpenSandboxWorkspace != null) {
                            GlassButton(
                                onClick = onOpenSandboxWorkspace,
                                containerColor = LimeBrandColor,
                                contentColor = OnAccentPrimaryColor,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = OnAccentPrimaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open Sandbox Workspace",
                                    color = OnAccentPrimaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Flatten the hierarchy based on expanded states
            val flattenedList = remember(rootNode, expandedMap.toMap()) {
                flattenTree(rootNode, expandedMap)
            }

            if (flattenedList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${rootNode.name} is empty",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!isSandbox) {
                        Text(
                            text = "Files or directories created in the terminal reside in the Sandbox Workspace (/workspace).",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (onOpenSandboxWorkspace != null) {
                            Button(
                                onClick = onOpenSandboxWorkspace,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Switch to Sandbox Workspace",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isSandboxInstalled) {
                                "Sandbox workspace is ready. Create a file or use the terminal to get started."
                            } else {
                                "Sandbox is not installed. You need to first install the sandbox to use the workspace."
                            },
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (!isSandboxInstalled) {
                                    triggerWarning()
                                } else {
                                    dialogInputText = ""
                                    showCreateFileDialog = rootNode.uri
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New File", fontSize = 12.sp, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                if (!isSandboxInstalled) {
                                    triggerWarning()
                                } else {
                                    dialogInputText = ""
                                    showCreateDirDialog = rootNode.uri
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Folder", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    items(flattenedList, key = { it.uri.toString() }) { node ->
                        FileTreeRow(
                            node = node,
                            isSelected = node.uri == activeFileUri,
                            isExpanded = expandedMap[node.uri.toString()] ?: false,
                            onToggleExpand = {
                                val curr = expandedMap[node.uri.toString()] ?: false
                                expandedMap[node.uri.toString()] = !curr
                            },
                            onClick = {
                                if (node.isDirectory) {
                                    val curr = expandedMap[node.uri.toString()] ?: false
                                    expandedMap[node.uri.toString()] = !curr
                                } else {
                                    onFileClick(node)
                                }
                            },
                            onAddFileInDir = {
                                if (!isSandboxInstalled) {
                                    triggerWarning()
                                } else {
                                    dialogInputText = ""
                                    showCreateFileDialog = node.uri
                                }
                            },
                            onRename = {
                                dialogInputText = node.name
                                nodeToRename = node
                            },
                            onDelete = {
                                nodeToDelete = node
                            }
                        )
                    }
                }
            }
        }
        }

        // Mockup 8: Floating Pill Footer with Theme Switcher & Settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val isDark = LocalIsDarkTheme.current

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurfacePillColor)
                    .glassBorder(CircleShape, strokeWidth = 1.dp)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Toggle: Light / Dark
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF253046) else LimeBrandColor)
                        .clickable {
                            val nextMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                            ThemeManager.setThemeMode(context, nextMode)
                        }
                        .pressEffect(0.94f)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDark) "🌙 Dark" else "☀️ Light",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                }

                // Settings Button
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = onOpenSettings != null) { onOpenSettings?.invoke() }
                        .pressEffect(0.94f)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Settings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Create File Dialog
    showCreateFileDialog?.let { parentUri ->
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = null },
            title = { Text("Create File", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = dialogInputText,
                    onValueChange = { dialogInputText = it },
                    label = { Text("File name (e.g. main.kt)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isSandboxInstalled) {
                            showCreateFileDialog = null
                            triggerWarning()
                        } else if (dialogInputText.isNotBlank()) {
                            onCreateFile(parentUri, dialogInputText.trim())
                            showCreateFileDialog = null
                        }
                    }
                ) {
                    Text("Create", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Create Directory Dialog
    showCreateDirDialog?.let { parentUri ->
        AlertDialog(
            onDismissRequest = { showCreateDirDialog = null },
            title = { Text("Create Folder", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = dialogInputText,
                    onValueChange = { dialogInputText = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isSandboxInstalled) {
                            showCreateDirDialog = null
                            triggerWarning()
                        } else if (dialogInputText.isNotBlank()) {
                            onCreateDirectory(parentUri, dialogInputText.trim())
                            showCreateDirDialog = null
                        }
                    }
                ) {
                    Text("Create", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDirDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Sandbox Warning Dialog
    if (showSandboxWarningDialog) {
        SandboxWarningDialog(
            onDismiss = { showSandboxWarningDialog = false },
            onInstallSandbox = {
                showSandboxWarningDialog = false
                onConfigureSandbox?.invoke()
            }
        )
    }

    // Rename Dialog
    nodeToRename?.let { node ->
        AlertDialog(
            onDismissRequest = { nodeToRename = null },
            title = { Text("Rename ${if (node.isDirectory) "Folder" else "File"}", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = dialogInputText,
                    onValueChange = { dialogInputText = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dialogInputText.isNotBlank() && dialogInputText != node.name) {
                            onRenameNode(node, dialogInputText.trim())
                            nodeToRename = null
                        }
                    }
                ) {
                    Text("Rename", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { nodeToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    nodeToDelete?.let { node ->
        AlertDialog(
            onDismissRequest = { nodeToDelete = null },
            title = { Text("Delete ${if (node.isDirectory) "Folder" else "File"}", color = AccentError) },
            text = {
                Text(
                    "Are you sure you want to delete \"${node.name}\"? This cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteNode(node)
                        nodeToDelete = null
                    }
                ) {
                    Text("Delete", color = AccentError)
                }
            },
            dismissButton = {
                TextButton(onClick = { nodeToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FileTreeRow(
    node: FileNode,
    isSelected: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onAddFileInDir: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val indent = (node.level * 16).dp
    val rowShape = RoundedCornerShape(10.dp)
    val bg = if (isSelected) CyanDimColor else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent + 4.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
            .clip(rowShape)
            .background(bg)
            .then(
                if (isSelected) {
                    Modifier.glassBorder(rowShape, strokeWidth = 0.8.dp, highlightAlpha = 0.35f, midAlpha = 0.12f)
                } else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(22.dp).pressEffect(0.9f)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = LimeBrandColor,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(22.dp))
            val extColor = when (node.extension.lowercase()) {
                "kt", "kts" -> Color(0xFFC084FC)
                "py" -> Color(0xFFFACC15)
                "js", "ts", "jsx", "tsx" -> CyanBrandColor
                "json" -> LimeBrandColor
                "md" -> Color(0xFF38BDF8)
                else -> TextSecondary
            }
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = extColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = node.name,
            color = if (isSelected) CyanBrandColor else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Contextual buttons
        if (node.isDirectory) {
            IconButton(
                onClick = onAddFileInDir,
                modifier = Modifier.size(24.dp).pressEffect(0.9f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = "Add File Here",
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        IconButton(
            onClick = onRename,
            modifier = Modifier.size(24.dp).pressEffect(0.9f)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename",
                tint = TextMuted,
                modifier = Modifier.size(13.dp)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp).pressEffect(0.9f)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = TextMuted,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

private fun flattenTree(root: FileNode, expandedMap: Map<String, Boolean>): List<FileNode> {
    val result = mutableListOf<FileNode>()

    fun traverse(node: FileNode) {
        result.add(node)
        if (node.isDirectory && (expandedMap[node.uri.toString()] == true)) {
            node.children.forEach { traverse(it) }
        }
    }

    root.children.forEach { traverse(it) }
    return result
}
