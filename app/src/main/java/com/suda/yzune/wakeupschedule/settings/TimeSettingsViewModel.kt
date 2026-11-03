package com.suda.yzune.wakeupschedule.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.bean.TimeDetailBean
import com.suda.yzune.wakeupschedule.bean.TimeTableBean
import com.suda.yzune.wakeupschedule.utils.CourseUtils

class TimeSettingsViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var table: TableBean

    private val dataBase = AppDatabase.getDatabase(application)
    private val timeDao = dataBase.timeDetailDao()
    private val timeTableDao = dataBase.timeTableDao()
    private val tableDao = dataBase.tableDao()
    val timeTableList = arrayListOf<TimeTableBean>()
    val timeList = arrayListOf<TimeDetailBean>()
    val timeSelectList = arrayListOf<String>()

    var entryPosition = 0
    var selectedId = 1

    suspend fun addNewTimeTable(name: String) {
        timeTableDao.initTimeTable(TimeTableBean(id = 0, name = name))
    }

    suspend fun initTimeTableData(id: Int) {
        val timeList = listOf(
                TimeDetailBean(1, "08:00", "08:50", id),
                TimeDetailBean(2, "09:00", "09:50", id),
                TimeDetailBean(3, "10:10", "11:00", id),
                TimeDetailBean(4, "11:10", "12:00", id),
                TimeDetailBean(5, "13:30", "14:20", id),
                TimeDetailBean(6, "14:30", "15:20", id),
                TimeDetailBean(7, "15:40", "16:30", id),
                TimeDetailBean(8, "16:40", "17:30", id),
                TimeDetailBean(9, "18:30", "19:20", id),
                TimeDetailBean(10, "19:30", "20:20", id),
                TimeDetailBean(11, "20:30", "21:20", id),
                TimeDetailBean(12, "00:00", "00:00", id),
                TimeDetailBean(13, "00:00", "00:00", id),
                TimeDetailBean(14, "00:00", "00:00", id),
                TimeDetailBean(15, "00:00", "00:00", id),
                TimeDetailBean(16, "00:00", "00:00", id),
                TimeDetailBean(17, "00:00", "00:00", id),
                TimeDetailBean(18, "00:00", "00:00", id),
                TimeDetailBean(19, "00:00", "00:00", id),
                TimeDetailBean(20, "00:00", "00:00", id),
                TimeDetailBean(21, "00:00", "00:00", id),
                TimeDetailBean(22, "00:00", "00:00", id),
                TimeDetailBean(23, "00:00", "00:00", id),
                TimeDetailBean(24, "00:00", "00:00", id),
                TimeDetailBean(25, "00:00", "00:00", id),
                TimeDetailBean(26, "00:00", "00:00", id),
                TimeDetailBean(27, "00:00", "00:00", id),
                TimeDetailBean(28, "00:00", "00:00", id),
                TimeDetailBean(29, "00:00", "00:00", id),
                TimeDetailBean(30, "00:00", "00:00", id)
        )
        timeDao.insertTimeList(timeList)
    }

    suspend fun deleteTimeTable(timeTableBean: TimeTableBean) {
        timeTableDao.deleteTimeTable(timeTableBean)
    }

    /**
     * 删除时间表（含内置的默认时间表）。
     * - 只剩一张时不允许删（课表总得有作息）
     * - 还有课表在用这张时间表时：默认时间表自动把用它的课表切换到另一张时间表后删除；
     *   非默认的则直接报错，避免悄悄改掉用户课表的作息。
     */
    suspend fun deleteTimeTableSafely(timeTableBean: TimeTableBean) {
        if (timeTableList.size <= 1) {
            throw Exception("至少要留一张时间表哦")
        }
        val using = tableDao.countTablesUsingTimeTable(timeTableBean.id)
        if (using > 0) {
            if (timeTableBean.id != 1) {
                throw Exception("还有 $using 张课表在用它")
            }
            val fallbackId = timeTableDao.getAnotherTimeTableId(timeTableBean.id)
                    ?: throw Exception("找不到可以替换的时间表")
            tableDao.reassignTimeTable(timeTableBean.id, fallbackId)
        }
        timeTableDao.deleteTimeTable(timeTableBean)
    }

    fun getTimeTableList(): LiveData<List<TimeTableBean>> {
        return timeTableDao.getTimeTableList()
    }

    fun getTimeData(id: Int): LiveData<List<TimeDetailBean>> {
        return timeDao.getTimeListLiveData(id)
    }

    suspend fun saveDetailData(tablePosition: Int) {
        timeTableDao.updateTimeTable(timeTableList[tablePosition])
        timeDao.updateTimeDetailList(timeList)
    }

    fun initTimeSelectList() {
        for (i in 6..23) {
            for (j in 0..55 step 5) {
                val h = if (i < 10) "0$i" else i.toString()
                val m = if (j < 10) "0$j" else j.toString()
                timeSelectList.add("$h:$m")
            }
        }
    }

    fun refreshEndTime(min: Int) {
        timeList.forEach {
            it.endTime = CourseUtils.calAfterTime(it.startTime, min)
        }
    }
}