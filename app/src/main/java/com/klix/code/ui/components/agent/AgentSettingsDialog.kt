package com.klix.code.ui.components.agent

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.agent.model.DiscoveredModel
import com.klix.code.data.agent.model.ModelDiscoveryRepository
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.components.KlixPeekingMascotScaffold
import com.klix.code.ui.components.KlixPrimaryPillButton
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.components.sandbox.ActiveDownloadBentoCard
import com.klix.code.ui.components.sandbox.SandboxSetupUiHelper
import com.klix.code.ui.components.sandbox.ToolchainCard
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixSurfaceMacroTint
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.ThemeManager
import com.klix.code.ui.theme.ThemeMode
import kotlinx.coroutines.launch

enum class OpenRouterModelFilter {
    ALL,
    FREE,
    PAID
}

private enum class SettingsDestination {
    HUB,
    AI_MODELS,
    LOCAL_SANDBOX
}

/**
 * Settings Modal / Screen with Bento Hub & Dedicated Expandable Panels:
 * - Hub: AI & Models, Local Sandbox, Appearance (with floating theme popover), Onboarding Flow
 * - AI & Models Panel: 2x2 Provider Grid, masked API key bar + inline editor, Model search & list
 * - Local Sandbox Panel: Alpine Linux status card, 2x2 Toolchain Grid, Custom apk bar, Download All
 */
