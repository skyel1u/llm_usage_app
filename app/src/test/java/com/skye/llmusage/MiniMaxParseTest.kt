package com.skye.llmusage

import com.skye.llmusage.data.api.MiniMaxApi
import com.skye.llmusage.data.api.MiniMaxApi.ModelRemain
import com.skye.llmusage.data.api.MiniMaxApi.RemainsResponse
import com.skye.llmusage.data.api.UsageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class MiniMaxParseTest {

    @Test
    fun `general bucket two tiers from remaining percent`() {
        // 剩余 98% / 95% → 已用 2% / 5%
        val usage = MiniMaxApi.parse(
            RemainsResponse(
                modelRemains = listOf(
                    ModelRemain(
                        modelName = "general",
                        intervalRemainPct = 98.0,
                        endTime = 1_780_329_600_000,
                        weeklyStatus = 1,
                        weeklyRemainPct = 95.0,
                        weeklyEndTime = 1_780_848_000_000,
                    ),
                    ModelRemain(modelName = "video", intervalRemainPct = 100.0),
                ),
            ),
        )
        assertEquals(2f, usage.fiveHour?.pct)
        assertEquals(1_780_329_600_000L, usage.fiveHour?.resetMs)
        assertEquals(5f, usage.weekly?.pct)
        assertEquals(1_780_848_000_000L, usage.weekly?.resetMs)
    }

    @Test
    fun `video bucket is skipped and general found in any position`() {
        val usage = MiniMaxApi.parse(
            RemainsResponse(
                modelRemains = listOf(
                    ModelRemain(modelName = "video", intervalRemainPct = 50.0),
                    ModelRemain(modelName = "general", intervalRemainPct = 80.0, weeklyStatus = 1, weeklyRemainPct = 70.0),
                ),
            ),
        )
        assertEquals(20f, usage.fiveHour?.pct)
        assertEquals(30f, usage.weekly?.pct)
    }

    @Test
    fun `weekly status other than 1 skips weekly tier`() {
        // 无周限额套餐:status=3 且剩余恒 100,不应显示 0% 已用的假周桶
        val usage = MiniMaxApi.parse(
            RemainsResponse(
                modelRemains = listOf(
                    ModelRemain(modelName = "general", intervalRemainPct = 99.0, weeklyStatus = 3, weeklyRemainPct = 100.0),
                ),
            ),
        )
        assertEquals(1f, usage.fiveHour?.pct)
        assertNull(usage.weekly)
    }

    @Test
    fun `missing general bucket yields no tiers`() {
        val usage = MiniMaxApi.parse(
            RemainsResponse(modelRemains = listOf(ModelRemain(modelName = "video"))),
        )
        assertNull(usage.fiveHour)
        assertNull(usage.weekly)
    }

    @Test
    fun `nonzero base_resp status throws usage exception`() {
        try {
            MiniMaxApi.parse(
                RemainsResponse(
                    baseResp = MiniMaxApi.BaseResp(statusCode = 1004, statusMsg = "invalid api key"),
                ),
            )
            fail("应抛出 UsageException")
        } catch (e: UsageException) {
            assertEquals("服务返回错误 (code 1004): invalid api key", e.message)
        }
    }

    @Test
    fun `json payload decodes end to end`() {
        val body = """
            {
              "base_resp": {"status_code": 0, "status_msg": "success"},
              "model_remains": [
                {
                  "model_name": "general",
                  "current_interval_remaining_percent": 60,
                  "current_interval_status": 1,
                  "end_time": 1780365600000,
                  "current_weekly_status": 1,
                  "current_weekly_remaining_percent": 90,
                  "weekly_end_time": 1780848000000
                }
              ]
            }
        """.trimIndent()
        val usage = MiniMaxApi.parseResponse(body)
        assertEquals(40f, usage.fiveHour?.pct)
        assertEquals(10f, usage.weekly?.pct)
    }
}
