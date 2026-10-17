package com.skye.llmusage.ui.glass

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.qmdeve.liquidglass.widget.LiquidGlassView
import kotlin.math.roundToInt

/** 玻璃染色:RGB 为 0..1 通道值,alpha 为 0..1 不透明度 */
data class GlassTint(val r: Float, val g: Float, val b: Float, val alpha: Float)

/**
 * 玻璃面板参数。dp/px 换算由控制器统一处理;明暗两套染色随主题切换自动应用。
 *
 * 材质语义遵循 Apple Liquid Glass 双层结构:
 * - CHROME(功能层):导航栏/Tab 胶囊/FAB 等悬浮控件,由 LiquidGlassView
 *   实时折射+模糊其下方滚过的内容,叠加高光描边;
 * - CARD(内容层):不透明的"纸面"卡片,不参与玻璃采样(避免玻璃叠玻璃)。
 */
data class GlassConfig(
    val cornerRadiusDp: Float = 20f,
    val refractionHeightDp: Float = 20f,
    val refractionOffsetDp: Float = 70f,
    val blurRadiusPx: Float = 0.01f,
    val dispersion: Float = 0.4f,
    val lightTint: GlassTint = GlassTint(1f, 1f, 1f, 0.14f),
    val darkTint: GlassTint = GlassTint(0.09f, 0.10f, 0.18f, 0.32f),
) {
    companion object {
        /** 内容卡片(CARD 材质):不透明纸面,仅取圆角等形状参数 */
        fun card() = GlassConfig(cornerRadiusDp = 20f)

        /** 错误提示条(CARD 材质) */
        fun notice() = GlassConfig(cornerRadiusDp = 12f)

        /** 悬浮胶囊(CHROME 材质):单按钮/圆片控件,圆角自动成胶囊 */
        fun capsule() = GlassConfig(
            cornerRadiusDp = 99f,
            refractionHeightDp = 14f,
            refractionOffsetDp = 40f,
            blurRadiusPx = 18f,
            dispersion = 0.25f,
            lightTint = GlassTint(1f, 1f, 1f, 0.10f),
            darkTint = GlassTint(0.13f, 0.14f, 0.24f, 0.34f),
        )

        /**
         * 底部 Tab 栏(CHROME 材质):圆角矩形而非完整胶囊——Apple 用胶囊
         * 表达单一控件,用圆角矩形表达容器/栏(iOS 26 悬浮 Tab 栏形态),
         * 30dp 圆角与条目药丸(~18dp)保持同心层级
         */
        fun tabBar() = GlassConfig(
            cornerRadiusDp = 30f,
            refractionHeightDp = 14f,
            refractionOffsetDp = 40f,
            blurRadiusPx = 18f,
            dispersion = 0.25f,
            lightTint = GlassTint(1f, 1f, 1f, 0.10f),
            darkTint = GlassTint(0.13f, 0.14f, 0.24f, 0.34f),
        )

        /** 顶栏(CHROME 材质):全宽无圆角,磨砂模糊滚动内容 */
        fun topBar() = GlassConfig(
            cornerRadiusDp = 0f,
            refractionHeightDp = 18f,
            refractionOffsetDp = 55f,
            blurRadiusPx = 22f,
            dispersion = 0.28f,
            lightTint = GlassTint(1f, 1f, 1f, 0.16f),
            darkTint = GlassTint(0.10f, 0.11f, 0.20f, 0.22f),
        )
    }
}

/**
 * 玻璃层协调器:Activity 持有一个实例。
 *
 * 窗口层级(自底向上):Compose 宿主(壁纸 + 全部应用内容,即采样源) → glassHost。
 * 玻璃视图必须位于采样源的兄弟图层——库的 record() 会把采样源整体录进
 * RenderNode 且不排除玻璃自身,嵌套会造成反馈拖影。采样源为整个 Compose
 * 宿主,因此玻璃折射/模糊的是真实滚过其下方的内容(Apple 的"内容穿层"行为)。
 *
 * 视图按 key 注册,占位 Composable 通过 updateBounds 同步位置/尺寸(滚动时
 * 仅改 translation,不触发布局);回收视图进池复用;setOpacity 支持导航栏
 * "静止透明、滚动浮现"的渐隐渐现。
 */
