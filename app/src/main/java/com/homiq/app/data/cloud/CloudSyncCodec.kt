package com.homiq.app.data.cloud

import com.homiq.app.data.backup.HomiqBackupCodec
import com.homiq.app.data.backup.HomiqBackupSnapshot
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity

object CloudSyncCodec {
    fun recordsFromSnapshot(
        snapshot: HomiqBackupSnapshot,
    ): List<CloudSyncLocalRecord> = buildList {
        snapshot.properties.forEach { entity ->
            add(
                record(
                    type = CloudSyncEntityType.PROPERTY,
                    entityId = entity.id,
                    revision = entity.revision,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                    isDeleted = entity.isDeleted,
                    snapshot = miniSnapshot(properties = listOf(entity)),
                ),
            )
        }
        snapshot.bookings.forEach { entity ->
            add(
                record(
                    type = CloudSyncEntityType.BOOKING,
                    entityId = entity.id,
                    revision = entity.revision,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                    isDeleted = entity.isDeleted,
                    snapshot = miniSnapshot(bookings = listOf(entity)),
                ),
            )
        }
        snapshot.payments.forEach { entity ->
            add(
                record(
                    type = CloudSyncEntityType.PAYMENT,
                    entityId = entity.id,
                    revision = entity.revision,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                    isDeleted = entity.isDeleted,
                    snapshot = miniSnapshot(payments = listOf(entity)),
                ),
            )
        }
        snapshot.deposits.forEach { entity ->
            add(
                record(
                    type = CloudSyncEntityType.DEPOSIT,
                    entityId = entity.id,
                    revision = entity.revision,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                    isDeleted = entity.isDeleted,
                    snapshot = miniSnapshot(deposits = listOf(entity)),
                ),
            )
        }
        snapshot.expenses.forEach { entity ->
            add(
                record(
                    type = CloudSyncEntityType.EXPENSE,
                    entityId = entity.id,
                    revision = entity.revision,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                    isDeleted = entity.isDeleted,
                    snapshot = miniSnapshot(expenses = listOf(entity)),
                ),
            )
        }
        snapshot.blockedDates.forEach { entity ->
            add(
                record(
                    type = CloudSyncEntityType.BLOCKED_DATE,
                    entityId = entity.id,
                    revision = entity.revision,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                    isDeleted = entity.isDeleted,
                    snapshot = miniSnapshot(blockedDates = listOf(entity)),
                ),
            )
        }
    }

    fun decode(
        type: CloudSyncEntityType,
        rawJson: String,
    ): DecodedCloudSyncEntity {
        val snapshot = HomiqBackupCodec.decode(rawJson)
        return when (type) {
            CloudSyncEntityType.PROPERTY ->
                DecodedCloudSyncEntity.Property(single(snapshot.properties, type))
            CloudSyncEntityType.BOOKING ->
                DecodedCloudSyncEntity.Booking(single(snapshot.bookings, type))
            CloudSyncEntityType.PAYMENT ->
                DecodedCloudSyncEntity.Payment(single(snapshot.payments, type))
            CloudSyncEntityType.DEPOSIT ->
                DecodedCloudSyncEntity.Deposit(single(snapshot.deposits, type))
            CloudSyncEntityType.EXPENSE ->
                DecodedCloudSyncEntity.Expense(single(snapshot.expenses, type))
            CloudSyncEntityType.BLOCKED_DATE ->
                DecodedCloudSyncEntity.BlockedDate(single(snapshot.blockedDates, type))
        }
    }

    private fun record(
        type: CloudSyncEntityType,
        entityId: String,
        revision: Long,
        updatedAtEpochMillis: Long,
        isDeleted: Boolean,
        snapshot: HomiqBackupSnapshot,
    ): CloudSyncLocalRecord =
        CloudSyncLocalRecord(
            type = type,
            entityId = entityId,
            revision = revision,
            updatedAtEpochMillis = updatedAtEpochMillis,
            isDeleted = isDeleted,
            rawJson = HomiqBackupCodec.encode(snapshot),
        )

    private fun miniSnapshot(
        properties: List<PropertyEntity> = emptyList(),
        bookings: List<BookingEntity> = emptyList(),
        payments: List<PaymentEntity> = emptyList(),
        deposits: List<DepositEntity> = emptyList(),
        expenses: List<ExpenseEntity> = emptyList(),
        blockedDates: List<BlockedDateEntity> = emptyList(),
    ): HomiqBackupSnapshot =
        HomiqBackupSnapshot(
            createdAtEpochMillis = System.currentTimeMillis(),
            properties = properties,
            bookings = bookings,
            payments = payments,
            deposits = deposits,
            expenses = expenses,
            blockedDates = blockedDates,
        )

    private fun <T> single(
        values: List<T>,
        type: CloudSyncEntityType,
    ): T {
        require(values.size == 1) { "Invalid ${type.wireName} sync payload." }
        return values.single()
    }
}

sealed interface DecodedCloudSyncEntity {
    val entityId: String
    val revision: Long
    val updatedAtEpochMillis: Long
    val isDeleted: Boolean

    data class Property(val entity: PropertyEntity) : DecodedCloudSyncEntity {
        override val entityId: String get() = entity.id
        override val revision: Long get() = entity.revision
        override val updatedAtEpochMillis: Long get() = entity.updatedAtEpochMillis
        override val isDeleted: Boolean get() = entity.isDeleted
    }

    data class Booking(val entity: BookingEntity) : DecodedCloudSyncEntity {
        override val entityId: String get() = entity.id
        override val revision: Long get() = entity.revision
        override val updatedAtEpochMillis: Long get() = entity.updatedAtEpochMillis
        override val isDeleted: Boolean get() = entity.isDeleted
    }

    data class Payment(val entity: PaymentEntity) : DecodedCloudSyncEntity {
        override val entityId: String get() = entity.id
        override val revision: Long get() = entity.revision
        override val updatedAtEpochMillis: Long get() = entity.updatedAtEpochMillis
        override val isDeleted: Boolean get() = entity.isDeleted
    }

    data class Deposit(val entity: DepositEntity) : DecodedCloudSyncEntity {
        override val entityId: String get() = entity.id
        override val revision: Long get() = entity.revision
        override val updatedAtEpochMillis: Long get() = entity.updatedAtEpochMillis
        override val isDeleted: Boolean get() = entity.isDeleted
    }

    data class Expense(val entity: ExpenseEntity) : DecodedCloudSyncEntity {
        override val entityId: String get() = entity.id
        override val revision: Long get() = entity.revision
        override val updatedAtEpochMillis: Long get() = entity.updatedAtEpochMillis
        override val isDeleted: Boolean get() = entity.isDeleted
    }

    data class BlockedDate(val entity: BlockedDateEntity) : DecodedCloudSyncEntity {
        override val entityId: String get() = entity.id
        override val revision: Long get() = entity.revision
        override val updatedAtEpochMillis: Long get() = entity.updatedAtEpochMillis
        override val isDeleted: Boolean get() = entity.isDeleted
    }
}
