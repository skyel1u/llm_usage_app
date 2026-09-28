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
        // 打开应用自动刷新过期数据(force=false:3 分钟内成功过的账户直接跳过);
        // 静默进行,不驱动刷新指示器,不打扰手动刷新
        viewModelScope.launch { repo.refreshAll(force = false).join() }
    }

    /**
     * 手动刷新(顶栏按钮 / 下拉)。
     * 重入保护:按钮 disabled 只挡住按钮,下拉刷新仍可能再次触发。
     */
    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            // join:等 refreshAll(含全部子任务)真正结束再收起指示器
            repo.refreshAll(force = true).join()
            refreshing.value = false
        }
    }
}
