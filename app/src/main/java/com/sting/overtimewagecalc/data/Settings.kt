package com.sting.overtimewagecalc.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 用户设置(v1.1)
 *
 * - defaultDailyRate:默认日薪(每天的基础工资)
 * - defaultHourlyRate:默认加班时薪
 * - weekendMultiplier:周末(周六/日)默认倍数
 * - holidayMultiplier:节假日默认倍数(用户在设置里维护节假日日期)
 * - holidayDates:用户手动维护的节假日日期(yyyy-MM-dd)
 */
data class Settings(
    val defaultDailyRate: Double = 400.0,
    val defaultHourlyRate: Double = 50.0,
    val weekendMultiplier: Double = 1.5,
    val holidayMultiplier: Double = 3.0,
    val holidayDates: Set<String> = emptySet()
) {
    companion object {
        /** 给定日期计算应填的默认倍数 */
        fun defaultMultiplierFor(
            date: LocalDate,
            holidayMultiplier: Double,
            weekendMultiplier: Double,
            holidayDates: Set<String>
        ): Double = when {
            holidayDates.contains(date.toString()) -> holidayMultiplier
            date.dayOfWeek == DayOfWeek.SATURDAY ||
            date.dayOfWeek == DayOfWeek.SUNDAY -> weekendMultiplier
            else -> 1.0
        }

        /** 给定日期的类型标签(用于 UI 提示) */
        fun dateTypeLabel(date: LocalDate, holidayDates: Set<String>): String = when {
            holidayDates.contains(date.toString()) -> "节假日"
            date.dayOfWeek == DayOfWeek.SATURDAY -> "周六"
            date.dayOfWeek == DayOfWeek.SUNDAY -> "周日"
            else -> ""
        }
    }
}