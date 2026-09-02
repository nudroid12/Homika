package com.homiq.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsRulesTest {
    @Test
    fun overlapNightsClampsToReportPeriod() {
        assertEquals(
            3L,
            AnalyticsRules.overlapNights(
                periodStartEpochDay = 10L,
                periodEndEpochDayExclusive = 20L,
                stayStartEpochDay = 17L,
                stayEndEpochDayExclusive = 23L,
            ),
        )
    }

    @Test
    fun occupancySubtractsBlockedCapacity() {
        assertEquals(
            50.0,
            AnalyticsRules.occupancyPercent(
                propertyCount = 1,
                periodNights = 10L,
                bookedNights = 4L,
                blockedNights = 2L,
            ),
            0.001,
        )
    }

    @Test
    fun occupancyNeverExceedsHundredPercent() {
        assertEquals(
            100.0,
            AnalyticsRules.occupancyPercent(
                propertyCount = 1,
                periodNights = 5L,
                bookedNights = 10L,
                blockedNights = 0L,
            ),
            0.001,
        )
    }

    @Test
    fun averageBookingValueUsesIntegerSen() {
        assertEquals(
            12_500L,
            AnalyticsRules.averageBookingValueSen(
                bookedValueSen = 25_000L,
                bookingCount = 2,
            ),
        )
    }
}
