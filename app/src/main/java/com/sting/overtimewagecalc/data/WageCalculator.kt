package com.sting.overtimewagecalc.data

import java.time.LocalDate
import java.time.YearMonth

/** 工资计算工具(纯函数) */
object WageCalculator {

    /** 本月所有天数(从 1 号到最后一天) */
    fun daysInMonth(yearMonth: YearMonth): List<LocalDate> {
        val first = yearMonth.atDay(1)
        val last = yearMonth.atEndOfMonth()
        return (1..yearMonth.lengthOfMonth()).map { first.withDayOfMonth(it) }
    }

    /** 给定一个月份的明细,算出本月总工资 */
    fun totalForMonth(entries: List<DayEntry>, settings: Settings): Double =
        entries.sumOf { it.totalWage(settings) }

    /** 过滤出某个月份的条目 */
    fun entriesForMonth(entries: List<DayEntry>, yearMonth: YearMonth): List<DayEntry> =
        entries.filter { it.date.month == yearMonth.month && it.date.year == yearMonth.year }

    /** 按日期索引(便于查找) */
    fun indexByDate(entries: List<DayEntry>): Map<LocalDate, DayEntry> =
        entries.associateBy { it.date }
}