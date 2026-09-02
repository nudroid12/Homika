package com.homiq.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.homiq.app.data.model.ExpenseCategory
import java.util.UUID

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["propertyId"]),
        Index(value = ["expenseDateEpochDay"]),
        Index(value = ["category"]),
        Index(value = ["isDeleted"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String? = null,
    val amountSen: Long,
    val expenseDateEpochDay: Long,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val description: String? = null,
    val notes: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val revision: Long = 0L,
    val isDeleted: Boolean = false,
)
