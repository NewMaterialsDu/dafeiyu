package com.suda.yzune.wakeupschedule.schedule_settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.bean.AppWidgetBean
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.utils.CourseUtils
import java.util.*

class ScheduleSettingsViewModel(application: Application) : AndroidViewModel(application) {

    var mYear = 2018
    var mMonth = 9
    var mDay = 20
    lateinit var table: TableBean
    lateinit var termStartList: List<String>

    private val dataBase = AppDatabase.getDatabase(application)
    private val tableDao = dataBase.tableDao()
    private val widgetDao = dataBase.appWidgetDao()
    private val timeDao = dataBase.timeDetailDao()

    suspend fun saveSettings() {
        tableDao.updateTable(table)
    }


    /**
     * 按大节排作息：默认两节连上（节间不休息），大节之间休息若干分钟。
     * 只改 1..nodes 这些行，不动其它时间表的数据。
     */
    suspend fun applyArrangement(startTime: String, periodMin: Int, bigBreakMin: Int): Int {
        val parts = startTime.split(":")
        if (parts.size != 2) throw Exception("开始时间格式不对，应该是 08:30 这样")
        var cursor = parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
        if (cursor < 0 || cursor >= 24 * 60) throw Exception("开始时间超出范围")
        val nodes = table.nodes.coerceIn(1, 30)
        val existing = timeDao.getTimeList(table.timeTable).associateBy { it.node }
        val toUpdate = arrayListOf<com.suda.yzune.wakeupschedule.bean.TimeDetailBean>()
        val toInsert = arrayListOf<com.suda.yzune.wakeupschedule.bean.TimeDetailBean>()
        for (node in 1..nodes) {
            if (node > 1 && (node - 1) % 2 == 0) cursor += bigBreakMin
            val start = String.format("%02d:%02d", cursor / 60 % 24, cursor % 60)
            cursor += periodMin
            val end = String.format("%02d:%02d", cursor / 60 % 24, cursor % 60)
            val bean = com.suda.yzune.wakeupschedule.bean.TimeDetailBean(node, start, end, table.timeTable)
            if (existing.containsKey(node)) toUpdate.add(bean) else toInsert.add(bean)
        }
        if (toUpdate.isNotEmpty()) timeDao.updateTimeDetailList(toUpdate)
        if (toInsert.isNotEmpty()) timeDao.insertTimeList(toInsert)
        return nodes
    }

    suspend fun getScheduleWidgetIds(): List<AppWidgetBean> {
        return widgetDao.getWidgetsByBaseType(0)
    }

    fun getCurrentWeek(): Int {
        val week = CourseUtils.countWeek(table.startDate, table.sundayFirst)
        // 起始日期可能是个很早的默认值，算出来几百周，钳到 1..maxWeek
        return week.coerceIn(1, table.maxWeek)
    }

    fun setCurrentWeek(week: Int) {
        val cal = Calendar.getInstance()
        if (table.sundayFirst) {
            cal.firstDayOfWeek = Calendar.SUNDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        } else {
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        cal.add(Calendar.WEEK_OF_YEAR, -week + 1)
        mYear = cal.get(Calendar.YEAR)
        mMonth = cal.get(Calendar.MONTH) + 1
        mDay = cal.get(Calendar.DATE)
        table.startDate = "${mYear}-${mMonth}-${mDay}"
    }

}