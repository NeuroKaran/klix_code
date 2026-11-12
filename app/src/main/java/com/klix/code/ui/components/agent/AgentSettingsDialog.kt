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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klix.code.R
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
 * - Bottom CTA: KlixPrimaryPillButton(text = "Apply & Close")
 */
@Composable
fun AgentSettingsDialog(
    currentSettings: ProviderSettings,
    onGetApiKey: (ModelProvider) -> String?,
    onSaveApiKey: (ModelProvider, String?) -> Unit,
    onSaveSettings: (ProviderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val currentThemeMode by ThemeManager.themeMode.collectAsState()

    var selectedProvider by remember { mutableStateOf(currentSettings.activeProvider) }
    var editingKeyProvider by remember { mutableStateOf<ModelProvider?>(null) }
    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

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
                            onClick = { selectedProvider = ModelProvider.GEMINI },
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
                            onClick = { selectedProvider = ModelProvider.ANTHROPIC },
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
                            onClick = { selectedProvider = ModelProvider.OPENAI },
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
                            onClick = { selectedProvider = ModelProvider.CUSTOM_OPENAI_COMPATIBLE },
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
                                                    onSaveApiKey(prov, apiKeyInput.trim().ifEmpty { null })
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

                Spacer(modifier = Modifier.weight(1f, fill = false))

                // Bottom CTA: Apply & Close
                KlixPrimaryPillButton(
                    text = "Apply & Close",
                    onClick = {
                        val updated = currentSettings.copy(
                            activeProvider = selectedProvider
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
