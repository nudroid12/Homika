package com.homiq.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.data.model.DepositStatus
import com.homiq.app.domain.BookingReferenceRules
import com.homiq.app.domain.DepositRules
import com.homiq.app.ui.components.InfoCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.util.formatEpochDay
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.util.labelRes
import com.homiq.app.ui.util.nightsBetween
import com.homiq.app.ui.viewmodel.BookingViewModel
import com.homiq.app.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun BookingDetailScreen(
    bookingId: String,
    viewModel: BookingViewModel,
    financeViewModel: FinanceViewModel,
    onEdit: () -> Unit,
    onCancelled: () -> Unit,
    onManageDeposit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookings by viewModel.bookingList.collectAsStateWithLifecycle()
    val properties by viewModel.propertyList.collectAsStateWithLifecycle()
    val booking = bookings.firstOrNull { it.id == bookingId }

    val depositFlow = remember(bookingId) {
        financeViewModel.depositFor(bookingId)
    }
    val deposit by depositFlow.collectAsStateWithLifecycle(
        initialValue = null,
    )

    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }

    if (booking == null) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.booking_not_found),
                    subtitle = stringResource(R.string.booking_not_found_body),
                )
            }
        }
        return
    }

    val property = properties
        .firstOrNull { it.id == booking.propertyId }
    val propertyName = property
        ?.name
        ?: stringResource(R.string.unknown_property)
    val bookingReference = BookingReferenceRules.display(
        storedReference = booking.bookingReference,
        propertyCode = property?.bookingCode.orEmpty(),
        propertyName = propertyName,
        checkInEpochDay = booking.checkInEpochDay,
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 18.dp,
            end = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = stringResource(booking.status.labelRes()),
                title = booking.guestName,
                subtitle = "$propertyName · $bookingReference",
            )
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                androidx.compose.foundation.layout.Column {
                    DetailRow(
                        label = stringResource(R.string.check_in),
                        value = formatEpochDay(booking.checkInEpochDay, locale),
                    )
                    HorizontalDivider()
                    DetailRow(
                        label = stringResource(R.string.check_out),
                        value = formatEpochDay(booking.checkOutEpochDay, locale),
                    )
                    HorizontalDivider()
                    DetailRow(
                        label = stringResource(R.string.nights),
                        value = nightsBetween(
                            booking.checkInEpochDay,
                            booking.checkOutEpochDay,
                        ).toString(),
                    )
                    HorizontalDivider()
                    DetailRow(
                        label = stringResource(R.string.homika_amount_received),
                        value = formatSenAsRinggit(booking.totalAmountSen, locale),
                    )
                    HorizontalDivider()
                    DetailRow(
                        label = stringResource(R.string.booking_source),
                        value = stringResource(booking.source.labelRes()),
                    )
                    booking.guestPhone?.let { phone ->
                        HorizontalDivider()
                        DetailRow(
                            label = stringResource(R.string.guest_phone),
                            value = phone,
                        )
                    }
                    booking.notes?.let { notes ->
                        HorizontalDivider()
                        DetailRow(
                            label = stringResource(R.string.notes),
                            value = notes,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.security_deposit),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        item {
            val currentDeposit = deposit
            if (
                currentDeposit == null ||
                currentDeposit.status == DepositStatus.NOT_REQUIRED
            ) {
                InfoCard(
                    title = stringResource(R.string.deposit_not_required),
                    body = stringResource(R.string.deposit_not_required_body),
                )
            } else {
                val remaining = DepositRules.remainingSen(
                    depositAmountSen = currentDeposit.amountSen,
                    returnedAmountSen = currentDeposit.returnedAmountSen,
                )
                InfoCard(
                    title = stringResource(currentDeposit.status.labelRes()),
                    body = stringResource(
                        R.string.deposit_summary_body,
                        formatSenAsRinggit(currentDeposit.amountSen, locale),
                        formatSenAsRinggit(remaining, locale),
                    ),
                )
            }
        }

        if (booking.status != BookingStatus.CANCELLED) {
            item {
                OutlinedButton(
                    onClick = onManageDeposit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(R.string.manage_deposit),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.edit_booking),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.cancel_booking),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(stringResource(R.string.cancel_booking))
            },
            text = {
                Text(stringResource(R.string.cancel_booking_confirmation))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        scope.launch {
                            if (viewModel.cancel(bookingId)) {
                                onCancelled()
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.confirm_cancel))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                ) {
                    Text(stringResource(R.string.keep_booking))
                }
            },
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
