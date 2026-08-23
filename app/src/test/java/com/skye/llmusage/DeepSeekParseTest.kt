package com.skye.llmusage

import com.skye.llmusage.data.api.DeepSeekApi
import com.skye.llmusage.data.api.UsageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekParseTest {

    @Test
    fun `parses cny balance with breakdown`() {
        val balance = DeepSeekApi.parse(
            DeepSeekApi.BalanceResponse(
                isAvailable = true,
                balanceInfos = listOf(
                    DeepSeekApi.BalanceInfo(
                        currency = "CNY",
                        totalBalance = "110.00",
                        grantedBalance = "10.00",
                        toppedUpBalance = "100.00",
                    ),
                ),
            ),
        )
        assertEquals("CNY", balance.currency)
        assertEquals(110.0, balance.total, 1e-9)
        assertEquals(10.0, balance.granted, 1e-9)
        assertEquals(100.0, balance.toppedUp, 1e-9)
        assertTrue(balance.available)
    }

    @Test
    fun `prefers cny entry when multiple currencies`() {
        val balance = DeepSeekApi.parse(
            DeepSeekApi.BalanceResponse(
                balanceInfos = listOf(
                    DeepSeekApi.BalanceInfo(currency = "USD", totalBalance = "5.00"),
                    DeepSeekApi.BalanceInfo(currency = "CNY", totalBalance = "36.00"),
                ),
            ),
        )
        assertEquals("CNY", balance.currency)
        assertEquals(36.0, balance.total, 1e-9)
    }

    @Test
    fun `defaults availability to true`() {
        val balance = DeepSeekApi.parse(
            DeepSeekApi.BalanceResponse(
                balanceInfos = listOf(DeepSeekApi.BalanceInfo(currency = "CNY", totalBalance = "1.00")),
            ),
        )
        assertTrue(balance.available)
    }

    @Test(expected = UsageException::class)
    fun `empty balance infos throws`() {
        DeepSeekApi.parse(DeepSeekApi.BalanceResponse(isAvailable = false))
    }

    @Test
    fun `formatting money for cny and usd`() {
        assertEquals("¥110.00", com.skye.llmusage.util.Fmt.money("CNY", 110.0))
        assertEquals("$5.50", com.skye.llmusage.util.Fmt.money("USD", 5.5))
    }

    @Test
    fun `unavailable flag propagates`() {
        val balance = DeepSeekApi.parse(
            DeepSeekApi.BalanceResponse(
                isAvailable = false,
                balanceInfos = listOf(DeepSeekApi.BalanceInfo(currency = "CNY", totalBalance = "0.00")),
            ),
        )
        assertFalse(balance.available)
    }
}
