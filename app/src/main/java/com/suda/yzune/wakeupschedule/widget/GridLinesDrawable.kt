package com.suda.yzune.wakeupschedule.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * 课表的分隔线。横线每节一条、竖线每列一条。
 * 支持：线色、线宽、实线/虚线、只横只竖，以及「竖线从第一条横线开始画」
 * （否则线层顶部会冒出 5px 竖线，看起来像竖线穿出了表格）。
 */
class GridLinesDrawable(
        private val nodes: Int,
        private val itemHeight: Int,
        private val marTop: Int,
        lineColor: Int,
        private val cols: Int = 7,
        strokeWidth: Float = 1f,
        private val drawHorizontal: Boolean = true,
        private val drawVertical: Boolean = true,
        dashed: Boolean = false,
        private val verticalTop: Float = 0f
) : Drawable() {

    private val paint = Paint().apply {
        color = lineColor
        this.strokeWidth = strokeWidth
        isAntiAlias = true
        style = Paint.Style.STROKE
        if (dashed) {
            val dash = (strokeWidth * 4f).coerceAtLeast(2f)
            pathEffect = DashPathEffect(floatArrayOf(dash, dash), 0f)
        }
    }

    override fun draw(canvas: Canvas) {
        val h = bounds.height().toFloat()
        val w = bounds.width().toFloat()
        // 节与节之间的横线
        if (drawHorizontal) {
            for (i in 0 until nodes) {
                val y = (marTop + (itemHeight + marTop) * i).toFloat()
                if (y > h) break
                canvas.drawLine(0f, y, w, y, paint)
            }
        }
        // 列与列之间的竖线（含最右侧收边）
        if (drawVertical && cols > 0) {
            val top = verticalTop.coerceIn(0f, h)
            val colW = w / cols
            for (i in 0..cols) {
                val x = (colW * i).coerceAtMost(w)
                canvas.drawLine(x, top, x, h, paint)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
