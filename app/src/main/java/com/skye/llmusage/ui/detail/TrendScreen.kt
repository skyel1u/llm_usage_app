package com.skye.llmusage.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skye.llmusage.R
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.data.db.SnapshotEntity
import com.skye.llmusage.ui.ChartLegend
import com.skye.llmusage.ui.HistoryChart
import com.skye.llmusage.ui.theme.tierColor
import com.skye.llmusage.util.Fmt

/** 趋势 Tab:账户切换 Chip + 用量历史曲线(5 小时% / 周%)与余额 */
@Composable
fun TrendScreen(
    accounts: List<AccountUi>,
    selectedAccountId: Long?,
    history: List<SnapshotEntity>,
    onSelectAccount: (Long) -> Unit,
    onRefresh: (Long) -> Unit,
    onEdit: (Long) -> Unit,
) {
    if (accounts.isEmpty()) {
        EmptyTrendState()
        return
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            items(accounts, key = { it.id }) { account ->
                FilterChip(
                    selected = account.id == selectedAccountId,
                    onClick = { onSelectAccount(account.id) },
                    label = { Text(account.name) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            accounts.firstOrNull { it.id == selectedAccountId }?.provider?.displayName ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRefresh(selectedAccountId ?: return@IconButton) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    IconButton(onClick = { selectedAccountId?.let(onEdit) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_account))
                    }
                }
            }

            if (history.any { it.fiveHourPct != null }) {
                item {
                    TrendCard(title = stringResource(R.string.five_hour_label)) {
                        HistoryChart(points = history.mapNotNull { s ->
                            s.fiveHourPct?.let { s.timestamp to it }
                        })
                        ChartLegend(
                            color = tierColor(50f),
                            label = stringResource(R.string.legend_five_hour),
                        )
                    }
                }
            }
            if (history.any { it.weeklyPct != null }) {
                item {
                    TrendCard(title = stringResource(R.string.weekly_label)) {
                        HistoryChart(points = history.mapNotNull { s ->
                            s.weeklyPct?.let { s.timestamp to it }
                        })
                        ChartLegend(
                            color = tierColor(50f),
                            label = stringResource(R.string.legend_weekly),
                        )
                    }
                }
            }
            if (history.any { it.balanceTotal != null }) {
                item {
                    TrendCard(title = stringResource(R.string.balance_history_title)) {
                        history.lastOrNull { it.balanceTotal != null }?.let {
                            Text(
                                Fmt.money(it.balanceCurrency, it.balanceTotal ?: 0.0),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            stringResource(R.string.snapshot_count_fmt, history.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (history.none { it.fiveHourPct != null || it.weeklyPct != null || it.balanceTotal != null }) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun EmptyTrendState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.trend_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.trend_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
