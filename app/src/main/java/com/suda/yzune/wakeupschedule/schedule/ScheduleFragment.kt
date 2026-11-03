package com.suda.yzune.wakeupschedule.schedule

import android.graphics.Color
import android.view.DragEvent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.setPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseFragment
import com.suda.yzune.wakeupschedule.bean.CourseBean
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.CourseUtils
import com.suda.yzune.wakeupschedule.utils.ViewUtils
import com.suda.yzune.wakeupschedule.utils.getPrefer
import com.suda.yzune.wakeupschedule.widget.TipTextView
import es.dmoral.toasty.Toasty
import splitties.dimensions.dip

class ScheduleFragment : BaseFragment() {

    private var week = 0
    private var preLoad = true
    private var weekDay = 1
    private lateinit var weekDate: List<String>
    private val viewModel by activityViewModels<ScheduleViewModel>()
    private lateinit var ui: ScheduleUI
    private var isLoaded = false
    // 七天各自的课程数据缓存：算相邻课程（圆角要不要圆）要用到左右两列
    private val dayLists = arrayOfNulls<List<CourseBean>?>(7)
    /** 正在拖动的课程（只影响当前这一周） */
    private var draggingCourse: CourseBean? = null
    private var dragOverlay: View? = null
    /** 拖动要落到哪一周（拖动中用顶部按钮切换，不依赖 ViewPager 翻页） */
    private var dragTargetWeek = 0
    private var tempList: List<com.suda.yzune.wakeupschedule.bean.CourseTempBean> = emptyList()

    private lateinit var showCourseNumber: LiveData<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            week = it.getInt("week")
            preLoad = it.getBoolean("preLoad")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        weekDay = CourseUtils.getWeekdayInt()
        ui = ScheduleUI(context!!, viewModel.table, if (week == viewModel.currentWeek) weekDay else -1)
        ui.showTimeDetail = context!!.getPrefer().getBoolean(Const.KEY_SCHEDULE_DETAIL_TIME, true)
        showCourseNumber = viewModel.getShowCourseNumber(week)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        weekDate = CourseUtils.getDateStringFromWeek(CourseUtils.countWeek(viewModel.table.startDate, viewModel.table.sundayFirst), week, viewModel.table.sundayFirst)
        ((view as ConstraintLayout).getViewById(R.id.anko_tv_title0) as AppCompatTextView).text = weekDate[0] + "\n月"
        var textView: AppCompatTextView?
        for (i in 1..7) {
            if (ui.dayMap[i] == -1) continue
            textView = view.getViewById(R.id.anko_tv_title0 + ui.dayMap[i]) as AppCompatTextView
            if (i == 7 && !viewModel.table.showSat && !viewModel.table.sundayFirst) {
                textView.text = viewModel.daysArray[i] + "\n${weekDate[7]}"
            } else if (!viewModel.table.showSun && viewModel.table.sundayFirst && i != 7) {
                textView.text = viewModel.daysArray[i] + "\n${weekDate[ui.dayMap[i] + 1]}"
            } else {
                textView.text = viewModel.daysArray[i] + "\n${weekDate[ui.dayMap[i]]}"
            }
        }
        if (viewModel.timeList.isNotEmpty() && ui.showTimeDetail) {
            for (i in 0 until viewModel.table.nodes) {
                (ui.content.getViewById(R.id.anko_tv_node1 + i) as FrameLayout).apply {
                    findViewById<AppCompatTextView>(R.id.tv_start).text = viewModel.timeList[i].startTime
                    findViewById<AppCompatTextView>(R.id.tv_end).text = viewModel.timeList[i].endTime
                }
            }
        }
        if (preLoad) {
            observeDayLists()
        }
        // 临时调课/加课改完通知重画

