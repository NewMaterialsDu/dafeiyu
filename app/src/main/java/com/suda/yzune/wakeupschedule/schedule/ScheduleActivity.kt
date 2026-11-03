package com.suda.yzune.wakeupschedule.schedule

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ShareCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.suda.yzune.wakeupschedule.utils.Docs
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseActivity
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.bean.TableSelectBean
import com.suda.yzune.wakeupschedule.course_add.AddCourseActivity
import com.suda.yzune.wakeupschedule.intro.AboutActivity
import com.suda.yzune.wakeupschedule.schedule_manage.ScheduleManageActivity
import com.suda.yzune.wakeupschedule.schedule_settings.ScheduleSettingsActivity
import com.suda.yzune.wakeupschedule.settings.SettingsActivity
import com.suda.yzune.wakeupschedule.utils.*
import es.dmoral.toasty.Toasty
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.coroutines.delay
import splitties.activities.start
import splitties.dimensions.dip
import splitties.resources.styledDimenPxSize
import splitties.snackbar.action
import splitties.snackbar.longSnack
import java.text.ParseException
import kotlin.math.roundToInt

class ScheduleActivity : BaseActivity() {

    companion object {
        /** 上次刷新小组件的时间，做节流用 */
        private var lastWidgetRefresh = 0L
    }

    private val viewModel by viewModels<ScheduleViewModel>()
    private var mAdapter: SchedulePagerAdapter? = null

    private lateinit var ui: ScheduleActivityUI
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val preLoad by lazy(LazyThreadSafetyMode.NONE) {
        getPrefer().getBoolean(Const.KEY_SCHEDULE_PRE_LOAD, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getPrefer().getBoolean(Const.KEY_HIDE_NAV_BAR, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
        ui = ScheduleActivityUI(this)
        setContentView(ui.root)

        val json = getPrefer().getString(Const.KEY_OLD_VERSION_COURSE, "")
        if (!json.isNullOrEmpty()) {
            launch {
                try {
                    viewModel.updateFromOldVer(json)
                    Toasty.success(applicationContext, "升级成功~").show()
                } catch (e: Exception) {
                    Toasty.error(applicationContext, "出现异常>_<\n${e.message}").show()
                }
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(ui.bottomSheet)

        ui.content.postDelayed({
            if (!getPrefer().getBoolean(Const.KEY_HAS_INTRO, false)) {
                initIntro()
            }
        }, 500)

        initView()
        initNavView()

        // 本地版：不再有捐赠提示、启动上报、版本检查
        viewModel.initTableSelectList().observe(this, Observer {
            if (it == null) return@Observer
            viewModel.tableSelectList.clear()
            viewModel.tableSelectList.addAll(it)
            if (ui.rvTableName.adapter == null) {
                initTableMenu(viewModel.tableSelectList)
            } else {
                ui.rvTableName.adapter?.notifyDataSetChanged()
            }
        })

        initBottomSheetAction()
        requestNotificationPermission()
    }


    /** 上课提醒要靠通知，Android 13+ 得自己弹一次，不能让用户跑去设置里翻 */
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        if (getPrefer().getBoolean(Const.KEY_ASKED_NOTIFICATION, false)) return
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        getPrefer().edit { putBoolean(Const.KEY_ASKED_NOTIFICATION, true) }
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9001)
    }

    private fun initTheme() {
        val bg = viewModel.table.background
        // 纯色（含默认）直接刷底色，不走 Glide —— 之前用 16×16 纯色图 + Glide，
        // 深色主题下会命中 drawable-night 变成一片黑，而且在白底上换色也不生效。
        if (bg.isEmpty() || WallpaperUtils.isSolid(bg)) {
            val color = if (bg.isEmpty()) WallpaperUtils.defaultSolid(this) else WallpaperUtils.solidColor(bg)
            ui.bg.setImageDrawable(null)
            ui.bg.setBackgroundColor(color)
            // 纯色背景下自动配字色，深底黑字看不清这类问题就不用手动调了
            val bgLum = WallpaperUtils.luminance(color)
            val textLum = WallpaperUtils.luminance(viewModel.table.textColor)
            if (bgLum < 0.45f && textLum < 0.45f) {
                viewModel.table.textColor = android.graphics.Color.WHITE
            } else if (bgLum >= 0.45f && textLum >= 0.45f) {
                viewModel.table.textColor = android.graphics.Color.BLACK
            }
        } else {
            ui.bg.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            val x = (ViewUtils.getRealSize(this).x * 0.5).toInt()
            val y = (ViewUtils.getRealSize(this).y * 0.5).toInt()
            Glide.with(this)
                    .load(WallpaperUtils.model(bg))
                    .override(x, y)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            Toasty.error(this@ScheduleActivity, "无法检索背景图片，可能是它为某个应用私有所致，可以尝试在文件管理器中将它移动到其他位置，或是选择其它图片", Toasty.LENGTH_LONG).show()
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                    })
                    .error(R.drawable.main_background_2020_1)
                    .into(ui.bg)
        }

        for (i in 0 until ui.content.childCount) {
            val view = ui.content.getChildAt(i)
            when (view) {
                is AppCompatTextView -> view.setTextColor(viewModel.table.textColor)
                is AppCompatImageButton -> view.setColorFilter(viewModel.table.textColor)
            }
        }

        if (ViewUtils.judgeColorIsLight(viewModel.table.textColor)) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
        }

