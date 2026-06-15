package com.sting.overtimewagecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.RecentInputs
import com.sting.overtimewagecalc.data.Settings
import com.sting.overtimewagecalc.data.WageCalculator
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1976D2),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFBBDEFB),
                    onPrimaryContainer = Color(0xFF0D47A1),
                    secondary = Color(0xFFE65100),
                    background = Color(0xFFF5F5F5),
                    surface = Color.White
                )
            ) {
                AppRoot()
            }
        }
    }
}

/** 应用根 */
@Composable
fun AppRoot() {
    val viewModel: WageViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            settings = state.settings,
            onSettingsChange = viewModel::updateSettings,
            onBack = { showSettings = false }
        )
    } else {
        HomeScreen(
            state = state,
            onPrevMonth = viewModel::prevMonth,
            onNextMonth = viewModel::nextMonth,
            onSelectDate = viewModel::selectDate,
            onCreateEntry = viewModel::createEntryFor,
            onUpdateEntry = viewModel::updateEntry,
            onClearEntry = viewModel::clearEntry,
            onOpenSettings = { showSettings = true }
        )
    }
}

/** 主屏幕(v1.4:工资总额嵌进标题 + 日历固定 + 内容可滑动)
 *  v1.6:加 onClearEntry 清空当天数据 */
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
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // v1.7:一行显示,工资计算器 + 本月工资
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
                // v1.7:设置按钮移到右边
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
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
            // 日历(固定,不滚动)
            CalendarSection(
                yearMonth = state.yearMonth,
                selectedDate = state.selectedDate,
                entryByDate = state.entryByDate,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onSelectDate = onSelectDate
            )

            // 下面内容:编辑器 + 本月明细,可滑动
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // 选中日期的输入区
                state.selectedDate?.let { date ->
                    val entry = state.entryByDate[date] ?: onCreateEntry(date)
                    item(key = "editor_${date}") {
                        DayEntryEditor(
                            entry = entry,
                            settings = state.settings,
                            onUpdate = onUpdateEntry,
                            onClear = onClearEntry
                        )
                    }
                }

                // 本月明细
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
}

@Composable
fun CalendarSection(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    entryByDate: Map<LocalDate, DayEntry>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            // 月份切换(v1.7:整行 +30%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "上月", modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "${yearMonth.year} 年 ${yearMonth.monthValue} 月",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "下月", modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(2.dp))

            // 星期表头
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(1.dp))

            // 日历网格
            val firstDay = yearMonth.atDay(1)
            val firstDayOfWeek = firstDay.dayOfWeek.value % 7  // 周日=0
            val daysInMonth = yearMonth.lengthOfMonth()
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - firstDayOfWeek + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayNum)
                                val isSelected = date == selectedDate
                                val hasEntry = entryByDate[date]?.isEmpty == false

                                CalendarDayCell(
                                    day = dayNum,
                                    date = date,
                                    isSelected = isSelected,
                                    hasEntry = hasEntry,
                                    onClick = { onSelectDate(date) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String, isDot: Boolean = false, size: androidx.compose.ui.unit.Dp = 12.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    date: LocalDate,
    isSelected: Boolean,
    hasEntry: Boolean,
    onClick: () -> Unit
) {
    val isWeekend = date.dayOfWeek.value == 6 || date.dayOfWeek.value == 7
    // v1.7:今天用绿色标注
    val isToday = date == LocalDate.now()

    val bgColor = when {
        isSelected -> Color(0xFF006064)  // 深青
        isWeekend -> Color(0xFF90CAF9)   // 加深的蓝
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> Color(0xFF2E7D32)     // 今天的数字用绿色
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(50)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    day.toString(),
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                // 有数据:在日期下面加深紫色小点
                if (hasEntry) {
                    Spacer(Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) Color.White
                                else Color(0xFF4A148C)  // 深紫
                            )
                    )
                }
            }
        }
    }
}

/** 每天输入编辑器(v1.6:加 保存/清空 按钮,改用草稿模式)
 *  v1.7:额外加班/备注 加最近 3 条历史选择 */
