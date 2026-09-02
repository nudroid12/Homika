package com.homiq.app.domain

object BookingRules {
    fun isDateRangeValid(
        checkInEpochDay: Long,
        checkOutEpochDay: Long,
    ): Boolean = checkOutEpochDay > checkInEpochDay

    fun rangesOverlap(
        firstStart: Long,
        firstEndExclusive: Long,
        secondStart: Long,
        secondEndExclusive: Long,
    ): Boolean =
        firstStart < secondEndExclusive &&
            firstEndExclusive > secondStart
}
