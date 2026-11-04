package com.klix.code.ui.components.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.agent.command.SlashCommand
import com.klix.code.data.agent.harness.HarnessStatus
import com.klix.code.data.agent.harness.PendingApproval
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
import com.klix.code.data.agent.session.ChatSession
import com.klix.code.data.agent.session.TaggedFileItem
import com.klix.code.data.agent.tools.ToolCallRequest
import com.klix.code.data.agent.tools.ToolCallResult
import com.klix.code.data.model.FileDiff
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.DiffAddedText
import com.klix.code.ui.theme.DiffRemovedText
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatPanel(
    messages: List<ChatMessage>,
    status: HarnessStatus,
    statusMessage: String?,
    pendingApproval: PendingApproval?,
    settings: ProviderSettings,
    totalTokens: Int,
    sessions: List<ChatSession> = emptyList(),
    activeSessionId: String = "",
    availableFiles: List<TaggedFileItem> = emptyList(),
    availableSlashCommands: List<SlashCommand> = emptyList(),
    onOpenSlashCommandManager: () -> Unit = {},
    onCreateSession: () -> Unit = {},
    onSwitchSession: (String) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    onRenameSession: (String, String) -> Unit = { _, _ -> },
    onSendPrompt: (String) -> Unit,
    onApproveTool: () -> Unit,
    onRejectTool: () -> Unit,
    onAnswerQuestion: (List<String>) -> Unit = {},
    onCancelTask: () -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onReviewDiff: (FileDiff) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    var showSessionSheet by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var renameTitleInput by remember { mutableStateOf("") }

    val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.firstOrNull()

    // Auto-scroll on new messages or status changes
    LaunchedEffect(messages.size, status) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Inspect "@" tag autocomplete from cursor position
    val cursor = promptInput.selection.end
    val textBeforeCursor = promptInput.text.take(cursor)
    val lastAtIndex = textBeforeCursor.lastIndexOf('@')
    val isTaggingActive = if (lastAtIndex >= 0) {
        val queryCandidate = textBeforeCursor.substring(lastAtIndex + 1)
        !queryCandidate.contains(' ') && !queryCandidate.contains('\n')
    } else false

    val tagQuery = if (isTaggingActive) textBeforeCursor.substring(lastAtIndex + 1) else ""
    val matchingTaggedItems = if (isTaggingActive) {
        availableFiles.filter {
            it.path.contains(tagQuery, ignoreCase = true) || it.fileName.contains(tagQuery, ignoreCase = true)
        }.take(8)
    } else emptyList()

    // Inspect "/" slash command autocomplete from cursor position
    val lastSlashIndex = textBeforeCursor.lastIndexOf('/')
    val isSlashCommandActive = if (lastSlashIndex >= 0 && !isTaggingActive) {
        val isAtStart = lastSlashIndex == 0 || textBeforeCursor[lastSlashIndex - 1].isWhitespace()
        if (isAtStart) {
            val queryCandidate = textBeforeCursor.substring(lastSlashIndex + 1)
            !queryCandidate.contains(' ') && !queryCandidate.contains('\n')
        } else false
    } else false

    val slashQuery = if (isSlashCommandActive) textBeforeCursor.substring(lastSlashIndex + 1) else ""
    val matchingSlashCommands = if (isSlashCommandActive) {
        availableSlashCommands.filter {
            it.trigger.contains(slashQuery, ignoreCase = true) ||
            it.description.contains(slashQuery, ignoreCase = true)
        }
    } else emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Modern Header Bar
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
            // Left: Bot Avatar & Interactive Session Switcher Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Agent",
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Session Selector Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
                    modifier = Modifier.clickable { showSessionSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = activeSession?.title ?: "Chat",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 130.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Chat",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Right: Actions (New Chat, Settings, Clear, Close)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // New Chat Button
                IconButton(
                    onClick = onCreateSession,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Chat History Drawer Button
                IconButton(
                    onClick = { showSessionSheet = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onClearChat, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Session",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onOpenSlashCommandManager, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Slash Commands",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Subheader Info: Model Chip & Token Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = settings.activeModel.substringAfterLast('/'),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AccentPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = settings.activeProvider.displayName,
                    fontSize = 10.5.sp,
                    color = TextMuted
                )
            }

            Text(
                text = if (totalTokens > 0) "$totalTokens tokens used" else "Ready",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        // Messages Stream Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyAgentState(
                    availableFiles = availableFiles,
                    onSelectSuggestion = { suggestion ->
                        promptInput = TextFieldValue(
                            text = suggestion,
                            selection = TextRange(suggestion.length)
                        )
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            activeModel = settings.activeModel,
                            onReviewDiff = onReviewDiff
                        )
                    }

                    // Pending Approval Card
                    if (pendingApproval != null) {
                        item {
                            PendingApprovalCard(
                                pending = pendingApproval,
                                onApprove = onApproveTool,
                                onReject = onRejectTool,
                                onReviewDiff = onReviewDiff,
                                onAnswerQuestion = onAnswerQuestion
                            )
                        }
                    }

                    // Status Indicator
                    if (status == HarnessStatus.THINKING || status == HarnessStatus.EXECUTING_TOOL) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.2.dp,
                                    color = AccentPrimary
                                )
                                Text(
                                    text = statusMessage ?: "Processing...",
                                    fontSize = 12.5.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tag Autocomplete Popup (Displays above text input when typing "@")
        AnimatedVisibility(
            visible = isTaggingActive && matchingTaggedItems.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "PROJECT FILES & FOLDERS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "${matchingTaggedItems.size} match${if (matchingTaggedItems.size > 1) "es" else ""}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(matchingTaggedItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Replace the current @query with @item.path
                                        val before = textBeforeCursor.substring(0, lastAtIndex)
                                        val after = promptInput.text.substring(cursor)
                                        val insertion = "@${item.path} "
                                        val newText = before + insertion + after
                                        val newCursor = before.length + insertion.length
                                        promptInput = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newCursor)
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) AccentWarning else AccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.fileName,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    if (item.parentPath.isNotBlank()) {
                                        Text(
                                            text = item.parentPath,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (item.isDirectory) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(DarkSurfaceVariant)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "DIR",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentWarning
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Slash Command Autocomplete Popup (Displays above text input when typing "/")
        AnimatedVisibility(
            visible = isSlashCommandActive && matchingSlashCommands.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "SLASH COMMANDS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${matchingSlashCommands.size} match${if (matchingSlashCommands.size > 1) "es" else ""}",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable { onOpenSlashCommandManager() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+ Custom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentPrimary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 220.dp)
                    ) {
                        items(matchingSlashCommands) { cmd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Replace current /query with /command
                                        val before = textBeforeCursor.substring(0, lastSlashIndex)
                                        val after = promptInput.text.substring(cursor)
                                        val insertion = "/${cmd.trigger} "
                                        val newText = before + insertion + after
                                        val newCursor = before.length + insertion.length
                                        promptInput = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newCursor)
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (cmd.isBuiltIn) DarkSurfaceVariant else Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "/${cmd.trigger}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (cmd.isBuiltIn) AccentPrimary else Color(0xFFA78BFA)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cmd.description,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!cmd.isBuiltIn) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "CUSTOM",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA78BFA)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Bar
        HorizontalDivider(color = DarkBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Quick "/" Slash Command Button
            IconButton(
                onClick = {
                    val currentText = promptInput.text
                    val sel = promptInput.selection.end
                    val before = currentText.take(sel)
                    val after = currentText.drop(sel)
                    val needSpace = before.isNotEmpty() && !before.endsWith(" ")
                    val insert = if (needSpace) " /" else "/"
                    val newText = before + insert + after
                    val newSel = sel + insert.length
                    promptInput = TextFieldValue(text = newText, selection = TextRange(newSel))
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Text(
                    text = "/",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
            }

            // Quick "@" Tag Button
            IconButton(
                onClick = {
                    val currentText = promptInput.text
                    val sel = promptInput.selection.end
                    val before = currentText.take(sel)
                    val after = currentText.drop(sel)
                    val needSpace = before.isNotEmpty() && !before.endsWith(" ")
                    val insert = if (needSpace) " @" else "@"
                    val newText = before + insert + after
                    val newSel = sel + insert.length
                    promptInput = TextFieldValue(text = newText, selection = TextRange(newSel))
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = "Mention File/Folder",
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = {
                    Text(
                        text = "Ask Klix, type / for commands, or @ for files...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            val isRunning = status == HarnessStatus.THINKING || status == HarnessStatus.EXECUTING_TOOL
            if (isRunning) {
                IconButton(
                    onClick = onCancelTask,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AccentError)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                val hasInput = promptInput.text.isNotBlank()
                IconButton(
                    onClick = {
                        if (hasInput) {
                            onSendPrompt(promptInput.text.trim())
                            promptInput = TextFieldValue("")
                        }
                    },
                    enabled = hasInput,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (hasInput) AccentPrimary else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (hasInput) Color.White else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Chat History / Sessions Modal Bottom Sheet
    if (showSessionSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSessionSheet = false },
            sheetState = sheetState,
            containerColor = DarkBg,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Chat History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${sessions.size}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onCreateSession()
                            showSessionSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(8.dp))

                // Sessions List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        val isActive = session.id == activeSessionId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) DarkSurfaceVariant else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isActive) 1.dp else 0.5.dp,
                                if (isActive) AccentPrimary else DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSwitchSession(session.id)
                                    showSessionSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = session.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isActive) AccentPrimary else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(AccentPrimary.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentPrimary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = session.previewText,
                                        fontSize = 11.5.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatRelativeTime(session.updatedAt),
                                            fontSize = 10.5.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "${session.messages.size} msgs",
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted
                                        )
                                        if (session.totalTokens > 0) {
                                            Text(
                                                text = "${session.totalTokens} tokens",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            sessionToRename = session
                                            renameTitleInput = session.title
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = TextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Session Dialog
    sessionToRename?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameTitleInput.isNotBlank()) {
                            onRenameSession(session.id, renameTitleInput.trim())
                        }
                        sessionToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun EmptyAgentState(
    availableFiles: List<TaggedFileItem>,
    onSelectSuggestion: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Klix Agentic Assistant",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Plan features, inspect files, or tag project resources using @ for instant surgical edits.",
            fontSize = 12.5.sp,
            color = TextSecondary,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Tag Suggestions from project files if available
        if (availableFiles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Quick Mentions:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableFiles.filter { !it.isDirectory }.take(3).forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
                        modifier = Modifier.clickable { onSelectSuggestion("Inspect @${item.path} and summarize logic") }
                    ) {
                        Text(
                            text = "@${item.fileName}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        val suggestions = listOf(
            "Inspect workspace files and summarize architecture",
            "Find all TODOs and unfinished items",
            "Write comprehensive unit tests",
            "Check for potential crashes or race conditions"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSuggestion(suggestion) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AccentWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = suggestion,
                            fontSize = 12.5.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    activeModel: String,
    onReviewDiff: (FileDiff) -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    var isMessageCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Assistant Message Header
        if (!isUser) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AccentPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = "Klix AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = activeModel.substringAfterLast('/'),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }

                // Copy Response Button
                if (message.content.isNotBlank()) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            isMessageCopied = true
                            scope.launch {
                                delay(2000)
                                isMessageCopied = false
                            }
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = if (isMessageCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Response",
                            tint = if (isMessageCopied) Color(0xFF10B981) else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Reasoning / Thinking card (Expandable)
        if (!isUser && !message.thought.isNullOrBlank()) {
            ReasoningCard(thought = message.thought)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Message Content Body
        if (message.content.isNotBlank()) {
            if (isUser) {
                // User Message Bubble
                Column(horizontalAlignment = Alignment.End) {
                    if (!message.command.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp, end = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentPrimary.copy(alpha = 0.2f))
                                    .border(0.5.dp, AccentPrimary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = message.command,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = 14.dp,
                                    bottomEnd = 3.dp
                                )
                            )
                            .background(AccentPrimary.copy(alpha = 0.15f))
                            .border(
                                0.75.dp,
                                AccentPrimary.copy(alpha = 0.35f),
                                RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 3.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = buildMarkdownAnnotatedString(message.content, TextPrimary),
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 21.sp
                        )
                    }
                }
            } else {
                // Assistant Message with rich typography & syntax highlighted code blocks
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    ),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MarkdownMessageView(
                        content = message.content,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // Tool calls rendered inside assistant messages
        message.toolCalls.forEach { tc ->
            Spacer(modifier = Modifier.height(6.dp))
            ToolCallCard(toolCall = tc, onReviewDiff = onReviewDiff)
        }

        // Tool results returned
        message.toolResults.forEach { res ->
            Spacer(modifier = Modifier.height(6.dp))
            ToolResultCard(result = res, onReviewDiff = onReviewDiff)
        }
    }
}

@Composable
private fun ReasoningCard(thought: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
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
                        tint = AccentSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Reasoning Process",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Text(
                        text = "(${thought.length} chars)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        text = thought.trim(),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .background(DarkBg)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolCallCard(
    toolCall: ToolCallRequest,
    onReviewDiff: (FileDiff) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val icon = getToolIcon(toolCall.name)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = toolCall.name.removePrefix("default_api:"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Arguments:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = toolCall.rawArguments,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg)
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolResultCard(
    result: ToolCallResult,
    onReviewDiff: (FileDiff) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (result.isSuccess) AccentSecondary.copy(alpha = 0.5f) else AccentError.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (result.isSuccess) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (result.isSuccess) AccentSecondary else AccentError,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${result.name.removePrefix("default_api:")} result",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // If diff is attached, show review button
                if (result.proposedDiff != null) {
                    val diff = result.proposedDiff
                    Button(
                        onClick = { onReviewDiff(diff) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Difference,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${diff.additionsCount} / -${diff.deletionsCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (result.proposedDiff == null || isExpanded) {
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = if (result.isSuccess) result.output else (result.error ?: "Error"),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (result.isSuccess) TextPrimary else AccentError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(DarkBg)
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingApprovalCard(
    pending: PendingApproval,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onReviewDiff: (FileDiff) -> Unit,
    onAnswerQuestion: ((List<String>) -> Unit)? = null
) {
    val isQuestion = !pending.questionOptions.isNullOrEmpty()
    val selectedOptions = remember(pending.toolCall.id) {
        val initial = if (isQuestion && !pending.questionOptions.isNullOrEmpty()) {
            listOf(pending.questionOptions.first())
        } else {
            emptyList()
        }
        androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(initial) }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isQuestion) AccentPrimary else AccentWarning),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isQuestion) Icons.Default.ChatBubbleOutline else Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (isQuestion) AccentPrimary else AccentWarning,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isQuestion) "REQUIREMENTS CLARIFICATION" else "ACTION APPROVAL REQUIRED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isQuestion) AccentPrimary else AccentWarning
                )
            }

            Text(
                text = pending.promptMessage,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // If question options available, render interactive options list
            if (isQuestion) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (pending.allowMultiple) "Select one or more options:" else "Select an option:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    pending.questionOptions.forEachIndexed { idx, opt ->
                        val isSelected = selectedOptions.contains(opt)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AccentPrimary.copy(alpha = 0.18f) else DarkBg)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) AccentPrimary else DarkBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (pending.allowMultiple) {
                                        if (isSelected) {
                                            selectedOptions.remove(opt)
                                        } else {
                                            selectedOptions.add(opt)
                                        }
                                    } else {
                                        selectedOptions.clear()
                                        selectedOptions.add(opt)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentPrimary else Color.Transparent)
                                        .border(1.5.dp, if (isSelected) AccentPrimary else TextMuted, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DarkBg,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${idx + 1}. $opt",
                                    fontSize = 12.sp,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // If diff available, offer Review button
            if (pending.diff != null) {
                val diff = pending.diff
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkBg)
                        .clickable { onReviewDiff(diff) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Difference,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = diff.filePath,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "+${diff.additionsCount}",
                            fontSize = 11.sp,
                            color = DiffAddedText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "-${diff.deletionsCount}",
                            fontSize = 11.sp,
                            color = DiffRemovedText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentError),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(if (isQuestion) "Decline" else "Deny", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (isQuestion) {
                            if (onAnswerQuestion != null) {
                                onAnswerQuestion(selectedOptions.toList())
                            } else {
                                onApprove()
                            }
                        } else {
                            onApprove()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isQuestion) AccentPrimary else AccentSecondary),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !isQuestion || selectedOptions.isNotEmpty()
                ) {
                    Text(if (isQuestion) "Confirm Option" else "Approve & Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getToolIcon(name: String): ImageVector {
    val clean = name.removePrefix("default_api:")
    return when (clean) {
        "run_command" -> Icons.Default.Terminal
        "search_files" -> Icons.Default.Search
        "list_dir" -> Icons.Default.Folder
        "write_file", "edit_file", "multi_replace_file_content" -> Icons.Default.Difference
        "ask_question" -> Icons.Default.ChatBubbleOutline
        else -> Icons.Default.Tune
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
