package com.klix.code.ui.components.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.data.agent.command.BuiltInSlashCommands
import com.klix.code.data.agent.command.SlashCommand
import com.klix.code.data.agent.command.SlashCommandActionType
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

@Composable
fun CustomSlashCommandsDialog(
    commands: List<SlashCommand>,
    onSaveCommand: (SlashCommand) -> Unit,
    onDeleteCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingCommand by remember { mutableStateOf<SlashCommand?>(null) }
    var commandToDelete by remember { mutableStateOf<SlashCommand?>(null) }

    val customCommands = commands.filter { !it.isBuiltIn }
    val builtInCommands = commands.filter { it.isBuiltIn }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(16.dp),
            color = DarkBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Slash Commands",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Create and run custom agent task macros",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(12.dp))

                if (showEditor) {
                    // Command Editor Form
                    CommandEditForm(
                        initialCommand = editingCommand,
                        existingTriggers = commands.map { it.trigger.lowercase() },
                        onSave = { saved ->
                            onSaveCommand(saved)
                            showEditor = false
                            editingCommand = null
                        },
                        onCancel = {
                            showEditor = false
                            editingCommand = null
                        }
                    )
                } else {
                    // Top Action: Add Command Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOM COMMANDS (${customCommands.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSecondary,
                            letterSpacing = 0.8.sp
                        )

                        Button(
                            onClick = {
                                editingCommand = null
                                showEditor = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Command",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (customCommands.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No custom commands yet",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap 'New Command' to create your first custom slash command with prompt templates and file variables.",
                                            fontSize = 11.5.sp,
                                            color = TextMuted,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(customCommands) { cmd ->
                                CommandCard(
                                    command = cmd,
                                    onEdit = {
                                        editingCommand = cmd
                                        showEditor = true
                                    },
                                    onDelete = {
                                        commandToDelete = cmd
                                    }
                                )
                            }
                        }

                        // Built-in Commands Section (Collapsible Reference)
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            BuiltInCommandsReferenceSection(builtIns = builtInCommands)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (commandToDelete != null) {
        val target = commandToDelete!!
        AlertDialog(
            onDismissRequest = { commandToDelete = null },
            title = {
                Text(
                    text = "Delete /${target.trigger}?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this custom slash command? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCommand(target.id)
                        commandToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentError)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { commandToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun CommandCard(
    command: SlashCommand,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Command Trigger Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentPrimary.copy(alpha = 0.2f))
                            .border(0.5.dp, AccentPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "/${command.trigger}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = AccentPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CUSTOM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA)
                        )
                    }
                }

                // Edit & Delete Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AccentError.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = command.description,
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (command.template.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (expanded) "Hide Template" else "Show Template",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentPrimary
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                    ) {
                        Text(
                            text = command.template,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandEditForm(
    initialCommand: SlashCommand?,
    existingTriggers: List<String>,
    onSave: (SlashCommand) -> Unit,
    onCancel: () -> Unit
) {
    var trigger by remember { mutableStateOf(initialCommand?.trigger ?: "") }
    var description by remember { mutableStateOf(initialCommand?.description ?: "") }
    var template by remember { mutableStateOf(initialCommand?.template ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = if (initialCommand == null) "Create Custom Slash Command" else "Edit /${initialCommand.trigger}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Trigger input
        OutlinedTextField(
            value = trigger,
            onValueChange = { input ->
                val clean = input.removePrefix("/").replace(" ", "-").lowercase()
                trigger = clean
                errorMessage = null
            },
            label = { Text("Command Trigger (e.g. summarize)") },
            prefix = {
                Text(
                    text = "/",
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Description input
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                errorMessage = null
            },
            label = { Text("Short Description") },
            placeholder = { Text("What does this command do?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Variable Insertion Helper Chips
        Text(
            text = "Insert template variables (tap to add):",
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VariableChip(label = "+ {input}", tooltip = "User prompt / arguments") {
                template += if (template.isEmpty() || template.endsWith(" ") || template.endsWith("\n")) "{input}" else " {input}"
            }
            VariableChip(label = "+ {file}", tooltip = "Active file name") {
                template += if (template.isEmpty() || template.endsWith(" ") || template.endsWith("\n")) "{file}" else " {file}"
            }
            VariableChip(label = "+ {code}", tooltip = "Active file code") {
                template += if (template.isEmpty() || template.endsWith("\n")) "```\n{code}\n```" else "\n```\n{code}\n```"
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Template input
        OutlinedTextField(
            value = template,
            onValueChange = {
                template = it
                errorMessage = null
            },
            label = { Text("Prompt Instructions Template") },
            placeholder = { Text("e.g. Summarize the following code in {file}:\n\n```\n{code}\n```\n\nFocus on: {input}") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage!!,
                color = AccentError,
                fontSize = 11.5.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextMuted)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val cleanTrigger = trigger.trim().removePrefix("/")
                    if (cleanTrigger.isBlank()) {
                        errorMessage = "Trigger name cannot be empty."
                        return@Button
                    }
                    if (cleanTrigger.contains(" ")) {
                        errorMessage = "Trigger cannot contain spaces."
                        return@Button
                    }
                    val isDuplicate = existingTriggers.any {
                        it.equals(cleanTrigger, ignoreCase = true) &&
                        it != initialCommand?.trigger?.lowercase()
                    }
                    if (isDuplicate) {
                        errorMessage = "A command with trigger '/$cleanTrigger' already exists."
                        return@Button
                    }
                    if (description.trim().isBlank()) {
                        errorMessage = "Description cannot be empty."
                        return@Button
                    }
                    if (template.trim().isBlank()) {
                        errorMessage = "Prompt template cannot be empty."
                        return@Button
                    }

                    val updated = (initialCommand ?: SlashCommand(
                        trigger = cleanTrigger,
                        description = description.trim(),
                        template = template.trim(),
                        actionType = SlashCommandActionType.PROMPT,
                        isBuiltIn = false
                    )).copy(
                        trigger = cleanTrigger,
                        description = description.trim(),
                        template = template.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (initialCommand == null) "Create Command" else "Save Changes",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VariableChip(
    label: String,
    tooltip: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = AccentPrimary
            )
        }
    }
}

@Composable
private fun BuiltInCommandsReferenceSection(builtIns: List<SlashCommand>) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "BUILT-IN COMMANDS (${builtIns.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    builtIns.forEach { cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "/${cmd.trigger}",
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AccentPrimary
                            )
                            Text(
                                text = cmd.description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

