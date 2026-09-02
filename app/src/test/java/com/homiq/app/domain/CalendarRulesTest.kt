package com.homiq.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRulesTest {
    @Test
    fun dayInsideHalfOpenRangeIsOccupied() {
        assertTrue(
            CalendarRules.containsDay(
                startEpochDay = 10,
                endEpochDayExclusive = 13,
                dayEpoch = 12,
            ),
        )
    }

    @Test
    fun checkoutOrEndDayIsAvailable() {
        assertFalse(
            CalendarRules.containsDay(
                startEpochDay = 10,
                endEpochDayExclusive = 13,
                dayEpoch = 13,
            ),
        )
    }

    @Test
    fun dayBeforeRangeIsAvailable() {
        assertFalse(
            CalendarRules.containsDay(
                startEpochDay = 10,
                endEpochDayExclusive = 13,
                dayEpoch = 9,
            ),
        )
    }
}
