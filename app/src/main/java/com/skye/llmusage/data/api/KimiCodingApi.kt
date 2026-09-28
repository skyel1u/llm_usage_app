package com.skye.llmusage.data.api

import com.skye.llmusage.data.CodingPlanUsage
import com.skye.llmusage.data.Tier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import java.time.Instant

/**
 * Kimi For Coding 用量查询。
 * 端点参考 cc-switch(farion1231/cc-switch)coding_plan.rs:
 * GET https://api.kimi.com/coding/v1/usages,Bearer 认证。
 * limits[].detail → 5 小时窗口,usage → 周限额;均为 limit/remaining 配额制。
 */
object KimiCodingApi {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class UsageDetail(
        val limit: JsonPrimitive? = null,
        val remaining: JsonPrimitive? = null,
        val resetTime: JsonPrimitive? = null,
    )

    @Serializable
    data class LimitItem(val detail: UsageDetail? = null)

    @Serializable
    data class UsageResponse(
        val limits: List<LimitItem> = emptyList(),
        val usage: UsageDetail? = null,
    )

    /** JsonPrimitive 宽松转 Double:兼容数字与数字字符串 */
    private fun JsonPrimitive.doubleOrNullOr(): Double? = content.toDoubleOrNull()

    suspend fun fetch(client: OkHttpClient, apiKey: String): CodingPlanUsage =
        parseResponse(
            httpGet(
                client,
                "https://api.kimi.com/coding/v1/usages",
                "Bearer $apiKey",
                mapOf("Accept" to "application/json"),
            ),
        )

    internal fun parseResponse(body: String): CodingPlanUsage {
        val parsed = runCatching { json.decodeFromString<UsageResponse>(body) }
            .getOrElse { throw UsageException("响应解析失败", it) }
        return parse(parsed)
    }

    fun parse(resp: UsageResponse): CodingPlanUsage {
        val fiveHour = resp.limits.firstNotNullOfOrNull { it.detail }?.let(::toTier)
        val weekly = resp.usage?.let(::toTier)
        return CodingPlanUsage(level = null, fiveHour = fiveHour, weekly = weekly)
    }

    /** (limit - remaining) / limit → 已用百分比;remaining 异常偏高时钳为 0 */
    private fun toTier(detail: UsageDetail): Tier? {
        val limit = detail.limit?.doubleOrNullOr() ?: return null
        val remaining = detail.remaining?.doubleOrNullOr() ?: 0.0
        if (limit <= 0.0) return null
        val pct = (limit - remaining).coerceAtLeast(0.0) / limit * 100.0
        return Tier(pct.toFloat(), resetMs = resetMs(detail.resetTime))
    }

    /** resetTime 兼容三种形态:秒级数字、毫秒级数字、ISO 8601 字符串 */
    internal fun resetMs(value: JsonPrimitive?): Long? {
        val primitive = value ?: return null
        primitive.content.toLongOrNull()?.let { n ->
            if (n <= 0) return null
            // 秒级时间戳 < 1e12,毫秒 >= 1e12
            return if (n < 1_000_000_000_000L) n * 1000 else n
        }
        return runCatching { Instant.parse(primitive.content).toEpochMilli() }
            .getOrNull()
            ?.takeIf { it > 0 }
    }
}
