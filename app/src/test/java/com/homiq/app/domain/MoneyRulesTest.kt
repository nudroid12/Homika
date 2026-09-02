package com.homiq.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyRulesTest {
    @Test
    fun netIncomeIsRevenueMinusExpenses() {
        assertEquals(
            35_000L,
            MoneyRules.netIncomeSen(
                revenueSen = 50_000L,
                expensesSen = 15_000L,
            ),
        )
    }

    @Test
    fun netIncomeCanBeNegative() {
        assertEquals(
            -5_000L,
            MoneyRules.netIncomeSen(
                revenueSen = 10_000L,
                expensesSen = 15_000L,
            ),
        )
    }
}
