package com.sting.overtimewagecalc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings
import com.sting.overtimewagecalc.data.Storage
import com.sting.overtimewagecalc.data.WageCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

/** UI 状态(v1.8.2 — 加 dirtyDates 跟踪哪些日期有未保存的草稿) */
data class WageUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val entries: List<DayEntry> = emptyList(),
    val settings: Settings = Settings(),
    val dirtyDates: Set<LocalDate> = emptySet()
) {
    val monthEntries: List<DayEntry>
        get() = WageCalculator.entriesForMonth(entries, yearMonth)

    val entryByDate: Map<LocalDate, DayEntry>
        get() = WageCalculator.indexByDate(entries)

    val monthTotal: Double
        get() = WageCalculator.totalForMonth(monthEntries, settings)
}

/** ViewModel(v1.8 — 持久化到 SharedPreferences,升级不丢数据) */
class WageViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _state = MutableStateFlow(WageUiState())
    val state: StateFlow<WageUiState> = _state.asStateFlow()

    init {
        // 从磁盘恢复(卸载重装没数据,这里 load 返回 null 用默认)
        Storage.load(context)?.let { loaded ->
            _state.update {
                it.copy(entries = loaded.entries, settings = loaded.settings)
            }
        }
    }

    /** 每次改 entries / settings 都写盘 */
    private fun persist() {
        val current = _state.value
        Storage.save(context, current.entries, current.settings)
    }

    fun prevMonth() {
        _state.update { it.copy(yearMonth = it.yearMonth.minusMonths(1)) }
        // 月份切换是 UI 状态,不入 entries/settings,不存
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
            current.copy(entries = newEntries, dirtyDates = current.dirtyDates - updated.date)
        }
        persist()
    }

    /** 清空某天的数据(从 entries 里删掉这天的条目) */
    fun clearEntry(date: LocalDate) {
        _state.update { current ->
            val newEntries = current.entries.filter { it.date != date }
            current.copy(entries = newEntries, dirtyDates = current.dirtyDates - date)
        }
        persist()
    }

    /** v1.8.2:标记某天有未保存的草稿(DayEntryEditor 改任何字段时调用) */
    fun markDirty(date: LocalDate) {
        _state.update { current ->
            if (date in current.dirtyDates) current
            else current.copy(dirtyDates = current.dirtyDates + date)
        }
    }

    /** v1.8.2:清除某天的草稿标记(切换到没改动的日期时清理) */
    fun markClean(date: LocalDate) {
        _state.update { current ->
            if (date !in current.dirtyDates) current
            else current.copy(dirtyDates = current.dirtyDates - date)
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
        persist()
    }
}
