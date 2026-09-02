package com.homiq.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.ui.components.EmptyStateCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.viewmodel.BookingViewModel
import com.homiq.app.ui.viewmodel.FinanceViewModel

@Composable
fun PaymentBookingPickerScreen(
    bookingViewModel: BookingViewModel,
    financeViewModel: FinanceViewModel,
    onBookingSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookings by bookingViewModel.bookingList
        .collectAsStateWithLifecycle()
    val properties by bookingViewModel.propertyList
        .collectAsStateWithLifecycle()
    val balances by financeViewModel.bookingBalances
        .collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]

    val balanceByBooking = balances.associateBy { it.bookingId }
    val outstandingBookings = bookings.filter { booking ->
        booking.status != BookingStatus.CANCELLED &&
            (
                balanceByBooking[booking.id]
                    ?.outstandingSen()
                    ?: booking.totalAmountSen
            ) > 0L
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 18.dp,
            end = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.record_payment),
                subtitle = stringResource(
                    R.string.choose_booking_for_payment,
                ),
            )
        }

        if (outstandingBookings.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(
                        R.string.no_outstanding_bookings,
                    ),
                    body = stringResource(
                        R.string.no_outstanding_bookings_body,
                    ),
                    icon = Icons.Outlined.Payments,
                )
            }
        } else {
            items(
                items = outstandingBookings,
                key = { it.id },
            ) { booking ->
                val propertyName = properties
                    .firstOrNull {
                        it.id == booking.propertyId
                    }
                    ?.name
                    ?: stringResource(R.string.unknown_property)

                val balance = balanceByBooking[booking.id]
                    ?.outstandingSen()
                    ?: booking.totalAmountSen

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onBookingSelected(booking.id)
                        },
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = booking.guestName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = propertyName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(
                                R.string.outstanding_value,
                                formatSenAsRinggit(
                                    balance,
                                    locale,
                                ),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
