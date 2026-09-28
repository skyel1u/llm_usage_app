package com.skye.llmusage.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skye.llmusage.LlmUsageApp
import com.skye.llmusage.data.Provider
import com.skye.llmusage.data.UsageRepository
import com.skye.llmusage.data.db.AccountEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: UsageRepository = (app as LlmUsageApp).repository

    /** 查重命中时,与之冲突的既有账户名;null 表示无冲突 */
    private val _duplicateName = MutableStateFlow<String?>(null)
    val duplicateName: StateFlow<String?> = _duplicateName.asStateFlow()

    private class PendingSave(
        val id: Long?,
        val name: String,
        val provider: Provider,
        val apiKey: String,
        val onDone: (Long) -> Unit,
    )

    private var pending: PendingSave? = null

    fun save(
        id: Long?,
        name: String,
        provider: Provider,
        apiKey: String,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val duplicate = repo.findDuplicateAccount(provider.name, apiKey, id ?: 0L)
            if (duplicate == null) {
                doSave(id, name, provider, apiKey, onDone)
            } else {
                pending = PendingSave(id, name, provider, apiKey, onDone)
                _duplicateName.value = duplicate.name
            }
        }
    }

    fun dismissDuplicate() {
        _duplicateName.value = null
        pending = null
    }

    /** 查重提示后仍确认保存 */
    fun confirmDuplicate() {
        val p = pending ?: return
        dismissDuplicate()
        viewModelScope.launch { doSave(p.id, p.name, p.provider, p.apiKey, p.onDone) }
    }

    private suspend fun doSave(
        id: Long?,
        name: String,
        provider: Provider,
        apiKey: String,
        onDone: (Long) -> Unit,
    ) {
        // 编辑时保留原 createdAt,避免账户被挪到列表末尾(主页按 createdAt 排序)
        val existing = id?.let { repo.getAccount(it) }
        val accountId = repo.upsertAccount(
            AccountEntity(
                id = id ?: 0,
                name = name.ifBlank { provider.displayName },
                type = provider.type.name,
                provider = provider.name,
                apiKey = apiKey,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            ),
        )
        repo.refresh(accountId, force = true)
        onDone(accountId)
    }
}
