package com.suda.yzune.wakeupschedule.settings

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseListActivity
import com.suda.yzune.wakeupschedule.dao.AppWidgetDao
import com.suda.yzune.wakeupschedule.settings.items.*
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.getPrefer
import com.suda.yzune.wakeupschedule.widget.colorpicker.ColorPickerFragment
import splitties.resources.color
import splitties.snackbar.longSnack

class AdvancedSettingsActivity : BaseListActivity(), ColorPickerFragment.ColorPickerDialogListener {

    override fun onColorSelected(dialogId: Int, color: Int) {
        getPrefer().edit {
            putInt(Const.KEY_THEME_COLOR, color)
        }
        mRecyclerView.longSnack("重启App后生效哦~")
    }

    private lateinit var dataBase: AppDatabase
    private lateinit var widgetDao: AppWidgetDao

    private val mAdapter = SettingItemAdapter()

    override fun onSetupSubButton(tvButton: AppCompatTextView): AppCompatTextView? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataBase = AppDatabase.getDatabase(application)
        widgetDao = dataBase.appWidgetDao()

        val items = mutableListOf<BaseSettingItem>()
        onItemsCreated(items)
        mAdapter.setItems(items)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.itemAnimator?.changeDuration = 250
        mRecyclerView.adapter = mAdapter
        mAdapter.addChildClickViewIds(R.id.anko_check_box)
        mAdapter.setOnItemChildClickListener { _, view, position ->
            when (val item = items[position]) {
                is SwitchItem -> onSwitchItemCheckChange(item, view.findViewById<AppCompatCheckBox>(R.id.anko_check_box).isChecked)
            }
        }
        mAdapter.setOnItemClickListener { _, view, position ->
            when (val item = items[position]) {
                is VerticalItem -> onVerticalItemClick(item)
                is SwitchItem -> view.findViewById<AppCompatCheckBox>(R.id.anko_check_box).performClick()
                is SeekBarItem -> onSeekBarItemClick(item, position)
            }
        }
    }

    private fun onItemsCreated(items: MutableList<BaseSettingItem>) {
        val colorStr = getPrefer().getInt(Const.KEY_THEME_COLOR, color(R.color.colorAccent))
                .toString(16)
        items.add(CategoryItem("外观", true))

        items.add(VerticalItem("主题颜色", "调整大部分标签和虚拟键的颜色。\n以下关于虚拟键的设置，只对有虚拟键的手机有效哦，是为了有更好的沉浸效果~\n有实体按键或全面屏手势的手机本身就很棒啦~"))
        items.add(SwitchItem("主界面虚拟键沉浸", getPrefer().getBoolean(Const.KEY_HIDE_NAV_BAR, false)))

        items.add(CategoryItem("上课提醒", false))
        items.add(VerticalItem("功能说明", "本功能处于<b><font color='#$colorStr'>试验性阶段</font></b>。由于国产手机对系统的定制不尽相同，本功能可能会在某些手机上失效。<b><font color='#$colorStr'>开启前提：设置好课程时间 + 允许 App 后台运行（HyperOS 建议在系统设置里给「自启动 / 后台运行」权限）</font></b>。<br>理论上<b><font color='#$colorStr'>每次设置之后</font></b>需要半天以上的时间才会正常工作，理论上不会很耗电。", true))
        items.add(SwitchItem("开启上课提醒", getPrefer().getBoolean(Const.KEY_COURSE_REMIND, false)))
        items.add(SwitchItem("提醒通知常驻", getPrefer().getBoolean(Const.KEY_REMINDER_ON_GOING, false)))
        items.add(SeekBarItem("提前几分钟提醒", getPrefer().getInt(Const.KEY_REMINDER_TIME, 20), 0, 90, "分钟"))
        //items.add(SwitchItem("提醒同时将手机静音", PreferenceUtils.getBooleanFromSP(applicationContext, "silence_reminder", false)))
    }

    private fun onSwitchItemCheckChange(item: SwitchItem, isChecked: Boolean) {
        when (item.title) {
            "主界面虚拟键沉浸" -> {
                getPrefer().edit {
                    putBoolean(Const.KEY_HIDE_NAV_BAR, isChecked)
                }
                mRecyclerView.longSnack("重启App后生效哦~")
                item.checked = isChecked
            }
            "开启上课提醒" -> {
                launch {
                    // 提醒现在是独立实现的（按当天课表排闹钟），不再依赖旧版的日视图小部件
                    getPrefer().edit {
                        putBoolean(Const.KEY_COURSE_REMIND, isChecked)
                    }
                    com.suda.yzune.wakeupschedule.reminder.ReminderScheduler.reschedule(applicationContext)
                    item.checked = isChecked
                    if (isChecked) {
                        mRecyclerView.longSnack("已开启。今天剩下的课会按「提前几分钟」提醒你")
                    } else {
                        mRecyclerView.longSnack("已关闭上课提醒")
                    }
                }
            }
            "提醒通知常驻" -> {
                getPrefer().edit {
                    putBoolean(Const.KEY_REMINDER_ON_GOING, isChecked)
                }
                item.checked = isChecked
                mRecyclerView.longSnack("对下一次提醒通知生效哦")
            }
            "提醒同时将手机静音" -> {
                val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.isNotificationPolicyAccessGranted) {
                    val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    startActivity(intent)
                    item.checked = false
                } else {
                    getPrefer().edit {
                        putBoolean(Const.KEY_SILENCE_REMINDER, isChecked)
                    }
                    com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(applicationContext)
                    item.checked = isChecked
                }
            }
        }
    }

    private fun onVerticalItemClick(item: VerticalItem) {
        when (item.title) {
            "主题颜色" -> {
                ColorPickerFragment.newBuilder()
                        .setShowAlphaSlider(true)
                        .setColor(getPrefer().getInt(Const.KEY_THEME_COLOR, color(R.color.colorAccent)))
                        .show(this)
            }
        }
    }

    private fun onSeekBarItemClick(item: SeekBarItem, position: Int) {
        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(item.title)
                .setView(R.layout.dialog_edit_text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sure, null)
                .create()
        dialog.show()
        val inputLayout = dialog.findViewById<TextInputLayout>(R.id.text_input_layout)
        val editText = dialog.findViewById<TextInputEditText>(R.id.edit_text)
        inputLayout?.helperText = "范围 ${item.min} ~ ${item.max}"
        inputLayout?.suffixText = item.unit
        editText?.inputType = InputType.TYPE_CLASS_NUMBER
        val valueStr = item.valueInt.toString()
        editText?.setText(valueStr)
        editText?.setSelection(valueStr.length)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = editText?.text
            if (value.isNullOrBlank()) {
                inputLayout?.error = "数值不能为空哦>_<"
                return@setOnClickListener
            }
            val valueInt = try {
                value.toString().toInt()
            } catch (e: Exception) {
                inputLayout?.error = "输入异常>_<"
                return@setOnClickListener
            }
            if (valueInt < item.min || valueInt > item.max) {
                inputLayout?.error = "注意范围 ${item.min} ~ ${item.max}"
                return@setOnClickListener
            }
            when (item.title) {
                "提前几分钟提醒" -> {
                    getPrefer().edit {
                        putInt(Const.KEY_REMINDER_TIME, valueInt)
                    }
                    com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(applicationContext)
                }
            }
            item.valueInt = valueInt
            mAdapter.notifyItemChanged(position)
            dialog.dismiss()
        }
    }

    override fun onDestroy() {
        com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(applicationContext)
        super.onDestroy()
    }

    private var isExiting = false

    // 返回只退一层，别让第二次 back 把栈底也带走
    override fun onBackPressed() {
        if (isExiting) return
        isExiting = true
        setResult(RESULT_OK)
        finish()
    }
}
