package com.suda.yzune.wakeupschedule.appwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.R
import kotlinx.coroutines.runBlocking

/** 添加小组件时选「显示哪张课表」 */
class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContentView(R.layout.activity_widget_config)
        val tableDao = AppDatabase.getDatabase(this).tableDao()
        val tables = runBlocking { tableDao.getTableSelectList() }
        val names = tables.map { if (it.tableName.isBlank()) "我的课表" else it.tableName }
        val list = findViewById<ListView>(R.id.lv_tables)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        list.setOnItemClickListener { _, _, position, _ ->
            WidgetStore.setTableId(this, appWidgetId, tables[position].id)
            WidgetViews.update(this, AppWidgetManager.getInstance(this), appWidgetId, modeOf(appWidgetId))
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, result)
            finish()
        }
        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
    }

    private fun modeOf(id: Int): String {
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        return if (info?.provider?.className == WeekWidgetProvider::class.java.name) {
            WidgetViews.MODE_WEEK
        } else {
            WidgetViews.MODE_TODAY
        }
    }
}
