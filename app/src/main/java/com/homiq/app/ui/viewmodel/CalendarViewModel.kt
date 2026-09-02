package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.PropertyRepository
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val properties: List<PropertyEntity> = emptyList(),
    val bookings: List<BookingEntity> = emptyList(),
    val blockedDates: List<BlockedDateEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    properties: PropertyRepository,
    bookings: BookingRepository,
    private val blockedDates: BlockedDateRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())

    private val monthBookings: Flow<List<BookingEntity>> =
        month.flatMapLatest { value ->
            val start = value.atDay(1).toEpochDay()
            val end = value.plusMonths(1).atDay(1).toEpochDay()
            bookings.observeInRange(start, end)
        }

    private val monthBlocks: Flow<List<BlockedDateEntity>> =
        month.flatMapLatest { value ->
            val start = value.atDay(1).toEpochDay()
            val end = value.plusMonths(1).atDay(1).toEpochDay()
            blockedDates.observeInRange(start, end)
        }

    private val activeProperties: Flow<List<PropertyEntity>> =
        properties.observeAll().map { list ->
            list.filter { !it.isDeleted }
        }

    val state: StateFlow<CalendarUiState> =
        combine(
            month,
            activeProperties,
            monthBookings,
            monthBlocks,
        ) { currentMonth, propertyList, bookingList, blockList ->
            CalendarUiState(
                month = currentMonth,
                properties = propertyList,
                bookings = bookingList,
                blockedDates = blockList,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(),
        )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun goToToday() {
        month.value = YearMonth.now()
    }

    fun deleteBlock(blockId: String) {
        viewModelScope.launch {
            blockedDates.delete(blockId)
        }
    }
}
