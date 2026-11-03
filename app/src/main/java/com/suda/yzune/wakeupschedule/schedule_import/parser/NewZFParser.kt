package com.suda.yzune.wakeupschedule.schedule_import.parser

import com.suda.yzune.wakeupschedule.schedule_import.Common
import com.suda.yzune.wakeupschedule.schedule_import.bean.Course
import org.jsoup.Jsoup

class NewZFParser(source: String) : Parser(source) {

    override fun generateCourseList(): List<Course> {
        val courseList = arrayListOf<Course>()
        val doc = Jsoup.parse(source)
        val table1 = doc.getElementById("table1")
        val trs = table1.getElementsByTag("tr")
        var node = 0
        var day = 0
        var teacher = ""
        var room = ""
        var step = 0
        var startWeek = 0
        var endWeek = 0
        var type = 0
        var timeStr = ""
        val nodeRegex = Regex("""\(.*节\)""")
        for (tr in trs) {
            val nodeStr = tr.getElementsByClass("festival").text()
            if (nodeStr.isEmpty()) {
                continue
            }
            node = nodeStr.toInt()

            val tds = tr.getElementsByTag("td")
            for (td in tds) {
                val divs = td.getElementsByTag("div")
                for (div in divs) {
                    val courseValue = div.text().trim()

                    if (courseValue.length <= 1) {
                        continue
                    }

                    val courseName = div.getElementsByClass("title").text()
                    if (courseName.isEmpty()) {
                        continue
                    }

                    day = td.attr("id")[0].toString().toInt()

                    // 每个格子独立解析，不能沿用上一格的单双周、连堂数
                    type = 0
                    step = 1
                    startWeek = 0
                    endWeek = 0

                    val pList = div.getElementsByTag("p")
                    val weekList = arrayListOf<String>()
                    pList.forEach { e ->
                        when (e.getElementsByAttribute("title").attr("title")) {
                            "教师" -> teacher = e.text().trim()
                            "上课地点" -> room = e.text().trim()
                            "节/周", "周/节" -> {
                                timeStr = e.text().trim()
                                val result = Common.nodePattern.find(timeStr)
                                        ?: Common.nodePattern1.find(timeStr)
                                if (result != null) {
                                    val nodeInfo = result.value
                                    val nodes = nodeInfo.substring(1, nodeInfo.length - 1).split("-".toRegex())
                                            .dropLastWhile { it.isEmpty() }

                                    if (nodes.isNotEmpty()) {
                                        node = nodes[0].toInt()
                                    }
                                    if (nodes.size > 1) {
                                        val endNode = nodes[1].toInt()
                                        step = endNode - node + 1
                                    }
                                }
                                weekList.clear()
                                weekList.addAll(nodeRegex.replace(timeStr, "").split(','))
                            }
                        }

                    }

                    weekList.forEach { raw ->
                        val weekText = raw.trim()
                        if (weekText.isEmpty()) return@forEach

                        // 正方的周次写法五花八门："1-16周"、"第5周"、"5周(调)"、"1-16周(单)"
                        // 统一抓出数字：一个数字=单周，两个=范围。抓不到就跳过这一条，
                        // 绝不能像以前那样兜底成 1-20 周（那会把单周调课变成整学期）
                        // 有的页面把节次和周次连写（"3-4节1-16周"），先把节次部分剔掉再提数字
                        val cleanText = Regex("""\d{1,2}\s*[-~]\s*\d{1,2}\s*节|\d{1,2}\s*节""")
                                .replace(weekText, "")
                        val nums = Regex("""\d{1,2}""").findAll(cleanText)
                                .map { it.value.toInt() }.toList()
                        if (nums.isEmpty()) {
                            android.util.Log.w("NewZF", "无法识别周次，已跳过：「" + weekText + "」 " + courseName)
                            return@forEach
                        }

                        val sw = nums[0]
                        val ew = if (nums.size > 1) nums[1] else sw
                        if (sw <= 0 || ew < sw) return@forEach

                        val weekType = when {
                            weekText.contains('单') -> 1
                            weekText.contains('双') -> 2
                            else -> 0
                        }

                        courseList.add(
                                Course(
                                        name = courseName, room = room,
                                        teacher = teacher, day = day,
                                        startNode = node, endNode = node + step - 1,
                                        startWeek = sw, endWeek = ew,
                                        type = weekType
                                )
                        )
                    }
                }
            }
        }
        return courseList
    }

}