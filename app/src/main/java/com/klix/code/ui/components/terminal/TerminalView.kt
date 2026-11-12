package com.klix.code.ui.components.terminal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.R
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.terminal.TerminalColors
import com.klix.code.ui.terminal.TerminalLine
import com.klix.code.ui.terminal.TerminalTab
import com.klix.code.ui.terminal.TerminalViewModel
import com.klix.code.ui.theme.KlixBgRootGradient
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixSurfaceSheet
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme

/**
 * Screen 5: Linux Terminal
 * Matches design specification:
 * - Top Header: "Linux Terminal" (screenTitle).
 * - Tab Strip: Horizontal scrollable row of pill tabs (height = 38.dp, RoundedCornerShape(19.dp)):
 *   - [Official Alpine Linux Logo] Alpine v1 (active)
 *   - + node-server / [Official Python Logo] palindrome.py
 *   - [Official Node.js Logo]
 * - Terminal Viewport Card (RoundedCornerShape(24.dp), height = 290.dp):
 *   - Monospace output (JetBrains Mono 13.sp):
 *     klix@mobile:/workspace$
 *     python3 palindrome.py
 *     ✓ Server & checks passed in 0.4s
 *   - Bottom-right overlay inside card: R.drawable.mascot_waving with speech bubble "All green!".
 * - Shortcuts Section:
 *   - Label "Shortcuts".
 *   - Row of pill keycaps: + TERM (filled with #D5FB66 lime), ESC, TAB, CTRL-C, ↑, ↓.
 * - Bottom Command Input Bar:
 *   - Pill input field "$ Run a command..." with circular send button.
 */
