package com.homiq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.homiq.app.data.local.entity.DepositEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositDao {
    @Query(
        """
        SELECT * FROM deposits
        WHERE isDeleted = 0 AND bookingId = :bookingId
        LIMIT 1
        """,
    )
    fun observeForBooking(bookingId: String): Flow<DepositEntity?>

    @Query("SELECT * FROM deposits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DepositEntity?

    @Upsert
    suspend fun upsert(entity: DepositEntity)

    @Query(
        """
        UPDATE deposits
        SET isDeleted = 1,
            updatedAtEpochMillis = :updatedAt,
            revision = revision + 1
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
