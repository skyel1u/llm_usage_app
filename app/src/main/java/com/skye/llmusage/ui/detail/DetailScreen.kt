package com.skye.llmusage.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skye.llmusage.R
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.data.db.SnapshotEntity
import com.skye.llmusage.ui.ChartLegend
import com.skye.llmusage.ui.HistoryChart
import com.skye.llmusage.ui.RefreshIcon
import com.skye.llmusage.ui.theme.tierColor

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !refreshing) {
                        RefreshIcon(spinning = refreshing)
                    }
                    IconButton(onClick = { account?.let { onEdit(it.id) } }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_account))
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete_account))
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),) {

            val fiveHourPoints = remember(history) {
                history.mapNotNull { s -> s.fiveHourPct?.let { s.timestamp to it } }
            }
            val weeklyPoints = remember(history) {
                history.mapNotNull { s -> s.weeklyPct?.let { s.timestamp to it } }
            }

            if (fiveHourPoints.isNotEmpty()) {
                HistoryChart(points = fiveHourPoints)
                ChartLegend(
                    color = tierColor(50f),
                    label = stringResource(R.string.legend_five_hour),
                )
            }
            if (weeklyPoints.isNotEmpty()) {
                HistoryChart(points = weeklyPoints)
                ChartLegend(
                    color = tierColor(50f),
                    label = stringResource(R.string.legend_weekly),
                )
            }
            if (fiveHourPoints.isEmpty() && weeklyPoints.isEmpty()) {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDelete) {
        AlertDialog(
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
