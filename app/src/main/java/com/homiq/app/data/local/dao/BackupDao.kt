package com.homiq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity

@Dao
interface BackupDao {
    @Query("SELECT * FROM properties ORDER BY createdAtEpochMillis ASC")
    suspend fun allProperties(): List<PropertyEntity>

    @Query("SELECT * FROM bookings ORDER BY createdAtEpochMillis ASC")
    suspend fun allBookings(): List<BookingEntity>

    @Query("SELECT * FROM payments ORDER BY createdAtEpochMillis ASC")
    suspend fun allPayments(): List<PaymentEntity>

    @Query("SELECT * FROM deposits ORDER BY createdAtEpochMillis ASC")
    suspend fun allDeposits(): List<DepositEntity>

    @Query("SELECT * FROM expenses ORDER BY createdAtEpochMillis ASC")
    suspend fun allExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM blocked_dates ORDER BY createdAtEpochMillis ASC")
    suspend fun allBlockedDates(): List<BlockedDateEntity>

    @Query("DELETE FROM payments")
    suspend fun clearPayments()

    @Query("DELETE FROM deposits")
    suspend fun clearDeposits()

    @Query("DELETE FROM blocked_dates")
    suspend fun clearBlockedDates()

    @Query("DELETE FROM bookings")
    suspend fun clearBookings()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM properties")
    suspend fun clearProperties()

    @Upsert
    suspend fun upsertProperties(items: List<PropertyEntity>)

    @Upsert
    suspend fun upsertBookings(items: List<BookingEntity>)

    @Upsert
    suspend fun upsertPayments(items: List<PaymentEntity>)

    @Upsert
    suspend fun upsertDeposits(items: List<DepositEntity>)

    @Upsert
    suspend fun upsertExpenses(items: List<ExpenseEntity>)

    @Upsert
    suspend fun upsertBlockedDates(items: List<BlockedDateEntity>)
}
