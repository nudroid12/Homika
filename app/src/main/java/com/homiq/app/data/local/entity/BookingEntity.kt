package com.homiq.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.homiq.app.data.model.BookingSource
import com.homiq.app.data.model.BookingStatus
import java.util.UUID

@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["propertyId"]),
        Index(value = ["propertyId", "checkInEpochDay", "checkOutEpochDay"]),
        Index(value = ["status"]),
        Index(value = ["guestPhone"]),
        Index(value = ["isDeleted"]),
    ],
)
data class BookingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    @ColumnInfo(defaultValue = "''")
    val bookingReference: String = "",
    val guestName: String,
    val guestPhone: String? = null,
    val checkInEpochDay: Long,
    val checkOutEpochDay: Long,
    val source: BookingSource = BookingSource.WHATSAPP,
    // Legacy column name kept for DB/backup compatibility. Semantically: Amount Received.
    val totalAmountSen: Long = 0L,
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val notes: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val revision: Long = 0L,
    val isDeleted: Boolean = false,
)
