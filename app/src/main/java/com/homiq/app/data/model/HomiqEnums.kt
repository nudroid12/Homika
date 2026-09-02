package com.homiq.app.data.model

enum class BookingSource {
    WHATSAPP,
    AIRBNB,
    BOOKING_COM,
    FACEBOOK,
    TIKTOK,
    REPEAT_GUEST,
    WALK_IN,
    OTHER,
}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
}

enum class PaymentMethod {
    CASH,
    BANK_TRANSFER,
    E_WALLET,
    CARD,
    PLATFORM,
    OTHER,
}

enum class DepositStatus {
    NOT_REQUIRED,
    PENDING,
    RECEIVED,
    PARTIALLY_RETURNED,
    RETURNED,
    RETAINED,
}

enum class ExpenseCategory {
    CLEANING,
    ELECTRICITY,
    WATER,
    INTERNET,
    SUPPLIES,
    MAINTENANCE,
    LAUNDRY,
    PLATFORM_FEE,
    OTHER,
}
