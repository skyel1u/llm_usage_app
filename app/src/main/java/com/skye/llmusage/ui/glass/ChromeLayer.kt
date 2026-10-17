package com.skye.llmusage.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.skye.llmusage.data.AppStyle
import com.skye.llmusage.data.ThemeMode
import com.skye.llmusage.ui.theme.LocalGlassEnabled
import com.skye.llmusage.ui.theme.LlmUsageTheme

/** chrome 组合层渲染所需的主题参数快照 */
class ChromeThemeParams(
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val appStyle: AppStyle,
    val dark: Boolean,
    val active: Boolean,
)

/** 下拉刷新指示器状态:由主组合共享 PullToRefreshState,chrome 层逐帧读取拉动进度 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class PullRefreshChrome(val state: PullToRefreshState, val isRefreshing: Boolean)

/**
 * 带所有权标识的功能层槽位:屏幕在 SideEffect 中以自身为 owner 赋值,
 * onDispose 时仅当槽位仍归自己所有才清空。
 *
 * 必要性:NavHost 转场期间旧屏保持组合,转场结束的 onDispose 晚于新屏
 * SideEffect 赋值——无所有权检查的清空会把新屏刚写入的栏一并抹掉,
 * 造成顶栏偶发消失。
 */
class ChromeSlot<T> {
    var value: T? by mutableStateOf(null)
        private set
    private var owner: Any? = null

    fun set(owner: Any, value: T) {
        this.owner = owner
        this.value = value
    }

    fun clear(owner: Any) {
        if (this.owner === owner) {
            this.owner = null
            value = null
        }
    }

    fun clearAll() {
        owner = null
        value = null
    }
}

/**
 * 玻璃模式"功能层"状态桥:主组合把各屏的栏内容(顶栏/底部胶囊/FAB/刷新指示器)
 * 写入这里,由玻璃层之上的独立 Compose 组合(chromeHost)渲染。
 *
 * 分层意义(Apple 前景永不模糊原则):LiquidGlass 面板位于主内容之上、折射/模糊
 * 滚动内容;而栏自身的图标文字又在玻璃之上,永远锐利。栏均为固定定位(顶部通栏/
 * 底部居中胶囊/右下 FAB),无需坐标同步。
 *
 * 各屏在每次重组的 SideEffect 中以自身 token 为 owner 重新赋值 lambda,
 * 保证捕获最新状态;离开屏幕时按 owner 清空,不会误清新屏的赋值。
 */
class ChromeLayer {
    val theme = mutableStateOf<ChromeThemeParams?>(null)
    val topBar = ChromeSlot<@Composable () -> Unit>()
    val topBarAlpha = mutableFloatStateOf(1f)
    val bottomBar = ChromeSlot<@Composable () -> Unit>()
    val pullRefresh = ChromeSlot<PullRefreshChrome>()

    fun clear() {
        topBar.clearAll()
        bottomBar.clearAll()
        pullRefresh.clearAll()
    }
}

val LocalChromeLayer = staticCompositionLocalOf<ChromeLayer?> { null }

/** chrome 组合层根:主题包装 + 固定定位槽位(顶栏/底部胶囊/FAB/刷新指示器) */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChromeLayerContent(layer: ChromeLayer, controller: GlassController?) {
    val params = layer.theme.value ?: return
    if (!params.active || controller == null) return

    LlmUsageTheme(params.themeMode, params.dynamicColor, params.appStyle) {
        CompositionLocalProvider(
            LocalGlassEnabled provides true,
            LocalGlassDark provides params.dark,
            LocalGlassController provides controller,
        ) {
            Box(Modifier.fillMaxSize()) {
                layer.topBar.value?.let { bar ->
                    GlassPanel(
                        key = "chrome-topbar",
                        config = GlassConfig.topBar(),
                        material = GlassMaterial.CHROME,
                        panelAlpha = layer.topBarAlpha.floatValue,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        classic = bar,
                        glass = bar,
                    )
                }

                layer.bottomBar.value?.let { bar ->
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                    ) {
                        GlassPanel(
                            key = "tab-capsule",
                            config = GlassConfig.tabBar(),
                            material = GlassMaterial.CHROME,
                            classic = bar,
                            glass = bar,
                        )
                    }
                }

                // 下拉刷新指示器:玻璃圆片 + spinner,随拉动进度渐显
                layer.pullRefresh.value?.let { pull ->
                    val fraction =
                        if (pull.isRefreshing) 1f
                        else pull.state.distanceFraction.coerceIn(0f, 1f)
                    if (fraction > 0.01f) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 10.dp)
                                .graphicsLayer { alpha = fraction },
                        ) {
                            GlassPanel(
                                key = "refresh-indicator",
                                config = GlassConfig.capsule(),
                                material = GlassMaterial.CHROME,
                                panelAlpha = fraction,
                                classic = {},
                                glass = {
                                    Box(
                                        Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (pull.isRefreshing) {
                                            CircularProgressIndicator(strokeWidth = 2.5.dp)
                                        } else {
                                            CircularProgressIndicator(
                                                progress = { fraction },
                                                strokeWidth = 2.5.dp,
                                                trackColor = androidx.compose.ui.graphics.Color.Transparent,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
