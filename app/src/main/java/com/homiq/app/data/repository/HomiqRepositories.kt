package com.homiq.app.data.repository

import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.local.model.BookingBalanceRow
import com.homiq.app.data.local.model.PropertyAmountRow
import kotlinx.coroutines.flow.Flow

interface PropertyRepository {
    fun observeAll(): Flow<List<PropertyEntity>>
    fun observeActive(): Flow<List<PropertyEntity>>
    suspend fun getById(id: String): PropertyEntity?
    suspend fun save(entity: PropertyEntity)
    suspend fun delete(id: String)
}

interface BookingRepository {
    fun observeAll(): Flow<List<BookingEntity>>
    fun observeByProperty(propertyId: String): Flow<List<BookingEntity>>
    fun observeInRange(
        rangeStart: Long,
        rangeEndExclusive: Long,
    ): Flow<List<BookingEntity>>

    suspend fun getById(id: String): BookingEntity?
    suspend fun findOverlaps(
        propertyId: String,
        checkIn: Long,
        checkOutExclusive: Long,
        excludeBookingId: String = "",
    ): List<BookingEntity>

    suspend fun countForProperty(propertyId: String): Int
    suspend fun save(entity: BookingEntity)
    suspend fun delete(id: String)
}

interface PaymentRepository {
    fun observeRevenueInRangeSen(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<Long>

    fun observeRevenueByPropertyInRange(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<List<PropertyAmountRow>>

    fun observeBookingBalances(): Flow<List<BookingBalanceRow>>
    fun observeForBooking(bookingId: String): Flow<List<PaymentEntity>>
    fun observeTotalPaidSen(bookingId: String): Flow<Long>
    suspend fun getById(id: String): PaymentEntity?
    suspend fun save(entity: PaymentEntity)
    suspend fun delete(id: String)
}

interface DepositRepository {
    fun observeForBooking(bookingId: String): Flow<DepositEntity?>
    suspend fun getById(id: String): DepositEntity?
    suspend fun save(entity: DepositEntity)
    suspend fun delete(id: String)
}

interface ExpenseRepository {
    fun observeByPropertyInRange(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<List<PropertyAmountRow>>

    fun observeAll(): Flow<List<ExpenseEntity>>
    fun observeInRange(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<List<ExpenseEntity>>

    fun observeTotalInRangeSen(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<Long>

    suspend fun getById(id: String): ExpenseEntity?
    suspend fun countForProperty(propertyId: String): Int
    suspend fun save(entity: ExpenseEntity)
    suspend fun delete(id: String)
}

interface BlockedDateRepository {
    fun observeInRange(
        rangeStart: Long,
        rangeEndExclusive: Long,
    ): Flow<List<BlockedDateEntity>>

    suspend fun findOverlaps(
        propertyId: String,
        start: Long,
        endExclusive: Long,
        excludeBlockId: String = "",
    ): List<BlockedDateEntity>

    suspend fun getById(id: String): BlockedDateEntity?
    suspend fun countForProperty(propertyId: String): Int
    suspend fun save(entity: BlockedDateEntity)
    suspend fun delete(id: String)
}