@Composable
fun AgentSettingsDialog(
    currentSettings: ProviderSettings,
    availableModels: List<DiscoveredModel> = emptyList(),
    isFetchingModels: Boolean = false,
    onGetApiKey: (ModelProvider) -> String?,
    onSaveApiKey: (ModelProvider, String?) -> Unit,
    onSaveSettings: (ProviderSettings) -> Unit,
    onRefreshModels: (ModelProvider, String?, Boolean) -> Unit = { _, _, _ -> },
    sandboxManager: SandboxManager? = null,
    onRunCommandInTerminal: ((String) -> Unit)? = null,
    onReplayOnboarding: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val currentThemeMode by ThemeManager.themeMode.collectAsState()

    var destination by remember { mutableStateOf(SettingsDestination.HUB) }
    var isAppearanceMenuExpanded by remember { mutableStateOf(false) }

    var selectedProvider by remember { mutableStateOf(currentSettings.activeProvider) }
    var selectedModel by remember { mutableStateOf(currentSettings.activeModel) }
    var isEditingApiKey by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var keyUpdateTrigger by remember { mutableStateOf(0) }

    var modelSearchQuery by remember { mutableStateOf("") }
    var openRouterFilter by remember { mutableStateOf(OpenRouterModelFilter.ALL) }

    val modelsForProvider = remember(availableModels, selectedProvider) {
        val matching = availableModels.filter { it.provider == selectedProvider }
        if (matching.isNotEmpty()) matching else ModelDiscoveryRepository().getDefaultModels(selectedProvider)
    }

    LaunchedEffect(selectedProvider) {
        onRefreshModels(selectedProvider, onGetApiKey(selectedProvider), false)
    }

    LaunchedEffect(selectedProvider, modelsForProvider) {
        if (modelsForProvider.none { it.id == selectedModel }) {
            selectedModel = modelsForProvider.firstOrNull()?.id ?: selectedProvider.defaultModel
        }
    }

    val filteredModels = remember(modelsForProvider, openRouterFilter, modelSearchQuery, selectedProvider) {
        modelsForProvider.filter { model ->
            val matchesFilter = when {
                selectedProvider != ModelProvider.CUSTOM_OPENAI_COMPATIBLE -> true
                openRouterFilter == OpenRouterModelFilter.ALL -> true
                openRouterFilter == OpenRouterModelFilter.FREE -> model.isFree
                openRouterFilter == OpenRouterModelFilter.PAID -> !model.isFree
                else -> true
            }
            val matchesSearch = if (modelSearchQuery.isBlank()) true else {
                model.displayName.contains(modelSearchQuery, ignoreCase = true) ||
                    model.id.contains(modelSearchQuery, ignoreCase = true) ||
                    (model.description?.contains(modelSearchQuery, ignoreCase = true) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    val saveCurrentSettings = {
        val updated = currentSettings.copy(
            activeProvider = selectedProvider,
            activeModel = selectedModel
        )
        onSaveSettings(updated)
    }

    Dialog(
        onDismissRequest = {
            if (destination != SettingsDestination.HUB) {
                destination = SettingsDestination.HUB
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        BackHandler(enabled = destination != SettingsDestination.HUB) {
            destination = SettingsDestination.HUB
        }

        KlixPeekingMascotScaffold(
            title = "Settings"
        ) {
            AnimatedContent(
                targetState = destination,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    val forward = targetState != SettingsDestination.HUB
                    if (forward) {
                        (slideInHorizontally(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            initialOffsetX = { it / 3 }
                        ) + fadeIn(tween(220))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(220, easing = FastOutLinearInEasing),
                                targetOffsetX = { -it / 4 }
                            ) + fadeOut(tween(180))
                        )
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            initialOffsetX = { -it / 3 }
                        ) + fadeIn(tween(220))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(220, easing = FastOutLinearInEasing),
                                targetOffsetX = { it / 4 }
                            ) + fadeOut(tween(180))
                        )
                    }
                },
                label = "SettingsDestinationTransition"
            ) { currentDest ->
                when (currentDest) {
                    SettingsDestination.HUB -> {
                        val activeHasKey = keyUpdateTrigger.let {
                            !onGetApiKey(selectedProvider).isNullOrBlank()
                        }
                        val sandboxStatus = sandboxManager?.status?.collectAsState()?.value

                        SettingsHubPanel(
                            selectedProvider = selectedProvider,
                            selectedModel = selectedModel,
                            activeProviderHasKey = activeHasKey,
                            sandboxStatus = sandboxStatus,
                            hasSandbox = sandboxManager != null && onRunCommandInTerminal != null,
                            currentThemeMode = currentThemeMode,
                            isAppearanceMenuExpanded = isAppearanceMenuExpanded,
                            onAppearanceMenuExpandedChange = { isAppearanceMenuExpanded = it },
                            onSelectThemeMode = { mode ->
                                ThemeManager.setThemeMode(context, mode)
                                isAppearanceMenuExpanded = false
                            },
                            onOpenAiModels = {
                                isEditingApiKey = false
                                destination = SettingsDestination.AI_MODELS
                            },
                            onOpenLocalSandbox = {
                                destination = SettingsDestination.LOCAL_SANDBOX
                            },
                            onReplayOnboarding = onReplayOnboarding?.let { replay ->
                                {
                                    onDismiss()
                                    replay()
                                }
                            },
                            onApplyAndClose = {
                                saveCurrentSettings()
                                onDismiss()
                            },
                            isDark = isDark
                        )
                    }

                    SettingsDestination.AI_MODELS -> {
                        val currentApiKey = keyUpdateTrigger.let { onGetApiKey(selectedProvider) }
                        val hasKey = !currentApiKey.isNullOrBlank()

                        AiModelsSettingsPanel(
                            selectedProvider = selectedProvider,
                            selectedModel = selectedModel,
                            currentApiKey = currentApiKey,
                            hasKey = hasKey,
                            isEditingApiKey = isEditingApiKey,
                            apiKeyInput = apiKeyInput,
                            isKeyVisible = isKeyVisible,
                            isFetchingModels = isFetchingModels,
                            modelsForProvider = modelsForProvider,
                            filteredModels = filteredModels,
                            modelSearchQuery = modelSearchQuery,
                            openRouterFilter = openRouterFilter,
                            onBack = { destination = SettingsDestination.HUB },
                            onSelectProvider = { provider ->
                                selectedProvider = provider
                                isEditingApiKey = false
                                apiKeyInput = onGetApiKey(provider) ?: ""
                                onRefreshModels(provider, onGetApiKey(provider), false)
                            },
                            onToggleEditApiKey = {
                                if (!isEditingApiKey) {
                                    apiKeyInput = onGetApiKey(selectedProvider) ?: ""
                                }
                                isEditingApiKey = !isEditingApiKey
                            },
                            onApiKeyInputChange = { apiKeyInput = it },
                            onToggleKeyVisibility = { isKeyVisible = !isKeyVisible },
                            onSaveApiKey = {
                                val trimmedKey = apiKeyInput.trim().ifEmpty { null }
                                onSaveApiKey(selectedProvider, trimmedKey)
                                onRefreshModels(selectedProvider, trimmedKey, true)
                                keyUpdateTrigger++
                                isEditingApiKey = false
                            },
                            onRefreshModels = {
                                onRefreshModels(selectedProvider, onGetApiKey(selectedProvider), true)
                            },
                            onSearchQueryChange = { modelSearchQuery = it },
                            onOpenRouterFilterChange = { openRouterFilter = it },
                            onSelectModel = { selectedModel = it },
                            onSaveAndBack = {
                                saveCurrentSettings()
                                destination = SettingsDestination.HUB
                            }
                        )
                    }

                    SettingsDestination.LOCAL_SANDBOX -> {
                        if (sandboxManager != null && onRunCommandInTerminal != null) {
                            LocalSandboxSettingsPanel(
                                sandboxManager = sandboxManager,
                                onRunCommandInTerminal = onRunCommandInTerminal,
                                onBack = { destination = SettingsDestination.HUB }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHubPanel(
    selectedProvider: ModelProvider,
    selectedModel: String,
    activeProviderHasKey: Boolean,
    sandboxStatus: SandboxStatus?,
    hasSandbox: Boolean,
    currentThemeMode: ThemeMode,
    isAppearanceMenuExpanded: Boolean,
    onAppearanceMenuExpandedChange: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onOpenAiModels: () -> Unit,
    onOpenLocalSandbox: () -> Unit,
    onReplayOnboarding: (() -> Unit)?,
    onApplyAndClose: () -> Unit,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tile 1: AI & Models
            val aiStatusLabel = AgentSettingsUiHelper.getProviderStatusLabel(
                hasKey = activeProviderHasKey,
                isSelected = true
            )
            SettingsBentoTile(
                title = "AI & Models",
                subtitle = "${selectedProvider.shortDisplayName()} • ${selectedModel.substringAfterLast('/')}",
                badgeText = aiStatusLabel,
                badgeIsPositive = activeProviderHasKey,
                iconContainerColor = Color(0xFFD5FB66),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF0C0D14),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onOpenAiModels
            )

            // Tile 2: Local Sandbox
            if (hasSandbox) {
                val isSandboxReady = sandboxStatus == SandboxStatus.READY
                SettingsBentoTile(
                    title = "Local Sandbox",
                    subtitle = "Python 3 built-in • Node & C++ on-demand",
                    badgeText = if (isSandboxReady) "Active" else "Setup",
                    badgeIsPositive = isSandboxReady,
                    badgePositiveBg = if (isDark) Color(0xFF16302B) else Color(0xFFC6F6D5),
                    badgePositiveText = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                    iconContainerColor = if (isDark) Color(0xFF16302B) else Color(0xFFC6F6D5),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_icon_terminal_prompt),
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF86EFAC) else Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = onOpenLocalSandbox
                )
            }

            // Tile 3: Appearance + Floating Popover
            Box(modifier = Modifier.fillMaxWidth()) {
                val themeSubtitle = when (currentThemeMode) {
                    ThemeMode.LIGHT -> "Light Mode"
                    ThemeMode.DARK -> "Dark Mode"
                    ThemeMode.SYSTEM -> "System"
                }
                SettingsBentoTile(
                    title = "Appearance",
                    subtitle = themeSubtitle,
                    iconContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFDBEAFE),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Contrast,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { onAppearanceMenuExpandedChange(true) }
                )

                DropdownMenu(
                    expanded = isAppearanceMenuExpanded,
                    onDismissRequest = { onAppearanceMenuExpandedChange(false) },
                    offset = DpOffset(x = 140.dp, y = (-12).dp),
                    shape = RoundedCornerShape(22.dp),
                    containerColor = KlixSurfaceCardItem,
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .width(190.dp)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppearanceMenuRow(
                            label = "Light",
                            icon = Icons.Default.LightMode,
                            isSelected = currentThemeMode == ThemeMode.LIGHT,
                            onClick = { onSelectThemeMode(ThemeMode.LIGHT) }
                        )
                        AppearanceMenuRow(
                            label = "Dark",
                            icon = Icons.Default.DarkMode,
                            isSelected = currentThemeMode == ThemeMode.DARK,
                            onClick = { onSelectThemeMode(ThemeMode.DARK) }
                        )
                        AppearanceMenuRow(
                            label = "System",
                            icon = Icons.Default.DesktopWindows,
                            isSelected = currentThemeMode == ThemeMode.SYSTEM,
                            onClick = { onSelectThemeMode(ThemeMode.SYSTEM) }
                        )
                    }
                }
            }

            // Tile 4: Onboarding Flow
            if (onReplayOnboarding != null) {
                SettingsBentoTile(
                    title = "Onboarding Flow",
                    subtitle = "Replay walkthrough",
                    containerColor = KlixSurfaceBentoGroup,
                    iconContainerColor = Color.Transparent,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_icon_play),
                            contentDescription = null,
                            tint = KlixInkSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = onReplayOnboarding
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        KlixPrimaryPillButton(
            text = "Apply & Close",
            onClick = onApplyAndClose,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsBentoTile(
    title: String,
    subtitle: String,
    iconContainerColor: Color,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeIsPositive: Boolean = true,
    badgePositiveBg: Color? = null,
    badgePositiveText: Color? = null,
    containerColor: Color = KlixSurfaceCardItem
) {
    val isDark = LocalIsDarkTheme.current
    val resolvedBadgeBg = if (badgeIsPositive) {
        badgePositiveBg ?: if (isDark) Color(0xFF2A3626) else Color(0xFFE2F9C8)
    } else {
        if (isDark) Color(0xFF3A2E14) else Color(0xFFFEF3C7)
    }
    val resolvedBadgeText = if (badgeIsPositive) {
        badgePositiveText ?: if (isDark) Color(0xFFD5FB66) else Color(0xFF3F6212)
    } else {
        if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.97f)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            leadingIcon()
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = KlixTypography.cardTitle,
                    fontWeight = FontWeight.Bold,
                    color = KlixInkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(resolvedBadgeBg)
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = KlixTypography.codePill,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = resolvedBadgeText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = KlixTypography.bodySecondary,
                color = KlixInkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = KlixInkSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AppearanceMenuRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFFD5FB66) else Color.Transparent
    val contentCol = if (isSelected) Color(0xFF0C0D14) else KlixInkPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .pressEffect(0.95f)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentCol,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = KlixTypography.bodyPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentCol
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF0C0D14),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SubPanelHeader(
    title: String,
    badgeText: String,
    badgeIsPositive: Boolean,
    onBack: () -> Unit,
    badgePositiveBg: Color? = null,
    badgePositiveText: Color? = null
) {
    val isDark = LocalIsDarkTheme.current
    val resolvedBadgeBg = if (badgeIsPositive) {
        badgePositiveBg ?: if (isDark) Color(0xFF2A3626) else Color(0xFFE2F9C8)
    } else {
        if (isDark) Color(0xFF3A2E14) else Color(0xFFFEF3C7)
    }
    val resolvedBadgeText = if (badgeIsPositive) {
        badgePositiveText ?: if (isDark) Color(0xFFD5FB66) else Color(0xFF3F6212)
    } else {
        if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KlixSurfaceBentoGroup)
                    .border(1.dp, KlixBorderHairline, CircleShape)
                    .clickable(onClick = onBack)
                    .pressEffect(0.92f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KlixInkPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                style = KlixTypography.sheetHeader,
                fontWeight = FontWeight.Bold,
                color = KlixInkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(resolvedBadgeBg)
                .padding(horizontal = 11.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                style = KlixTypography.codePill,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = resolvedBadgeText,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text.uppercase(),
            style = KlixTypography.codePill,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = KlixInkSecondary
        )
        trailing?.invoke()
    }
}

@Composable
private fun AiModelsSettingsPanel(
    selectedProvider: ModelProvider,
    selectedModel: String,
    currentApiKey: String?,
    hasKey: Boolean,
    isEditingApiKey: Boolean,
    apiKeyInput: String,
    isKeyVisible: Boolean,
    isFetchingModels: Boolean,
    modelsForProvider: List<DiscoveredModel>,
    filteredModels: List<DiscoveredModel>,
    modelSearchQuery: String,
    openRouterFilter: OpenRouterModelFilter,
    onBack: () -> Unit,
    onSelectProvider: (ModelProvider) -> Unit,
    onToggleEditApiKey: () -> Unit,
    onApiKeyInputChange: (String) -> Unit,
    onToggleKeyVisibility: () -> Unit,
    onSaveApiKey: () -> Unit,
    onRefreshModels: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenRouterFilterChange: (OpenRouterModelFilter) -> Unit,
    onSelectModel: (String) -> Unit,
    onSaveAndBack: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val statusLabel = AgentSettingsUiHelper.getProviderStatusLabel(
        hasKey = hasKey,
        isSelected = true
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SubPanelHeader(
            title = "AI & Models",
            badgeText = statusLabel,
            badgeIsPositive = hasKey,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Provider 2x2 Bento Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "PROVIDER")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderGridPill(
                        name = "Google Gemini",
                        logoRes = R.drawable.ic_logo_gemini,
                        isSelected = selectedProvider == ModelProvider.GEMINI,
                        onClick = { onSelectProvider(ModelProvider.GEMINI) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderGridPill(
                        name = "Claude",
                        logoRes = R.drawable.ic_logo_claude,
                        isSelected = selectedProvider == ModelProvider.ANTHROPIC,
                        onClick = { onSelectProvider(ModelProvider.ANTHROPIC) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderGridPill(
                        name = "OpenAI",
                        logoRes = R.drawable.ic_logo_openai,
                        isSelected = selectedProvider == ModelProvider.OPENAI,
                        onClick = { onSelectProvider(ModelProvider.OPENAI) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderGridPill(
                        name = "OpenRouter",
                        logoRes = R.drawable.ic_logo_openrouter,
                        isSelected = selectedProvider == ModelProvider.CUSTOM_OPENAI_COMPATIBLE,
                        onClick = { onSelectProvider(ModelProvider.CUSTOM_OPENAI_COMPATIBLE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // API Key Compact Masked Row + Inline Editor
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "API KEY")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(KlixSurfaceCardItem)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                        .clickable(onClick = onToggleEditApiKey)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = KlixInkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = maskApiKey(currentApiKey),
                            style = KlixTypography.bodySecondary,
                            fontWeight = FontWeight.Medium,
                            color = if (hasKey) KlixInkPrimary else KlixInkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(KlixSurfaceBentoGroup)
                            .clickable(onClick = onToggleEditApiKey)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEditingApiKey) "Close" else if (hasKey) "Edit" else "Add",
                            style = KlixTypography.codePill,
                            fontWeight = FontWeight.Bold,
                            color = KlixInkPrimary
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isEditingApiKey,
                    enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(240, easing = FastOutSlowInEasing), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(150, easing = FastOutLinearInEasing)) +
                        shrinkVertically(tween(180, easing = FastOutLinearInEasing), shrinkTowards = Alignment.Top)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(KlixSurfaceCardItem)
                            .border(1.dp, KlixBorderHairline, RoundedCornerShape(18.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "API Key for ${selectedProvider.shortDisplayName()}",
                                style = KlixTypography.bodySecondary,
                                fontWeight = FontWeight.Bold,
                                color = KlixInkPrimary
                            )
                            IconButton(
                                onClick = onToggleEditApiKey,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = KlixInkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = onApiKeyInputChange,
                            placeholder = { Text("Enter API key...", style = KlixTypography.bodySecondary) },
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = onToggleKeyVisibility) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = KlixInkSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD5FB66),
                                unfocusedBorderColor = KlixBorderHairline
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFD5FB66))
                                    .clickable(onClick = onSaveApiKey)
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Save Key",
                                    style = KlixTypography.bodySecondary,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0C0D14)
                                )
                            }
                        }
                    }
                }
            }

            // Models Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(
                    text = "MODEL (${filteredModels.size})",
                    trailing = {
                        IconButton(
                            onClick = onRefreshModels,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFD5FB66)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Models",
                                    tint = KlixInkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                // OpenRouter Free/Paid Tabs
                if (selectedProvider == ModelProvider.CUSTOM_OPENAI_COMPATIBLE) {
                    val freeCount = modelsForProvider.count { it.isFree }
                    val paidCount = modelsForProvider.count { !it.isFree }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModelCategoryTab(
                            label = "All (${modelsForProvider.size})",
                            isSelected = openRouterFilter == OpenRouterModelFilter.ALL,
                            onClick = { onOpenRouterFilterChange(OpenRouterModelFilter.ALL) },
                            modifier = Modifier.weight(1f)
                        )
                        ModelCategoryTab(
                            label = "Free ($freeCount)",
                            isFreeBadge = true,
                            isSelected = openRouterFilter == OpenRouterModelFilter.FREE,
                            onClick = { onOpenRouterFilterChange(OpenRouterModelFilter.FREE) },
                            modifier = Modifier.weight(1f)
                        )
                        ModelCategoryTab(
                            label = "Paid ($paidCount)",
                            isSelected = openRouterFilter == OpenRouterModelFilter.PAID,
                            onClick = { onOpenRouterFilterChange(OpenRouterModelFilter.PAID) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (modelsForProvider.size > 3) {
                    OutlinedTextField(
                        value = modelSearchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text("Filter models...", style = KlixTypography.bodySecondary, color = KlixInkSecondary)
                        },
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = KlixInkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        textStyle = KlixTypography.bodySecondary.copy(color = KlixInkPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = KlixSurfaceCardItem,
                            unfocusedContainerColor = KlixSurfaceCardItem,
                            focusedBorderColor = Color(0xFFD5FB66),
                            unfocusedBorderColor = KlixBorderHairline
                        )
                    )
                }

                // Models List Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(KlixSurfaceCardItem)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                        .padding(6.dp)
                ) {
                    if (filteredModels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFetchingModels) "Discovering models..." else "No matching models found.",
                                style = KlixTypography.bodySecondary,
                                color = KlixInkSecondary
                            )
                        }
                    } else {
                        val modelRowMinHeight = rememberModelRowMinHeight()
                        val maxVisibleModelsHeight = modelRowMinHeight * MAX_VISIBLE_MODEL_ROWS +
                            6.dp * (MAX_VISIBLE_MODEL_ROWS - 1) + 4.dp
                        val modelsListState = rememberLazyListState()

                        LaunchedEffect(selectedProvider, openRouterFilter, modelSearchQuery) {
                            val selectedIndex = filteredModels.indexOfFirst { it.id == selectedModel }
                            if (selectedIndex >= MAX_VISIBLE_MODEL_ROWS && modelSearchQuery.isBlank()) {
                                modelsListState.scrollToItem(selectedIndex)
                            } else {
                                modelsListState.scrollToItem(0)
                            }
                        }

                        LazyColumn(
                            state = modelsListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxVisibleModelsHeight),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(
                                items = filteredModels,
                                key = { index, model -> "${model.provider.name}_${model.id}_$index" }
                            ) { index, model ->
                                ModelItemRow(
                                    model = model,
                                    isSelected = model.id == selectedModel,
                                    minHeight = modelRowMinHeight,
                                    onClick = { onSelectModel(model.id) }
                                )
                                if (index < filteredModels.lastIndex && model.id != selectedModel) {
                                    HorizontalDivider(
                                        color = KlixBorderHairline.copy(alpha = 0.45f),
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        KlixPrimaryPillButton(
            text = "Save & Back",
            onClick = onSaveAndBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProviderGridPill(
    name: String,
    logoRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Color(0xFFD5FB66) else KlixSurfaceCardItem
    val textCol = if (isSelected) Color(0xFF0C0D14) else KlixInkPrimary
    val borderCol = if (isSelected) Color(0xFF84CC16) else KlixBorderHairline

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderCol, CircleShape)
            .clickable(onClick = onClick)
            .pressEffect(0.95f)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            style = KlixTypography.bodySecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textCol,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LocalSandboxSettingsPanel(
    sandboxManager: SandboxManager,
    onRunCommandInTerminal: (String) -> Unit,
    onBack: () -> Unit
) {
    val status by sandboxManager.status.collectAsState()
    val statusMsg by sandboxManager.statusMessage.collectAsState()
    val progress by sandboxManager.progress.collectAsState()
    val installedTools by sandboxManager.installedTools.collectAsState()
    val installingTools by sandboxManager.installingTools.collectAsState()
    val isInstallingAll by sandboxManager.isInstallingAll.collectAsState()
    val installAllProgress by sandboxManager.installAllProgress.collectAsState()
    val installAllMessage by sandboxManager.installAllMessage.collectAsState()
    val activeDownload by sandboxManager.activeDownload.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(status) {
        sandboxManager.refreshInstalledTools()
    }

    val isReady = status == SandboxStatus.READY
    val isNodeInstalled = installedTools["nodejs"] == true
    val isPythonInstalled = installedTools["python3"] == true
    val isDashInstalled = installedTools["dash"] == true
    val isCppInstalled = installedTools["c/c++"] == true
    val allToolsInstalled = isNodeInstalled && isPythonInstalled && isDashInstalled && isCppInstalled
    val everythingReady = isReady && allToolsInstalled
    val isRootFsBusy = status == SandboxStatus.DOWNLOADING || status == SandboxStatus.EXTRACTING
    val isInstalling = isInstallingAll || isRootFsBusy || activeDownload != null

    fun handleToolClick(toolId: String, verifyCmd: String, isInstalled: Boolean) {
        if (isInstallingAll) {
            Toast.makeText(context, "Download All is currently in progress", Toast.LENGTH_SHORT).show()
            return
        }
        if (sandboxManager.isOnDemandToolchain(toolId)) {
            sandboxManager.launchInstallToolchain(toolId) { result ->
                result.onSuccess {
                    val label = sandboxManager.getOnDemandSpec(toolId)?.displayName ?: toolId
                    Toast.makeText(context, if (isInstalled) "$label updated!" else "$label ready!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Download failed", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        if (!isReady) {
            scope.launch { sandboxManager.installSandbox() }
            return
        }
        onRunCommandInTerminal(verifyCmd)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubPanelHeader(
            title = "Local Sandbox",
            badgeText = if (isReady) "Active" else "Setup",
            badgeIsPositive = isReady,
            badgePositiveBg = if (isDark) Color(0xFF16302B) else Color(0xFFC6F6D5),
            badgePositiveText = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(KlixSurfaceMacroTint)
                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFD5FB66)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_icon_terminal_prompt),
                                contentDescription = null,
                                tint = Color(0xFF0C0D14),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isReady) "Python 3 Runtime Active" else "Python 3 Runtime Setup",
                                style = KlixTypography.cardTitle,
                                fontWeight = FontWeight.Bold,
                                color = KlixInkPrimary
                            )
                            Text(
                                text = "Python 3 built-in • Node & C++ on-demand",
                                style = KlixTypography.bodySecondary,
                                color = KlixInkSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val actionLabel = SandboxSetupUiHelper.getActionLabel(status)
                            val actionIcon = SandboxSetupUiHelper.getActionIcon(status)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(KlixSurfaceCardItem)
                                    .border(1.dp, KlixBorderHairline, CircleShape)
                                    .clickable(enabled = !isInstalling) {
                                        scope.launch { sandboxManager.installSandbox() }
                                    }
                                    .pressEffect(if (isInstalling) 1f else 0.94f)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = actionIcon,
                                        contentDescription = actionLabel,
                                        tint = if (isInstalling) KlixInkSecondary else KlixInkPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = actionLabel,
                                        style = KlixTypography.codePill,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isInstalling) KlixInkSecondary else KlixInkPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (activeDownload != null && !isInstallingAll) {
                        ActiveDownloadBentoCard(
                            downloadState = activeDownload,
                            visible = true,
                            onCancel = { sandboxManager.cancelActiveDownload() },
                            onDismiss = { sandboxManager.dismissDownloadBanner() }
                        )
                    } else if (isInstallingAll || isRootFsBusy) {
                        val activeProgress = if (isInstallingAll) installAllProgress else progress
                        val activeMessage = if (isInstallingAll) {
                            installAllMessage ?: "Downloading all components..."
                        } else {
                            statusMsg
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { activeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFD5FB66),
                                trackColor = if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "$activeMessage (${(activeProgress * 100).toInt()}%)",
                                style = KlixTypography.bodySecondary,
                                color = KlixInkSecondary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            // Runtimes & On-Demand Toolchains 2x2 Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "RUNTIMES & ON-DEMAND TOOLCHAINS")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolchainCard(
                        name = "Python 3",
                        logoRes = R.drawable.ic_logo_python,
                        isInstalled = isPythonInstalled,
                        isInstalling = installingTools.contains("python3"),
                        subtitle = "Built-in • Bundled",
                        onClick = {
                            handleToolClick(
                                toolId = "python3",
                                verifyCmd = "python3 --version",
                                isInstalled = isPythonInstalled
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ToolchainCard(
                        name = "Dash Shell",
                        logoRes = R.drawable.ic_icon_terminal_prompt,
                        isInstalled = isDashInstalled,
                        isInstalling = installingTools.contains("dash"),
                        subtitle = "Built-in • POSIX sh",
                        onClick = {
                            handleToolClick(
                                toolId = "dash",
                                verifyCmd = "echo \$0 && pwd",
                                isInstalled = isDashInstalled
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolchainCard(
                        name = "Node.js",
                        logoRes = R.drawable.ic_logo_nodejs,
                        isInstalled = isNodeInstalled,
                        isInstalling = installingTools.contains("nodejs"),
                        subtitle = if (isNodeInstalled) "On-Demand • Ready" else "On-Demand • 30.5 MB",
                        onClick = {
                            handleToolClick(
                                toolId = "nodejs",
                                verifyCmd = "node --version",
                                isInstalled = isNodeInstalled
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ToolchainCard(
                        name = "C/C++",
                        logoRes = R.drawable.ic_logo_cpp,
                        isInstalled = isCppInstalled,
                        isInstalling = installingTools.contains("c/c++"),
                        subtitle = if (isCppInstalled) "On-Demand • Ready" else "On-Demand • 71.2 MB",
                        onClick = {
                            handleToolClick(
                                toolId = "c/c++",
                                verifyCmd = "clang --version",
                                isInstalled = isCppInstalled
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Runtime Diagnostics Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "RUNTIME DIAGNOSTICS")

                val commandText = buildList {
                    add("python3 --version")
                    if (isNodeInstalled) add("node --version")
                    if (isCppInstalled) add("clang --version")
                }.joinToString(" && ")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(KlixSurfaceCardItem)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KlixInkSecondary
                        )
                        Text(
                            text = commandText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KlixInkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(KlixSurfaceBentoGroup)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(commandText))
                                    Toast.makeText(context, "Copied: $commandText", Toast.LENGTH_SHORT).show()
                                }
                                .pressEffect(0.92f)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = KlixInkPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Copy",
                                    style = KlixTypography.codePill,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KlixInkPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(KlixSurfaceBentoGroup)
                                .clickable {
                                    if (!isReady) {
                                        Toast.makeText(context, "Please set up Python 3 RootFS first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onRunCommandInTerminal(commandText)
                                    }
                                }
                                .pressEffect(0.92f)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_icon_terminal_prompt),
                                    contentDescription = "Run",
                                    tint = KlixInkPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Run",
                                    style = KlixTypography.codePill,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KlixInkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom CTAs: Download All + Save & Back
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val ctaContainerColor = when {
                everythingReady -> if (isDark) Color(0xFF16302B) else Color(0xFFDCFCE7)
                isInstalling -> Color(0xFFD5FB66).copy(alpha = 0.55f)
                else -> Color(0xFFD5FB66)
            }
            val ctaContentColor = when {
                everythingReady -> if (isDark) Color(0xFFD5FB66) else Color(0xFF15803D)
                else -> Color(0xFF0C0D14)
            }

            Button(
                onClick = {
                    if (everythingReady) {
                        Toast.makeText(context, "RootFS and all toolchains are already installed", Toast.LENGTH_SHORT).show()
                    } else {
                        sandboxManager.launchInstallAll { result ->
                            result.onSuccess {
                                Toast.makeText(context, "All sandbox components installed!", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Download All failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isInstalling,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ctaContainerColor,
                    contentColor = ctaContentColor,
                    disabledContainerColor = ctaContainerColor,
                    disabledContentColor = ctaContentColor.copy(alpha = 0.85f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .pressEffect(if (isInstalling) 1f else 0.96f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    when {
                        isInstalling -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ctaContentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Downloading Everything...",
                                style = KlixTypography.bodyPrimary,
                                fontWeight = FontWeight.Bold,
                                color = ctaContentColor
                            )
                        }
                        everythingReady -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ctaContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Everything Installed",
                                style = KlixTypography.bodyPrimary,
                                fontWeight = FontWeight.Bold,
                                color = ctaContentColor
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = ctaContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download All",
                                style = KlixTypography.bodyPrimary,
                                fontWeight = FontWeight.Bold,
                                color = ctaContentColor
                            )
                        }
                    }
                }
            }

            KlixPrimaryPillButton(
                text = "Save & Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun ModelProvider.shortDisplayName(): String = when (this) {
    ModelProvider.GEMINI -> "Google Gemini"
    ModelProvider.ANTHROPIC -> "Claude"
    ModelProvider.OPENAI -> "OpenAI"
    ModelProvider.CUSTOM_OPENAI_COMPATIBLE -> "OpenRouter"
}

private fun maskApiKey(key: String?): String {
    val trimmed = key?.trim().orEmpty()
    if (trimmed.isEmpty()) return "No API key configured"
    if (trimmed.length <= 8) return "••••••••"
    return "${trimmed.take(4)}••••••••••${trimmed.takeLast(3)}"
}

object AgentSettingsUiHelper {
    /**
     * Determines the status label displayed in the AI Provider pill badge.
     * "Ready" is ONLY shown if the provider's API key is configured (hasKey == true).
     * Until the API key is configured, shows "Not Ready" (a negation of ready) for the selected provider,
     * or "Add Key" for unselected providers.
     */
    fun getProviderStatusLabel(hasKey: Boolean, isSelected: Boolean): String {
        return when {
            hasKey -> "Ready"
            isSelected -> "Not Ready"
            else -> "Add Key"
        }
    }
}

@Composable
private fun ModelCategoryTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFreeBadge: Boolean = false
) {
    val bg = if (isSelected) {
        if (isFreeBadge) Color(0xFF22C55E) else Color(0xFFD5FB66)
    } else KlixSurfaceCardItem

    val textCol = if (isSelected) {
        if (isFreeBadge) Color.White else Color(0xFF0C0D14)
    } else KlixInkPrimary

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(
                1.dp,
                if (isSelected) (if (isFreeBadge) Color(0xFF16A34A) else Color(0xFFD5FB66)) else KlixBorderHairline,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .pressEffect(0.95f)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = KlixTypography.bodySecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textCol,
            maxLines = 1,
            softWrap = false
        )
    }
}

private const val MAX_VISIBLE_MODEL_ROWS = 3

@Composable
private fun rememberModelRowMinHeight(): Dp {
    val density = LocalDensity.current
    return remember(density) {
        with(density) {
            22.dp + 20.sp.toDp() + 17.5.sp.toDp()
        }
    }
}

@Composable
private fun ModelItemRow(
    model: DiscoveredModel,
    isSelected: Boolean,
    minHeight: Dp,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isSelected) {
        if (isDark) Color(0xFF2A3626) else Color(0xFFEDF8CE)
    } else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .pressEffect(0.97f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = model.displayName,
                        style = KlixTypography.bodyPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = KlixInkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (model.isFree) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FREE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (model.provider == ModelProvider.CUSTOM_OPENAI_COMPATIBLE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF64748B).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF64748B).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PAID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KlixInkSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                val metaInfo = buildString {
                    if (model.contextLength != null && model.contextLength > 0) {
                        val kCtx = if (model.contextLength >= 1024) "${model.contextLength / 1024}k ctx" else "${model.contextLength} ctx"
                        append(kCtx)
                    }
                    if (model.promptPrice != null && model.promptPrice > 0.0) {
                        if (isNotEmpty()) append(" • ")
                        val perMillion = model.promptPrice * 1_000_000
                        val priceStr = if (perMillion < 0.01) "<$0.01/M in" else String.format(java.util.Locale.US, "$%.2f/M in", perMillion)
                        append(priceStr)
                    }
                    if (!model.description.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(model.description)
                    } else if (model.id != model.displayName) {
                        if (isNotEmpty()) append(" • ")
                        append(model.id)
                    }
                }

                if (metaInfo.isNotBlank()) {
                    Text(
                        text = metaInfo,
                        style = KlixTypography.bodySecondary,
                        color = KlixInkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5FB66)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF0C0D14),
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, KlixBorderHairline, CircleShape)
                )
            }
        }
    }
}
