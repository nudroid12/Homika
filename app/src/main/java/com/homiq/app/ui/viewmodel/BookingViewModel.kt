package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.PropertyRepository
import com.homiq.app.domain.BookingDraft
import com.homiq.app.domain.BookingManager
import com.homiq.app.domain.BookingSaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookingViewModel(
    properties: PropertyRepository,
    private val bookings: BookingRepository,
    private val bookingManager: BookingManager,
    blockedDates: BlockedDateRepository? = null,
) : ViewModel() {
    val bookingList: StateFlow<List<BookingEntity>> =
        bookings.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val blockedDateList: StateFlow<List<BlockedDateEntity>> =
        blockedDates?.observeInRange(
            rangeStart = Long.MIN_VALUE,
            rangeEndExclusive = Long.MAX_VALUE,
        )?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        ) ?: MutableStateFlow<List<BlockedDateEntity>>(emptyList())

    val propertyList: StateFlow<List<PropertyEntity>> =
        properties.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    suspend fun save(
        draft: BookingDraft,
    ): BookingSaveResult = bookingManager.save(draft)

    suspend fun cancel(bookingId: String): Boolean =
        bookingManager.cancel(bookingId)

    fun delete(bookingId: String) {
        viewModelScope.launch {
            bookings.delete(bookingId)
        }
    }
}
