package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.ExpenseRepository
import com.homiq.app.data.repository.PropertyRepository
import com.homiq.app.domain.AnalyticsDashboard
import com.homiq.app.domain.AnalyticsDashboardBuilder
import com.homiq.app.domain.AnalyticsPeriod
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class AnalyticsRangePreset {
    THIS_MONTH,
    THREE_MONTHS,
    SIX_MONTHS,
    YTD,
    CUSTOM,
}

data class ReportsUiState(
    val preset: AnalyticsRangePreset = AnalyticsRangePreset.THIS_MONTH,
    val selectedPropertyId: String? = null,
    val customStartEpochDay: Long = LocalDate.now().withDayOfMonth(1).toEpochDay(),
    val customEndEpochDay: Long = LocalDate.now().toEpochDay(),
    val properties: List<PropertyEntity> = emptyList(),
    val dashboard: AnalyticsDashboard? = null,
)

private data class AnalyticsSelection(
    val preset: AnalyticsRangePreset,
    val selectedPropertyId: String?,
    val customStartEpochDay: Long,
    val customEndEpochDay: Long,
)

private data class AnalyticsResolvedRange(
    val current: AnalyticsPeriod,
    val previous: AnalyticsPeriod,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val properties: PropertyRepository,
    private val bookings: BookingRepository,
    private val blockedDates: BlockedDateRepository,
    private val expenses: ExpenseRepository,
) : ViewModel() {
    private val selectedPreset = MutableStateFlow(AnalyticsRangePreset.THIS_MONTH)
    private val selectedPropertyId = MutableStateFlow<String?>(null)
    private val customStart = MutableStateFlow(
        LocalDate.now().withDayOfMonth(1).toEpochDay(),
    )
    private val customEnd = MutableStateFlow(LocalDate.now().toEpochDay())

    private val selection = combine(
        selectedPreset,
        selectedPropertyId,
        customStart,
        customEnd,
    ) { preset, propertyId, start, end ->
        AnalyticsSelection(
            preset = preset,
            selectedPropertyId = propertyId,
            customStartEpochDay = start,
            customEndEpochDay = end,
        )
    }

    val state: StateFlow<ReportsUiState> = selection
        .flatMapLatest { selected ->
            val range = resolveRange(selected)
            val blockedQueryStart = minOf(
                range.previous.startEpochDay,
                range.current.startEpochDay,
            )
            val blockedQueryEnd = maxOf(
                range.previous.endEpochDayExclusive,
                range.current.endEpochDayExclusive,
            )

            combine(
                properties.observeAll(),
                bookings.observeAll(),
                expenses.observeAll(),
                blockedDates.observeInRange(
                    rangeStart = blockedQueryStart,
                    rangeEndExclusive = blockedQueryEnd,
                ),
            ) { propertyList, bookingList, expenseList, blockedList ->
                ReportsUiState(
                    preset = selected.preset,
                    selectedPropertyId = selected.selectedPropertyId,
                    customStartEpochDay = selected.customStartEpochDay,
                    customEndEpochDay = selected.customEndEpochDay,
                    properties = propertyList,
                    dashboard = AnalyticsDashboardBuilder.build(
                        period = range.current,
                        previousPeriod = range.previous,
                        properties = propertyList,
                        bookings = bookingList,
                        blockedDates = blockedList,
                        expenses = expenseList,
                        selectedPropertyId = selected.selectedPropertyId,
                    ),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportsUiState(),
        )

    fun selectPreset(preset: AnalyticsRangePreset) {
        selectedPreset.value = preset
    }

    fun selectProperty(propertyId: String?) {
        selectedPropertyId.value = propertyId
    }

    fun setCustomStart(epochDay: Long) {
        customStart.value = epochDay
        if (customEnd.value < epochDay) {
            customEnd.value = epochDay
        }
        selectedPreset.value = AnalyticsRangePreset.CUSTOM
    }

    fun setCustomEnd(epochDay: Long) {
        customEnd.value = epochDay
        if (customStart.value > epochDay) {
            customStart.value = epochDay
        }
        selectedPreset.value = AnalyticsRangePreset.CUSTOM
    }

    private fun resolveRange(selection: AnalyticsSelection): AnalyticsResolvedRange {
        val today = LocalDate.now()
        val current: AnalyticsPeriod
        val previous: AnalyticsPeriod

        when (selection.preset) {
            AnalyticsRangePreset.THIS_MONTH -> {
                val month = YearMonth.from(today)
                val start = month.atDay(1)
                val end = month.plusMonths(1).atDay(1)
                val previousStart = month.minusMonths(1).atDay(1)
                current = period(start, end)
                previous = period(previousStart, start)
            }

            AnalyticsRangePreset.THREE_MONTHS -> {
                val currentMonth = YearMonth.from(today)
                val start = currentMonth.minusMonths(2).atDay(1)
                val end = currentMonth.plusMonths(1).atDay(1)
                val previousStart = currentMonth.minusMonths(5).atDay(1)
                current = period(start, end)
                previous = period(previousStart, start)
            }

            AnalyticsRangePreset.SIX_MONTHS -> {
                val currentMonth = YearMonth.from(today)
                val start = currentMonth.minusMonths(5).atDay(1)
                val end = currentMonth.plusMonths(1).atDay(1)
                val previousStart = currentMonth.minusMonths(11).atDay(1)
                current = period(start, end)
                previous = period(previousStart, start)
            }

            AnalyticsRangePreset.YTD -> {
                val start = LocalDate.of(today.year, 1, 1)
                val end = today.plusDays(1)
                current = period(start, end)
                previous = period(
                    start = start.minusYears(1),
                    endExclusive = end.minusYears(1),
                )
            }

            AnalyticsRangePreset.CUSTOM -> {
                val startEpoch = minOf(
                    selection.customStartEpochDay,
                    selection.customEndEpochDay,
                )
                val endInclusiveEpoch = maxOf(
                    selection.customStartEpochDay,
                    selection.customEndEpochDay,
                )
                val endExclusiveEpoch = endInclusiveEpoch + 1L
                val duration = (endExclusiveEpoch - startEpoch).coerceAtLeast(1L)
                current = AnalyticsPeriod(
                    startEpochDay = startEpoch,
                    endEpochDayExclusive = endExclusiveEpoch,
                )
                previous = AnalyticsPeriod(
                    startEpochDay = startEpoch - duration,
                    endEpochDayExclusive = startEpoch,
                )
            }
        }

        return AnalyticsResolvedRange(
            current = current,
            previous = previous,
        )
    }

    private fun period(
        start: LocalDate,
        endExclusive: LocalDate,
    ): AnalyticsPeriod = AnalyticsPeriod(
        startEpochDay = start.toEpochDay(),
        endEpochDayExclusive = endExclusive.toEpochDay(),
    )
}
