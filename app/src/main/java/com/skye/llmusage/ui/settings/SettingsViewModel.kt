package com.skye.llmusage.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skye.llmusage.LlmUsageApp
import com.skye.llmusage.data.Settings
import com.skye.llmusage.data.SettingsRepository
import com.skye.llmusage.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: SettingsRepository = (app as LlmUsageApp).settingsRepository

    val settings: StateFlow<Settings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repo.setDynamicColor(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { (getApplication<LlmUsageApp>()).repository.clearHistory() }
    }
}
