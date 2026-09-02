package com.homiq.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingRulesTest {
    @Test
    fun checkoutDayCanBeNextBookingCheckinDay() {
        assertFalse(
            BookingRules.rangesOverlap(
                firstStart = 10,
                firstEndExclusive = 12,
                secondStart = 12,
                secondEndExclusive = 14,
            ),
        )
    }

    @Test
    fun overlappingStayIsDetected() {
        assertTrue(
            BookingRules.rangesOverlap(
                firstStart = 10,
                firstEndExclusive = 13,
                secondStart = 12,
                secondEndExclusive = 14,
            ),
        )
    }

    @Test
    fun checkoutMustBeAfterCheckin() {
        assertFalse(
            BookingRules.isDateRangeValid(
                checkInEpochDay = 10,
                checkOutEpochDay = 10,
            ),
        )
        assertTrue(
            BookingRules.isDateRangeValid(
                checkInEpochDay = 10,
                checkOutEpochDay = 11,
            ),
        )
    }
}
