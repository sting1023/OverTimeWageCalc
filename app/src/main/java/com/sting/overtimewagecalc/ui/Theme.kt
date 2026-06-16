package com.sting.overtimewagecalc.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 主题与配色常量(v2.0)
 *  - 集中放,改色统一改这里
 *  - v1.7 的 5 元素配色方案(今天/选中/周末/有数据/默认)标准化
 */
object WageColors {
    // 5 元素(按优先级)
    val SelectedBg = Color(0xFF006064)    // 深青 800(选中最高优先)
    val SelectedText = Color.White
    val WeekendBg = Color(0xFF90CAF9)     // 蓝 200(周末第二优先)
    val TodayText = Color(0xFF2E7D32)     // 绿 800(今天只改字色)
    val HasData = Color(0xFF4A148C)        // 深紫 800(有数据小点)
    val DirtyText = Color(0xFFD32F2F)      // 红(未保存提示)
}

// Material 主题
@Composable
fun WageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1976D2),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFBBDEFB),
            onPrimaryContainer = Color(0xFF0D47A1),
            secondary = Color(0xFFE65100),
            background = Color(0xFFF5F5F5),
            surface = Color.White
        ),
        content = content
    )
}
