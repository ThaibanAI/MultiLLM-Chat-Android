package com.thaibanai.multillmchat.ui.theme

import android.app.Application
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.thaibanai.multillmchat.data.local.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    application: Application,
    private val secureStorage: SecureStorage
) : AndroidViewModel(application) {

    var isDarkTheme by mutableStateOf(computeIsDark())
        private set

    fun setThemeMode(mode: String) {
        secureStorage.setThemeMode(mode)
        isDarkTheme = computeIsDark()
    }

    fun getThemeMode(): String = secureStorage.getThemeMode()

    private fun computeIsDark(): Boolean {
        return when (secureStorage.getThemeMode()) {
            SecureStorage.THEME_LIGHT -> false
            SecureStorage.THEME_DARK -> true
            else -> {
                // System default — will be computed at composition time
                false // placeholder, recomposed with actual value
            }
        }
    }
}
