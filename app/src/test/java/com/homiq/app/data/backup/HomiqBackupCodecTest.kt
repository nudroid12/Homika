package com.homiq.app.data.backup

import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.model.BookingSource
import com.homiq.app.data.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class HomiqBackupCodecTest {
    @Test
    fun backupRoundTripPreservesIdentityAndMoney() {
        val property =
            PropertyEntity(
                id = "property-1",
                name = "HOMIQ Test",
                defaultNightlyRateSen = 12_050L,
            )
        val booking =
            BookingEntity(
                id = "booking-1",
                propertyId = property.id,
                guestName = "Guest",
                checkInEpochDay = 10L,
                checkOutEpochDay = 12L,
                source = BookingSource.WHATSAPP,
                totalAmountSen = 25_000L,
            )
        val payment =
            PaymentEntity(
                id = "payment-1",
                bookingId = booking.id,
                amountSen = 10_000L,
                paymentDateEpochDay = 10L,
                method =
                    PaymentMethod.BANK_TRANSFER,
            )

        val original =
            HomiqBackupSnapshot(
                createdAtEpochMillis = 1234L,
                properties = listOf(property),
                bookings = listOf(booking),
                payments = listOf(payment),
                deposits = emptyList(),
                expenses = emptyList(),
                blockedDates = emptyList(),
            )

        val decoded =
            HomiqBackupCodec.decode(
                HomiqBackupCodec.encode(original),
            )

        assertEquals(
            property.id,
            decoded.properties.single().id,
        )
        assertEquals(
            25_000L,
            decoded.bookings
                .single()
                .totalAmountSen,
        )
        assertEquals(
            10_000L,
            decoded.payments
                .single()
                .amountSen,
        )
    }
}
