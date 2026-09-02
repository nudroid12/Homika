package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.ExpenseRepository
import com.homiq.app.data.repository.PropertyRepository
import com.homiq.app.domain.BookingReferenceRules
import com.homiq.app.domain.PropertyDraft
import com.homiq.app.domain.PropertySaveIssue
import com.homiq.app.domain.PropertySaveResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class PropertyDeleteResult {
    SUCCESS,
    HAS_RELATED_DATA,
}

class PropertyViewModel(
    private val properties: PropertyRepository,
    private val bookings: BookingRepository,
    private val expenses: ExpenseRepository,
    private val blockedDates: BlockedDateRepository,
) : ViewModel() {
    val propertyList: StateFlow<List<PropertyEntity>> =
        properties.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    suspend fun save(
        draft: PropertyDraft,
    ): PropertySaveResult {
        if (draft.name.isBlank()) {
            return PropertySaveResult.Failure(
                PropertySaveIssue.NAME_REQUIRED,
            )
        }
        if (draft.defaultNightlyRateSen < 0L) {
            return PropertySaveResult.Failure(
                PropertySaveIssue.INVALID_RATE,
            )
        }

        val existing = if (draft.id != null) {
            properties.getById(draft.id)
        } else {
            null
        }

        val entity = PropertyEntity(
            id = existing?.id ?: draft.id
                ?: java.util.UUID.randomUUID().toString(),
            name = draft.name.trim(),
            bookingCode = BookingReferenceRules.effectivePropertyCode(
                storedCode = draft.bookingCode,
                propertyName = draft.name,
            ),
            address = draft.address.trim().ifBlank { null },
            notes = draft.notes.trim().ifBlank { null },
            defaultNightlyRateSen = draft.defaultNightlyRateSen,
            isActive = draft.isActive,
            createdAtEpochMillis = existing?.createdAtEpochMillis
                ?: System.currentTimeMillis(),
            updatedAtEpochMillis = existing?.updatedAtEpochMillis
                ?: System.currentTimeMillis(),
            revision = existing?.revision ?: 0L,
            isDeleted = existing?.isDeleted ?: false,
        )

        properties.save(entity)
        return PropertySaveResult.Success(entity.id)
    }
    suspend fun delete(propertyId: String): PropertyDeleteResult {
        val hasRelatedData =
            bookings.countForProperty(propertyId) > 0 ||
                expenses.countForProperty(propertyId) > 0 ||
                blockedDates.countForProperty(propertyId) > 0

        if (hasRelatedData) {
            return PropertyDeleteResult.HAS_RELATED_DATA
        }

        properties.delete(propertyId)
        return PropertyDeleteResult.SUCCESS
    }

}
