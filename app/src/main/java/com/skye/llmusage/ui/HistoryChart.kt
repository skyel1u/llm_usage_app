package com.skye.llmusage.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** 百分比历史曲线:0/50/100 网格线 + 平滑路径。points 为 (timestampMs, pct) 列表 */
@Composable
fun HistoryChart(points: List<Pair<Long, Float>>, modifier: Modifier = Modifier, height: Dp = 160.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val tMin = points.first().first
        val tMax = max(points.last().first, tMin + 1)
        val pad = 4.dp.toPx()

        // 网格:0 / 50 / 100
        listOf(0.5f, 0.25f, 0f).forEach { frac ->
            val y = h - pad - (h - 2 * pad) * frac
            drawLine(
                color = gridColor,
                start = Offset(pad, y),
                end = Offset(w - pad, y),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
            )
        }

        val path = Path()
        points.forEachIndexed { i, (t, pct) ->
            val x = pad + (w - 2 * pad) * (t - tMin).toFloat() / (tMax - tMin).toFloat()
            val y = h - pad - (h - 2 * pad) * pct.coerceIn(0f, 100f) / 100f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = primary,
            style = Stroke(width = 2.5.dp.toPx()),
        )
        points.forEach { (t, pct) ->
            val x = pad + (w - 2 * pad) * (t - tMin).toFloat() / (tMax - tMin).toFloat()
            val y = h - pad - (h - 2 * pad) * pct.coerceIn(0f, 100f) / 100f
            drawCircle(color = primary, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Spacer(
            Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
