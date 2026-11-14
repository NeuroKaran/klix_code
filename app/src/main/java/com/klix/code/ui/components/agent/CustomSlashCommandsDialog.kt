package com.klix.code.ui.components.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.agent.command.SlashCommand
import com.klix.code.data.agent.command.SlashCommandActionType
import com.klix.code.ui.components.KlixPeekingMascotScaffold
import com.klix.code.ui.components.KlixPrimaryPillButton
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.KlixBorderActiveLime
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixPrimaryCtaBg
import com.klix.code.ui.theme.KlixPrimaryCtaText
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixSurfaceMacroTint
import com.klix.code.ui.theme.KlixSurfaceSheet
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme

/**
 * Screen 6: Slash Commands Studio
 * Matches design specification:
 * - Layout: KlixPeekingMascotScaffold(title = "Slash Commands")
 * - Card 1 — Built-in Quick Commands (surfaceBentoGroup, RoundedCornerShape(22.dp)):
 *   - FlowRow of 4 pill chips (JetBrains Mono 12.sp) with 1.5dp vector icons:
 *     - [Git-Branch Icon] /refactor
 *     - [Wrench Icon] /fix
 *     - [Shield-Check Icon] /test
 *     - [Book-Open Icon] /explain
 * - Card 2 — Custom Macros (2) (surfaceMacroTint gradient card, RoundedCornerShape(22.dp)):
 *   - 2 stacked inner cards (RoundedCornerShape(16.dp)):
 *     1. [Official Google Gemini Star Logo] /summarize — "Summarize file in 3 key bullet points"
 *     2. [Official Anthropic Claude Starburst Logo] /optimize — "Analyze Big-O and optimize speed"
 * - Bottom CTA: KlixPrimaryPillButton(text = "+ Create Custom Command")
 */
