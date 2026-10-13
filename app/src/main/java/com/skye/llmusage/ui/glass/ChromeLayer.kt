package com.skye.llmusage.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * 玻璃模式"功能层"状态桥:主组合把各屏的栏内容(顶栏/底部胶囊/FAB)写入这里,
 * 由玻璃层之上的独立 Compose 组合(chromeHost)渲染。
 *
 * 分层意义(Apple 前景永不模糊原则):LiquidGlass 面板位于主内容之上、折射/模糊
 * 滚动内容;而栏自身的图标文字又在玻璃之上,永远锐利。栏均为固定定位(顶部通栏/
 * 底部居中胶囊/右下 FAB),无需坐标同步。
 *
 * 各屏在每次重组的 SideEffect 中重新赋值 lambda,保证捕获最新状态;
 * 离开屏幕时在 DisposableEffect 中清空对应槽位。
 */
class ChromeLayer {
    val theme = mutableStateOf<ChromeThemeParams?>(null)
    val topBar = mutableStateOf<(@Composable () -> Unit)?>(null)
    val topBarAlpha = mutableFloatStateOf(1f)
    val bottomBar = mutableStateOf<(@Composable () -> Unit)?>(null)
    val fab = mutableStateOf<(@Composable () -> Unit)?>(null)

    fun clear() {
        topBar.value = null
        bottomBar.value = null
        fab.value = null
    }
}

val LocalChromeLayer = staticCompositionLocalOf<ChromeLayer?> { null }

/** chrome 组合层根:主题包装 + 固定定位槽位(顶栏/底部胶囊/FAB) */
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
                            config = GlassConfig.capsule(),
                            material = GlassMaterial.CHROME,
                            classic = bar,
                            glass = bar,
                        )
                    }
                }

                layer.fab.value?.let { fab ->
                    // 悬于胶囊之上:胶囊(约 64dp)+ 上下边距
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 20.dp, bottom = 86.dp),
                    ) {
                        fab()
                    }
                }
            }
        }
    }
}
