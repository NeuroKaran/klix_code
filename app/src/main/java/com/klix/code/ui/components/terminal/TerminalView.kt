package com.klix.code.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.terminal.TerminalLine
import com.klix.code.ui.terminal.TerminalTab
import com.klix.code.ui.terminal.TerminalViewModel
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.TabActiveBg
import com.klix.code.ui.theme.TabInactiveBg
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

@Composable
fun TerminalView(
    viewModel: TerminalViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTerminalId.collectAsState()
    val lines by viewModel.terminalLines.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val currentCwd by viewModel.currentCwd.collectAsState()
    val sandboxStatus by viewModel.sandboxManager.status.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var tabToRename by remember { mutableStateOf<TerminalTab?>(null) }

    // Synchronize input draft when switching active terminal tab
    LaunchedEffect(activeTabId) {
        inputCommand = viewModel.getInputDraft(activeTabId)
        if (lines.isNotEmpty()) {
            listState.scrollToItem(lines.size - 1)
        }
    }

    // Auto scroll to bottom on new output lines or prompt update
    LaunchedEffect(lines.size, lines.lastOrNull()?.id) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.isCtrlPressed &&
                    event.key == Key.C &&
                    isExecuting
                ) {
                    viewModel.cancelExecution()
                    true
                } else {
                    false
                }
            }
            .navigationBarsPadding()
            .imePadding()
    ) {
        // 1. Top Header Bar
        TerminalHeaderBar(
            sandboxStatus = sandboxStatus,
            onOpenSetupDialog = { viewModel.openSetupDialog() },
            onAddNewTerminal = { viewModel.addNewTerminal() },
            onClearTerminal = { viewModel.clearTerminal() },
            onClose = onClose
        )

        // 2. Terminal Tab Bar
        TerminalTabBar(
            tabs = tabs,
            activeTabId = activeTabId,
            onSelectTab = { id -> viewModel.selectTerminal(id) },
            onCloseTab = { id -> viewModel.closeTerminal(id) },
            onAddTab = { viewModel.addNewTerminal() },
            onRenameTab = { tab ->
                tabToRename = tab
                showRenameDialog = true
            }
        )

        // 3. Active Terminal Info & Quick Breadcrumb
        val activeTab = tabs.find { it.id == activeTabId }
        TerminalBreadcrumbBar(
            currentCwd = currentCwd,
            tabsCount = tabs.size,
            activeTab = activeTab,
            onRenameClick = { tab ->
                tabToRename = tab
                showRenameDialog = true
            }
        )

        // 4. Terminal Output Stream
        TerminalOutputList(
            lines = lines,
            isExecuting = isExecuting,
            listState = listState,
            modifier = Modifier.weight(1f)
        )

        // 5. Mobile Developer Soft-Keys Row
        TerminalQuickKeysBar(
            onInsertText = { text ->
                inputCommand += text
                viewModel.setInputDraft(inputCommand, activeTabId)
            },
            onAddTab = { viewModel.addNewTerminal() },
            onCtrlC = { viewModel.cancelExecution() },
            onPrevHistory = {
                viewModel.getPreviousHistory()?.let {
                    inputCommand = it
                    viewModel.setInputDraft(it, activeTabId)
                }
            },
            onNextHistory = {
                viewModel.getNextHistory()?.let {
                    inputCommand = it
                    viewModel.setInputDraft(it, activeTabId)
                }
            },
            onClear = { viewModel.clearTerminal() }
        )

        // 6. Command Input Line
        TerminalInputBar(
            inputCommand = inputCommand,
            onInputChange = {
                inputCommand = it
                viewModel.setInputDraft(it, activeTabId)
            },
            isExecuting = isExecuting,
            onSubmit = {
                if (inputCommand.isNotBlank()) {
                    if (isExecuting) {
                        viewModel.sendInput(inputCommand)
                    } else {
                        viewModel.executeCommand(inputCommand)
                    }
                    inputCommand = ""
                    viewModel.setInputDraft("", activeTabId)
                }
            },
            onCancelExecution = { viewModel.cancelExecution() }
        )
    }

    // Rename Terminal Dialog
    if (showRenameDialog && tabToRename != null) {
        val currentTab = tabToRename!!
        TerminalRenameDialog(
            initialName = currentTab.title,
            onRename = { newName ->
                viewModel.renameTerminal(currentTab.id, newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
private fun TerminalHeaderBar(
    sandboxStatus: SandboxStatus,
    onOpenSetupDialog: () -> Unit,
    onAddNewTerminal: () -> Unit,
    onClearTerminal: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .border(0.5.dp, DarkBorder)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentSecondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal",
                    tint = AccentSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Terminal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onOpenSetupDialog() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (sandboxStatus) {
                            SandboxStatus.READY -> "Alpine Linux"
                            SandboxStatus.DOWNLOADING -> "Downloading..."
                            SandboxStatus.EXTRACTING -> "Extracting..."
                            else -> "Setup Sandbox"
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (sandboxStatus == SandboxStatus.READY) AccentSecondary else AccentWarning
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onAddNewTerminal,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Terminal",
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onClearTerminal,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Terminal",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenSetupDialog,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Sandbox Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TerminalBreadcrumbBar(
    currentCwd: String,
    tabsCount: Int,
    activeTab: TerminalTab?,
    onRenameClick: (TerminalTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg)
            .border(0.5.dp, DarkBorder)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = currentCwd,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeTab != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onRenameClick(activeTab) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = AccentPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Rename",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentPrimary
                        )
                    }
                }
            }

            Text(
                text = "$tabsCount ${if (tabsCount == 1) "terminal" else "terminals"}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TerminalOutputList(
    lines: List<TerminalLine>,
    isExecuting: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(lines, key = { it.id }) { line ->
                Text(
                    text = line.text,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = line.color,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }

            if (isExecuting) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = AccentPrimary
                        )
                        Text(
                            text = "Running...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalQuickKeysBar(
    onInsertText: (String) -> Unit,
    onAddTab: () -> Unit,
    onCtrlC: () -> Unit,
    onPrevHistory: () -> Unit,
    onNextHistory: () -> Unit,
    onClear: () -> Unit
) {
    HorizontalDivider(color = DarkBorder)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val quickKeys = listOf(
            Pair("+ TERM", onAddTab),
            Pair("ESC", {}),
            Pair("TAB") { onInsertText("    ") },
            Pair("CTRL-C", onCtrlC),
            Pair("↑", onPrevHistory),
            Pair("↓", onNextHistory),
            Pair("|") { onInsertText(" | ") },
            Pair("~") { onInsertText("~") },
            Pair("`") { onInsertText("`") },
            Pair("-") { onInsertText("-") },
            Pair("/") { onInsertText("/") },
            Pair("clear", onClear)
        )

        quickKeys.forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (label == "+ TERM") AccentPrimary.copy(alpha = 0.15f) else DarkSurface)
                    .border(
                        0.5.dp,
                        if (label == "+ TERM") AccentPrimary.copy(alpha = 0.5f) else DarkBorder,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { action() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (label == "+ TERM") AccentPrimary else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun TerminalInputBar(
    inputCommand: String,
    onInputChange: (String) -> Unit,
    isExecuting: Boolean,
    onSubmit: () -> Unit,
    onCancelExecution: () -> Unit
) {
    HorizontalDivider(color = DarkBorder)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isExecuting) ">" else "$",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isExecuting) AccentPrimary else AccentSecondary
        )

        OutlinedTextField(
            value = inputCommand,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    text = if (isExecuting) "Send input to process (e.g. 25)..." else "npm test, python app.py, ls...",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        if (isExecuting) {
            if (inputCommand.isNotBlank()) {
                IconButton(
                    onClick = onSubmit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Input",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onCancelExecution,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentError)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            IconButton(
                onClick = onSubmit,
                enabled = inputCommand.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (inputCommand.isNotBlank()) AccentPrimary else DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Execute",
                    tint = if (inputCommand.isNotBlank()) Color.White else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TerminalRenameDialog(
    initialName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var renameInput by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Terminal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a new name for this terminal session:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (renameInput.isNotBlank()) {
                            onRename(renameInput.trim())
                        }
                    })
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (renameInput.isNotBlank()) {
                        onRename(renameInput.trim())
                    }
                }
            ) {
                Text("Rename", color = AccentPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun TerminalTabBar(
    tabs: List<TerminalTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddTab: () -> Unit,
    onRenameTab: (TerminalTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(DarkSurface)
            .border(width = 0.5.dp, color = DarkBorder)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTabId
            val bg = if (isActive) TabActiveBg else TabInactiveBg

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(bg)
                    .clickable { onSelectTab(tab.id) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Running spinner or terminal icon
                if (tab.isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(11.dp),
                        strokeWidth = 1.5.dp,
                        color = AccentPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (isActive) AccentSecondary else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = tab.title,
                    color = if (isActive) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Close terminal tab button
                IconButton(
                    onClick = { onCloseTab(tab.id) },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close ${tab.title}",
                        tint = if (isActive) TextSecondary else TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Divider between tabs
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(DarkBorder)
            )
        }

        // Add new terminal button in the tab strip
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable { onAddTab() }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Terminal",
                    tint = AccentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "New",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPrimary
                )
            }
        }
    }
}
