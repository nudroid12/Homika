package com.homiq.app.domain

object AnalyticsRules {
    fun overlapNights(
        periodStartEpochDay: Long,
        periodEndEpochDayExclusive: Long,
        stayStartEpochDay: Long,
        stayEndEpochDayExclusive: Long,
    ): Long {
        val start = maxOf(
            periodStartEpochDay,
            stayStartEpochDay,
        )
        val end = minOf(
            periodEndEpochDayExclusive,
            stayEndEpochDayExclusive,
        )
        return (end - start).coerceAtLeast(0L)
    }

    fun occupancyPercent(
        propertyCount: Int,
        periodNights: Long,
        bookedNights: Long,
        blockedNights: Long,
    ): Double {
        if (propertyCount <= 0 || periodNights <= 0L) {
            return 0.0
        }

        val grossCapacity =
            propertyCount.toLong() * periodNights
        val availableCapacity =
            (grossCapacity - blockedNights)
                .coerceAtLeast(0L)

        if (availableCapacity <= 0L) {
            return 0.0
        }

        return (
            bookedNights.toDouble() /
                availableCapacity.toDouble() *
                100.0
            )
            .coerceIn(0.0, 100.0)
    }

    fun averageBookingValueSen(
        bookedValueSen: Long,
        bookingCount: Int,
    ): Long =
        if (bookingCount <= 0) {
            0L
        } else {
            bookedValueSen / bookingCount
        }
}
