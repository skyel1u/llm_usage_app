package com.skye.llmusage.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skye.llmusage.data.Tier
import com.skye.llmusage.ui.theme.tierColor
import com.skye.llmusage.util.Fmt
import kotlinx.coroutines.delay

/** 5 小时窗口环形进度,中心百分比 + 下方重置倒计时 */
@Composable
fun FiveHourRing(tier: Tier, modifier: Modifier = Modifier, ringSize: Dp = 108.dp) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val targetColor = tierColor(tier.pct)
    val resetMs = tier.resetMs
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // 仅当有重置时间需要倒计时时才启动计时器,否则不空转
    if (resetMs != null) {
        LaunchedEffect(resetMs) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(30_000)
            }
        }
    }
    val sweep by animateFloatAsState(
        targetValue = tier.pct.coerceIn(0f, 100f) / 100f * 360f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 90f),
        label = "sweep",
    )
    val color = animateColorAsStateM3(targetColor)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
            Canvas(Modifier.size(ringSize)) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text(
                text = Fmt.pct(tier.pct),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("5 小时窗口", style = MaterialTheme.typography.titleSmall)
            resetMs?.let {
                Text(
                    Fmt.remaining(nowMs, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 周限制线性进度条 */
@Composable
fun WeeklyBar(tier: Tier, modifier: Modifier = Modifier) {
    val resetMs = tier.resetMs
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // 仅当有重置时间需要倒计时时才启动计时器,否则不空转
    if (resetMs != null) {
        LaunchedEffect(resetMs) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(30_000)
            }
        }
    }
    val animated by animateFloatAsState(
        targetValue = tier.pct.coerceIn(0f, 100f) / 100f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 90f),
        label = "weekly",
    )
    val color = animateColorAsStateM3(tierColor(tier.pct))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("本周用量", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    Fmt.pct(tier.pct),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
                tier.resetMs?.let {
                    Text(
                        Fmt.remaining(nowMs, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { animated },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** animateColorAsState 的薄封装,避免每个调用点重复 import 样板 */
@Composable
private fun animateColorAsStateM3(target: Color): Color =
    androidx.compose.animation.animateColorAsState(
        targetValue = target,
        animationSpec = spring(stiffness = 400f),
        label = "tierColor",
    ).value
