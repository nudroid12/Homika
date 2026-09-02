package com.homiq.app.data.local.model

data class BookingBalanceRow(
    val bookingId: String,
    val totalAmountSen: Long,
    val paidAmountSen: Long,
) {
    fun outstandingSen(): Long =
        (totalAmountSen - paidAmountSen).coerceAtLeast(0L)
}
