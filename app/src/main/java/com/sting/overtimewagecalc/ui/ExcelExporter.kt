package com.sting.overtimewagecalc.ui

import android.content.Context
import android.os.Environment
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Excel 导出器(v2.0)
 *  - 单 sheet 包含两个区块:日历视图 + 明细列表
 *  - 输出路径:Downloads/OverTimeWageCalc_<year>-<month>.xlsx
 *  - 用 Apache POI(标准 .xlsx 库,APK 增加约 4-5MB)
 */
object ExcelExporter {

    fun exportCurrentMonth(
        context: Context,
        yearMonth: YearMonth,
        entries: List<DayEntry>,
        settings: Settings
    ): String {
        val filename = "OverTimeWageCalc_${yearMonth.year}-${"%02d".format(yearMonth.monthValue)}.xlsx"

        // 写文件
        val workbook = XSSFWorkbook()
        try {
            val sheet = workbook.createSheet("${yearMonth.year}年${yearMonth.monthValue}月")
            writeCalendarBlock(sheet, yearMonth, entries, settings)
            writeDetailsBlock(sheet, yearMonth, entries, settings)
            autoSizeColumns(sheet, 12)
        } catch (e: Exception) {
            workbook.close()
            return "导出失败:${e.message}"
        }

        // 保存到 Downloads
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        val file = File(downloads, filename)
        try {
            FileOutputStream(file).use { out ->
                workbook.write(out)
            }
        } catch (e: Exception) {
            workbook.close()
            return "保存失败:${e.message}"
        }
        workbook.close()

        return "已导出到:\n${file.absolutePath}"
    }

    /** 区块 1:日历视图(7 列 × 多行,7 列 = 日 一 二 三 四 五 六) */
    private fun writeCalendarBlock(
        sheet: Sheet,
        yearMonth: YearMonth,
        entries: List<DayEntry>,
        settings: Settings
    ) {
        // 标题
        val titleRow = sheet.createRow(0)
        titleRow.createCell(0).setCellValue("📅 ${yearMonth.year} 年 ${yearMonth.monthValue} 月  日历视图")
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))

        // 星期表头
        val headerRow = sheet.createRow(1)
        listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { i, label ->
            headerRow.createCell(i).setCellValue(label)
        }

        val firstDay = yearMonth.atDay(1)
        val firstDayOfWeek = firstDay.dayOfWeek.value % 7  // 周日=0
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        val entryByDate = entries.associateBy { it.date }

        for (row in 0 until rows) {
            val excelRow = sheet.createRow(2 + row)
            for (col in 0..6) {
                val cellIndex = row * 7 + col
                val dayNum = cellIndex - firstDayOfWeek + 1
                val cell = excelRow.createCell(col)
                if (dayNum in 1..daysInMonth) {
                    val date = yearMonth.atDay(dayNum)
                    val entry = entryByDate[date]
                    val text = buildString {
                        append(dayNum)
                        if (entry != null && !entry.isEmpty) {
                            append("\n¥").append("%.2f".format(entry.totalWage(settings)))
                            val parts = mutableListOf<String>()
                            if (entry.dailyWageEnabled) {
                                val daily = if (entry.dailyRate > 0) entry.dailyRate else settings.defaultDailyRate
                                parts.add("日薪${"%.0f".format(daily)}")
                            }
                            if (entry.overtimeHours > 0) {
                                val hourly = if (entry.hourlyRate > 0) entry.hourlyRate else settings.defaultHourlyRate
                                parts.add("加班×${entry.overtimeMultiplier}×${entry.overtimeHours}h")
                            }
                            if (entry.extraOvertime > 0) {
                                parts.add("额外${"%.0f".format(entry.extraOvertime)}")
                            }
                            if (parts.isNotEmpty()) {
                                append("\n").append(parts.joinToString(" + "))
                            }
                            if (entry.dayNote.isNotBlank()) {
                                append("\n📝").append(entry.dayNote)
                            }
                        }
                    }
                    cell.setCellValue(text)
                }
            }
        }
    }

    /** 区块 2:明细列表(每行一天,所有字段) */
    private fun writeDetailsBlock(
        sheet: Sheet,
        yearMonth: YearMonth,
        entries: List<DayEntry>,
        settings: Settings
    ) {
        val startRow = (sheet.lastRowNum + 3)
        val sorted = entries.filter { !it.isEmpty }.sortedBy { it.date }

        // 标题
        val titleRow = sheet.createRow(startRow)
        titleRow.createCell(0).setCellValue("📋 本月明细(${sorted.size} 天)")
        sheet.addMergedRegion(CellRangeAddress(startRow, startRow, 0, 10))

        // 表头
        val headers = listOf(
            "日期", "星期", "启用日薪", "日薪", "时薪", "倍数",
            "加班小时", "加班费", "额外加班", "备注", "当天合计"
        )
        val headerRow = sheet.createRow(startRow + 1)
        headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

        // 数据行
        sorted.forEachIndexed { i, entry ->
            val excelRow = sheet.createRow(startRow + 2 + i)
            val weekday = entry.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
            val hourly = if (entry.hourlyRate > 0) entry.hourlyRate else settings.defaultHourlyRate
            val daily = if (entry.dailyRate > 0) entry.dailyRate else settings.defaultDailyRate
            val overtimePay = hourly * entry.overtimeMultiplier * entry.overtimeHours

            excelRow.createCell(0).setCellValue(entry.date.toString())
            excelRow.createCell(1).setCellValue(weekday)
            excelRow.createCell(2).setCellValue(if (entry.dailyWageEnabled) "✓" else "")
            excelRow.createCell(3).setCellValue(if (entry.dailyWageEnabled) daily else 0.0)
            excelRow.createCell(4).setCellValue(hourly)
            excelRow.createCell(5).setCellValue(entry.overtimeMultiplier)
            excelRow.createCell(6).setCellValue(entry.overtimeHours)
            excelRow.createCell(7).setCellValue(overtimePay)
            excelRow.createCell(8).setCellValue(entry.extraOvertime)
            excelRow.createCell(9).setCellValue(entry.dayNote)
            excelRow.createCell(10).setCellValue(entry.totalWage(settings))
        }

        // 合计行
        if (sorted.isNotEmpty()) {
            val totalRow = sheet.createRow(startRow + 2 + sorted.size)
            totalRow.createCell(0).setCellValue("本月合计")
            sheet.addMergedRegion(CellRangeAddress(startRow + 2 + sorted.size, startRow + 2 + sorted.size, 0, 9))
            totalRow.createCell(10).setCellValue(sorted.sumOf { it.totalWage(settings) })
        }
    }

    private fun autoSizeColumns(sheet: Sheet, numCols: Int) {
        for (i in 0 until numCols) {
            try {
                sheet.autoSizeColumn(i)
            } catch (e: Exception) {
                // 某些内容含换行符,autoSize 会失败,忽略
            }
        }
    }
}
