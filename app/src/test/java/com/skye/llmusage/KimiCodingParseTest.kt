package com.skye.llmusage

import com.skye.llmusage.data.api.KimiCodingApi
import com.skye.llmusage.data.api.KimiCodingApi.UsageDetail
import com.skye.llmusage.data.api.KimiCodingApi.UsageResponse
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KimiCodingParseTest {

    private fun detail(limit: Number?, remaining: Number?, reset: Any? = null) = UsageDetail(
        limit = limit?.let { JsonPrimitive(it.toDouble()) },
        remaining = remaining?.let { JsonPrimitive(it.toDouble()) },
        resetTime = when (reset) {
            null -> null
            is Number -> JsonPrimitive(reset.toLong())
            else -> JsonPrimitive(reset.toString())
        },
    )

    @Test
    fun `limit and remaining produce used percentage`() {
        // limit=100 remaining=25 → 已用 75%
        val usage = KimiCodingApi.parse(
            UsageResponse(
                limits = listOf(com.skye.llmusage.data.api.KimiCodingApi.LimitItem(detail = detail(100, 25))),
                usage = detail(200, 100),
            ),
        )
        assertEquals(75f, usage.fiveHour?.pct)
        assertEquals(50f, usage.weekly?.pct)
        assertNull(usage.level)
    }

    @Test
    fun `remaining above limit clamps to zero percent`() {
        // remaining > limit(异常数据)不出现负百分比
        val usage = KimiCodingApi.parse(
            UsageResponse(
                limits = listOf(com.skye.llmusage.data.api.KimiCodingApi.LimitItem(detail = detail(100, 120))),
                usage = null,
            ),
        )
        assertEquals(0f, usage.fiveHour?.pct)
        assertNull(usage.weekly)
    }

    @Test
    fun `zero or missing limit yields no tier`() {
        val usage = KimiCodingApi.parse(
            UsageResponse(
                limits = listOf(com.skye.llmusage.data.api.KimiCodingApi.LimitItem(detail = detail(0, 0))),
                usage = detail(null, 0),
            ),
        )
        assertNull(usage.fiveHour)
        assertNull(usage.weekly)
    }

    @Test
    fun `reset time accepts seconds milliseconds and iso strings`() {
        // 秒级:1_000_000_000 → *1000
        assertEquals(1_000_000_000_000L, KimiCodingApi.resetMs(JsonPrimitive(1_000_000_000)))
        // 毫秒级:1_700_000_000_000 原样
        assertEquals(1_700_000_000_000L, KimiCodingApi.resetMs(JsonPrimitive(1_700_000_000_000)))
        // ISO 8601 字符串
        val iso = KimiCodingApi.resetMs(JsonPrimitive("2026-08-18T00:00:00Z"))
        assertTrue("ISO 解析应成功: $iso", iso != null && iso > 1_700_000_000_000L)
        // 0 / 负值视为无重置
        assertNull(KimiCodingApi.resetMs(JsonPrimitive(0)))
        assertNull(KimiCodingApi.resetMs(JsonPrimitive(-1)))
        assertNull(KimiCodingApi.resetMs(null))
    }

    @Test
    fun `json payload decodes end to end`() {
        val body = """
            {
              "limits": [
                {"detail": {"limit": 120, "remaining": 30, "resetTime": 1755500000}}
              ],
              "usage": {"limit": 2400, "remaining": 1200, "resetTime": 1755900000}
            }
        """.trimIndent()
        val usage = KimiCodingApi.parseResponse(body)
        assertEquals(75f, usage.fiveHour?.pct)
        assertEquals(1_755_500_000_000L, usage.fiveHour?.resetMs)
        assertEquals(50f, usage.weekly?.pct)
    }
}
