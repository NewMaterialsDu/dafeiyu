package com.suda.yzune.wakeupschedule.appwidget

import android.content.Context
import android.graphics.Color
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.bean.CourseBean
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.utils.CourseUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/** 小组件里的一行课程 */
data class WidgetCourseRow(
        val dayLabel: String,
        val time: String,
        val name: String,
        val room: String,
        val color: Int,
        val past: Boolean
)

/**
 * 小组件的数据层：不碰 UI，全部用同步查询（RemoteViewsFactory 本来就在工作线程）。
 * 格式跟「今日课程」一致：时间 + 课名 + 教室 + 左侧色条。
 */
object WidgetData {

    private val DAY_NAMES = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    private fun db(context: Context) = AppDatabase.getDatabase(context)

    fun table(context: Context, appWidgetId: Int): TableBean {
        val dao = db(context).tableDao()
        val id = WidgetStore.tableId(context, appWidgetId)
        return (if (id != null) dao.getTableByIdSync(id) else null) ?: dao.getDefaultTableSync()
    }

    fun currentWeek(table: TableBean): Int = try {
        CourseUtils.countWeek(table.startDate, table.sundayFirst).coerceIn(1, table.maxWeek)
    } catch (e: Exception) {
        1
    }

    private fun minutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        if (parts.size != 2) return -1
        val h = parts[0].trim().toIntOrNull() ?: return -1
        val m = parts[1].trim().toIntOrNull() ?: return -1
        return h * 60 + m
    }

    private fun nowMinutes(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    private fun coursesOfDay(context: Context, table: TableBean, day: Int, week: Int): List<CourseBean> {
        val raw = try {
            val type = if (week % 2 == 0) 2 else 1
            db(context).courseDao().getCourseByDayOfTableSync(day, week, type, table.id).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
        // 叠加当周调整（调课/停课/临时课），否则小组件和 App 里的课表不一致
        return try {
            val adjusts = com.suda.yzune.wakeupschedule.utils.WeekAdjustStore
                    .load(context, table.id).filter { it.week == week }
            android.util.Log.e("WIDGET", "day=" + day + " week=" + week + " raw=" + raw.size + " adjusts=" + adjusts.size)
            if (adjusts.isEmpty()) return raw.sortedBy { it.startNode }
            val keep = raw.filter { c ->
                adjusts.none { a -> (a.type == 0 || a.type == 1) && a.origDay == day &&
                        a.courseId == c.id && a.origStartNode == c.startNode }
            }.toMutableList()
            adjusts.filter { it.type == 0 && it.day == day }.forEach { a ->
                keep.add(CourseBean(-10000 - a.id, a.name, a.day, a.room, "", a.startNode,
                        a.step.coerceAtLeast(1), week, week, 0, a.color.ifEmpty { "#ff1744" }, table.id))
            }
            adjusts.filter { it.type == 2 && it.day == day }.forEach { a ->
                keep.add(CourseBean(-10000 - a.id, a.name, a.day, a.room, "", a.startNode,
                        a.step.coerceAtLeast(1), week, week, 0, a.color.ifEmpty { "#43a047" }, table.id))
            }
            keep.sortedBy { it.startNode }
        } catch (e: Exception) {
            raw.sortedBy { it.startNode }
        }
    }

    /** 课程开始/结束时间（"HH:mm"），拿不到返回空串 */
    private fun times(context: Context, table: TableBean, course: CourseBean): Pair<String, String> {
        return try {
            val list = db(context).timeDetailDao().getTimeListSync(table.timeTable)
            val start = list.getOrNull(course.startNode - 1)?.startTime ?: ""
            val end = list.getOrNull(course.startNode + course.step - 2)?.endTime ?: ""
            Pair(start, end)
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    private fun endedNow(context: Context, table: TableBean, course: CourseBean): Boolean {
        val (_, end) = times(context, table, course)
        if (end.isEmpty()) return false
        val m = minutes(end)
        return m in 0 until nowMinutes()
    }

    private fun colorOf(color: String): Int = try {
        if (color.isBlank()) Color.GRAY else Color.parseColor(color)
    } catch (e: Exception) {
        Color.GRAY
    }

    /** 今日课程（offsetDays: 0=今天 1=明天），返回 (第几周, 行) */
    fun rowsForToday(context: Context, table: TableBean, offsetDays: Int, showPast: Boolean): Pair<Int, List<WidgetCourseRow>> {
        val week = try {
            CourseUtils.countWeek(table.startDate, table.sundayFirst, offsetDays > 0).coerceIn(1, table.maxWeek)
        } catch (e: Exception) {
            1
        }
        val day = CourseUtils.getWeekdayInt(offsetDays > 0).coerceIn(1, 7)
        val rows = mutableListOf<WidgetCourseRow>()
        for (c in coursesOfDay(context, table, day, week)) {
            val (start, _) = times(context, table, c)
            val past = offsetDays == 0 && endedNow(context, table, c)
            if (!showPast && past) continue
            rows.add(WidgetCourseRow(DAY_NAMES[day - 1], start, c.courseName, c.room ?: "", colorOf(c.color), past))
        }
        return Pair(week, rows)
    }

    /** 本周课程（offsetWeeks: -1/0/+1），返回 (第几周, 行) */
    fun rowsForWeek(context: Context, table: TableBean, offsetWeeks: Int, showPast: Boolean): Pair<Int, List<WidgetCourseRow>> {
        val week = (currentWeek(table) + offsetWeeks).coerceIn(1, table.maxWeek)
        val todayIdx = CourseUtils.getWeekdayInt().coerceIn(1, 7)
        val rows = mutableListOf<WidgetCourseRow>()
        for (d in 1..7) {
            if (d == 6 && !table.showSat) continue
            if (d == 7 && !table.showSun) continue
            val dayPast = offsetWeeks < 0 || (offsetWeeks == 0 && d < todayIdx)
            for (c in coursesOfDay(context, table, d, week)) {
                val (start, _) = times(context, table, c)
                val past = dayPast || (offsetWeeks == 0 && d == todayIdx && endedNow(context, table, c))
                if (!showPast && past) continue
                rows.add(WidgetCourseRow(DAY_NAMES[d - 1], start, c.courseName, c.room ?: "", colorOf(c.color), past))
            }
        }
        return Pair(week, rows)
    }

    /** 每周第一天 / 最后一天的日期文本，形如 "10.26 - 11.01" */
    fun weekRangeText(table: TableBean, week: Int): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
            val start = sdf.parse(table.startDate) ?: return ""
            val cal = Calendar.getInstance()
            cal.time = start
            cal.add(Calendar.DATE, (week - 1) * 7)
            val first = cal.time
            cal.add(Calendar.DATE, 6)
            val last = cal.time
            val fmt = SimpleDateFormat("M.d", java.util.Locale.CHINA)
            fmt.format(first) + " - " + fmt.format(last)
        } catch (e: Exception) {
            ""
        }
    }

    fun dayName(day: Int): String = DAY_NAMES[(day - 1).coerceIn(0, 6)]

    fun todayTitle(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        if (offsetDays > 0) cal.add(Calendar.DATE, 1)
        return SimpleDateFormat("M月d日", java.util.Locale.CHINA).format(cal.time)
    }
}
