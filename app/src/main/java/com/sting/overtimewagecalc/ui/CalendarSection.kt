package com.sting.overtimewagecalc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings
import java.time.LocalDate
import java.time.YearMonth

/**
 * 日历组件(v2.0)
 *  - v1.7 5 元素配色(今天绿/选中深青/周末浅蓝/有数据深紫点/默认)
 *  - v2.0 新增:有数据的天显示当日工资 ¥xxx(数字下面)
 *  - v1.8.2:有未保存的天显示红字"未保存"(覆盖金额)
 */
@Composable
fun CalendarSection(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    entryByDate: Map<LocalDate, DayEntry>,
    settings: Settings,                            // v2.0:用来算当日工资预览
    dirtyDates: Set<LocalDate>,
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
                                val entry = entryByDate[date]
                                val isSelected = date == selectedDate
                                val hasEntry = entry?.isEmpty == false
                                val isDirty = date in dirtyDates
                                // v2.0:有数据就显示当日工资
                                val wageText = if (entry != null) {
                                    "%.0f".format(entry.totalWage(settings))
                                } else ""

                                CalendarDayCell(
                                    day = dayNum,
                                    isSelected = isSelected,
                                    hasEntry = hasEntry,
                                    isDirty = isDirty,
                                    wageText = wageText,
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
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    hasEntry: Boolean,
    isDirty: Boolean,
    wageText: String,                                // v2.0 新增
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val date = today.withDayOfMonth(day)              // 简化(只用于 isToday 判断,实际日期由父级传)
    val isToday = today.dayOfMonth == day && today.month == today.month  // 同月同日
    val isWeekend = date.dayOfWeek.value == 6 || date.dayOfWeek.value == 7

    val bgColor = when {
        isSelected -> WageColors.SelectedBg
        isWeekend -> WageColors.WeekendBg
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> WageColors.SelectedText
        isToday -> WageColors.TodayText
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
                // 优先级:未保存(红字) > 有数据金额 > 空
                when {
                    isDirty -> {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            "未保存",
                            color = if (isSelected) WageColors.SelectedText else WageColors.DirtyText,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    hasEntry && wageText.isNotEmpty() -> {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            "¥$wageText",
                            color = if (isSelected) WageColors.SelectedText else WageColors.HasData,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
