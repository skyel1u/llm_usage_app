package com.skye.llmusage.data.api

import com.skye.llmusage.data.CodingPlanUsage
import com.skye.llmusage.data.Provider
import com.skye.llmusage.data.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * GLM Coding Plan 配额查询。
 * 端点规格参考 cc-switch(farion1231/cc-switch)coding_plan.rs:
 * GET {host}/api/monitor/usage/quota/limit,Authorization 为裸 key(无 Bearer 前缀)。
 */
object GlmApi {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class LimitItem(
        val type: String? = null,
        val percentage: Double? = null,
        @SerialName("nextResetTime") val nextResetTime: Long? = null,
        val unit: Int? = null,
    )

    @Serializable
    data class QuotaData(
        val level: String? = null,
        val limits: List<LimitItem> = emptyList(),
    )

    @Serializable
    data class QuotaResponse(
        val success: Boolean? = null,
        val msg: String? = null,
        val data: QuotaData? = null,
    )

    suspend fun fetch(client: OkHttpClient, provider: Provider, apiKey: String): CodingPlanUsage =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(provider.baseUrl + "/api/monitor/usage/quota/limit")
                .header("Authorization", apiKey) // 智谱特殊:不带 Bearer
                .header("Accept-Language", "en-US,en")
                .build()

            val body = client.newCall(request).await().use { resp ->
                when (resp.code) {
                    401, 403 -> throw UsageException("认证失败 (HTTP ${resp.code}),请检查 API Key")
                }
                if (!resp.isSuccessful) throw UsageException("HTTP ${resp.code}")
                resp.body!!.string()
            }

            parseResponse(body)
        }

    /** 解析响应体:校验 success/data 后交给 [parse];坏响应抛 UsageException */
    internal fun parseResponse(body: String): CodingPlanUsage {
        val parsed = runCatching { json.decodeFromString<QuotaResponse>(body) }
            .getOrElse { throw UsageException("响应解析失败", it) }
        if (parsed.success == false) throw UsageException(parsed.msg ?: "服务返回错误")
        val data = parsed.data ?: throw UsageException("响应缺少 data 字段")
        return parse(data)
        }

    /** unit=3 → 5 小时限制,unit=6 → 周限制;unit 缺失时按序填入空缺槽位(兜底) */
    fun parse(data: QuotaData): CodingPlanUsage {
        // 智谱已切换积分制,现网返回 CREDIT_LIMIT;TOKENS_LIMIT 为旧字段,两者都认
        val tokens = data.limits.filter {
            it.type?.uppercase() in setOf("TOKENS_LIMIT", "CREDIT_LIMIT")
        }
        val toTier: (LimitItem) -> Tier = { item ->
            Tier(
                pct = (item.percentage ?: 0.0).toFloat(),
                resetMs = item.nextResetTime?.takeIf { it > 0 },
            )
        }

        var fiveHour = tokens.firstOrNull { it.unit == 3 }?.let(toTier)
        var weekly = tokens.firstOrNull { it.unit == 6 }?.let(toTier)

        for (item in tokens) {
            val u = item.unit
            if (u == 3 || u == 6) continue
            val tier = toTier(item)
            if (fiveHour == null) {
                fiveHour = tier
            } else if (weekly == null) {
                weekly = tier
            }
        }
        return CodingPlanUsage(level = data.level, fiveHour = fiveHour, weekly = weekly)
    }
}
