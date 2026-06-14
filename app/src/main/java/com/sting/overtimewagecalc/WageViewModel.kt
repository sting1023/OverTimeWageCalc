package com.sting.overtimewagecalc

import androidx.lifecycle.ViewModel
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings
import com.sting.overtimewagecalc.data.WageCalculator
import com.sting.overtimewagecalc.data.WageMode
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

/** ViewModel:管理状态 + 处理用户操作 */
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

    fun updateEntry(updated: DayEntry) {
        _state.update { current ->
            val newEntries = current.entries.filter { it.date != updated.date } + updated
            current.copy(entries = newEntries)
        }
    }

    fun updateSettings(newSettings: Settings) {
        _state.update { it.copy(settings = newSettings) }
    }
}