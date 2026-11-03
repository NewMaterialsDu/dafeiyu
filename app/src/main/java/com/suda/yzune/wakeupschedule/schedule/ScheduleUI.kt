package com.suda.yzune.wakeupschedule.schedule

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.ColorUtils
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.Ui
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.getPrefer
import com.suda.yzune.wakeupschedule.widget.GridLinesDrawable
import splitties.dimensions.dip
import splitties.resources.styledColor
import splitties.dimensions.dp

class ScheduleUI(override val ctx: Context, table: TableBean, day: Int, forWidget: Boolean = false) : Ui {

    private var col = 6

    var showTimeDetail = true

    val dayMap = IntArray(8)
    val itemHeight = ctx.dip(if (forWidget) table.widgetItemHeight else table.itemHeight)
    val textColor = if (forWidget) table.widgetTextColor else table.textColor

    init {
        for (i in 1..7) {
            if (!table.sundayFirst || !table.showSun) {
                if (!table.showSat && i == 7) {
                    dayMap[i] = 6
                } else {
                    dayMap[i] = i
                }
            } else {
                if (i == 7) {
                    dayMap[i] = 1
                } else {
                    dayMap[i] = i + 1
                }
            }
        }
        if (table.showSat) {
            col++
        } else {
            dayMap[6] = -1
        }
        if (table.showSun) {
            col++
        } else {
            dayMap[7] = -1
        }
    }

