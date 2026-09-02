package com.homiq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.homiq.app.data.local.entity.BlockedDateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDateDao {
    @Query(
        """
        SELECT * FROM blocked_dates
        WHERE isDeleted = 0
          AND startEpochDay < :rangeEndExclusive
          AND endEpochDay > :rangeStart
        ORDER BY startEpochDay ASC
        """,
    )
    fun observeInRange(
        rangeStart: Long,
        rangeEndExclusive: Long,
    ): Flow<List<BlockedDateEntity>>

    @Query(
        """
        SELECT * FROM blocked_dates
        WHERE isDeleted = 0
          AND propertyId = :propertyId
          AND id != :excludeBlockId
          AND startEpochDay < :endExclusive
          AND endEpochDay > :start
        ORDER BY startEpochDay ASC
        """,
    )
    suspend fun findOverlaps(
        propertyId: String,
        start: Long,
        endExclusive: Long,
        excludeBlockId: String,
    ): List<BlockedDateEntity>

    @Query("SELECT * FROM blocked_dates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BlockedDateEntity?

    @Query(
        """
        SELECT COUNT(*) FROM blocked_dates
        WHERE isDeleted = 0 AND propertyId = :propertyId
        """,
    )
    suspend fun countForProperty(propertyId: String): Int

    @Upsert
    suspend fun upsert(entity: BlockedDateEntity)

    @Query(
        """
        UPDATE blocked_dates
        SET isDeleted = 1,
            updatedAtEpochMillis = :updatedAt,
            revision = revision + 1
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
