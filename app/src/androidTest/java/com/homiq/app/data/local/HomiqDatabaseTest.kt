package com.homiq.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.model.PaymentMethod
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomiqDatabaseTest {
    private lateinit var database: HomiqDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            HomiqDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bookingOverlapAndPaymentTotal_areCalculatedFromLocalData() = runBlocking {
        val property = PropertyEntity(name = "Test Homestay")
        database.propertyDao().upsert(property)

        val checkIn = LocalDate.of(2026, 9, 10).toEpochDay()
        val checkOut = LocalDate.of(2026, 9, 12).toEpochDay()

        val booking = BookingEntity(
            propertyId = property.id,
            guestName = "Test Guest",
            checkInEpochDay = checkIn,
            checkOutEpochDay = checkOut,
            totalAmountSen = 35_000L,
        )
        database.bookingDao().upsert(booking)

        val overlaps = database.bookingDao().findOverlaps(
            propertyId = property.id,
            checkIn = LocalDate.of(2026, 9, 11).toEpochDay(),
            checkOutExclusive = LocalDate.of(2026, 9, 13).toEpochDay(),
        )
        assertEquals(1, overlaps.size)

        val touchesCheckoutOnly = database.bookingDao().findOverlaps(
            propertyId = property.id,
            checkIn = checkOut,
            checkOutExclusive = LocalDate.of(2026, 9, 13).toEpochDay(),
        )
        assertTrue(touchesCheckoutOnly.isEmpty())

        database.paymentDao().upsert(
            PaymentEntity(
                bookingId = booking.id,
                amountSen = 10_000L,
                paymentDateEpochDay = checkIn,
                method = PaymentMethod.BANK_TRANSFER,
            ),
        )
        database.paymentDao().upsert(
            PaymentEntity(
                bookingId = booking.id,
                amountSen = 5_000L,
                paymentDateEpochDay = checkIn,
                method = PaymentMethod.CASH,
            ),
        )

        assertEquals(
            15_000L,
            database.paymentDao().observeTotalPaidSen(booking.id).first(),
        )
    }
}
