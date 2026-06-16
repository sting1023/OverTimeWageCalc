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

/**
 * UI 状态(v2.0)
 *  - entries + settings: 已保存到磁盘的真实数据
 *  - dirtyDates: 哪些日期有未保存的草稿(由 ViewModel.markDirty/markClean 维护)
 */
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

/**
 * ViewModel(v2.0)
 *  - 持久化:Storage.save 在每次 updateEntry/clearEntry/updateSettings 后调
 *  - 草稿追踪:dirtyDates 显式标记,onValueChange 调 markDirty
 *  - 草稿合成:commitDraft 把外部 DraftFields + Settings 合成 DayEntry 后调 updateEntry
 */
class WageViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _state = MutableStateFlow(WageUiState())
    val state: StateFlow<WageUiState> = _state.asStateFlow()

    init {
        Storage.load(context)?.let { loaded ->
            _state.update {
                it.copy(entries = loaded.entries, settings = loaded.settings)
            }
        }
    }

    private fun persist() {
        val current = _state.value
        Storage.save(context, current.entries, current.settings)
    }

    fun prevMonth() {
        _state.update { it.copy(yearMonth = it.yearMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _state.update { it.copy(yearMonth = it.yearMonth.plusMonths(1)) }
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
    }

    /** 新建某天的空白条目(自动填默认倍数:周末=weekendMultiplier,工作日=1.0) */
    fun createEntryFor(date: LocalDate): DayEntry {
        val defaultMultiplier = Settings.defaultMultiplierFor(
            date, _state.value.settings.weekendMultiplier
        )
        return DayEntry(date = date, overtimeMultiplier = defaultMultiplier)
    }

    /** v2.0:核心 commit 函数 —— 接受 DayEntry 写入并清 dirty 标 */
    fun updateEntry(updated: DayEntry) {
        _state.update { current ->
            val newEntries = current.entries.filter { it.date != updated.date } + updated
            current.copy(entries = newEntries, dirtyDates = current.dirtyDates - updated.date)
        }
        persist()
    }

    /** 清空某天数据 */
    fun clearEntry(date: LocalDate) {
        _state.update { current ->
            val newEntries = current.entries.filter { it.date != date }
            current.copy(entries = newEntries, dirtyDates = current.dirtyDates - date)
        }
        persist()
    }

    /** v2.0:批量清 dirty(用于"放弃修改"和"全部保存"弹框流程) */
    fun markDirty(date: LocalDate) {
        _state.update { current ->
            if (date in current.dirtyDates) current
            else current.copy(dirtyDates = current.dirtyDates + date)
        }
    }

    fun markClean(date: LocalDate) {
        _state.update { current ->
            if (date !in current.dirtyDates) current
            else current.copy(dirtyDates = current.dirtyDates - date)
        }
    }

    /** v2.0:批量清多个 dirtyDates(用于退出未保存弹框) */
    fun markManyClean(dates: Set<LocalDate>) {
        _state.update { current ->
            current.copy(dirtyDates = current.dirtyDates - dates)
        }
    }

    /** 设置变更:已有条目如果倍数是按默认填的,跟着新设置更新 */
    fun updateSettings(newSettings: Settings) {
        _state.update { current ->
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
