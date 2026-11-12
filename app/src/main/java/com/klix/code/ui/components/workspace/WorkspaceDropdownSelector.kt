package com.klix.code.ui.components.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.workspace.SandboxWorkspace
import com.klix.code.ui.theme.*

@Composable
fun WorkspaceDropdownSelector(
    activeWorkspace: SandboxWorkspace?,
    workspaces: List<SandboxWorkspace>,
    onSelectWorkspace: (String) -> Unit,
    onCreateWorkspace: (String) -> Unit,
    onDeleteWorkspace: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var workspaceToDelete by remember { mutableStateOf<SandboxWorkspace?>(null) }

    Box(modifier = modifier) {
        // Pill Button in TopAppBar
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceVariant)
                .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = activeWorkspace?.name ?: "Select Workspace",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(260.dp)
                .background(DarkSurface)
                .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "WORKSPACES (${workspaces.size})",
                    fontSize = 10.sp,
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

                            if (!isActive && workspaces.size > 1) {
                                IconButton(
                                    onClick = {
                                        expanded = false
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
                        expanded = false
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
                    expanded = false
                    showCreateDialog = true
                }
            )
        }
    }

    // Create Workspace Dialog
    if (showCreateDialog) {
        CreateWorkspaceDialog(
            onCreate = { name ->
                onCreateWorkspace(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
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
}

@Composable
fun CreateWorkspaceDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var workspaceName by remember { mutableStateOf("") }
    val isNameValid = workspaceName.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Workspace", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter a name for your new workspace. It will be created inside the isolated Alpine sandbox (/workspaces/).",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
                OutlinedTextField(
                    value = workspaceName,
                    onValueChange = { workspaceName = it },
                    placeholder = { Text("e.g. Web App 2026", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (isNameValid) {
                            onCreate(workspaceName.trim())
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isNameValid) {
                        onCreate(workspaceName.trim())
                    }
                },
                enabled = isNameValid,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
