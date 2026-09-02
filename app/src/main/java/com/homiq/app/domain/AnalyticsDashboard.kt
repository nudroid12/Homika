package com.homiq.app.domain

import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.model.BookingStatus
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/**
 * Management analytics for Homika.
 *
 * Revenue follows Homika's locked financial model: the amount stored on a booking
 * is money already received. Financial periods use the booking check-in date.
 */
data class AnalyticsPeriod(
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
)

data class AnalyticsMetrics(
    val revenueSen: Long = 0L,
    val expensesSen: Long = 0L,
    val netProfitSen: Long = 0L,
    val occupancyPercent: Double = 0.0,
    val bookingCount: Int = 0,
    val bookedNights: Long = 0L,
    val blockedNights: Long = 0L,
    val availableNights: Long = 0L,
    val averageBookingValueSen: Long = 0L,
    val averageStayNights: Double = 0.0,
    val revenuePerAvailableNightSen: Long = 0L,
)

data class AnalyticsComparison(
    val revenuePercentChange: Double? = null,
    val netProfitPercentChange: Double? = null,
    val occupancyPointChange: Double = 0.0,
    val bookingCountDelta: Int = 0,
)

data class AnalyticsTrendPoint(
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
    val revenueSen: Long,
    val netProfitSen: Long,
    val occupancyPercent: Double,
)

data class PropertyPerformance(
    val propertyId: String,
    val propertyName: String,
    val revenueSen: Long,
    val expensesSen: Long,
    val netProfitSen: Long,
    val occupancyPercent: Double,
    val bookingCount: Int,
)

data class AnalyticsDashboard(
    val period: AnalyticsPeriod,
    val previousPeriod: AnalyticsPeriod,
    val current: AnalyticsMetrics,
    val previous: AnalyticsMetrics,
    val comparison: AnalyticsComparison,
    val trend: List<AnalyticsTrendPoint>,
    val propertyPerformance: List<PropertyPerformance>,
)

