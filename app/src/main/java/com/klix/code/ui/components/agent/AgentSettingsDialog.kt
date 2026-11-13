package com.klix.code.ui.components.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
import com.klix.code.data.agent.model.DiscoveredModel
import com.klix.code.data.agent.model.ModelDiscoveryRepository
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.ui.components.KlixPeekingMascotScaffold
import com.klix.code.ui.components.KlixPrimaryPillButton
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.ThemeManager
import com.klix.code.ui.theme.ThemeMode

enum class OpenRouterModelFilter {
    ALL,
    FREE,
    PAID
}

/**
 * Screen 2: Settings Modal / Screen
 * Matches design specification:
 * - Layout: KlixPeekingMascotScaffold(title = "Settings")
 * - Card 1 — App Theme (surfaceBentoGroup, RoundedCornerShape(22.dp)):
 *   - Header "App Theme"
 *   - Row of 3 pill chips (RoundedCornerShape(50)):
 *     - [Sun Vector Icon] Light (highlighted in #D5FB66 when Light is active)
 *     - [Moon Vector Icon] Dark (highlighted in #D5FB66 when Dark is active)
 *     - [Monitor Vector Icon] System
 * - Card 2 — Active AI Provider (surfaceBentoGroup, RoundedCornerShape(22.dp)):
 *   - Header "Active AI Provider"
 *   - 4 stacked provider pill rows (52.dp height, RoundedCornerShape(26.dp)):
 *     1. [Official Google Gemini Star Logo] Google Gemini — full #D5FB66 lime highlight fill with right pill badge "Ready"
 *     2. [Official Anthropic Claude Starburst Logo] Anthropic Claude — right pill button "Add Key" / "Ready"
 *     3. [Official OpenAI Blossom Logo] OpenAI — right pill button "Add Key" / "Ready"
 *     4. [Vector Node Graph Icon] OpenRouter / Groq — right pill button "Add Key" / "Ready"
 * - Card 3 — Model Selection (dynamic querying from provider API, free vs paid categorization for OpenRouter)
 * - Bottom CTA: KlixPrimaryPillButton(text = "Apply & Close")
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val currentThemeMode by ThemeManager.themeMode.collectAsState()

    var selectedProvider by remember { mutableStateOf(currentSettings.activeProvider) }
    var selectedModel by remember { mutableStateOf(currentSettings.activeModel) }
    var editingKeyProvider by remember { mutableStateOf<ModelProvider?>(null) }
    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        KlixPeekingMascotScaffold(
            title = "Settings"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: App Theme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(KlixSurfaceBentoGroup)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "App Theme",
                            style = KlixTypography.sectionHeader,
                            color = KlixInkPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeChip(
                                label = "Light",
                                icon = Icons.Default.LightMode,
                                isSelected = currentThemeMode == ThemeMode.LIGHT,
                                onClick = { ThemeManager.setThemeMode(context, ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeChip(
                                label = "Dark",
                                icon = Icons.Default.DarkMode,
                                isSelected = currentThemeMode == ThemeMode.DARK,
                                onClick = { ThemeManager.setThemeMode(context, ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeChip(
                                label = "System",
                                icon = Icons.Default.DesktopWindows,
                                isSelected = currentThemeMode == ThemeMode.SYSTEM,
                                onClick = { ThemeManager.setThemeMode(context, ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Card 2: Active AI Provider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(KlixSurfaceBentoGroup)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Active AI Provider",
                            style = KlixTypography.sectionHeader,
                            color = KlixInkPrimary
                        )

                        // 4 Stacked Provider Pill Rows
                        ProviderPillRow(
                            name = "Google Gemini",
                            logoRes = R.drawable.ic_logo_gemini,
                            isSelected = selectedProvider == ModelProvider.GEMINI,
                            hasKey = onGetApiKey(ModelProvider.GEMINI)?.isNotEmpty() == true,
                            onClick = {
                                selectedProvider = ModelProvider.GEMINI
                                onRefreshModels(ModelProvider.GEMINI, onGetApiKey(ModelProvider.GEMINI), false)
                            },
                            onEditKey = {
                                editingKeyProvider = ModelProvider.GEMINI
                                apiKeyInput = onGetApiKey(ModelProvider.GEMINI) ?: ""
                            }
                        )

                        ProviderPillRow(
                            name = "Anthropic Claude",
                            logoRes = R.drawable.ic_logo_claude,
                            isSelected = selectedProvider == ModelProvider.ANTHROPIC,
                            hasKey = onGetApiKey(ModelProvider.ANTHROPIC)?.isNotEmpty() == true,
                            onClick = {
                                selectedProvider = ModelProvider.ANTHROPIC
                                onRefreshModels(ModelProvider.ANTHROPIC, onGetApiKey(ModelProvider.ANTHROPIC), false)
                            },
                            onEditKey = {
                                editingKeyProvider = ModelProvider.ANTHROPIC
                                apiKeyInput = onGetApiKey(ModelProvider.ANTHROPIC) ?: ""
                            }
                        )

                        ProviderPillRow(
                            name = "OpenAI",
                            logoRes = R.drawable.ic_logo_openai,
                            isSelected = selectedProvider == ModelProvider.OPENAI,
                            hasKey = onGetApiKey(ModelProvider.OPENAI)?.isNotEmpty() == true,
                            onClick = {
                                selectedProvider = ModelProvider.OPENAI
                                onRefreshModels(ModelProvider.OPENAI, onGetApiKey(ModelProvider.OPENAI), false)
                            },
                            onEditKey = {
                                editingKeyProvider = ModelProvider.OPENAI
                                apiKeyInput = onGetApiKey(ModelProvider.OPENAI) ?: ""
                            }
                        )

                        ProviderPillRow(
                            name = "OpenRouter / Groq",
                            logoRes = R.drawable.ic_logo_openrouter,
                            isSelected = selectedProvider == ModelProvider.CUSTOM_OPENAI_COMPATIBLE,
                            hasKey = onGetApiKey(ModelProvider.CUSTOM_OPENAI_COMPATIBLE)?.isNotEmpty() == true,
                            onClick = {
                                selectedProvider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE
                                onRefreshModels(ModelProvider.CUSTOM_OPENAI_COMPATIBLE, onGetApiKey(ModelProvider.CUSTOM_OPENAI_COMPATIBLE), false)
                            },
                            onEditKey = {
                                editingKeyProvider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE
                                apiKeyInput = onGetApiKey(ModelProvider.CUSTOM_OPENAI_COMPATIBLE) ?: ""
                            }
                        )

                        // Inline API key entry when user clicks Add/Edit Key
                        AnimatedVisibility(visible = editingKeyProvider != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(KlixSurfaceCardItem)
                                    .border(1.dp, KlixBorderHairline, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "API Key for ${editingKeyProvider?.name ?: ""}",
                                    style = KlixTypography.bodySecondary,
                                    fontWeight = FontWeight.Bold,
                                    color = KlixInkPrimary
                                )

                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    placeholder = { Text("Enter API key...", style = KlixTypography.bodySecondary) },
                                    singleLine = true,
                                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                            Icon(
                                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = KlixInkSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
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
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFD5FB66))
                                            .clickable {
                                                editingKeyProvider?.let { prov ->
                                                    val trimmedKey = apiKeyInput.trim().ifEmpty { null }
                                                    onSaveApiKey(prov, trimmedKey)
                                                    onRefreshModels(prov, trimmedKey, true)
                                                }
                                                editingKeyProvider = null
                                            }
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
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
                }

                // Card 3: Model Selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(KlixSurfaceBentoGroup)
                        .border(1.dp, KlixBorderHairline, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Available Models",
                                    style = KlixTypography.sectionHeader,
                                    color = KlixInkPrimary
                                )
                                Text(
                                    text = if (isFetchingModels) "Discovering models from ${selectedProvider.displayName}..."
                                           else "${filteredModels.size} models • Active: ${selectedModel.substringAfterLast('/')}",
                                    style = KlixTypography.bodySecondary,
                                    color = KlixInkSecondary
                                )
                            }

                            IconButton(
                                onClick = { onRefreshModels(selectedProvider, onGetApiKey(selectedProvider), true) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (isFetchingModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFD5FB66)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Models",
                                        tint = KlixInkSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // For OpenRouter, display Free vs Paid Category Filter Tabs
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
                                    onClick = { openRouterFilter = OpenRouterModelFilter.ALL },
                                    modifier = Modifier.weight(1f)
                                )
                                ModelCategoryTab(
                                    label = "Free ($freeCount)",
                                    isFreeBadge = true,
                                    isSelected = openRouterFilter == OpenRouterModelFilter.FREE,
                                    onClick = { openRouterFilter = OpenRouterModelFilter.FREE },
                                    modifier = Modifier.weight(1f)
                                )
                                ModelCategoryTab(
                                    label = "Paid ($paidCount)",
                                    isSelected = openRouterFilter == OpenRouterModelFilter.PAID,
                                    onClick = { openRouterFilter = OpenRouterModelFilter.PAID },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Search Filter if more than 3 models
                        if (modelsForProvider.size > 3) {
                            OutlinedTextField(
                                value = modelSearchQuery,
                                onValueChange = { modelSearchQuery = it },
                                placeholder = {
                                    Text("Filter models...", style = KlixTypography.bodySecondary, color = KlixInkSecondary)
                                },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = KlixInkSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = KlixTypography.bodySecondary.copy(color = KlixInkPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD5FB66),
                                    unfocusedBorderColor = KlixBorderHairline
                                )
                            )
                        }

                        // Models List
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredModels.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isFetchingModels) "Discovering models from ${selectedProvider.displayName}..."
                                               else "No matching models found.",
                                        style = KlixTypography.bodySecondary,
                                        color = KlixInkSecondary
                                    )
                                }
                            } else {
                                val displayedModels = remember(filteredModels, selectedModel) {
                                    if (filteredModels.size <= 40) {
                                        filteredModels
                                    } else {
                                        val top = filteredModels.take(40).toMutableList()
                                        val selectedItem = filteredModels.find { it.id == selectedModel }
                                        if (selectedItem != null && !top.contains(selectedItem)) {
                                            top.add(0, selectedItem)
                                        }
                                        top
                                    }
                                }

                                displayedModels.forEach { model ->
                                    ModelItemRow(
                                        model = model,
                                        isSelected = model.id == selectedModel,
                                        onClick = { selectedModel = model.id }
                                    )
                                }
                                val extraCount = filteredModels.size - displayedModels.size
                                if (extraCount > 0) {
                                    Text(
                                        text = "+ $extraCount more models (type in filter to search)",
                                        style = KlixTypography.bodySecondary,
                                        color = KlixInkSecondary,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                // Bottom CTA: Apply & Close
                KlixPrimaryPillButton(
                    text = "Apply & Close",
                    onClick = {
                        val updated = currentSettings.copy(
                            activeProvider = selectedProvider,
                            activeModel = selectedModel
                        )
                        onSaveSettings(updated)
                        onDismiss()
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isSelected) Color(0xFFD5FB66) else KlixSurfaceCardItem
    val textCol = if (isSelected) Color(0xFF0C0D14) else KlixInkPrimary

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (isSelected) Color(0xFFD5FB66) else KlixBorderHairline, CircleShape)
            .clickable(onClick = onClick)
            .pressEffect(0.94f)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textCol,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = KlixTypography.bodySecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textCol
            )
        }
    }
}

@Composable
private fun ProviderPillRow(
    name: String,
    logoRes: Int,
    isSelected: Boolean,
    hasKey: Boolean,
    onClick: () -> Unit,
    onEditKey: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val rowBg = if (isSelected) Color(0xFFD5FB66) else KlixSurfaceCardItem
    val textCol = if (isSelected) Color(0xFF0C0D14) else KlixInkPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(rowBg)
            .border(1.dp, if (isSelected) Color(0xFF84CC16) else KlixBorderHairline, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.96f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = name,
                style = KlixTypography.cardTitle,
                color = textCol
            )
        }

        // Right pill badge/button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isSelected) Color.White else (if (isDark) Color(0xFF2E364F) else Color(0xFFE2E8F0)))
                .clickable(onClick = onEditKey)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasKey || isSelected) "Ready" else "Add Key",
                style = KlixTypography.codePill,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF0C0D14) else KlixInkPrimary
            )
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
    val isDark = LocalIsDarkTheme.current
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
            maxLines = 1
        )
    }
}

@Composable
private fun ModelItemRow(
    model: DiscoveredModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isSelected) {
        if (isDark) Color(0xFF2A3626) else Color(0xFFEDF8CE)
    } else KlixSurfaceCardItem
    val borderCol = if (isSelected) Color(0xFFD5FB66) else KlixBorderHairline

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .pressEffect(0.97f)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = model.displayName,
                        style = KlixTypography.bodyPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = KlixInkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
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
                                color = KlixInkSecondary
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
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5FB66)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF0C0D14),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
