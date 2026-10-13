package com.skye.llmusage.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ripple
import com.skye.llmusage.ui.theme.LocalGlassEnabled

/** 当前会话的玻璃协调器;由 MainActivity 在根组合中提供 */
val LocalGlassController = staticCompositionLocalOf<GlassController?> { null }

/** 玻璃模式下当前是否深色(卡片材质与高光描边需要) */
val LocalGlassDark = staticCompositionLocalOf { false }

/** 玻璃模式底部悬浮胶囊占位的高度(Tab 栏 + 安全区),列表内容以此为底部内边距 */
val LocalGlassChromeInset = staticCompositionLocalOf { 0.dp }

/** 面板材质:CHROME=悬浮玻璃功能层(折射内容);CARD=不透明内容卡 */
enum class GlassMaterial { CHROME, CARD }

/**
 * 风格自适应容器,按 Apple Liquid Glass 双层结构分材质渲染:
 *
 * - 经典模式渲染 [classic],与现有 M3 外观完全一致;
 * - CHROME(玻璃功能层):[glass] 内容照常组合,窗口玻璃层同位置放一块
 *   LiquidGlass 面板实时折射/模糊下方滚过的内容;叠加高光描边与柔和投影,
 *   [panelAlpha] 支持"静止透明、滚动浮现"(仅玻璃与描边渐隐,内容不隐);
 * - CARD(内容层):不透明"纸面"卡 + 发丝描边 + 顶部反光 + 柔和投影,
 *   不参与玻璃采样(避免玻璃叠玻璃)。
 *
 * [classic]/[glass] 均须返回单一布局节点(外壳是 Box)。
 *
 * @param key 面板唯一标识,LazyColumn 条目应含条目 id
 */
@Composable
fun GlassPanel(
    key: Any,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.card(),
    material: GlassMaterial = GlassMaterial.CARD,
    panelAlpha: Float = 1f,
    classic: @Composable () -> Unit,
    glass: @Composable () -> Unit = classic,
) {
    if (!LocalGlassEnabled.current) {
        Box(modifier) { classic() }
        return
    }

    // 圆角 ≥99dp 视为胶囊:百分比圆角自动钳到短边一半
    val shape = if (config.cornerRadiusDp >= 99f) RoundedCornerShape(50)
    else RoundedCornerShape(config.cornerRadiusDp.dp)
    val dark = LocalGlassDark.current

    if (material == GlassMaterial.CARD) {
        val surface = if (dark) CardDarkSurface else CardLightSurface
        val shadowColor = if (dark) ShadowDark else ShadowLight
        Box(
            modifier
                .shadow(3.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
                .clip(shape)
                .background(surface),
        ) {
            glass()
            Box(Modifier.matchParentSize().liquidEdge(config.cornerRadiusDp, dark))
        }
        return
    }

    val controller = LocalGlassController.current
    if (controller == null) {
        Box(modifier) { classic() }
        return
    }

    DisposableEffect(controller, key) {
        controller.attach(key, config)
        onDispose { controller.detach(key) }
    }
    SideEffect { controller.setOpacity(key, panelAlpha) }

    Box(
        modifier.onGloballyPositioned { coords ->
            val b = coords.boundsInWindow()
            controller.updateBounds(key, b.left, b.top, b.right, b.bottom)
        },
    ) {
        glass()
        // 玻璃本体特效层:高光描边 + 投影,随 panelAlpha 渐隐(内容保持可见)
        val shadowColor = if (dark) ShadowDark else ShadowLight
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = panelAlpha }
                .shadow(10.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
                .liquidEdge(config.cornerRadiusDp, dark, strengthen = true),
        )
    }
}

private val CardLightSurface = Color(0xF8FBFAFF)
private val CardDarkSurface = Color(0xF20F1116)
private val ShadowLight = Color(0x1E2E3B8F)
private val ShadowDark = Color(0x66000000)

/**
 * 液态玻璃边缘处理(Apple 视觉签名):
 * 1. 发丝描边——上缘亮、下缘暗,模拟光掠过玻璃边缘;
 * 2. 顶部反光带——上缘约 40% 高度的白色渐隐,模拟镜面高光。
 */
private fun Modifier.liquidEdge(
    cornerRadiusDp: Float,
    dark: Boolean,
    strengthen: Boolean = false,
): Modifier = drawWithContent {
    drawContent()
    val radius = minOf(
        cornerRadiusDp.dp.toPx(),
        size.minDimension / 2f,
    )
    val stroke = 1.dp.toPx()
    val (topA, bottomA) = when {
        dark -> if (strengthen) 0.32f to 0.06f else 0.22f to 0.04f
        else -> if (strengthen) 0.70f to 0.10f else 0.55f to 0.08f
    }
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = topA),
            1f to Color.White.copy(alpha = bottomA),
        ),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(stroke),
    )
    val highlightA = if (dark) 0.08f else 0.16f
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = highlightA),
            1f to Color.Transparent,
        ),
        size = Size(size.width, size.height * 0.4f),
    )
}

/**
 * iOS 风格按压反馈:按下时轻微弹性收缩。玻璃 FAB / Tab 项等悬浮控件使用。
 */
@Composable
fun Modifier.pressableGlass(onClick: () -> Unit, scaleDown: Float = 0.96f): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
}
