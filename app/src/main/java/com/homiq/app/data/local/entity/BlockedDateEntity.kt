package com.homiq.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "blocked_dates",
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
        Index(value = ["propertyId", "startEpochDay", "endEpochDay"]),
        Index(value = ["isDeleted"]),
    ],
)
data class BlockedDateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val reason: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val revision: Long = 0L,
    val isDeleted: Boolean = false,
)
