package com.skye.llmusage.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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

/** 外观风格:经典 Material / iOS 液态玻璃(折射效果仅 Android 13+) */
enum class AppStyle(val label: String) {
    CLASSIC("经典"),
    LIQUID_GLASS("液态玻璃"),
    ;

    companion object {
        fun of(name: String?): AppStyle = entries.firstOrNull { it.name == name } ?: CLASSIC
    }
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val appStyle: AppStyle = AppStyle.CLASSIC,
)

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    private val appStyleKey = stringPreferencesKey("app_style")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = ThemeMode.of(prefs[themeKey]),
            dynamicColor = prefs[dynamicColorKey] ?: true,
            appStyle = AppStyle.of(prefs[appStyleKey]),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            if (mode == ThemeMode.SYSTEM) prefs.remove(themeKey) else prefs[themeKey] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            if (enabled) prefs.remove(dynamicColorKey) else prefs[dynamicColorKey] = false
        }
    }

    suspend fun setAppStyle(style: AppStyle) {
        context.dataStore.edit { prefs ->
            if (style == AppStyle.CLASSIC) prefs.remove(appStyleKey) else prefs[appStyleKey] = style.name
        }
    }
}
