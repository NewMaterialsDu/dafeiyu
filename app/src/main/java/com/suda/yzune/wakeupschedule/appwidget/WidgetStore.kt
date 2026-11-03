package com.suda.yzune.wakeupschedule.appwidget

import android.content.Context
import androidx.core.content.edit
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.getPrefer

/** 小组件的本地状态：绑定哪张课表、当前看的是第几周/哪一天 */
object WidgetStore {

    private fun keyTable(id: Int) = "widget_table_$id"
    private fun keyWeekOffset(id: Int) = "widget_week_off_$id"
    private fun keyDayOffset(id: Int) = "widget_day_off_$id"

    /** 绑定的课表 id；null = 没绑，用默认课表 */
    fun tableId(context: Context, appWidgetId: Int): Int? {
        val v = context.getPrefer().getString(keyTable(appWidgetId), "") ?: ""
        return v.toIntOrNull()
    }

    fun setTableId(context: Context, appWidgetId: Int, tableId: Int) {
        context.getPrefer().edit { putString(keyTable(appWidgetId), tableId.toString()) }
    }

    fun weekOffset(context: Context, appWidgetId: Int): Int =
            context.getPrefer().getInt(keyWeekOffset(appWidgetId), 0)

    fun setWeekOffset(context: Context, appWidgetId: Int, offset: Int) {
        context.getPrefer().edit { putInt(keyWeekOffset(appWidgetId), offset) }
    }

    fun dayOffset(context: Context, appWidgetId: Int): Int =
            context.getPrefer().getInt(keyDayOffset(appWidgetId), 0)

    fun setDayOffset(context: Context, appWidgetId: Int, offset: Int) {
        context.getPrefer().edit { putInt(keyDayOffset(appWidgetId), offset) }
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getPrefer().edit {
            remove(keyTable(appWidgetId))
            remove(keyWeekOffset(appWidgetId))
            remove(keyDayOffset(appWidgetId))
        }
    }

    /** 小组件里是否显示「已经上完」的课（设置里可关） */
    fun showPast(context: Context): Boolean =
            context.getPrefer().getBoolean(Const.KEY_WIDGET_SHOW_PAST, true)
}
