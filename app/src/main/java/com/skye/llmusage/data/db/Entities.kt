package com.skye.llmusage.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // AccountType.name
    val provider: String, // Provider.name
    val apiKey: String,
    val createdAt: Long,
    val lastRefreshMs: Long? = null,
    val lastError: String? = null,
)

/** 每次成功刷新落一条快照,既是展示数据也是历史曲线数据源 */
@Entity(
    tableName = "snapshots",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val timestamp: Long,
    // Coding Plan
    val level: String? = null,
    val fiveHourPct: Float? = null,
    val fiveHourResetMs: Long? = null,
    val weeklyPct: Float? = null,
    val weeklyResetMs: Long? = null,
    // PAYG
    val balanceTotal: Double? = null,
    val balanceGranted: Double? = null,
    val balanceToppedUp: Double? = null,
    val balanceCurrency: String? = null,
    val available: Boolean? = null,
)
