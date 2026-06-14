package com.sting.overtimewagecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings
import com.sting.overtimewagecalc.data.WageCalculator
import com.sting.overtimewagecalc.data.WageMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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
                    secondary = Color(0xFF388E3C),
                    background = Color(0xFFF5F5F5),
                    surface = Color.White
                )
            ) {
                AppRoot()
            }
        }
    }
}

/** 整个应用的根 Composable(在 SettingsScreen 和 HomeScreen 之间切换) */
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
            onUpdateEntry = viewModel::updateEntry,
            onOpenSettings = { showSettings = true }
        )
    }
}

/** 主屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: WageUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onUpdateEntry: (DayEntry) -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("加班工资计算器") },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 本月工资总额(大字)
            TotalWageCard(total = state.monthTotal)

            Spacer(Modifier.height(16.dp))

            // 日历
            CalendarSection(
                yearMonth = state.yearMonth,
                selectedDate = state.selectedDate,
                entryByDate = state.entryByDate,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onSelectDate = onSelectDate
            )

            Spacer(Modifier.height(16.dp))

            // 选中日期的输入区
            state.selectedDate?.let { date ->
                val entry = state.entryByDate[date] ?: DayEntry(date = date)
                DayEntryEditor(
                    entry = entry,
                    settings = state.settings,
                    onUpdate = onUpdateEntry
                )
            }

            Spacer(Modifier.height(16.dp))

            // 本月所有明细
            MonthDetailsSection(
                entries = state.monthEntries,
                settings = state.settings
            )
        }
    }
}

@Composable
fun TotalWageCard(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "本月工资总额",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "¥ %.2f".format(total),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 月份切换行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "上月")
                }
                Text(
                    text = "${yearMonth.year} 年 ${yearMonth.monthValue} 月",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "下月")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 星期表头
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

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
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayNum)
                                val isSelected = date == selectedDate
                                val hasEntry = entryByDate[date]?.isEmpty == false

                                CalendarDayCell(
                                    day = dayNum,
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
fun CalendarDayCell(day: Int, isSelected: Boolean, hasEntry: Boolean, onClick: () -> Unit) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasEntry -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
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
            Text(
                day.toString(),
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun DayEntryEditor(
    entry: DayEntry,
    settings: Settings,
    onUpdate: (DayEntry) -> Unit
) {
    var mode by rememberSaveable(entry.date) { mutableStateOf(entry.mode) }
    var hourlyRateText by rememberSaveable(entry.date) {
        mutableStateOf(if (entry.hourlyRate > 0) entry.hourlyRate.toString() else settings.defaultHourlyRate.toString())
    }
    var hoursText by rememberSaveable(entry.date) {
        mutableStateOf(if (entry.hours > 0) entry.hours.toString() else "")
    }
    var dailyRateText by rememberSaveable(entry.date) {
        mutableStateOf(if (entry.dailyRate > 0) entry.dailyRate.toString() else settings.defaultDailyRate.toString())
    }
    var extraText by rememberSaveable(entry.date) {
        mutableStateOf(if (entry.extraOvertime > 0) entry.extraOvertime.toString() else "")
    }
    var noteText by rememberSaveable(entry.date) {
        mutableStateOf(entry.extraNote)
    }

    fun commit() {
        val updated = entry.copy(
            mode = mode,
            hourlyRate = hourlyRateText.toDoubleOrNull() ?: 0.0,
            hours = hoursText.toDoubleOrNull() ?: 0.0,
            dailyRate = dailyRateText.toDoubleOrNull() ?: 0.0,
            extraOvertime = extraText.toDoubleOrNull() ?: 0.0,
            extraNote = noteText
        )
        onUpdate(updated)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${entry.date} 的工资",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))

            // 模式切换
            SegmentedButton(
                mode = mode,
                onModeChange = {
                    mode = it
                    commit()
                }
            )

            Spacer(Modifier.height(12.dp))

            // 输入区
            when (mode) {
                WageMode.HOURLY -> {
                    NumberField(
                        label = "时薪(¥/小时)",
                        value = hourlyRateText,
                        onValueChange = {
                            hourlyRateText = it
                            commit()
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    NumberField(
                        label = "工作小时数",
                        value = hoursText,
                        onValueChange = {
                            hoursText = it
                            commit()
                        }
                    )
                }
                WageMode.DAILY -> {
                    NumberField(
                        label = "日薪(¥/天)",
                        value = dailyRateText,
                        onValueChange = {
                            dailyRateText = it
                            commit()
                        }
                    )
                    Text(
                        "本月其他天默认日薪: ¥ ${"%.2f".format(settings.defaultDailyRate)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            // 额外加班
            NumberField(
                label = "额外加班(¥)",
                value = extraText,
                onValueChange = {
                    extraText = it
                    commit()
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    commit()
                },
                label = { Text("加班备注(可选)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButton(mode: WageMode, onModeChange: (WageMode) -> Unit) {
    val options = listOf("时薪" to WageMode.HOURLY, "日薪" to WageMode.DAILY)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, value) ->
            SegmentedButton(
                selected = mode == value,
                onClick = { onModeChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.date.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            val modeLabel = if (entry.mode == WageMode.HOURLY)
                "时薪 ¥${"%.2f".format(entry.hourlyRate)} × ${entry.hours} 小时"
            else
                "日薪 ¥${"%.2f".format(entry.dailyRate)}"
            Text(
                modeLabel,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (entry.extraOvertime > 0 || entry.extraNote.isNotBlank()) {
                Text(
                    "加班 +¥${"%.2f".format(entry.extraOvertime)}${if (entry.extraNote.isNotBlank()) " · ${entry.extraNote}" else ""}",
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

/** 设置屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onBack: () -> Unit
) {
    var hourlyText by rememberSaveable { mutableStateOf(settings.defaultHourlyRate.toString()) }
    var dailyText by rememberSaveable { mutableStateOf(settings.defaultDailyRate.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        onSettingsChange(
                            Settings(
                                defaultHourlyRate = hourlyText.toDoubleOrNull() ?: settings.defaultHourlyRate,
                                defaultDailyRate = dailyText.toDoubleOrNull() ?: settings.defaultDailyRate
                            )
                        )
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "默认值设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "如果没有特殊情况,这些值会自动填到时薪/日薪模式里。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            NumberField(
                label = "默认时薪(¥/小时)",
                value = hourlyText,
                onValueChange = { hourlyText = it }
            )
            Spacer(Modifier.height(12.dp))
            NumberField(
                label = "默认日薪(¥/天)",
                value = dailyText,
                onValueChange = { dailyText = it }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSettingsChange(
                        Settings(
                            defaultHourlyRate = hourlyText.toDoubleOrNull() ?: settings.defaultHourlyRate,
                            defaultDailyRate = dailyText.toDoubleOrNull() ?: settings.defaultDailyRate
                        )
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}