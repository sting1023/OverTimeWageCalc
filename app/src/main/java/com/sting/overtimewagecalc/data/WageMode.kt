package com.sting.overtimewagecalc.data

/** 工资计算模式 */
enum class WageMode {
    HOURLY,  // 时薪模式:时薪 × 工作小时数
    DAILY    // 日薪模式:固定日薪
}