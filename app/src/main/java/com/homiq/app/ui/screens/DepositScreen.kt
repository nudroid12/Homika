package com.homiq.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.homiq.app.data.model.DepositStatus
import com.homiq.app.domain.DepositActionResult
import com.homiq.app.domain.DepositRules
import com.homiq.app.ui.components.DateField
import com.homiq.app.ui.components.InfoCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.util.labelRes
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.util.parseRinggitToSen
import com.homiq.app.ui.viewmodel.BookingViewModel
import com.homiq.app.ui.viewmodel.FinanceViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun DepositScreen(
    bookingId: String,
    bookingViewModel: BookingViewModel,
    financeViewModel: FinanceViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookings by bookingViewModel.bookingList
        .collectAsStateWithLifecycle()
    val booking = bookings.firstOrNull { it.id == bookingId }

    val depositFlow = remember(bookingId) {
        financeViewModel.depositFor(bookingId)
    }
    val deposit by depositFlow.collectAsStateWithLifecycle(
        initialValue = null,
    )

    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()

    var amount by remember(deposit?.id) {
        mutableStateOf(
            deposit
                ?.takeIf {
                    it.status in setOf(
                        DepositStatus.NOT_REQUIRED,
                        DepositStatus.PENDING,
                    )
                }
                ?.amountSen
                ?.takeIf { it > 0L }
                ?.let {
                    java.math.BigDecimal.valueOf(
                        it,
                        2,
                    ).stripTrailingZeros().toPlainString()
                }
                .orEmpty(),
        )
    }
    var notes by remember(deposit?.id) {
        mutableStateOf(deposit?.notes.orEmpty())
    }
    var returnAmount by remember(deposit?.id) {
        mutableStateOf("")
    }
    var actionDate by remember {
        mutableLongStateOf(LocalDate.now().toEpochDay())
    }
    var errorMessage by remember {
        mutableStateOf<Int?>(null)
    }

    fun handle(result: DepositActionResult) {
        when (result) {
            DepositActionResult.Success -> {
                errorMessage = null
            }
            is DepositActionResult.Failure -> {
                errorMessage = result.issue.messageRes()
            }
        }
    }

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
                title = stringResource(R.string.security_deposit),
                subtitle = booking?.guestName.orEmpty(),
            )
        }

        item {
            InfoCard(
                title = stringResource(
                    R.string.deposit_separate_title,
                ),
                body = stringResource(
                    R.string.homika_deposit_separate_body_v2,
                ),
            )
        }

        val current = deposit
        if (
            current == null ||
            current.status in setOf(
                DepositStatus.NOT_REQUIRED,
                DepositStatus.PENDING,
            )
        ) {
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
                        Text(
                            stringResource(
                                R.string.deposit_required_amount,
                            ),
                        )
                    },
                    prefix = { Text("RM ") },
                    singleLine = true,
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

            if (current?.status == DepositStatus.PENDING) {
                item {
                    Text(
                        text = stringResource(
                            R.string.deposit_status_value,
                            stringResource(current.status.labelRes()),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item {
                    DateField(
                        label = stringResource(
                            R.string.deposit_received_date,
                        ),
                        epochDay = actionDate,
                        onDateSelected = {
                            actionDate = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Button(
                        onClick = {
                            scope.launch {
                                handle(
                                    financeViewModel.markDepositReceived(
                                        bookingId = bookingId,
                                        receivedEpochDay = actionDate,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                R.string.mark_deposit_received,
                            ),
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val amountSen = parseRinggitToSen(amount)
                        if (amountSen == null) {
                            errorMessage =
                                R.string.error_invalid_deposit_amount
                            return@Button
                        }

                        scope.launch {
                            handle(
                                financeViewModel.setDepositRequired(
                                    bookingId = bookingId,
                                    amountSen = amountSen,
                                    notes = notes,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (current?.status ==
                                DepositStatus.PENDING
                            ) {
                                R.string.update_deposit_requirement
                            } else {
                                R.string.set_deposit_required
                            },
                        ),
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            handle(
                                financeViewModel
                                    .markDepositNotRequired(
                                        bookingId,
                                    ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.mark_deposit_not_required,
                        ),
                    )
                }
            }
        } else {
            val remaining = DepositRules.remainingSen(
                depositAmountSen = current.amountSen,
                returnedAmountSen = current.returnedAmountSen,
            )

            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                current.status.labelRes(),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(
                                R.string.deposit_total_value,
                                formatSenAsRinggit(
                                    current.amountSen,
                                    locale,
                                ),
                            ),
                        )
                        Text(
                            text = stringResource(
                                R.string.deposit_returned_value,
                                formatSenAsRinggit(
                                    current.returnedAmountSen,
                                    locale,
                                ),
                            ),
                        )
                        Text(
                            text = stringResource(
                                R.string.deposit_remaining_value,
                                formatSenAsRinggit(
                                    remaining,
                                    locale,
                                ),
                            ),
                        )
                    }
                }
            }

            if (
                current.status in setOf(
                    DepositStatus.RECEIVED,
                    DepositStatus.PARTIALLY_RETURNED,
                )
            ) {
                item {
                    OutlinedTextField(
                        value = returnAmount,
                        onValueChange = {
                            returnAmount = it.filter { char ->
                                char.isDigit() || char == '.'
                            }
                            errorMessage = null
                        },
                        label = {
                            Text(
                                stringResource(
                                    R.string.deposit_return_amount,
                                ),
                            )
                        },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    DateField(
                        label = stringResource(
                            R.string.deposit_return_date,
                        ),
                        epochDay = actionDate,
                        onDateSelected = {
                            actionDate = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                val returnSen =
                                    parseRinggitToSen(
                                        returnAmount,
                                    )
                                if (returnSen == null) {
                                    errorMessage =
                                        R.string.error_invalid_deposit_amount
                                    return@Button
                                }

                                scope.launch {
                                    handle(
                                        financeViewModel.returnDeposit(
                                            bookingId = bookingId,
                                            amountSen = returnSen,
                                            returnedEpochDay =
                                                actionDate,
                                        ),
                                    )
                                    returnAmount = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(
                                    R.string.record_deposit_return,
                                ),
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    handle(
                                        financeViewModel
                                            .retainDeposit(
                                                bookingId,
                                            ),
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(
                                    R.string.retain_remaining_deposit,
                                ),
                            )
                        }
                    }
                }
            }

            if (
                current.status in setOf(
                    DepositStatus.RETURNED,
                    DepositStatus.RETAINED,
                )
            ) {
                item {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }

        errorMessage?.let { message ->
            item {
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
