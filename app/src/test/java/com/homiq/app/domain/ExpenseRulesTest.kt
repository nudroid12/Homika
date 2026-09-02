package com.homiq.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseRulesTest {
    @Test
    fun expenseMustBeGreaterThanZero() {
        assertFalse(ExpenseRules.isAmountValid(0L))
        assertFalse(ExpenseRules.isAmountValid(-1L))
        assertTrue(ExpenseRules.isAmountValid(1L))
    }
}
