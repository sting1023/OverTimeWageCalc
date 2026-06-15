package com.sting.overtimewagecalc

import androidx.lifecycle.ViewModel
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings
import com.sting.overtimewagecalc.data.WageCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

/** UI 状态 */
data class WageUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val entries: List<DayEntry> = emptyList(),
    val settings: Settings = Settings()
) {
    val monthEntries: List<DayEntry>
        get() = WageCalculator.entriesForMonth(entries, yearMonth)

    val entryByDate: Map<LocalDate, DayEntry>
        get() = WageCalculator.indexByDate(entries)

    val monthTotal: Double
        get() = WageCalculator.totalForMonth(monthEntries, settings)
}

/** ViewModel */
class WageViewModel : ViewModel() {

    private val _state = MutableStateFlow(WageUiState())
    val state: StateFlow<WageUiState> = _state.asStateFlow()

    fun prevMonth() {
        _state.update { it.copy(yearMonth = it.yearMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _state.update { it.copy(yearMonth = it.yearMonth.plusMonths(1)) }
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
    }

    /** 新建某天的条目,自动填默认倍数(周末/工作日) */
    fun createEntryFor(date: LocalDate): DayEntry {
        val defaultMultiplier = Settings.defaultMultiplierFor(
            date, _state.value.settings.weekendMultiplier
        )
        return DayEntry(date = date, overtimeMultiplier = defaultMultiplier)
    }

    fun updateEntry(updated: DayEntry) {
        _state.update { current ->
            val newEntries = current.entries.filter { it.date != updated.date } + updated
            current.copy(entries = newEntries)
        }
    }

    /** 清空某天的数据(从 entries 里删掉这天的条目) */
    fun clearEntry(date: LocalDate) {
        _state.update { current ->
            val newEntries = current.entries.filter { it.date != date }
            current.copy(entries = newEntries)
        }
    }

    fun updateSettings(newSettings: Settings) {
        _state.update { current ->
            // 已有条目,如果倍数是按默认填的,跟着新设置更新
            val updatedEntries = current.entries.map { entry ->
                val oldDefault = Settings.defaultMultiplierFor(
                    entry.date, current.settings.weekendMultiplier
                )
                val newDefault = Settings.defaultMultiplierFor(
                    entry.date, newSettings.weekendMultiplier
                )
                if (entry.overtimeMultiplier == oldDefault && oldDefault != newDefault) {
                    entry.copy(overtimeMultiplier = newDefault)
                } else {
                    entry
                }
            }
            current.copy(settings = newSettings, entries = updatedEntries)
        }
    }
}