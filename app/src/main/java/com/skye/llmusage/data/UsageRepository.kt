package com.skye.llmusage.data

import com.skye.llmusage.data.api.DeepSeekApi
import com.skye.llmusage.data.api.GlmApi
import com.skye.llmusage.data.api.KimiCodingApi
import com.skye.llmusage.data.api.MiniMaxApi
import com.skye.llmusage.data.api.UsageException
import com.skye.llmusage.data.db.AccountDao
import com.skye.llmusage.data.db.AccountEntity
import com.skye.llmusage.data.db.SnapshotDao
import com.skye.llmusage.data.db.SnapshotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.joinAll
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** 每账户保留的快照上限:5 分钟自动刷新 → 7 天约 2000 条,封顶防无限增长 */
private const val SNAPSHOT_LIMIT = 2000

/** 刷新节流:成功数据 3 分钟内不重复请求 */
private const val FRESH_MS = 3 * 60_000L

class UsageRepository(
    private val accountDao: AccountDao,
    private val snapshotDao: SnapshotDao,
    private val scope: CoroutineScope,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun observeAccounts(): Flow<List<AccountUi>> =
        combine(accountDao.observeAll(), snapshotDao.observeLatest()) { accounts, latest ->
            val byAccount = latest.associateBy { it.accountId }
            accounts.map { it.toUi(byAccount[it.id]) }
        }

    fun observeAccount(id: Long): Flow<AccountUi?> =
        combine(accountDao.observe(id), snapshotDao.observeLatest()) { account, latest ->
            account?.toUi(latest.firstOrNull { it.id == account.id })
        }

    fun observeHistory(accountId: Long): Flow<List<SnapshotEntity>> =
        snapshotDao.observeFor(accountId)

    suspend fun upsertAccount(entity: AccountEntity): Long = accountDao.upsert(entity)

    suspend fun getAccount(id: Long): AccountEntity? = accountDao.get(id)

    suspend fun deleteAccount(id: Long) = accountDao.delete(id)

    /** 保存前查重:同提供商 + 同 Key 的既有账户(排除自身);null 表示无冲突 */
    suspend fun findDuplicateAccount(provider: String, apiKey: String, excludeId: Long): AccountEntity? =
        accountDao.findByKey(provider, apiKey, excludeId)

    /**
     * 刷新一个账户:请求 → 写快照 → 更新账户状态。
     * force=false 时,若上次成功刷新在 FRESH_MS 内则跳过。
     */
    suspend fun refresh(accountId: Long, force: Boolean = true) {
        val account = accountDao.get(accountId) ?: return
        if (!force) {
            account.lastRefreshMs?.let { last ->
                if (System.currentTimeMillis() - last < FRESH_MS) return
            }
        }
        try {
            val provider = Provider.of(account.provider)
            val snapshot = when (provider) {
                Provider.DEEPSEEK -> {
                    val balance = DeepSeekApi.fetch(client, account.apiKey)
                    SnapshotEntity(
                        accountId = account.id,
                        timestamp = System.currentTimeMillis(),
                        balanceTotal = balance.total,
                        balanceGranted = balance.granted,
                        balanceToppedUp = balance.toppedUp,
                        balanceCurrency = balance.currency,
                        available = balance.available,
                    )
                }
                Provider.GLM_CN, Provider.GLM_INTL -> {
                    val usage = GlmApi.fetch(client, provider, account.apiKey)
                    usage.toSnapshot(account.id)
                }
                Provider.KIMI_CODING -> KimiCodingApi.fetch(client, account.apiKey).toSnapshot(account.id)
                Provider.MINIMAX_CN, Provider.MINIMAX_INTL -> {
                    MiniMaxApi.fetch(client, provider, account.apiKey).toSnapshot(account.id)
                }
            }
            snapshotDao.insert(snapshot)
            snapshotDao.trim(account.id, SNAPSHOT_LIMIT)
            accountDao.markRefreshed(account.id, snapshot.timestamp)
        } catch (e: UsageException) {
            accountDao.markError(account.id, e.message ?: "未知错误")
        } catch (e: Exception) {
            accountDao.markError(account.id, "未知错误: ${e.message}")
        }
    }

    /** 设置页:清空全部历史快照(保留账户) */
    suspend fun clearHistory() = snapshotDao.clearAll()

    /** 主页整体刷新:并发拉取全部账户(force=false 时仅刷新过期账户) */
    fun refreshAll(force: Boolean) = scope.launch {
        val accounts = accountDao.observeAll().first()
        val gate = Semaphore(4)
        accounts.map { account ->
            scope.launch { gate.withPermit { refresh(account.id, force) } }
        }.joinAll()
    }
}

private fun CodingPlanUsage.toSnapshot(accountId: Long) = SnapshotEntity(
    accountId = accountId,
    timestamp = System.currentTimeMillis(),
    level = level,
    fiveHourPct = fiveHour?.pct,
    fiveHourResetMs = fiveHour?.resetMs,
    weeklyPct = weekly?.pct,
    weeklyResetMs = weekly?.resetMs,
)

private fun AccountEntity.toUi(latest: SnapshotEntity?): AccountUi = AccountUi(
    id = id,
    name = name,
    type = AccountType.of(type),
    provider = Provider.of(provider),
    lastRefreshMs = latest?.timestamp ?: lastRefreshMs,
    lastError = lastError,
    level = latest?.level,
    fiveHour = latest?.fiveHourPct?.let { Tier(it, latest.fiveHourResetMs) },
    weekly = latest?.weeklyPct?.let { Tier(it, latest.weeklyResetMs) },
    balance = latest?.balanceTotal?.let {
        Balance(
            currency = latest.balanceCurrency,
            total = it,
            granted = latest.balanceGranted ?: 0.0,
            toppedUp = latest.balanceToppedUp ?: 0.0,
            available = latest.available ?: true,
        )
    },
)
