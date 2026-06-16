package com.sting.overtimewagecalc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sting.overtimewagecalc.data.DayEntry
import com.sting.overtimewagecalc.data.Settings

/**
 * 本月明细(v2.0)
 *  - v1.7 起:list ALL 有数据的天(不限数量,自动滚动)
 *  - v1.6 起:按日期升序(WageCalculator.entriesForMonth 已 sortBy)
 */
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
private fun DetailRow(entry: DayEntry, settings: Settings) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${entry.date} · ${Settings.dateTypeLabel(entry.date)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
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
            if (entry.extraOvertime > 0 || entry.dayNote.isNotBlank()) {
                val extraText = buildString {
                    append("额外 +¥${"%.2f".format(entry.extraOvertime)}")
                    if (entry.dayNote.isNotBlank()) append(" · ${entry.dayNote}")
                }
                Text(
                    extraText,
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
