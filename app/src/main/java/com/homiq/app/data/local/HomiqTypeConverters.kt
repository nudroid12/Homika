package com.homiq.app.data.local

import androidx.room.TypeConverter
import com.homiq.app.data.model.BookingSource
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.data.model.DepositStatus
import com.homiq.app.data.model.ExpenseCategory
import com.homiq.app.data.model.PaymentMethod

class HomiqTypeConverters {
    @TypeConverter
    fun fromBookingSource(value: BookingSource): String = value.name

    @TypeConverter
    fun toBookingSource(value: String): BookingSource =
        enumValueOf(value)

    @TypeConverter
    fun fromBookingStatus(value: BookingStatus): String = value.name

    @TypeConverter
    fun toBookingStatus(value: String): BookingStatus =
        enumValueOf(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        enumValueOf(value)

    @TypeConverter
    fun fromDepositStatus(value: DepositStatus): String = value.name

    @TypeConverter
    fun toDepositStatus(value: String): DepositStatus =
        enumValueOf(value)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory =
        enumValueOf(value)
}
