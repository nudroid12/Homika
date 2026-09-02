package com.homiq.app.domain

object PaymentRules {
    fun outstandingSen(
        bookingTotalSen: Long,
        totalPaidSen: Long,
    ): Long = (bookingTotalSen - totalPaidSen).coerceAtLeast(0L)

    fun isPaymentWithinBalance(
        amountSen: Long,
        outstandingSen: Long,
    ): Boolean =
        amountSen > 0L &&
            outstandingSen > 0L &&
            amountSen <= outstandingSen
}
