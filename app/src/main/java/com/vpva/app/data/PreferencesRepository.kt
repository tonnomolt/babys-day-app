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
        val NAPS_PER_DAY = intPreferencesKey("naps_per_day")
        val NAP_DURATIONS = stringPreferencesKey("nap_durations")
        val NAP_START_OFFSETS = stringPreferencesKey("nap_start_offsets")
        val MILKS_PER_DAY = intPreferencesKey("milks_per_day")
        val MILK_OFFSETS = stringPreferencesKey("milk_offsets")
        val MEALS_PER_DAY = intPreferencesKey("meals_per_day")
        val MEAL_OFFSETS = stringPreferencesKey("meal_offsets")
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

    private fun String?.toCsvInts(): List<Int>? =
        this?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.isNotEmpty() }

    val configFlow: Flow<ScheduleConfig> = context.dataStore.data.map { prefs ->
        ScheduleConfig(
            napsPerDay = prefs[Keys.NAPS_PER_DAY] ?: 2,
            napDurations = prefs[Keys.NAP_DURATIONS].toCsvInts() ?: listOf(90, 90),
            napStartOffsets = prefs[Keys.NAP_START_OFFSETS].toCsvInts() ?: listOf(120, 300),
            milksPerDay = prefs[Keys.MILKS_PER_DAY] ?: 4,
            milkOffsets = prefs[Keys.MILK_OFFSETS].toCsvInts() ?: listOf(0, 180, 360, 540),
            mealsPerDay = prefs[Keys.MEALS_PER_DAY] ?: 3,
            mealOffsets = prefs[Keys.MEAL_OFFSETS].toCsvInts() ?: listOf(60, 270, 480),
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
        val n = config.normalized()
        context.dataStore.edit { prefs ->
            prefs[Keys.NAPS_PER_DAY] = n.napsPerDay
            prefs[Keys.NAP_DURATIONS] = n.napDurations.joinToString(",")
            prefs[Keys.NAP_START_OFFSETS] = n.napStartOffsets.joinToString(",")
            prefs[Keys.MILKS_PER_DAY] = n.milksPerDay
            prefs[Keys.MILK_OFFSETS] = n.milkOffsets.joinToString(",")
            prefs[Keys.MEALS_PER_DAY] = n.mealsPerDay
            prefs[Keys.MEAL_OFFSETS] = n.mealOffsets.joinToString(",")
            prefs[Keys.BEDTIME_HOUR] = n.bedtimeHour
            prefs[Keys.BEDTIME_MINUTE] = n.bedtimeMinute
        }
    }
}
