package com.sting.overtimewagecalc.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 持久化 WageUiState 的 entries 和 settings 到 SharedPreferences
 * v1.8 — 升级安装数据不丢(SharedPreferences 跟应用同一份 data,升级不删)
 *
 * 卸载重装会清空(SharedPreferences 跟包一起删),符合"升级保留 / 重装清空"语义
 */
object Storage {
    private const val PREF_NAME = "app_state"
    private const val KEY_STATE = "state_v1"

    data class Loaded(
        val entries: List<DayEntry>,
        val settings: Settings
    )

    fun save(context: Context, entries: List<DayEntry>, settings: Settings) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val root = JSONObject()

        // entries
        val arr = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("date", entry.date.toString())
            obj.put("dailyWageEnabled", entry.dailyWageEnabled)
            obj.put("dailyRate", entry.dailyRate)
            obj.put("hourlyRate", entry.hourlyRate)
            obj.put("overtimeMultiplier", entry.overtimeMultiplier)
            obj.put("overtimeHours", entry.overtimeHours)
            obj.put("extraOvertime", entry.extraOvertime)
            obj.put("dayNote", entry.dayNote)   // v2.0 改
            arr.put(obj)
        }
        root.put("entries", arr)

        // settings
        val sObj = JSONObject()
        sObj.put("defaultDailyRate", settings.defaultDailyRate)
        sObj.put("defaultHourlyRate", settings.defaultHourlyRate)
        sObj.put("weekendMultiplier", settings.weekendMultiplier)
        root.put("settings", sObj)

        prefs.edit().putString(KEY_STATE, root.toString()).apply()
    }

    fun load(context: Context): Loaded? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_STATE, null) ?: return null
        return try {
            val root = JSONObject(json)

            val arr = root.getJSONArray("entries")
            val entries = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DayEntry(
                    date = LocalDate.parse(o.getString("date")),
                    dailyWageEnabled = o.optBoolean("dailyWageEnabled", false),
                    dailyRate = o.optDouble("dailyRate", 0.0),
                    hourlyRate = o.optDouble("hourlyRate", 0.0),
                    overtimeMultiplier = o.optDouble("overtimeMultiplier", 1.0),
                    overtimeHours = o.optDouble("overtimeHours", 0.0),
                    extraOvertime = o.optDouble("extraOvertime", 0.0),
                    dayNote = o.optString("dayNote", "")  // v2.0 改
                )
            }

            val sObj = root.getJSONObject("settings")
            val settings = Settings(
                defaultDailyRate = sObj.optDouble("defaultDailyRate", 400.0),
                defaultHourlyRate = sObj.optDouble("defaultHourlyRate", 50.0),
                weekendMultiplier = sObj.optDouble("weekendMultiplier", 1.5)
            )

            Loaded(entries, settings)
        } catch (e: Exception) {
            // 数据损坏 / 字段缺失 → 当作新装,返 null 用默认
            null
        }
    }
}
