package com.sting.overtimewagecalc.data

import java.time.LocalDate

/**
 * 某一天的工资条目(v1.1 — 合并时薪/日薪模式)
 *
 * 每天工资 = 日薪 + (时薪 × 倍数 × 加班小时) + 额外加班金额
 *
 * - dailyRate = 0:用 settings.defaultDailyRate
 * - hourlyRate = 0:用 settings.defaultHourlyRate
 * - overtimeMultiplier:倍数,默认 1.0;周末自动填 weekendMultiplier,工作日为 1.0
 * - extraOvertime:直接金额的额外加班(老功能保留)
 * - extraNote:加班备注
 */
data class DayEntry(
    val date: LocalDate,
    val dailyRate: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val overtimeMultiplier: Double = 1.0,
    val overtimeHours: Double = 0.0,
    val extraOvertime: Double = 0.0,
    val extraNote: String = ""
) {
    /** 是否"空"记录(没有任何输入) */
    val isEmpty: Boolean
        get() = dailyRate == 0.0 && hourlyRate == 0.0 &&
                overtimeHours == 0.0 && extraOvertime == 0.0 && extraNote.isBlank()

    /** 该天的总工资 */
    fun totalWage(settings: Settings): Double {
        val daily = if (dailyRate > 0) dailyRate else settings.defaultDailyRate
        val hourly = if (hourlyRate > 0) hourlyRate else settings.defaultHourlyRate
        val overtimePay = hourly * overtimeMultiplier * overtimeHours
        return daily + overtimePay + extraOvertime
    }

    /** 该天加班部分的工资(不含基础日薪) */
    fun overtimeWage(settings: Settings): Double {
        val hourly = if (hourlyRate > 0) hourlyRate else settings.defaultHourlyRate
        return hourly * overtimeMultiplier * overtimeHours + extraOvertime
    }
}