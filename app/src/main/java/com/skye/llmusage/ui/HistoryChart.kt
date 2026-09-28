package com.skye.llmusage.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skye.llmusage.util.Fmt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** 轴上限:向上取整到 1/2/5/10 × 10^k,保证中位刻度落在可读数值上 */
internal fun niceCeil(v: Float): Float {
    if (v <= 0f) return 1f
    val exp = kotlin.math.floor(kotlin.math.log10(v.toDouble())).toInt()
    val base = Math.pow(10.0, exp.toDouble()).toFloat()
    val f = v / base
    val step = when {
        f <= 1f -> 1f
        f <= 2f -> 2f
        f <= 5f -> 5f
        else -> 10f
    }
    return (step * base).coerceAtLeast(base)
}

/**
 * 数值历史曲线:y 轴顶/中/底三档刻度标签 + x 轴首尾时间 + 点按/拖动查看数值。
 * points 为 (timestampMs, value) 列表;yMax 默认百分比量程,余额等金额量程由调用方传入。
 */
@Composable
fun HistoryChart(
    points: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    yMax: Float = 100f,
    formatY: (Float) -> String = { "${it.roundToInt()}%" },
    tooltip: (Long, Float) -> String = { t, v -> "${Fmt.pct(v)} · ${Fmt.formatAbsolute(t)}" },
) {
    if (points.size < 2) {
        Spacer(modifier.fillMaxWidth().height(height))
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
    val tipStyle = TextStyle(fontSize = 11.sp, color = onPrimary)

    val tMin = points.first().first
    val tMax = max(points.last().first, tMin + 1)
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    // 指针坐标 → 最近数据点:x 反演时间,再找最近时间戳(在组合期把 Dp 定准,避免手势 lambda 重建)
    val density = LocalDensity.current
    val leftPadPx = with(density) { AxisLeftPad.toPx() }
    val rightPadPx = with(density) { AxisRightPad.toPx() }
    val indexAt: (Float, Float) -> Int? = { xPx, wPx ->
        val plotW = wPx - leftPadPx - rightPadPx
        if (plotW <= 0f) {
            null
        } else {
            val frac = ((xPx - leftPadPx) / plotW).coerceIn(0f, 1f)
            val t = tMin + frac * (tMax - tMin)
            points.indices.minByOrNull { abs(points[it].first - t) }
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points) {
                detectTapGestures { pos -> selected = indexAt(pos.x, size.width.toFloat()) }
            }
            .pointerInput(points) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    selected = indexAt(change.position.x, size.width.toFloat())
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val leftPad = leftPadPx
        val rightPad = rightPadPx
        val topPad = TopPad.toPx()
        val bottomPad = BottomPad.toPx()
        val plotW = w - leftPad - rightPad
        val plotH = h - topPad - bottomPad
        if (plotW <= 0f || plotH <= 0f) return@Canvas
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))

        fun px(t: Long) = leftPad + plotW * (t - tMin) / (tMax - tMin)
        fun py(v: Float) = topPad + plotH * (1f - v.coerceIn(0f, yMax) / yMax)

        // 网格线 + y 轴刻度标签(顶/中/底),标签右对齐贴轴线左侧
        listOf(1f, 0.5f, 0f).forEach { frac ->
            val y = topPad + plotH * (1f - frac)
            drawLine(gridColor, Offset(leftPad, y), Offset(w - rightPad, y), pathEffect = dash)
            val label = textMeasurer.measure(formatY(yMax * frac), labelStyle)
            drawText(label, topLeft = Offset(leftPad - 4.dp.toPx() - label.size.width, y - label.size.height / 2f))
        }

        // x 轴首尾时间标签
        val xStart = textMeasurer.measure(Fmt.axisTime(tMin), labelStyle)
        drawText(xStart, topLeft = Offset(leftPad, h - xStart.size.height.toFloat()))
        val xEnd = textMeasurer.measure(Fmt.axisTime(tMax), labelStyle)
        drawText(xEnd, topLeft = Offset(w - rightPad - xEnd.size.width, h - xEnd.size.height.toFloat()))

        // 折线 + 数据点
        val path = Path()
        points.forEachIndexed { i, (t, v) ->
            val x = px(t)
            val y = py(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, primary, style = Stroke(width = 2.5.dp.toPx()))
        points.forEachIndexed { i, (t, v) ->
            if (i != selected) drawCircle(primary, radius = 3.dp.toPx(), center = Offset(px(t), py(v)))
        }

        // 选中点:竖直参考线 + 高亮点 + 气泡数值
        selected?.let { idx ->
            val (t, v) = points[idx]
            val cx = px(t)
            val cy = py(v)
            drawLine(primary.copy(alpha = 0.4f), Offset(cx, topPad), Offset(cx, topPad + plotH), pathEffect = dash)
            drawCircle(primary, radius = 5.dp.toPx(), center = Offset(cx, cy))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))

            val tip = textMeasurer.measure(tooltip(t, v), tipStyle)
            val padH = 6.dp.toPx()
            val padV = 4.dp.toPx()
            val bx = (cx - tip.size.width / 2f).coerceIn(leftPad, max(leftPad, w - rightPad - tip.size.width))
            val by = (cy - tip.size.height - 10.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(
                color = primary,
                topLeft = Offset(bx - padH, by - padV),
                size = Size(tip.size.width + 2 * padH, tip.size.height + 2 * padV),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            drawText(tip, topLeft = Offset(bx, by))
        }
    }
}

/** 轴留白:左放下刻度文本,底放时间标签,顶部给气泡留空间 */
private val AxisLeftPad = 34.dp
private val AxisRightPad = 6.dp
private val TopPad = 10.dp
private val BottomPad = 16.dp

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