        showCourseNumber.observe(viewLifecycleOwner, Observer {
            if (it == 0) {
                ui.content.visibility = View.GONE
                if (ui.root.getViewById(R.id.anko_empty_view) != null) {
                    return@Observer
                }
                val img = AppCompatImageView(context!!).apply {
                    setImageResource(R.drawable.ic_schedule_empty)
                }
                ui.root.addView(LinearLayoutCompat(context!!).apply {
                    id = R.id.anko_empty_view
                    orientation = LinearLayoutCompat.VERTICAL
                    gravity = Gravity.CENTER
                    if (context.getPrefer().getBoolean(Const.KEY_SHOW_EMPTY_VIEW, true)) {
                        addView(img, LinearLayoutCompat.LayoutParams.WRAP_CONTENT, dip(240))
                    }
                    addView(AppCompatTextView(context).apply {
                        text = "本周没有课程哦"
                        setTextColor(viewModel.table.textColor)
                        gravity = Gravity.CENTER
                    }, LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dip(16)
                    })
                }, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                    startToStart = ConstraintSet.PARENT_ID
                    endToEnd = ConstraintSet.PARENT_ID
                    topToBottom = R.id.anko_tv_title0
                    bottomToBottom = ConstraintSet.PARENT_ID
                    marginStart = context!!.dip(32)
                    marginEnd = context!!.dip(32)
                })
            } else {
                ui.content.visibility = View.VISIBLE
                ui.root.getViewById(R.id.anko_empty_view)?.let { emptyView ->
                    ui.root.removeView(emptyView)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (preLoad) return
        if (!isLoaded) {
            observeDayLists()
            isLoaded = true
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(week: Int, preLoad: Boolean) =
                ScheduleFragment().apply {
                    arguments = Bundle().apply {
                        putInt("week", week)
                        putBoolean("preLoad", preLoad)
                    }
                }
    }

    private fun observeDayLists() {
        for (i in 1..7) {
            viewModel.allCourseList[i - 1].observe(viewLifecycleOwner, Observer { list ->
                dayLists[i - 1] = list
                viewModel.tempRefresh.observe(viewLifecycleOwner, Observer { rebuildAll() })
        // 空白处点击加课：在内容层兜一层（列视图在某些机型/布局下收不到点击）
        var tapX = 0f
        var tapY = 0f
        ui.content.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                tapX = event.x
                tapY = event.y
            }
            false
        }
        ui.content.setOnClickListener {
            if (draggingCourse != null) return@setOnClickListener
            var hitDay = -1
            for (col in 0..6) {
                val v = ui.content.getViewById(R.id.anko_ll_week_panel_0 + col) ?: continue
                if (tapX >= v.left && tapX <= v.right) {
                    hitDay = (1..7).firstOrNull { ui.dayMap[it] == col + 1 } ?: -1
                }
            }
            // 空白格加临时课功能已移除（请用右上角 ＋ 新建课程）
        }
        // 邻列数据到齐后整体重画（必须走 rebuildAll，否则会把"当周调整"覆盖掉）
                rebuildAll()
            })
        }
    }

    private fun rebuildAll() {
        // 调课/临时调整功能已移除：直接画原始课程
        tempList = emptyList()
        for (d in 1..7) {
            dayLists[d - 1]?.let { initWeekPanel(it, d, viewModel.table) }
        }
    }

    /** 本周叠加了调课/停课/临时课之后的数据 */
    private fun adjustedFor(day: Int): List<CourseBean> {
        val raw = dayLists[day - 1] ?: return emptyList()
        val stops = tempList.filter { it.type == 1 && it.origDay == day }
        val outs = tempList.filter { it.type == 0 && it.origDay == day }
        // 同一门课可能有多条时间段（周次不同/单双周不同），必须全字段比对，否则会匹配错甚至匹配不到
        fun sameSlot(c: CourseBean, t: com.suda.yzune.wakeupschedule.bean.CourseTempBean) =
                c.id == t.courseId && c.day == t.origDay && c.startNode == t.origStartNode &&
                        (t.origStartWeek == 0 || c.startWeek == t.origStartWeek) &&
                        (t.origEndWeek == 0 || c.endWeek == t.origEndWeek) &&
                        (t.origType == 0 || c.type == t.origType)
        val list = raw.filter { c ->
            stops.none { sameSlot(c, it) } && outs.none { sameSlot(c, it) }
        }.toMutableList()
        tempList.filter { it.type == 0 && it.day == day }.forEach { mv ->
            // 用记录里存的信息直接画，不去原课程列表里找（重启后原列表时序不稳会找不到）
            list.add(CourseBean(
                    id = -10000 - mv.id,
                    courseName = mv.name,
                    day = mv.day,
                    room = mv.room,
                    teacher = "",
                    startNode = mv.startNode,
                    step = mv.step.coerceAtLeast(1),
                    startWeek = week,
                    endWeek = week,
                    type = 0,
                    color = mv.color.ifEmpty { "#ff1744" },
                    tableId = viewModel.table.id))
        }
        tempList.filter { it.type == 2 && it.day == day }.forEach { tt ->
            list.add(CourseBean(-tt.id, tt.name, tt.day, tt.room, "", tt.startNode, tt.step, week, week, 0,
                    tt.color.ifEmpty { "#" + Integer.toHexString(ViewUtils.getCustomizedColor(activity!!, tt.id % 9)) },
                    viewModel.table.id))
        }
        return list.sortedWith(compareBy({ it.startNode }, { it.id }))
    }