@Composable
fun DayEntryEditor(
    entry: DayEntry,
    settings: Settings,
    onUpdate: (DayEntry) -> Unit,
    onClear: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    // 清空/重新打开日期时,递增 resetNonce 强制所有本地状态重新初始化
    var resetNonce by rememberSaveable { mutableStateOf(0) }

    // v1.7:最近输入历史(自动跟随 resetNonce 重新读取,比如清空后)
    var extraHistory by remember(resetNonce) {
        mutableStateOf(RecentInputs.getExtraOvertime(context))
    }
    var noteHistory by remember(resetNonce) {
        mutableStateOf(RecentInputs.getExtraNote(context))
    }

    // v1.5:日薪/时薪默认值自动填入(用户可改,清空 = 用设置里的默认)
    var dailyEnabled by rememberSaveable(entry.date, resetNonce) {
        mutableStateOf(entry.dailyWageEnabled)
    }
    var dailyText by rememberSaveable(entry.date, resetNonce) {
        // 优先显示已填的 dailyRate,否则填入默认日薪
        mutableStateOf(
            if (entry.dailyRate > 0) entry.dailyRate.toString()
            else settings.defaultDailyRate.toString()
        )
    }
    var hourlyText by rememberSaveable(entry.date, resetNonce) {
        mutableStateOf(
            if (entry.hourlyRate > 0) entry.hourlyRate.toString()
            else settings.defaultHourlyRate.toString()
        )
    }
    var multiplierText by rememberSaveable(entry.date, resetNonce) {
        mutableStateOf(entry.overtimeMultiplier.toString())
    }
    var hoursText by rememberSaveable(entry.date, resetNonce) {
        mutableStateOf(if (entry.overtimeHours > 0) entry.overtimeHours.toString() else "")
    }
    var extraText by rememberSaveable(entry.date, resetNonce) {
        mutableStateOf(if (entry.extraOvertime > 0) entry.extraOvertime.toString() else "")
    }
    var noteText by rememberSaveable(entry.date, resetNonce) {
        mutableStateOf(entry.extraNote)
    }

    fun commit() {
        val updated = entry.copy(
            dailyWageEnabled = dailyEnabled,
            // 用户清空 = 用设置里的默认;有内容 = 用用户填的
            dailyRate = dailyText.toDoubleOrNull() ?: settings.defaultDailyRate,
            hourlyRate = hourlyText.toDoubleOrNull() ?: settings.defaultHourlyRate,
            overtimeMultiplier = multiplierText.toDoubleOrNull() ?: 1.0,
            overtimeHours = hoursText.toDoubleOrNull() ?: 0.0,
            extraOvertime = extraText.toDoubleOrNull() ?: 0.0,
            extraNote = noteText
        )
        onUpdate(updated)
        // v1.7:保存到最近输入历史
        if (extraText.isNotBlank()) {
            RecentInputs.addExtraOvertime(context, extraText)
            extraHistory = RecentInputs.getExtraOvertime(context)
        }
        if (noteText.isNotBlank()) {
            RecentInputs.addExtraNote(context, noteText)
            noteHistory = RecentInputs.getExtraNote(context)
        }
    }

    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    // v1.7:历史下拉菜单控制
    var showExtraMenu by rememberSaveable { mutableStateOf(false) }
    var showNoteMenu by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // v1.5:删日期标题行(日历已显示选中状态,这里多此一举)

            // 1. 日薪(checkbox 启用才显示输入框)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = dailyEnabled,
                    onCheckedChange = { dailyEnabled = it }
                )
                Text(
                    "启用日薪",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { dailyEnabled = !dailyEnabled }
                )
            }
            if (dailyEnabled) {
                NumberField(
                    label = "日薪(¥/天)",
                    value = dailyText,
                    onValueChange = { dailyText = it }
                )
            } else {
                Text(
                    "未启用日薪(今天只有加班 / 额外加班工资)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 8.dp)
                )
            }

            // 2. 加班时薪(默认已填,清空用设置默认)
            NumberField(
                label = "加班时薪(¥/小时)",
                value = hourlyText,
                onValueChange = { hourlyText = it }
            )

            // 3. 倍数 + 小时(并排)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "加班倍数",
                        value = multiplierText,
                        onValueChange = { multiplierText = it }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "加班小时数",
                        value = hoursText,
                        onValueChange = { hoursText = it }
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

            // 4. 额外加班(v1.7:带历史选择)
            Box(modifier = Modifier.fillMaxWidth()) {
                NumberField(
                    label = "额外加班(¥,可选)",
                    value = extraText,
                    onValueChange = { extraText = it },
                    trailingIcon = if (extraHistory.isNotEmpty()) {
                        {
                            IconButton(onClick = { showExtraMenu = true }) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "最近填写",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else null
                )
                DropdownMenu(
                    expanded = showExtraMenu,
                    onDismissRequest = { showExtraMenu = false }
                ) {
                    extraHistory.forEach { value ->
                        DropdownMenuItem(
                            text = { Text("¥ $value") },
                            onClick = {
                                extraText = value
                                showExtraMenu = false
                                commit()  // 自动保存 + 记录历史
                            }
                        )
                    }
                }
            }
            if (extraHistory.isNotEmpty()) {
                Text(
                    "点击右侧时钟图标可快速选择最近 3 条金额",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp)
                )
            }
            Spacer(Modifier.height(4.dp))

            // 5. 备注(v1.7:带历史选择)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("加班备注(可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = if (noteHistory.isNotEmpty()) {
                        {
                            IconButton(onClick = { showNoteMenu = true }) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "最近填写",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else null
                )
                DropdownMenu(
                    expanded = showNoteMenu,
                    onDismissRequest = { showNoteMenu = false }
                ) {
                    noteHistory.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(value) },
                            onClick = {
                                noteText = value
                                showNoteMenu = false
                                commit()  // 自动保存 + 记录历史
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 当天小计(实时,基于草稿;v1.8:脏了才显示红色"未保存")
            val liveDaily = if (dailyEnabled) (dailyText.toDoubleOrNull() ?: settings.defaultDailyRate) else 0.0
            val liveHourly = hourlyText.toDoubleOrNull() ?: settings.defaultHourlyRate
            val liveMult = multiplierText.toDoubleOrNull() ?: 1.0
            val liveHours = hoursText.toDoubleOrNull() ?: 0.0
            val liveExtra = extraText.toDoubleOrNull() ?: 0.0
            val preview = liveDaily + liveHourly * liveMult * liveHours + liveExtra

            // 比较草稿 vs 已保存,任何一个字段不同就算脏
            val isDirty = dailyEnabled != entry.dailyWageEnabled ||
                liveDaily != entry.dailyRate ||
                liveHourly != entry.hourlyRate ||
                liveMult != entry.overtimeMultiplier ||
                liveHours != entry.overtimeHours ||
                liveExtra != entry.extraOvertime ||
                noteText != entry.extraNote

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当天工资小计", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    if (isDirty) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "(未保存)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    "¥ %.2f".format(preview),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // v1.6:清空 + 保存 按钮
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清空")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { commit() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }
        }
    }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空当天数据") },
            text = { Text("确定要清空 ${entry.date} 的所有工资数据吗?此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClear(entry.date)
                    // 触发本地状态重置为默认值
                    resetNonce++
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberField(
    label: String,
    value: String,
    placeholder: String? = null,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        trailingIcon = trailingIcon
    )
}

@Composable
fun MonthDetailsSection(entries: List<DayEntry>, settings: Settings) {
    val nonEmpty = entries.filter { !it.isEmpty }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "本月明细(${nonEmpty.size} 天有数据)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            if (nonEmpty.isEmpty()) {
                Text(
                    "本月还没录入数据。点击日历上的日期开始。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                nonEmpty.forEach { entry ->
                    DetailRow(entry = entry, settings = settings)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("本月合计", fontWeight = FontWeight.Bold)
                    Text(
                        "¥ %.2f".format(nonEmpty.sumOf { it.totalWage(settings) }),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(entry: DayEntry, settings: Settings) {
    val dateLabel = Settings.dateTypeLabel(entry.date)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.date.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (dateLabel.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "·$dateLabel",
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0)
                    )
                }
            }
            val detail = buildString {
                if (entry.dailyWageEnabled) {
                    val daily = if (entry.dailyRate > 0) entry.dailyRate else settings.defaultDailyRate
                    append("日薪 ¥${"%.2f".format(daily)}")
                }
                if (entry.overtimeHours > 0) {
                    if (entry.dailyWageEnabled) append(" + ")
                    val hourly = if (entry.hourlyRate > 0) entry.hourlyRate else settings.defaultHourlyRate
                    append("加班 ¥${"%.2f".format(hourly)} × ${entry.overtimeMultiplier} × ${entry.overtimeHours}h")
                }
                if (length == 0) append("仅加班 / 额外加班")
            }
            Text(
                detail,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (entry.extraOvertime > 0 || entry.extraNote.isNotBlank()) {
                Text(
                    "额外 +¥${"%.2f".format(entry.extraOvertime)}${if (entry.extraNote.isNotBlank()) " · ${entry.extraNote}" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Text(
            "¥ %.2f".format(entry.totalWage(settings)),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** 设置屏幕(v1.2 — 删除节假日管理) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onBack: () -> Unit
) {
    var dailyText by rememberSaveable { mutableStateOf(settings.defaultDailyRate.toString()) }
    var hourlyText by rememberSaveable { mutableStateOf(settings.defaultHourlyRate.toString()) }
    var weekendText by rememberSaveable { mutableStateOf(settings.weekendMultiplier.toString()) }

    fun save() {
        onSettingsChange(
            settings.copy(
                defaultDailyRate = dailyText.toDoubleOrNull() ?: settings.defaultDailyRate,
                defaultHourlyRate = hourlyText.toDoubleOrNull() ?: settings.defaultHourlyRate,
                weekendMultiplier = weekendText.toDoubleOrNull() ?: settings.weekendMultiplier
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = {
                                save()
                                onBack()
                            })
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "返回",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "默认值(每天输入框留空时用)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "输入框默认空,留空即用这里设置的值;手动填了就用填的",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            NumberField(
                label = "默认日薪(¥/天)",
                value = dailyText,
                onValueChange = { dailyText = it }
            )
            Spacer(Modifier.height(8.dp))
            NumberField(
                label = "默认加班时薪(¥/小时)",
                value = hourlyText,
                onValueChange = { hourlyText = it }
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "周末加班倍数",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "周末(六/日)选日期时自动填这个倍数,用户在每天输入页也能改",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            NumberField(
                label = "周末倍数",
                value = weekendText,
                onValueChange = { weekendText = it }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    save()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存设置")
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}