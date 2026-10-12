package com.skye.llmusage.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.skye.llmusage.ui.theme.LocalGlassEnabled

/** 当前会话的玻璃协调器;由 MainActivity 在根组合中提供 */
val LocalGlassController = staticCompositionLocalOf<GlassController?> { null }

/**
 * 风格自适应容器:
 * - 经典模式渲染 [classic],与现有 M3 外观完全一致;
 * - 液态玻璃模式渲染 [glass] 内容,并在窗口玻璃层的相同位置放一块
 *   LiquidGlass 面板作为折射背景(玻璃位于内容之下、渐变背景之上)。
 *
 * 两个分支应复用同一份内容组合函数;[classic]/[glass] 均须返回单一布局节点
 * (GlassPanel 的外壳是 Box,多个裸子节点会叠放)。
 *
 * @param key 面板唯一标识,LazyColumn 条目应含条目 id
 * @param floating true 时玻璃挂到内容之上的前置层(FAB 等悬浮元素)
 */
@Composable
fun GlassPanel(
    key: Any,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.card(),
    floating: Boolean = false,
    classic: @Composable () -> Unit,
    glass: @Composable () -> Unit = classic,
) {
    val controller = LocalGlassController.current
    if (!LocalGlassEnabled.current || controller == null) {
        Box(modifier) { classic() }
        return
    }

    DisposableEffect(controller, key) {
        controller.attach(key, config, floating)
        onDispose { controller.detach(key) }
    }

    Box(
        modifier.onGloballyPositioned { coords ->
            val b = coords.boundsInWindow()
            controller.updateBounds(key, b.left, b.top, b.right, b.bottom)
        },
    ) {
        glass()
    }
}
