package com.suda.yzune.wakeupschedule.schedule_settings

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.suda.yzune.wakeupschedule.R
import kotlinx.coroutines.launch
import com.suda.yzune.wakeupschedule.base_view.BaseListActivity
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.bean.TableSelectBean
import com.suda.yzune.wakeupschedule.schedule_manage.ScheduleManageActivity
import com.suda.yzune.wakeupschedule.settings.AdvancedSettingsActivity
import com.suda.yzune.wakeupschedule.settings.SettingItemAdapter
import com.suda.yzune.wakeupschedule.settings.TimeSettingsActivity
import com.suda.yzune.wakeupschedule.settings.items.*
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.ViewUtils
import com.suda.yzune.wakeupschedule.utils.getPrefer
import com.suda.yzune.wakeupschedule.utils.menuTextColor
import splitties.resources.styledColor
import androidx.core.content.edit
import com.suda.yzune.wakeupschedule.appwidget.WidgetBackgrounds
import com.suda.yzune.wakeupschedule.appwidget.WidgetViews
import com.suda.yzune.wakeupschedule.utils.TableCoverStore
import com.suda.yzune.wakeupschedule.utils.TableStyleStore
import com.suda.yzune.wakeupschedule.utils.ThemePresets
import com.suda.yzune.wakeupschedule.utils.WallpaperUtils
import com.suda.yzune.wakeupschedule.widget.colorpicker.ColorPickerFragment
import es.dmoral.toasty.Toasty
import splitties.activities.start
import splitties.dimensions.dip
import splitties.snackbar.longSnack

private const val TITLE_COLOR = 1
private const val COURSE_TEXT_COLOR = 2
private const val STROKE_COLOR = 3
private const val WIDGET_TITLE_COLOR = 4
private const val WIDGET_COURSE_TEXT_COLOR = 5
private const val WIDGET_STROKE_COLOR = 6

class ScheduleSettingsActivity : BaseListActivity(), ColorPickerFragment.ColorPickerDialogListener {

    override fun onColorSelected(dialogId: Int, color: Int) {
        when (dialogId) {
            TITLE_COLOR -> viewModel.table.textColor = color
            COURSE_TEXT_COLOR -> viewModel.table.courseTextColor = color
            STROKE_COLOR -> viewModel.table.strokeColor = color
            WIDGET_TITLE_COLOR -> viewModel.table.widgetTextColor = color
            WIDGET_COURSE_TEXT_COLOR -> viewModel.table.widgetCourseTextColor = color
            WIDGET_STROKE_COLOR -> viewModel.table.widgetStrokeColor = color
        }
    }

    override fun onSetupSubButton(tvButton: AppCompatTextView): AppCompatTextView? {
        return null
    }

    private val viewModel by viewModels<ScheduleSettingsViewModel>()
    private val mAdapter = SettingItemAdapter()
    /** 分隔线样式条目：弹窗改完要把摘要回写到列表上 */
    private lateinit var gridLineStyleItem: HorizontalItem
    /** 小组件背景条目 */
    private lateinit var widgetBgItem: HorizontalItem
    private val REQUEST_CODE_CHOOSE_TABLE = 21
    private val REQUEST_CODE_COVER = 24
    private val allItems = mutableListOf<BaseSettingItem>()
    private val showItems = mutableListOf<BaseSettingItem>()

