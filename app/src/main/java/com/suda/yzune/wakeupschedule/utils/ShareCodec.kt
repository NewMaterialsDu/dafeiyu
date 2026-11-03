package com.suda.yzune.wakeupschedule.utils

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * 分享码编解码。
 *
 * 编码流程：5 行 JSON 文本 -> UTF-8 -> raw DEFLATE -> Base64(URL-Safe, 不换行)，前置 "WKP1:"。
 * 用 URL-Safe 字符集是为了让分享码在微信/QQ 等地方复制粘贴时不会被吞字符或自动断行。
 */
object ShareCodec {

    private const val PREFIX = "WKP1:"

    fun encode(raw: String): String {
        val input = raw.toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        val out = ByteArrayOutputStream(input.size / 2)
        try {
            deflater.setInput(input)
            deflater.finish()
            val buffer = ByteArray(4096)
            while (!deflater.finished()) {
                val n = deflater.deflate(buffer)
                if (n <= 0) break
                out.write(buffer, 0, n)
            }
        } finally {
            deflater.end()
        }
        return PREFIX + Base64.encodeToString(out.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun decode(code: String): String {
        // 粘贴时经常带着空格、换行，甚至前后还混着别的聊天内容，先清洗一遍
        var body = code.replace(Regex("\\s"), "")
        val idx = body.indexOf(PREFIX)
        if (idx < 0) throw Exception("这不是一份有效的分享码（应以 WKP1: 开头）")
        body = body.substring(idx + PREFIX.length)
        if (body.isEmpty()) throw Exception("分享码是空的")

        val data = try {
            Base64.decode(body, Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (e: Exception) {
            throw Exception("分享码已损坏，请重新复制完整的分享码")
        }

        val inflater = Inflater(true)
        val out = ByteArrayOutputStream(data.size * 4)
        try {
            inflater.setInput(data)
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val n = inflater.inflate(buffer)
                if (n <= 0) break
                out.write(buffer, 0, n)
            }
        } catch (e: Exception) {
            throw Exception("分享码解压失败，请重新复制完整的分享码")
        } finally {
            inflater.end()
        }

        val text = out.toString("UTF-8")
        if (text.isBlank()) throw Exception("分享码内容是空的")
        return text
    }
}
