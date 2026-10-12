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
 * 单块玻璃面板的参数。dp/px 换算由控制器统一处理;
 * 明暗两套染色随主题切换自动应用。
 */
data class GlassConfig(
    val cornerRadiusDp: Float = 24f,
    val refractionHeightDp: Float = 20f,
    val refractionOffsetDp: Float = 70f,
    val blurRadiusPx: Float = 0.01f,
    val dispersion: Float = 0.4f,
    val lightTint: GlassTint = GlassTint(1f, 1f, 1f, 0.14f),
    val darkTint: GlassTint = GlassTint(0.09f, 0.10f, 0.18f, 0.32f),
) {
    companion object {
        /** 内容卡片:仅折射不模糊,保证图表文字清晰且开销最小 */
        fun card() = GlassConfig()

        /** 顶栏/底栏:叠加一层磨砂模糊,接近 iOS 导航栏观感 */
        fun bar() = GlassConfig(
            cornerRadiusDp = 30f,
            blurRadiusPx = 12f,
            dispersion = 0.3f,
            lightTint = GlassTint(1f, 1f, 1f, 0.22f),
            darkTint = GlassTint(0.08f, 0.09f, 0.16f, 0.38f),
        )

        /** 胶囊(FAB 等小元素):圆角被库自动钳到高度一半,即成胶囊 */
        fun pill() = GlassConfig(
            cornerRadiusDp = 99f,
            refractionHeightDp = 14f,
            refractionOffsetDp = 40f,
            lightTint = GlassTint(1f, 1f, 1f, 0.20f),
            darkTint = GlassTint(0.10f, 0.11f, 0.20f, 0.40f),
        )

        /** 错误提示条 */
        fun notice() = GlassConfig(
            cornerRadiusDp = 12f,
            refractionHeightDp = 14f,
            refractionOffsetDp = 50f,
            lightTint = GlassTint(1f, 0.42f, 0.38f, 0.24f),
            darkTint = GlassTint(0.45f, 0.10f, 0.09f, 0.34f),
        )
    }
}

/**
 * 玻璃层协调器:管理背景采样源与两档玻璃宿主,Activity 持有一个实例。
 *
 * 窗口层级(自底向上):
 * backgroundSource(静态渐变,采样源) → backHost(常规玻璃) → Compose 宿主(全部应用内容)
 * → frontHost(悬浮玻璃,如 FAB,需盖在内容之上)。
 *
 * 关键约束:LiquidGlassView 不能是采样源的后代——库的 record() 会把采样源整体
 * 录进 RenderNode 且不排除玻璃自身,嵌套会造成反馈拖影,故玻璃视图全部放在
 * 独立的兄弟宿主中;采样源只含静态渐变,逐帧录制成本可忽略。
 * 视图按 key 注册,占位 Composable 通过 updateBounds 同步位置/尺寸(滚动时仅改
 * translation,不触发布局);回收的视图进池复用,避免 LazyColumn 回收重建着色器。
 */
class GlassController(context: Context) {

    /** 玻璃折射的采样源 */
    val backgroundSource: FrameLayout

    /** 常规玻璃(卡片/栏),位于内容之下 */
    val backHost: FrameLayout

    /** 悬浮玻璃(FAB),位于内容之上 */
    val frontHost: FrameLayout

    private val density = context.resources.displayMetrics.density
    private val gradient = GradientBlobView(context)

    private class Entry(
        val view: LiquidGlassView,
        var config: GlassConfig,
        var floating: Boolean,
        var w: Int = -1,
        var h: Int = -1,
    )

    private val entries = HashMap<Any, Entry>()
    private val pool = ArrayList<LiquidGlassView>(POOL_CAP)

    private var active = false
    private var dark = false

    init {
        backgroundSource = FrameLayout(context).apply {
            addView(gradient, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        backHost = FrameLayout(context)
        frontHost = FrameLayout(context)
    }

    /** 由主组合的 SideEffect 驱动;风格开关/明暗切换时同步渐变与面板参数 */
    fun setEnabled(enabled: Boolean, dark: Boolean) {
        if (enabled == active && dark == this.dark) return
        active = enabled
        this.dark = dark
        gradient.setDark(dark)
        backgroundSource.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled) {
            entries.values.forEach(::applyConfig)
        } else {
            entries.values.forEach(::recycle)
            entries.clear()
        }
    }

    fun attach(key: Any, config: GlassConfig, floating: Boolean = false) {
        var entry = entries[key]
        if (entry == null) {
            val view = if (pool.isNotEmpty()) pool.removeAt(pool.size - 1) else createView()
            entry = Entry(view, config, floating)
            entries[key] = entry
        }
        entry.config = config
        if (entry.floating != floating) {
            // 池化复用可能换宿主:先从当前父容器摘下再挂到目标层
            (entry.view.parent as? ViewGroup)?.removeView(entry.view)
            entry.floating = floating
            entry.w = -1
            entry.h = -1
        }
        if (entry.view.parent == null) {
            (if (floating) frontHost else backHost).addView(entry.view, FrameLayout.LayoutParams(0, 0))
            entry.view.visibility = View.GONE
        }
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
        entry.view.visibility = View.VISIBLE
    }

    private fun createView(): LiquidGlassView =
        LiquidGlassView(backHost.context).apply { bind(backgroundSource) }

    private fun recycle(entry: Entry) {
        (entry.view.parent as? ViewGroup)?.removeView(entry.view)
        entry.view.visibility = View.GONE
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
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val POOL_CAP = 12
    }
}
