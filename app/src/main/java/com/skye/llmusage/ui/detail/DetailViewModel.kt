package com.skye.llmusage.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skye.llmusage.LlmUsageApp
import com.skye.llmusage.data.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: UsageRepository = (app as LlmUsageApp).repository

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun account(id: Long) = repo.observeAccount(id)
    fun history(id: Long) = repo.observeHistory(id)

    fun refresh(id: Long) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            repo.refresh(id, force = true)
            _refreshing.value = false
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteAccount(id)
            onDone()
        }
    }
}