    private val currentWeekItem by lazy(LazyThreadSafetyMode.NONE) {
        SeekBarItem("当前周", viewModel.getCurrentWeek(), 1, viewModel.table.maxWeek, "周", "第", keys = listOf("学期", "周", "日期", "开学", "开始", "时间"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        showSearch = true
        textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                showItems.clear()
                if (s.isNullOrBlank() || s.isEmpty()) {
                    showItems.addAll(allItems)
                } else {
                    showItems.add(CategoryItem("搜索结果", true))
                    showItems.addAll(allItems.filter {
                        val k = it.keyWords
                        k?.contains(s.toString()) ?: false
                    })
                }
                mRecyclerView.adapter?.notifyDataSetChanged()
                if (showItems.size == 1) {
                    mRecyclerView.longSnack("找不到哦，换个关键词试试看，或者请仔细找找啦，一般都能找到的。")
                }
            }
        }
        super.onCreate(savedInstanceState)
        viewModel.table = intent.extras!!.getParcelable<TableBean>("tableData") as TableBean

        //onAdapterCreated(mAdapter)

        onItemsCreated(allItems)
        showItems.addAll(allItems)
        mAdapter.setItems(showItems)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.itemAnimator?.changeDuration = 250
        mRecyclerView.adapter = mAdapter
        mAdapter.addChildClickViewIds(R.id.anko_check_box)
        mAdapter.setOnItemChildClickListener { _, view, position ->
            when (val item = showItems[position]) {
                is SwitchItem -> onSwitchItemCheckChange(item, view.findViewById<AppCompatCheckBox>(R.id.anko_check_box).isChecked)
            }
        }
        mAdapter.setOnItemClickListener { _, view, position ->
            when (val item = showItems[position]) {
                is HorizontalItem -> onHorizontalItemClick(item, position)
                is VerticalItem -> onVerticalItemClick(item)
                is SwitchItem -> view.findViewById<AppCompatCheckBox>(R.id.anko_check_box).performClick()
                is SeekBarItem -> onSeekBarItemClick(item, position)
            }
        }
        mAdapter.setOnItemLongClickListener { _, _, position ->
            when (val item = showItems[position]) {
                is VerticalItem -> onVerticalItemLongClick(item)
            }
            true
        }
        viewModel.termStartList = viewModel.table.startDate.split("-")
        viewModel.mYear = Integer.parseInt(viewModel.termStartList[0])
        viewModel.mMonth = Integer.parseInt(viewModel.termStartList[1])
        viewModel.mDay = Integer.parseInt(viewModel.termStartList[2])
        val settingItem = intent?.extras?.getString("settingItem")
        if (settingItem != null) {
            mRecyclerView.postDelayed({
                try {
                    val i = showItems.indexOfFirst {
                        it.title == settingItem
                    }
                    (mRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(i, dip(64))
                    when (showItems[i]) {
                        is HorizontalItem -> onHorizontalItemClick(showItems[i] as HorizontalItem, i)
                        is VerticalItem -> onVerticalItemClick(showItems[i] as VerticalItem)
                        is SeekBarItem -> onSeekBarItemClick(showItems[i] as SeekBarItem, i)
                    }
                } catch (e: Exception) {

                }
            }, 100)
        }
    }

    private fun onItemsCreated(items: MutableList<BaseSettingItem>) {
        items.add(CategoryItem("课程数据", true))
        items.add(HorizontalItem("课表名称", viewModel.table.tableName, listOf("名称", "名字", "名", "课表")))
        items.add(HorizontalItem("上课时间", "点击此处更改", listOf("时间")))
        items.add(HorizontalItem("学期开始日期", viewModel.table.startDate, listOf("学期", "周", "日期", "开学", "开始", "时间")))
        items.add(currentWeekItem)
        items.add(SeekBarItem("一天课程节数", viewModel.table.nodes, 1, 30, "节", keys = listOf("节数", "数量", "数")))
        items.add(SeekBarItem("学期周数", viewModel.table.maxWeek, 1, 30, "周", keys = listOf("学期", "周", "时间")))
        items.add(SwitchItem("周日为每周第一天", viewModel.table.sundayFirst, keys = listOf("周日", "第一天", "起始", "星期天", "天")))
        items.add(SwitchItem("显示周六", viewModel.table.showSat, keys = listOf("周六", "显示", "星期六", "六")))
        items.add(SwitchItem("显示周日", viewModel.table.showSun, keys = listOf("周日", "显示", "星期日", "日", "星期天", "周天")))

        items.add(CategoryItem("课表外观", false))
        items.add(SwitchItem("在格子内显示上课时间", viewModel.table.showTime, keys = listOf("时间", "显示", "格子", "上课时间")))
        items.add(SwitchItem("显示格子分隔线", getPrefer().getBoolean(Const.KEY_SHOW_GRID_LINE, true), "关掉的话课表就是一整片底色，看起来更清爽。", keys = listOf("格子", "分隔", "线", "网格")))
        gridLineStyleItem = HorizontalItem("分隔线样式", gridLineStyleSummary(), keys = listOf("格子", "分隔", "线", "网格", "样式", "虚线", "实线"))
        items.add(gridLineStyleItem)
        items.add(SwitchItem("课程上的分隔线用虚线", getPrefer().getBoolean(Const.KEY_GRID_LINE_DASH_ON_COURSE, true), "分隔线经过课程块的位置用虚线，空格子仍是实线。", keys = listOf("虚线", "分隔", "线", "课程")))
        items.add(VerticalItem("按大节生成作息", "两节连上、大节之间休息若干分钟，一键排好每节课的时间。", keys = listOf("作息", "大节", "课间", "休息", "时间")))
        items.add(VerticalItem("配色方案", "一键换掉主题色、课程文字色和格子边框。", keys = listOf("配色", "主题", "颜色", "方案", "皮肤")))
        items.add(VerticalItem("课程表背景", "长按可以恢复默认哦~", keys = listOf("背景", "显示", "图片")))
        items.add(VerticalItem("课表卡片封面图", "「已添课表」列表里卡片顶部那张图，可以换成相册里的图片。长按恢复默认。", keys = listOf("卡片", "封面", "图片", "已添课表", "缩略图")))
        items.add(VerticalItem("界面文字颜色", "指标题等字体的颜色\n还可以调颜色的透明度哦 (●ﾟωﾟ●)", keys = listOf("颜色", "显示", "文字", "文字颜色")))
        items.add(VerticalItem("课程文字颜色", "指课程格子内的颜色\n还可以调颜色的透明度哦 (●ﾟωﾟ●)", keys = listOf("颜色", "显示", "文字", "文字颜色")))
        items.add(VerticalItem("格子边框颜色", "将不透明度调到最低就可以隐藏边框了哦~", keys = listOf("边框", "显示", "边框颜色", "格子", "边")))
        items.add(SeekBarItem("课程格子高度", viewModel.table.itemHeight, 32, 96, "dp", keys = listOf("格子", "高度", "格子高度", "显示")))
        items.add(SeekBarItem("课程格子圆角", getPrefer().getInt(Const.KEY_ITEM_CORNER, 0), 0, 24, "dp", keys = listOf("圆角", "角", "格子", "圆")))
        items.add(SeekBarItem("课程格子不透明度", viewModel.table.itemAlpha, 0, 100, "%", keys = listOf("格子", "透明", "格子高度", "显示")))
        items.add(SeekBarItem("课程显示文字大小", viewModel.table.itemTextSize, 8, 16, "sp", keys = listOf("文字", "大小", "文字大小")))
        items.add(SwitchItem("显示非本周课程", viewModel.table.showOtherWeekCourse, keys = listOf("非本周")))

        items.add(CategoryItem("桌面小部件外观", false))
        widgetBgItem = HorizontalItem("小组件背景", WidgetBackgrounds.titleOf(this), keys = listOf("小组件", "背景", "透明", "桌面", "小部件"))
        items.add(widgetBgItem)
        items.add(SeekBarItem("小组件圆角", getPrefer().getInt(Const.KEY_WIDGET_CORNER, 16), 0, 30, "dp", keys = listOf("小组件", "圆角", "角", "桌面", "小部件")))
        items.add(SwitchItem("小组件显示已上完的课程", getPrefer().getBoolean(Const.KEY_WIDGET_SHOW_PAST, true), "关掉后，小组件里只显示还没上的课。", keys = listOf("小组件", "上完", "已上", "显示", "桌面")))
        items.add(SeekBarItem("小部件格子高度", viewModel.table.widgetItemHeight, 32, 96, "dp", keys = listOf("格子", "高度", "格子高度", "显示", "小部件", "小", "插件", "桌面")))
        items.add(SeekBarItem("小部件格子不透明度", viewModel.table.widgetItemAlpha, 0, 100, "%", keys = listOf("格子", "透明", "格子高度", "显示", "小部件", "小", "插件", "桌面")))
        items.add(SeekBarItem("小部件显示文字大小", viewModel.table.widgetItemTextSize, 8, 16, "sp", keys = listOf("文字", "大小", "文字大小", "小部件", "小", "插件", "桌面")))
        items.add(VerticalItem("小部件标题颜色", "指标题等字体的颜色\n对于日视图则是全部文字的颜色\n还可以调颜色的透明度哦 (●ﾟωﾟ●)", keys = listOf("颜色", "显示", "文字", "文字颜色", "小部件", "小", "插件", "桌面")))
        items.add(VerticalItem("小部件课程颜色", "指课程格子内的文字颜色\n还可以调颜色的透明度哦 (●ﾟωﾟ●)", keys = listOf("颜色", "显示", "文字", "文字颜色", "小部件", "小", "插件", "桌面")))
        items.add(VerticalItem("小部件格子边框颜色", "将不透明度调到最低就可以隐藏边框了哦~", keys = listOf("边框", "显示", "边框颜色", "格子", "边", "小部件", "小", "插件", "桌面")))

        items.add(CategoryItem("高级", false))
        items.add(VerticalItem("保存课表设置", "把这套配色、背景、格子和分隔线存成一份快照，之后可以套到别的课表。", keys = listOf("保存", "快照", "备份", "设置", "应用")))
        items.add(VerticalItem("应用课表设置", "把之前保存的快照一键套到当前课表。", keys = listOf("应用", "套用", "快照", "设置", "恢复")))
        items.add(VerticalItem("高级设置", "主题颜色、上课提醒等", keys = listOf("高级")))

        items.add(VerticalItem("", "\n\n\n"))
    }

    private fun onSwitchItemCheckChange(item: SwitchItem, isChecked: Boolean) {
        when (item.title) {
            "周日为每周第一天" -> viewModel.table.sundayFirst = isChecked
            "显示周六" -> viewModel.table.showSat = isChecked
            "显示周日" -> viewModel.table.showSun = isChecked
            "在格子内显示上课时间" -> viewModel.table.showTime = isChecked
            "显示非本周课程" -> viewModel.table.showOtherWeekCourse = isChecked
            "显示格子分隔线" -> {
                getPrefer().edit {
                    putBoolean(Const.KEY_SHOW_GRID_LINE, isChecked)
                }
                mRecyclerView.longSnack("退出设置后生效哦~")
            }
            "课程上的分隔线用虚线" -> {
                getPrefer().edit {
                    putBoolean(Const.KEY_GRID_LINE_DASH_ON_COURSE, isChecked)
                }
                mRecyclerView.longSnack("退出设置后生效哦~")
            }
            "小组件显示已上完的课程" -> {
                getPrefer().edit {
                    putBoolean(Const.KEY_WIDGET_SHOW_PAST, isChecked)
                }
                com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(applicationContext)
                mRecyclerView.longSnack("小组件已刷新")
            }
        }
        item.checked = isChecked
    }

    private fun onSeekBarItemClick(item: SeekBarItem, position: Int) {
        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(item.title)
                .setView(R.layout.dialog_edit_text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sure, null)
                .setCancelable(false)
                .create()
        dialog.show()
        val inputLayout = dialog.findViewById<TextInputLayout>(R.id.text_input_layout)
        val editText = dialog.findViewById<TextInputEditText>(R.id.edit_text)
        inputLayout?.helperText = "范围 ${item.min} ~ ${item.max}"
        if (item.prefix.isNotEmpty()) {
            inputLayout?.prefixText = item.prefix
        }
        inputLayout?.suffixText = item.unit
        editText?.inputType = InputType.TYPE_CLASS_NUMBER
        if (item.valueInt < item.min) {
            item.valueInt = item.min
        }
        if (item.valueInt > item.max) {
            item.valueInt = item.max
        }
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
                "一天课程节数" -> viewModel.table.nodes = valueInt
                "学期周数" -> {
                    currentWeekItem.max = valueInt
                    viewModel.table.maxWeek = valueInt
                }
                "当前周" -> {
                    viewModel.setCurrentWeek(valueInt)
                    item.valueInt = valueInt
                    (mAdapter.data[position - 1] as HorizontalItem).value = viewModel.table.startDate
                    mAdapter.notifyItemChanged(position - 1)
                    mAdapter.notifyItemChanged(position)
                    dialog.dismiss()
                }
                "课程格子高度" -> viewModel.table.itemHeight = valueInt
                "课程格子圆角" -> {
                    getPrefer().edit { putInt(Const.KEY_ITEM_CORNER, valueInt) }
                    mRecyclerView.longSnack("退出设置后生效哦~")
                }
                "小组件圆角" -> {
                    getPrefer().edit { putInt(Const.KEY_WIDGET_CORNER, valueInt) }
                    WidgetViews.updateAll(applicationContext)
                    mRecyclerView.longSnack("小组件已刷新")
                }
                "课程格子不透明度" -> viewModel.table.itemAlpha = valueInt
                "课程显示文字大小" -> viewModel.table.itemTextSize = valueInt
                "小部件格子高度" -> viewModel.table.widgetItemHeight = valueInt
                "小部件格子不透明度" -> viewModel.table.widgetItemAlpha = valueInt
                "小部件显示文字大小" -> viewModel.table.widgetItemTextSize = valueInt
            }
            item.valueInt = valueInt
            mAdapter.notifyItemChanged(position)
            dialog.dismiss()
        }
    }

    private fun onHorizontalItemClick(item: HorizontalItem, position: Int) {
        when (item.title) {
            "小组件背景" -> {
                showWidgetBgDialog()
                return
            }
            "分隔线样式" -> {
                showGridLineStyleDialog()
                return
            }
            "课表名称" -> {
                val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.setting_schedule_name)
                        .setView(R.layout.dialog_edit_text)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.sure, null)
                        .create()
                dialog.show()
                val inputLayout = dialog.findViewById<TextInputLayout>(R.id.text_input_layout)
                val editText = dialog.findViewById<TextInputEditText>(R.id.edit_text)
                editText?.setText(item.value)
                editText?.setSelection(item.value.length)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value = editText?.text
                    if (value.isNullOrBlank()) {
                        inputLayout?.error = "名称不能为空哦>_<"
                        return@setOnClickListener
                    }
                    viewModel.table.tableName = value.toString()
                    item.value = value.toString()
                    mAdapter.notifyItemChanged(position)
                    dialog.dismiss()
                }
            }
            "学期开始日期" -> {
                DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    viewModel.mYear = year
                    viewModel.mMonth = monthOfYear + 1
                    viewModel.mDay = dayOfMonth
                    val mDate = "${viewModel.mYear}-${viewModel.mMonth}-${viewModel.mDay}"
                    item.value = mDate
                    viewModel.table.startDate = mDate
                    currentWeekItem.valueInt = viewModel.getCurrentWeek()
                    mAdapter.notifyItemChanged(position)
                    mAdapter.notifyItemChanged(position + 1)
                }, viewModel.mYear, viewModel.mMonth - 1, viewModel.mDay).show()
                if (viewModel.table.sundayFirst) {
                    Toasty.success(this, "为了周数计算准确，建议选择周日哦", Toast.LENGTH_LONG).show()
                } else {
                    Toasty.success(this, "为了周数计算准确，建议选择周一哦", Toast.LENGTH_LONG).show()
                }
            }
            "上课时间" -> {
                startActivityForResult(Intent(this, TimeSettingsActivity::class.java).apply {
                    putExtra("selectedId", viewModel.table.timeTable)
                }, REQUEST_CODE_CHOOSE_TABLE)
            }
        }
    }

    /** 分隔线样式：线色 / 线宽 / 实线虚线 / 只横只竖 */
    private fun showGridLineStyleDialog() {
        val ctx = this
        val prefs = getPrefer()
        var lineColor = prefs.getInt(Const.KEY_GRID_LINE_COLOR, Const.GRID_LINE_COLOR_FOLLOW_TEXT)
        var lineWidth = prefs.getInt(Const.KEY_GRID_LINE_WIDTH, 1)
        var lineStyle = prefs.getInt(Const.KEY_GRID_LINE_STYLE, Const.GRID_LINE_STYLE_SOLID)

        val root = androidx.appcompat.widget.LinearLayoutCompat(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dip(20), dip(4), dip(20), dip(4))
        }

        fun rowTitle(text: String) {
            root.addView(AppCompatTextView(ctx).apply {
                this.text = text
                textSize = 13f
                setTextColor(styledColor(R.attr.colorOnSurface))
            }, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dip(12) })
        }

        fun paintDot(view: android.view.View, color: Int, selected: Boolean) {
            val fill = if (color == Const.GRID_LINE_COLOR_FOLLOW_TEXT) styledColor(R.attr.colorOnSurface) else color
            view.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(fill)
                setStroke(dip(if (selected) 3 else 1), if (selected) styledColor(R.attr.colorOnSurface) else 0x33000000)
            }
        }

        // 线色
        rowTitle("线色")
        val colorRow = androidx.appcompat.widget.LinearLayoutCompat(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dip(8), 0, 0)
        }
        root.addView(colorRow)
        val dots = mutableListOf<android.view.View>()
        val presetColors = listOf(
                Const.GRID_LINE_COLOR_FOLLOW_TEXT to "跟",
                0xFFBDBDBD.toInt() to "",
                0xFF9E9E9E.toInt() to "",
                0xFF616161.toInt() to "",
                0xFF000000.toInt() to "",
                0xFFFFFFFF.toInt() to "",
                ctx.menuTextColor() to "主")
        presetColors.forEach { pair ->
            val fill = if (pair.first == Const.GRID_LINE_COLOR_FOLLOW_TEXT) ctx.menuTextColor() else pair.first
            val dot = AppCompatTextView(ctx).apply {
                text = pair.second
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(if (ViewUtils.judgeColorIsLight(fill)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                setOnClickListener {
                    lineColor = pair.first
                    dots.forEachIndexed { index, v ->
                        paintDot(v, presetColors[index].first, presetColors[index].first == lineColor)
                    }
                }
            }
            paintDot(dot, pair.first, pair.first == lineColor)
            dots.add(dot)
            colorRow.addView(dot, android.widget.LinearLayout.LayoutParams(dip(34), dip(34)).apply { marginEnd = dip(10) })
        }

        // 线宽
        rowTitle("线宽")
        val widthGroup = android.widget.RadioGroup(ctx).apply {
            orientation = android.widget.RadioGroup.HORIZONTAL
        }
        listOf(1, 2, 3, 4).forEach { px ->
            val rb = android.widget.RadioButton(ctx).apply {
                id = android.view.View.generateViewId()
                text = "${px}px"
                textSize = 13f
                setOnCheckedChangeListener { _, checked -> if (checked) lineWidth = px }
            }
            widthGroup.addView(rb)
            rb.isChecked = px == lineWidth
        }
        root.addView(widthGroup)

        // 样式
        rowTitle("样式")
        val styleNames = listOf("实线", "虚线", "只横线", "只竖线")
        val styleGroup = android.widget.RadioGroup(ctx).apply {
            orientation = android.widget.RadioGroup.HORIZONTAL
        }
        styleNames.forEachIndexed { index, name ->
            val rb = android.widget.RadioButton(ctx).apply {
                id = android.view.View.generateViewId()
                text = name
                textSize = 13f
                setOnCheckedChangeListener { _, checked -> if (checked) lineStyle = index }
            }
            styleGroup.addView(rb)
            rb.isChecked = index == lineStyle
        }
        root.addView(styleGroup)

        val dialog = MaterialAlertDialogBuilder(ctx)
                .setTitle("分隔线样式")
                .setView(android.widget.ScrollView(ctx).apply { addView(root) })
                .setPositiveButton("确定", null)
                .setNeutralButton("恢复默认", null)
                .setNegativeButton(R.string.cancel, null)
                .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            prefs.edit {
                putInt(Const.KEY_GRID_LINE_COLOR, lineColor)
                putInt(Const.KEY_GRID_LINE_WIDTH, lineWidth)
                putInt(Const.KEY_GRID_LINE_STYLE, lineStyle)
            }
            gridLineStyleItem.value = gridLineStyleSummary()
            mAdapter.notifyDataSetChanged()
            mRecyclerView.longSnack("退出设置后生效哦~")
            dialog.dismiss()
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            prefs.edit {
                putInt(Const.KEY_GRID_LINE_COLOR, Const.GRID_LINE_COLOR_FOLLOW_TEXT)
                putInt(Const.KEY_GRID_LINE_WIDTH, 1)
                putInt(Const.KEY_GRID_LINE_STYLE, Const.GRID_LINE_STYLE_SOLID)
            }
            gridLineStyleItem.value = gridLineStyleSummary()
            mAdapter.notifyDataSetChanged()
            mRecyclerView.longSnack("已恢复默认的分隔线")
            dialog.dismiss()
        }
    }

    /** 列表上显示的当前分隔线摘要 */
    private fun gridLineStyleSummary(): String {
        val prefs = getPrefer()
        val color = prefs.getInt(Const.KEY_GRID_LINE_COLOR, Const.GRID_LINE_COLOR_FOLLOW_TEXT)
        val width = prefs.getInt(Const.KEY_GRID_LINE_WIDTH, 1)
        val style = prefs.getInt(Const.KEY_GRID_LINE_STYLE, Const.GRID_LINE_STYLE_SOLID)
        val styleName = when (style) {
            Const.GRID_LINE_STYLE_DASHED -> "虚线"
            Const.GRID_LINE_STYLE_HORIZONTAL -> "只横线"
            Const.GRID_LINE_STYLE_VERTICAL -> "只竖线"
            else -> "实线"
        }
        val colorName = if (color == Const.GRID_LINE_COLOR_FOLLOW_TEXT) "跟文字色" else "自定义色"
        return "${width}px $styleName · $colorName"
    }

    /** 课表卡片封面图：相册选图 / 恢复默认 */
    private fun showCoverDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("课表卡片封面图")
                .setMessage("挑一张相册里的图片，显示在「已添课表」列表卡片顶部。\n长按这一项也能恢复默认。")
                .setPositiveButton("从相册选", null)
                .setNeutralButton("恢复默认", null)
                .setNegativeButton(R.string.cancel, null)
                .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivityForResult(intent, REQUEST_CODE_COVER)
            } catch (e: Exception) {
                Toasty.error(applicationContext, "打不开相册>_<").show()
            }
            dialog.dismiss()
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            TableCoverStore.clear(this, viewModel.table.id)
            Toasty.success(applicationContext, "已恢复默认封面").show()
            dialog.dismiss()
        }
    }

    /** 小组件背景：跟随课表 / 纯色库 / 内置图库 / 透明（每套自带适配的字体颜色） */
    private fun showWidgetBgDialog() {
        val options = WidgetBackgrounds.LIST
        MaterialAlertDialogBuilder(this)
                .setTitle("小组件背景")
                .setItems(options.map { it.title }.toTypedArray()) { _, which ->
                    WidgetBackgrounds.set(this, options[which].key)
                    widgetBgItem.value = WidgetBackgrounds.titleOf(this)
                    mAdapter.notifyDataSetChanged()
                    WidgetViews.updateAll(applicationContext)
                    Toasty.success(applicationContext, "已换成「${options[which].title}」，小组件会跟着变").show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    /** 保存当前这套外观设置为快照 */
    private fun showSaveStyleDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("保存课表设置")
                .setView(R.layout.dialog_edit_text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sure, null)
                .create()
        dialog.show()
        val inputLayout = dialog.findViewById<TextInputLayout>(R.id.text_input_layout)
        val editText = dialog.findViewById<TextInputEditText>(R.id.edit_text)
        inputLayout?.hint = "快照名字，例如「深海蓝款」"
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = editText?.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                inputLayout?.error = "名字不能为空哦>_<"
                return@setOnClickListener
            }
            TableStyleStore.save(this, TableStyleStore.capture(this, viewModel.table, name))
            Toasty.success(applicationContext, "已保存「${name}」，在别张课表里可以一键应用").show()
            dialog.dismiss()
        }
    }

    /** 一键把某个快照套到当前课表 */
    private fun showApplyStyleDialog() {
        val snapshots = TableStyleStore.list(this)
        if (snapshots.isEmpty()) {
            Toasty.info(applicationContext, "还没有保存过课表设置哦").show()
            return
        }
        val names = snapshots.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(this)
                .setTitle("应用课表设置")
                .setItems(names) { _, which ->
                    val style = snapshots[which]
                    TableStyleStore.apply(this, viewModel.table, style)
                    launch {
                        try {
                            viewModel.saveSettings()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    refreshSettingsList()
                    Toasty.success(applicationContext, "已套用「${style.name}」，返回课表就能看到").show()
                }
                .setNeutralButton("删除一份") { _, _ -> showDeleteStyleDialog() }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun showDeleteStyleDialog() {
        val snapshots = TableStyleStore.list(this)
        if (snapshots.isEmpty()) return
        MaterialAlertDialogBuilder(this)
                .setTitle("删除哪一份？")
                .setItems(snapshots.map { it.name }.toTypedArray()) { _, which ->
                    TableStyleStore.delete(this, snapshots[which].name)
                    Toasty.success(applicationContext, "已删除").show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    /** 重新生成一遍设置项，让列表上的数值/开关跟着新设置走 */
    private fun refreshSettingsList() {
        allItems.clear()
        showItems.clear()
        onItemsCreated(allItems)
        showItems.addAll(allItems)
        mAdapter.setItems(showItems)
    }

    private fun onVerticalItemClick(item: VerticalItem) {
        when (item.title) {
            "配色方案" -> showThemePresetDialog()
            "课程表背景" -> showWallpaperDialog()
            "界面文字颜色" -> {
                buildColorPickerDialogBuilder(viewModel.table.textColor, TITLE_COLOR)
            }
            "课程文字颜色" -> {
                buildColorPickerDialogBuilder(viewModel.table.courseTextColor, COURSE_TEXT_COLOR)
            }
            "格子边框颜色" -> {
                buildColorPickerDialogBuilder(viewModel.table.strokeColor, STROKE_COLOR)
            }
            "小部件标题颜色" -> {
                buildColorPickerDialogBuilder(viewModel.table.widgetTextColor, WIDGET_TITLE_COLOR)
            }
            "小部件课程颜色" -> {
                buildColorPickerDialogBuilder(viewModel.table.widgetCourseTextColor, WIDGET_COURSE_TEXT_COLOR)
            }
            "小部件格子边框颜色" -> {
                buildColorPickerDialogBuilder(viewModel.table.widgetStrokeColor, WIDGET_STROKE_COLOR)
            }
            "高级设置" -> {
                start<AdvancedSettingsActivity>()
            }
            "课表卡片封面图" -> showCoverDialog()
            "保存课表设置" -> showSaveStyleDialog()
            "应用课表设置" -> showApplyStyleDialog()
            "按大节生成作息" -> showArrangementDialog()
        }
    }

    private fun onVerticalItemLongClick(item: VerticalItem): Boolean {
        return when (item.title) {
            "课表卡片封面图" -> {
                TableCoverStore.clear(this, viewModel.table.id)
                Toasty.success(applicationContext, "已恢复默认封面").show()
                true
            }
            "课程表背景" -> {
                viewModel.table.background = ""
                Toasty.success(applicationContext, "恢复默认壁纸成功~").show()
                true
            }
            else -> false
        }
    }

    /** 配色方案：点一套就把强调色、课程文字、边框、格子透明度、背景纯色一起换掉 */
    /** 课表背景：6 张内置壁纸 + 6 个纯色 + 相册 + 恢复默认 */
    private fun showWallpaperDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_wallpaper_choose, null)
        val tiles = listOf(
                view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_wallpaper_1),
                view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_wallpaper_2),
                view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_wallpaper_3),
                view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_wallpaper_4),
                view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_wallpaper_5),
                view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_wallpaper_6)
        )

        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("课表背景")
                .setView(view)
                .setPositiveButton("从相册选", null)
                .setNeutralButton("恢复默认", null)
                .setNegativeButton(R.string.cancel, null)
                .create()

        tiles.forEachIndexed { index, card ->
            val item = WallpaperUtils.LIST.getOrNull(index) ?: return@forEachIndexed
            card.setOnClickListener {
                viewModel.table.background = item.key
                Toasty.success(applicationContext, "已换成「${item.title}」，退出设置后生效").show()
                dialog.dismiss()
            }
        }

        val solids = view.findViewById<androidx.appcompat.widget.LinearLayoutCompat>(R.id.ll_solids)
        val colors = listOf(
                0xFFFFFFFF.toInt() to "白",
                0xFFF6F2EA.toInt() to "米",
                0xFFEAF1FB.toInt() to "浅蓝",
                0xFFFBEDF3.toInt() to "浅粉",
                0xFFEDF7F1.toInt() to "浅绿",
                0xFF23272E.toInt() to "深灰"
        )
        val ctx = this
        colors.forEach { pair ->
            val color = pair.first
            val name = pair.second
            val dot = android.view.View(ctx)
            dot.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color)
                setStroke(ctx.dip(1), 0x33000000)
            }
            dot.setOnClickListener {
                viewModel.table.background = WallpaperUtils.solidValue(color)
                Toasty.success(applicationContext, "已换成纯色（$name）").show()
                dialog.dismiss()
            }
            solids.addView(dot, androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(0, ctx.dip(34), 1f).apply {
                marginEnd = ctx.dip(8)
            })
        }

        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            try {
                startActivityForResult(intent, WallpaperUtils.REQUEST_CODE_CHOOSE_BG)
            } catch (e: Exception) {
                Toasty.error(applicationContext, "打不开相册>_<${e.message}").show()
            }
            dialog.dismiss()
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            viewModel.table.background = ""
            Toasty.success(applicationContext, "已恢复默认纯色，退出设置后生效").show()
            dialog.dismiss()
        }
    }

    private fun showThemePresetDialog() {
        val ctx = this
        val list = androidx.appcompat.widget.LinearLayoutCompat(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dip(8), dip(8), dip(8), dip(8))
        }
        val scroll = android.widget.ScrollView(ctx).apply {
            addView(list)
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
                .setTitle("配色方案")
                .setView(scroll)
                .setNegativeButton(R.string.cancel, null)
                .create()

        ThemePresets.LIST.forEach { preset ->
            val row = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dip(12), dip(12), dip(12), dip(12))
            }
            // 左边一个课程格子示意
            row.addView(android.view.View(ctx).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dip(6).toFloat()
                    setColor(blendWithWhite(ThemePresets.toInt(preset.accent), preset.itemAlpha))
                    setStroke(dip(1), ThemePresets.toInt(preset.stroke))
                }
            }, android.widget.LinearLayout.LayoutParams(dip(44), dip(30)))

            row.addView(androidx.appcompat.widget.AppCompatTextView(ctx).apply {
                text = preset.name
                textSize = 14f
                // 弹窗底是深色的，名字必须显式用前景色，否则黑字压深底看不见
                setTextColor(styledColor(R.attr.colorOnSurface))
            }, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dip(12)
            })

            // 右边三个色点：强调色 / 边框 / 背景
            listOf(ThemePresets.toInt(preset.accent), ThemePresets.toInt(preset.bg), 0xFFFFFFFF.toInt()).forEach { c ->
                row.addView(android.view.View(ctx).apply {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(c)
                        setStroke(dip(1), 0x33000000)
                    }
                }, android.widget.LinearLayout.LayoutParams(dip(14), dip(14)).apply { marginEnd = dip(4) })
            }

            row.setOnClickListener {
                applyThemePreset(preset)
                dialog.dismiss()
            }
            list.addView(row)
        }
        dialog.show()
    }

    /** 按格子不透明度把颜色往白色上叠，和课表里画出来的效果一致 */
    private fun blendWithWhite(color: Int, percent: Int): Int {
        val a = percent.coerceIn(0, 100) / 100f
        val r = ((color shr 16 and 0xFF) * a + 255 * (1 - a)).toInt()
        val g = ((color shr 8 and 0xFF) * a + 255 * (1 - a)).toInt()
        val b = ((color and 0xFF) * a + 255 * (1 - a)).toInt()
        return android.graphics.Color.rgb(r, g, b)
    }

    private fun applyThemePreset(preset: ThemePresets.Preset) {
        getPrefer().edit {
            putInt(Const.KEY_THEME_COLOR, ThemePresets.toInt(preset.accent))
        }
        viewModel.table.courseTextColor = ThemePresets.toInt(preset.courseText)
        viewModel.table.widgetCourseTextColor = ThemePresets.toInt(preset.courseText)
        viewModel.table.strokeColor = ThemePresets.toInt(preset.stroke)
        viewModel.table.widgetStrokeColor = ThemePresets.toInt(preset.stroke)
        viewModel.table.itemAlpha = preset.itemAlpha
        // 换配色方案时背景跟着换：按系统深浅色挑浅色/深色那一套底色，连带把文字色配成能看清的黑/白
        val isNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        viewModel.table.background = WallpaperUtils.solidValue(
                ThemePresets.toInt(if (isNight) preset.bgDark else preset.bg))
        viewModel.table.textColor = if (isNight) 0xFFFFFFFF.toInt() else 0xFF1F2329.toInt()
        // 配色方案也同步到小组件背景：直接换成本方案对应的底色，并配好小组件文字颜色
        val widgetBg = ThemePresets.toInt(if (isNight) preset.bgDark else preset.bg)
        WidgetBackgrounds.set(this, WidgetBackgrounds.solidValue(widgetBg))
        val widgetText = if (WidgetBackgrounds.luminance(widgetBg) < 0.45f) 0xFFFFFFFF.toInt() else 0xFF1F2329.toInt()
        viewModel.table.widgetTextColor = widgetText
        viewModel.table.widgetCourseTextColor = widgetText
        WidgetViews.updateAll(applicationContext)
        Toasty.success(applicationContext, "已套用「${preset.name}」，背景和文字色一起换了\n重主题色要重启 App 才变", Toast.LENGTH_LONG).show()
    }

    private fun showArrangementDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_arrangement, null)
        val etStart = view.findViewById<TextInputEditText>(R.id.et_start)
        val etPeriod = view.findViewById<TextInputEditText>(R.id.et_period)
        val etBreak = view.findViewById<TextInputEditText>(R.id.et_break)
        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("按大节排作息")
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sure, null)
                .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val start = etStart.text?.toString()?.trim() ?: ""
            val period = etPeriod.text?.toString()?.trim()?.toIntOrNull()
            val brk = etBreak.text?.toString()?.trim()?.toIntOrNull()
            if (start.isEmpty() || period == null || brk == null) {
                Toasty.error(applicationContext, "三个都要填哦").show()
                return@setOnClickListener
            }
            if (period !in 10..120 || brk !in 0..120) {
                Toasty.error(applicationContext, "节次时长 10~120 分钟，休息 0~120 分钟").show()
                return@setOnClickListener
            }
            launch {
                try {
                    val n = viewModel.applyArrangement(start, period, brk)
                    Toasty.success(applicationContext, "已生成 $n 节课的作息~").show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toasty.error(applicationContext, "生成失败>_<${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun buildColorPickerDialogBuilder(color: Int, id: Int) {
        ColorPickerFragment.newBuilder()
                .setShowAlphaSlider(true)
                .setColor(color)
                .setDialogId(id)
                .show(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WallpaperUtils.REQUEST_CODE_CHOOSE_BG && resultCode == RESULT_OK) {
            //viewModel.table.background = Matisse.obtainResult(data)[0].toString()
            val uri = data?.data
            if (uri != null) {
                viewModel.table.background = uri.toString()
            }
        }
        if (requestCode == REQUEST_CODE_CHOOSE_TABLE && resultCode == RESULT_OK) {
            viewModel.table.timeTable = data!!.getIntExtra("selectedId", 1)
        }
        if (requestCode == REQUEST_CODE_COVER && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    // 有些相册不给持久授权，那就只在本次运行期间有效，忽略
                }
                TableCoverStore.set(this, viewModel.table.id, uri.toString())
                Toasty.success(applicationContext, "封面图已更新，去「已添课表」看看").show()
            }
        }
    }

    private var isExiting = false

    // 先把界面关掉，再写库、刷小组件。
    // 原来是在 lifecycleScope 的协程里 finish()，落库没完成界面就不响应，
    // 用户以为没效果会连按返回，于是又多弹一层，看起来就是「返回两次直接退到桌面」。
    override fun onBackPressed() {
        if (isExiting) return
        isExiting = true

        val app = applicationContext
        val table = viewModel.table
        val tableId = table.id
        try {
            // 只是单条 UPDATE，阻塞主线程几十毫秒可接受，换来的是一按就退
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                viewModel.saveSettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setResult(RESULT_OK)
        finish()

        // 小组件刷新慢，扔后台，不挡退出
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                WidgetViews.updateAll(app)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
