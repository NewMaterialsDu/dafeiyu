package com.suda.yzune.wakeupschedule.utils

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suda.yzune.wakeupschedule.bean.TableBean

/**
 * 一套「课表外观设置」快照：配色、背景、格子尺寸/透明度/字号、分隔线、圆角。
 * 用来把调好的样式一键套到别的课表上。
 */
data class TableStyleSnapshot(
        var name: String = "",
        var textColor: Int = 0,
        var courseTextColor: Int = 0,
        var strokeColor: Int = 0,
        var itemHeight: Int = 56,
        var itemAlpha: Int = 60,
        var itemTextSize: Int = 12,
        var background: String = "",
        var showTime: Boolean = false,
        var themeColor: Int = 0,
        var showGridLine: Boolean = true,
        var gridLineColor: Int = -1,
        var gridLineWidth: Int = 1,
        var gridLineStyle: Int = 0,
        var dashOnCourse: Boolean = true,
        var cornerRadius: Int = 0
)

object TableStyleStore {

    private val gson = Gson()

    fun list(context: Context): MutableList<TableStyleSnapshot> {
        val json = context.getPrefer().getString(Const.KEY_TABLE_STYLE_SNAPSHOTS, "") ?: ""
        if (json.isBlank()) return mutableListOf()
        return try {
            gson.fromJson<MutableList<TableStyleSnapshot>>(
                    json, object : TypeToken<MutableList<TableStyleSnapshot>>() {}.type
            ) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun persist(context: Context, list: MutableList<TableStyleSnapshot>) {
        context.getPrefer().edit {
            putString(Const.KEY_TABLE_STYLE_SNAPSHOTS, gson.toJson(list))
        }
    }

    /** 同名覆盖，否则追加 */
    fun save(context: Context, snapshot: TableStyleSnapshot) {
        val list = list(context)
        val index = list.indexOfFirst { it.name == snapshot.name }
        if (index >= 0) list[index] = snapshot else list.add(snapshot)
        persist(context, list)
    }

    fun delete(context: Context, name: String) {
        val list = list(context)
        list.removeAll { it.name == name }
        persist(context, list)
    }

    /** 把当前课表的外观抓成快照 */
    fun capture(context: Context, table: TableBean, name: String): TableStyleSnapshot {
        val prefs = context.getPrefer()
        return TableStyleSnapshot(
                name = name,
                textColor = table.textColor,
                courseTextColor = table.courseTextColor,
                strokeColor = table.strokeColor,
                itemHeight = table.itemHeight,
                itemAlpha = table.itemAlpha,
                itemTextSize = table.itemTextSize,
                background = table.background,
                showTime = table.showTime,
                themeColor = prefs.getInt(Const.KEY_THEME_COLOR, 0),
                showGridLine = prefs.getBoolean(Const.KEY_SHOW_GRID_LINE, true),
                gridLineColor = prefs.getInt(Const.KEY_GRID_LINE_COLOR, Const.GRID_LINE_COLOR_FOLLOW_TEXT),
                gridLineWidth = prefs.getInt(Const.KEY_GRID_LINE_WIDTH, 1),
                gridLineStyle = prefs.getInt(Const.KEY_GRID_LINE_STYLE, Const.GRID_LINE_STYLE_SOLID),
                dashOnCourse = prefs.getBoolean(Const.KEY_GRID_LINE_DASH_ON_COURSE, true),
                cornerRadius = prefs.getInt(Const.KEY_ITEM_CORNER, 0)
        )
    }

    /** 把快照套到课表上（课表字段 + 全局的线/圆角 prefs） */
    fun apply(context: Context, table: TableBean, style: TableStyleSnapshot) {
        table.textColor = style.textColor
        table.courseTextColor = style.courseTextColor
        table.strokeColor = style.strokeColor
        table.itemHeight = style.itemHeight
        table.itemAlpha = style.itemAlpha
        table.itemTextSize = style.itemTextSize
        table.background = style.background
        table.showTime = style.showTime
        context.getPrefer().edit {
            putInt(Const.KEY_THEME_COLOR, style.themeColor)
            putBoolean(Const.KEY_SHOW_GRID_LINE, style.showGridLine)
            putInt(Const.KEY_GRID_LINE_COLOR, style.gridLineColor)
            putInt(Const.KEY_GRID_LINE_WIDTH, style.gridLineWidth)
            putInt(Const.KEY_GRID_LINE_STYLE, style.gridLineStyle)
            putBoolean(Const.KEY_GRID_LINE_DASH_ON_COURSE, style.dashOnCourse)
            putInt(Const.KEY_ITEM_CORNER, style.cornerRadius)
        }
    }
}

/** 已添课表卡片的自定义封面图（按课表 id 存 prefs，不改数据库，避免动 Room 版本） */
object TableCoverStore {

    fun get(context: Context, tableId: Int): String =
            context.getPrefer().getString(Const.KEY_TABLE_COVER_PREFIX + tableId, "") ?: ""

    fun set(context: Context, tableId: Int, uri: String) {
        context.getPrefer().edit {
            putString(Const.KEY_TABLE_COVER_PREFIX + tableId, uri)
        }
    }

    fun clear(context: Context, tableId: Int) {
        context.getPrefer().edit {
            remove(Const.KEY_TABLE_COVER_PREFIX + tableId)
        }
    }
}
