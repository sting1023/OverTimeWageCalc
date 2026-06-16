package com.sting.overtimewagecalc

import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings
import java.time.LocalDate

/**
 * 草稿数据结构(v2.0)
 *  - 用户在 DayEntryEditor 里改的所有字段
 *  - 提到 HomeScreen 层统一管理(避免 v1.8.3 "数据丢失 bug")
 *  - 详见 working-app-design-patterns.md §13
 *
 * 字段值 = String(因为 OutlinedTextField 的 value 是 String,中途可能不合法)
 *  - 合成 DayEntry 时用 toDoubleOrNull() 解析
 *  - 解析失败 = 用 settings 默认值
 */
data class WageDraft(
    val date: LocalDate,
    val dailyEnabled: Boolean = false,
    val dailyText: String = "",
    val hourlyText: String = "",
    val multiplierText: String = "1.0",
    val hoursText: String = "",
    val extraText: String = "",
    val noteText: String = ""
) {
    /** 从已保存的 DayEntry 初始化草稿(无值时用空) */
    companion object {
        fun fromEntry(entry: DayEntry, settings: Settings): WageDraft {
            return WageDraft(
                date = entry.date,
                dailyEnabled = entry.dailyWageEnabled,
                // 日薪/时薪:用户填过用用户的,否则用设置默认
                dailyText = if (entry.dailyRate > 0) entry.dailyRate.toString()
                            else settings.defaultDailyRate.toString(),
                hourlyText = if (entry.hourlyRate > 0) entry.hourlyRate.toString()
                             else settings.defaultHourlyRate.toString(),
                multiplierText = entry.overtimeMultiplier.toString(),
                hoursText = if (entry.overtimeHours > 0) entry.overtimeHours.toString() else "",
                extraText = if (entry.extraOvertime > 0) entry.extraOvertime.toString() else "",
                noteText = entry.dayNote
            )
        }

        /** 空草稿(用于新建一个日期,没任何已保存数据) */
        fun empty(date: LocalDate, settings: Settings): WageDraft {
            return WageDraft(
                date = date,
                dailyEnabled = false,
                dailyText = settings.defaultDailyRate.toString(),
                hourlyText = settings.defaultHourlyRate.toString(),
                multiplierText = Settings.defaultMultiplierFor(
                    date, settings.weekendMultiplier
                ).toString(),
                hoursText = "",
                extraText = "",
                noteText = ""
            )
        }
    }

    /** 草稿 → DayEntry(用 settings 默认值兜底) */
    fun toEntry(settings: Settings): DayEntry {
        return DayEntry(
            date = date,
            dailyWageEnabled = dailyEnabled,
            dailyRate = dailyText.toDoubleOrNull() ?: settings.defaultDailyRate,
            hourlyRate = hourlyText.toDoubleOrNull() ?: settings.defaultHourlyRate,
            overtimeMultiplier = multiplierText.toDoubleOrNull() ?: 1.0,
            overtimeHours = hoursText.toDoubleOrNull() ?: 0.0,
            extraOvertime = extraText.toDoubleOrNull() ?: 0.0,
            dayNote = noteText
        )
    }

    /** 当天小计(实时预览,跟 commit 后的 totalWage 一致) */
    fun previewTotal(settings: Settings): Double {
        return toEntry(settings).totalWage(settings)
    }
}

/**
 * 字段变化项(用于退出未保存弹框的内容展示)
 *  - 列出某天从原值变成新值的字段
 */
data class FieldChange(
    val fieldName: String,
    val oldValue: String,
    val newValue: String
) {
    val displayText: String
        get() = "$fieldName: $oldValue → $newValue"
}

/**
 * 计算某天的字段变化(对比 committed entry 和 draft)
 *  - 只列出"有变化"的字段
 *  - 用户不关心的字段(如 dailyEnabled)略过(不展示 checkbox 变化)
 */
fun computeFieldChanges(
    committed: DayEntry?,
    draft: WageDraft,
    settings: Settings
): List<FieldChange> {
    val changes = mutableListOf<FieldChange>()
    val baseline = committed ?: DayEntry(date = draft.date)

    fun addIfChanged(name: String, old: String, new: String) {
        if (old != new) changes.add(FieldChange(name, old, new))
    }

    addIfChanged("日薪", baseline.dailyRate.toString(), draft.dailyText)
    addIfChanged("时薪", baseline.hourlyRate.toString(), draft.hourlyText)
    addIfChanged("加班倍数", baseline.overtimeMultiplier.toString(), draft.multiplierText)
    addIfChanged("加班小时", baseline.overtimeHours.toString(), draft.hoursText)
    addIfChanged("额外加班", baseline.extraOvertime.toString(), draft.extraText)
    addIfChanged("备注", baseline.dayNote, draft.noteText)
    if (baseline.dailyWageEnabled != draft.dailyEnabled) {
        val old = if (baseline.dailyWageEnabled) "启用" else "不启用"
        val new = if (draft.dailyEnabled) "启用" else "不启用"
        changes.add(FieldChange("启用日薪", old, new))
    }

    return changes
}
