package com.skye.llmusage.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.skye.llmusage.R

/**
 * 刷新图标:spinning 期间持续匀速旋转,结束后停回原位。
 * 主页/详情/趋势的刷新按钮共用,保证加载反馈一致。
 */
@Composable
fun RefreshIcon(spinning: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "refreshSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "refreshAngle",
    )
    Icon(
        Icons.Rounded.Refresh,
        contentDescription = stringResource(R.string.action_refresh),
        modifier = modifier.graphicsLayer { rotationZ = if (spinning) angle else 0f },
    )
}
