package com.suda.yzune.wakeupschedule.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.suda.yzune.wakeupschedule.bean.CourseTempBean

@Dao
interface CourseTempDao {

    @Query("select * from coursetempbean where tableId = :tableId and week = :week")
    fun getByWeek(tableId: Int, week: Int): List<CourseTempBean>

    @Query("select * from coursetempbean where tableId = :tableId and week = :week and type = 0")
    fun getMoves(tableId: Int, week: Int): List<CourseTempBean>

    @Query("select * from coursetempbean where tableId = :tableId and week = :week and type = 1")
    fun getStops(tableId: Int, week: Int): List<CourseTempBean>

    @Query("select * from coursetempbean where tableId = :tableId and week = :week and type = 2")
    fun getTemps(tableId: Int, week: Int): List<CourseTempBean>

    @Query("select * from coursetempbean where id = :id limit 1")
    fun getById(id: Int): CourseTempBean?

    @Query("delete from coursetempbean where id = :id")
    fun delete(id: Int)

    /** 同一节课的临时调课/停课只保留一条，避免叠一堆 */
    @Query("delete from coursetempbean where tableId = :tableId and week = :week " +
            "and courseId = :courseId and origDay = :origDay and origStartNode = :origStartNode")
    fun clearOld(tableId: Int, week: Int, courseId: Int, origDay: Int, origStartNode: Int)

    @Insert
    fun insert(bean: CourseTempBean): Long
}
