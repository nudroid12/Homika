package com.homiq.app.domain

object CalendarRules {
    fun containsDay(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
        dayEpoch: Long,
    ): Boolean =
        dayEpoch >= startEpochDay &&
            dayEpoch < endEpochDayExclusive
}
