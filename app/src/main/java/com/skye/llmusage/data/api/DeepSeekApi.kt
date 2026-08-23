package com.skye.llmusage.data.api

import com.skye.llmusage.data.Balance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** DeepSeek PAYG 余额查询:GET /user/balance (Bearer)。 */
object DeepSeekApi {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class BalanceInfo(
        val currency: String? = null,
        @SerialName("total_balance") val totalBalance: String? = null,
        @SerialName("granted_balance") val grantedBalance: String? = null,
        @SerialName("topped_up_balance") val toppedUpBalance: String? = null,
    )

    @Serializable
    data class BalanceResponse(
        @SerialName("is_available") val isAvailable: Boolean? = null,
        @SerialName("balance_infos") val balanceInfos: List<BalanceInfo> = emptyList(),
    )

    suspend fun fetch(client: OkHttpClient, apiKey: String): Balance = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.deepseek.com/user/balance")
            .header("Authorization", "Bearer $apiKey")
            .build()

        val body = client.newCall(request).await().use { resp ->
            when (resp.code) {
                401, 403 -> throw UsageException("认证失败 (HTTP ${resp.code}),请检查 API Key")
            }
            if (!resp.isSuccessful) throw UsageException("HTTP ${resp.code}")
            resp.body!!.string()
        }

        val parsed = runCatching { json.decodeFromString<BalanceResponse>(body) }
            .getOrElse { throw UsageException("响应解析失败", it) }
        parse(parsed)
    }

    /** 优先取 CNY 条目,否则第一条;总额解析失败视为无数据 */
    fun parse(resp: BalanceResponse): Balance {
        val info = resp.balanceInfos.firstOrNull { it.currency.equals("CNY", ignoreCase = true) }
            ?: resp.balanceInfos.firstOrNull()
            ?: throw UsageException("响应中没有余额数据")
        val total = info.totalBalance?.toDoubleOrNull()
            ?: throw UsageException("余额数据无法解析")
        return Balance(
            currency = info.currency,
            total = total,
            granted = info.grantedBalance?.toDoubleOrNull() ?: 0.0,
            toppedUp = info.toppedUpBalance?.toDoubleOrNull() ?: 0.0,
            available = resp.isAvailable ?: true,
        )
    }
}
