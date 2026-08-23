package com.skye.llmusage

import com.skye.llmusage.data.api.GlmApi
import com.skye.llmusage.data.api.GlmApi.LimitItem
import com.skye.llmusage.data.api.GlmApi.QuotaData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GlmParseTest {

    @Test
    fun `unit 3 maps to five-hour and unit 6 maps to weekly`() {
        val usage = GlmApi.parse(
            QuotaData(
                level = "Max",
                limits = listOf(
                    LimitItem(type = "TOKENS_LIMIT", percentage = 42.5, nextResetTime = 1750000000000, unit = 3),
                    LimitItem(type = "TOKENS_LIMIT", percentage = 63.0, nextResetTime = 1750100000000, unit = 6),
                ),
            ),
        )
        assertEquals("Max", usage.level)
        assertEquals(42.5f, usage.fiveHour?.pct)
        assertEquals(1750000000000L, usage.fiveHour?.resetMs)
        assertEquals(63.0f, usage.weekly?.pct)
    }

    @Test
    fun `unknown unit items fill missing slots in order`() {
        val usage = GlmApi.parse(
            QuotaData(
                limits = listOf(
                    LimitItem(type = "TOKENS_LIMIT", percentage = 10.0, unit = null),
                    LimitItem(type = "TOKENS_LIMIT", percentage = 20.0, unit = null),
                ),
            ),
        )
        assertEquals(10.0f, usage.fiveHour?.pct)
        assertEquals(20.0f, usage.weekly?.pct)
    }

    @Test
    fun `non-token limit types are ignored`() {
        val usage = GlmApi.parse(
            QuotaData(
                limits = listOf(
                    LimitItem(type = "REQUESTS_LIMIT", percentage = 99.0, unit = 3),
                    LimitItem(type = "tokens_limit", percentage = 15.0, unit = 3),
                ),
            ),
        )
        assertEquals(15.0f, usage.fiveHour?.pct)
        assertNull(usage.weekly)
    }

    @Test
    fun `zero or negative reset time is treated as absent`() {
        val usage = GlmApi.parse(
            QuotaData(
                limits = listOf(LimitItem(type = "TOKENS_LIMIT", percentage = 5.0, nextResetTime = 0, unit = 3)),
            ),
        )
        assertNotNull(usage.fiveHour)
        assertNull(usage.fiveHour?.resetMs)
    }

    @Test
    fun `empty limits yield null tiers`() {
        val usage = GlmApi.parse(QuotaData(level = "Lite"))
        assertEquals("Lite", usage.level)
        assertNull(usage.fiveHour)
        assertNull(usage.weekly)
    }

    @Test
    fun `credit limit items map by unit like tokens limit`() {
        val usage = GlmApi.parse(
            QuotaData(
                level = "lite",
                limits = listOf(
                    LimitItem(type = "CREDIT_LIMIT", percentage = 46.0, nextResetTime = 1786986558422, unit = 3),
                    LimitItem(type = "CREDIT_LIMIT", percentage = 84.0, nextResetTime = 1787447294998, unit = 6),
                ),
            ),
        )
        assertEquals(46.0f, usage.fiveHour?.pct)
        assertEquals(1786986558422L, usage.fiveHour?.resetMs)
        assertEquals(84.0f, usage.weekly?.pct)
        assertEquals(1787447294998L, usage.weekly?.resetMs)
    }

    @Test
    fun `live credit limit payload decodes end to end`() {
        val body = """
            {
                "code": 200,
                "msg": "操作成功",
                "data": {
                    "limits": [
                        {
                            "type": "CREDIT_LIMIT",
                            "unit": 3,
                            "number": 5,
                            "usage": 2000,
                            "currentValue": 935,
                            "remaining": 1064,
                            "percentage": 46,
                            "nextResetTime": 1786986558422
                        },
                        {
                            "type": "CREDIT_LIMIT",
                            "unit": 6,
                            "number": 1,
                            "usage": 10000,
                            "currentValue": 8482,
                            "remaining": 1517,
                            "percentage": 84,
                            "nextResetTime": 1787447294998
                        }
                    ],
                    "level": "lite"
                },
                "success": true
            }
        """.trimIndent()
        val usage = GlmApi.parseResponse(body)
        assertEquals("lite", usage.level)
        assertEquals(46.0f, usage.fiveHour?.pct)
        assertEquals(1786986558422L, usage.fiveHour?.resetMs)
        assertEquals(84.0f, usage.weekly?.pct)
        assertEquals(1787447294998L, usage.weekly?.resetMs)
    }
}
