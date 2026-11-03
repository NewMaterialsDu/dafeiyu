package com.suda.yzune.wakeupschedule.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 原本这些说明都指向作者的在线客服/教程站，这里改成 App 内直接弹出来。
 * 不联网、也不把用户带到任何外部站点。
 */
object Docs {

    const val FAQ = "1. 换课表：左侧栏「已添课表」里可以新建、切换、改名。\n" +
            "2. 换背景：课表设置 →「课程表背景」，内置了几张，也可以从相册选。\n" +
            "3. 改节数/周数：课表设置 →「一天课程节数」「学期周数」。\n" +
            "4. 课表显示不全：检查「学期开始日期」是不是本周，周次会跟着它算。\n" +
            "5. 导入失败：先试「从网页源码导入」，再不行就用「从 Excel / CSV 导入」。\n" +
            "6. 小组件：长按桌面空白处 → 小组件 → 找到「大肥鱼课表」，拖到桌面即可。"

    const val EXPORT = "导出没反应，通常是系统文件选择器的问题：\n" +
            "点选择器右上角三个点 →「显示内部存储设备」，再从侧栏挑一个目录保存。\n" +
            "文件名不要改扩展名，否则导入会失败。"

    const val SCHOOL_TYPE = "不同类型对应不同厂家的教务系统，选错了会解析失败：\n" +
            "· 正方教务：网址里带 default2.aspx 或 jwglxt\n" +
            "· 新正方教务：较新的 jwglxt/xtgl/login_slogin.html\n" +
            "· 强智教务：网址里带 qzdatasoft\n" +
            "· URP 系统：网址里带 urp 或 login\n\n" +
            "实在分不清，就挨个试一遍；也可以直接用「从网页源码导入」，在教务页面把源码存下来再导入。"

    const val WEB_SOURCE = "在电脑或手机浏览器里登录教务，进到「我的课表」页面，\n" +
            "把网页另存为 .html 文件，再用这里导入。\n" +
            "这种方法不依赖任何教务类型判断，最不容易出错。"

    const val TEMPLATE = "表格固定 7 列，第一行是表头，不要改列顺序：\n" +
            "课程名称｜星期几(1-7)｜开始节次｜结束节次｜教师｜地点｜周次\n\n" +
            "周次写法：14-17 或 1-4、6-11、13-18 或 11-13单、14-18\n" +
            "（「、」分隔，单双周加「单」「双」）\n\n" +
            "支持 .xlsx 和 .csv；csv 用 GBK 或 UTF-8 保存都行。"

    /** 用一张图说明问题，比大段文字直观 */
    fun showImage(context: Context, title: String, imageRes: Int) {
        val d = context.resources.displayMetrics.density
        val iv = android.widget.ImageView(context).apply {
            setImageResource(imageRes)
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setPadding((d * 8).toInt(), (d * 8).toInt(), (d * 8).toInt(), 0)
        }
        MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(iv)
                .setPositiveButton("知道了", null)
                .show()
    }

    fun show(context: Context, title: String, content: String) {
        MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("知道了", null)
                .show()
    }
}