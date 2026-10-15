package com.skye.llmusage.ui.detail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.skye.llmusage.R
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.data.db.SnapshotEntity
import com.skye.llmusage.ui.ChartLegend
import com.skye.llmusage.ui.HistoryChart
import com.skye.llmusage.ui.RefreshIcon
import com.skye.llmusage.ui.glass.GlassAlertDialog
import com.skye.llmusage.ui.glass.GlassConfig
import com.skye.llmusage.ui.glass.GlassPanel
import com.skye.llmusage.ui.glass.LocalChromeLayer
import com.skye.llmusage.ui.glass.LocalGlassChromeInset
import com.skye.llmusage.ui.glass.LocalGlassController
import com.skye.llmusage.ui.niceCeil
import com.skye.llmusage.ui.theme.LocalGlassEnabled
import com.skye.llmusage.ui.theme.tierColor
import com.skye.llmusage.util.Fmt

/** 单账户详情:本地快照历史曲线(5h% / 周%),编辑/删除 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    account: AccountUi?,
    history: List<SnapshotEntity>,
    refreshing: Boolean,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
) {
    var showDelete by remember { mutableStateOf(false) }
    val glass = LocalGlassEnabled.current && LocalGlassController.current != null

    if (glass) {
        LiquidDetailScreen(account, history, refreshing, onBack, onEdit, onDelete, onRefresh, { showDelete = true })
    } else {
        Scaffold(
            topBar = {
                DetailTopBar(onBack, onEdit, onDelete, { showDelete = true }, onRefresh, account, refreshing, useDefaults = true)
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
                DetailChartList(history)
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showDelete) {
        GlassAlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** 玻璃模式详情:内容穿层滚动,顶栏静止透明、滚动后玻璃浮现 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiquidDetailScreen(
    account: AccountUi?,
    history: List<SnapshotEntity>,
    refreshing: Boolean,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onShowDelete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val barAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(240),
        label = "detailBar",
    )
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chrome = LocalChromeLayer.current
    val chromeOwner = remember { Any() }

    SideEffect {
        chrome?.topBar?.set(chromeOwner) {
            DetailTopBar(onBack, onEdit, onDelete, onShowDelete, onRefresh, account, refreshing, useDefaults = false)
        }
        chrome?.topBarAlpha?.floatValue = barAlpha
    }
    DisposableEffect(chrome) {
        onDispose { chrome?.topBar?.clear(chromeOwner) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(statusBarTop + 80.dp))
        DetailChartList(history)
        Spacer(Modifier.height(LocalGlassChromeInset.current + 24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit,
    onShowDelete: () -> Unit,
    onRefresh: () -> Unit,
    account: AccountUi?,
    refreshing: Boolean,
    useDefaults: Boolean,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.detail_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                RefreshIcon(spinning = refreshing)
            }
            IconButton(onClick = { account?.let { onEdit(it.id) } }) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_account))
            }
            IconButton(onClick = onShowDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete_account))
            }
        },
        colors = if (useDefaults) TopAppBarDefaults.topAppBarColors()
        else TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

/** 图表块列表:5 小时% / 周% / 余额 */
@Composable
private fun DetailChartList(history: List<SnapshotEntity>) {
    val fiveHourPoints = remember(history) {
        history.mapNotNull { s -> s.fiveHourPct?.let { s.timestamp to it } }
    }
    val weeklyPoints = remember(history) {
        history.mapNotNull { s -> s.weeklyPct?.let { s.timestamp to it } }
    }
    val balancePoints = remember(history) {
        history.mapNotNull { s -> s.balanceTotal?.let { s.timestamp to it.toFloat() } }
    }

    if (fiveHourPoints.isNotEmpty()) {
        DetailChartPanel(key = "detail-chart-5h") {
            HistoryChart(points = fiveHourPoints)
            ChartLegend(
                color = tierColor(50f),
                label = stringResource(R.string.legend_five_hour),
            )
        }
    }
    if (weeklyPoints.isNotEmpty()) {
        DetailChartPanel(key = "detail-chart-weekly") {
            HistoryChart(points = weeklyPoints)
            ChartLegend(
                color = tierColor(50f),
                label = stringResource(R.string.legend_weekly),
            )
        }
    }
    if (balancePoints.isNotEmpty()) {
        val currency = history.lastOrNull { it.balanceCurrency != null }?.balanceCurrency
        DetailChartPanel(key = "detail-chart-balance") {
            HistoryChart(
                points = balancePoints,
                yMax = niceCeil(balancePoints.maxOf { it.second }),
                formatY = { Fmt.moneyAxis(currency, it.toDouble()) },
                tooltip = { t, v -> "${Fmt.money(currency, v.toDouble())} · ${Fmt.formatAbsolute(t)}" },
            )
            ChartLegend(
                color = tierColor(50f),
                label = stringResource(R.string.balance_history_title),
            )
        }
    }
    if (fiveHourPoints.isEmpty() && weeklyPoints.isEmpty() && balancePoints.isEmpty()) {
        Text(
            stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 详情页图表块:经典模式保持裸布局,玻璃模式为不透明纸面卡 */
@Composable
private fun DetailChartPanel(key: String, content: @Composable () -> Unit) {
    GlassPanel(
        key = key,
        config = GlassConfig.card(),
        modifier = Modifier.fillMaxWidth(),
        classic = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() } },
        glass = {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        },
    )
}
