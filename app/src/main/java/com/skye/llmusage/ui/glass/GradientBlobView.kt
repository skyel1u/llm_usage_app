package com.skye.llmusage.ui.glass

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

/**
 * 玻璃外观的全屏静态渐变背景:基础线性渐变 + 若干品牌色系径向光斑。
 * 仅在尺寸/明暗变化时重建着色器,平时不重绘;作为 LiquidGlass 的采样源,录制开销极小。
 */
class GradientBlobView(context: Context) : View(context) {

    private var dark = false
    private var basePaint = Paint()
    private var blobPaints: List<Paint> = emptyList()

    fun setDark(dark: Boolean) {
        if (this.dark == dark) return
        this.dark = dark
        rebuildShaders()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rebuildShaders()
    }

    override fun onDraw(canvas: Canvas) {
        if (blobPaints.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, basePaint)
        blobPaints.forEach { canvas.drawRect(0f, 0f, w, h, it) }
    }

    /** 光斑:(中心相对坐标 x,y) / 半径相对 min(w,h) / 中心色(边缘淡出为透明) */
    private data class Blob(val cxRatio: Float, val cyRatio: Float, val radiusRatio: Float, val color: Int)

    private fun rebuildShaders() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val baseFrom: Int
        val baseTo: Int
        val blobs: List<Blob>
        if (dark) {
            baseFrom = 0xFF0B0D1A.toInt(); baseTo = 0xFF121830.toInt()
            blobs = listOf(
                Blob(0.15f, 0.10f, 0.85f, 0x804355B9.toInt()),
                Blob(0.85f, 0.28f, 0.70f, 0x592E3B8F),
                Blob(0.55f, 0.85f, 0.80f, 0x402B6E8F),
                Blob(0.95f, 0.95f, 0.55f, 0x40314E9E),
            )
        } else {
            baseFrom = 0xFFEFF2FF.toInt(); baseTo = 0xFFDCE3FB.toInt()
            blobs = listOf(
                Blob(0.12f, 0.08f, 0.80f, 0x664355B9),
                Blob(0.88f, 0.20f, 0.65f, 0x4D7A9BF5),
                Blob(0.58f, 0.82f, 0.75f, 0x4045C4B0),
                Blob(0.95f, 0.92f, 0.50f, 0x4DE89AB0),
            )
        }

        basePaint = Paint().apply {
            shader = LinearGradient(0f, 0f, w * 0.35f, h, baseFrom, baseTo, Shader.TileMode.CLAMP)
        }
        val minDim = minOf(w, h)
        blobPaints = blobs.map { b ->
            Paint().apply {
                shader = RadialGradient(
                    b.cxRatio * w,
                    b.cyRatio * h,
                    b.radiusRatio * minDim,
                    b.color,
                    b.color and 0x00FFFFFF,
                    Shader.TileMode.CLAMP,
                )
            }
        }
        invalidate()
    }
}
