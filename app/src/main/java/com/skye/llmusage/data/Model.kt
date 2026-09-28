package com.skye.llmusage.data

/** 账户类型:Coding Plan(订阅套餐)或 PAYG(按量付费) */
enum class AccountType(val label: String) {
    CODING_PLAN("Coding Plan"),
    PAYG("PAYG");

    companion object {
        fun of(name: String?): AccountType = entries.firstOrNull { it.name == name } ?: CODING_PLAN
    }
}

/** 提供商:仅支持已确认端点的官方站点 */
enum class Provider(val displayName: String, val baseUrl: String) {
    GLM_CN("GLM 国内站", "https://open.bigmodel.cn"),
    GLM_INTL("GLM 国际站", "https://api.z.ai"),
    KIMI_CODING("Kimi For Coding", "https://api.kimi.com"),
    MINIMAX_CN("MiniMax 国内站", "https://api.minimaxi.com"),
    MINIMAX_INTL("MiniMax 国际站", "https://api.minimax.io"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com");

    val type: AccountType
        get() = if (this == DEEPSEEK) AccountType.PAYG else AccountType.CODING_PLAN

    companion object {
        fun of(name: String?): Provider = entries.firstOrNull { it.name == name } ?: GLM_CN
    }
}

/** Coding Plan 用量:各提供商统一为 5 小时 + 周两个档位,GLM 额外返回套餐等级 */
data class CodingPlanUsage(val level: String?, val fiveHour: Tier?, val weekly: Tier?)

/** 单一限额档位:pct 为 0..100 */
data class Tier(val pct: Float, val resetMs: Long? = null)


data class Balance(
    val currency: String?,
    val total: Double,
    val granted: Double,
    val toppedUp: Double,
    val available: Boolean,
)

/** 主页卡片所需的全部状态 */
data class AccountUi(
    val id: Long,
    val name: String,
    val type: AccountType,
    val provider: Provider,
    val lastRefreshMs: Long?,
    val lastError: String?,
    val level: String?,
    val fiveHour: Tier?,
    val weekly: Tier?,
    val balance: Balance?,
)
