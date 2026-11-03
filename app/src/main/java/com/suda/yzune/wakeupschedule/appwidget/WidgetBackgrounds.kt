package com.suda.yzune.wakeupschedule.appwidget

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.WallpaperUtils
import com.suda.yzune.wakeupschedule.utils.getPrefer

/**
 * 小组件背景库：纯色 / 内置图 / 透明。
 * 每套都带「适合的字体颜色」，选背景时文字自动跟着变，不会出现浅底白字看不清。
 */
data class WidgetBgOption(
        val key: String,
        val title: String,
        val color: Int = 0,
        val imageRes: Int = 0,
        val lightText: Boolean = false
)

object WidgetBackgrounds {

    const val FOLLOW = ""
    const val TRANSPARENT = "transparent"
    private const val SOLID_PREFIX = "solid:"
    private const val BUILTIN_PREFIX = "builtin:"

    const val LIGHT_TEXT = 0xFFFFFFFF.toInt()
    const val DARK_TEXT = 0xFF1F2329.toInt()

    val LIST = listOf(
            WidgetBgOption(FOLLOW, "跟随课表背景（默认）"),
            WidgetBgOption(TRANSPARENT, "透明（露出桌面）"),
            WidgetBgOption(SOLID_PREFIX + "#FFFFFFFF", "白", color = 0xFFFFFFFF.toInt()),
            WidgetBgOption(SOLID_PREFIX + "#FFF6F2EA", "米", color = 0xFFF6F2EA.toInt()),
            WidgetBgOption(SOLID_PREFIX + "#FFEAF1FB", "浅蓝", color = 0xFFEAF1FB.toInt()),
            WidgetBgOption(SOLID_PREFIX + "#FFFBEDF3", "浅粉", color = 0xFFFBEDF3.toInt()),
            WidgetBgOption(SOLID_PREFIX + "#FFEDF7F1", "浅绿", color = 0xFFEDF7F1.toInt()),
            WidgetBgOption(SOLID_PREFIX + "#FFF2EEFB", "浅紫", color = 0xFFF2EEFB.toInt()),
            WidgetBgOption(SOLID_PREFIX + "#FFF3F5F7", "浅灰", color = 0xFFF3F5F7.toInt()),
            WidgetBgOption(SOLID_PREFIX + "#FF1B1F27", "墨黑", color = 0xFF1B1F27.toInt(), lightText = true),
            WidgetBgOption(SOLID_PREFIX + "#FF121A2B", "深海蓝", color = 0xFF121A2B.toInt(), lightText = true),
            WidgetBgOption(SOLID_PREFIX + "#FF0F2020", "墨绿", color = 0xFF0F2020.toInt(), lightText = true),
            WidgetBgOption(BUILTIN_PREFIX + "maid", "深夜鲸鱼娘", imageRes = R.drawable.wallpaper_maid, lightText = true),
            WidgetBgOption(BUILTIN_PREFIX + "sea", "海底打个招呼", imageRes = R.drawable.wallpaper_sea, lightText = true),
            WidgetBgOption(BUILTIN_PREFIX + "seat", "深海小憩", imageRes = R.drawable.wallpaper_seat, lightText = true),
            WidgetBgOption(BUILTIN_PREFIX + "sunset", "落日鲸鱼", imageRes = R.drawable.wallpaper_sunset, lightText = true),
            WidgetBgOption(BUILTIN_PREFIX + "cloud", "云海妖鲸", imageRes = R.drawable.wallpaper_cloud, lightText = true),
            WidgetBgOption(BUILTIN_PREFIX + "white", "白裙设定图", imageRes = R.drawable.wallpaper_white, lightText = false)
    )

    fun get(context: Context): String = context.getPrefer().getString(Const.KEY_WIDGET_BG, FOLLOW) ?: FOLLOW

    fun set(context: Context, key: String) {
        context.getPrefer().edit { putString(Const.KEY_WIDGET_BG, key) }
    }

    fun find(key: String): WidgetBgOption = LIST.firstOrNull { it.key == key } ?: LIST[0]

    fun titleOf(context: Context): String = find(get(context)).title

    /** 0(黑)~1(白) */
    fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    /** 文字颜色按背景亮度自动配黑/白（只在不搭配时才改，保留用户自选色） */
    fun adaptTextColor(textColor: Int, bgLum: Float): Int {
        val lum = luminance(textColor)
        return when {
            bgLum < 0.45f && lum < 0.45f -> LIGHT_TEXT
            bgLum >= 0.45f && lum >= 0.45f -> DARK_TEXT
            else -> textColor
        }
    }

    /** 「跟随课表」时的底色：课表是纯色就用它，是壁纸就退回默认深/浅纯色 */
    fun followColor(context: Context, table: TableBean): Int {
        val bg = table.background
        return when {
            bg.isEmpty() -> WallpaperUtils.defaultSolid(context)
            WallpaperUtils.isSolid(bg) -> WallpaperUtils.solidColor(bg)
            else -> WallpaperUtils.defaultSolid(context)
        }
    }

    /** 「跟随课表」时如果是内置壁纸，返回图片资源 */
    fun followImageRes(table: TableBean): Int {
        val bg = table.background
        if (!WallpaperUtils.isBuiltin(bg)) return 0
        return WallpaperUtils.LIST.firstOrNull { it.key == bg }?.resId ?: 0
    }

    fun solidValue(color: Int): String = SOLID_PREFIX + "#" + Integer.toHexString(color)
}
