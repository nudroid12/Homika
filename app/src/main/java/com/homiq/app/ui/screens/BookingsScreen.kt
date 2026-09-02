package com.homiq.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.domain.BookingReferenceRules
import com.homiq.app.ui.components.EmptyStateCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.util.formatEpochDay
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.util.labelRes
import com.homiq.app.ui.viewmodel.BookingViewModel
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel: BookingViewModel,
    onBookingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookings by viewModel.bookingList.collectAsStateWithLifecycle()
    val properties by viewModel.propertyList.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now().toEpochDay()
    val propertyNames = remember(properties) { properties.associate { it.id to it.name } }
    val propertyCodes = remember(properties) { properties.associate { it.id to it.bookingCode } }
    var selectedFilter by remember { mutableIntStateOf(0) }
    var actionBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var deleteBooking by remember { mutableStateOf<BookingEntity?>(null) }
    val filters = listOf(
        stringResource(R.string.filter_all),
        stringResource(R.string.filter_upcoming),
        stringResource(R.string.filter_completed),
        stringResource(R.string.filter_cancelled),
    )
    val filtered = remember(bookings, selectedFilter, today) {
        when (selectedFilter) {
            1 -> bookings.filter { it.status != BookingStatus.CANCELLED && it.checkOutEpochDay > today }
            2 -> bookings.filter {
                it.status == BookingStatus.CHECKED_OUT ||
                    (it.status != BookingStatus.CANCELLED && it.checkOutEpochDay <= today)
            }
            3 -> bookings.filter { it.status == BookingStatus.CANCELLED }
            else -> bookings
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 10.dp,
            end = 16.dp,
            bottom = 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.bookings_title),
                subtitle = stringResource(R.string.bookings_count, filtered.size),
                compact = true,
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                filters.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedFilter == index,
                        onClick = { selectedFilter = index },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(
                        if (bookings.isEmpty()) R.string.bookings_empty_title else R.string.no_matching_bookings,
                    ),
                    body = stringResource(
                        if (bookings.isEmpty()) R.string.upcoming_empty_body else R.string.change_booking_filter,
                    ),
                    icon = Icons.Outlined.EventNote,
                    compact = true,
                )
            }
        } else {
            items(filtered, key = { it.id }) { booking ->
                BookingListCard(
                    booking = booking,
                    propertyName = propertyNames[booking.propertyId].orEmpty(),
                    bookingReference = BookingReferenceRules.display(
                        storedReference = booking.bookingReference,
                        propertyCode = propertyCodes[booking.propertyId].orEmpty(),
                        propertyName = propertyNames[booking.propertyId].orEmpty(),
                        checkInEpochDay = booking.checkInEpochDay,
                    ),
                    locale = locale,
                    onClick = { onBookingClick(booking.id) },
                    onLongClick = { actionBooking = booking },
                )
            }
        }
    }

    val selectedActionBooking = actionBooking
    if (selectedActionBooking != null) {
        ModalBottomSheet(
            onDismissRequest = { actionBooking = null },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = selectedActionBooking.guestName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                androidx.compose.material3.ListItem(
                    modifier = Modifier.clickable {
                        actionBooking = null
                        onBookingClick(selectedActionBooking.id)
                    },
                    headlineContent = {
                        Text(stringResource(R.string.list_booking_action_view))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.EventNote,
                            contentDescription = null,
                        )
                    },
                )
                androidx.compose.material3.ListItem(
                    modifier = Modifier.clickable {
                        actionBooking = null
                        deleteBooking = selectedActionBooking
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.list_booking_action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
    }

    val selectedDeleteBooking = deleteBooking
    if (selectedDeleteBooking != null) {
        AlertDialog(
            onDismissRequest = { deleteBooking = null },
            title = {
                Text(stringResource(R.string.list_booking_delete_title))
            },
            text = {
                Text(
                    stringResource(
                        R.string.list_booking_delete_message,
                        selectedDeleteBooking.guestName,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(selectedDeleteBooking.id)
                        deleteBooking = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.list_booking_action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteBooking = null },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookingListCard(
    booking: BookingEntity,
    propertyName: String,
    bookingReference: String,
    locale: java.util.Locale,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = booking.guestName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(propertyName, bookingReference)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatSenAsRinggit(booking.totalAmountSen, locale),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(
                        R.string.booking_date_range,
                        formatEpochDay(booking.checkInEpochDay, locale),
                        formatEpochDay(booking.checkOutEpochDay, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactPill(stringResource(booking.source.labelRes()))
                CompactPill(
                    text = stringResource(booking.status.labelRes()),
                    emphasized = booking.status != BookingStatus.CANCELLED,
                )
            }
        }
    }
}

@Composable
private fun CompactPill(
    text: String,
    emphasized: Boolean = false,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (emphasized) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasized) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}
