package com.klix.code.ui.components.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import com.klix.code.ui.theme.LocalIsDarkTheme
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.res.painterResource
import com.klix.code.R
import com.klix.code.ui.theme.HeroCardBgColor
import com.klix.code.ui.theme.LilacCardBgColor
import com.klix.code.ui.theme.SoftCardBgColor
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
import com.klix.code.data.agent.model.ModelProvider
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
import com.klix.code.ui.components.glass.GlassCard
import com.klix.code.ui.components.glass.GlassButton
import com.klix.code.ui.components.glass.GlassBadge
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.GlassSurfacePillColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.LimeGlowColor
import com.klix.code.ui.theme.LimeDimColor
import com.klix.code.ui.theme.LimeBorderColor
import com.klix.code.ui.theme.CyanBrandColor
import com.klix.code.ui.theme.CyanGlowColor
import com.klix.code.ui.theme.CyanDimColor
import com.klix.code.ui.theme.CyanBorderColor
import com.klix.code.ui.theme.OnAccentPrimaryColor
import com.klix.code.ui.theme.KlixBgRootGradient
import com.klix.code.ui.theme.KlixSurfaceSheet
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixBorderActiveLime
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixPrimaryCtaBg
import com.klix.code.ui.theme.KlixPrimaryCtaText
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.components.KlixPrimaryPillButton
import androidx.compose.material.icons.filled.Bolt
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
    val isDark = LocalIsDarkTheme.current

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
        // Top Modern Header Bar matching Screen 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KlixBgRootGradient)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Screen Title
            Text(
                text = "Klix AI Agent",
                style = KlixTypography.screenTitle,
                color = KlixInkPrimary
            )

            // Right: Dropdown Pill "Chat 1 ▾" + Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Session Selector Capsule Pill "Chat 1 ▾"
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(KlixSurfaceCardItem)
                        .border(1.dp, KlixBorderHairline, CircleShape)
                        .pressEffect(0.95f)
                        .clickable { showSessionSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = activeSession?.title ?: "Chat 1",
                            style = KlixTypography.bodySecondary,
                            fontWeight = FontWeight.Bold,
                            color = KlixInkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Chat",
                            tint = KlixInkSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp).pressEffect(0.9f)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = KlixInkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Subheader Info: Model Chip & Token Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurfaceColor)
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassBadge(
                    text = settings.activeModel.substringAfterLast('/'),
                    containerColor = CyanDimColor,
                    textColor = CyanBrandColor
                )
                Text(
                    text = settings.activeProvider.displayName,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Text(
                text = if (totalTokens > 0) "$totalTokens tokens" else "Ready",
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
                    activeProvider = settings.activeProvider,
                    activeModel = settings.activeModel,
                    onOpenSettings = onOpenSettings,
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
                                    .background(DarkSurfaceVariant)
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
                                        .background(if (cmd.isBuiltIn) DarkSurfaceVariant else Color(0xFF2A2040))
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
                                            .background(Color(0xFF2A2040))
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

        // Bottom Floating Pill Composer Dock (Screen 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KlixBgRootGradient)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(KlixSurfaceCardItem)
                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(28.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick "/" Slash Command Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF22293D) else Color(0xFFE2EDF7))
                        .clickable {
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
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "/",
                        style = KlixTypography.codePill,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F)
                    )
                }

                // Quick "@" Tag Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF22293D) else Color(0xFFE2EDF7))
                        .clickable {
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
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = "Mention File",
                        tint = KlixInkSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                BasicTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    singleLine = true,
                    textStyle = KlixTypography.bodyPrimary.copy(color = KlixInkPrimary),
                    cursorBrush = SolidColor(if (isDark) Color(0xFFD5FB66) else Color(0xFF0C0D14)),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (promptInput.text.isEmpty()) {
                                Text(
                                    text = "Ask Klix or type / ...",
                                    style = KlixTypography.bodyPrimary,
                                    color = KlixInkSecondary,
                                    maxLines = 1
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                val isRunning = status == HarnessStatus.THINKING || status == HarnessStatus.EXECUTING_TOOL
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentError)
                            .pressEffect(0.92f)
                            .clickable(onClick = onCancelTask),
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
                    val hasInput = promptInput.text.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFFD5FB66) else Color(0xFF0C0D14))
                            .pressEffect(0.92f)
                            .clickable(enabled = hasInput) {
                                if (hasInput) {
                                    onSendPrompt(promptInput.text.trim())
                                    promptInput = TextFieldValue("")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (isDark) Color(0xFF0C0D14) else Color(0xFFFFFFFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }

    // Chat History / Sessions Modal Bottom Sheet
    // Chat History / Sessions Modal Bottom Sheet (Screen 9: Chat History & Klix AI Sessions)
    if (showSessionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSessionSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .background(KlixBgRootGradient)
            ) {
                // Top Header Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mascot_waving),
                        contentDescription = "Klix Bot Waving",
                        modifier = Modifier.size(68.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Klix AI Sessions",
                            style = KlixTypography.sheetHeader,
                            fontWeight = FontWeight.ExtraBold,
                            color = KlixInkPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Pill chip [Chat Bubble] 3 Chats
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF22293D) else Color(0xFFE2EDF7))
                                    .border(1.dp, KlixBorderHairline, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${sessions.size.coerceAtLeast(3)} Chats",
                                        style = KlixTypography.codePill,
                                        color = KlixInkPrimary
                                    )
                                }
                            }
                            // Pill chip [Bolt] 50K Auto-Compacted
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF22293D) else Color(0xFFE2EDF7))
                                    .border(1.dp, KlixBorderHairline, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "50K Auto-Compacted",
                                        style = KlixTypography.codePill,
                                        color = KlixInkPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Elevated Sheet (RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = KlixSurfaceSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Centered drag handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF333D55) else Color(0xFFCBD5E1))
                            )
                        }

                        // Sheet Header Row: "Chat History" on left, "ACTIVE" on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Chat History",
                                style = KlixTypography.sheetHeader,
                                color = KlixInkPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFD5FB66))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFF0C0D14)
                                )
                            }
                        }

                        // 3 Stacked Session Cards
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Card 1: Chat 1 • palindrome.py
                            SessionHistoryCard(
                                title = if (sessions.isNotEmpty()) sessions[0].title else "Chat 1 • palindrome.py",
                                subtitle = "Python",
                                langLogoRes = R.drawable.ic_logo_python,
                                modelLogoRes = R.drawable.ic_logo_gemini,
                                isHighlighted = false,
                                onClick = {
                                    if (sessions.isNotEmpty()) onSwitchSession(sessions[0].id)
                                    showSessionSheet = false
                                },
                                onRename = {
                                    if (sessions.isNotEmpty()) {
                                        sessionToRename = sessions[0]
                                        renameTitleInput = sessions[0].title
                                    }
                                },
                                onDelete = {
                                    if (sessions.isNotEmpty()) onDeleteSession(sessions[0].id)
                                }
                            )

                            // Card 2: Chat 2 • orbit-ui (Soft Lavender-Indigo Tint)
                            SessionHistoryCard(
                                title = if (sessions.size > 1) sessions[1].title else "Chat 2 • orbit-ui",
                                subtitle = "Anthropic and Kotlin",
                                langLogoRes = R.drawable.ic_logo_kotlin,
                                modelLogoRes = R.drawable.ic_logo_claude,
                                isHighlighted = true,
                                onClick = {
                                    if (sessions.size > 1) onSwitchSession(sessions[1].id)
                                    showSessionSheet = false
                                },
                                onRename = {
                                    if (sessions.size > 1) {
                                        sessionToRename = sessions[1]
                                        renameTitleInput = sessions[1].title
                                    }
                                },
                                onDelete = {
                                    if (sessions.size > 1) onDeleteSession(sessions[1].id)
                                }
                            )

                            // Card 3: Chat 3 • study-buddy
                            SessionHistoryCard(
                                title = if (sessions.size > 2) sessions[2].title else "Chat 3 • study-buddy",
                                subtitle = "Node.js",
                                langLogoRes = R.drawable.ic_logo_nodejs,
                                modelLogoRes = R.drawable.ic_logo_openai,
                                isHighlighted = false,
                                onClick = {
                                    if (sessions.size > 2) onSwitchSession(sessions[2].id)
                                    showSessionSheet = false
                                },
                                onRename = {
                                    if (sessions.size > 2) {
                                        sessionToRename = sessions[2]
                                        renameTitleInput = sessions[2].title
                                    }
                                },
                                onDelete = {
                                    if (sessions.size > 2) onDeleteSession(sessions[2].id)
                                }
                            )

                            // Any additional user sessions beyond 3
                            if (sessions.size > 3) {
                                sessions.drop(3).forEach { s ->
                                    SessionHistoryCard(
                                        title = s.title,
                                        subtitle = s.previewText.ifEmpty { "Chat Session" },
                                        langLogoRes = R.drawable.ic_logo_python,
                                        modelLogoRes = R.drawable.ic_logo_gemini,
                                        isHighlighted = s.id == activeSessionId,
                                        onClick = {
                                            onSwitchSession(s.id)
                                            showSessionSheet = false
                                        },
                                        onRename = {
                                            sessionToRename = s
                                            renameTitleInput = s.title
                                        },
                                        onDelete = {
                                            onDeleteSession(s.id)
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom CTA: + Start New Chat
                        KlixPrimaryPillButton(
                            text = "+ Start New Chat",
                            onClick = {
                                onCreateSession()
                                showSessionSheet = false
                            }
                        )
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
    activeProvider: ModelProvider = ModelProvider.GEMINI,
    activeModel: String = "gemini-2.5-flash",
    onOpenSettings: () -> Unit = {},
    onSelectSuggestion: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Quick Mentions & Model
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(KlixSurfaceBentoGroup)
                .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick Mentions & Model",
                    style = KlixTypography.sectionHeader,
                    color = KlixInkPrimary
                )

                // Row 1: Pill chip [Active Provider Logo] activeModel
                val logoRes = when (activeProvider) {
                    ModelProvider.ANTHROPIC -> R.drawable.ic_logo_claude
                    ModelProvider.OPENAI -> R.drawable.ic_logo_openai
                    ModelProvider.CUSTOM_OPENAI_COMPATIBLE -> R.drawable.ic_logo_openrouter
                    ModelProvider.GEMINI -> R.drawable.ic_logo_gemini
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF22293D) else Color(0xFFEBE6F8))
                        .border(1.dp, KlixBorderHairline, CircleShape)
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = activeModel.substringAfterLast('/'),
                            style = KlixTypography.codePill,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFD5FB66) else Color(0xFF4D7C0F)
                        )
                    }
                }

                // Row 2: Two side-by-side file mention pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // File 1: palindrome.py
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(KlixSurfaceCardItem)
                            .border(1.dp, KlixBorderHairline, CircleShape)
                            .clickable { onSelectSuggestion("Explain palindrome.py") }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo_python),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (availableFiles.any { it.fileName.endsWith(".py") }) {
                                    availableFiles.first { it.fileName.endsWith(".py") }.fileName
                                } else "palindrome.py",
                                style = KlixTypography.codePill,
                                color = KlixInkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // File 2: MainActivity.kt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(KlixSurfaceCardItem)
                            .border(1.dp, KlixBorderHairline, CircleShape)
                            .clickable { onSelectSuggestion("Inspect MainActivity.kt") }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo_kotlin),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (availableFiles.any { it.fileName.endsWith(".kt") }) {
                                    availableFiles.first { it.fileName.endsWith(".kt") }.fileName
                                } else "MainActivity.kt",
                                style = KlixTypography.codePill,
                                color = KlixInkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Card 2: What shall we do?
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(KlixSurfaceBentoGroup)
                .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "What shall we do?",
                    style = KlixTypography.sectionHeader,
                    color = KlixInkPrimary
                )

                val actionItems = listOf(
                    Triple(R.drawable.ic_icon_layers, "Summarize workspace architecture", "Summarize workspace architecture"),
                    Triple(R.drawable.ic_icon_search_code, "Find all TODOs & unfinished items", "Find all TODOs & unfinished items"),
                    Triple(R.drawable.ic_icon_test_tube, "Write unit tests for palindrome.py", "Write unit tests for palindrome.py"),
                    Triple(R.drawable.ic_icon_shield_check, "Check for crashes or edge cases", "Check for crashes or edge cases")
                )

                actionItems.forEach { (iconRes, label, prompt) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(KlixSurfaceCardItem)
                            .border(1.dp, KlixBorderHairline, RoundedCornerShape(23.dp))
                            .clickable { onSelectSuggestion(prompt) }
                            .pressEffect(0.96f)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            style = KlixTypography.bodyPrimary,
                            color = KlixInkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    Image(
                        painter = painterResource(id = R.drawable.klix_logo),
                        contentDescription = "Klix Mascot",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                    )

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
                                    .background(LimeDimColor)
                                    .border(0.5.dp, LimeBorderColor, RoundedCornerShape(4.dp))
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

                    val userBubbleShape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    )
                    Box(
                        modifier = Modifier
                            .clip(userBubbleShape)
                            .background(CyanDimColor)
                            .glassBorder(userBubbleShape, strokeWidth = 1.dp, highlightAlpha = 0.35f, midAlpha = 0.12f)
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
                val assistantShape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                )
                Surface(
                    shape = assistantShape,
                    color = GlassSurfaceElevatedColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(assistantShape, strokeWidth = 1.dp, highlightAlpha = 0.25f, midAlpha = 0.08f)
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
        color = DarkSurface,
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
    val isDark = LocalIsDarkTheme.current
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (result.isSuccess) CyanBorderColor else (if (isDark) Color(0xFF7F1D1D) else Color(0xFFFCA5A5))
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
        shape = RoundedCornerShape(18.dp),
        color = GlassSurfaceElevatedColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isQuestion) CyanBorderColor else LimeBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(RoundedCornerShape(18.dp), strokeWidth = 1.dp, highlightAlpha = 0.35f, midAlpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isQuestion) Icons.Default.ChatBubbleOutline else Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (isQuestion) CyanBrandColor else LimeBrandColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isQuestion) "REQUIREMENTS CLARIFICATION" else "ACTION APPROVAL REQUIRED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isQuestion) CyanBrandColor else LimeBrandColor
                )
            }

            Text(
                text = pending.promptMessage,
                fontSize = 13.5.sp,
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyanDimColor else DarkBg)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) CyanBrandColor else DarkBorder,
                                    shape = RoundedCornerShape(8.dp)
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
                                        .background(if (isSelected) CyanBrandColor else Color.Transparent)
                                        .border(1.5.dp, if (isSelected) CyanBrandColor else TextMuted, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                             imageVector = Icons.Default.Check,
                                             contentDescription = null,
                                             tint = OnAccentPrimaryColor,
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
                        .clip(RoundedCornerShape(8.dp))
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
                            tint = CyanBrandColor,
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
                    modifier = Modifier.weight(1f).pressEffect(0.95f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentError),
                    shape = CircleShape
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
                    modifier = Modifier.weight(1f).pressEffect(0.95f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isQuestion) CyanBrandColor else LimeBrandColor,
                        contentColor = OnAccentPrimaryColor
                    ),
                    shape = CircleShape,
                    enabled = !isQuestion || selectedOptions.isNotEmpty()
                ) {
                    Text(
                        text = if (isQuestion) "Confirm Option" else "Approve & Run",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
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

@Composable
private fun SessionHistoryCard(
    title: String,
    subtitle: String,
    langLogoRes: Int,
    modelLogoRes: Int,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isHighlighted) {
        if (isDark) Color(0xFF26233B) else Color(0xFFEBE6F8)
    } else {
        KlixSurfaceCardItem
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, if (isHighlighted) Color(0xFFB594EB) else KlixBorderHairline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.97f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = KlixTypography.cardTitle,
                color = KlixInkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(id = langLogoRes),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = subtitle,
                    style = KlixTypography.bodySecondary,
                    color = KlixInkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Image(
            painter = painterResource(id = modelLogoRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
    }
}

