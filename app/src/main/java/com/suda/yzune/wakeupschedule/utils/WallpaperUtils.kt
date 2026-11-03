package com.suda.yzune.wakeupschedule.utils

import com.suda.yzune.wakeupschedule.R

/**
 * 课表背景的统一入口。
 *
 * `TableBean.background` 存的可能是：
 *  - ""               → 用默认（纯色）
 *  - "builtin:xxx"    → 内置壁纸（下面这份清单）
 *  - "content://..."  → 用户自己从相册选的图
 *
 * 交给 Glide 之前都要过一遍 [model]，否则内置壁纸会被当成文件路径去加载。
 */
object WallpaperUtils {

    const val PREFIX = "builtin:"
    const val SOLID_PREFIX = "solid:"
    const val REQUEST_CODE_CHOOSE_BG = 23

    data class BuiltinWallpaper(val key: String, val resId: Int, val thumbId: Int, val title: String)

    val LIST = listOf(
            BuiltinWallpaper("${PREFIX}maid", R.drawable.wallpaper_maid, R.drawable.wallpaper_maid_thumb, "深夜鲸鱼娘"),
            BuiltinWallpaper("${PREFIX}sea", R.drawable.wallpaper_sea, R.drawable.wallpaper_sea_thumb, "海底打个招呼"),
            BuiltinWallpaper("${PREFIX}seat", R.drawable.wallpaper_seat, R.drawable.wallpaper_seat_thumb, "深海小憩"),
            BuiltinWallpaper("${PREFIX}sunset", R.drawable.wallpaper_sunset, R.drawable.wallpaper_sunset_thumb, "落日鲸鱼"),
            BuiltinWallpaper("${PREFIX}cloud", R.drawable.wallpaper_cloud, R.drawable.wallpaper_cloud_thumb, "云海妖鲸"),
            BuiltinWallpaper("${PREFIX}white", R.drawable.wallpaper_white, R.drawable.wallpaper_white_thumb, "白裙设定图")
    )

    fun isBuiltin(background: String) = background.startsWith(PREFIX)

    fun isSolid(background: String) = background.startsWith(SOLID_PREFIX)

    fun solidColor(background: String): Int {
        val hex = background.removePrefix(SOLID_PREFIX).trim()
        return try {
            android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (e: Exception) {
            0xFFF7F9FC.toInt()
        }
    }

    /** 默认纯色：跟着系统深浅色走，浅色 #F7F9FC，深色 #14181D */
    fun defaultSolid(context: android.content.Context): Int {
        val night = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        return if (night) 0xFF2E3540.toInt() else 0xFFF7F9FC.toInt()
    }

    /** 0(黑)~1(白) 的相对亮度，用来决定文字该黑还是该白 */
    fun luminance(color: Int): Float {
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    fun solidValue(color: Int): String = SOLID_PREFIX + "#" + Integer.toHexString(color)

    /**
     * 纯色直接刷在 ImageView 上，返回 true 表示已处理完，调用方不用再交给 Glide。
     * Glide 对 ColorDrawable 做 CenterCrop/RoundedCorners 会出问题，纯色走这条捷径。
     */
    fun paintSolid(imageView: android.widget.ImageView, background: String): Boolean {
        if (!isSolid(background)) {
            imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            return false
        }
        imageView.setImageDrawable(null)
        imageView.setBackgroundColor(solidColor(background))
        return true
    }

    /**
     * 返回 Glide 能吃的 model：
     * 内置壁纸是资源 id，纯色是 ColorDrawable，其余（相册图）原样返回 Uri 字符串。
     */
    fun model(background: String): Any {
        if (isSolid(background)) {
            return android.graphics.drawable.ColorDrawable(solidColor(background))
        }
        if (isBuiltin(background)) {
            LIST.firstOrNull { it.key == background }?.let { return it.resId }
        }
        return background
    }
}
