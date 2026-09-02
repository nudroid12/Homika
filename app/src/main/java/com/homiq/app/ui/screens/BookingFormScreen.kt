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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.model.BookingSource
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.domain.BookingDraft
import com.homiq.app.domain.BookingSaveResult
import com.homiq.app.ui.components.AvailabilityDateField
import com.homiq.app.ui.components.AvailabilityDateMode
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.components.SelectionField
import com.homiq.app.ui.components.isStayRangeAvailable
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.util.formatSenForInput
import com.homiq.app.ui.util.labelRes
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.util.parseRinggitToSen
import com.homiq.app.ui.viewmodel.BookingViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun BookingFormScreen(
    bookingId: String?,
    viewModel: BookingViewModel,
    onSaved: (String) -> Unit,
    onNeedProperty: () -> Unit,
    modifier: Modifier = Modifier,
    initialCheckInEpochDay: Long? = null,
    initialPropertyId: String? = null,
) {
    val bookings by viewModel.bookingList.collectAsStateWithLifecycle()
    val blockedDates by viewModel.blockedDateList.collectAsStateWithLifecycle()
    val properties by viewModel.propertyList.collectAsStateWithLifecycle()
    val existing = bookings.firstOrNull { it.id == bookingId }
    val selectableProperties = remember(properties, existing) {
        properties.filter {
            it.isActive || it.id == existing?.propertyId
        }
    }

    var propertyId by remember(
        bookingId,
        initialPropertyId,
    ) {
        mutableStateOf(initialPropertyId.orEmpty())
    }
    var guestName by remember(bookingId) { mutableStateOf("") }
    var guestPhone by remember(bookingId) { mutableStateOf("") }
    val defaultCheckIn = initialCheckInEpochDay
        ?: LocalDate.now().toEpochDay()
    var checkIn by remember(
        bookingId,
        initialCheckInEpochDay,
    ) {
        mutableLongStateOf(defaultCheckIn)
    }
    var checkOut by remember(
        bookingId,
        initialCheckInEpochDay,
    ) {
        mutableLongStateOf(defaultCheckIn + 1L)
    }
    var source by remember(bookingId) {
        mutableStateOf(BookingSource.WHATSAPP)
    }
    var totalAmount by remember(bookingId) { mutableStateOf("") }
    var amountManuallyEdited by remember(bookingId) {
        mutableStateOf(bookingId != null)
    }
    var notes by remember(bookingId) { mutableStateOf("") }
    var initialized by remember(bookingId) { mutableStateOf(false) }
    var errorMessage by remember(bookingId) {
        mutableStateOf<Int?>(null)
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(existing, selectableProperties) {
        if (!initialized && existing != null) {
            propertyId = existing.propertyId
            guestName = existing.guestName
            guestPhone = existing.guestPhone.orEmpty()
            checkIn = existing.checkInEpochDay
            checkOut = existing.checkOutEpochDay
            source = existing.source
            totalAmount = formatSenForInput(
                existing.totalAmountSen,
            )
            notes = existing.notes.orEmpty()
            initialized = true
        } else if (
            !initialized &&
            bookingId == null &&
            propertyId.isBlank() &&
            selectableProperties.size == 1
        ) {
            propertyId = selectableProperties.first().id
        }
    }

    val selectedProperty = remember(
        propertyId,
        selectableProperties,
    ) {
        selectableProperties.firstOrNull {
            it.id == propertyId
        }
    }
    val stayNights = (checkOut - checkIn).coerceAtLeast(0L)
    val suggestedAmountSen = remember(
        selectedProperty?.defaultNightlyRateSen,
        stayNights,
    ) {
        val rate = selectedProperty?.defaultNightlyRateSen
            ?.coerceAtLeast(0L)
            ?: 0L
        if (stayNights <= 0L || rate == 0L) {
            0L
        } else if (rate > Long.MAX_VALUE / stayNights) {
            Long.MAX_VALUE
        } else {
            rate * stayNights
        }
    }

    LaunchedEffect(
        bookingId,
        propertyId,
        checkIn,
        checkOut,
        suggestedAmountSen,
        amountManuallyEdited,
    ) {
        if (
            bookingId == null &&
            propertyId.isNotBlank() &&
            !amountManuallyEdited
        ) {
            totalAmount = formatSenForInput(suggestedAmountSen)
        }
    }

    val selectedRangeAvailable = remember(
        checkIn,
        checkOut,
        propertyId,
        bookings,
        blockedDates,
        bookingId,
    ) {
        isStayRangeAvailable(
            checkInEpochDay = checkIn,
            checkOutEpochDay = checkOut,
            propertyId = propertyId,
            bookings = bookings,
            blockedDates = blockedDates,
            excludeBookingId = bookingId,
        )
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
                title = stringResource(
                    if (bookingId == null) {
                        R.string.new_booking
                    } else {
                        R.string.edit_booking
                    },
                ),
                subtitle = stringResource(
                    R.string.booking_form_subtitle,
                ),
            )
        }

        if (selectableProperties.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        R.string.booking_needs_property,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            item {
                Button(
                    onClick = onNeedProperty,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add_property))
                }
            }
        } else {
            item {
                val selectedName = selectableProperties
                    .firstOrNull { it.id == propertyId }
                    ?.name
                    .orEmpty()
                SelectionField(
                    label = stringResource(R.string.property),
                    selectedText = selectedName,
                    options = selectableProperties,
                    optionText = { it.name },
                    onSelected = {
                        propertyId = it.id
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = guestName,
                    onValueChange = {
                        guestName = it
                        errorMessage = null
                    },
                    label = {
                        Text(stringResource(R.string.guest_name))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = guestPhone,
                    onValueChange = { guestPhone = it },
                    label = {
                        Text(stringResource(R.string.guest_phone))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                AvailabilityDateField(
                    label = stringResource(R.string.check_in),
                    epochDay = checkIn,
                    onDateSelected = {
                        checkIn = it
                        if (checkOut <= checkIn) {
                            checkOut = checkIn + 1L
                        }
                        errorMessage = null
                    },
                    mode = AvailabilityDateMode.CHECK_IN,
                    propertyId = propertyId,
                    bookings = bookings,
                    blockedDates = blockedDates,
                    excludeBookingId = bookingId,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                AvailabilityDateField(
                    label = stringResource(R.string.check_out),
                    epochDay = checkOut,
                    onDateSelected = {
                        checkOut = it
                        errorMessage = null
                    },
                    mode = AvailabilityDateMode.CHECK_OUT,
                    propertyId = propertyId,
                    bookings = bookings,
                    blockedDates = blockedDates,
                    excludeBookingId = bookingId,
                    checkInEpochDay = checkIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (propertyId.isNotBlank() && !selectedRangeAvailable) {
                item {
                    Text(
                        text = stringResource(R.string.selected_dates_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                SelectionField(
                    label = stringResource(R.string.booking_source),
                    selectedText = stringResource(source.labelRes()),
                    options = BookingSource.entries,
                    optionText = {
                        stringResource(it.labelRes())
                    },
                    onSelected = {
                        source = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = {
                        amountManuallyEdited = true
                        totalAmount = it.filter { char ->
                            char.isDigit() || char == '.'
                        }
                        errorMessage = null
                    },
                    label = {
                        Text(
                            stringResource(
                                R.string.homika_amount_received,
                            ),
                        )
                    },
                    supportingText = {
                        val rate = selectedProperty
                            ?.defaultNightlyRateSen
                            ?: 0L
                        Text(
                            stringResource(
                                if (amountManuallyEdited) {
                                    R.string.homika_amount_received_custom_hint
                                } else {
                                    R.string.homika_amount_received_default_hint
                                },
                                stayNights,
                                formatSenAsRinggit(
                                    rate,
                                    java.util.Locale.getDefault(),
                                ),
                                formatSenAsRinggit(
                                    suggestedAmountSen,
                                    java.util.Locale.getDefault(),
                                ),
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
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            errorMessage?.let { message ->
                item {
                    Text(
                        text = stringResource(message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val amountSen = parseRinggitToSen(totalAmount)
                        if (amountSen == null) {
                            errorMessage = R.string.error_invalid_amount
                            return@Button
                        }
                        scope.launch {
                            when (
                                val result = viewModel.save(
                                    BookingDraft(
                                        id = bookingId,
                                        propertyId = propertyId,
                                        guestName = guestName,
                                        guestPhone = guestPhone,
                                        checkInEpochDay = checkIn,
                                        checkOutEpochDay = checkOut,
                                        source = source,
                                        totalAmountSen = amountSen,
                                        status = existing?.status
                                            ?: BookingStatus.CONFIRMED,
                                        notes = notes,
                                    ),
                                )
                            ) {
                                is BookingSaveResult.Success ->
                                    onSaved(result.bookingId)

                                is BookingSaveResult.Failure ->
                                    errorMessage = result.issue.messageRes()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = propertyId.isBlank() || selectedRangeAvailable,
                ) {
                    Text(stringResource(R.string.save_booking))
                }
            }
        }
    }
}