@OptIn(ExperimentalLayoutApi::class)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        KlixPeekingMascotScaffold(
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Slash Commands",
                        style = KlixTypography.screenTitle,
                        color = KlixInkPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = KlixInkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        ) {
            if (showEditor) {
                // Command Editor View
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card 1 — Built-in Quick Commands
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = KlixSurfaceBentoGroup,
                            border = androidx.compose.foundation.BorderStroke(1.dp, KlixBorderHairline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Built-in Quick Commands",
                                    style = KlixTypography.sectionHeader,
                                    color = KlixInkPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    QuickCommandChip(
                                        iconRes = R.drawable.ic_icon_git_branch,
                                        label = "/refactor"
                                    )
                                    QuickCommandChip(
                                        iconRes = R.drawable.ic_icon_wrench,
                                        label = "/fix"
                                    )
                                    QuickCommandChip(
                                        iconRes = R.drawable.ic_icon_shield_check,
                                        label = "/test"
                                    )
                                    QuickCommandChip(
                                        iconRes = R.drawable.ic_icon_book_open,
                                        label = "/explain"
                                    )
                                }
                            }
                        }
                    }

                    // Card 2 — Custom Macros (surfaceMacroTint gradient card)
                    item {
                        val count = if (customCommands.isNotEmpty()) customCommands.size else 2
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(KlixSurfaceMacroTint)
                                .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Custom Macros ($count)",
                                    style = KlixTypography.sectionHeader,
                                    color = KlixInkPrimary
                                )

                                if (customCommands.isEmpty()) {
                                    // Default preview macros matching specifications
                                    CustomMacroCard(
                                        iconRes = R.drawable.ic_logo_gemini,
                                        trigger = "summarize",
                                        description = "Summarize file in 3 key bullet points",
                                        onEdit = {
                                            editingCommand = SlashCommand(
                                                id = "custom_summarize",
                                                trigger = "summarize",
                                                description = "Summarize file in 3 key bullet points",
                                                template = "Provide a concise 3-bullet-point summary of {file}:\n\n```\n{code}\n```\n\nSpecific focus: {input}",
                                                actionType = SlashCommandActionType.PROMPT,
                                                isBuiltIn = false
                                            )
                                            showEditor = true
                                        },
                                        onDelete = null
                                    )

                                    CustomMacroCard(
                                        iconRes = R.drawable.ic_logo_claude,
                                        trigger = "optimize",
                                        description = "Analyze Big-O and optimize speed",
                                        onEdit = {
                                            editingCommand = SlashCommand(
                                                id = "custom_optimize",
                                                trigger = "optimize",
                                                description = "Analyze Big-O and optimize speed",
                                                template = "Analyze the algorithmic runtime and memory complexity of the code in {file} and provide high-performance alternatives:\n\n```\n{code}\n```\n\nKey areas: {input}",
                                                actionType = SlashCommandActionType.PROMPT,
                                                isBuiltIn = false
                                            )
                                            showEditor = true
                                        },
                                        onDelete = null
                                    )
                                } else {
                                    customCommands.forEach { cmd ->
                                        val icon = when {
                                            cmd.trigger.contains("summarize", ignoreCase = true) -> R.drawable.ic_logo_gemini
                                            cmd.trigger.contains("optimize", ignoreCase = true) -> R.drawable.ic_logo_claude
                                            cmd.trigger.contains("claude", ignoreCase = true) -> R.drawable.ic_logo_claude
                                            else -> R.drawable.ic_logo_gemini
                                        }

                                        CustomMacroCard(
                                            iconRes = icon,
                                            trigger = cmd.trigger,
                                            description = cmd.description,
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
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom CTA
                KlixPrimaryPillButton(
                    text = "+ Create Custom Command",
                    onClick = {
                        editingCommand = null
                        showEditor = true
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    style = KlixTypography.sectionHeader,
                    color = KlixInkPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this custom slash command? This action cannot be undone.",
                    style = KlixTypography.bodySecondary,
                    color = KlixInkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCommand(target.id)
                        commandToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentError),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { commandToDelete = null }) {
                    Text("Cancel", color = KlixInkSecondary)
                }
            },
            containerColor = KlixSurfaceSheet,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun QuickCommandChip(
    iconRes: Int,
    label: String
) {
    Surface(
        shape = CircleShape,
        color = KlixSurfaceCardItem,
        border = androidx.compose.foundation.BorderStroke(1.dp, KlixBorderHairline),
        modifier = Modifier.pressEffect(0.95f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(15.dp),
                colorFilter = ColorFilter.tint(KlixInkPrimary)
            )
            Text(
                text = label,
                style = KlixTypography.codePill,
                color = KlixInkPrimary
            )
        }
    }
}

@Composable
private fun CustomMacroCard(
    iconRes: Int,
    trigger: String,
    description: String,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressEffect(0.98f),
        shape = RoundedCornerShape(16.dp),
        color = KlixSurfaceCardItem,
        border = androidx.compose.foundation.BorderStroke(1.dp, KlixBorderHairline),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vector Model / Logo
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(KlixSurfaceBentoGroup),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = trigger,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "/$trigger",
                        style = KlixTypography.codePill,
                        fontWeight = FontWeight.Bold,
                        color = KlixInkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = KlixTypography.bodySecondary,
                        color = KlixInkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = KlixInkSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AccentError,
                            modifier = Modifier.size(16.dp)
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
            .clip(RoundedCornerShape(22.dp))
            .background(KlixSurfaceBentoGroup)
            .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(
            text = if (initialCommand == null) "Create Custom Command" else "Edit /${initialCommand.trigger}",
            style = KlixTypography.sectionHeader,
            color = KlixInkPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Trigger input
        OutlinedTextField(
            value = trigger,
            onValueChange = { input ->
                val clean = input.removePrefix("/").replace(" ", "-").lowercase()
                trigger = clean
                errorMessage = null
            },
            label = { Text("Command Trigger (e.g. summarize)", style = KlixTypography.bodySecondary) },
            prefix = {
                Text(
                    text = "/",
                    color = KlixBorderActiveLime,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KlixSurfaceCardItem,
                unfocusedContainerColor = KlixSurfaceCardItem,
                focusedBorderColor = KlixBorderActiveLime,
                unfocusedBorderColor = KlixBorderHairline,
                focusedTextColor = KlixInkPrimary,
                unfocusedTextColor = KlixInkPrimary
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
            label = { Text("Short Description", style = KlixTypography.bodySecondary) },
            placeholder = { Text("What does this command do?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KlixSurfaceCardItem,
                unfocusedContainerColor = KlixSurfaceCardItem,
                focusedBorderColor = KlixBorderActiveLime,
                unfocusedBorderColor = KlixBorderHairline,
                focusedTextColor = KlixInkPrimary,
                unfocusedTextColor = KlixInkPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Variable Insertion Helper Chips
        Text(
            text = "Insert template variables (tap to add):",
            style = KlixTypography.bodySecondary,
            color = KlixInkSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
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

        Spacer(modifier = Modifier.height(10.dp))

        // Template input
        OutlinedTextField(
            value = template,
            onValueChange = {
                template = it
                errorMessage = null
            },
            label = { Text("Prompt Instructions Template", style = KlixTypography.bodySecondary) },
            placeholder = { Text("e.g. Summarize the following code in {file}:\n\n```\n{code}\n```\n\nFocus on: {input}") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KlixSurfaceCardItem,
                unfocusedContainerColor = KlixSurfaceCardItem,
                focusedBorderColor = KlixBorderActiveLime,
                unfocusedBorderColor = KlixBorderHairline,
                focusedTextColor = KlixInkPrimary,
                unfocusedTextColor = KlixInkPrimary
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage!!,
                color = AccentError,
                style = KlixTypography.bodySecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = KlixInkSecondary)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = KlixPrimaryCtaBg,
                    contentColor = KlixPrimaryCtaText
                ),
                shape = CircleShape
            ) {
                Text(
                    text = if (initialCommand == null) "Create Command" else "Save Changes",
                    fontWeight = FontWeight.Bold
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
        modifier = Modifier
            .clickable { onClick() }
            .pressEffect(0.93f),
        shape = RoundedCornerShape(8.dp),
        color = KlixSurfaceCardItem,
        border = androidx.compose.foundation.BorderStroke(1.dp, KlixBorderHairline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = KlixTypography.codePill,
                color = KlixInkPrimary
            )
        }
    }
}
