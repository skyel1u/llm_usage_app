package com.skye.llmusage.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object Fmt {
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFmt = DateTimeFormatter.ofPattern("M月d日 HH:mm")

    /** 相对时间:刚刚 / N 分钟前 / N 小时前 / 绝对时间(超过 22h) */
    fun relativeTime(nowMs: Long, pastMs: Long): String {
        val diff = nowMs - pastMs
        return when {
            diff < 45_000L -> "刚刚"
            diff < 55 * 60_000L -> "${(diff / 60_000L).coerceAtLeast(1)} 分钟前"
            diff < 22 * 3_600_000L -> "${diff / 3_600_000L} 小时前"
            else -> formatAbsolute(pastMs)
        }
    }

    fun formatAbsolute(epochMs: Long): String {
        val t = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        return (if (t.toLocalDate() == LocalDate.now()) timeFmt else dateTimeFmt).format(t)
    }

    /** 距重置:Xd Xh / Xh Ym / Xm / 即将重置 */
    fun remaining(nowMs: Long, resetMs: Long): String {
        val d = resetMs - nowMs
        if (d <= 0) return "即将重置"
        val dur = Duration.ofMillis(d)
        val days = dur.toDays()
        val hours = dur.toHours() % 24
        val minutes = dur.toMinutes() % 60
        return when {
            days > 0 -> "${days}天${hours}小时后重置"
            hours > 0 -> "${hours}小时${minutes}分后重置"
            minutes > 0 -> "${minutes}分钟后重置"
            else -> "即将重置"
        }
    }

    fun money(currency: String?, v: Double): String = when (currency?.uppercase()) {
        "CNY", null -> "¥%.2f".format(v)
        "USD" -> "$%.2f".format(v)
        else -> "%.2f $currency".format(v)
    }

    fun pct(p: Float): String = "${p.roundToInt()}%"
}
