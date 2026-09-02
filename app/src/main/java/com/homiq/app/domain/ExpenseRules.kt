package com.homiq.app.domain

object ExpenseRules {
    fun isAmountValid(amountSen: Long): Boolean =
        amountSen > 0L
}
