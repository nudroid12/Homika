package com.homiq.app.data.backup

import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity

data class HomiqBackupSnapshot(
    val createdAtEpochMillis: Long,
    val properties: List<PropertyEntity>,
    val bookings: List<BookingEntity>,
    val payments: List<PaymentEntity>,
    val deposits: List<DepositEntity>,
    val expenses: List<ExpenseEntity>,
    val blockedDates: List<BlockedDateEntity>,
)

data class BackupPreview(
    val createdAtEpochMillis: Long,
    val propertyCount: Int,
    val bookingCount: Int,
    val paymentCount: Int,
    val depositCount: Int,
    val expenseCount: Int,
    val blockedDateCount: Int,
) {
    val totalRecordCount: Int
        get() =
            propertyCount +
                bookingCount +
                paymentCount +
                depositCount +
                expenseCount +
                blockedDateCount
}

data class BackupHistory(
    val lastBackupEpochMillis: Long?,
    val lastRestoreEpochMillis: Long?,
)

sealed interface BackupWriteResult {
    data class Success(
        val preview: BackupPreview,
    ) : BackupWriteResult

    data class Failure(
        val reason: BackupFailureReason,
    ) : BackupWriteResult
}

sealed interface BackupReadResult {
    data class Success(
        val preview: BackupPreview,
    ) : BackupReadResult

    data class Failure(
        val reason: BackupFailureReason,
    ) : BackupReadResult
}

sealed interface BackupRestoreResult {
    data class Success(
        val preview: BackupPreview,
    ) : BackupRestoreResult

    data class Failure(
        val reason: BackupFailureReason,
    ) : BackupRestoreResult
}

enum class BackupFailureReason {
    FILE_UNAVAILABLE,
    INVALID_BACKUP,
    UNSUPPORTED_FORMAT,
    UNSUPPORTED_DATABASE_VERSION,
    WRITE_FAILED,
    RESTORE_FAILED,
}
