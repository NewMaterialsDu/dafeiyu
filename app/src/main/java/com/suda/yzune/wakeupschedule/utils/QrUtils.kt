package com.suda.yzune.wakeupschedule.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * 二维码生成（ZXing core，纯 Java，不联网）。
 * 分享码整段内容直接编进二维码：课表数据 + 样式 + 作息时间都在里面。
 */
object QrUtils {

    /** 内容太长时返回 null（二维码容量上限：版本 40 + L 级约 2953 字节） */
    fun generate(text: String, sizePx: Int = 720): Bitmap? {
        return try {
            val hints = mapOf(
                    EncodeHintType.MARGIN to 4,
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L
            )
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    pixels[y * w + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        } catch (e: Exception) {
            null
        }
    }
}

/** 二维码图片临时存放：弹窗关掉后，保存到文件的回调还要用 */
object QrShareCache {
    var bitmap: Bitmap? = null
}
