package com.homiq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.homiq.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(
        """
        SELECT * FROM expenses
        WHERE isDeleted = 0
        ORDER BY expenseDateEpochDay DESC, createdAtEpochMillis DESC
        """,
    )
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE isDeleted = 0
          AND expenseDateEpochDay >= :startEpochDay
          AND expenseDateEpochDay < :endEpochDayExclusive
        ORDER BY expenseDateEpochDay DESC
        """,
    )
    fun observeInRange(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amountSen), 0) FROM expenses
        WHERE isDeleted = 0
          AND expenseDateEpochDay >= :startEpochDay
          AND expenseDateEpochDay < :endEpochDayExclusive
        """,
    )
    fun observeTotalInRangeSen(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<Long>


    @Query(
        """
        SELECT
            propertyId AS propertyId,
            COALESCE(SUM(amountSen), 0) AS amountSen
        FROM expenses
        WHERE isDeleted = 0
          AND expenseDateEpochDay >= :startEpochDay
          AND expenseDateEpochDay < :endEpochDayExclusive
        GROUP BY propertyId
        """,
    )
    fun observeByPropertyInRange(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<List<com.homiq.app.data.local.model.PropertyAmountRow>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExpenseEntity?

    @Query(
        """
        SELECT COUNT(*) FROM expenses
        WHERE isDeleted = 0 AND propertyId = :propertyId
        """,
    )
    suspend fun countForProperty(propertyId: String): Int

    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Query(
        """
        UPDATE expenses
        SET isDeleted = 1,
            updatedAtEpochMillis = :updatedAt,
            revision = revision + 1
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
