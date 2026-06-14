package com.sting.overtimewagecalc.data

import java.time.LocalDate

/**
 * 某一天的工资条目
 *
 * - mode = HOURLY:使用 hourlyRate × hours
 * - mode = DAILY:使用 dailyRate(忽略 hours)
 * - extraOvertime / extraNote:无论哪种模式都生效
 */
data class DayEntry(
    val date: LocalDate,
    val mode: WageMode = WageMode.DAILY,
    val hourlyRate: Double = 0.0,
    val hours: Double = 0.0,
    val dailyRate: Double = 0.0,
    val extraOvertime: Double = 0.0,
    val extraNote: String = ""
) {
    /** 是否"空"记录(没有任何输入) */
    val isEmpty: Boolean
        get() = hourlyRate == 0.0 && hours == 0.0 && dailyRate == 0.0 &&
                extraOvertime == 0.0 && extraNote.isBlank()

    /** 该天的基础工资(不含额外加班) */
    fun baseWage(settings: Settings): Double = when (mode) {
        WageMode.HOURLY -> (if (hourlyRate > 0) hourlyRate else settings.defaultHourlyRate) *
                           (if (hours > 0) hours else 0.0)
        WageMode.DAILY -> if (dailyRate > 0) dailyRate else settings.defaultDailyRate
    }

    /** 该天的总工资(基础 + 加班) */
    fun totalWage(settings: Settings): Double =
        baseWage(settings) + extraOvertime
}