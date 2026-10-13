package com.skye.llmusage.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.skye.llmusage.R
import com.skye.llmusage.data.AccountType
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.ui.FiveHourRing
import com.skye.llmusage.ui.RefreshIcon
import com.skye.llmusage.ui.WeeklyBar
import com.skye.llmusage.ui.glass.GlassConfig
import com.skye.llmusage.ui.glass.GlassMaterial
import com.skye.llmusage.ui.glass.GlassPanel
import com.skye.llmusage.ui.glass.LocalChromeLayer
import com.skye.llmusage.ui.glass.LocalGlassChromeInset
import com.skye.llmusage.ui.glass.LocalGlassController
import com.skye.llmusage.ui.glass.pressableGlass
import com.skye.llmusage.ui.theme.LocalGlassEnabled
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
    if (LocalGlassEnabled.current && LocalGlassController.current != null) {
        LiquidHomeScreen(state, onRefresh, onOpenAccount, onAddAccount)
    } else {
        ClassicHomeScreen(state, onRefresh, onOpenAccount, onAddAccount)
    }
}

/**
 * 玻璃模式主页(Apple 双层结构):内容层全屏滚动,从顶部导航栏与底部
 * Tab 胶囊下方穿过;导航栏静止时透明,内容滚入后玻璃渐显。栏的前景文字
 * 图标由 chrome 层渲染(位于玻璃之上,永不参与模糊)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiquidHomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val barAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(240),
        label = "homeBar",
    )
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chromeInset = LocalGlassChromeInset.current
    val chrome = LocalChromeLayer.current

    SideEffect {
        chrome?.topBar?.value = { HomeTopBar(onRefresh, state.refreshing, Color.Transparent) }
        chrome?.topBarAlpha?.floatValue = barAlpha
        chrome?.fab?.value = { GlassFabPanel(onAddAccount) }
    }
    DisposableEffect(chrome) {
        onDispose {
            chrome?.topBar?.value = null
            chrome?.fab?.value = null
        }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Crossfade(
            targetState = state.accounts.isEmpty(),
            animationSpec = tween(300),
            label = "homeContent",
        ) { empty ->
            if (empty) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = statusBarTop + 64.dp),
                ) {
                    EmptyState(onAddAccount, showFab = false)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = statusBarTop + 80.dp,
                        bottom = chromeInset + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.failedCount > 0) {
                        item {
                            RefreshFailedNotice(
                                count = state.failedCount,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    items(state.accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            onClick = { onOpenAccount(account.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeTopBar(onRefresh: () -> Unit, refreshing: Boolean, container: Color) {
    TopAppBar(
        title = { Text(stringResource(R.string.home_title)) },
        actions = {
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                RefreshIcon(spinning = refreshing)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = container),
    )
}

/** 经典模式主页:与既有 Scaffold 布局完全一致 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassicHomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
) {
    Scaffold(
        topBar = { HomeTopBar(onRefresh, state.refreshing, MaterialTheme.colorScheme.background) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.accounts.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                ClassicFab(onClick = onAddAccount)
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
            // 滚动状态提到 Crossfade 外:空态 ↔ 列表切换后仍保留滚动位置
            val listState = rememberLazyListState()
            Crossfade(
                targetState = state.accounts.isEmpty(),
                animationSpec = tween(300),
                label = "homeContent",
            ) { empty ->
                if (empty) {
                    EmptyState(onAddAccount)
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.failedCount > 0) {
                            item {
                                RefreshFailedNotice(count = state.failedCount, modifier = Modifier.animateItem())
                            }
                        }
                        items(state.accounts, key = { it.id }) { account ->
                            AccountCard(
                                account = account,
                                onClick = { onOpenAccount(account.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, showFab: Boolean = true) {
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
        if (showFab) ClassicFab(onClick = onAdd)
    }
}

/** 经典模式添加账户 FAB(M3 ExtendedFAB) */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ClassicFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.add_account)) },
    )
}

/** 玻璃模式添加账户 FAB:胶囊玻璃 + 按压回弹,由 chrome 层渲染 */
@Composable
private fun GlassFabPanel(onClick: () -> Unit) {
    GlassPanel(
        key = "home-fab",
        config = GlassConfig.capsule(),
        material = GlassMaterial.CHROME,
        classic = {},
        glass = {
            Row(
                modifier = Modifier
                    .pressableGlass(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.add_account),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}

@Composable
private fun RefreshFailedNotice(count: Int, modifier: Modifier = Modifier) {
    val row: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.refresh_failed_fmt, count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
    GlassPanel(
        key = "refresh-failed",
        modifier = modifier,
        config = GlassConfig.notice(),
        classic = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
            ) { row() }
        },
        glass = {
            // 纸面卡片自带暖红染色,文字直接用对比色
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.refresh_failed_fmt, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
fun AccountCard(account: AccountUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(account.id) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }
    // 刷新后错误提示/数据块的增删会引起高度变化,用尺寸动画避免生硬跳变
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 300f)),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 头部:名称 + 类型徽章;玻璃模式下徽章圆角与卡片曲率同心
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
                Surface(
                    shape = if (LocalGlassEnabled.current) RoundedCornerShape(9.dp)
                    else MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = account.lastError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    account.lastError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            when (account.type) {
                AccountType.CODING_PLAN -> {
                    // 始终显示提供商(Kimi/MiniMax 无套餐等级),有等级时追加
                    Text(
                        account.level?.let { "${account.provider.displayName} $it" }
                            ?: account.provider.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

    GlassPanel(
        key = "account-${account.id}",
        modifier = modifier,
        config = GlassConfig.card(),
        classic = {
            Card(
                onClick = onClick,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) { body() }
        },
        glass = {
            // 面板提供纸面背景与裁剪,此处仅承接点击涟漪
            Box(Modifier.clickable(onClick = onClick)) { body() }
        },
    )
}
