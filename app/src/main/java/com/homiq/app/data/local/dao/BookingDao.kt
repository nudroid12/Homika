package com.homiq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.homiq.app.data.local.entity.BookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query(
        """
        SELECT * FROM bookings
        WHERE isDeleted = 0
        ORDER BY checkInEpochDay ASC, createdAtEpochMillis ASC
        """,
    )
    fun observeAll(): Flow<List<BookingEntity>>

    @Query(
        """
        SELECT * FROM bookings
        WHERE isDeleted = 0 AND propertyId = :propertyId
        ORDER BY checkInEpochDay ASC
        """,
    )
    fun observeByProperty(propertyId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookingEntity?

    @Query(
        """
        SELECT * FROM bookings
        WHERE isDeleted = 0
          AND status != 'CANCELLED'
          AND checkInEpochDay < :rangeEndExclusive
          AND checkOutEpochDay > :rangeStart
        ORDER BY checkInEpochDay ASC
        """,
    )
    fun observeInRange(
        rangeStart: Long,
        rangeEndExclusive: Long,
    ): Flow<List<BookingEntity>>

    @Query(
        """
        SELECT * FROM bookings
        WHERE isDeleted = 0
          AND propertyId = :propertyId
          AND status != 'CANCELLED'
          AND id != :excludeBookingId
          AND checkInEpochDay < :checkOutExclusive
          AND checkOutEpochDay > :checkIn
        ORDER BY checkInEpochDay ASC
        """,
    )
    suspend fun findOverlaps(
        propertyId: String,
        checkIn: Long,
        checkOutExclusive: Long,
        excludeBookingId: String,
    ): List<BookingEntity>

    @Query(
        """
        SELECT COUNT(*) FROM bookings
        WHERE isDeleted = 0 AND propertyId = :propertyId
        """,
    )
    suspend fun countForProperty(propertyId: String): Int

    @Upsert
    suspend fun upsert(entity: BookingEntity)

    @Query(
        """
        UPDATE bookings
        SET isDeleted = 1,
            updatedAtEpochMillis = :updatedAt,
            revision = revision + 1
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
