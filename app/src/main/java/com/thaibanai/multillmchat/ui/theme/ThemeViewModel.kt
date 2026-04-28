package com.thaibanai.multillmchat.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thaibanai.multillmchat.data.local.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val secureStorage: SecureStorage
) : ViewModel() {

    var themeMode by mutableStateOf(secureStorage.getThemeMode())
        private set

    init {
        viewModelScope.launch {
            secureStorage.themeModeFlow.collect { mode ->
                themeMode = mode
            }
        }
    }
}
