package com.homiq.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.homiq.app.data.model.PaymentMethod
import java.util.UUID

@Entity(
    tableName = "payments",
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
        Index(value = ["bookingId"]),
        Index(value = ["paymentDateEpochDay"]),
        Index(value = ["isDeleted"]),
    ],
)
data class PaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookingId: String,
    val amountSen: Long,
    val paymentDateEpochDay: Long,
    val method: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val notes: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val revision: Long = 0L,
    val isDeleted: Boolean = false,
)