@Composable
fun TerminalView(
    viewModel: TerminalViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTerminalId.collectAsState()
    val lines by viewModel.terminalLines.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
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
            .background(KlixBgRootGradient)
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Header Row: "Linux Terminal" + Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Linux Terminal",
                style = KlixTypography.screenTitle,
                color = KlixInkPrimary
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KlixSurfaceCardItem)
                    .border(1.dp, KlixBorderHairline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = KlixInkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. Tab Strip (Horizontal scrollable row of pill tabs, 38.dp height, RoundedCornerShape(19.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val logoRes = when {
                    tab.title.contains("python", ignoreCase = true) || tab.title.contains("py", ignoreCase = true) -> R.drawable.ic_logo_python
                    tab.title.contains("node", ignoreCase = true) || tab.title.contains("js", ignoreCase = true) -> R.drawable.ic_logo_nodejs
                    else -> R.drawable.ic_logo_alpine
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(if (isActive) (if (isDark) Color(0xFF1E2638) else Color(0xFFFFFFFF)) else (if (isDark) Color(0xFF121521) else Color(0xFFE2EDF7)))
                        .border(
                            width = 1.dp,
                            color = if (isActive) (if (isDark) Color(0xFFD5FB66) else Color(0xFF84CC16)) else KlixBorderHairline,
                            shape = RoundedCornerShape(19.dp)
                        )
                        .clickable { viewModel.selectTerminal(tab.id) }
                        .pressEffect(0.95f)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tab.title,
                            style = KlixTypography.codePill,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = KlixInkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Add Tab Button
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (isDark) Color(0xFF121521) else Color(0xFFE2EDF7))
                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(19.dp))
                    .clickable { viewModel.addNewTerminal() }
                    .pressEffect(0.95f)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Terminal",
                        tint = KlixInkPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "node-server",
                        style = KlixTypography.codePill,
                        color = KlixInkSecondary
                    )
                }
            }
        }

        // 3. Terminal Viewport Card (RoundedCornerShape(24.dp), height = 290.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF0C0F1A) else Color(0xFF121622))
                .border(1.dp, if (isDark) Color(0xFF2E364F) else Color(0xFF475569), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // If lines are empty, provide standard welcome output matching mockup
                if (lines.isEmpty()) {
                    item {
                        Text(
                            text = "klix@mobile:/workspace$",
                            style = KlixTypography.codeEditor,
                            color = Color(0xFF86EFAC)
                        )
                    }
                    item {
                        Text(
                            text = "python3 palindrome.py",
                            style = KlixTypography.codeEditor,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                    item {
                        Text(
                            text = "✓ Server & checks passed in 0.4s",
                            style = KlixTypography.codeEditor,
                            color = Color(0xFF86EFAC)
                        )
                    }
                } else {
                    items(lines, key = { it.id }) { line ->
                        val textColor = when (line.color) {
                            TerminalColors.Primary -> Color(0xFF86EFAC)
                            TerminalColors.Secondary -> Color(0xFF38BDF8)
                            TerminalColors.Warning -> Color(0xFFFBBF24)
                            TerminalColors.Error -> Color(0xFFF87171)
                            TerminalColors.TextSecondary -> Color(0xFF94A3B8)
                            TerminalColors.TextMuted -> Color(0xFF64748B)
                            Color.Unspecified -> Color(0xFFF8FAFC)
                            else -> line.color
                        }
                        Text(
                            text = line.text,
                            style = KlixTypography.codeEditor,
                            color = textColor,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }

                if (isExecuting) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFFD5FB66)
                            )
                            Text(
                                text = "Running...",
                                style = KlixTypography.codeEditor,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Bottom-right overlay inside card: R.drawable.mascot_waving with speech bubble "All green!"
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2638))
                        .border(1.dp, Color(0xFF333F58), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "All green!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.mascot_waving),
                    contentDescription = "Klix Bot Waving",
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        // 4. Shortcuts Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Shortcuts",
                style = KlixTypography.bodyPrimary,
                fontWeight = FontWeight.Bold,
                color = KlixInkPrimary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Key 1: + TERM (Filled with #D5FB66 lime)
                ShortcutKeycap(
                    label = "+ TERM",
                    isHighlight = true,
                    onClick = { viewModel.addNewTerminal() }
                )

                // Key 2: ESC
                ShortcutKeycap(
                    label = "ESC",
                    isHighlight = false,
                    onClick = { }
                )

                // Key 3: TAB
                ShortcutKeycap(
                    label = "TAB",
                    isHighlight = false,
                    onClick = {
                        inputCommand += "    "
                        viewModel.setInputDraft(inputCommand, activeTabId)
                    }
                )

                // Key 4: CTRL-C
                ShortcutKeycap(
                    label = "CTRL-C",
                    isHighlight = false,
                    onClick = { viewModel.cancelExecution() }
                )

                // Key 5: ↑
                ShortcutKeycap(
                    label = "↑",
                    isHighlight = false,
                    onClick = {
                        viewModel.getPreviousHistory()?.let {
                            inputCommand = it
                            viewModel.setInputDraft(it, activeTabId)
                        }
                    }
                )

                // Key 6: ↓
                ShortcutKeycap(
                    label = "↓",
                    isHighlight = false,
                    onClick = {
                        viewModel.getNextHistory()?.let {
                            inputCommand = it
                            viewModel.setInputDraft(it, activeTabId)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // 5. Bottom Command Input Bar: "$ Run a command..." with circular send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(KlixSurfaceCardItem)
                .border(1.dp, KlixBorderHairline, RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F)
            )

            OutlinedTextField(
                value = inputCommand,
                onValueChange = {
                    inputCommand = it
                    viewModel.setInputDraft(it, activeTabId)
                },
                placeholder = {
                    Text(
                        text = "Run a command...",
                        style = KlixTypography.codeEditor,
                        color = KlixInkSecondary
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = KlixInkPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputCommand.isNotBlank()) {
                        if (isExecuting) {
                            viewModel.sendInput(inputCommand)
                        } else {
                            viewModel.executeCommand(inputCommand)
                        }
                        inputCommand = ""
                        viewModel.setInputDraft("", activeTabId)
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = KlixInkPrimary,
                    unfocusedTextColor = KlixInkPrimary
                )
            )

            if (isExecuting) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .pressEffect(0.92f)
                        .clickable { viewModel.cancelExecution() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                val hasInput = inputCommand.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFFD5FB66) else Color(0xFF0C0D14))
                        .pressEffect(0.92f)
                        .clickable(enabled = hasInput) {
                            if (hasInput) {
                                viewModel.executeCommand(inputCommand)
                                inputCommand = ""
                                viewModel.setInputDraft("", activeTabId)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute",
                        tint = if (isDark) Color(0xFF0C0D14) else Color(0xFFFFFFFF),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
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
private fun ShortcutKeycap(
    label: String,
    isHighlight: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isHighlight) Color(0xFFD5FB66) else KlixSurfaceCardItem
    val textCol = if (isHighlight) Color(0xFF0C0D14) else KlixInkPrimary

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(bg)
            .border(1.dp, if (isHighlight) Color(0xFFD5FB66) else KlixBorderHairline, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.92f)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = KlixTypography.codePill,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
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
                style = KlixTypography.sectionHeader,
                color = KlixInkPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = renameInput,
                onValueChange = { renameInput = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(renameInput.trim().ifEmpty { initialName }) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
