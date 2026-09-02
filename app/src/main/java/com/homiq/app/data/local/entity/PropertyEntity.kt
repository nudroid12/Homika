package com.homiq.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "properties",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isActive", "isDeleted"]),
    ],
)
data class PropertyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @ColumnInfo(defaultValue = "''")
    val bookingCode: String = "",
    val address: String? = null,
    val notes: String? = null,
    val defaultNightlyRateSen: Long = 0L,
    val isActive: Boolean = true,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val revision: Long = 0L,
    val isDeleted: Boolean = false,
)
