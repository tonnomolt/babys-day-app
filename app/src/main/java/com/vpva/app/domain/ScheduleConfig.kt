package com.vpva.app.domain

/**
 * Konffattavat asetukset.
 *
 * napDurations: jokaisen päiväunen kesto minuuteissa (lista, pituus = napsPerDay).
 * Vanhat awakeWindowMinutes/napDurationMinutes korvattu per-uni kestoilla.
 * Valveillaoloikkunat lasketaan automaattisesti timelinelta.
 */
data class ScheduleConfig(
    val milkIntervalMinutes: Int = 180,
    val foodIntervalMinutes: Int = 240,
    val napsPerDay: Int = 2,
    val napDurations: List<Int> = listOf(90, 90),  // per-uni kesto minuuteissa
    val napStartOffsets: List<Int> = listOf(120, 300),  // jokaisen unen alkuhetki minuuteissa heräämisestä
    val bedtimeHour: Int = 20,
    val bedtimeMinute: Int = 0
) {
    /** Palauttaa validoidun kopion jossa listat ovat oikean pituisia */
    fun normalized(): ScheduleConfig {
        val durations = napDurations.take(napsPerDay).toMutableList()
        while (durations.size < napsPerDay) durations.add(90)

        val offsets = napStartOffsets.take(napsPerDay).toMutableList()
        // Generoi puuttuvat offsetit tasaisesti
        if (offsets.size < napsPerDay) {
            val totalMinutes = bedtimeHour * 60 + bedtimeMinute // approx from midnight
            for (i in offsets.size until napsPerDay) {
                val prev = if (i > 0) offsets[i - 1] + durations[i - 1] + 60 else 120
                offsets.add(prev)
            }
        }

        return copy(napDurations = durations, napStartOffsets = offsets)
    }
}
