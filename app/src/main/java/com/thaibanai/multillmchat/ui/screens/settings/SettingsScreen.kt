package com.thaibanai.multillmchat.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thaibanai.multillmchat.data.local.SecureStorage
import com.thaibanai.multillmchat.data.remote.model.LLMProvider
import com.thaibanai.multillmchat.ui.theme.*
import com.thaibanai.multillmchat.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveSettings()
                        showSavedSnackbar = true
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ===== Anthropic Section =====
            ProviderSettingsSection(
                title = "Anthropic Claude",
                icon = Icons.Outlined.Psychology,
                accentColor = ClaudeOrange,
                isEnabled = uiState.claudeConfig.isEnabled,
                onEnabledChange = { viewModel.updateClaudeEnabled(it) },
                apiKey = uiState.claudeConfig.apiKey,
                onApiKeyChange = { viewModel.updateClaudeApiKey(it) },
                showKey = uiState.claudeShowKey,
                onToggleKeyVisibility = { viewModel.toggleClaudeKeyVisibility() },
                model = uiState.claudeConfig.model,
                onModelChange = { viewModel.updateClaudeModel(it) },
                suggestedModels = Constants.CLAUDE_MODELS,
                hasSavedKey = uiState.claudeConfig.hasSavedKey
            )

            HorizontalDivider()

            // ===== OpenAI Section =====
            ProviderSettingsSection(
                title = "OpenAI",
                icon = Icons.Outlined.AutoAwesome,
                accentColor = OpenAiGreen,
                isEnabled = uiState.openAiConfig.isEnabled,
                onEnabledChange = { viewModel.updateOpenAiEnabled(it) },
                apiKey = uiState.openAiConfig.apiKey,
                onApiKeyChange = { viewModel.updateOpenAiApiKey(it) },
                showKey = uiState.openAiShowKey,
                onToggleKeyVisibility = { viewModel.toggleOpenAiKeyVisibility() },
                model = uiState.openAiConfig.model,
                onModelChange = { viewModel.updateOpenAiModel(it) },
                suggestedModels = Constants.OPENAI_MODELS,
                hasSavedKey = uiState.openAiConfig.hasSavedKey
            )

            HorizontalDivider()

            // ===== DeepSeek Section =====
            ProviderSettingsSection(
                title = "DeepSeek",
                icon = Icons.Outlined.Hub,
                accentColor = DeepSeekBlue,
                isEnabled = uiState.deepSeekConfig.isEnabled,
                onEnabledChange = { viewModel.updateDeepSeekEnabled(it) },
                apiKey = uiState.deepSeekConfig.apiKey,
                onApiKeyChange = { viewModel.updateDeepSeekApiKey(it) },
                showKey = uiState.deepSeekShowKey,
                onToggleKeyVisibility = { viewModel.toggleDeepSeekKeyVisibility() },
                model = uiState.deepSeekConfig.model,
                onModelChange = { viewModel.updateDeepSeekModel(it) },
                suggestedModels = Constants.DEEPSEEK_MODELS,
                hasSavedKey = uiState.deepSeekConfig.hasSavedKey
            )

            HorizontalDivider()

            // ===== Theme Section =====
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val themeOptions = listOf(
                SecureStorage.THEME_SYSTEM to "System Default",
                SecureStorage.THEME_LIGHT to "Light",
                SecureStorage.THEME_DARK to "Dark"
            )

            themeOptions.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.themeMode == value,
                        onClick = { viewModel.setThemeMode(value) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            HorizontalDivider()

            // ===== Danger Zone =====
            Text(
                "Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedButton(
                onClick = { showClearConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear All Conversations")
            }

            Spacer(Modifier.height(24.dp))

            // ===== Version Info =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MultiLLM Chat v${Constants.APP_VERSION}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Clear history confirmation
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All History") },
            text = { Text("This will delete all conversations and messages. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showKey: Boolean,
    onToggleKeyVisibility: () -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    suggestedModels: List<String>,
    hasSavedKey: Boolean
) {
    var expandedModelDropdown by remember { mutableStateOf(false) }
    var customModel by remember { mutableStateOf(model) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    checkedThumbColor = accentColor
                )
            )
        }

        // API Key
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleKeyVisibility) {
                    Icon(
                        if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showKey) "Hide key" else "Show key"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            supportingText = if (hasSavedKey) {
                { Text("✓ Key saved", color = accentColor) }
            } else null
        )

        // Model selection
        ExposedDropdownMenuBox(
            expanded = expandedModelDropdown,
            onExpandedChange = { expandedModelDropdown = it }
        ) {
            OutlinedTextField(
                value = customModel,
                onValueChange = {
                    customModel = it
                    onModelChange(it)
                },
                label = { Text("Model") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModelDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(
                expanded = expandedModelDropdown,
                onDismissRequest = { expandedModelDropdown = false }
            ) {
                suggestedModels.forEach { suggestedModel ->
                    DropdownMenuItem(
                        text = { Text(suggestedModel) },
                        onClick = {
                            customModel = suggestedModel
                            onModelChange(suggestedModel)
                            expandedModelDropdown = false
                        }
                    )
                }
            }
        }
    }
}
