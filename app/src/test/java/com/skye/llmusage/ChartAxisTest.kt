package com.skye.llmusage

import com.skye.llmusage.ui.niceCeil
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartAxisTest {

    @Test
    fun `rounds up to 1-2-5-10 steps`() {
        assertEquals(1f, niceCeil(0f), 1e-6f)
        assertEquals(1f, niceCeil(-5f), 1e-6f)
        assertEquals(0.5f, niceCeil(0.4f), 1e-6f)
        assertEquals(2f, niceCeil(1.5f), 1e-6f)
        assertEquals(5f, niceCeil(3.3f), 1e-6f)
        assertEquals(10f, niceCeil(7f), 1e-6f)
        assertEquals(20f, niceCeil(12f), 1e-6f)
        assertEquals(100f, niceCeil(99.9f), 1e-6f)
        assertEquals(200f, niceCeil(123.4f), 1e-6f)
    }
}
