package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.model.BookingBalanceRow
import com.homiq.app.data.repository.DepositRepository
import com.homiq.app.data.repository.PaymentRepository
import com.homiq.app.domain.DepositActionResult
import com.homiq.app.domain.DepositManager
import com.homiq.app.domain.PaymentDraft
import com.homiq.app.domain.PaymentManager
import com.homiq.app.domain.PaymentSaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(
    private val payments: PaymentRepository,
    private val deposits: DepositRepository,
    private val paymentManager: PaymentManager,
    private val depositManager: DepositManager,
) : ViewModel() {
    val bookingBalances: StateFlow<List<BookingBalanceRow>> =
        payments.observeBookingBalances().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun paymentsFor(
        bookingId: String,
    ): Flow<List<PaymentEntity>> =
        payments.observeForBooking(bookingId)

    fun totalPaidFor(
        bookingId: String,
    ): Flow<Long> =
        payments.observeTotalPaidSen(bookingId)

    fun depositFor(
        bookingId: String,
    ): Flow<DepositEntity?> =
        deposits.observeForBooking(bookingId)

    suspend fun recordPayment(
        draft: PaymentDraft,
    ): PaymentSaveResult =
        paymentManager.record(draft)

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            payments.delete(paymentId)
        }
    }

    suspend fun setDepositRequired(
        bookingId: String,
        amountSen: Long,
        notes: String,
    ): DepositActionResult =
        depositManager.setRequired(
            bookingId = bookingId,
            amountSen = amountSen,
            notes = notes,
        )

    suspend fun markDepositNotRequired(
        bookingId: String,
    ): DepositActionResult =
        depositManager.markNotRequired(bookingId)

    suspend fun markDepositReceived(
        bookingId: String,
        receivedEpochDay: Long,
    ): DepositActionResult =
        depositManager.markReceived(
            bookingId = bookingId,
            receivedEpochDay = receivedEpochDay,
        )

    suspend fun returnDeposit(
        bookingId: String,
        amountSen: Long,
        returnedEpochDay: Long,
    ): DepositActionResult =
        depositManager.recordReturn(
            bookingId = bookingId,
            returnAmountSen = amountSen,
            returnedEpochDay = returnedEpochDay,
        )

    suspend fun retainDeposit(
        bookingId: String,
    ): DepositActionResult =
        depositManager.retainRemaining(bookingId)
}
