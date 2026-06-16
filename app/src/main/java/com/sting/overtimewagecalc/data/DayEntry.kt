package com.sting.overtimewagecalc.data

import java.time.LocalDate

/**
 * 某一天的工资条目(v2.0 — 备注改名:extraNote → dayNote)
 *
 * 每天工资 = 日薪(若启用) + (时薪 × 倍数 × 加班小时) + 额外加班金额
 *
 * - dailyWageEnabled:false → 日薪 = 0(sting 用户控制,默认 false)
 *                       true + dailyRate > 0 → 用 dailyRate
 *                       true + dailyRate == 0 → 用 settings.defaultDailyRate
 * - hourlyRate = 0:用 settings.defaultHourlyRate
 * - overtimeMultiplier:倍数,默认 1.0;周末自动填 weekendMultiplier,工作日为 1.0
 * - extraOvertime:直接金额的额外加班
 * - dayNote:备注(对当天所有工资的说明,不只限加班)— v2.0 改名
 */
data class DayEntry(
    val date: LocalDate,
    val dailyWageEnabled: Boolean = false,
    val dailyRate: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val overtimeMultiplier: Double = 1.0,
    val overtimeHours: Double = 0.0,
    val extraOvertime: Double = 0.0,
    val dayNote: String = ""   // v2.0 改:extraNote → dayNote
) {
    /** 是否"空"记录(用户没真正输入过任何东西) */
    val isEmpty: Boolean
        get() = !dailyWageEnabled && overtimeHours == 0.0 &&
                extraOvertime == 0.0 && dayNote.isBlank()

    /** 该天的总工资 */
    fun totalWage(settings: Settings): Double {
        val daily = when {
            !dailyWageEnabled -> 0.0
            dailyRate > 0 -> dailyRate
            else -> settings.defaultDailyRate
        }
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
