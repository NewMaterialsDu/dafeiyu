package com.suda.yzune.wakeupschedule.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.util.LruCache
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.schedule.ScheduleActivity
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.CourseUtils
import com.suda.yzune.wakeupschedule.utils.getPrefer

/**
 * 小组件渲染入口。两种组件都是「标题 + 课程列表」，列表项格式跟 App 里的今日课程一致。
 * 背景支持：跟随课表 / 纯色 / 内置图 / 透明，并自动适配文字颜色。
 */
object WidgetViews {

    const val MODE_WEEK = "week"
    const val MODE_TODAY = "today"

    const val EXTRA_ID = "appWidgetId"
    const val EXTRA_MODE = "widgetMode"

    const val ACTION_PREV = "fun.wakeup.local.widget.PREV"
    const val ACTION_NEXT = "fun.wakeup.local.widget.NEXT"

    /** 背景算出来之后实际要用的文字颜色 */
    data class TextColors(val text: Int, val name: Int)

    fun updateAll(context: Context) {
        updateMode(context, MODE_WEEK)
        updateMode(context, MODE_TODAY)
    }

    fun updateMode(context: Context, mode: String) {
        val manager = AppWidgetManager.getInstance(context)
        val cls = if (mode == MODE_WEEK) WeekWidgetProvider::class.java else TodayWidgetProvider::class.java
        val ids = manager.getAppWidgetIds(ComponentName(context, cls))
        for (id in ids) {
            update(context, manager, id, mode)
        }
    }

    fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int, mode: String) {
        try {
            val table = WidgetData.table(context, appWidgetId)
            val views = if (mode == MODE_WEEK) {
                buildWeek(context, table, appWidgetId)
            } else {
                buildToday(context, table, appWidgetId)
            }
            val serviceIntent = Intent(context, WidgetListService::class.java).apply {
                putExtra(EXTRA_ID, appWidgetId)
                putExtra(EXTRA_MODE, mode)
                data = Uri.parse("wakeupwidget://$mode/$appWidgetId/${System.currentTimeMillis()}")
            }
            views.setRemoteAdapter(R.id.lv_courses, serviceIntent)
            views.setEmptyView(R.id.lv_courses, R.id.tv_empty)

            val openIntent = Intent(context, ScheduleActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPi = PendingIntent.getActivity(context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setPendingIntentTemplate(R.id.lv_courses, openPi)
            views.setOnClickPendingIntent(R.id.ll_root, openPi)
            views.setOnClickPendingIntent(R.id.tv_prev, actionPi(context, appWidgetId, mode, ACTION_PREV))
            views.setOnClickPendingIntent(R.id.tv_next, actionPi(context, appWidgetId, mode, ACTION_NEXT))

            manager.updateAppWidget(appWidgetId, views)
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_courses)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun actionPi(context: Context, appWidgetId: Int, mode: String, action: String): PendingIntent {
        val intent = Intent(context, if (mode == MODE_WEEK) WeekWidgetProvider::class.java else TodayWidgetProvider::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(context, appWidgetId * 10 + action.hashCode() % 10, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** 按背景设置决定文字/课名颜色（列表行也用这套） */
    fun resolveTextColors(context: Context, table: TableBean): TextColors {
        val key = WidgetBackgrounds.get(context)
        return when {
            key == WidgetBackgrounds.TRANSPARENT -> TextColors(table.widgetTextColor, table.widgetCourseTextColor)
            WidgetBackgrounds.find(key).imageRes != 0 -> {
                // 内置图：加暗色蒙层 + 白字
                TextColors(WidgetBackgrounds.LIGHT_TEXT, WidgetBackgrounds.LIGHT_TEXT)
            }
            key == WidgetBackgrounds.FOLLOW -> {
                val img = WidgetBackgrounds.followImageRes(table)
                if (img != 0) {
                    TextColors(WidgetBackgrounds.LIGHT_TEXT, WidgetBackgrounds.LIGHT_TEXT)
                } else {
                    val lum = WidgetBackgrounds.luminance(WidgetBackgrounds.followColor(context, table))
                    TextColors(
                            WidgetBackgrounds.adaptTextColor(table.widgetTextColor, lum),
                            WidgetBackgrounds.adaptTextColor(table.widgetCourseTextColor, lum))
                }
            }
            else -> {
                val opt = WidgetBackgrounds.find(key)
                if (opt.lightText) {
                    TextColors(WidgetBackgrounds.LIGHT_TEXT, WidgetBackgrounds.LIGHT_TEXT)
                } else {
                    val lum = WidgetBackgrounds.luminance(opt.color)
                    TextColors(
                            WidgetBackgrounds.adaptTextColor(table.widgetTextColor, lum),
                            WidgetBackgrounds.adaptTextColor(table.widgetCourseTextColor, lum))
                }
            }
        }
    }

    /** 背景位图缓存：远程视图每次更新都重新编码会很慢，按 背景+尺寸+圆角 缓存 */
    private val bgCache = LruCache<String, Bitmap>(8)

    /** 把背景（带自定义圆角）画到 RemoteViews 上 */
    private fun applyBackground(context: Context, manager: AppWidgetManager?, appWidgetId: Int,
                                views: RemoteViews, table: TableBean, colors: TextColors) {
        views.setInt(R.id.ll_root, "setBackgroundColor", Color.TRANSPARENT)
        views.setInt(R.id.ll_content, "setBackgroundColor", Color.TRANSPARENT)
        // 空态文字用同一套颜色（列表行颜色在 WidgetListService 里设置）
        views.setTextColor(R.id.tv_empty, colors.text)

        val key = WidgetBackgrounds.get(context)
        if (key == WidgetBackgrounds.TRANSPARENT) {
            views.setViewVisibility(R.id.iv_bg, View.GONE)
            return
        }
        val opt = WidgetBackgrounds.find(key)
        val imageRes = when {
            opt.imageRes != 0 -> opt.imageRes
            key == WidgetBackgrounds.FOLLOW -> WidgetBackgrounds.followImageRes(table)
            else -> 0
        }
        val color = if (imageRes != 0 || key != WidgetBackgrounds.FOLLOW) {
            if (imageRes == 0) opt.color else 0
        } else {
            WidgetBackgrounds.followColor(context, table)
        }
        val cornerDp = context.getPrefer().getInt(Const.KEY_WIDGET_CORNER, 16)
        val bitmap = buildBackgroundBitmap(context, manager, appWidgetId, color, imageRes, cornerDp)
        if (bitmap != null) {
            views.setViewVisibility(R.id.iv_bg, View.VISIBLE)
            views.setImageViewBitmap(R.id.iv_bg, bitmap)
        } else {
            // 拿不到位图就退回纯色（没有圆角，但不至于空着）
            views.setViewVisibility(R.id.iv_bg, View.GONE)
            views.setInt(R.id.ll_root, "setBackgroundColor", if (color != 0) color else Color.TRANSPARENT)
        }
    }

    private fun widgetSizeDp(context: Context, manager: AppWidgetManager?, appWidgetId: Int): Pair<Int, Int> {
        var w = 250
        var h = 110
        try {
            if (manager != null) {
                val opts = manager.getAppWidgetOptions(appWidgetId)
                val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                val maxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                if (minW > 0) w = minW
                if (maxH > 0) h = maxH
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(w, h)
    }

    /**
     * 生成带圆角的背景位图。
     * 圆角要清晰就**必须按小部件的实际像素尺寸渲染**（之前压到 180px 再放大，圆角就是锯齿/模糊的），
     * 只用「总字节数」控制上限（RemoteViews 单次传输约 1MB），在预算内尽量接近原分辨率。
     * 用 drawRoundRect + 抗锯齿画笔直接画，不用 clipPath，边缘才会平滑。
     */
    private fun buildBackgroundBitmap(context: Context, manager: AppWidgetManager?, appWidgetId: Int,
                                      color: Int, imageRes: Int, cornerDp: Int): Bitmap? {
        return try {
            val density = context.resources.displayMetrics.density
            val (wDp, hDp) = widgetSizeDp(context, manager, appWidgetId)
            var w = (wDp * density).toInt().coerceAtLeast(2)
            var h = (hDp * density).toInt().coerceAtLeast(2)
            // 600KB 预算内尽量保持原分辨率
            val budgetPixels = (600 * 1024) / 4
            if (w * h > budgetPixels) {
                val f = kotlin.math.sqrt(budgetPixels.toDouble() / (w * h).toDouble()).toFloat()
                w = (w * f).toInt().coerceAtLeast(2)
                h = (h * f).toInt().coerceAtLeast(2)
            }
            // dp→px 跟着同样的比例缩放
            val factor = w / (wDp * density).coerceAtLeast(1f)
            val key = "$color|$imageRes|$w|$h|$cornerDp"
            bgCache.get(key)?.let { return it }

            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val radius = (cornerDp * density * factor).coerceAtMost(minOf(w, h) / 2f)
            val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            if (imageRes != 0) {
                val src = BitmapFactory.decodeResource(context.resources, imageRes)
                if (src == null) {
                    paint.color = if (color != 0) color else Color.TRANSPARENT
                } else {
                    val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    val scale = maxOf(w / src.width.toFloat(), h / src.height.toFloat())
                    val matrix = Matrix().apply {
                        setScale(scale, scale)
                        postTranslate((w - src.width * scale) / 2f, (h - src.height * scale) / 2f)
                    }
                    shader.setLocalMatrix(matrix)
                    paint.shader = shader
                }
            } else {
                paint.color = if (color != 0) color else Color.TRANSPARENT
            }
            canvas.drawRoundRect(rect, radius, radius, paint)
            if (imageRes != 0) {
                // 图上蒙一层暗色，保证白字能看清
                val scrim = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = 0x4D000000 }
                canvas.drawRoundRect(rect, radius, radius, scrim)
            }
            bgCache.put(key, bitmap)
            bitmap
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun dim(hex: Int) = hex

    private fun buildWeek(context: Context, table: TableBean, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_week)
        val colors = resolveTextColors(context, table)
        applyBackground(context, AppWidgetManager.getInstance(context), appWidgetId, views, table, colors)

        val offset = WidgetStore.weekOffset(context, appWidgetId)
        val (week, rows) = WidgetData.rowsForWeek(context, table, offset, WidgetStore.showPast(context))
        val title = if (rows.isEmpty() && offset == 0) {
            "第${week}周  ·  这周没课"
        } else {
            "第${week}周  ·  ${WidgetData.weekRangeText(table, week)}"
        }
        val size = table.widgetItemTextSize.toFloat()
        views.setTextViewText(R.id.tv_title, title)
        views.setTextColor(R.id.tv_title, colors.text)
        views.setTextColor(R.id.tv_prev, colors.text)
        views.setTextColor(R.id.tv_next, colors.text)
        views.setTextViewTextSize(R.id.tv_title, TypedValue.COMPLEX_UNIT_SP, size + 1)
        views.setTextViewTextSize(R.id.tv_prev, TypedValue.COMPLEX_UNIT_SP, size + 8)
        views.setTextViewTextSize(R.id.tv_next, TypedValue.COMPLEX_UNIT_SP, size + 8)
        views.setTextViewTextSize(R.id.tv_empty, TypedValue.COMPLEX_UNIT_SP, size - 1)
        views.setTextViewText(R.id.tv_empty, "这周没有课哦")
        return views
    }

    private fun buildToday(context: Context, table: TableBean, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        val colors = resolveTextColors(context, table)
        applyBackground(context, AppWidgetManager.getInstance(context), appWidgetId, views, table, colors)

        val offset = WidgetStore.dayOffset(context, appWidgetId)
        val (week, _) = WidgetData.rowsForToday(context, table, offset, WidgetStore.showPast(context))
        val day = WidgetData.dayName(CourseUtils.getWeekdayInt(offset > 0))
        val size = table.widgetItemTextSize.toFloat()
        views.setTextViewText(R.id.tv_title, WidgetData.todayTitle(offset) + "  " + if (offset > 0) "明天" else "今天")
        views.setTextViewText(R.id.tv_subtitle, "第${week}周  ${day}")
        views.setTextColor(R.id.tv_title, colors.text)
        views.setTextColor(R.id.tv_subtitle, colors.text)
        views.setTextColor(R.id.tv_prev, colors.text)
        views.setTextColor(R.id.tv_next, colors.text)
        views.setTextViewTextSize(R.id.tv_title, TypedValue.COMPLEX_UNIT_SP, size + 2)
        views.setTextViewTextSize(R.id.tv_subtitle, TypedValue.COMPLEX_UNIT_SP, size - 1)
        views.setTextViewTextSize(R.id.tv_prev, TypedValue.COMPLEX_UNIT_SP, size + 8)
        views.setTextViewTextSize(R.id.tv_next, TypedValue.COMPLEX_UNIT_SP, size + 8)
        views.setTextViewTextSize(R.id.tv_empty, TypedValue.COMPLEX_UNIT_SP, size - 1)
        views.setTextViewText(R.id.tv_empty, if (offset > 0) "明天没有课哦" else "今天没有课，好好休息~")
        return views
    }
}
