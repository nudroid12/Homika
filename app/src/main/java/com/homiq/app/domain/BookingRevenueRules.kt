package com.homiq.app.domain

import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.model.PropertyAmountRow

/**
 * Homika records one received amount on each booking.
 * There is no invoice/outstanding-balance model.
 *
 * Financial periods use the booking check-in date because the booking itself
 * is the single business record and no separate payment date is collected.
 */
object BookingRevenueRules {
    fun revenueInRangeSen(
        bookings: List<BookingEntity>,
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Long =
        bookings
            .asSequence()
            .filter { !it.isDeleted }
            .filter {
                it.checkInEpochDay >= startEpochDay &&
                    it.checkInEpochDay < endEpochDayExclusive
            }
            .sumOf { it.totalAmountSen.coerceAtLeast(0L) }

    fun revenueByPropertyInRange(
        bookings: List<BookingEntity>,
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): List<PropertyAmountRow> =
        bookings
            .asSequence()
            .filter { !it.isDeleted }
            .filter {
                it.checkInEpochDay >= startEpochDay &&
                    it.checkInEpochDay < endEpochDayExclusive
            }
            .groupBy { it.propertyId }
            .map { (propertyId, propertyBookings) ->
                PropertyAmountRow(
                    propertyId = propertyId,
                    amountSen = propertyBookings.sumOf {
                        it.totalAmountSen.coerceAtLeast(0L)
                    },
                )
            }
}
