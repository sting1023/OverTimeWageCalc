package com.sting.overtimewagecalc.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sting.overtimewagecalc.WageDraft
import com.sting.overtimewagecalc.WageUiState
import com.sting.overtimewagecalc.computeFieldChanges
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.RecentInputs
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * 主屏幕(v2.0)
 *  - 草稿 map 状态提升:HomeScreen 持有 draftsByDate,DayEntryEditor 接 WageDraft + onDraftChange
 *  - 退出未保存保护:BackHandler + 三按钮 AlertDialog
 *  - 拦截日期/月份切换 + 设置
 *  - 导出按钮:触发 ExcelExporter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: WageUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onCreateEntry: (LocalDate) -> DayEntry,
    onUpdateEntry: (DayEntry) -> Unit,
    onClearEntry: (LocalDate) -> Unit,
    onMarkDirty: (LocalDate) -> Unit,
    onMarkManyClean: (Set<LocalDate>) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    // v2.0 关键:草稿 map 状态提升到 HomeScreen(避开 v1.8.3 数据丢失 bug)
    var draftsByDate by remember { mutableStateOf<Map<LocalDate, WageDraft>>(emptyMap()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var exitAction by remember { mutableStateOf<ExitAction?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    // 给当前选中日期同步草稿
    val selectedDraft: WageDraft? = state.selectedDate?.let { date ->
        draftsByDate[date] ?: run {
            val entry = state.entryByDate[date]
            val draft = if (entry != null) {
                WageDraft.fromEntry(entry, state.settings)
            } else {
                WageDraft.empty(date, state.settings)
            }
            // 首次访问时初始化(写回 map)
            draftsByDate = draftsByDate + (date to draft)
            draft
        }
    }

    // 拦截所有"会离开当前编辑页"的操作
    fun guardNavigation(action: ExitAction, onProceed: () -> Unit) {
        if (state.dirtyDates.isEmpty()) {
            onProceed()
        } else {
            exitAction = action
            showExitDialog = true
        }
    }

    // 系统返回键
    BackHandler(enabled = state.dirtyDates.isNotEmpty()) {
        showExitDialog = true
        exitAction = ExitAction.Back
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "工资计算器",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "本月工资 ¥ %.2f".format(state.monthTotal),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // v2.0:导出按钮(在设置左边)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                exportStatus = ExcelExporter.exportCurrentMonth(
                                    context = context,
                                    yearMonth = state.yearMonth,
                                    entries = state.monthEntries,
                                    settings = state.settings
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "导出 Excel",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "导出",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // 设置按钮
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                guardNavigation(ExitAction.OpenSettings, onOpenSettings)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "设置",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 日历(固定)
            CalendarSection(
                yearMonth = state.yearMonth,
                selectedDate = state.selectedDate,
                entryByDate = state.entryByDate,
                settings = state.settings,
                dirtyDates = state.dirtyDates,
                onPrevMonth = { guardNavigation(ExitAction.PrevMonth) { onPrevMonth() } },
                onNextMonth = { guardNavigation(ExitAction.NextMonth) { onNextMonth() } },
                onSelectDate = { newDate ->
                    guardNavigation(ExitAction.SelectDate(newDate)) { onSelectDate(newDate) }
                }
            )

            // 选中日期的编辑器 + 本月明细
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                state.selectedDate?.let { date ->
                    val draft = draftsByDate[date]
                    if (draft != null) {
                        item(key = "editor_${date}") {
                            DayEntryEditor(
                                entryDate = date,
                                settings = state.settings,
                                draft = draft,
                                onDraftChange = { newDraft ->
                                    draftsByDate = draftsByDate + (date to newDraft)
                                },
                                onMarkDirty = onMarkDirty,
                                onCommit = { d ->
                                    val entry = d.toEntry(state.settings)
                                    onUpdateEntry(entry)
                                    // 写最近历史(在 commit 时,而不是选历史时)
                                    if (d.extraText.isNotBlank()) {
                                        RecentInputs.addExtraOvertime(context, d.extraText)
                                    }
                                    if (d.noteText.isNotBlank()) {
                                        RecentInputs.addDayNote(context, d.noteText)
                                    }
                                },
                                onClear = onClearEntry
                            )
                        }
                    }
                }

                item(key = "details") {
                    Spacer(Modifier.height(8.dp))
                    MonthDetailsSection(
                        entries = state.monthEntries,
                        settings = state.settings
                    )
                }
            }
        }
    }

    // 退出未保存保护(三按钮弹框)
    if (showExitDialog) {
        val dirtyCount = state.dirtyDates.size
        val dirtyChanges = state.dirtyDates.sorted().flatMap { date ->
            val draft = draftsByDate[date] ?: return@flatMap emptyList()
            val committed = state.entryByDate[date]
            computeFieldChanges(committed, draft, state.settings)
                .map { date to it }
        }

        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("⚠️ 有 $dirtyCount 天未保存") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "以下日期的数据被改动过,还没点保存:",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    state.dirtyDates.sorted().forEach { d ->
                        Text(
                            "· $d",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (dirtyChanges.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "改动内容:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        dirtyChanges.forEach { (date, change) ->
                            Text(
                                "$date · ${change.displayText}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                // 保存:把所有 dirty 天的草稿真正写入
                TextButton(onClick = {
                    state.dirtyDates.forEach { date ->
                        draftsByDate[date]?.let { draft ->
                            onUpdateEntry(draft.toEntry(state.settings))
                            // 写历史
                            if (draft.extraText.isNotBlank()) {
                                RecentInputs.addExtraOvertime(context, draft.extraText)
                            }
                            if (draft.noteText.isNotBlank()) {
                                RecentInputs.addDayNote(context, draft.noteText)
                            }
                        }
                    }
                    onMarkManyClean(state.dirtyDates)
                    showExitDialog = false
                    // 执行原动作
                    executeExitAction(exitAction, onPrevMonth, onNextMonth, onSelectDate, onOpenSettings)
                }) {
                    Text("保存", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 放弃修改
                    TextButton(onClick = {
                        // 从 draftsByDate 删除 dirty 的天,下次进入会从 entry 重新初始化
                        draftsByDate = draftsByDate - state.dirtyDates
                        onMarkManyClean(state.dirtyDates)
                        showExitDialog = false
                        executeExitAction(exitAction, onPrevMonth, onNextMonth, onSelectDate, onOpenSettings)
                    }) {
                        Text("放弃修改", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(4.dp))
                    // 继续编辑
                    TextButton(onClick = {
                        showExitDialog = false
                        exitAction = null
                    }) {
                        Text("继续编辑")
                    }
                }
            }
        )
    }

    // 导出结果 toast(用 Snackbar 比较重,简化为字符串提示)
    exportStatus?.let { msg ->
        AlertDialog(
            onDismissRequest = { exportStatus = null },
            title = { Text("导出完成") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { exportStatus = null }) { Text("好") }
            }
        )
    }
}

/** 执行用户的原始动作(放弃/保存后) */
private fun executeExitAction(
    action: ExitAction?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit
) {
    when (action) {
        is ExitAction.PrevMonth -> onPrevMonth()
        is ExitAction.NextMonth -> onNextMonth()
        is ExitAction.SelectDate -> onSelectDate(action.date)
        is ExitAction.OpenSettings -> onOpenSettings()
        is ExitAction.Back -> { /* nothing, user is leaving */ }
        null -> {}
    }
}

private sealed class ExitAction {
    object Back : ExitAction()
    object PrevMonth : ExitAction()
    object NextMonth : ExitAction()
    data class SelectDate(val date: LocalDate) : ExitAction()
    object OpenSettings : ExitAction()
}
