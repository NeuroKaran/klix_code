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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.data.agent.harness.HarnessStatus
import com.klix.code.data.agent.harness.PendingApproval
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
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

@Composable
fun AgentChatPanel(
    messages: List<ChatMessage>,
    status: HarnessStatus,
    statusMessage: String?,
    pendingApproval: PendingApproval?,
    settings: ProviderSettings,
    totalTokens: Int,
    onSendPrompt: (String) -> Unit,
    onApproveTool: () -> Unit,
    onRejectTool: () -> Unit,
    onCancelTask: () -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onReviewDiff: (FileDiff) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(messages.size, status) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header Bar
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

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Klix AI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Model Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = settings.activeModel.substringAfterLast('/'),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AccentPrimary
                            )
                        }
                    }

                    // Token count / status
                    Text(
                        text = if (totalTokens > 0) "$totalTokens tokens used" else settings.activeProvider.displayName,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearChat, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Chat",
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

        // Messages Stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // Empty State / Suggestions
                EmptyAgentState(
                    onSelectSuggestion = { suggestion ->
                        promptInput = suggestion
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
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
                                onReviewDiff = onReviewDiff
                            )
                        }
                    }

                    // Status Indicator
                    if (status == HarnessStatus.THINKING || status == HarnessStatus.EXECUTING_TOOL) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentPrimary
                                )
                                Text(
                                    text = statusMessage ?: "Processing...",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
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
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ask Klix to code, inspect, or refactor...", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
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
                IconButton(
                    onClick = {
                        if (promptInput.isNotBlank()) {
                            onSendPrompt(promptInput.trim())
                            promptInput = ""
                        }
                    },
                    enabled = promptInput.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (promptInput.isNotBlank()) AccentPrimary else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (promptInput.isNotBlank()) Color.White else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAgentState(onSelectSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccentPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Klix AI Agent",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Describe a task, feature, or bugfix. All changes are shown as diffs before applying.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val suggestions = listOf(
            "Inspect workspace files and summarize structure",
            "Find all TODOs and unfinished items",
            "Write a unit test for diff calculations",
            "Refactor and clean up active file"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .clickable { onSelectSuggestion(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onReviewDiff: (FileDiff) -> Unit
) {
    val isUser = message.role == MessageRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // If assistant produced internal reasoning / thoughts, render expandable card
        if (!isUser && !message.thought.isNullOrBlank()) {
            ReasoningCard(thought = message.thought)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (message.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(if (isUser) DarkSurfaceVariant else DarkSurface)
                    .border(
                        0.5.dp,
                        if (isUser) AccentPrimary.copy(alpha = 0.5f) else DarkBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        text = thought.trim(),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        lineHeight = 15.sp,
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
                        text = toolCall.name,
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
                        text = "${result.name} result",
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
    onReviewDiff: (FileDiff) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentWarning),
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
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = AccentWarning,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ACTION APPROVAL REQUIRED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AccentWarning
                )
            }

            Text(
                text = pending.promptMessage,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

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
                    Text("Deny", fontSize = 12.sp)
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Approve & Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getToolIcon(name: String): ImageVector {
    return when (name) {
        "run_command" -> Icons.Default.Terminal
        "search_files" -> Icons.Default.Search
        "list_dir" -> Icons.Default.Folder
        "write_file", "edit_file" -> Icons.Default.Difference
        else -> Icons.Default.Tune
    }
}
