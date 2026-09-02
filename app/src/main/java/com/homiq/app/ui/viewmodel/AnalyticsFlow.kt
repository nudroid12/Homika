package com.homiq.app.ui.viewmodel

import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.ExpenseRepository
import com.homiq.app.data.repository.PropertyRepository
import com.homiq.app.domain.BookingRevenueRules
import com.homiq.app.domain.ReportAnalytics
import com.homiq.app.domain.ReportAnalyticsBuilder
import com.homiq.app.domain.ReportPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
fun reportAnalyticsFlow(
    periodFlow: Flow<ReportPeriod>,
    properties: PropertyRepository,
    bookings: BookingRepository,
    blockedDates: BlockedDateRepository,
    expenses: ExpenseRepository,
): Flow<ReportAnalytics> =
    periodFlow.flatMapLatest { period ->
        combine(
            properties.observeAll(),
            bookings.observeInRange(
                rangeStart = period.startEpochDay,
                rangeEndExclusive = period.endEpochDayExclusive,
            ),
            blockedDates.observeInRange(
                rangeStart = period.startEpochDay,
                rangeEndExclusive = period.endEpochDayExclusive,
            ),
            bookings.observeAll(),
            expenses.observeTotalInRangeSen(
                startEpochDay = period.startEpochDay,
                endEpochDayExclusive = period.endEpochDayExclusive,
            ),
        ) {
            propertyList,
            bookingList,
            blockedList,
            allBookings,
            expenseSen,
        ->
            ReportAnalyticsBuilder.build(
                period = period,
                properties = propertyList,
                bookings = bookingList,
                blockedDates = blockedList,
                revenueSen = BookingRevenueRules.revenueInRangeSen(
                    bookings = allBookings,
                    startEpochDay = period.startEpochDay,
                    endEpochDayExclusive = period.endEpochDayExclusive,
                ),
                expensesSen = expenseSen,
            )
        }
    }
