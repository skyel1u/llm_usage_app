package com.skye.llmusage

import com.skye.llmusage.data.db.SnapshotEntity
import com.skye.llmusage.ui.buildHeatWeeks
import com.skye.llmusage.ui.heatLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class HeatmapTest {

    @Test
    fun `levels map pct buckets`() {
        assertEquals(0, heatLevel(null, 0))
        assertEquals(1, heatLevel(null, 3)) // PAYG 快照无百分比 → 最低活跃档
        assertEquals(0, heatLevel(0f, 2)) // 明确 0% 当天无用量 → 空
        assertEquals(1, heatLevel(0.5f, 1))
        assertEquals(1, heatLevel(24.9f, 1))
        assertEquals(2, heatLevel(25f, 1))
        assertEquals(2, heatLevel(49.9f, 1))
        assertEquals(3, heatLevel(50f, 1))
        assertEquals(3, heatLevel(74.9f, 1))
        assertEquals(4, heatLevel(75f, 1))
        assertEquals(4, heatLevel(100f, 1))
    }

    @Test
    fun `weeks start monday and end today`() {
        val today = LocalDate.of(2026, 9, 27)
        val weeks = buildHeatWeeks(emptyList(), weeks = 4, today = today, zone = ZoneId.of("UTC"))
        assertEquals(4, weeks.size)
        val days = weeks.flatten()
        assertEquals(28, days.size)
        assertEquals(DayOfWeek.MONDAY, days.first().date.dayOfWeek)
        assertEquals(today, days.last().date)
        // 全网格日期连续
        days.zipWithNext().forEach { (a, b) -> assertEquals(a.date.plusDays(1), b.date) }
        days.forEach { d ->
            assertEquals(0, d.snapshotCount)
            assertEquals(null, d.maxPct)
        }
    }

    @Test
    fun `snapshots aggregate per day by max pct`() {
        val zone = ZoneId.of("UTC")
        val day1 = LocalDate.of(2026, 9, 20)
        val day2 = LocalDate.of(2026, 9, 21)
        fun ts(date: LocalDate, hour: Int) = date.atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()
        val snapshots = listOf(
            SnapshotEntity(accountId = 1, timestamp = ts(day1, 9), fiveHourPct = 30.5f),
            SnapshotEntity(accountId = 1, timestamp = ts(day1, 20), fiveHourPct = 80.2f),
            SnapshotEntity(accountId = 1, timestamp = ts(day1, 12)), // PAYG 快照无百分比
            SnapshotEntity(accountId = 1, timestamp = ts(day2, 10), fiveHourPct = 12f),
        )
        val weeks = buildHeatWeeks(snapshots, weeks = 2, today = LocalDate.of(2026, 9, 26), zone = zone)
        val all = weeks.flatten().associateBy { it.date }
        val d1 = all.getValue(day1)
        assertEquals(80.2f, d1.maxPct!!, 0.001f)
        assertEquals(3, d1.snapshotCount)
        val d2 = all.getValue(day2)
        assertEquals(12f, d2.maxPct!!, 0.001f)
        assertEquals(1, d2.snapshotCount)
        // 其余日期无数据
        assertTrue(all.getValue(LocalDate.of(2026, 9, 22)).snapshotCount == 0)
    }
}
