package com.suda.yzune.wakeupschedule.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

fun Context.getPrefer(name: String = "config"): SharedPreferences = getSharedPreferences(name, MODE_PRIVATE)

object Const {

    const val REQUEST_CODE_EXPORT = 100
    const val REQUEST_CODE_IMPORT = 101
    const val REQUEST_CODE_SCHEDULE_SETTING = 102
    const val REQUEST_CODE_EXPORT_ICS = 103
    const val REQUEST_CODE_IMPORT_FILE = 104
    const val REQUEST_CODE_IMPORT_HTML = 105
    const val REQUEST_CODE_IMPORT_CSV = 106
    const val REQUEST_CODE_CHOOSE_SCHOOL = 107
    const val REQUEST_CODE_ADD_COURSE = 108
    const val REQUEST_CODE_QR_SAVE = 109

    const val KEY_OLD_VERSION_COURSE = "course"
    const val KEY_OLD_VERSION_BG_URI = "pic_uri"
    const val KEY_OLD_VERSION_TERM_START = "termStart"
    const val KEY_HAS_ADJUST = "has_adjust"

    const val KEY_IMPORT_SCHOOL = "import_school"
    const val KEY_SCHOOL_URL = "school_url"
    const val KEY_DAY_NIGHT_THEME = "day_night_theme"
    const val KEY_HIDE_NAV_BAR = "hide_main_nav_bar"
    const val KEY_COURSE_REMIND = "course_reminder"
    const val KEY_REMINDER_ON_GOING = "reminder_on_going"
    const val KEY_SILENCE_REMINDER = "silence_reminder"
    const val KEY_HAS_COUNT = "has_count"

    /** 小组件圆角（dp） */
    const val KEY_WIDGET_CORNER = "widget_corner_radius"

    /** 小组件背景："" 跟随课表 / "transparent" / "solid:#AARRGGBB" / "builtin:xxx" */
    const val KEY_WIDGET_BG = "widget_bg"

    /** 小组件是否显示已经上完的课程 */
    const val KEY_WIDGET_SHOW_PAST = "widget_show_past"

    /** 已保存的「课表外观设置快照」JSON 列表 */
    const val KEY_TABLE_STYLE_SNAPSHOTS = "table_style_snapshots"

    /** 已添课表卡片的自定义封面图，按课表 id 存：table_cover_<id> */
    const val KEY_TABLE_COVER_PREFIX = "table_cover_"

    /** 课表是否显示每节课的分隔线 */
    const val KEY_SHOW_GRID_LINE = "show_grid_line"

    /** 分隔线颜色；哨兵值表示「跟随文字色」 */
    const val KEY_GRID_LINE_COLOR = "grid_line_color"
    const val GRID_LINE_COLOR_FOLLOW_TEXT = -1

    /** 分隔线线宽（dp） */
    const val KEY_GRID_LINE_WIDTH = "grid_line_width"

    /** 分隔线经过课程块的位置改用虚线 */
    const val KEY_GRID_LINE_DASH_ON_COURSE = "grid_line_dash_on_course"

    /** 分隔线样式：0 实线 / 1 虚线 / 2 只横线 / 3 只竖线 */
    const val KEY_GRID_LINE_STYLE = "grid_line_style"
    const val GRID_LINE_STYLE_SOLID = 0
    const val GRID_LINE_STYLE_DASHED = 1
    const val GRID_LINE_STYLE_HORIZONTAL = 2
    const val GRID_LINE_STYLE_VERTICAL = 3

    /** 是否已经弹过通知权限申请 */
    const val KEY_ASKED_NOTIFICATION = "asked_notification"

    /** 课程格子圆角半径（dp） */
    const val KEY_ITEM_CORNER = "item_corner_radius"
    const val KEY_CHECK_UPDATE = "s_update"
    const val KEY_DAY_WIDGET_COLOR = "s_colorful_day_widget"
    const val KEY_SHOW_EMPTY_VIEW = "show_empty_view"
    const val KEY_SHOW_SUDA_LIFE = "suda_life"
    const val KEY_THEME_COLOR = "nav_bar_color"
    const val KEY_REMINDER_TIME = "reminder_min"
    const val KEY_OPEN_TIMES = "open_times"
    const val KEY_HAS_INTRO = "has_intro"
    const val KEY_SCHEDULE_PRE_LOAD = "schedule_pre_load"
    const val KEY_SCHEDULE_BLANK_AREA = "schedule_blank_area"
    const val KEY_SCHEDULE_DETAIL_TIME = "schedule_detail_time"

}