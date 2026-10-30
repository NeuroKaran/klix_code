package com.klix.code

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Difference

import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.klix.code.data.stats.StreakManager
import com.klix.code.ui.components.agent.AgentChatPanel
import com.klix.code.ui.components.agent.AgentSettingsDialog
import com.klix.code.ui.components.sandbox.SandboxSetupDialog
import com.klix.code.ui.components.terminal.TerminalView
import com.klix.code.ui.terminal.TerminalViewModel
import com.klix.code.ui.viewmodel.AgentViewModel
import com.klix.code.widget.KlixWidgets
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.klix.code.ui.components.DiffViewerDialog
import com.klix.code.ui.components.EditorAccessoryBar
import com.klix.code.ui.components.EditorTabBar
import com.klix.code.ui.components.FileTreeView
import com.klix.code.ui.components.FindReplaceBar
import com.klix.code.ui.components.RecentProjectsDialog
import com.klix.code.ui.components.SoraEditorView
import com.klix.code.ui.components.rememberEditorController
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.KlixTheme
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import com.klix.code.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()
    private val agentViewModel: AgentViewModel by viewModels()
    private val terminalViewModel: TerminalViewModel by viewModels()

    private var pendingWidgetAction by mutableStateOf<String?>(null)
    private var pendingProjectToOpen by mutableStateOf<Pair<Uri, String>?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseWidgetIntent(intent)
    }

    private fun parseWidgetIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            KlixWidgets.ACTION_OPEN_PROJECT -> {
                val uriStr = intent.getStringExtra(KlixWidgets.EXTRA_PROJECT_URI)
                val name = intent.getStringExtra(KlixWidgets.EXTRA_PROJECT_NAME) ?: "Project"
                if (!uriStr.isNullOrEmpty() && uriStr != "null") {
                    val uri = Uri.parse(uriStr)
                    if (uri != Uri.EMPTY) {
                        pendingProjectToOpen = Pair(uri, name)
                    }
                }
            }
            KlixWidgets.ACTION_NEW_PROJECT,
            KlixWidgets.ACTION_OPEN_FOLDER,
            KlixWidgets.ACTION_SEE_ALL_PROJECTS,
            KlixWidgets.ACTION_ASK_AGENT -> {
                pendingWidgetAction = intent.action
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StreakManager.recordActivity(this)
        parseWidgetIntent(intent)

        setContent {
            KlixTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val currentProject by viewModel.currentProject.collectAsState()
                val recentProjects by viewModel.recentProjects.collectAsState()
                val fileTree by viewModel.fileTree.collectAsState()
                val openTabs by viewModel.openTabs.collectAsState()
                val activeTabUri by viewModel.activeTabUri.collectAsState()
                val isWordWrap by viewModel.isWordWrap.collectAsState()
                val fontSizeSp by viewModel.fontSizeSp.collectAsState()
                val isSearchOpen by viewModel.isSearchOpen.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val replaceQuery by viewModel.replaceQuery.collectAsState()
                val activeDiff by viewModel.activeDiff.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()
                val cursorPosition by viewModel.cursorPosition.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()

                var showRecentProjectsDialog by remember { mutableStateOf(false) }
                var isAgentPanelOpen by remember { mutableStateOf(false) }
                var isTerminalOpen by remember { mutableStateOf(false) }

                val agentMessages by agentViewModel.messages.collectAsState()
                val agentStatus by agentViewModel.status.collectAsState()
                val agentStatusMessage by agentViewModel.statusMessage.collectAsState()
                val agentPendingApproval by agentViewModel.pendingApproval.collectAsState()
                val agentPromptTokens by agentViewModel.totalPromptTokens.collectAsState()
                val agentCompletionTokens by agentViewModel.totalCompletionTokens.collectAsState()
                val agentSettings by agentViewModel.settings.collectAsState()
                val agentDiffToReview by agentViewModel.activeDiffToReview.collectAsState()
                val isAgentSettingsOpen by agentViewModel.isSettingsOpen.collectAsState()
                val showSandboxSetup by terminalViewModel.showSetupDialog.collectAsState()

                val editorController = rememberEditorController()

                // Sync workspace context with Agent
                LaunchedEffect(currentProject, activeTabUri, openTabs) {
                    val activeTab = openTabs.find { it.uri == activeTabUri }
                    agentViewModel.updateWorkspaceContext(
                        workspaceUri = currentProject?.uri,
                        workspaceName = currentProject?.name ?: "Workspace",
                        activeFileName = activeTab?.name,
                        activeFileContent = activeTab?.content
                    )
                }

                // Sync terminal file changes to GUI when terminal closes
                LaunchedEffect(isTerminalOpen) {
                    if (!isTerminalOpen) {
                        viewModel.refreshTree()
                    }
                }



                var showAgentPromptDialog by remember { mutableStateOf(false) }
                var agentPromptText by remember { mutableStateOf("") }

                // Handle Widget Actions
                LaunchedEffect(pendingWidgetAction) {
                    when (pendingWidgetAction) {
                        KlixWidgets.ACTION_NEW_PROJECT, KlixWidgets.ACTION_OPEN_FOLDER -> {
                            viewModel.openSandboxWorkspace()
                        }
                        KlixWidgets.ACTION_SEE_ALL_PROJECTS -> {
                            showRecentProjectsDialog = true
                        }
                        KlixWidgets.ACTION_ASK_AGENT -> {
                            isAgentPanelOpen = true
                        }
                    }
                    pendingWidgetAction = null
                }

                LaunchedEffect(pendingProjectToOpen) {
                    pendingProjectToOpen?.let { (uri, name) ->
                        viewModel.openProject(uri, name)
                        pendingProjectToOpen = null
                    }
                }

                val agentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val terminalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = drawerState.isOpen,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .fillMaxHeight(),
                            drawerContainerColor = DarkSurface
                        ) {
                            FileTreeView(
                                rootNode = fileTree,
                                activeFileUri = activeTabUri,
                                currentProjectName = currentProject?.name,
                                onOpenSandboxWorkspace = {
                                    viewModel.openSandboxWorkspace()
                                },
                                onFileClick = { fileNode ->
                                    viewModel.openFile(fileNode)
                                    scope.launch { drawerState.close() }
                                },
                                onCreateFile = { parentUri, name ->
                                    viewModel.createFile(parentUri, name)
                                },
                                onCreateDirectory = { parentUri, name ->
                                    viewModel.createDirectory(parentUri, name)
                                },
                                onDeleteNode = { node ->
                                    viewModel.deleteNode(node)
                                },
                                onRenameNode = { node, newName ->
                                    viewModel.renameNode(node, newName)
                                },
                                onRefresh = {
                                    viewModel.refreshTree()
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.klix_logo),
                                            contentDescription = "Klix Logo",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                        )
                                        Text(
                                            text = currentProject?.name ?: "KlixCode",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = TextPrimary
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu",
                                            tint = TextPrimary
                                        )
                                    }
                                },
                                actions = {
                                    // Save Button
                                    val activeTab = openTabs.find { it.uri == activeTabUri }
                                    IconButton(
                                        onClick = { viewModel.saveActiveTab() },
                                        enabled = activeTab?.isModified == true
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "Save",
                                            tint = if (activeTab?.isModified == true) AccentPrimary else TextMuted
                                        )
                                    }

                                    // Find & Replace toggle
                                    IconButton(onClick = { viewModel.toggleSearch() }) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = if (isSearchOpen) AccentPrimary else TextSecondary
                                        )
                                    }

                                    // Word Wrap toggle
                                    IconButton(onClick = { viewModel.toggleWordWrap() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.WrapText,
                                            contentDescription = "Toggle Word Wrap",
                                            tint = if (isWordWrap) AccentPrimary else TextSecondary
                                        )
                                    }

                                    // Klix AI Agent
                                    IconButton(onClick = { isAgentPanelOpen = !isAgentPanelOpen }) {
                                        Icon(
                                            imageVector = Icons.Default.SmartToy,
                                            contentDescription = "Klix AI Agent",
                                            tint = if (isAgentPanelOpen) AccentPrimary else TextSecondary
                                        )
                                    }

                                    // Terminal
                                    IconButton(onClick = { isTerminalOpen = !isTerminalOpen }) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = "Terminal",
                                            tint = if (isTerminalOpen) AccentSecondary else TextSecondary
                                        )
                                    }


                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = DarkSurface
                                )
                            )
                        },
                        bottomBar = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (openTabs.isNotEmpty() && activeTabUri != null) {
                                    EditorAccessoryBar(
                                        controller = editorController,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Bottom Status Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .background(DarkSurfaceVariant)
                                        .border(width = 0.5.dp, color = DarkBorder)
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = AccentPrimary
                                            )
                                        }
                                        Text(
                                            text = statusMessage ?: "Ready",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val activeTab = openTabs.find { it.uri == activeTabUri }
                                        if (activeTab != null) {
                                            Text(
                                                text = activeTab.extension.uppercase().ifEmpty { "TEXT" },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            Text(
                                                text = "Ln ${cursorPosition.first}, Col ${cursorPosition.second}",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // Zoom controls
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.zoomOut() },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomOut,
                                                    contentDescription = "Zoom Out",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Text(
                                                text = "${fontSizeSp.toInt()}pt",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            IconButton(
                                                onClick = { viewModel.zoomIn() },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomIn,
                                                    contentDescription = "Zoom In",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(DarkBg)
                        ) {
                            if (openTabs.isNotEmpty()) {
                                EditorTabBar(
                                    tabs = openTabs,
                                    activeTabUri = activeTabUri,
                                    onSelectTab = { uri -> viewModel.selectTab(uri) },
                                    onCloseTab = { uri -> viewModel.closeTab(uri) }
                                )

                                if (isSearchOpen) {
                                    FindReplaceBar(
                                        searchQuery = searchQuery,
                                        replaceQuery = replaceQuery,
                                        onSearchQueryChange = { q ->
                                            viewModel.setSearchQuery(q)
                                            editorController.search(q)
                                        },
                                        onReplaceQueryChange = { q ->
                                            viewModel.setReplaceQuery(q)
                                        },
                                        onFindNext = { editorController.findNext() },
                                        onFindPrevious = { editorController.findPrevious() },
                                        onReplaceCurrent = {
                                            editorController.replaceCurrent(replaceQuery)
                                        },
                                        onReplaceAll = {
                                            editorController.replaceAll(replaceQuery)
                                        },
                                        onClose = {
                                            editorController.stopSearch()
                                            viewModel.toggleSearch()
                                        }
                                    )
                                }

                                val activeTab = openTabs.find { it.uri == activeTabUri }
                                if (activeTab != null) {
                                    androidx.compose.runtime.key(activeTab.uri) {
                                        SoraEditorView(
                                            content = activeTab.content,
                                            extension = activeTab.extension,
                                            isWordWrap = isWordWrap,
                                            fontSizeSp = fontSizeSp,
                                            onContentChange = { newContent ->
                                                viewModel.updateActiveTabContent(newContent)
                                            },
                                            onCursorChange = { line, col ->
                                                viewModel.updateCursor(line, col)
                                            },
                                            controller = editorController,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            } else {
                                // Empty state when no tabs are open
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.klix_logo),
                                            contentDescription = "Klix Logo",
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                        )

                                        Text(
                                            text = if (currentProject != null) "Select a file from the sidebar to start editing" else "No Workspace Open",
                                            color = TextSecondary,
                                            fontSize = 14.sp
                                        )

                                        Button(
                                            onClick = { viewModel.openSandboxWorkspace() },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Terminal,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Open Sandbox Workspace")
                                        }

                                        if (recentProjects.isNotEmpty()) {
                                            Button(
                                                onClick = { showRecentProjectsDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                                            ) {
                                                Text("Recent Workspaces (${recentProjects.size})", color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Projects Dialog
                if (showRecentProjectsDialog) {
                    RecentProjectsDialog(
                        projects = recentProjects,
                        onSelectProject = { uri, name ->
                            viewModel.openProject(uri, name)
                            showRecentProjectsDialog = false
                        },
                        onDismiss = { showRecentProjectsDialog = false }
                    )
                }

                // Diff Viewer Dialog (for Agent proposed diff reviews)
                activeDiff?.let { diff ->
                    DiffViewerDialog(
                        diff = diff,
                        onApply = { viewModel.applyActiveDiff() },
                        onDiscard = { viewModel.discardActiveDiff() }
                    )
                }

                // Ask Agent Dialog (triggered from widget or in-app)
                if (showAgentPromptDialog) {
                    AlertDialog(
                        onDismissRequest = { showAgentPromptDialog = false },
                        containerColor = DarkBg,
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.klix_logo),
                                contentDescription = "Klix Robot",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        },
                        title = {
                            Text(
                                text = "Hey! What shall we build?",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Describe a feature, bugfix, or command for Klix AI Agent:",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                OutlinedTextField(
                                    value = agentPromptText,
                                    onValueChange = { agentPromptText = it },
                                    placeholder = { Text("e.g. Add unit tests for user authentication", color = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (agentPromptText.isNotBlank()) {
                                        val prompt = agentPromptText.trim()
                                        showAgentPromptDialog = false
                                        agentPromptText = ""
                                        isAgentPanelOpen = true
                                        agentViewModel.sendPrompt(prompt)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Text("Plan & Run")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAgentPromptDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                        }
                    )
                }

                // Agent Chat Panel (Modal Bottom Sheet)
                if (isAgentPanelOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { isAgentPanelOpen = false },
                        sheetState = agentSheetState,
                        containerColor = DarkBg,
                        dragHandle = null,
                        modifier = Modifier.fillMaxHeight(0.92f)
                    ) {
                        AgentChatPanel(
                            messages = agentMessages,
                            status = agentStatus,
                            statusMessage = agentStatusMessage,
                            pendingApproval = agentPendingApproval,
                            settings = agentSettings,
                            totalTokens = agentPromptTokens + agentCompletionTokens,
                            onSendPrompt = { prompt -> agentViewModel.sendPrompt(prompt) },
                            onApproveTool = {
                                agentViewModel.approvePendingTool()
                                viewModel.refreshTree()
                            },
                            onRejectTool = { agentViewModel.rejectPendingTool() },
                            onCancelTask = { agentViewModel.cancelTask() },
                            onClearChat = { agentViewModel.clearChat() },
                            onOpenSettings = { agentViewModel.toggleSettings() },
                            onReviewDiff = { diff -> agentViewModel.openReviewDiff(diff) },
                            onClose = { isAgentPanelOpen = false }
                        )
                    }
                }

                // Agent Diff Review Dialog
                agentDiffToReview?.let { diff ->
                    DiffViewerDialog(
                        diff = diff,
                        onApply = {
                            agentViewModel.approvePendingTool()
                            agentViewModel.closeReviewDiff()
                            viewModel.refreshTree()
                            diff.fileUri?.let { uri ->
                                val tab = openTabs.find { it.uri == uri }
                                if (tab != null) {
                                    viewModel.updateActiveTabContent(diff.modifiedText)
                                }
                            }
                        },
                        onDiscard = {
                            agentViewModel.closeReviewDiff()
                        }
                    )
                }

                // Agent Settings Dialog
                if (isAgentSettingsOpen) {
                    AgentSettingsDialog(
                        currentSettings = agentSettings,
                        onGetApiKey = { provider -> agentViewModel.getApiKey(provider) },
                        onSaveApiKey = { provider, key -> agentViewModel.saveApiKey(provider, key) },
                        onSaveSettings = { updated -> agentViewModel.updateSettings(updated) },
                        onDismiss = { agentViewModel.toggleSettings() }
                    )
                }

                // Terminal Bottom Sheet
                if (isTerminalOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { isTerminalOpen = false },
                        sheetState = terminalSheetState,
                        containerColor = DarkBg,
                        dragHandle = null,
                        modifier = Modifier.fillMaxHeight(0.92f)
                    ) {
                        TerminalView(
                            viewModel = terminalViewModel,
                            onClose = { isTerminalOpen = false }
                        )
                    }
                }

                // Sandbox Setup Dialog
                if (showSandboxSetup) {
                    SandboxSetupDialog(
                        sandboxManager = terminalViewModel.sandboxManager,
                        onRunCommandInTerminal = { cmd ->
                            isTerminalOpen = true
                            terminalViewModel.executeCommand(cmd)
                        },
                        onDismiss = { terminalViewModel.closeSetupDialog() }
                    )
                }
            }
        }
    }
}
