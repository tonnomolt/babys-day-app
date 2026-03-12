package com.vpva.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vpva.app.domain.ScheduleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "baby_day_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val WAKE_HOUR = intPreferencesKey("wake_hour")
        val WAKE_MINUTE = intPreferencesKey("wake_minute")
        val AWAKE_WINDOW = intPreferencesKey("awake_window_min")
        val NAP_DURATION = intPreferencesKey("nap_duration_min")
        val MILK_INTERVAL = intPreferencesKey("milk_interval_min")
        val FOOD_INTERVAL = intPreferencesKey("food_interval_min")
        val NAPS_PER_DAY = intPreferencesKey("naps_per_day")
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
    }

    data class WakeTime(val hour: Int, val minute: Int)

    val wakeTimeFlow: Flow<WakeTime?> = context.dataStore.data.map { prefs ->
        val h = prefs[Keys.WAKE_HOUR]
        val m = prefs[Keys.WAKE_MINUTE]
        if (h != null && m != null) WakeTime(h, m) else null
    }

    val configFlow: Flow<ScheduleConfig> = context.dataStore.data.map { prefs ->
        ScheduleConfig(
            awakeWindowMinutes = prefs[Keys.AWAKE_WINDOW] ?: 120,
            napDurationMinutes = prefs[Keys.NAP_DURATION] ?: 90,
            milkIntervalMinutes = prefs[Keys.MILK_INTERVAL] ?: 180,
            foodIntervalMinutes = prefs[Keys.FOOD_INTERVAL] ?: 240,
            napsPerDay = prefs[Keys.NAPS_PER_DAY] ?: 2,
            bedtimeHour = prefs[Keys.BEDTIME_HOUR] ?: 20,
            bedtimeMinute = prefs[Keys.BEDTIME_MINUTE] ?: 0
        )
    }

    suspend fun saveWakeTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WAKE_HOUR] = hour
            prefs[Keys.WAKE_MINUTE] = minute
        }
    }

    suspend fun saveConfig(config: ScheduleConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AWAKE_WINDOW] = config.awakeWindowMinutes
            prefs[Keys.NAP_DURATION] = config.napDurationMinutes
            prefs[Keys.MILK_INTERVAL] = config.milkIntervalMinutes
            prefs[Keys.FOOD_INTERVAL] = config.foodIntervalMinutes
            prefs[Keys.NAPS_PER_DAY] = config.napsPerDay
            prefs[Keys.BEDTIME_HOUR] = config.bedtimeHour
            prefs[Keys.BEDTIME_MINUTE] = config.bedtimeMinute
        }
    }
}
