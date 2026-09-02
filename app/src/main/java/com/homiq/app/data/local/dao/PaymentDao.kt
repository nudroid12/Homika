package com.homiq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.model.BookingBalanceRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query(
        """
        SELECT * FROM payments
        WHERE isDeleted = 0 AND bookingId = :bookingId
        ORDER BY paymentDateEpochDay ASC, createdAtEpochMillis ASC
        """,
    )
    fun observeForBooking(bookingId: String): Flow<List<PaymentEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amountSen), 0) FROM payments
        WHERE isDeleted = 0 AND bookingId = :bookingId
        """,
    )
    fun observeTotalPaidSen(bookingId: String): Flow<Long>


    @Query(
        """
        SELECT
            b.id AS bookingId,
            b.totalAmountSen AS totalAmountSen,
            COALESCE(SUM(
                CASE
                    WHEN p.isDeleted = 0 THEN p.amountSen
                    ELSE 0
                END
            ), 0) AS paidAmountSen
        FROM bookings b
        LEFT JOIN payments p
            ON p.bookingId = b.id
        WHERE b.isDeleted = 0
        GROUP BY b.id, b.totalAmountSen
        ORDER BY b.checkInEpochDay ASC
        """,
    )
    fun observeBookingBalances(): Flow<List<BookingBalanceRow>>


    @Query(
        """
        SELECT COALESCE(SUM(p.amountSen), 0)
        FROM payments p
        INNER JOIN bookings b
            ON b.id = p.bookingId
        WHERE p.isDeleted = 0
          AND b.isDeleted = 0
          AND p.paymentDateEpochDay >= :startEpochDay
          AND p.paymentDateEpochDay < :endEpochDayExclusive
        """,
    )
    fun observeRevenueInRangeSen(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<Long>

    @Query(
        """
        SELECT
            b.propertyId AS propertyId,
            COALESCE(SUM(p.amountSen), 0) AS amountSen
        FROM payments p
        INNER JOIN bookings b
            ON b.id = p.bookingId
        WHERE p.isDeleted = 0
          AND b.isDeleted = 0
          AND p.paymentDateEpochDay >= :startEpochDay
          AND p.paymentDateEpochDay < :endEpochDayExclusive
        GROUP BY b.propertyId
        """,
    )
    fun observeRevenueByPropertyInRange(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<List<com.homiq.app.data.local.model.PropertyAmountRow>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PaymentEntity?

    @Upsert
    suspend fun upsert(entity: PaymentEntity)

    @Query(
        """
        UPDATE payments
        SET isDeleted = 1,
            updatedAtEpochMillis = :updatedAt,
            revision = revision + 1
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
