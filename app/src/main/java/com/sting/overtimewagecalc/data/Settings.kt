package com.sting.overtimewagecalc.data

/**
 * 用户设置(持久化到 DataStore)
 *
 * - defaultHourlyRate:默认时薪(时薪模式自动填)
 * - defaultDailyRate:默认日薪(日薪模式自动填)
 */
data class Settings(
    val defaultHourlyRate: Double = 50.0,
    val defaultDailyRate: Double = 400.0
)