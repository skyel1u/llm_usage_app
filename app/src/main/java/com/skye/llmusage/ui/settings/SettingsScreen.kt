package com.skye.llmusage.ui.settings
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skye.llmusage.BuildConfig
import com.skye.llmusage.R
import com.skye.llmusage.data.AppStyle
import com.skye.llmusage.data.ThemeMode
import com.skye.llmusage.ui.glass.GlassConfig
import com.skye.llmusage.ui.glass.GlassPanel
import com.skye.llmusage.ui.glass.LocalChromeLayer
import com.skye.llmusage.ui.glass.LocalGlassChromeInset
import com.skye.llmusage.ui.glass.LocalGlassController
import com.skye.llmusage.ui.theme.LocalGlassEnabled

/** 设置页:外观风格/主题模式选择、数据管理、关于 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    appStyle: AppStyle,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAppStyleChange: (AppStyle) -> Unit,
    onClearHistory: () -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val glass = LocalGlassEnabled.current && LocalGlassController.current != null

    if (glass) {
        LiquidSettingsScreen(
            themeMode, dynamicColor, appStyle,
            onThemeChange, onDynamicColorChange, onAppStyleChange,
        ) { showClearDialog = true }
    } else {
        Scaffold(
            topBar = { SettingsTopBar(useDefaults = true) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsGroups(
                    themeMode, dynamicColor, appStyle,
                    onThemeChange, onDynamicColorChange, onAppStyleChange,
                    bare = true,
                ) { showClearDialog = true }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.settings_clear_history_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearHistory()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** 玻璃模式设置页:分组纸面卡(iOS 分组列表形态)+ 顶栏滚动浮现 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiquidSettingsScreen(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    appStyle: AppStyle,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAppStyleChange: (AppStyle) -> Unit,
    onClearHistory: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val barAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(240),
        label = "settingsBar",
    )
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chrome = LocalChromeLayer.current

    SideEffect {
        chrome?.topBar?.value = { SettingsTopBar(useDefaults = false) }
        chrome?.topBarAlpha?.floatValue = barAlpha
    }
    DisposableEffect(chrome) {
        onDispose { chrome?.topBar?.value = null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(statusBarTop + 80.dp))
        SettingsGroups(
            themeMode, dynamicColor, appStyle,
            onThemeChange, onDynamicColorChange, onAppStyleChange,
            bare = false,
            onClearHistory = onClearHistory,
        )
        Spacer(Modifier.height(LocalGlassChromeInset.current + 24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(useDefaults: Boolean) {
    TopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        colors = if (useDefaults) TopAppBarDefaults.topAppBarColors()
        else TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

/**
 * 设置分组:bare=true 为经典裸布局(外层 Column 已有 16dp 间距);否则每组
 * 包进一块不透明纸面卡。
 */
@Composable
private fun SettingsGroups(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    appStyle: AppStyle,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAppStyleChange: (AppStyle) -> Unit,
    bare: Boolean,
    onClearHistory: () -> Unit = {},
) {
    val hasStyleSection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    if (hasStyleSection) {
        GroupPanel(key = "settings-style", bare = bare, topPadding = 8.dp) { StyleGroup(appStyle, onAppStyleChange) }
    }
    GroupPanel(
        key = "settings-appearance",
        bare = bare,
        topPadding = if (hasStyleSection) 0.dp else 8.dp,
    ) {
        Text(
            stringResource(R.string.settings_appearance),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = themeMode == mode,
                    onClick = { onThemeChange(mode) },
                )
                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // 动态取色(Android 12+ 跟随壁纸;关闭回退内置靛蓝色板)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_dynamic_color),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.settings_dynamic_color_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
        }
    }

    GroupPanel(key = "settings-data", bare = bare) {
        Text(
            stringResource(R.string.settings_data),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.settings_clear_history_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClearHistory) {
            Text(stringResource(R.string.settings_clear_history))
        }
    }

    HorizontalDivider()

    GroupPanel(key = "settings-about", bare = bare) {
        Text(
            stringResource(R.string.settings_about),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.app_name) + " v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.settings_privacy_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 外观风格(经典 / 液态玻璃);折射效果依赖 AGSL,仅 Android 13+ 提供选项 */
@Composable
private fun StyleGroup(appStyle: AppStyle, onAppStyleChange: (AppStyle) -> Unit) {
    Text(
        stringResource(R.string.settings_app_style),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    AppStyle.entries.forEach { style ->
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = appStyle == style,
                    onClick = { onAppStyleChange(style) },
                )
                Text(style.label, style = MaterialTheme.typography.bodyLarge)
            }
            if (style == AppStyle.LIQUID_GLASS) {
                Text(
                    stringResource(R.string.settings_liquid_glass_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupPanel(
    key: String,
    bare: Boolean,
    topPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    GlassPanel(
        key = key,
        config = GlassConfig.card(),
        modifier = Modifier.fillMaxWidth(),
        classic = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = topPadding),
            ) { content() }
        },
        glass = {
            if (bare) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
            } else {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) { content() }
            }
        },
    )
}
