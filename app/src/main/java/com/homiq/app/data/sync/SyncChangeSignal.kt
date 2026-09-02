package com.homiq.app.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SyncChangeSignal {
    private val mutableChanges =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
        )

    val changes: SharedFlow<Unit> =
        mutableChanges.asSharedFlow()

    fun notifyChanged() {
        mutableChanges.tryEmit(Unit)
    }
}
