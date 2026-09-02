package com.homiq.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentRulesTest {
    @Test
    fun outstandingNeverBecomesNegative() {
        assertEquals(
            0L,
            PaymentRules.outstandingSen(
                bookingTotalSen = 10_000L,
                totalPaidSen = 12_000L,
            ),
        )
    }

    @Test
    fun partialPaymentWithinBalanceIsAllowed() {
        assertTrue(
            PaymentRules.isPaymentWithinBalance(
                amountSen = 5_000L,
                outstandingSen = 10_000L,
            ),
        )
    }

    @Test
    fun overpaymentIsRejected() {
        assertFalse(
            PaymentRules.isPaymentWithinBalance(
                amountSen = 10_001L,
                outstandingSen = 10_000L,
            ),
        )
    }
}
