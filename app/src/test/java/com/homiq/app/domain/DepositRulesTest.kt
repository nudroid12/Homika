package com.homiq.app.domain

import com.homiq.app.data.model.DepositStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DepositRulesTest {
    @Test
    fun remainingDepositUsesReturnedAmount() {
        assertEquals(
            6_000L,
            DepositRules.remainingSen(
                depositAmountSen = 10_000L,
                returnedAmountSen = 4_000L,
            ),
        )
    }

    @Test
    fun partialReturnGetsPartialStatus() {
        assertEquals(
            DepositStatus.PARTIALLY_RETURNED,
            DepositRules.statusAfterReturn(
                depositAmountSen = 10_000L,
                returnedAmountSen = 4_000L,
            ),
        )
    }

    @Test
    fun fullReturnGetsReturnedStatus() {
        assertEquals(
            DepositStatus.RETURNED,
            DepositRules.statusAfterReturn(
                depositAmountSen = 10_000L,
                returnedAmountSen = 10_000L,
            ),
        )
    }
}
