package com.homiq.app.ui.util

import androidx.annotation.StringRes
import com.homiq.app.R
import com.homiq.app.data.model.BookingSource
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.data.model.DepositStatus
import com.homiq.app.data.model.ExpenseCategory
import com.homiq.app.data.model.PaymentMethod
import com.homiq.app.domain.BookingSaveIssue
import com.homiq.app.domain.BlockedDateSaveIssue
import com.homiq.app.domain.PropertySaveIssue
import com.homiq.app.domain.DepositActionIssue
import com.homiq.app.domain.ExpenseSaveIssue
import com.homiq.app.domain.PaymentSaveIssue

@StringRes
fun BookingSource.labelRes(): Int = when (this) {
    BookingSource.WHATSAPP -> R.string.source_whatsapp
    BookingSource.AIRBNB -> R.string.source_airbnb
    BookingSource.BOOKING_COM -> R.string.source_booking_com
    BookingSource.FACEBOOK -> R.string.source_facebook
    BookingSource.TIKTOK -> R.string.source_tiktok
    BookingSource.REPEAT_GUEST -> R.string.source_repeat_guest
    BookingSource.WALK_IN -> R.string.source_walk_in
    BookingSource.OTHER -> R.string.source_other
}

@StringRes
fun BookingStatus.labelRes(): Int = when (this) {
    BookingStatus.PENDING -> R.string.status_pending
    BookingStatus.CONFIRMED -> R.string.status_confirmed
    BookingStatus.CHECKED_IN -> R.string.status_checked_in
    BookingStatus.CHECKED_OUT -> R.string.status_checked_out
    BookingStatus.CANCELLED -> R.string.status_cancelled
}

@StringRes
fun BookingSaveIssue.messageRes(): Int = when (this) {
    BookingSaveIssue.PROPERTY_REQUIRED -> R.string.error_property_required
    BookingSaveIssue.PROPERTY_NOT_FOUND -> R.string.error_property_not_found
    BookingSaveIssue.GUEST_REQUIRED -> R.string.error_guest_required
    BookingSaveIssue.INVALID_DATES -> R.string.error_invalid_dates
    BookingSaveIssue.INVALID_AMOUNT -> R.string.error_invalid_amount
    BookingSaveIssue.BOOKING_OVERLAP -> R.string.error_booking_overlap
    BookingSaveIssue.BLOCKED_DATE_OVERLAP -> R.string.error_blocked_overlap
}

@StringRes
fun PropertySaveIssue.messageRes(): Int = when (this) {
    PropertySaveIssue.NAME_REQUIRED -> R.string.error_property_name_required
    PropertySaveIssue.INVALID_RATE -> R.string.error_invalid_rate
}


@StringRes
fun BlockedDateSaveIssue.messageRes(): Int = when (this) {
    BlockedDateSaveIssue.PROPERTY_REQUIRED ->
        R.string.error_property_required
    BlockedDateSaveIssue.PROPERTY_NOT_FOUND ->
        R.string.error_property_not_found
    BlockedDateSaveIssue.INVALID_DATES ->
        R.string.error_invalid_block_dates
    BlockedDateSaveIssue.BOOKING_OVERLAP ->
        R.string.error_block_booking_overlap
    BlockedDateSaveIssue.BLOCKED_DATE_OVERLAP ->
        R.string.error_block_overlap
}


@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.payment_cash
    PaymentMethod.BANK_TRANSFER -> R.string.payment_bank_transfer
    PaymentMethod.E_WALLET -> R.string.payment_e_wallet
    PaymentMethod.CARD -> R.string.payment_card
    PaymentMethod.PLATFORM -> R.string.payment_platform
    PaymentMethod.OTHER -> R.string.payment_other
}

@StringRes
fun DepositStatus.labelRes(): Int = when (this) {
    DepositStatus.NOT_REQUIRED -> R.string.deposit_not_required
    DepositStatus.PENDING -> R.string.deposit_pending
    DepositStatus.RECEIVED -> R.string.deposit_received
    DepositStatus.PARTIALLY_RETURNED -> R.string.deposit_partially_returned
    DepositStatus.RETURNED -> R.string.deposit_returned
    DepositStatus.RETAINED -> R.string.deposit_retained
}

@StringRes
fun PaymentSaveIssue.messageRes(): Int = when (this) {
    PaymentSaveIssue.BOOKING_NOT_FOUND ->
        R.string.error_booking_not_found
    PaymentSaveIssue.BOOKING_CANCELLED ->
        R.string.error_payment_cancelled_booking
    PaymentSaveIssue.INVALID_AMOUNT ->
        R.string.error_invalid_payment_amount
    PaymentSaveIssue.NO_OUTSTANDING_BALANCE ->
        R.string.error_no_outstanding
    PaymentSaveIssue.EXCEEDS_OUTSTANDING_BALANCE ->
        R.string.error_payment_exceeds_balance
}

@StringRes
fun DepositActionIssue.messageRes(): Int = when (this) {
    DepositActionIssue.BOOKING_NOT_FOUND ->
        R.string.error_booking_not_found
    DepositActionIssue.INVALID_AMOUNT ->
        R.string.error_invalid_deposit_amount
    DepositActionIssue.DEPOSIT_NOT_FOUND ->
        R.string.error_deposit_not_found
    DepositActionIssue.INVALID_STATE ->
        R.string.error_deposit_state
    DepositActionIssue.RETURN_EXCEEDS_REMAINING ->
        R.string.error_deposit_return_exceeds
}


@StringRes
fun ExpenseCategory.labelRes(): Int = when (this) {
    ExpenseCategory.CLEANING -> R.string.expense_cleaning
    ExpenseCategory.ELECTRICITY -> R.string.expense_electricity
    ExpenseCategory.WATER -> R.string.expense_water
    ExpenseCategory.INTERNET -> R.string.expense_internet
    ExpenseCategory.SUPPLIES -> R.string.expense_supplies
    ExpenseCategory.MAINTENANCE -> R.string.expense_maintenance
    ExpenseCategory.LAUNDRY -> R.string.expense_laundry
    ExpenseCategory.PLATFORM_FEE -> R.string.expense_platform_fee
    ExpenseCategory.OTHER -> R.string.expense_other
}

@StringRes
fun ExpenseSaveIssue.messageRes(): Int = when (this) {
    ExpenseSaveIssue.INVALID_AMOUNT ->
        R.string.error_invalid_expense_amount
    ExpenseSaveIssue.PROPERTY_NOT_FOUND ->
        R.string.error_expense_property_not_found
}
