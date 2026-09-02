package com.homiq.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.homiq.app.data.model.DepositStatus
import java.util.UUID

@Entity(
    tableName = "deposits",
    foreignKeys = [
        ForeignKey(
            entity = BookingEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookingId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookingId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["isDeleted"]),
    ],
)
data class DepositEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookingId: String,
    val amountSen: Long = 0L,
    val status: DepositStatus = DepositStatus.NOT_REQUIRED,
    val receivedAtEpochDay: Long? = null,
    val returnedAmountSen: Long = 0L,
    val returnedAtEpochDay: Long? = null,
    val notes: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val revision: Long = 0L,
    val isDeleted: Boolean = false,
)
