package com.skye.llmusage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skye.llmusage.R
import com.skye.llmusage.data.db.SnapshotEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val HeatWeeks = 26
private val CellSize = 13.dp
private val CellGap = 3.dp
private val WeekdayLabelWidth = 18.dp
private val DateFmt = DateTimeFormatter.ofPattern("M月d日")

/** 热力图中的一天:当日 5h 窗口百分比峰值与快照数 */
internal data class HeatDay(
    val date: LocalDate,
    val maxPct: Float?,
    val snapshotCount: Int,
)

/** 把快照按本地日历日聚合成 weeks 列(每列周一起 7 天),最后一列含 today */
internal fun buildHeatWeeks(
    snapshots: List<SnapshotEntity>,
    weeks: Int,
    today: LocalDate,
    zone: ZoneId,
): List<List<HeatDay>> {
    val byDay = snapshots
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .mapValues { (date, list) ->
            HeatDay(
                date = date,
                maxPct = list.mapNotNull { it.fiveHourPct }.maxOrNull(),
                snapshotCount = list.size,
            )
        }
    val firstMonday = today.minusWeeks((weeks - 1).toLong()).with(DayOfWeek.MONDAY)
    return (0 until weeks).map { w ->
        val monday = firstMonday.plusWeeks(w.toLong())
        (0..6).map { d ->
            val date = monday.plusDays(d.toLong())
            byDay[date] ?: HeatDay(date, null, 0)
        }
    }
}

/** 0 无数据 / 1~4 按当日峰值分档;仅 PAYG 快照(无百分比)记为 1 */
internal fun heatLevel(maxPct: Float?, snapshotCount: Int): Int = when {
    maxPct != null -> when {
        maxPct <= 0f -> 0 // 当日峰值 0% = 无用量,与 GitHub 零提交同义
        maxPct < 25f -> 1
        maxPct < 50f -> 2
        maxPct < 75f -> 3
        else -> 4
    }
    // 无百分比的快照(PAYG):当天刷新过记为最低档
    snapshotCount > 0 -> 1
    else -> 0
}

/** GitHub 风格用量日历:最近 26 周,列=周、行=周一到周日,点按查看当日详情 */
@Composable
fun UsageHeatmap(snapshots: List<SnapshotEntity>, modifier: Modifier = Modifier) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val weeks = remember(snapshots, today) { buildHeatWeeks(snapshots, HeatWeeks, today, zone) }

    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primary = MaterialTheme.colorScheme.primary
    val levelColors = remember(emptyColor, primary) {
        listOf(emptyColor, primary.copy(0.25f), primary.copy(0.5f), primary.copy(0.75f), primary)
    }

    var selectedDate by remember(weeks) { mutableStateOf(today) }

    val scroll = rememberScrollState()
    // 初次布局后滚到最右(最近一周)
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            // 周几标签固定在左侧,不随网格滚动
            Column(Modifier.width(WeekdayLabelWidth)) {
                Spacer(Modifier.height(CellSize)) // 与月份标签行对齐
                weeks.first().forEach { day ->
                    val label = when (day.date.dayOfWeek) {
                        DayOfWeek.MONDAY -> "一"
                        DayOfWeek.WEDNESDAY -> "三"
                        DayOfWeek.FRIDAY -> "五"
                        else -> null
                    }
                    Box(Modifier.height(CellSize), contentAlignment = Alignment.Center) {
                        Text(
                            label ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f).horizontalScroll(scroll)) {
                // 月份标签:与列对齐
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    weeks.forEach { week ->
                        val label = week.firstOrNull { it.date.dayOfMonth == 1 }
                            ?.let { "${it.date.monthValue}月" }
                        Box(Modifier.size(CellSize), contentAlignment = Alignment.CenterStart) {
                            if (label != null) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                            week.forEach { day ->
                                HeatCell(
                                    color = levelColors[heatLevel(day.maxPct, day.snapshotCount)],
                                    isToday = day.date == today,
                                    isSelected = day.date == selectedDate,
                                    onClick = { selectedDate = day.date },
                                )
                            }
                        }
                    }
                }
            }
        }

        val selected = weeks.firstNotNullOfOrNull { week -> week.firstOrNull { it.date == selectedDate } }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = selected?.let { day ->
                    val dateStr = day.date.format(DateFmt)
                    when {
                        day.maxPct != null && day.maxPct > 0f ->
                            stringResource(R.string.heatmap_day_pct_fmt, dateStr, day.maxPct.toInt())
                        day.snapshotCount > 0 ->
                            stringResource(R.string.heatmap_day_snapshots_fmt, dateStr, day.snapshotCount)
                        else -> stringResource(R.string.heatmap_day_empty, dateStr)
                    }
                } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            HeatLegend(colors = levelColors)
        }
    }
}

@Composable
private fun HeatCell(color: Color, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(3.dp)
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.onSurface
        isToday -> MaterialTheme.colorScheme.primary
        else -> null
    }
    Box(
        Modifier
            .size(CellSize)
            .clip(shape)
            .background(color, shape)
            .then(
                borderColor?.let {
                    Modifier.border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = it,
                        shape = shape,
                    )
                } ?: Modifier,
            )
            .clickable(onClick = onClick),
    )
}

@Composable
private fun HeatLegend(colors: List<Color>, cell: Dp = 10.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            stringResource(R.string.heatmap_legend_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        val shape = RoundedCornerShape(2.dp)
        colors.forEach { c ->
            Box(Modifier.size(cell).clip(shape).background(c, shape))
        }
        Text(
            stringResource(R.string.heatmap_legend_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
