package com.skye.llmusage.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 玻璃外观的全屏静态"壁纸":基础线性渐变 + 品牌色系径向光斑。
 * 绘制在 Compose 宿主最底层,随应用内容一起被玻璃面板采样,
 * 因此悬浮玻璃在空白区域同样折射到色彩。静态无动画,不产生重绘开销。
 */
@Composable
fun LiquidWallpaper(modifier: Modifier = Modifier, dark: Boolean) {
    Canvas(modifier.fillMaxSize()) {
        val (baseFrom, baseTo, blobs) = if (dark) DarkSpec else LightSpec
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(baseFrom, baseTo),
                start = Offset.Zero,
                end = Offset(size.width * 0.35f, size.height),
            ),
        )
        val minDim = minOf(size.width, size.height)
        blobs.forEach { spec ->
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.color,
                        Color(spec.color.toArgb() and 0x00FFFFFF),
                    ),
                    center = Offset(spec.cxR * size.width, spec.cyR * size.height),
                    radius = spec.radiusR * minDim,
                ),
            )
        }
    }
}

/** 光斑:(中心相对坐标 x,y) / 半径相对 min(w,h) / 中心色(边缘淡出为透明) */
private data class BlobSpec(val cxR: Float, val cyR: Float, val radiusR: Float, val color: Color)

private val LightSpec = Triple(
    Color(0xFFEFF2FF),
    Color(0xFFDCE3FB),
    listOf(
        BlobSpec(0.12f, 0.08f, 0.80f, Color(0x664355B9)),
        BlobSpec(0.88f, 0.20f, 0.65f, Color(0x4D7A9BF5)),
        BlobSpec(0.58f, 0.82f, 0.75f, Color(0x4045C4B0)),
        BlobSpec(0.95f, 0.92f, 0.50f, Color(0x4DE89AB0)),
    ),
)

private val DarkSpec = Triple(
    Color(0xFF05060F),
    Color(0xFF0C1124),
    listOf(
        // 深色壁纸:基底近黑,色块为低透明度的深色辉光,保持底层最暗(iOS 抬升层级的前提)
        BlobSpec(0.08f, 0.05f, 0.70f, Color(0x594355B9)),
        BlobSpec(0.90f, 0.18f, 0.55f, Color(0x363D5BD9)),
        BlobSpec(0.55f, 0.90f, 0.65f, Color(0x2E1E6E85)),
        BlobSpec(0.93f, 0.97f, 0.50f, Color(0x249A6B4F)),
    ),
)
