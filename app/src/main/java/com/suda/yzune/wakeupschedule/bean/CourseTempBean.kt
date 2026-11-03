package com.suda.yzune.wakeupschedule.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 临时调课记录：只对某一周生效，不动原课程数据。
 *
 * type:
 *  0 = 调课（把 origDay/origStartNode 那节课挪到 day/startNode）
 *  1 = 停课（这一周不上 origDay/origStartNode 那节课）
 *  2 = 临时课（这一周多出来的课，名字/教室/颜色自带）
 */
@Entity(tableName = "coursetempbean")
data class CourseTempBean(
        @PrimaryKey(autoGenerate = true) var id: Int = 0,
        var tableId: Int = 1,
        var week: Int = 1,
        var type: Int = 0,
        var courseId: Int = -1,
        var origDay: Int = 0,
        var origStartNode: Int = 0,
        var origStartWeek: Int = 0,
        var origEndWeek: Int = 0,
        var origType: Int = 0,
        var day: Int = 0,
        var startNode: Int = 0,
        var step: Int = 1,
        var name: String = "",
        var room: String = "",
        var color: String = ""
)