object AnalyticsDashboardBuilder {
    fun build(
        period: AnalyticsPeriod,
        previousPeriod: AnalyticsPeriod,
        properties: List<PropertyEntity>,
        bookings: List<BookingEntity>,
        blockedDates: List<BlockedDateEntity>,
        expenses: List<ExpenseEntity>,
        selectedPropertyId: String?,
    ): AnalyticsDashboard {
        val visibleProperties = properties.filter { !it.isDeleted }
        val selectedProperties = if (selectedPropertyId == null) {
            visibleProperties.filter { property ->
                property.isActive ||
                    bookings.any { booking ->
                        !booking.isDeleted &&
                            booking.propertyId == property.id &&
                            booking.checkInEpochDay < period.endEpochDayExclusive &&
                            booking.checkOutEpochDay > period.startEpochDay
                    } ||
                    blockedDates.any { block ->
                        !block.isDeleted &&
                            block.propertyId == property.id &&
                            block.startEpochDay < period.endEpochDayExclusive &&
                            block.endEpochDay > period.startEpochDay
                    }
            }
        } else {
            visibleProperties.filter { it.id == selectedPropertyId }
        }
        val selectedIds = selectedProperties.map { it.id }.toSet()

        val eligibleBookings = bookings.filter {
            !it.isDeleted && it.propertyId in selectedIds
        }
        val operationalBookings = eligibleBookings.filter {
            it.status != BookingStatus.CANCELLED
        }
        val eligibleBlocks = blockedDates.filter {
            !it.isDeleted && it.propertyId in selectedIds
        }
        val eligibleExpenses = expenses.filter { expense ->
            !expense.isDeleted && (
                selectedPropertyId == null ||
                    expense.propertyId == selectedPropertyId
                )
        }

        val current = metricsFor(
            period = period,
            propertyCount = selectedProperties.size,
            revenueBookings = eligibleBookings,
            operationalBookings = operationalBookings,
            blocks = eligibleBlocks,
            expenses = eligibleExpenses,
        )
        val previous = metricsFor(
            period = previousPeriod,
            propertyCount = selectedProperties.size,
            revenueBookings = eligibleBookings,
            operationalBookings = operationalBookings,
            blocks = eligibleBlocks,
            expenses = eligibleExpenses,
        )

        val trend = buildBuckets(period).map { bucket ->
            val bucketMetrics = metricsFor(
                period = bucket,
                propertyCount = selectedProperties.size,
                revenueBookings = eligibleBookings,
                operationalBookings = operationalBookings,
                blocks = eligibleBlocks,
                expenses = eligibleExpenses,
            )
            AnalyticsTrendPoint(
                startEpochDay = bucket.startEpochDay,
                endEpochDayExclusive = bucket.endEpochDayExclusive,
                revenueSen = bucketMetrics.revenueSen,
                netProfitSen = bucketMetrics.netProfitSen,
                occupancyPercent = bucketMetrics.occupancyPercent,
            )
        }

        val propertyPerformance = if (selectedPropertyId == null) {
            selectedProperties.map { property ->
                val propertyMetrics = metricsFor(
                    period = period,
                    propertyCount = 1,
                    revenueBookings = bookings.filter {
                        !it.isDeleted && it.propertyId == property.id
                    },
                    operationalBookings = bookings.filter {
                        !it.isDeleted &&
                            it.propertyId == property.id &&
                            it.status != BookingStatus.CANCELLED
                    },
                    blocks = blockedDates.filter {
                        !it.isDeleted && it.propertyId == property.id
                    },
                    expenses = expenses.filter {
                        !it.isDeleted && it.propertyId == property.id
                    },
                )
                PropertyPerformance(
                    propertyId = property.id,
                    propertyName = property.name,
                    revenueSen = propertyMetrics.revenueSen,
                    expensesSen = propertyMetrics.expensesSen,
                    netProfitSen = propertyMetrics.netProfitSen,
                    occupancyPercent = propertyMetrics.occupancyPercent,
                    bookingCount = propertyMetrics.bookingCount,
                )
            }.sortedWith(
                compareByDescending<PropertyPerformance> { it.revenueSen }
                    .thenByDescending { it.occupancyPercent }
                    .thenBy { it.propertyName.lowercase() },
            )
        } else {
            emptyList()
        }

        return AnalyticsDashboard(
            period = period,
            previousPeriod = previousPeriod,
            current = current,
            previous = previous,
            comparison = AnalyticsComparison(
                revenuePercentChange = percentChange(
                    current = current.revenueSen,
                    previous = previous.revenueSen,
                ),
                netProfitPercentChange = percentChange(
                    current = current.netProfitSen,
                    previous = previous.netProfitSen,
                ),
                occupancyPointChange =
                    current.occupancyPercent - previous.occupancyPercent,
                bookingCountDelta = current.bookingCount - previous.bookingCount,
            ),
            trend = trend,
            propertyPerformance = propertyPerformance,
        )
    }

