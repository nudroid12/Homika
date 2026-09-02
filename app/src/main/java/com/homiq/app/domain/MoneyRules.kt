package com.homiq.app.domain

object MoneyRules {
    fun netIncomeSen(
        revenueSen: Long,
        expensesSen: Long,
    ): Long = revenueSen - expensesSen
}
