package com.suda.yzune.wakeupschedule.appwidget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.bean.TableBean

/** 给两个小组件的 ListView 提供课程行（格式跟 App 里的今日课程一致） */
class WidgetListService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        val id = intent?.getIntExtra(WidgetViews.EXTRA_ID, 0) ?: 0
        val mode = intent?.getStringExtra(WidgetViews.EXTRA_MODE) ?: WidgetViews.MODE_WEEK
        return WidgetFactory(applicationContext, id, mode)
    }

    private class WidgetFactory(
            private val context: Context,
            private val appWidgetId: Int,
            private val mode: String
    ) : RemoteViewsFactory {

        private var table: TableBean? = null
        private var rows: List<WidgetCourseRow> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            val t = WidgetData.table(context, appWidgetId)
            table = t
            val showPast = WidgetStore.showPast(context)
            rows = if (mode == WidgetViews.MODE_WEEK) {
                WidgetData.rowsForWeek(context, t, WidgetStore.weekOffset(context, appWidgetId), showPast).second
            } else {
                WidgetData.rowsForToday(context, t, WidgetStore.dayOffset(context, appWidgetId), showPast).second
            }
        }

        override fun onDestroy() {
            rows = emptyList()
        }

        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.item_widget_course)
            if (position !in rows.indices) return views
            val row = rows[position]
            val t = table
            val colors = t?.let { WidgetViews.resolveTextColors(context, it) }
            val textColor = colors?.text ?: Color.WHITE
            val nameColor = colors?.name ?: Color.WHITE
            val size = (t?.widgetItemTextSize ?: 12).toFloat()

            views.setTextViewTextSize(R.id.tv_name, TypedValue.COMPLEX_UNIT_SP, size)
            views.setTextViewTextSize(R.id.tv_time, TypedValue.COMPLEX_UNIT_SP, (size - 3).coerceAtLeast(8f))
            views.setTextViewTextSize(R.id.tv_day, TypedValue.COMPLEX_UNIT_SP, (size - 3).coerceAtLeast(8f))
            views.setTextViewTextSize(R.id.tv_room, TypedValue.COMPLEX_UNIT_SP, (size - 4).coerceAtLeast(8f))

            views.setTextViewText(R.id.tv_day, row.dayLabel)
            views.setTextViewText(R.id.tv_time, row.time)
            views.setTextViewText(R.id.tv_name, row.name)
            views.setTextViewText(R.id.tv_room, row.room)
            views.setViewVisibility(R.id.tv_day, if (mode == WidgetViews.MODE_WEEK) View.VISIBLE else View.GONE)

            val alpha = if (row.past) 0x66 else 0xFF
            views.setTextColor(R.id.tv_time, withAlpha(textColor, alpha))
            views.setTextColor(R.id.tv_day, withAlpha(textColor, alpha))
            views.setTextColor(R.id.tv_room, withAlpha(textColor, alpha))
            views.setTextColor(R.id.tv_name, withAlpha(nameColor, alpha))
            views.setInt(R.id.v_strip, "setBackgroundColor", withAlpha(row.color, alpha))

            val fill = Intent().apply {
                putExtra("widgetDay", row.dayLabel)
                putExtra("widgetCourse", row.name)
            }
            views.setOnClickFillInIntent(R.id.ll_row, fill)
            return views
        }

        private fun withAlpha(color: Int, alpha: Int): Int =
                (color and 0x00FFFFFF) or (alpha shl 24)

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = false
    }
}
