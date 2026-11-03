package com.suda.yzune.wakeupschedule.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/** 周课表小组件（4×2，列表形态，可上下缩放） */
class WeekWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetViews.update(context, appWidgetManager, id, WidgetViews.MODE_WEEK)
        }
        WidgetAlarm.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
                                           appWidgetId: Int, newOptions: android.os.Bundle) {
        WidgetViews.update(context, appWidgetManager, appWidgetId, WidgetViews.MODE_WEEK)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetStore.clear(context, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(WidgetViews.EXTRA_ID, -1)
        if (id <= 0) return
        when (intent.action) {
            WidgetViews.ACTION_PREV -> {
                WidgetStore.setWeekOffset(context, id, WidgetStore.weekOffset(context, id) - 1)
                WidgetViews.update(context, AppWidgetManager.getInstance(context), id, WidgetViews.MODE_WEEK)
            }
            WidgetViews.ACTION_NEXT -> {
                WidgetStore.setWeekOffset(context, id, WidgetStore.weekOffset(context, id) + 1)
                WidgetViews.update(context, AppWidgetManager.getInstance(context), id, WidgetViews.MODE_WEEK)
            }
        }
    }
}
