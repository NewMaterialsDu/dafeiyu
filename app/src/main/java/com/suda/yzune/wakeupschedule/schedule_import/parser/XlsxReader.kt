package com.suda.yzune.wakeupschedule.schedule_import.parser

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * 极简 xlsx 读取器：把第一张工作表转成 CSV 文本，交给 [CSVParser] 复用同一套解析逻辑。
 *
 * xlsx 本质就是个 zip，里面 xl/sharedStrings.xml 存文本、xl/worksheets/sheetN.xml 存单元格。
 * 表格结构很简单（7 列纯文本/数字），不值得引入 Apache POI。
 */
object XlsxReader {

    fun toCsv(input: InputStream): String {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(BufferedInputStream(input)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // Windows 上有些工具打包会把分隔符写成反斜杠，统一成正斜杠再匹配
                    entries[entry.name.replace('\\', '/')] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        if (entries.isEmpty()) throw Exception("这个文件不是有效的 xlsx")

        val shared = entries["xl/sharedStrings.xml"]
                ?.let { parseSharedStrings(String(it, Charsets.UTF_8)) }
                ?: emptyList()

        val sheetName = entries.keys
                .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
                .minOrNull()
                ?: throw Exception("这个 xlsx 里没有找到工作表")

        return sheetToCsv(String(entries[sheetName]!!, Charsets.UTF_8), shared)
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val result = ArrayList<String>()
        val siRegex = Regex("<si>(.*?)</si>", setOf(RegexOption.DOT_MATCHES_ALL))
        val tRegex = Regex("<t[^>]*>(.*?)</t>", setOf(RegexOption.DOT_MATCHES_ALL))
        siRegex.findAll(xml).forEach { si ->
            // 一个 si 里可能被拆成多个富文本片段，拼起来才是完整内容
            result.add(tRegex.findAll(si.groupValues[1]).joinToString("") { unescape(it.groupValues[1]) })
        }
        return result
    }

    private fun sheetToCsv(xml: String, shared: List<String>): String {
        val sb = StringBuilder()
        val rowRegex = Regex("<row[^>]*>(.*?)</row>", setOf(RegexOption.DOT_MATCHES_ALL))
        val cellRegex = Regex("<c([^>]*?)(/>|>(.*?)</c>)", setOf(RegexOption.DOT_MATCHES_ALL))

        rowRegex.findAll(xml).forEachIndexed { rowIndex, rowMatch ->
            val cells = HashMap<Int, String>()
            cellRegex.findAll(rowMatch.groupValues[1]).forEach { cell ->
                val attrs = cell.groupValues[1]
                val inner = cell.groupValues[3]
                val ref = Regex("r=\"([A-Z]+)\\d+\"").find(attrs)?.groupValues?.get(1) ?: return@forEach
                val type = Regex("t=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: ""
                val col = columnIndex(ref)
                val raw = when {
                    type == "inlineStr" -> Regex("<t[^>]*>(.*?)</t>", setOf(RegexOption.DOT_MATCHES_ALL))
                            .find(inner)?.groupValues?.get(1)?.let { unescape(it) } ?: ""
                    type == "s" -> rawValue(inner).toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                    else -> unescape(rawValue(inner))
                }
                if (raw.isNotEmpty()) cells[col] = raw
            }
            // 空行没有意义，直接跳过，否则 CSVParser 的行号会错位
            if (cells.isEmpty()) return@forEachIndexed
            val maxCol = cells.keys.maxOrNull() ?: 0
            val line = (0..maxCol).joinToString(",") { col ->
                csvCell(normalize(col, cells[col] ?: ""))
            }
            sb.append(line).append("\n")
        }
        if (sb.isEmpty()) throw Exception("工作表是空的")
        return sb.toString()
    }

    private fun rawValue(inner: String): String {
        val m = Regex("<v[^>]*>(.*?)</v>", setOf(RegexOption.DOT_MATCHES_ALL)).find(inner)
        return m?.groupValues?.get(1) ?: ""
    }

    /** 周次列如果被 Excel 自动转成了日期，会变成一串 40000+ 的数字，这里还原成「M月D日」 */
    private fun normalize(col: Int, value: String): String {
        if (col != 6) return value
        val serial = value.toDoubleOrNull() ?: return value
        if (serial < 40000 || serial > 60000) return value
        val days = serial.toInt()
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = 0
        cal.set(1899, 11, 30, 0, 0, 0)
        cal.add(java.util.Calendar.DAY_OF_YEAR, days)
        return "${cal.get(java.util.Calendar.MONTH) + 1}月${cal.get(java.util.Calendar.DAY_OF_MONTH)}日"
    }

    private fun csvCell(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun columnIndex(letters: String): Int {
        var n = 0
        for (c in letters) {
            n = n * 26 + (c - 'A' + 1)
        }
        return n - 1
    }

    private fun unescape(s: String): String {
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&")
    }
}
