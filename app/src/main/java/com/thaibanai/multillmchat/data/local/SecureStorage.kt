package com.thaibanai.multillmchat.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted storage for API keys and other sensitive data.
 * Uses Android EncryptedSharedPreferences with AES-256 GCM.
 */
@Singleton
class SecureStorage @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "multillm_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Anthropic
    fun getAnthropicApiKey(): String? = prefs.getString(KEY_ANTHROPIC_KEY, null)
    fun setAnthropicApiKey(key: String) = prefs.edit().putString(KEY_ANTHROPIC_KEY, key).apply()
    fun getAnthropicModel(): String = prefs.getString(KEY_ANTHROPIC_MODEL, DEFAULT_CLAUDE_MODEL) ?: DEFAULT_CLAUDE_MODEL
    fun setAnthropicModel(model: String) = prefs.edit().putString(KEY_ANTHROPIC_MODEL, model).apply()
    fun isAnthropicEnabled(): Boolean = prefs.getBoolean(KEY_ANTHROPIC_ENABLED, true)
    fun setAnthropicEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ANTHROPIC_ENABLED, enabled).apply()

    // OpenAI
    fun getOpenAiApiKey(): String? = prefs.getString(KEY_OPENAI_KEY, null)
    fun setOpenAiApiKey(key: String) = prefs.edit().putString(KEY_OPENAI_KEY, key).apply()
    fun getOpenAiModel(): String = prefs.getString(KEY_OPENAI_MODEL, DEFAULT_OPENAI_MODEL) ?: DEFAULT_OPENAI_MODEL
    fun setOpenAiModel(model: String) = prefs.edit().putString(KEY_OPENAI_MODEL, model).apply()
    fun isOpenAiEnabled(): Boolean = prefs.getBoolean(KEY_OPENAI_ENABLED, true)
    fun setOpenAiEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_OPENAI_ENABLED, enabled).apply()

    // DeepSeek
    fun getDeepSeekApiKey(): String? = prefs.getString(KEY_DEEPSEEK_KEY, null)
    fun setDeepSeekApiKey(key: String) = prefs.edit().putString(KEY_DEEPSEEK_KEY, key).apply()
    fun getDeepSeekModel(): String = prefs.getString(KEY_DEEPSEEK_MODEL, DEFAULT_DEEPSEEK_MODEL) ?: DEFAULT_DEEPSEEK_MODEL
    fun setDeepSeekModel(model: String) = prefs.edit().putString(KEY_DEEPSEEK_MODEL, model).apply()
    fun isDeepSeekEnabled(): Boolean = prefs.getBoolean(KEY_DEEPSEEK_ENABLED, true)
    fun setDeepSeekEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DEEPSEEK_ENABLED, enabled).apply()

    // Theme
    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    fun setThemeMode(mode: String) = prefs.edit().putString(KEY_THEME_MODE, mode).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ANTHROPIC_KEY = "anthropic_api_key"
        private const val KEY_ANTHROPIC_MODEL = "anthropic_model"
        private const val KEY_ANTHROPIC_ENABLED = "anthropic_enabled"
        private const val KEY_OPENAI_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_OPENAI_ENABLED = "openai_enabled"
        private const val KEY_DEEPSEEK_KEY = "deepseek_api_key"
        private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        private const val KEY_DEEPSEEK_ENABLED = "deepseek_enabled"
        private const val KEY_THEME_MODE = "theme_mode"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"

        const val DEFAULT_CLAUDE_MODEL = "claude-opus-4-7"
        const val DEFAULT_OPENAI_MODEL = "gpt-5.5"
        const val DEFAULT_DEEPSEEK_MODEL = "deepseek-v4-pro"
    }
}
