package com.suda.yzune.wakeupschedule.utils

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suda.yzune.wakeupschedule.bean.CourseTempBean

/**
 * 当周调整（调课/停课/临时课）的本地存储。
 *
 * 原先存在 Room 表里，但写完立刻"重启就复原"，怀疑是写库时机/取消/WAL 的问题。
 * 改成 SharedPreferences + commit() 同步落盘，重启必然还在。
 */
object WeekAdjustStore {

    private const val PREFIX = "week_adjust_"
    private val gson = Gson()

    private fun key(tableId: Int) = PREFIX + tableId

    fun load(context: Context, tableId: Int): MutableList<CourseTempBean> {
        return try {
            val json = context.getPrefer().getString(key(tableId), null) ?: return mutableListOf()
            val type = object : TypeToken<MutableList<CourseTempBean>>() {}.type
            gson.fromJson<MutableList<CourseTempBean>>(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun save(context: Context, tableId: Int, list: List<CourseTempBean>) {
        try {
            context.getPrefer().edit(commit = true) {
                putString(key(tableId), gson.toJson(list))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun remove(context: Context, tableId: Int, recordId: Int) {
        val list = load(context, tableId)
        list.removeAll { it.id == recordId }
        save(context, tableId, list)
    }

    fun update(context: Context, tableId: Int, bean: CourseTempBean) {
        val list = load(context, tableId)
        val idx = list.indexOfFirst { it.id == bean.id }
        if (idx >= 0) list[idx] = bean else list.add(bean)
        save(context, tableId, list)
    }

    fun add(context: Context, tableId: Int, bean: CourseTempBean): MutableList<CourseTempBean> {
        val list = load(context, tableId)
        // 给记录分配唯一 id，否则每条都是 0：第二次拖动时无法对应到具体某条 → 会叠出重复块
        if (bean.id <= 0) {
            bean.id = (list.maxOfOrNull { it.id } ?: 0) + 1
        }
        // 同一节课只保留一条
        list.removeAll {
            it.week == bean.week && it.type == bean.type && it.courseId == bean.courseId &&
                    it.origDay == bean.origDay && it.origStartNode == bean.origStartNode &&
                    it.origStartWeek == bean.origStartWeek && it.origEndWeek == bean.origEndWeek &&
                    it.origType == bean.origType
        }
        list.add(bean)
        save(context, tableId, list)
        return list
    }
}
