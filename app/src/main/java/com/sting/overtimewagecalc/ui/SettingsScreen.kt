package com.sting.overtimewagecalc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sting.overtimewagecalc.data.Settings
import java.time.LocalDate
import java.time.DayOfWeek

/**
 * 设置屏幕(v2.0 简化)
 *  - 3 个字段:默认日薪 / 默认时薪 / 周末倍数
 *  - v1.2 已删节假日管理,保持
 *  - v1.7 起 sting 偏好:每次保存设置时,跟新设置走"已用旧默认"的条目自动更新(在 ViewModel)
 */
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
                "输入框默认已显示这些值,可手动改;改完即用",
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
