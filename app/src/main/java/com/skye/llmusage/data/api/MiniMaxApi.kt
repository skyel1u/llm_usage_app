package com.skye.llmusage.data.api

import com.skye.llmusage.data.CodingPlanUsage
import com.skye.llmusage.data.Provider
import com.skye.llmusage.data.Tier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/**
 * MiniMax Coding Plan 用量查询。
 * 端点参考 cc-switch(farion1231/cc-switch)coding_plan.rs:
 * GET {api.minimaxi.com | api.minimax.io}/v1/api/openplatform/coding_plan/remains,Bearer 认证。
 * model_remains 中 model_name="general" 为编程套餐桶:接口给"剩余百分比",反转为已用。
 */
object MiniMaxApi {
    private const val PATH = "/v1/api/openplatform/coding_plan/remains"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class BaseResp(
        @SerialName("status_code") val statusCode: Int? = null,
        @SerialName("status_msg") val statusMsg: String? = null,
    )

    @Serializable
    data class ModelRemain(
        @SerialName("model_name") val modelName: String? = null,
        @SerialName("current_interval_remaining_percent") val intervalRemainPct: Double? = null,
        @SerialName("end_time") val endTime: Long? = null,
        @SerialName("current_weekly_status") val weeklyStatus: Int? = null,
        @SerialName("current_weekly_remaining_percent") val weeklyRemainPct: Double? = null,
        @SerialName("weekly_end_time") val weeklyEndTime: Long? = null,
    )

    @Serializable
    data class RemainsResponse(
        @SerialName("base_resp") val baseResp: BaseResp? = null,
        @SerialName("model_remains") val modelRemains: List<ModelRemain> = emptyList(),
    )

    suspend fun fetch(client: OkHttpClient, provider: Provider, apiKey: String): CodingPlanUsage =
        parseResponse(
            httpGet(
                client,
                provider.baseUrl + PATH,
                "Bearer $apiKey",
                mapOf("Accept" to "application/json"),
            ),
        )

    internal fun parseResponse(body: String): CodingPlanUsage {
        val parsed = runCatching { json.decodeFromString<RemainsResponse>(body) }
            .getOrElse { throw UsageException("响应解析失败", it) }
        return parse(parsed)
    }

    fun parse(resp: RemainsResponse): CodingPlanUsage {
        resp.baseResp?.let { base ->
            val code = base.statusCode ?: -1
            if (code != 0) throw UsageException("服务返回错误 (code $code): ${base.statusMsg ?: "未知"}")
        }

        // 只取 general(编程套餐)桶,跳过 video 等非编程模型
        val item = resp.modelRemains.firstOrNull { it.modelName == "general" }
            ?: return CodingPlanUsage(level = null, fiveHour = null, weekly = null)

        val fiveHour = item.intervalRemainPct?.let {
            Tier(pct = (100.0 - it).toFloat(), resetMs = item.endTime?.takeIf { t -> t > 0 })
        }
        // current_weekly_status == 1 表示周限额激活;3 等其他值代表该套餐无周限,不展示
        val weekly = if (item.weeklyStatus == 1) {
            item.weeklyRemainPct?.let {
                Tier(pct = (100.0 - it).toFloat(), resetMs = item.weeklyEndTime?.takeIf { t -> t > 0 })
            }
        } else {
            null
        }
        return CodingPlanUsage(level = null, fiveHour = fiveHour, weekly = weekly)
    }
}
