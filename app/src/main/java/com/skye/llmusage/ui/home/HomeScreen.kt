package com.skye.llmusage.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import com.skye.llmusage.R
import com.skye.llmusage.data.AccountType
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.ui.FiveHourRing
import com.skye.llmusage.ui.WeeklyBar
import com.skye.llmusage.util.Fmt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.refreshing) {
                        RefreshIcon(spinning = state.refreshing)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (state.accounts.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddAccount,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_account)) },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.accounts.isEmpty()) {
                EmptyState(onAddAccount)
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.failedCount > 0) {
                        item {
                            RefreshFailedNotice(count = state.failedCount)
                        }
                    }
                    items(state.accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            onClick = { onOpenAccount(account.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshIcon(spinning: Boolean) {
    val angle by animateFloatAsState(
        targetValue = if (spinning) 360f else 0f,
        animationSpec = tween(900),
        label = "refreshAngle",
    )
    Icon(
        Icons.Rounded.Refresh,
        contentDescription = stringResource(R.string.action_refresh),
        modifier = Modifier.graphicsLayer { rotationZ = angle },
    )
}


@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.Bolt,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.add_account)) },
        )
    }
}

@Composable
private fun RefreshFailedNotice(count: Int) {
    AssistChip(
        onClick = {},
        label = { Text(stringResource(R.string.refresh_failed_fmt, count)) },
        leadingIcon = {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, Modifier.size(18.dp))
        },
    )
}

@Composable
fun AccountCard(account: AccountUi, onClick: () -> Unit) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(account.id) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // 头部:名称 + 类型徽章
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (icon, label) = when (account.type) {
                    AccountType.CODING_PLAN -> Icons.Rounded.Bolt to "Coding Plan"
                    AccountType.PAYG -> Icons.Rounded.Savings to "PAYG"
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(onClick = onClick, label = { Text(label) })
            }

            account.lastError?.let { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            when (account.type) {
                AccountType.CODING_PLAN -> {
                    account.level?.let {
                        Text(
                            "${account.provider.displayName} $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val fiveHour = account.fiveHour
                    val weekly = account.weekly
                    if (fiveHour != null) FiveHourRing(fiveHour)
                    if (weekly != null) WeeklyBar(weekly)
                    if (fiveHour == null && weekly == null && account.lastError == null) {
                        Text(
                            "暂无用量数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AccountType.PAYG -> {
                    val balance = account.balance
                    if (balance != null) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                Fmt.money(balance.currency, balance.total),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (balance.available) "可用" else "不可用",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (balance.available) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Text(
                            "赠送 ${Fmt.money(balance.currency, balance.granted)} · 充值 ${Fmt.money(balance.currency, balance.toppedUp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (account.lastError == null) {
                        Text(
                            "暂无余额数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            account.lastRefreshMs?.let {
                Text(
                    "${Fmt.relativeTime(nowMs, it)}更新",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } ?: Text(
                "尚未刷新",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
