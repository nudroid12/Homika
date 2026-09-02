package com.homiq.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.model.PaymentMethod
import com.homiq.app.domain.PaymentDraft
import com.homiq.app.domain.PaymentSaveResult
import com.homiq.app.ui.components.DateField
import com.homiq.app.ui.components.InfoCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.components.SelectionField
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.util.labelRes
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.util.parseRinggitToSen
import com.homiq.app.ui.viewmodel.BookingViewModel
import com.homiq.app.ui.viewmodel.FinanceViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun PaymentFormScreen(
    bookingId: String,
    bookingViewModel: BookingViewModel,
    financeViewModel: FinanceViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookings by bookingViewModel.bookingList
        .collectAsStateWithLifecycle()
    val booking = bookings.firstOrNull { it.id == bookingId }

    val totalPaidFlow = remember(bookingId) {
        financeViewModel.totalPaidFor(bookingId)
    }
    val totalPaid by totalPaidFlow.collectAsStateWithLifecycle(
        initialValue = 0L,
    )

    val locale = LocalConfiguration.current.locales[0]
    val outstanding = (
        (booking?.totalAmountSen ?: 0L) - totalPaid
    ).coerceAtLeast(0L)

    var amount by remember(bookingId, outstanding) {
        mutableStateOf(
            if (outstanding > 0L) {
                java.math.BigDecimal.valueOf(
                    outstanding,
                    2,
                ).stripTrailingZeros().toPlainString()
            } else {
                ""
            },
        )
    }
    var paymentDate by remember {
        mutableLongStateOf(LocalDate.now().toEpochDay())
    }
    var method by remember {
        mutableStateOf(PaymentMethod.BANK_TRANSFER)
    }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember {
        mutableStateOf<Int?>(null)
    }

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 18.dp,
            end = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.record_payment),
                subtitle = booking?.guestName.orEmpty(),
            )
        }

        item {
            InfoCard(
                title = stringResource(R.string.payment_balance_title),
                body = stringResource(
                    R.string.payment_balance_body,
                    formatSenAsRinggit(
                        booking?.totalAmountSen ?: 0L,
                        locale,
                    ),
                    formatSenAsRinggit(
                        totalPaid,
                        locale,
                    ),
                    formatSenAsRinggit(
                        outstanding,
                        locale,
                    ),
                ),
            )
        }

        if (booking != null && outstanding > 0L) {
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char ->
                            char.isDigit() || char == '.'
                        }
                        errorMessage = null
                    },
                    label = {
                        Text(stringResource(R.string.payment_amount))
                    },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                DateField(
                    label = stringResource(R.string.payment_date),
                    epochDay = paymentDate,
                    onDateSelected = {
                        paymentDate = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                SelectionField(
                    label = stringResource(R.string.payment_method),
                    selectedText = stringResource(method.labelRes()),
                    options = PaymentMethod.entries,
                    optionText = {
                        stringResource(it.labelRes())
                    },
                    onSelected = {
                        method = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            errorMessage?.let { message ->
                item {
                    Text(
                        text = stringResource(message),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val amountSen = parseRinggitToSen(amount)
                        if (amountSen == null) {
                            errorMessage =
                                R.string.error_invalid_payment_amount
                            return@Button
                        }

                        scope.launch {
                            when (
                                val result =
                                    financeViewModel.recordPayment(
                                        PaymentDraft(
                                            bookingId = bookingId,
                                            amountSen = amountSen,
                                            paymentDateEpochDay =
                                                paymentDate,
                                            method = method,
                                            notes = notes,
                                        ),
                                    )
                            ) {
                                is PaymentSaveResult.Success ->
                                    onSaved()

                                is PaymentSaveResult.Failure ->
                                    errorMessage =
                                        result.issue.messageRes()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save_payment))
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(
                        R.string.no_outstanding_balance,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