        viewModel.itemHeight = dip(viewModel.table.itemHeight)
    }

    private fun initTableMenu(data: MutableList<TableSelectBean>) {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val adapter = TableNameAdapter(R.layout.item_table_select_main, data)
        adapter.addChildClickViewIds(R.id.menu_setting)
        adapter.setOnItemChildClickListener { _, view, _ ->
            when (view.id) {
                R.id.menu_setting -> {
                    startActivityForResult(Intent(this,
                            ScheduleSettingsActivity::class.java).apply {
                        putExtra("tableData", viewModel.table)
                    }, Const.REQUEST_CODE_SCHEDULE_SETTING)
                }
            }
        }
        adapter.setOnItemClickListener { _, _, position ->
            if (position < data.size) {
                if (data[position].id != viewModel.table.id) {
                    launch {
                        viewModel.changeDefaultTable(data[position].id)
                        initView()
                        // 换默认课表后刷新桌面小组件
                        com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(applicationContext)
                    }
                }
            }
        }
        ui.rvTableName.adapter = adapter
    }

    private fun initBottomSheetAction() {
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    ui.weekScrollView.smoothScrollTo(if (viewModel.selectedWeek > 4) (viewModel.selectedWeek - 4) * dip(56) else 0, 0)
                    if (ui.weekToggleGroup.checkedButtonId != viewModel.selectedWeek) {
                        ui.weekToggleGroup.check(viewModel.selectedWeek)
                    }
                }
            }
        })
        ui.createScheduleBtn.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.setting_schedule_name)
                    .setView(R.layout.dialog_edit_text)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.sure, null)
                    .create()
            dialog.show()
            val inputLayout = dialog.findViewById<TextInputLayout>(R.id.text_input_layout)
            val editText = dialog.findViewById<TextInputEditText>(R.id.edit_text)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = editText?.text
                if (value.isNullOrBlank()) {
                    inputLayout?.error = "名称不能为空哦>_<"
                } else {
                    launch {
                        try {
                            viewModel.addBlankTable(editText.text.toString())
                            Toasty.success(this@ScheduleActivity, "新建成功~").show()
                        } catch (e: Exception) {
                            Toasty.error(this@ScheduleActivity, "操作失败>_<").show()
                        }
                        dialog.dismiss()
                    }
                }
            }
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        ui.manageScheduleBtn.setOnClickListener {
            startActivityForResult(
                    Intent(this, ScheduleManageActivity::class.java), Const.REQUEST_CODE_SCHEDULE_SETTING)
        }
        ui.changeWeekBtn.setOnClickListener {
            startActivityForResult(Intent(this,
                    ScheduleSettingsActivity::class.java).apply {
                putExtra("tableData", viewModel.table)
                putExtra("settingItem", "当前周")
            }, Const.REQUEST_CODE_SCHEDULE_SETTING)
        }
        ui.timeBtn.setOnClickListener {
            startActivityForResult(Intent(this,
                    ScheduleSettingsActivity::class.java).apply {
                putExtra("tableData", viewModel.table)
                putExtra("settingItem", "上课时间")
            }, Const.REQUEST_CODE_SCHEDULE_SETTING)
        }
        ui.changeBgBtn.setOnClickListener {
            startActivityForResult(Intent(this,
                    ScheduleSettingsActivity::class.java).apply {
                putExtra("tableData", viewModel.table)
                putExtra("settingItem", "课程表背景")
            }, Const.REQUEST_CODE_SCHEDULE_SETTING)
        }
        ui.courseBtn.setOnClickListener {
            start<ScheduleManageActivity> {
                putExtra("jumpToCourse", true)
                putExtra("selectedTable", TableSelectBean(
                        id = viewModel.table.id,
                        background = viewModel.table.background,
                        tableName = viewModel.table.tableName,
                        maxWeek = viewModel.table.maxWeek,
                        nodes = viewModel.table.nodes,
                        type = viewModel.table.type
                ))
            }
        }
        ui.qaBtn.setOnClickListener {
            Docs.show(this, "常见问题", Docs.FAQ)
        }
    }


    private fun initIntro() {
        val builder = Tooltip.Builder(this@ScheduleActivity)
                .overlay(true)
                .maxWidth(dip(240))
                .customView(R.layout.my_tooltip, R.id.tv_tip)
        val navTooltip = builder
                .text("点这里打开左栏")
                .anchor(ui.navBtn)
                .create()
        val jumpTooltip = builder
                .text("点这里快速回到当前周")
                .anchor(ui.weekDayView)
                .create()
        val addBtnTooltip = builder
                .text("点这里手动添加课程")
                .anchor(ui.addBtn)
                .create()
        val importTooltip = builder
                .text("点这里导入课表")
                .anchor(ui.importBtn)
                .create()
        val shareTooltip = builder
                .text("点这里导出、分享课表")
                .anchor(ui.shareBtn)
                .create()
        val moreTooltip = builder
                .text("点这里查看更多设置")
                .anchor(ui.moreBtn)
                .create()
        navTooltip.doOnHidden {
            jumpTooltip.doOnHidden {
                addBtnTooltip.doOnHidden {
                    importTooltip.doOnHidden {
                        shareTooltip.doOnHidden {
                            moreTooltip.doOnHidden {
                                getPrefer().edit {
                                    putBoolean(Const.KEY_HAS_INTRO, true)
                                }
                                showBottomSheetDialog()
                            }.show(ui.content, Tooltip.Gravity.LEFT)
                            moreTooltip.contentView?.findViewById<TextView>(R.id.btn_next)?.apply {
                                text = "完成教程"
                                setOnClickListener {
                                    moreTooltip.hide()
                                }
                            }
                        }.show(ui.content, Tooltip.Gravity.LEFT)
                        shareTooltip.contentView?.findViewById<TextView>(R.id.btn_next)?.setOnClickListener {
                            shareTooltip.hide()
                        }
                    }.show(ui.content, Tooltip.Gravity.LEFT)
                    importTooltip.contentView?.findViewById<TextView>(R.id.btn_next)?.setOnClickListener {
                        importTooltip.hide()
                    }
                }.show(ui.content, Tooltip.Gravity.BOTTOM)
                addBtnTooltip.contentView?.findViewById<TextView>(R.id.btn_next)?.setOnClickListener {
                    addBtnTooltip.hide()
                }
            }.show(ui.content, Tooltip.Gravity.BOTTOM)
            jumpTooltip.contentView?.findViewById<TextView>(R.id.btn_next)?.setOnClickListener {
                jumpTooltip.hide()
            }
        }.show(ui.content, Tooltip.Gravity.RIGHT)
        navTooltip.contentView?.findViewById<TextView>(R.id.btn_next)?.setOnClickListener {
            navTooltip.hide()
        }
    }

    override fun onStart() {
        super.onStart()
        ui.dateView.text = CourseUtils.getTodayDate()
    }

    override fun onResume() {
        super.onResume()
        // 回到课表页时刷一次小组件（节流，避免频繁重绘）。加课/导入/换表后都会走到这里
        val now = System.currentTimeMillis()
        if (now - lastWidgetRefresh > 3000) {
            lastWidgetRefresh = now
            try {
                com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(applicationContext)
                com.suda.yzune.wakeupschedule.reminder.ReminderScheduler.reschedule(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** 使用教程：直接把新手引导在当前页面上再放一遍（不重建 Activity，否则会闪一下） */
    private fun showTutorial() {
        ui.drawerLayout.postDelayed({
            try {
                initIntro()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 260)
    }

    /** 拖动调课：翻到第 week 周 */
    fun setWeekForDrag(week: Int) {
        try {
            val target = (week - 1).coerceIn(0, (ui.viewPager.adapter?.count ?: 1) - 1)
            if (target == ui.viewPager.currentItem) return
            // 拖动会话中直接 setCurrentItem 会被 pager 自己的触摸状态吃掉，post 到下一帧再翻
            ui.viewPager.post {
                try {
                    ui.viewPager.setCurrentItem(target, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun currentWeekForDrag(): Int = viewModel.selectedWeek

    private fun initNavView() {
        ui.navViewStart.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_setting -> {
                    ui.drawerLayout.closeDrawer(GravityCompat.START)
                    ui.drawerLayout.postDelayed({
                        startActivityForResult(Intent(this, SettingsActivity::class.java), Const.REQUEST_CODE_SCHEDULE_SETTING)
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_course -> {
                    ui.drawerLayout.closeDrawer(GravityCompat.START)
                    ui.drawerLayout.postDelayed({
                        start<ScheduleManageActivity> {
                            putExtra("selectedTable", TableSelectBean(
                                    id = viewModel.table.id,
                                    background = viewModel.table.background,
                                    tableName = viewModel.table.tableName,
                                    maxWeek = viewModel.table.maxWeek,
                                    nodes = viewModel.table.nodes,
                                    type = viewModel.table.type
                            ))
                        }
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_tutorial -> {
                    ui.drawerLayout.closeDrawer(GravityCompat.START)
                    ui.drawerLayout.postDelayed({ showTutorial() }, 300)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_about -> {
                    ui.drawerLayout.closeDrawer(GravityCompat.START)
                    ui.drawerLayout.postDelayed({
                        start<AboutActivity>()
                    }, 360)
                    return@setNavigationItemSelectedListener true
                }
                else -> {
                    Toasty.info(this.applicationContext, "敬请期待").show()
                    return@setNavigationItemSelectedListener true
                }
            }
        }
    }

    private fun initViewPage(maxWeek: Int, table: TableBean) {
        if (mAdapter == null) {
            mAdapter = SchedulePagerAdapter(maxWeek, preLoad, supportFragmentManager)
            ui.viewPager.adapter = mAdapter
            ui.viewPager.offscreenPageLimit = 1
        }
        mAdapter!!.maxWeek = maxWeek
        mAdapter!!.notifyDataSetChanged()
        if (CourseUtils.countWeek(table.startDate, table.sundayFirst) > 0) {
            ui.viewPager.currentItem = CourseUtils.countWeek(table.startDate, table.sundayFirst) - 1
        } else {
            ui.viewPager.currentItem = 0
        }
    }

    private fun initEvent() {
        ui.addBtn.setOnClickListener {
            start<AddCourseActivity> {
                putExtra("tableId", viewModel.table.id)
                putExtra("maxWeek", viewModel.table.maxWeek)
                putExtra("nodes", viewModel.table.nodes)
                putExtra("id", -1)
            }
        }


        ui.bottomSheet.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        ui.weekToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                ui.viewPager.currentItem = checkedId - 1
            }
        }

        ui.navBtn.setOnClickListener { ui.drawerLayout.openDrawer(GravityCompat.START) }

        ui.shareBtn.setOnClickListener {
            ExportSettingsFragment().show(supportFragmentManager, null)
        }

        ui.moreBtn.setOnClickListener {
            showBottomSheetDialog()
        }

        ui.importBtn.setOnClickListener {
            ImportChooseFragment().show(supportFragmentManager, "importDialog")
        }

        ui.weekDayView.setOnClickListener {
            ui.weekDayView.text = CourseUtils.getWeekday()
            if (viewModel.currentWeek > 0) {
                ui.viewPager.currentItem = viewModel.currentWeek - 1
            } else {
                ui.viewPager.currentItem = 0
            }
        }

        ui.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageSelected(position: Int) {
                viewModel.selectedWeek = position + 1
                try {
                    if (viewModel.currentWeek > 0) {
                        if (viewModel.selectedWeek == viewModel.currentWeek) {
                            ui.weekView.text = "第${viewModel.selectedWeek}周"
                            ui.weekDayView.text = CourseUtils.getWeekday()
                        } else {
                            ui.weekView.text = "第${viewModel.selectedWeek}周"
                            ui.weekDayView.text = "非本周"
                        }
                    } else {
                        ui.weekView.text = "第${viewModel.selectedWeek}周"
                        ui.weekDayView.text = "还没有开学哦"
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            }

            override fun onPageScrolled(a: Int, b: Float, c: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

    }

    private fun showBottomSheetDialog() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

    }

    private fun initView() {
        launch {
            viewModel.table = viewModel.getDefaultTable()
            viewModel.currentWeek = CourseUtils.countWeek(viewModel.table.startDate, viewModel.table.sundayFirst)
            viewModel.selectedWeek = viewModel.currentWeek
            if (viewModel.currentWeek > 0) {
                if (viewModel.currentWeek <= viewModel.table.maxWeek) {
                    ui.weekView.text = "第${viewModel.currentWeek}周"
                } else {
                    ui.weekView.text = "当前周已超出设定范围"
                    MaterialAlertDialogBuilder(this@ScheduleActivity)
                            .setTitle("提示")
                            .setMessage("发现当前周已超出设定的周数范围，是否去设置修改「当前周」或「开学日期」？")
                            .setPositiveButton("打开设置") { _, _ ->
                                startActivityForResult(Intent(this@ScheduleActivity,
                                        ScheduleSettingsActivity::class.java).apply {
                                    putExtra("tableData", viewModel.table)
                                }, Const.REQUEST_CODE_SCHEDULE_SETTING)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                }
            } else {
                ui.weekView.text = "还没有开学哦"
            }

            ui.weekToggleGroup.removeAllViews()
            ui.weekToggleGroup.clearChecked()
            for (i in 1..viewModel.table.maxWeek) {
                ui.weekToggleGroup.addView(ui.createOutlineButton().apply {
                    id = i
                    text = i.toString()
                    textSize = 12f
                }, dip(48), dip(48))
            }

            launch {
                delay(1000)
                if (ui.weekToggleGroup.checkedButtonId != viewModel.selectedWeek) {
                    ui.weekToggleGroup.check(viewModel.selectedWeek)
                }
                ui.weekScrollView.smoothScrollTo(if (viewModel.selectedWeek > 4) (viewModel.selectedWeek - 4) * dip(56) else 0, 0)
            }

            ui.weekDayView.text = CourseUtils.getWeekday()

            initTheme()

            viewModel.timeList = viewModel.getTimeList(viewModel.table.timeTable)

            viewModel.alphaInt = (255 * (viewModel.table.itemAlpha.toFloat() / 100)).roundToInt()

            initViewPage(viewModel.table.maxWeek, viewModel.table)

            initEvent()

            for (i in 1..7) {
                viewModel.getRawCourseByDay(i, viewModel.table.id).observe(this@ScheduleActivity, Observer { list ->
                    if (list == null) return@Observer
                    if (list.isNotEmpty() && list[0].tableId != viewModel.table.id) return@Observer
                    viewModel.allCourseList[i - 1].value = list
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) {
            when (requestCode) {
                Const.REQUEST_CODE_EXPORT -> {
                    ui.content.longSnack("导出是否遇到了问题？") {
                        action("看看怎么解决") {
                            Docs.show(this@ScheduleActivity, "导出没反应？", Docs.EXPORT)
                        }
                    }
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        when (requestCode) {
            Const.REQUEST_CODE_SCHEDULE_SETTING -> {
                try {
                    initView()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toasty.error(applicationContext, "刷新课表时出了点问题>_<", Toast.LENGTH_LONG).show()
                }
            }
            Const.REQUEST_CODE_IMPORT -> {
                showBottomSheetDialog()
                //ui.drawerLayout.openDrawer(Gravity.END)
                MaterialAlertDialogBuilder(this)
                        .setTitle("温馨提示")
                        .setView(AppCompatTextView(this).apply {
                            text = ViewUtils.getHtmlSpannedString("记得<b><font color='#fa6278'>仔细检查</font></b>有没有少课、课程信息对不对哦，不要到时候<b><font color='#fa6278'>一不小心就翘课</font></b>啦<br>解析算法不是100%可靠的哦<br>但会朝这个方向努力")
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            val space = styledDimenPxSize(R.attr.dialogPreferredPadding)
                            setPadding(space, dip(8), space, 0)
                        })
                        .setCancelable(false)
                        .setPositiveButton("我知道啦", null)
                        .show()
            }
            Const.REQUEST_CODE_EXPORT -> {
                val uri = data?.data
                launch {
                    try {
                        viewModel.exportData(uri)
                        showShareDialog("分享课程文件", uri!!)
                    } catch (e: Exception) {
                        Toasty.error(this@ScheduleActivity, "导出失败>_<${e.message}")
                    }
                }
            }
            Const.REQUEST_CODE_QR_SAVE -> {
                val uri = data?.data
                val bitmap = com.suda.yzune.wakeupschedule.utils.QrShareCache.bitmap
                if (uri != null && bitmap != null) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }
                        Toasty.success(applicationContext, "二维码已保存", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toasty.error(applicationContext, "保存失败>_<${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            Const.REQUEST_CODE_EXPORT_ICS -> {
                val uri = data?.data
                launch {
                    try {
                        viewModel.exportICS(uri)
                        showShareDialog("分享日历文件", uri!!)
                    } catch (e: Exception) {
                        Toasty.error(this@ScheduleActivity, "导出失败>_<${e.message}")
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /** 二维码分享：把整段分享码画成二维码，可保存图片 / 复制文字码 */
    fun showQrShare() {
        launch {
            try {
                val code = com.suda.yzune.wakeupschedule.utils.ShareCodec.encode(viewModel.buildExportString())
                showQrDialog(code)
            } catch (e: Exception) {
                Toasty.error(applicationContext, "生成二维码失败>_<${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showQrDialog(code: String) {
        val qr = com.suda.yzune.wakeupschedule.utils.QrUtils.generate(code, 1000)
        if (qr == null) {
            MaterialAlertDialogBuilder(this)
                    .setTitle("生成不了二维码")
                    .setMessage("这份课表内容比较长（${code.length} 个字符），超过了二维码容量。\n可以先「复制分享码」发给对方粘贴导入。")
                    .setPositiveButton("好", null)
                    .show()
            return
        }
        com.suda.yzune.wakeupschedule.utils.QrShareCache.bitmap = qr
        // 调试包顺手往缓存里存一份原始位图，方便真机核对二维码内容是否可扫
        if (com.suda.yzune.wakeupschedule.BuildConfig.DEBUG) {
            try {
                java.io.File(cacheDir, "qr_share.png").outputStream().use {
                    qr.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_share, null)
        dialogView.findViewById<ImageView>(R.id.iv_qr).setImageBitmap(qr)
        dialogView.findViewById<TextView>(R.id.tv_qr_code).text = code
        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("二维码分享课表")
                .setView(dialogView)
                .setNegativeButton("关闭", null)
                .create()
        dialog.show()
        dialogView.findViewById<android.widget.Button>(R.id.btn_qr_copy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("课表分享码", code))
            Toasty.success(applicationContext, "分享码已复制", Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<android.widget.Button>(R.id.btn_qr_save).setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/png"
                putExtra(Intent.EXTRA_TITLE, "${viewModel.table.tableName}-二维码.png")
            }
            try {
                startActivityForResult(intent, Const.REQUEST_CODE_QR_SAVE)
                dialog.dismiss()
            } catch (e: Exception) {
                Toasty.error(applicationContext, "打不开保存界面>_<", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener { dialog.dismiss() }
    }

    private fun showShareDialog(title: String, uri: Uri) {
        MaterialAlertDialogBuilder(this)
                .setTitle("分享")
                .setMessage("成功导出至你指定的路径啦，是否还要分享出去呢？")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("分享") { _, _ ->
                    val shareIntent = ShareCompat.IntentBuilder.from(this)
                            .setChooserTitle(title)
                            .setStream(uri)
                            .setType("*/*")
                            .createChooserIntent()
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(shareIntent)
                }
                .setCancelable(false)
                .show()
    }

    override fun onBackPressed() {
        when {
            ui.drawerLayout.isDrawerOpen(GravityCompat.START) -> ui.drawerLayout.closeDrawer(GravityCompat.START)
            ui.drawerLayout.isDrawerOpen(GravityCompat.END) -> ui.drawerLayout.closeDrawer(GravityCompat.END)
            bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        ui.viewPager.clearOnPageChangeListeners()
        ui.weekToggleGroup.clearOnButtonCheckedListeners()
        try {
            com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

}