package com.skye.llmusage.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skye.llmusage.LlmUsageApp
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.data.UsageRepository
import com.skye.llmusage.data.db.SnapshotEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** 手动选择优先;选中账户被删/列表变化失效时回退首个账户 */
    val selectedAccountId: StateFlow<Long?> =
        combine(accounts, manualSelection) { list, sel ->
            sel?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** flatMapLatest:切换账户只保留最新查询,旧查询立即取消 */
    val history: StateFlow<List<SnapshotEntity>> =
        selectedAccountId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.observeHistory(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(id: Long) {
        manualSelection.value = id
    }

    fun refresh(id: Long) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            repo.refresh(id, force = true)
            _refreshing.value = false
        }
    }
}