    /** 开始拖动：整屏浮层负责翻周、落点、删除区 */
    private fun startDragWithOverlay(course: CourseBean, block: View) {
        val act = activity ?: return
        draggingCourse = course
        val root = act.findViewById<android.view.ViewGroup>(android.R.id.content) ?: return
        val w = act.resources.displayMetrics.widthPixels
        val h = act.resources.displayMetrics.heightPixels
        val overlay = FrameLayout(act)
        val bar = AppCompatTextView(act).apply {
            text = "拖到这里删除（只删这一周）"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundColor(0xDDD32F2F.toInt())
        }
        overlay.addView(bar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, context!!.dip(72), Gravity.BOTTOM))
        // 顶部：另一根手指点按钮切换「要落到哪一周」，不用管 ViewPager 翻页
        dragTargetWeek = (act as? ScheduleActivity)?.currentWeekForDrag() ?: week
        val weekBar = LinearLayoutCompat(act).apply {
            orientation = LinearLayoutCompat.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xCC000000.toInt())
            setPadding(context!!.dip(12), context!!.dip(8), context!!.dip(12), context!!.dip(8))
        }
        val prevBtn = AppCompatTextView(act).apply {
            text = "‹"
            setTextColor(Color.WHITE); textSize = 24f; gravity = Gravity.CENTER
            setPadding(context!!.dip(20), 0, context!!.dip(20), 0)
        }
        val weekBadge = AppCompatTextView(act).apply {
            setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER
        }
        val nextBtn = AppCompatTextView(act).apply {
            text = "›"
            setTextColor(Color.WHITE); textSize = 24f; gravity = Gravity.CENTER
            setPadding(context!!.dip(20), 0, context!!.dip(20), 0)
        }
        weekBar.addView(prevBtn)
        weekBar.addView(weekBadge)
        weekBar.addView(nextBtn)
        overlay.addView(weekBar, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = context!!.dip(64)
        })
        fun refreshBadge() {
            weekBadge.text = "放到第 " + dragTargetWeek + " 周  (‹ › 切换)"
        }
        fun stepWeek(delta: Int) {
            // 跨周调课已移除：拖到屏幕边缘或点 ‹ › 都不再切周
        }
        prevBtn.setOnClickListener { stepWeek(-1) }
        nextBtn.setOnClickListener { stepWeek(1) }
        refreshBadge()
        // 拖动过程中，另一根手指左右滑也能切周
        var fingerDownX = 0f
        overlay.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> fingerDownX = ev.rawX
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = ev.rawX - fingerDownX
                    if (kotlin.math.abs(dx) > act.resources.displayMetrics.density * 60) {
                        val cur = (activity as? ScheduleActivity)?.currentWeekForDrag() ?: week
                        (activity as? ScheduleActivity)?.setWeekForDrag(if (dx > 0) cur - 1 else cur + 1)
                        refreshBadge()
                    }
                }
            }
            false
        }
        var lastSwitch = 0L
        overlay.setOnDragListener { _, ev ->
            when (ev.action) {
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val now = System.currentTimeMillis()
                    if (now - lastSwitch > 700) {
                        val cur = (activity as? ScheduleActivity)?.currentWeekForDrag() ?: week
                        if (ev.x < w * 0.12f) {
                            stepWeek(-1)
                            lastSwitch = now
                        } else if (ev.x > w * 0.88f) {
                            stepWeek(1)
                            lastSwitch = now
                        }
                    }
                }
                DragEvent.ACTION_DROP -> {
                    val inDelete = ev.y > h - context!!.dip(96)
                    finishDrag(inDelete, ev.x, ev.y)
                }
                DragEvent.ACTION_DRAG_ENDED -> removeDragOverlay()
            }
            true
        }
        root.addView(overlay, android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT))
        dragOverlay = overlay
        block.startDragAndDrop(null, View.DragShadowBuilder(block), null, 0)
    }

    /** 写完库后强制重画：切到目标周 + 重新载入本周调整（不依赖 LiveData 时序） */
    private fun forceRedraw(targetWeek: Int) {
        // 桌面小组件也要跟着刷新（否则 App 里变了、桌面还是旧的）
        try {
            com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(context!!.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        (activity as? ScheduleActivity)?.setWeekForDrag(targetWeek)
        ui.content.postDelayed({
            try {
                rebuildAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 120)
    }

    private fun removeDragOverlay() {
        (dragOverlay?.parent as? android.view.ViewGroup)?.removeView(dragOverlay)
        dragOverlay = null
    }

    /**
     * 由手指 y 算第几节。课表里 1-2、3-4、5-6… 是一大节，
     * 拖动统一按「大节」对齐（不会停在 8 这种半截位置），识别更稳。
     */
    private fun nodeOfY(y: Float): Int {
        val cell = (viewModel.itemHeight + viewModel.marTop).coerceAtLeast(1)
        val raw = (((y - viewModel.marTop) / cell).toInt() + 1).coerceIn(1, viewModel.table.nodes)
        // 吸附到所在大节的起始节（一大节 = 2 节）
        val node = if (raw % 2 == 1) raw else raw - 1
        return node.coerceIn(1, viewModel.table.nodes)
    }

    private fun finishDrag(inDelete: Boolean, rawX: Float, rawY: Float) {
        val course = draggingCourse ?: return
        draggingCourse = null
        removeDragOverlay()
        val targetWeek = week   // 跨周调课已移除：只调整当前这一周
        // 第二次拖动：拖的块本身是上次调整生成的（id 为负），按原记录 id 原地更新，避免又叠一条出来
        val synthRecordId = if (course.id <= -10000) -10000 - course.id else -1
        // 合成块 id = -10000 - 记录id，记录 id 从 1 开始；用 >=0 判断才不漏掉 id=0 的老记录
        val loc = IntArray(2)
        ui.content.getLocationOnScreen(loc)
        val y = rawY - loc[1]
        val x = rawX - loc[0]
        launch {
            try {
                if (inDelete) {
                    if (synthRecordId >= 0) {
                        com.suda.yzune.wakeupschedule.utils.WeekAdjustStore.remove(context!!, viewModel.table.id, synthRecordId)
                    } else {
                        com.suda.yzune.wakeupschedule.utils.WeekAdjustStore.add(context!!, viewModel.table.id,
                                com.suda.yzune.wakeupschedule.bean.CourseTempBean(
                                        tableId = viewModel.table.id, week = targetWeek, type = 1,
                                        courseId = course.id, origDay = course.day, origStartNode = course.startNode,
                                        origStartWeek = course.startWeek, origEndWeek = course.endWeek, origType = course.type))
                    }
                    forceRedraw(targetWeek)
                    Toasty.success(context!!, "第" + targetWeek + "周已去掉这节课").show()
                } else {
                    var hitDay = -1
                    for (col in 0..6) {
                        val v = ui.content.getViewById(R.id.anko_ll_week_panel_0 + col) ?: continue
                        if (x >= v.left && x <= v.right) {
                            hitDay = (1..7).firstOrNull { ui.dayMap[it] == col + 1 } ?: -1
                        }
                    }
                    if (hitDay < 0) return@launch
                    val node = nodeOfY(y)
                    val step = course.step.coerceAtMost(viewModel.table.nodes - node + 1).coerceAtLeast(1)
                    // 这块可能本来就是「本周调整」出来的（第二次拖动）：沿用原来那条记录的原时段，
                    // 只改目标位置，否则会以「调整后的位置」当原位置，匹配不到就消失
                    if (synthRecordId >= 0) {
                        // 已有调整记录：直接把目标位置改掉
                        val rec = com.suda.yzune.wakeupschedule.utils.WeekAdjustStore
                                .load(context!!, viewModel.table.id).firstOrNull { it.id == synthRecordId }
                        if (rec != null) {
                            com.suda.yzune.wakeupschedule.utils.WeekAdjustStore.update(context!!, viewModel.table.id,
                                    rec.copy(day = hitDay, startNode = node, step = step))
                            forceRedraw(targetWeek)
                            Toasty.success(context!!, "已再次移动").show()
                            return@launch
                        }
                    }
                    val existing = try {
                        com.suda.yzune.wakeupschedule.utils.WeekAdjustStore
                                .load(context!!, viewModel.table.id).filter { it.week == targetWeek }.firstOrNull {
                            it.type == 0 && it.courseId == course.id &&
                                    it.day == course.day && it.startNode == course.startNode
                        }
                    } catch (e: Exception) {
                        null
                    }
                    val bean = if (existing != null) {
                        existing.copy(day = hitDay, startNode = node, step = step)
                    } else {
                        com.suda.yzune.wakeupschedule.bean.CourseTempBean(
                                tableId = viewModel.table.id, week = targetWeek, type = 0,
                                courseId = course.id, origDay = course.day, origStartNode = course.startNode,
                                origStartWeek = course.startWeek, origEndWeek = course.endWeek, origType = course.type,
                                day = hitDay, startNode = node, step = step,
                                name = course.courseName, room = course.room.orEmpty(), color = course.color)
                    }
                    com.suda.yzune.wakeupschedule.utils.WeekAdjustStore.add(context!!, viewModel.table.id, bean)
                    forceRedraw(targetWeek)
                    val dn = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[hitDay - 1]
                    Toasty.success(context!!, "第" + targetWeek + "周已调到 " + dn + " 第 " + node + " 节").show()
                }
            } catch (e: Exception) {
                Toasty.error(context!!, "操作失败>_<" + e.message).show()
            }
        }
    }

    /** 空白格子点击：只给这一周加一节临时课 */
    private fun showQuickAddDialog(day: Int, node: Int) {
        val ctx = activity ?: return
        val container = LinearLayoutCompat(ctx).apply {
            orientation = LinearLayoutCompat.VERTICAL
            setPadding(context!!.dip(24), context!!.dip(8), context!!.dip(24), 0)
        }
        fun lp() = LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT)
        val etName = androidx.appcompat.widget.AppCompatEditText(ctx).apply { hint = "课名（必填）" }
        val etRoom = androidx.appcompat.widget.AppCompatEditText(ctx).apply { hint = "教室（可不填）" }
        container.addView(etName, lp().apply { topMargin = context!!.dip(8) })
        container.addView(etRoom, lp().apply { topMargin = context!!.dip(8) })
        val dialog = MaterialAlertDialogBuilder(ctx)
                .setTitle("加课（第" + week + "周 周" + day + " 第 " + node + " 节）")
                .setView(container)
                .setPositiveButton("添加", null)
                .setNegativeButton(R.string.cancel, null)
                .create()
        dialog.show()
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) { etName.error = "课名不能为空哦"; return@setOnClickListener }
            val room = etRoom.text?.toString()?.trim().orEmpty()
            dialog.dismiss()
            launch {
                try {
                    com.suda.yzune.wakeupschedule.utils.WeekAdjustStore.add(context!!, viewModel.table.id,
                            com.suda.yzune.wakeupschedule.bean.CourseTempBean(
                                    tableId = viewModel.table.id, week = week, type = 2,
                                    day = day, startNode = node, step = 2, name = name, room = room))
                    rebuildAll()
                    try {
                        com.suda.yzune.wakeupschedule.appwidget.WidgetViews.updateAll(ctx.applicationContext)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                    Toasty.success(ctx, "加好了，只对第" + week + "周生效").show()
                } catch (e: Exception) {
                    Toasty.error(ctx, "加课失败>_<" + e.message).show()
                }
            }
        }
    }

    /** 与 initWeekPanel 循环里一致的可见性判断，用来算相邻课程 */
    private fun isCourseVisible(c: CourseBean, week: Int, table: TableBean): Boolean {
        if (c.endWeek < week) return false
        val otherWeek = (week % 2 == 0 && c.type == 1) || (week % 2 == 1 && c.type == 2) || (c.startWeek > week)
        return table.showOtherWeekCourse || !otherWeek
    }

    private fun initWeekPanel(data: List<CourseBean>?, day: Int, table: TableBean) {
        val ll = ui.content.getViewById(R.id.anko_ll_week_panel_0 + ui.dayMap[day] - 1) as FrameLayout?
                ?: return
        ll.removeAllViews()
        // 拖动落点 / 空白格子点击加课
        if (data == null || data.isEmpty()) return

        // 圆角只在「露在外面」的角上，贴着相邻课程的角保持直角，这样块铺满格子又不会裂出背景缝
        val cornerRadius = context!!.getPrefer().getInt(Const.KEY_ITEM_CORNER, 0)
        val visibleHere = data.filter { isCourseVisible(it, week, table) }
        val col = ui.dayMap[day]
        val leftDay = (1..7).firstOrNull { ui.dayMap[it] == col - 1 }
        val rightDay = (1..7).firstOrNull { ui.dayMap[it] == col + 1 }
        val leftCourses: List<CourseBean> = leftDay?.let { d ->
            adjustedFor(d).filter { isCourseVisible(it, week, table) }
        } ?: emptyList()
        val rightCourses: List<CourseBean> = rightDay?.let { d ->
            adjustedFor(d).filter { isCourseVisible(it, week, table) }
        } ?: emptyList()

        var isCovered = false
        var pre = data[0]
        for (i in data.indices) {
            val c = data[i]

            // 过期的不显示
            if (c.endWeek < week) {
                continue
            }

            val isOtherWeek = (week % 2 == 0 && c.type == 1) || (week % 2 == 1 && c.type == 2)
                    || (c.startWeek > week)

            if (!table.showOtherWeekCourse && isOtherWeek) continue

            var isError = false

            val strBuilder = StringBuilder()
            if (c.step <= 0) {
                c.step = 1
                isError = true
                Toasty.info(context!!, R.string.error_course_data, Toast.LENGTH_LONG).show()
            }
            if (c.startNode <= 0) {
                c.startNode = 1
                isError = true
                Toasty.info(context!!, R.string.error_course_data, Toast.LENGTH_LONG).show()
            }
            if (c.startNode > table.nodes) {
                c.startNode = table.nodes
                isError = true
                Toasty.info(context!!, R.string.error_course_node, Toast.LENGTH_LONG).show()
            }
            if (c.startNode + c.step - 1 > table.nodes) {
                c.step = table.nodes - c.startNode + 1
                isError = true
                Toasty.info(context!!, R.string.error_course_node, Toast.LENGTH_LONG).show()
            }

            val textView = TipTextView(context!!)
            run {
                val nodeStart = c.startNode
                val nodeEnd = c.startNode + c.step - 1
                fun coversStart(list: List<CourseBean>) = list.any {
                    it.startNode <= nodeStart && it.startNode + it.step - 1 >= nodeStart
                }
                fun coversEnd(list: List<CourseBean>) = list.any {
                    it.startNode <= nodeEnd && it.startNode + it.step - 1 >= nodeEnd
                }
                val hasTop = visibleHere.any { it !== c && it.startNode + it.step == nodeStart }
                val hasBottom = visibleHere.any { it !== c && it.startNode == nodeEnd + 1 }
                // 左右相邻要按「角所在的那一节」判断：块只跟隔壁列部分重叠时，另一半仍是外侧角
                val leftAtTop = coversStart(leftCourses)
                val leftAtBottom = coversEnd(leftCourses)
                val rightAtTop = coversStart(rightCourses)
                val rightAtBottom = coversEnd(rightCourses)
                textView.setCornerRadii(
                        if (!hasTop && !leftAtTop) cornerRadius else 0,
                        if (!hasTop && !rightAtTop) cornerRadius else 0,
                        if (!hasBottom && !rightAtBottom) cornerRadius else 0,
                        if (!hasBottom && !leftAtBottom) cornerRadius else 0)
            }

            if (ll.childCount != 0) {
                isCovered = (pre.startNode == c.startNode)
            }

            textView.setPadding(context!!.dip(4))

            if (c.color.isEmpty()) {
                c.color = "#${Integer.toHexString(ViewUtils.getCustomizedColor(activity!!, c.id % 9))}"
            }

            strBuilder.append(c.courseName)

            if (c.room != "") {
                strBuilder.append("\n@${c.room}")
            }

            if (isOtherWeek) {
                when (c.type) {
                    1 -> strBuilder.append("\n单周")
                    2 -> strBuilder.append("\n双周")
                }
                strBuilder.append("[非本周]")
                textView.visibility = View.VISIBLE
            } else {
                when (c.type) {
                    1 -> strBuilder.append("\n单周")
                    2 -> strBuilder.append("\n双周")
                }
            }

            if (isCovered) {
                val tv = ll.getChildAt(ll.childCount - 1) as TipTextView?
                if (tv != null) {
                    if (tv.tipVisibility == TipTextView.TIP_OTHER_WEEK) {
                        tv.visibility = View.INVISIBLE
                    }
                }
            }

            val tv = ll.findViewWithTag<TipTextView?>(c.startNode)
            if (tv != null) {
                textView.visibility = View.INVISIBLE
                if (tv.tipVisibility != TipTextView.TIP_VISIBLE && !isOtherWeek) {
                    if (tv.tipVisibility != TipTextView.TIP_ERROR) {
                        tv.tipVisibility = TipTextView.TIP_VISIBLE
                    }
                    tv.setOnClickListener {
                        MultiCourseFragment.newInstance(week, c.day, c.startNode).show(parentFragmentManager, "multi")
                    }
                }
            }

            if (isError) {
                textView.tipVisibility = TipTextView.TIP_ERROR
            }

            if (!isOtherWeek) {
                textView.tag = c.startNode
            } else {
                textView.tipVisibility = TipTextView.TIP_OTHER_WEEK
            }

            if (table.showTime && viewModel.timeList.isNotEmpty()) {
                strBuilder.insert(0, viewModel.timeList[c.startNode - 1].startTime + "\n")
            }

            textView.init(
                    text = strBuilder.toString(),
                    txtSize = table.itemTextSize,
                    txtColor = table.courseTextColor,
                    bgColor = Color.parseColor(c.color),
                    bgAlpha = viewModel.alphaInt,
                    stroke = table.strokeColor
            )

            textView.setOnClickListener {
                if (c.id <= -10000) {
                    // 这一节是「本周调整」生成出来的块（调课后的位置 或 临时加的课）
                    val recId = -10000 - c.id
                    val rec = try {
                        com.suda.yzune.wakeupschedule.utils.WeekAdjustStore
                                .load(context!!, viewModel.table.id).firstOrNull { it.id == recId }
                    } catch (e: Exception) {
                        null
                    }
                    if (rec != null && rec.type == 0) {
                        // 调课后的块：删除 = 把整门课删掉（否则会"回到原位"被新课盖住）
                        MaterialAlertDialogBuilder(activity!!)
                                .setTitle(c.courseName)
                                .setMessage("删除这门课吗？（会把它的所有时间段一起删掉）")
                                .setPositiveButton("删除") { _, _ ->
                                    launch {
                                        try {
                                            viewModel.deleteWholeCourse(com.suda.yzune.wakeupschedule.bean.CourseBean(
                                                    id = rec.courseId, courseName = c.courseName, day = c.day,
                                                    room = c.room, teacher = "", startNode = c.startNode,
                                                    step = c.step, startWeek = week, endWeek = week, type = 0,
                                                    color = c.color, tableId = viewModel.table.id))
                                            com.suda.yzune.wakeupschedule.utils.WeekAdjustStore.remove(context!!, viewModel.table.id, recId)
                                            rebuildAll()
                                            Toasty.success(context!!, "已删除").show()
                                        } catch (e: Exception) {
                                            Toasty.error(context!!, "删除失败>_<" + e.message).show()
                                        }
                                    }
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .show()
                        return@setOnClickListener
                    }
                    MaterialAlertDialogBuilder(activity!!)
                            .setTitle(c.courseName)
                            .setMessage("这是第" + week + "周临时加的课，要删掉吗？")
                            .setPositiveButton("删除") { _, _ ->
                                launch {
                                    try {
                                        android.util.Log.e("DEL", "remove temp recId=" + recId + " table=" + viewModel.table.id)
                                        com.suda.yzune.wakeupschedule.utils.WeekAdjustStore.remove(context!!, viewModel.table.id, recId)
                                        rebuildAll()
                                        Toasty.success(context!!, "已删除").show()
                                    } catch (e: Exception) {
                                        Toasty.error(context!!, "删除失败>_<" + e.message).show()
                                    }
                                }
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    return@setOnClickListener
                }
                try {
                    val detailFragment = CourseDetailFragment.newInstance(c)
                    detailFragment.show(parentFragmentManager, "courseDetail")
                } catch (e: Exception) {
                    //TODO: 提示是否要删除异常的数据
                    Toasty.error(activity!!.applicationContext, "哎呀>_<差点崩溃了").show()
                }
            }
            // 调课（拖动）功能已移除，长按不再做任何事

            // 高度取「节数 × (格高 + 行距)」，正好从本条横线铺到下一条横线，不在块下面留缝
            ll.addView(textView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    (viewModel.itemHeight + viewModel.marTop) * c.step).apply {
                gravity = Gravity.TOP
                topMargin = (c.startNode - 1) * (viewModel.itemHeight + viewModel.marTop) + viewModel.marTop
            })

            pre = c
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isLoaded = false
    }

}