package com.aether.nova.phantom.utils

import java.util.Calendar

enum class TimePeriod {
    MORNING,    // 06:00 - 12:00
    AFTERNOON,  // 12:00 - 18:00
    EVENING,    // 18:00 - 22:00
    NIGHT       // 22:00 - 06:00
}

class TimeScheduler {

    fun getCurrentPeriod(): TimePeriod {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..11 -> TimePeriod.MORNING
            hour in 12..17 -> TimePeriod.AFTERNOON
            hour in 18..21 -> TimePeriod.EVENING
            else -> TimePeriod.NIGHT
        }
    }

    fun isNightTime(): Boolean {
        val period = getCurrentPeriod()
        return period == TimePeriod.NIGHT
    }

    fun isMorningTime(): Boolean {
        return getCurrentPeriod() == TimePeriod.MORNING
    }

    fun getInteractionIntervalMs(): Long {
        return when (getCurrentPeriod()) {
            TimePeriod.MORNING -> 60_000L  // 1 min
            TimePeriod.AFTERNOON -> 90_000L // 1.5 min
            TimePeriod.EVENING -> 120_000L  // 2 min
            TimePeriod.NIGHT -> 300_000L    // 5 min
        }
    }

    fun getWalkSpeed(): Float {
        return when (getCurrentPeriod()) {
            TimePeriod.MORNING -> 3.0f
            TimePeriod.AFTERNOON -> 2.5f
            TimePeriod.EVENING -> 1.5f
            TimePeriod.NIGHT -> 0.5f
        }
    }

    fun getEnergyLevel(): Float {
        return when (getCurrentPeriod()) {
            TimePeriod.MORNING -> 1.0f
            TimePeriod.AFTERNOON -> 0.9f
            TimePeriod.EVENING -> 0.5f
            TimePeriod.NIGHT -> 0.2f
        }
    }
}
