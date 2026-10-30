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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
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
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

@Composable
fun FileTreeView(
    rootNode: FileNode?,
    activeFileUri: Uri?,
    currentProjectName: String? = null,
    onOpenSandboxWorkspace: (() -> Unit)? = null,
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

    var showCreateFileDialog by remember { mutableStateOf<Uri?>(null) }
    var showCreateDirDialog by remember { mutableStateOf<Uri?>(null) }
    var nodeToDelete by remember { mutableStateOf<FileNode?>(null) }
    var nodeToRename by remember { mutableStateOf<FileNode?>(null) }

    var dialogInputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Workspace Tree Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.klix_logo),
                    contentDescription = "Klix Logo",
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = "KLIX CODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rootNode?.let { root ->
                    IconButton(
                        onClick = {
                            dialogInputText = ""
                            showCreateFileDialog = root.uri
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = "New File",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            dialogInputText = ""
                            showCreateDirDialog = root.uri
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
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
                .background(DarkSurfaceVariant.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                    tint = if (isSandbox) AccentPrimary else AccentSecondary,
                    modifier = Modifier.size(14.dp)
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
                TextButton(
                    onClick = onOpenSandboxWorkspace,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        text = "Sandbox",
                        fontSize = 10.sp,
                        color = AccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = DarkBorder, thickness = 1.dp)

        if (rootNode == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No folder open",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
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
                    Button(
                        onClick = onOpenSandboxWorkspace,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = DarkSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Sandbox Workspace",
                            color = DarkSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                                    tint = DarkSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Switch to Sandbox Workspace",
                                    color = DarkSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Sandbox workspace is ready. Create a file or use the terminal to get started.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                dialogInputText = ""
                                showCreateFileDialog = rootNode.uri
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
                                dialogInputText = ""
                                showCreateDirDialog = rootNode.uri
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
                                dialogInputText = ""
                                showCreateFileDialog = node.uri
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
                        if (dialogInputText.isNotBlank()) {
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
                        if (dialogInputText.isNotBlank()) {
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
    var showActionMenu by remember { mutableStateOf(false) }

    val indent = (node.level * 16).dp
    val bg = if (isSelected) DarkSurfaceVariant else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(start = indent + 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(20.dp)
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
                tint = AccentPrimary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
            val extColor = when (node.extension.lowercase()) {
                "kt", "kts" -> Color(0xFFC084FC)
                "py" -> Color(0xFFFACC15)
                "js", "ts", "jsx", "tsx" -> Color(0xFF60A5FA)
                "json" -> Color(0xFF34D399)
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
            color = if (isSelected) AccentPrimary else TextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Contextual buttons
        if (node.isDirectory) {
            IconButton(
                onClick = onAddFileInDir,
                modifier = Modifier.size(22.dp)
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
            modifier = Modifier.size(22.dp)
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
            modifier = Modifier.size(22.dp)
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
