package com.suda.yzune.wakeupschedule.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.SplashActivity
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.CourseUtils
import com.suda.yzune.wakeupschedule.utils.getPrefer
import java.util.Calendar

/**
 * 上课提醒。
 *
 * 旧版提醒逻辑挂在「日视图小组件」的 Provider 里，小组件下线后提醒也跟着失效了（开关白点）。
 * 这里重写成独立实现：按当天课表把「上课前 N 分钟」的闹钟排好，不依赖任何小组件。
 * HyperOS 上后台限制较严，除了闹钟还会在打开 App / 每天 00:05 / 开机时重新排一遍。
 */
object ReminderScheduler {

    private const val ACTION_REMIND = "fun.wakeup.local.REMIND"
    private const val REQ_BASE = 9000
    private const val MAX_ALARM = 20

    /** 取消旧闹钟后按当前课表重新排 */
    fun reschedule(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val exists = PendingIntent.getBroadcast(context, REQ_BASE,
                    Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMIND },
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (exists != null) {
                for (i in 0 until MAX_ALARM) {
                    val pi = PendingIntent.getBroadcast(context, REQ_BASE + i,
                            Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMIND },
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                    if (pi != null) {
                        am.cancel(pi)
                        pi.cancel()
                    }
                }
            }
            if (!context.getPrefer().getBoolean(Const.KEY_COURSE_REMIND, false)) return

            val db = AppDatabase.getDatabase(context)
            val table = db.tableDao().getDefaultTableSync()
            val week = try {
                CourseUtils.countWeek(table.startDate, table.sundayFirst)
            } catch (e: Exception) {
                1
            }
            if (week < 1 || week > table.maxWeek) return
            val day = CourseUtils.getWeekdayInt()
            val type = if (week % 2 == 0) 2 else 1
            val courses = db.courseDao().getCourseByDayOfTableSync(day, week, type, table.id).sortedBy { it.startNode }
            if (courses.isEmpty()) return
            val times = db.timeDetailDao().getTimeListSync(table.timeTable)
            val advance = context.getPrefer().getInt(Const.KEY_REMINDER_TIME, 20)
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
            var index = 0
            for (c in courses) {
                if (index >= MAX_ALARM) break
                val start = times.getOrNull(c.startNode - 1)?.startTime ?: continue
                val parts = start.split(":")
                if (parts.size != 2) continue
                val h = parts[0].trim().toIntOrNull() ?: continue
                val m = parts[1].trim().toIntOrNull() ?: continue
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -advance)
                }
                if (target.timeInMillis <= System.currentTimeMillis()) continue
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_REMIND
                    putExtra("courseName", c.courseName)
                    putExtra("room", c.room ?: "")
                    putExtra("time", start)
                    putExtra("index", index)
                }
                val pi = PendingIntent.getBroadcast(context, REQ_BASE + index, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                if (canExact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                }
                index++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 立刻发一条通知。
     * 用来单独验证「通知权限 → 通知渠道 → 发通知」这条链路：
     * 点一下能收到，说明系统侧正常；收不到就是权限被关或渠道被系统屏蔽了。
     */
    fun sendTestNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(android.app.NotificationChannel(
                        "schedule_reminder", "课程提醒", NotificationManager.IMPORTANCE_HIGH))
            }
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                            android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(context,
                        "通知权限没开：系统设置 → 应用 → 大肥鱼课表 → 通知，打开后再试",
                        android.widget.Toast.LENGTH_LONG).show()
                return
            }
            val openPi = PendingIntent.getActivity(context, 0,
                    Intent(context, SplashActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(context, "schedule_reminder")
                    .setContentTitle("这是一条测试提醒")
                    .setSubText("上课提醒")
                    .setContentText("能收到这条，说明提醒功能正常")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.wakeup)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                    .setContentIntent(openPi)
                    .build()
            manager.notify(8899, n)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "发送失败：" + e.message,
                    android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

/** 到点弹通知 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == "fun.wakeup.local.REMIND_DISMISS") {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(intent.getIntExtra("index", 0))
                return
            }
            if (!context.getPrefer().getBoolean(Const.KEY_COURSE_REMIND, false)) return
            val courseName = intent.getStringExtra("courseName") ?: return
            val room = intent.getStringExtra("room").orEmpty().ifBlank { "未知" }
            val time = intent.getStringExtra("time").orEmpty()
            val index = intent.getIntExtra("index", 0)
            val dayName = CourseUtils.getWeekday()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val openIntent = Intent(context, SplashActivity::class.java)
            val openPi = PendingIntent.getActivity(context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = "fun.wakeup.local.REMIND_DISMISS"
                putExtra("index", index)
            }
            val dismissPi = PendingIntent.getBroadcast(context, 1000 + index, dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(context, "schedule_reminder")
                    .setContentTitle("$time $courseName")
                    .setSubText("上课提醒")
                    .setContentText("$dayName  地点：$room")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.wakeup)
                    .setAutoCancel(true)
                    .setOngoing(context.getPrefer().getBoolean(Const.KEY_REMINDER_ON_GOING, false))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                    .addAction(R.drawable.wakeup, "我知道啦", dismissPi)
                    .setContentIntent(openPi)
                    .build()
            manager.notify(index, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