class GlassController(context: Context, private val source: ViewGroup) {

    /** 玻璃视图宿主,位于 Compose 宿主之上 */
    val glassHost: FrameLayout

    private val density = context.resources.displayMetrics.density

    private class Entry(
        val view: LiquidGlassView,
        var config: GlassConfig,
        var w: Int = -1,
        var h: Int = -1,
        var alpha: Float = 1f,
    )

    private val entries = HashMap<Any, Entry>()
    private val pool = ArrayList<LiquidGlassView>(POOL_CAP)

    private var active = false
    private var dark = false

    init {
        glassHost = FrameLayout(context)
    }

    /** 由主组合的 SideEffect 驱动;风格开关/明暗切换时同步面板参数 */
    fun setEnabled(enabled: Boolean, dark: Boolean) {
        if (enabled == active && dark == this.dark) return
        active = enabled
        this.dark = dark
        if (enabled) {
            entries.values.forEach(::applyConfig)
        } else {
            entries.values.forEach(::recycle)
            entries.clear()
        }
    }

    fun attach(key: Any, config: GlassConfig) {
        var entry = entries[key]
        if (entry == null) {
            val view = if (pool.isNotEmpty()) pool.removeAt(pool.size - 1) else createView()
            entry = Entry(view, config)
            entries[key] = entry
            glassHost.addView(view, FrameLayout.LayoutParams(0, 0))
            view.visibility = View.GONE
        }
        entry.config = config
        if (active) applyConfig(entry)
    }

    fun detach(key: Any) {
        entries.remove(key)?.let(::recycle)
    }

    /** 占位符布局坐标(窗口系)同步到玻璃视图;尺寸变化才重排,平时仅平移 */
    fun updateBounds(key: Any, left: Float, top: Float, right: Float, bottom: Float) {
        val entry = entries[key] ?: return
        val w = (right - left).roundToInt()
        val h = (bottom - top).roundToInt()
        if (w <= 0 || h <= 0) {
            entry.view.visibility = View.GONE
            return
        }
        if (w != entry.w || h != entry.h) {
            entry.w = w
            entry.h = h
            val lp = entry.view.layoutParams
            lp.width = w
            lp.height = h
            entry.view.layoutParams = lp
        }
        entry.view.translationX = left
        entry.view.translationY = top
        entry.view.visibility = if (entry.alpha <= 0.01f) View.INVISIBLE else View.VISIBLE
    }

    /** 玻璃不透明度:0 时隐藏视图(导航栏静止态) */
    fun setOpacity(key: Any, alpha: Float) {
        val entry = entries[key] ?: return
        entry.alpha = alpha
        entry.view.alpha = alpha
        if (entry.w > 0) {
            entry.view.visibility = if (alpha <= 0.01f) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun createView(): LiquidGlassView =
        LiquidGlassView(glassHost.context).apply { bind(source) }

    private fun recycle(entry: Entry) {
        glassHost.removeView(entry.view)
        entry.view.visibility = View.GONE
        entry.view.alpha = 1f
        entry.alpha = 1f
        entry.w = -1
        entry.h = -1
        if (pool.size < POOL_CAP) pool.add(entry.view)
    }

    private fun applyConfig(entry: Entry) {
        val v = entry.view
        val c = entry.config
        v.setCornerRadius(c.cornerRadiusDp * density)
        v.setRefractionHeight(c.refractionHeightDp * density)
        v.setRefractionOffset(c.refractionOffsetDp * density)
        v.setBlurRadius(c.blurRadiusPx)
        v.setDispersion(c.dispersion)
        val tint = if (dark) c.darkTint else c.lightTint
        v.setTintColorRed(tint.r)
        v.setTintColorGreen(tint.g)
        v.setTintColorBlue(tint.b)
        v.setTintAlpha(tint.alpha)
    }

    private companion object {
        const val POOL_CAP = 8
    }
}
