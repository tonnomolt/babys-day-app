package com.vpva.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vpva.app.domain.ScheduleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "baby_day_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val WAKE_HOUR = intPreferencesKey("wake_hour")
        val WAKE_MINUTE = intPreferencesKey("wake_minute")
        val MILK_INTERVAL = intPreferencesKey("milk_interval_min")
        val FOOD_INTERVAL = intPreferencesKey("food_interval_min")
        val NAPS_PER_DAY = intPreferencesKey("naps_per_day")
        val NAP_DURATIONS = stringPreferencesKey("nap_durations")  // CSV: "90,60,90"
        val NAP_START_OFFSETS = stringPreferencesKey("nap_start_offsets")  // CSV: "120,300"
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    data class WakeTime(val hour: Int, val minute: Int)

    val wakeTimeFlow: Flow<WakeTime?> = context.dataStore.data.map { prefs ->
        val h = prefs[Keys.WAKE_HOUR]
        val m = prefs[Keys.WAKE_MINUTE]
        if (h != null && m != null) WakeTime(h, m) else null
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val configFlow: Flow<ScheduleConfig> = context.dataStore.data.map { prefs ->
        val naps = prefs[Keys.NAPS_PER_DAY] ?: 2
        val durations = prefs[Keys.NAP_DURATIONS]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: listOf(90, 90)
        val offsets = prefs[Keys.NAP_START_OFFSETS]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: listOf(120, 300)
        ScheduleConfig(
            milkIntervalMinutes = prefs[Keys.MILK_INTERVAL] ?: 180,
            foodIntervalMinutes = prefs[Keys.FOOD_INTERVAL] ?: 240,
            napsPerDay = naps,
            napDurations = durations,
            napStartOffsets = offsets,
            bedtimeHour = prefs[Keys.BEDTIME_HOUR] ?: 20,
            bedtimeMinute = prefs[Keys.BEDTIME_MINUTE] ?: 0
        ).normalized()
    }

    suspend fun saveWakeTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WAKE_HOUR] = hour
            prefs[Keys.WAKE_MINUTE] = minute
        }
    }

    suspend fun saveConfig(config: ScheduleConfig) {
        val normalized = config.normalized()
        context.dataStore.edit { prefs ->
            prefs[Keys.MILK_INTERVAL] = normalized.milkIntervalMinutes
            prefs[Keys.FOOD_INTERVAL] = normalized.foodIntervalMinutes
            prefs[Keys.NAPS_PER_DAY] = normalized.napsPerDay
            prefs[Keys.NAP_DURATIONS] = normalized.napDurations.joinToString(",")
            prefs[Keys.NAP_START_OFFSETS] = normalized.napStartOffsets.joinToString(",")
            prefs[Keys.BEDTIME_HOUR] = normalized.bedtimeHour
            prefs[Keys.BEDTIME_MINUTE] = normalized.bedtimeMinute
        }
    }
}
