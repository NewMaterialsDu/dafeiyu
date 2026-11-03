package com.suda.yzune.wakeupschedule.appwidget

import com.suda.yzune.wakeupschedule.BuildConfig
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** 每天 00:05 刷一次，兜底跨天/跨周（系统 updatePeriodMillis 在 HyperOS 上不靠谱） */
object WidgetAlarm {

    private const val REQUEST = 5712
    private val ACTION_DAILY = BuildConfig.APPLICATION_ID + ".widget.DAILY"

    fun schedule(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WidgetAlarmReceiver::class.java).apply { action = ACTION_DAILY }
            val pi = PendingIntent.getBroadcast(context, REQUEST, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val cal = Calendar.getInstance().apply {
                add(Calendar.DATE, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            am.setInexactRepeating(AlarmManager.RTC, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            WidgetViews.updateAll(context)
            WidgetAlarm.schedule(context)
            com.suda.yzune.wakeupschedule.reminder.ReminderScheduler.reschedule(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
