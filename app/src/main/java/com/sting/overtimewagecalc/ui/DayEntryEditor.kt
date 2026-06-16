package com.sting.overtimewagecalc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sting.overtimewagecalc.WageDraft
import com.sting.overtimewagecalc.data.RecentInputs
import com.sting.overtimewagecalc.data.Settings
import java.time.LocalDate

/**
 * 每天输入编辑器(v2.0 重做)
 *  - 草稿模式:不维护 local var,所有字段通过 WageDraft + onDraftChange 提给父级
 *  - 这是 v1.8.3 失败的根因修复 —— 草稿在父级 HomeScreen,弹框能真存
 *  - 历史选择:v1.7 的内嵌 DropdownMenu 删了,改成"点输入框弹底部 ModalBottomSheet"
 *  - 手动填 / 选历史 = 行为完全一致(都只改 draft + markDirty,不 commit)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEntryEditor(
    entryDate: LocalDate,
    settings: Settings,
    draft: WageDraft,
    onDraftChange: (WageDraft) -> Unit,
    onMarkDirty: (LocalDate) -> Unit,
    onCommit: (WageDraft) -> Unit,
    onClear: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    // v2.0:历史数据(只在初始化时读,后续变动由 commit 写入)
    val extraHistory = remember { RecentInputs.getExtraOvertime(context) }
    val noteHistory = remember { RecentInputs.getDayNote(context) }

    // 弹窗控制
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var historySheet by remember { mutableStateOf<HistoryField?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 1. 启用日薪(checkbox)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = draft.dailyEnabled,
                    onCheckedChange = {
                        onDraftChange(draft.copy(dailyEnabled = it))
                        onMarkDirty(entryDate)
                    }
                )
                Text(
                    "启用日薪",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        onDraftChange(draft.copy(dailyEnabled = !draft.dailyEnabled))
                        onMarkDirty(entryDate)
                    }
                )
            }
            if (draft.dailyEnabled) {
                NumberField(
                    label = "日薪(¥/天)",
                    value = draft.dailyText,
                    onValueChange = {
                        onDraftChange(draft.copy(dailyText = it))
                        onMarkDirty(entryDate)
                    }
                )
            } else {
                Text(
                    "未启用日薪(今天只有加班 / 额外加班工资)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 8.dp)
                )
            }

            // 2. 加班时薪
            NumberField(
                label = "加班时薪(¥/小时)",
                value = draft.hourlyText,
                onValueChange = {
                    onDraftChange(draft.copy(hourlyText = it))
                    onMarkDirty(entryDate)
                }
            )

            // 3. 倍数 + 小时(并排)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "加班倍数",
                        value = draft.multiplierText,
                        onValueChange = {
                            onDraftChange(draft.copy(multiplierText = it))
                            onMarkDirty(entryDate)
                        }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "加班小时数",
                        value = draft.hoursText,
                        onValueChange = {
                            onDraftChange(draft.copy(hoursText = it))
                            onMarkDirty(entryDate)
                        }
                    )
                }
            }
            Text(
                "周末 ${settings.weekendMultiplier} 倍(可在设置改)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Spacer(Modifier.height(4.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // 4. 额外加班(点输入框/图标弹历史抽屉)
            NumberField(
                label = "额外加班(¥,可选)",
                value = draft.extraText,
                onValueChange = {
                    onDraftChange(draft.copy(extraText = it))
                    onMarkDirty(entryDate)
                },
                onClickIfHistory = if (extraHistory.isNotEmpty()) {
                    { historySheet = HistoryField.ExtraOvertime }
                } else null
            )
            if (extraHistory.isNotEmpty()) {
                Text(
                    "点击输入框可快速选择最近 3 条金额",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp)
                )
            }
            Spacer(Modifier.height(4.dp))

            // 5. 备注(点输入框/图标弹历史抽屉)
            OutlinedTextField(
                value = draft.noteText,
                onValueChange = {
                    onDraftChange(draft.copy(noteText = it))
                    onMarkDirty(entryDate)
                },
                label = { Text("备注(对当天工资的说明)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (noteHistory.isNotEmpty()) {
                            Modifier.clickable { historySheet = HistoryField.DayNote }
                        } else Modifier
                    ),
                singleLine = true,
                trailingIcon = if (noteHistory.isNotEmpty()) {
                    {
                        IconButton(onClick = { historySheet = HistoryField.DayNote }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "点输入框或这里选历史",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else null
            )

            Spacer(Modifier.height(12.dp))

            // 当天小计(实时,基于草稿)
            val preview = draft.previewTotal(settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("当天工资小计", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(
                    "¥ %.2f".format(preview),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // 清空 + 保存 按钮
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清空")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onCommit(draft) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }
        }
    }

    // 清空确认弹框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空当天数据") },
            text = { Text("确定要清空 $entryDate 的所有工资数据吗?此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClear(entryDate)
                }) {
                    Text("确定清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // v2.0:历史选择底部抽屉
    if (historySheet != null) {
        val field = historySheet!!
        val items = when (field) {
            HistoryField.ExtraOvertime -> extraHistory
            HistoryField.DayNote -> noteHistory
        }
        ModalBottomSheet(
            onDismissRequest = { historySheet = null }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when (field) {
                        HistoryField.ExtraOvertime -> "选择历史额外加班金额"
                        HistoryField.DayNote -> "选择历史备注"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
                if (items.isEmpty()) {
                    Text(
                        "暂无历史记录",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    items.forEach { value ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    when (field) {
                                        HistoryField.ExtraOvertime -> "¥ $value"
                                        HistoryField.DayNote -> value
                                    },
                                    fontSize = 15.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // v2.0 关键修复:只更新草稿,不自动 commit
                                    val newDraft = when (field) {
                                        HistoryField.ExtraOvertime -> draft.copy(extraText = value)
                                        HistoryField.DayNote -> draft.copy(noteText = value)
                                    }
                                    onDraftChange(newDraft)
                                    onMarkDirty(entryDate)
                                    historySheet = null
                                }
                        )
                        Divider()
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** 历史抽屉的字段标识 */
private enum class HistoryField { ExtraOvertime, DayNote }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberField(
    label: String,
    value: String,
    placeholder: String? = null,
    onValueChange: (String) -> Unit,
    onClickIfHistory: (() -> Unit)? = null,   // v2.0:有历史时整输入框可点
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClickIfHistory != null) Modifier.clickable(onClick = onClickIfHistory) else Modifier),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        trailingIcon = trailingIcon
    )
}