    private fun metricsFor(
        period: AnalyticsPeriod,
        propertyCount: Int,
        revenueBookings: List<BookingEntity>,
        operationalBookings: List<BookingEntity>,
        blocks: List<BlockedDateEntity>,
        expenses: List<ExpenseEntity>,
    ): AnalyticsMetrics {
        val periodNights = (
            period.endEpochDayExclusive - period.startEpochDay
            ).coerceAtLeast(0L)

        val arrivalRevenueBookings = revenueBookings.filter {
            it.checkInEpochDay >= period.startEpochDay &&
                it.checkInEpochDay < period.endEpochDayExclusive
        }
        val arrivalOperationalBookings = operationalBookings.filter {
            it.checkInEpochDay >= period.startEpochDay &&
                it.checkInEpochDay < period.endEpochDayExclusive
        }
        val overlappingBookings = operationalBookings.filter {
            it.checkInEpochDay < period.endEpochDayExclusive &&
                it.checkOutEpochDay > period.startEpochDay
        }
        val overlappingBlocks = blocks.filter {
            it.startEpochDay < period.endEpochDayExclusive &&
                it.endEpochDay > period.startEpochDay
        }

        val revenueSen = arrivalRevenueBookings.sumOf {
            it.totalAmountSen.coerceAtLeast(0L)
        }
        val expensesSen = expenses.asSequence()
            .filter {
                it.expenseDateEpochDay >= period.startEpochDay &&
                    it.expenseDateEpochDay < period.endEpochDayExclusive
            }
            .sumOf { it.amountSen.coerceAtLeast(0L) }
        val bookedNights = overlappingBookings.sumOf {
            AnalyticsRules.overlapNights(
                periodStartEpochDay = period.startEpochDay,
                periodEndEpochDayExclusive = period.endEpochDayExclusive,
                stayStartEpochDay = it.checkInEpochDay,
                stayEndEpochDayExclusive = it.checkOutEpochDay,
            )
        }
        val blockedNights = overlappingBlocks.sumOf {
            AnalyticsRules.overlapNights(
                periodStartEpochDay = period.startEpochDay,
                periodEndEpochDayExclusive = period.endEpochDayExclusive,
                stayStartEpochDay = it.startEpochDay,
                stayEndEpochDayExclusive = it.endEpochDay,
            )
        }
        val grossCapacity = propertyCount.toLong() * periodNights
        val availableNights = (grossCapacity - blockedNights).coerceAtLeast(0L)
        val bookingCount = arrivalOperationalBookings.size
        val averageBookingValueSen = if (bookingCount == 0) {
            0L
        } else {
            arrivalOperationalBookings.sumOf {
                it.totalAmountSen.coerceAtLeast(0L)
            } / bookingCount
        }
        val averageStayNights = if (bookingCount == 0) {
            0.0
        } else {
            arrivalOperationalBookings.sumOf {
                (it.checkOutEpochDay - it.checkInEpochDay).coerceAtLeast(0L)
            }.toDouble() / bookingCount.toDouble()
        }
        val revenuePerAvailableNightSen = if (availableNights <= 0L) {
            0L
        } else {
            revenueSen / availableNights
        }

        return AnalyticsMetrics(
            revenueSen = revenueSen,
            expensesSen = expensesSen,
            netProfitSen = MoneyRules.netIncomeSen(
                revenueSen = revenueSen,
                expensesSen = expensesSen,
            ),
            occupancyPercent = AnalyticsRules.occupancyPercent(
                propertyCount = propertyCount,
                periodNights = periodNights,
                bookedNights = bookedNights,
                blockedNights = blockedNights,
            ),
            bookingCount = bookingCount,
            bookedNights = bookedNights,
            blockedNights = blockedNights,
            availableNights = availableNights,
            averageBookingValueSen = averageBookingValueSen,
            averageStayNights = averageStayNights,
            revenuePerAvailableNightSen = revenuePerAvailableNightSen,
        )
    }

    private fun buildBuckets(period: AnalyticsPeriod): List<AnalyticsPeriod> {
        val dayCount = period.endEpochDayExclusive - period.startEpochDay
        if (dayCount <= 45L) {
            return (period.startEpochDay until period.endEpochDayExclusive).map { day ->
                AnalyticsPeriod(day, day + 1L)
            }
        }

        val periodStart = LocalDate.ofEpochDay(period.startEpochDay)
        val periodEndExclusive = LocalDate.ofEpochDay(period.endEpochDayExclusive)
        val buckets = mutableListOf<AnalyticsPeriod>()
        var cursor = YearMonth.from(periodStart).atDay(1)
        while (cursor.isBefore(periodEndExclusive)) {
            val monthEnd = cursor.plusMonths(1)
            val bucketStart = maxOf(cursor, periodStart)
            val bucketEnd = minOf(monthEnd, periodEndExclusive)
            if (bucketStart.isBefore(bucketEnd)) {
                buckets += AnalyticsPeriod(
                    startEpochDay = bucketStart.toEpochDay(),
                    endEpochDayExclusive = bucketEnd.toEpochDay(),
                )
            }
            cursor = monthEnd
        }
        return buckets
    }

    private fun percentChange(
        current: Long,
        previous: Long,
    ): Double? {
        if (previous == 0L) return null
        return (
            (current - previous).toDouble() /
                abs(previous.toDouble()) *
                100.0
            )
    }
}
