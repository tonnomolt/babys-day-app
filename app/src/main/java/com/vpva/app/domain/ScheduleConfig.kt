package com.vpva.app.domain

/**
 * Päivän aikataulun konfiguraatio.
 *
 * Kaikki offsetit ovat minuutteja heräämisajasta.
 */
data class ScheduleConfig(
    val napsPerDay: Int = 2,
    val napDurations: List<Int> = listOf(90, 90),
    val napStartOffsets: List<Int> = listOf(120, 300),
    val milksPerDay: Int = 4,
    val milkOffsets: List<Int> = listOf(0, 180, 360, 540),
    val mealsPerDay: Int = 3,
    val mealOffsets: List<Int> = listOf(60, 270, 480),
    val bedtimeHour: Int = 20,
    val bedtimeMinute: Int = 0
) {
    fun normalized(): ScheduleConfig {
        val nd = napDurations.take(napsPerDay).toMutableList()
        while (nd.size < napsPerDay) nd.add(90)

        val no = napStartOffsets.take(napsPerDay).toMutableList()
        while (no.size < napsPerDay) {
            val prev = if (no.isNotEmpty()) no.last() + nd[no.size - 1] + 60 else 120
            no.add(prev)
        }

        val mo = milkOffsets.take(milksPerDay).toMutableList()
        while (mo.size < milksPerDay) {
            val prev = if (mo.isNotEmpty()) mo.last() + 120 else 0
            mo.add(prev)
        }

        val fo = mealOffsets.take(mealsPerDay).toMutableList()
        while (fo.size < mealsPerDay) {
            val prev = if (fo.isNotEmpty()) fo.last() + 150 else 60
            fo.add(prev)
        }

        return copy(
            napDurations = nd,
            napStartOffsets = no.sorted(),
            milkOffsets = mo.sorted(),
            mealOffsets = fo.sorted()
        )
    }
}
