package com.skye.llmusage.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 主题模式:跟随系统 / 浅色 / 深色 */
enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
    ;

    companion object {
        fun of(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(themeMode = ThemeMode.of(prefs[themeKey]))
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            if (mode == ThemeMode.SYSTEM) prefs.remove(themeKey) else prefs[themeKey] = mode.name
        }
    }
}
