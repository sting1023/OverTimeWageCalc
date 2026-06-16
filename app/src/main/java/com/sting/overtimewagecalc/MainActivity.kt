package com.sting.overtimewagecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sting.overtimewagecalc.ui.HomeScreen
import com.sting.overtimewagecalc.ui.SettingsScreen
import com.sting.overtimewagecalc.ui.WageTheme

/**
 * App 入口(v2.0)
 *  - 极简:ComponentActivity + AppRoot
 *  - 主题/UI 都在 ui/ 包
 *  - ViewModel 全局唯一(WageViewModel)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WageTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val viewModel: WageViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

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
            onMarkDirty = viewModel::markDirty,
            onMarkManyClean = viewModel::markManyClean,
            onOpenSettings = { showSettings = true }
        )
    }
}
