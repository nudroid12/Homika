package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.repository.PropertyRepository
import com.homiq.app.domain.BlockedDateDraft
import com.homiq.app.domain.BlockedDateManager
import com.homiq.app.domain.BlockedDateSaveResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BlockedDateViewModel(
    properties: PropertyRepository,
    private val manager: BlockedDateManager,
) : ViewModel() {
    val activeProperties: StateFlow<List<PropertyEntity>> =
        properties.observeAll()
            .map { list ->
                list.filter {
                    it.isActive && !it.isDeleted
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    suspend fun save(
        draft: BlockedDateDraft,
    ): BlockedDateSaveResult =
        manager.save(draft)

    suspend fun delete(id: String) {
        manager.delete(id)
    }
}
