package com.sting.overtimewagecalc.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 用户设置(v1.2 — 删除节假日管理)
 *
 * - defaultDailyRate:默认日薪(留空时用)
 * - defaultHourlyRate:默认加班时薪(留空时用)
 * - weekendMultiplier:周末(周六/日)默认倍数
 */
data class Settings(
    val defaultDailyRate: Double = 400.0,
    val defaultHourlyRate: Double = 50.0,
    val weekendMultiplier: Double = 1.5
) {
    companion object {
        /** 给定日期计算应填的默认倍数 */
        fun defaultMultiplierFor(date: LocalDate, weekendMultiplier: Double): Double =
            if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                weekendMultiplier
            } else {
                1.0
            }

        /** 给定日期的类型标签(用于 UI 提示) */
        fun dateTypeLabel(date: LocalDate): String = when {
            date.dayOfWeek == DayOfWeek.SATURDAY -> "周六"
            date.dayOfWeek == DayOfWeek.SUNDAY -> "周日"
            else -> ""
        }
    }
}