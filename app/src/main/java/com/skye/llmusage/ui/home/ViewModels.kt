package com.skye.llmusage.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skye.llmusage.LlmUsageApp
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.data.UsageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val accounts: List<AccountUi> = emptyList(),
    val refreshing: Boolean = false,
    val failedCount: Int = 0,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: UsageRepository = (app as LlmUsageApp).repository

    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<HomeUiState> =
        combine(repo.observeAccounts(), refreshing) { accounts, isRefreshing ->
            HomeUiState(
                accounts = accounts,
                refreshing = isRefreshing,
                failedCount = accounts.count { it.lastError != null },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            refreshing.value = true
            // join:等 refreshAll(含全部子任务)真正结束再收起指示器,
            // 否则按钮立刻重新启用,可重复触发并发刷新
            repo.refreshAll(force = false).join()
            refreshing.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            repo.refreshAll(force = true).join()
            refreshing.value = false
        }
    }
}

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: UsageRepository = (app as LlmUsageApp).repository

    fun save(
        id: Long?,
        name: String,
        provider: com.skye.llmusage.data.Provider,
        apiKey: String,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val accountId = repo.upsertAccount(
                com.skye.llmusage.data.db.AccountEntity(
                    id = id ?: 0,
                    name = name.ifBlank { provider.displayName },
                    type = provider.type.name,
                    provider = provider.name,
                    apiKey = apiKey,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            repo.refresh(accountId, force = true)
            onDone(accountId)
        }
    }
}

class DetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: UsageRepository = (app as LlmUsageApp).repository

    fun account(id: Long) = repo.observeAccount(id)
    fun history(id: Long) = repo.observeHistory(id)

    fun refresh(id: Long) {
        viewModelScope.launch { repo.refresh(id, force = true) }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteAccount(id)
            onDone()
        }
    }
}

/**
 * 趋势 Tab:全量账户 + 选中账户历史。
 * 选中态与历史收集都收敛到 VM:UI 重组不再新建 Room Flow 重跑查询,
 * 也避免在组合期写状态(副作用)。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TrendViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: UsageRepository = (app as LlmUsageApp).repository

    val accounts: StateFlow<List<AccountUi>> =
        repo.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val manualSelection = MutableStateFlow<Long?>(null)

    /** 手动选择优先;选中账户被删/列表变化失效时回退首个账户 */
    val selectedAccountId: StateFlow<Long?> =
        combine(accounts, manualSelection) { list, sel ->
            sel?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** flatMapLatest:切换账户只保留最新查询,旧查询立即取消 */
    val history: StateFlow<List<com.skye.llmusage.data.db.SnapshotEntity>> =
        selectedAccountId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.observeHistory(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(id: Long) {
        manualSelection.value = id
    }

    fun refresh(id: Long) {
        viewModelScope.launch { repo.refresh(id, force = true) }
    }
}
