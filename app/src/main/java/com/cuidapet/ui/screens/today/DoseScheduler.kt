package com.cuidadopet.ui.screens.today

import com.cuidadopet.data.db.entity.MedicationEntity

internal object DoseScheduler {

    fun calculateDosesForDay(
        med: MedicationEntity,
        dayStart: Long,
        dayEnd: Long
    ): List<Long> {
        return when (med.frequencyType) {

            "FIXED_TIMES" -> {
                med.fixedTimes
                    ?.removePrefix("[")?.removeSuffix("]")
                    ?.split(",")
                    ?.map { it.trim().removeSurrounding("\"") }
                    ?.filter { it.isNotBlank() }
                    ?.mapNotNull { time ->
                        val parts = time.split(":").mapNotNull { it.toIntOrNull() }
                        if (parts.size != 2) return@mapNotNull null
                        val (h, m) = parts
                        dayStart + h * 3_600_000L + m * 60_000L
                    }
                    ?: emptyList()
            }

            "INTERVAL" -> {
                val intervalMs = (med.frequencyHours ?: 24) * 3_600_000L
                val doses = mutableListOf<Long>()

                val elapsed   = dayStart - med.startDate
                val skipCount = if (elapsed <= 0L) 0L else elapsed / intervalMs
                var t         = med.startDate + skipCount * intervalMs

                while (t < dayStart) t += intervalMs

                while (t <= dayEnd) {
                    doses.add(t)
                    t += intervalMs
                }
                doses
            }

            else -> emptyList()
        }
    }
}