    val content = ConstraintLayout(ctx).apply {
        id = R.id.anko_cl_content_panel
        val timeSize = when (col) {
            7 -> 9f
            6 -> 10f
            else -> 8f
        }
        for (i in 1..table.nodes) {
            addView(FrameLayout(context).apply {
                id = R.id.anko_tv_node1 + i - 1
                if (showTimeDetail) {
                    addView(AppCompatTextView(context).apply {
                        id = R.id.tv_start
                        setTextColor(textColor)
                        //gravity = Gravity.CENTER
                        //textAlignment = View.TEXT_ALIGNMENT_CENTER
                        setSingleLine()
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, timeSize)
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                    })
                    addView(AppCompatTextView(context).apply {
                        id = R.id.tv_end
                        setTextColor(textColor)
                        setSingleLine()
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, timeSize)
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    })
                }
                addView(AppCompatTextView(context).apply {
                    setTextColor(textColor)
                    text = i.toString()
                    textSize = 12f
                    setSingleLine()
                }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                })
            }, ConstraintLayout.LayoutParams(0, itemHeight).apply {
                topMargin = dip(2)
                endToStart = R.id.anko_ll_week_panel_0
                horizontalWeight = 0.5f
                startToStart = ConstraintSet.PARENT_ID
                when (i) {
                    1 -> {
                        bottomToTop = R.id.anko_tv_node1 + i
                        topToTop = ConstraintSet.PARENT_ID
                        verticalBias = 0f
                        verticalChainStyle = ConstraintSet.CHAIN_PACKED
                    }
                    table.nodes -> {
                        //bottomToTop = R.id.anko_navigation_bar_view
                        bottomToBottom = ConstraintSet.PARENT_ID
                        topToBottom = R.id.anko_tv_node1 + i - 2
                    }
                    else -> {
                        bottomToTop = R.id.anko_tv_node1 + i
                        topToBottom = R.id.anko_tv_node1 + i - 2
                    }
                }
            })
        }

        if (!forWidget && context.getPrefer().getBoolean(Const.KEY_SCHEDULE_BLANK_AREA, true)) {
            addView(View(context), ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, itemHeight * 4).apply {
                topToBottom = R.id.anko_tv_node1 + table.nodes - 1
                bottomToBottom = ConstraintSet.PARENT_ID
                startToStart = ConstraintSet.PARENT_ID
                endToEnd = ConstraintSet.PARENT_ID
            })
        }

        // ---- 分隔线参数 ----
        val gridPrefs = context.getPrefer()
        val showGrid = !forWidget && gridPrefs.getBoolean(Const.KEY_SHOW_GRID_LINE, true)
        val customColor = gridPrefs.getInt(Const.KEY_GRID_LINE_COLOR, Const.GRID_LINE_COLOR_FOLLOW_TEXT)
        val gridWidth = ctx.dip(gridPrefs.getInt(Const.KEY_GRID_LINE_WIDTH, 1)).toFloat()
        val gridStyle = gridPrefs.getInt(Const.KEY_GRID_LINE_STYLE, Const.GRID_LINE_STYLE_SOLID)
        val dashOnCourse = gridPrefs.getBoolean(Const.KEY_GRID_LINE_DASH_ON_COURSE, true)
        // 「跟随文字色」跟课表自己的文字色，换配色方案后浅底/深底都能看见
        val gridColor = if (customColor == Const.GRID_LINE_COLOR_FOLLOW_TEXT) {
            ColorUtils.setAlphaComponent(textColor, 48)
        } else {
            customColor
        }
        fun addGridLayer(dashed: Boolean, onTop: Boolean) {
            addView(View(context).apply {
                background = GridLinesDrawable(
                        nodes = table.nodes,
                        itemHeight = itemHeight,
                        marTop = ctx.dip(2),
                        lineColor = gridColor,
                        cols = col - 1,
                        strokeWidth = gridWidth,
                        drawHorizontal = gridStyle != Const.GRID_LINE_STYLE_VERTICAL,
                        drawVertical = gridStyle != Const.GRID_LINE_STYLE_HORIZONTAL,
                        dashed = dashed,
                        // 竖线从第一条横线开始，避免顶部冒出一截「穿出表格」的竖线
                        verticalTop = ctx.dip(2).toFloat()
                )
            }, ConstraintLayout.LayoutParams(0, 0).apply {
                startToStart = R.id.anko_ll_week_panel_0
                endToEnd = ConstraintSet.PARENT_ID
                topToTop = ConstraintSet.PARENT_ID
                bottomToBottom = R.id.anko_tv_node1 + table.nodes - 1
            })
        }

        // ---- 底层实线：空格子里看得见，被课程块盖住 ----
        if (showGrid) addGridLayer(dashed = gridStyle == Const.GRID_LINE_STYLE_DASHED, onTop = false)

        // ---- 结构：每列是一个 FrameLayout 容器，课程块作为它的子 View ----
        for (i in 0 until col - 1) {
            addView(FrameLayout(context).apply { id = R.id.anko_ll_week_panel_0 + i }, ConstraintLayout.LayoutParams(0,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                // 不再给每列留 1dp 边距：课程块要严格填满格子，竖分隔线也正好落在列边界上
                horizontalWeight = 1f
                when (i) {
                    0 -> {
                        startToEnd = R.id.anko_tv_node1
                        endToStart = R.id.anko_ll_week_panel_0 + i + 1
                    }
                    col - 2 -> {
                        startToEnd = R.id.anko_ll_week_panel_0 + i - 1
                        endToEnd = ConstraintSet.PARENT_ID
                    }
                    else -> {
                        startToEnd = R.id.anko_ll_week_panel_0 + i - 1
                        endToStart = R.id.anko_ll_week_panel_0 + i + 1
                    }
                }
            })
        }

        // ---- 顶层虚线：只在课程块上看得出来（空格子被底层实线压住，视觉仍是实线）----
        if (showGrid && dashOnCourse) {
            addGridLayer(dashed = true, onTop = true)
        }
    }

    val scrollView = ScrollView(ctx).apply {
        id = R.id.anko_sv_schedule
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        addView(content)
    }

    override val root = ConstraintLayout(ctx).apply {
        val textAlphaColor = ColorUtils.setAlphaComponent(textColor, (0.32 * (textColor shr 24 and 0xff)).toInt())
        for (i in 0 until col) {
            addView(AppCompatTextView(context).apply {
                id = R.id.anko_tv_title0 + i
                setPadding(0, dip(8), 0, dip(8))
                textSize = 12f
                gravity = Gravity.CENTER
                setLineSpacing(dp(2), 1f)
                if (i == 0 || (day > 0 && i == dayMap[day])) {
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(textColor)
                } else {
                    setTextColor(textAlphaColor)
                }
            }, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                when (i) {
                    0 -> {
                        horizontalWeight = 0.5f
                        startToStart = ConstraintSet.PARENT_ID
                        topToTop = ConstraintSet.PARENT_ID
                        endToStart = R.id.anko_tv_title0 + i + 1
                    }
                    col - 1 -> {
                        horizontalWeight = 1f
                        startToEnd = R.id.anko_tv_title0 + i - 1
                        endToEnd = ConstraintSet.PARENT_ID
                        baselineToBaseline = R.id.anko_tv_title0 + i - 1
                    }
                    else -> {
                        horizontalWeight = 1f
                        startToEnd = R.id.anko_tv_title0 + i - 1
                        endToStart = R.id.anko_tv_title0 + i + 1
                        baselineToBaseline = R.id.anko_tv_title0 + i - 1
                    }
                }
            })
        }

        addView(scrollView, ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT).apply {
            bottomToBottom = ConstraintSet.PARENT_ID
            topToBottom = R.id.anko_tv_title0
            startToStart = ConstraintSet.PARENT_ID
            endToEnd = ConstraintSet.PARENT_ID
        })
    }
}
