package com.suda.yzune.wakeupschedule.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import splitties.dimensions.dp

@SuppressLint("ViewConstructor")
class TipTextView(context: Context) : View(context) {

    var tipVisibility = 0
        set(value) {
            field = value
            invalidate()
        }

    private var text = ""
    private var mStaticLayout: StaticLayout? = null
    private lateinit var mTextPaint: TextPaint
    private lateinit var mPaint: Paint
    private lateinit var bgPaint: Paint
    private lateinit var strokePaint: Paint
    private val path = Path()
    private val rect = RectF()
    private val strokeRect = RectF()
    private val bgPath = Path()
    private val strokePath = Path()
    private val dpUnit = dp(1)
    // 四个角分别可设圆角：只有「露在外面」的角才圆，贴着相邻课程的角保持直角，
    // 这样课程块既铺满格子又不会在相邻处裂出背景缝
    private var radiusTl = 0
    private var radiusTr = 0
    private var radiusBr = 0
    private var radiusBl = 0

    fun setCornerRadii(tl: Int, tr: Int, br: Int, bl: Int) {
        radiusTl = tl
        radiusTr = tr
        radiusBr = br
        radiusBl = bl
        invalidate()
    }

    private fun buildPath(path: Path, area: RectF) {
        path.reset()
        path.addRoundRect(area, floatArrayOf(
                radiusTl * dpUnit, radiusTl * dpUnit,
                radiusTr * dpUnit, radiusTr * dpUnit,
                radiusBr * dpUnit, radiusBr * dpUnit,
                radiusBl * dpUnit, radiusBl * dpUnit
        ), Path.Direction.CW)
    }

    private var otherWeekTextAlpha = 255
    private var otherWeekBgAlpha = 255
    private var otherWeekStrokeAlpha = 255

    fun init(text: String, txtSize: Int, txtColor: Int, bgColor: Int, bgAlpha: Int, stroke: Int) {
        this.text = text
        mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            // textSize = mSize * resources.displayMetrics.scaledDensity
            textSize = txtSize * dpUnit
            typeface = Typeface.DEFAULT_BOLD
            color = txtColor
        }
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = txtColor
            isDither = true
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2 * dpUnit
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            isDither = true
            style = Paint.Style.FILL
            alpha = bgAlpha
        }
        strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke
            isDither = true
            style = Paint.Style.STROKE
            strokeWidth = 2 * dpUnit
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        otherWeekTextAlpha = (mTextPaint.alpha * 0.55).toInt()
        otherWeekBgAlpha = (bgPaint.alpha * 0.4).toInt()
        otherWeekStrokeAlpha = (strokePaint.alpha * 0.4).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Tell the parent layout how big this view would like to be
        // but still respect any requirements (measure specs) that are passed down.

        // determine the width
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // determine the height
        val height = MeasureSpec.getSize(heightMeasureSpec)
        // 整块铺满自己的格子（原来四周内缩 1dp，看起来比格子小一圈）
        rect.left = 0f
        rect.right = width.toFloat()
        rect.top = 0f
        rect.bottom = height.toFloat()
        // Required call: set width and height
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 背景也只画在自己的范围里：抗锯齿的圆弧/边缘有时会溢出 1~2px 到相邻格子，
        // 在部分密度/字体的机型上会看到「课程压到格子线外」。
        canvas.save()
        canvas.clipRect(rect)
        if (tipVisibility == TIP_OTHER_WEEK) {
            mTextPaint.alpha = otherWeekTextAlpha
            mPaint.alpha = otherWeekTextAlpha
            strokePaint.alpha = otherWeekStrokeAlpha
            bgPaint.alpha = otherWeekBgAlpha
        }
        if (mStaticLayout == null) {
            // 按当前块高限制行数：字大/块矮的机型上文字不会挤满整块，末尾用省略号
            val avail = height - paddingTop - paddingBottom
            val lineHeight = mTextPaint.fontMetricsInt.let { it.descent - it.ascent } + 1
            val maxLines = (avail / lineHeight.coerceAtLeast(1)).coerceAtLeast(1)
            mStaticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout
                        .Builder
                        .obtain(text, 0, text.length, mTextPaint, width - paddingRight - paddingLeft)
                        .setIncludePad(false)
                        .setMaxLines(maxLines)
                        .setEllipsize(android.text.TextUtils.TruncateAt.END)
                        .build()
            } else {
                StaticLayout(
                        text,
                        mTextPaint,
                        width - paddingRight - paddingLeft,
                        Layout.Alignment.ALIGN_NORMAL,
                        1.0f,
                        0f,
                        false
                )
            }
        }
        buildPath(bgPath, rect)
        canvas.drawPath(bgPath, bgPaint)
        // 不再在块内部画一圈边框：那一圈浅色描边会让课程看起来「缩在格子里」。
        // 格子线由课表底层的 GridLinesDrawable 统一画在格子边界上。
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        mStaticLayout!!.draw(canvas)
        canvas.restore()
        if (tipVisibility == 1) {
            path.moveTo(width - 12 * dpUnit, height - 6 * dpUnit) // 此点为多边形的起点
            path.lineTo(width - 6 * dpUnit, height - 6 * dpUnit)
            path.lineTo(width - 6 * dpUnit, height - 12 * dpUnit)
            path.close() // 使这些点构成封闭的多边形
            canvas.drawPath(path, mPaint)
        } else if (tipVisibility == -1) {
            canvas.drawLine(width - 12 * dpUnit,
                    height - 6 * dpUnit,
                    width - 6 * dpUnit,
                    height - 12 * dpUnit, mPaint)
            canvas.drawLine(width - 6 * dpUnit,
                    height - 6 * dpUnit,
                    width - 12 * dpUnit,
                    height - 12 * dpUnit, mPaint)
        }
    }

    companion object {
        const val TIP_INVISIBLE = 0
        const val TIP_VISIBLE = 1
        const val TIP_ERROR = -1
        const val TIP_OTHER_WEEK = 2
    }
}
