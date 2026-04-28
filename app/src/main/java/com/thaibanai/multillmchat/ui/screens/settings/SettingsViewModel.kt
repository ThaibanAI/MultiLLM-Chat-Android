package com.thaibanai.multillmchat.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thaibanai.multillmchat.data.local.SecureStorage
import com.thaibanai.multillmchat.data.remote.model.LLMProvider
import com.thaibanai.multillmchat.data.repository.ChatRepository
import com.thaibanai.multillmchat.domain.model.ProviderConfigState
import com.thaibanai.multillmchat.ui.theme.ThemeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val claudeConfig: ProviderConfigState = ProviderConfigState(provider = LLMProvider.CLAUDE),
    val openAiConfig: ProviderConfigState = ProviderConfigState(provider = LLMProvider.OPENAI),
    val deepSeekConfig: ProviderConfigState = ProviderConfigState(provider = LLMProvider.DEEPSEEK),
    val themeMode: String = SecureStorage.THEME_SYSTEM,
    val claudeShowKey: Boolean = false,
    val openAiShowKey: Boolean = false,
    val deepSeekShowKey: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val themeViewModel: ThemeViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                claudeConfig = repository.getProviderConfig(LLMProvider.CLAUDE),
                openAiConfig = repository.getProviderConfig(LLMProvider.OPENAI),
                deepSeekConfig = repository.getProviderConfig(LLMProvider.DEEPSEEK),
                themeMode = repository.getThemeMode()
            )
        }
    }

    fun updateClaudeApiKey(key: String) {
        _uiState.update { it.copy(claudeConfig = it.claudeConfig.copy(apiKey = key)) }
    }

    fun updateClaudeModel(model: String) {
        _uiState.update { it.copy(claudeConfig = it.claudeConfig.copy(model = model)) }
    }

    fun updateClaudeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(claudeConfig = it.claudeConfig.copy(isEnabled = enabled)) }
    }

    fun updateOpenAiApiKey(key: String) {
        _uiState.update { it.copy(openAiConfig = it.openAiConfig.copy(apiKey = key)) }
    }

    fun updateOpenAiModel(model: String) {
        _uiState.update { it.copy(openAiConfig = it.openAiConfig.copy(model = model)) }
    }

    fun updateOpenAiEnabled(enabled: Boolean) {
        _uiState.update { it.copy(openAiConfig = it.openAiConfig.copy(isEnabled = enabled)) }
    }

    fun updateDeepSeekApiKey(key: String) {
        _uiState.update { it.copy(deepSeekConfig = it.deepSeekConfig.copy(apiKey = key)) }
    }

    fun updateDeepSeekModel(model: String) {
        _uiState.update { it.copy(deepSeekConfig = it.deepSeekConfig.copy(model = model)) }
    }

    fun updateDeepSeekEnabled(enabled: Boolean) {
        _uiState.update { it.copy(deepSeekConfig = it.deepSeekConfig.copy(isEnabled = enabled)) }
    }

    fun toggleClaudeKeyVisibility() {
        _uiState.update { it.copy(claudeShowKey = !it.claudeShowKey) }
    }

    fun toggleOpenAiKeyVisibility() {
        _uiState.update { it.copy(openAiShowKey = !it.openAiShowKey) }
    }

    fun toggleDeepSeekKeyVisibility() {
        _uiState.update { it.copy(deepSeekShowKey = !it.deepSeekShowKey) }
    }

    fun setThemeMode(mode: String) {
        _uiState.update { it.copy(themeMode = mode) }
        themeViewModel.setThemeMode(mode)
        repository.setThemeMode(mode)
    }

    fun saveSettings() {
        viewModelScope.launch {
            repository.saveProviderConfig(_uiState.value.claudeConfig)
            repository.saveProviderConfig(_uiState.value.openAiConfig)
            repository.saveProviderConfig(_uiState.value.deepSeekConfig)
            repository.setThemeMode(_uiState.value.themeMode)
            themeViewModel.setThemeMode(_uiState.value.themeMode)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllConversations()
        }
    }
}
