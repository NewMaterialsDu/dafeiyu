package com.suda.yzune.wakeupschedule.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/** 今日课程小组件（4×1，列表形态，可上下缩放看更多课） */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetViews.update(context, appWidgetManager, id, WidgetViews.MODE_TODAY)
        }
        WidgetAlarm.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
                                           appWidgetId: Int, newOptions: android.os.Bundle) {
        WidgetViews.update(context, appWidgetManager, appWidgetId, WidgetViews.MODE_TODAY)
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
                WidgetStore.setDayOffset(context, id, 0)
                WidgetViews.update(context, AppWidgetManager.getInstance(context), id, WidgetViews.MODE_TODAY)
            }
            WidgetViews.ACTION_NEXT -> {
                WidgetStore.setDayOffset(context, id, 1)
                WidgetViews.update(context, AppWidgetManager.getInstance(context), id, WidgetViews.MODE_TODAY)
            }
        }
    }
}
