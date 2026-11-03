package com.suda.yzune.wakeupschedule.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/** 两行文字（星期/日期、开始/结束时间）中间画一条分隔线 */
class MidLineTextView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    var midLineColor: Int = 0x26888888.toInt()

    private val linePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (lineCount < 2) return
        linePaint.color = midLineColor
        val y = height / 2f
        canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
    }
}
