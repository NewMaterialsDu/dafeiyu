package com.suda.yzune.wakeupschedule.utils

/**
 * 预调配色。每套给出：主题强调色、课程文字色、格子边框色、格子不透明度、背景纯色（浅色/深色两套）。
 * 换配色方案时连背景一起换，并且按系统深浅色模式挑合适的那一套，避免「浅底配白字」这种糊成一片。
 */
object ThemePresets {

    data class Preset(
            val name: String,
            val accent: Long,
            val courseText: Long,
            val stroke: Long,
            val itemAlpha: Int,
            val bg: Long,
            val bgDark: Long
    )

    val LIST = listOf(
            Preset("深海蓝", 0xFF3D6FD0, 0xFFFFFFFF, 0x66B9D5F5, 72, 0xFFF2F6FD, 0xFF121A2B),
            Preset("樱花粉", 0xFFE4658E, 0xFFFFFFFF, 0x66F8CFDD, 72, 0xFFFDF3F7, 0xFF2B1720),
            Preset("抹茶绿", 0xFF4CAF87, 0xFFFFFFFF, 0x66C3E8D6, 72, 0xFFF2FAF6, 0xFF10201A),
            Preset("暮光紫", 0xFF7C6BD6, 0xFFFFFFFF, 0x66D3CBF3, 72, 0xFFF5F3FD, 0xFF1A1726),
            Preset("暖阳橙", 0xFFEF8A4B, 0xFFFFFFFF, 0x66F9D3B8, 72, 0xFFFFF6F0, 0xFF241A13),
            Preset("青碧", 0xFF2FA9A0, 0xFFFFFFFF, 0x66BFE6E2, 72, 0xFFF0FAF9, 0xFF0F2020),
            Preset("墨黑", 0xFF39414D, 0xFFFFFFFF, 0x66C9CDD4, 78, 0xFFF3F5F7, 0xFF0F1115),
            Preset("雾灰", 0xFF78909C, 0xFFFFFFFF, 0x66D5DDE2, 68, 0xFFF4F6F8, 0xFF171A1D)
    )

    fun toInt(color: Long): Int = color.toInt()
}
