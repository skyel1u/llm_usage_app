package com.skye.llmusage.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skye.llmusage.LlmUsageApp
import com.skye.llmusage.data.AccountUi
import com.skye.llmusage.data.UsageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    private val refreshing = kotlinx.coroutines.flow.MutableStateFlow(false)

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
            repo.refreshAll(force = false)
            refreshing.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            repo.refreshAll(force = true)
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

/** 趋势 Tab:全量账户 + 当前选中账户的历史;选中 ID 由 UI 层持有(saveable) */
class TrendViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: UsageRepository = (app as LlmUsageApp).repository

    val accounts: StateFlow<List<AccountUi>> =
        repo.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun history(id: Long) = repo.observeHistory(id)

    fun refresh(id: Long) {
        viewModelScope.launch { repo.refresh(id, force = true) }
    }
}
