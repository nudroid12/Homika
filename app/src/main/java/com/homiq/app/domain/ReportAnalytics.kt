package com.homiq.app.domain

import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.model.BookingSource

data class ReportPeriod(
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
)

data class SourceAnalytics(
    val source: BookingSource,
    val bookingCount: Int,
    val bookedValueSen: Long,
)

data class ReportAnalytics(
    val period: ReportPeriod,
    val revenueSen: Long,
    val expensesSen: Long,
    val netIncomeSen: Long,
    val occupancyPercent: Double,
    val bookedNights: Long,
    val blockedNights: Long,
    val availableNights: Long,
    val bookingCount: Int,
    val bookedValueSen: Long,
    val averageBookingValueSen: Long,
    val sourceAnalytics: List<SourceAnalytics>,
)

object ReportAnalyticsBuilder {
    fun build(
        period: ReportPeriod,
        properties: List<PropertyEntity>,
        bookings: List<BookingEntity>,
        blockedDates: List<BlockedDateEntity>,
        revenueSen: Long,
        expensesSen: Long,
    ): ReportAnalytics {
        val periodNights = (
            period.endEpochDayExclusive -
                period.startEpochDay
            ).coerceAtLeast(0L)

        val eligiblePropertyIds = properties
            .filter { property ->
                !property.isDeleted &&
                    (
                        property.isActive ||
                            bookings.any {
                                it.propertyId == property.id
                            } ||
                            blockedDates.any {
                                it.propertyId == property.id
                            }
                    )
            }
            .map { it.id }
            .toSet()

        val eligibleBookings = bookings.filter {
            it.propertyId in eligiblePropertyIds
        }
        val eligibleBlocks = blockedDates.filter {
            it.propertyId in eligiblePropertyIds
        }

        val bookedNights = eligibleBookings.sumOf {
            AnalyticsRules.overlapNights(
                periodStartEpochDay =
                    period.startEpochDay,
                periodEndEpochDayExclusive =
                    period.endEpochDayExclusive,
                stayStartEpochDay =
                    it.checkInEpochDay,
                stayEndEpochDayExclusive =
                    it.checkOutEpochDay,
            )
        }

        val blockedNights = eligibleBlocks.sumOf {
            AnalyticsRules.overlapNights(
                periodStartEpochDay =
                    period.startEpochDay,
                periodEndEpochDayExclusive =
                    period.endEpochDayExclusive,
                stayStartEpochDay =
                    it.startEpochDay,
                stayEndEpochDayExclusive =
                    it.endEpochDay,
            )
        }

        val grossCapacity =
            eligiblePropertyIds.size.toLong() *
                periodNights
        val availableNights =
            (grossCapacity - blockedNights)
                .coerceAtLeast(0L)

        val arrivalBookings = eligibleBookings.filter {
            it.checkInEpochDay >=
                period.startEpochDay &&
                it.checkInEpochDay <
                period.endEpochDayExclusive
        }

        val bookedValueSen =
            arrivalBookings.sumOf {
                it.totalAmountSen
            }

        val sourceAnalytics = arrivalBookings
            .groupBy { it.source }
            .map { (source, sourceBookings) ->
                SourceAnalytics(
                    source = source,
                    bookingCount =
                        sourceBookings.size,
                    bookedValueSen =
                        sourceBookings.sumOf {
                            it.totalAmountSen
                        },
                )
            }
            .sortedWith(
                compareByDescending<SourceAnalytics> {
                    it.bookingCount
                }.thenByDescending {
                    it.bookedValueSen
                },
            )

        return ReportAnalytics(
            period = period,
            revenueSen = revenueSen,
            expensesSen = expensesSen,
            netIncomeSen = MoneyRules.netIncomeSen(
                revenueSen = revenueSen,
                expensesSen = expensesSen,
            ),
            occupancyPercent =
                AnalyticsRules.occupancyPercent(
                    propertyCount =
                        eligiblePropertyIds.size,
                    periodNights = periodNights,
                    bookedNights = bookedNights,
                    blockedNights = blockedNights,
                ),
            bookedNights = bookedNights,
            blockedNights = blockedNights,
            availableNights = availableNights,
            bookingCount = arrivalBookings.size,
            bookedValueSen = bookedValueSen,
            averageBookingValueSen =
                AnalyticsRules.averageBookingValueSen(
                    bookedValueSen = bookedValueSen,
                    bookingCount =
                        arrivalBookings.size,
                ),
            sourceAnalytics = sourceAnalytics,
        )
    }
}
