package com.skye.llmusage.ui.settings
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skye.llmusage.BuildConfig
import com.skye.llmusage.R
import com.skye.llmusage.data.AppStyle
import com.skye.llmusage.data.ThemeMode
import com.skye.llmusage.ui.glass.GlassConfig
import com.skye.llmusage.ui.glass.GlassPanel

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

    Scaffold(
        topBar = {
            GlassPanel(
                key = "settings-topbar",
                config = GlassConfig.bar(),
                modifier = Modifier.fillMaxWidth(),
                classic = {
                    TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
                },
                glass = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings_title)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 外观风格(经典 / 液态玻璃);折射效果依赖 AGSL,仅 Android 13+ 提供选项
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SettingsGroupPanel(key = "settings-style", topPadding = 8.dp) {
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
            }

            SettingsGroupPanel(key = "settings-appearance", topPadding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 0.dp else 8.dp) {
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

            SettingsGroupPanel(key = "settings-data") {
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
                TextButton(onClick = { showClearDialog = true }) {
                    Text(stringResource(R.string.settings_clear_history))
                }
            }

            HorizontalDivider()

            SettingsGroupPanel(key = "settings-about") {
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
            Spacer(Modifier.height(16.dp))
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

/**
 * 设置分组:经典模式为裸布局(外层 Column 已有 16dp 间距,首个分组的标题保留
 * 顶部 8dp 补偿),玻璃模式整组包进一块玻璃面板。
 */
@Composable
private fun SettingsGroupPanel(
    key: String,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
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
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        },
    )
}
