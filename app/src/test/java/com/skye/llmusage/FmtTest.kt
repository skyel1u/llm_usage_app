package com.skye.llmusage

import com.skye.llmusage.util.Fmt
import org.junit.Assert.assertEquals
import org.junit.Test

class FmtTest {

    @Test
    fun `relative time buckets`() {
        val now = 1_000_000_000L
        assertEquals("刚刚", Fmt.relativeTime(now, now - 10_000))
        assertEquals("5 分钟前", Fmt.relativeTime(now, now - 5 * 60_000))
        assertEquals("3 小时前", Fmt.relativeTime(now, now - 3 * 3_600_000))
    }

    @Test
    fun `remaining formats durations`() {
        val now = 1_000_000_000L
        assertEquals("1小时30分后重置", Fmt.remaining(now, now + 90 * 60_000))
        assertEquals("2天3小时后重置", Fmt.remaining(now, now + (2 * 24 + 3) * 3_600_000))
        assertEquals("即将重置", Fmt.remaining(now, now - 1))
        assertEquals("45分钟后重置", Fmt.remaining(now, now + 45 * 60_000))
    }

    @Test
    fun `pct rounds`() {
        assertEquals("42%", Fmt.pct(42.4f))
        assertEquals("43%", Fmt.pct(42.5f))
        assertEquals("100%", Fmt.pct(99.9f))
    }

    @Test
    fun `money formats with locale-independent dot decimal`() {
        assertEquals("¥110.50", Fmt.money("CNY", 110.5))
        assertEquals("$3.20", Fmt.money("USD", 3.2))
        assertEquals("¥5.00", Fmt.money(null, 5.0))
        assertEquals("7.25 EUR", Fmt.money("EUR", 7.25))
    }

    @Test
    fun `money axis keeps labels short`() {
        assertEquals("¥233", Fmt.moneyAxis("CNY", 233.33))
        assertEquals("$99.99", Fmt.moneyAxis("USD", 99.99))
    }
}
