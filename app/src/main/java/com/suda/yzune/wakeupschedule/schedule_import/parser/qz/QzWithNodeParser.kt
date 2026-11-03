package com.suda.yzune.wakeupschedule.schedule_import.parser.qz

import com.suda.yzune.wakeupschedule.schedule_import.bean.Course
import org.jsoup.Jsoup

class QzWithNodeParser(source: String) : QzParser(source) {

    override fun convert(day: Int, nodeCount: Int, infoStr: String, courseList: MutableList<Course>) {
        val courseHtml = Jsoup.parse(infoStr)
        val courseName = infoStr.substringBefore("<br>").trim()
        val teacher = courseHtml.getElementsByAttributeValue("title", "老师").text().trim()
        val room = courseHtml.getElementsByAttributeValue(
                "title",
                "教室"
        ).text().trim() + courseHtml.getElementsByAttributeValue("title", "分组").text().trim()
        val tempStr = courseHtml.getElementsByAttributeValue("title", "周次(节次)").text()
        val weekStr = when {
            tempStr.contains(' ') -> courseHtml.getElementsByAttributeValue("title", "周次(节次)").text().split(' ')[0]
            tempStr.isBlank() -> courseHtml.getElementsByAttributeValue("title", "周次").text()
            else -> courseHtml.getElementsByAttributeValue("title", "周次(节次)").text().substringBefore(')')
        }
        val nodeList = when {
            tempStr.contains(' ') -> courseHtml.getElementsByAttributeValue(
                    "title",
                    "周次(节次)"
            ).text().split(' ')[1].removeSurrounding("[", "]").split('-')
            tempStr.isBlank() -> courseHtml.getElementsByAttributeValue(
                    "title",
                    "节次"
            ).text().substringAfter(')').removeSurrounding("[", "]").split('-')
            else -> courseHtml.getElementsByAttributeValue(
                    "title",
                    "周次(节次)"
            ).text().substringAfter(')').removeSurrounding("[", "]").split('-')
        }
        val weekList = weekStr.split(',')
        var startWeek = 0
        var endWeek = 0
        var type = 0
        weekList.forEach { raw ->
            val weekText = raw.trim()
            if (weekText.isEmpty()) return@forEach

            // 强智的周次文字有的带「周」字（"1-16周"），直接 toInt() 会抛异常把整个导入搞崩
            // 统一抓数字：一个=单周，两个=范围，抓不到就跳过
            val nums = Regex("""\d{1,2}""").findAll(weekText)
                    .map { it.value.toInt() }.toList()
            if (nums.isEmpty()) {
                android.util.Log.w("QzNode", "无法识别周次，已跳过：「" + weekText + "」 " + courseName)
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

            try {
                courseList.add(
                        Course(
                                name = courseName, teacher = teacher,
                                room = room, day = day,
                                startNode = nodeList.first().substringBefore('节').replace("""\D""".toRegex(), "").toInt(),
                                endNode = nodeList.last().substringBefore('节').replace("""\D""".toRegex(), "").toInt(),
                                startWeek = sw, endWeek = ew,
                                type = weekType
                        )
                )
            } catch (e: Exception) {
                android.util.Log.w("QzNode", "节次解析失败，已跳过：" + courseName + " / " + e.message)
            }
        }
    }

}