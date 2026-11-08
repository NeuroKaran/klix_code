package com.klix.code.ui.components.agent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.klix.code.R
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.klix.code.ui.theme.ThemeManager
import com.klix.code.ui.theme.ThemeMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.ui.theme.AccentError
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.AccentSecondary
import com.klix.code.ui.theme.AccentWarning
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurface
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.components.glass.GlassCard
import com.klix.code.ui.components.glass.GlassButton
import com.klix.code.ui.components.glass.GlassBadge
import com.klix.code.ui.components.glass.GlassSegmentedControl
import com.klix.code.ui.components.glass.GlassSurface
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.LimeBrandColor
import com.klix.code.ui.theme.LimeDimColor
import com.klix.code.ui.theme.CyanBrandColor
import com.klix.code.ui.theme.CyanDimColor
import com.klix.code.ui.theme.OnAccentPrimaryColor
import com.klix.code.ui.theme.LocalIsDarkTheme
import com.klix.code.ui.theme.ObsidianBorder
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

@Composable
fun AgentSettingsDialog(
    currentSettings: ProviderSettings,
    onGetApiKey: (ModelProvider) -> String?,
    onSaveApiKey: (ModelProvider, String?) -> Unit,
    onSaveSettings: (ProviderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val dividerColor = if (isDark) Color(0xFF2A374E) else Color(0xFF94A3B8)
    var selectedProvider by remember { mutableStateOf(currentSettings.activeProvider) }
    var activeModel by remember { mutableStateOf(currentSettings.activeModel) }
    var customEndpoint by remember { mutableStateOf(currentSettings.customEndpointUrl) }
    var autoApproveRead by remember { mutableStateOf(currentSettings.autoApproveReadTools) }
    var autoApproveWrite by remember { mutableStateOf(currentSettings.autoApproveWriteTools) }
    var autoVerify by remember { mutableStateOf(currentSettings.autoVerifyEdits) }

    var apiKeyInput by remember(selectedProvider) {
        mutableStateOf(onGetApiKey(selectedProvider) ?: "")
    }
    var isKeyVisible by remember { mutableStateOf(false) }
    var keySavedNotification by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .padding(vertical = 24.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = GlassSurfaceElevatedColor
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mockup 2: Peeking Mascot on top of Settings card
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.klix_mascot_peeking),
                        contentDescription = "Klix Robot Peeking",
                        modifier = Modifier
                            .size(width = 110.dp, height = 52.dp)
                            .padding(top = 4.dp)
                    )
                }

                // Pinned Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LimeDimColor)
                                .glassBorder(CircleShape, strokeWidth = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = LimeBrandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Settings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Theme, AI models & editor preferences",
                                fontSize = 11.sp,
                                color = CyanBrandColor
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp).pressEffect(0.9f)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = dividerColor, thickness = 1.dp)

                // Scrollable Content Area (User scrolls for information)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // App Theme Section with Liquid Glass Slider (Reference 2 style)
                    Text(
                        text = "APP THEME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )

                    val context = LocalContext.current
                    val currentThemeMode by ThemeManager.themeMode.collectAsState()
                    val themeModes = ThemeMode.entries

                    GlassSegmentedControl(
                        items = themeModes,
                        selectedIndex = themeModes.indexOf(currentThemeMode),
                        onItemSelected = { idx ->
                            ThemeManager.setThemeMode(context, themeModes[idx])
                        },
                        modifier = Modifier.fillMaxWidth(),
                        itemLabel = { it.displayName }
                    )

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // Provider Selector Tabs
                    Text(
                        text = "SELECT ACTIVE PROVIDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModelProvider.entries.forEach { provider ->
                            val isSelected = selectedProvider == provider
                            val hasKey = !onGetApiKey(provider).isNullOrBlank()
                            val cardShape = RoundedCornerShape(14.dp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(if (isSelected) (if (isDark) Color(0xFF192415) else Color(0xFFF4FCE3)) else (if (isDark) Color(0xFF111622) else Color(0xFFF8FAFC)))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.5.dp, if (isDark) LimeBrandColor else Color(0xFF65A30D), cardShape)
                                        } else {
                                            Modifier.border(1.dp, if (isDark) ObsidianBorder else Color(0xFFCBD5E1), cardShape)
                                        }
                                    )
                                    .pressEffect(0.96f)
                                    .clickable {
                                        selectedProvider = provider
                                        activeModel = provider.defaultModel
                                        keySavedNotification = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        text = if (hasKey) "Key configured" else "No key configured",
                                        fontSize = 10.5.sp,
                                        color = if (hasKey) LimeBrandColor else AccentWarning
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = LimeBrandColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // API Key Input for selected provider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${selectedProvider.displayName} API Key",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                keySavedNotification = false
                            },
                            placeholder = {
                                Text("Paste your ${selectedProvider.name.lowercase()} key here", color = TextMuted, fontSize = 12.sp)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (keySavedNotification) {
                                Text("Key saved securely!", color = AccentSecondary, fontSize = 11.sp)
                            } else {
                                Text("Stored in Android Keystore", color = TextMuted, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    onSaveApiKey(selectedProvider, apiKeyInput.ifBlank { null })
                                    keySavedNotification = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Save Key", fontSize = 12.sp)
                            }
                        }
                    }

                    // Custom endpoint URL if custom provider
                    if (selectedProvider == ModelProvider.CUSTOM_OPENAI_COMPATIBLE) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Custom Endpoint Base URL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = customEndpoint,
                                onValueChange = { customEndpoint = it },
                                placeholder = { Text("https://openrouter.ai/api/v1/chat/completions", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = AccentPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    // Active Model Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Model",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        OutlinedTextField(
                            value = activeModel,
                            onValueChange = { activeModel = it },
                            placeholder = { Text(selectedProvider.defaultModel, color = TextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Quick model chips with horizontal scroll
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            selectedProvider.supportedModels.forEach { modelName ->
                                val isCurrent = activeModel == modelName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCurrent) LimeDimColor else DarkSurface)
                                        .border(0.5.dp, if (isCurrent) AccentPrimary else DarkBorder, RoundedCornerShape(6.dp))
                                        .clickable { activeModel = modelName }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = modelName.substringAfterLast('/'),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isCurrent) AccentPrimary else TextSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // Tool Execution Policies
                    Text(
                        text = "TOOL APPROVAL POLICY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-approve Read Tools",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Allow reading files, search, and directory listings without prompt",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = autoApproveRead,
                            onCheckedChange = { autoApproveRead = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LimeBrandColor,
                                checkedTrackColor = LimeDimColor
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-approve Write & Commands",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Apply diffs and run commands without manual approval (Not recommended)",
                                fontSize = 11.sp,
                                color = AccentWarning
                            )
                        }
                        Switch(
                            checked = autoApproveWrite,
                            onCheckedChange = { autoApproveWrite = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentWarning,
                                checkedTrackColor = if (isDark) Color(0xFF452C06) else Color(0xFFFDE68A)
                            )
                        )
                    }

                    // Auto-Verify & Self-Correct Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Auto-Verify & Self-Correct",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Run sandbox syntax/linter checks on edits and auto-heal errors",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = autoVerify,
                            onCheckedChange = { autoVerify = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LimeBrandColor,
                                checkedTrackColor = LimeDimColor
                            )
                        )
                    }
                }

                // Pinned Footer: The button is ALWAYS in front of the user (pinned at bottom)
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                Surface(
                    color = GlassSurfaceColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        GlassButton(
                            onClick = {
                                val updated = currentSettings.copy(
                                    activeProvider = selectedProvider,
                                    activeModel = activeModel.ifBlank { selectedProvider.defaultModel },
                                    customEndpointUrl = customEndpoint,
                                    autoApproveReadTools = autoApproveRead,
                                    autoApproveWriteTools = autoApproveWrite,
                                    autoVerifyEdits = autoVerify
                                )
                                onSaveSettings(updated)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            containerColor = LimeBrandColor,
                            contentColor = OnAccentPrimaryColor,
                            shape = CircleShape
                        ) {
                            Text("Apply & Close", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }
}
