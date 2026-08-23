package com.skye.llmusage

import android.app.Application
import com.skye.llmusage.data.SettingsRepository
import com.skye.llmusage.data.UsageRepository
import com.skye.llmusage.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LlmUsageApp : Application() {
    lateinit var repository: UsageRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.build(this)
        repository = UsageRepository(db.accountDao(), db.snapshotDao(), appScope)
        settingsRepository = SettingsRepository(this)
    }
}
