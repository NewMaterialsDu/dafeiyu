package com.suda.yzune.wakeupschedule.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.suda.yzune.wakeupschedule.R

/**
 * 菜单文字统一用「主题色」。
 *
 * 以前侧栏菜单是主题粉、二级/三级设置页却是默认黑/白，看着像两套主题；
 * 这里收成一个取色口：侧栏、设置页标题、各设置条目标题都走它，
 * 高级设置里改「主题颜色」后全站一起变（默认品牌粉 #fa6278）。
 */
fun Context.menuTextColor(): Int =
        getPrefer().getInt(Const.KEY_THEME_COLOR, ContextCompat.getColor(this, R.color.colorAccent))

/** 侧栏菜单用：平时主题色，按下时半透明 */
fun Context.menuTextColorList(): ColorStateList {
    val c = menuTextColor()
    val pressed = Color.argb(0x99, Color.red(c), Color.green(c), Color.blue(c))
    return ColorStateList(arrayOf(intArrayOf(), intArrayOf(android.R.attr.state_pressed)), intArrayOf(c, pressed))
}
