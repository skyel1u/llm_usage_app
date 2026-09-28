package com.skye.llmusage.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observe(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: Long): AccountEntity?

    @Upsert
    suspend fun upsert(account: AccountEntity): Long

    @Query("UPDATE accounts SET lastRefreshMs = :ts, lastError = NULL WHERE id = :id")
    suspend fun markRefreshed(id: Long, ts: Long)

    @Query("UPDATE accounts SET lastError = :error WHERE id = :id")
    suspend fun markError(id: Long, error: String)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    /** 同提供商 + 同 Key 的既有账户(排除自身),用于保存前查重 */
    @Query("SELECT * FROM accounts WHERE provider = :provider AND apiKey = :apiKey AND id != :excludeId LIMIT 1")
    suspend fun findByKey(provider: String, apiKey: String, excludeId: Long): AccountEntity?
}

@Dao
interface SnapshotDao {
    /** 每个账户最新一条快照 */
    @Query("SELECT * FROM snapshots WHERE id IN (SELECT MAX(id) FROM snapshots GROUP BY accountId)")
    fun observeLatest(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE accountId = :accountId ORDER BY timestamp")
    fun observeFor(accountId: Long): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots ORDER BY timestamp")
    fun observeAll(): Flow<List<SnapshotEntity>>

    @Query("DELETE FROM snapshots")
    suspend fun clearAll()

    /** 每账户只保留最近 [limit] 条快照(按 id 降序保留) */
    @Query(
        "DELETE FROM snapshots WHERE id IN (" +
            "SELECT id FROM snapshots WHERE accountId = :accountId " +
            "ORDER BY id DESC LIMIT -1 OFFSET :limit" +
            ")",
    )
    suspend fun trim(accountId: Long, limit: Int)

    @Insert
    suspend fun insert(snapshot: SnapshotEntity)
}
