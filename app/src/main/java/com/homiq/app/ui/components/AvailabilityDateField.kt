package com.homiq.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homiq.app.R
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.ui.util.formatEpochDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal val CalendarCheckInBlue = Color(0xFF3B82F6)
internal val CalendarCheckOutRed = Color(0xFFEF4444)

enum class AvailabilityDateMode {
    CHECK_IN,
    CHECK_OUT,
}

@Composable
fun AvailabilityDateField(
    label: String,
    epochDay: Long,
    onDateSelected: (Long) -> Unit,
    mode: AvailabilityDateMode,
    propertyId: String,
    bookings: List<BookingEntity>,
    blockedDates: List<BlockedDateEntity>,
    excludeBookingId: String? = null,
    checkInEpochDay: Long? = null,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    var pickerOpen by remember { mutableStateOf(false) }
    val enabled = propertyId.isNotBlank()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { pickerOpen = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatEpochDay(epochDay, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = stringResource(R.string.choose_date),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }

    if (pickerOpen) {
        AvailabilityCalendarDialog(
            selectedEpochDay = epochDay,
            mode = mode,
            propertyId = propertyId,
            bookings = bookings,
            blockedDates = blockedDates,
            excludeBookingId = excludeBookingId,
            checkInEpochDay = checkInEpochDay,
            onDismiss = { pickerOpen = false },
            onSelected = {
                pickerOpen = false
                onDateSelected(it)
            },
        )
    }
}

@Composable
private fun AvailabilityCalendarDialog(
    selectedEpochDay: Long,
    mode: AvailabilityDateMode,
    propertyId: String,
    bookings: List<BookingEntity>,
    blockedDates: List<BlockedDateEntity>,
    excludeBookingId: String?,
    checkInEpochDay: Long?,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now() }
    var month by remember(selectedEpochDay) {
        mutableStateOf(YearMonth.from(LocalDate.ofEpochDay(selectedEpochDay)))
    }

    val activeBookings = remember(bookings, propertyId, excludeBookingId) {
        bookings.filter {
            !it.isDeleted &&
                it.status != BookingStatus.CANCELLED &&
                it.propertyId == propertyId &&
                it.id != excludeBookingId
        }
    }
    val propertyBlocks = remember(blockedDates, propertyId) {
        blockedDates.filter {
            !it.isDeleted && it.propertyId == propertyId
        }
    }
    val monthTitle = remember(month, locale) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { month = month.minusMonths(1) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = stringResource(R.string.previous_month),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { month = month.plusMonths(1) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = stringResource(R.string.next_month),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                AvailabilityMonthGrid(
                    month = month,
                    today = today,
                    selectedEpochDay = selectedEpochDay,
                    mode = mode,
                    checkInEpochDay = checkInEpochDay,
                    bookings = activeBookings,
                    blocks = propertyBlocks,
                    onSelected = onSelected,
                )

                AvailabilityLegend()
            }
        }
    }
}

@Composable
private fun AvailabilityMonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedEpochDay: Long,
    mode: AvailabilityDateMode,
    checkInEpochDay: Long?,
    bookings: List<BookingEntity>,
    blocks: List<BlockedDateEntity>,
    onSelected: (Long) -> Unit,
) {
    val weekdays = stringArrayResource(R.array.weekdays_short)
    val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
    val cells = remember(month) {
        val usedCells = firstDayOffset + month.lengthOfMonth()
        val weekCount = (usedCells + 6) / 7
        List(weekCount * 7) { index ->
            val day = index - firstDayOffset + 1
            if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                        )
                    } else {
                        val epochDay = date.toEpochDay()
                        val selectable = when (mode) {
                            AvailabilityDateMode.CHECK_IN ->
                                isCheckInAvailable(epochDay, bookings, blocks)

                            AvailabilityDateMode.CHECK_OUT ->
                                checkInEpochDay != null && isCheckoutAvailable(
                                    checkInEpochDay = checkInEpochDay,
                                    candidateCheckoutEpochDay = epochDay,
                                    bookings = bookings,
                                    blocks = blocks,
                                )
                        }
                        val hasCheckIn = bookings.any {
                            it.checkInEpochDay == epochDay
                        }
                        val hasCheckOut = bookings.any {
                            it.checkOutEpochDay == epochDay
                        }
                        val hasStay = bookings.any {
                            epochDay >= it.checkInEpochDay &&
                                epochDay < it.checkOutEpochDay
                        }
                        val hasBlock = blocks.any {
                            epochDay >= it.startEpochDay && epochDay < it.endEpochDay
                        }

                        AvailabilityDayCell(
                            day = date.dayOfMonth,
                            isToday = date == today,
                            isSelected = epochDay == selectedEpochDay,
                            enabled = selectable,
                            hasCheckIn = hasCheckIn,
                            hasCheckOut = hasCheckOut,
                            hasStay = hasStay,
                            hasBlock = hasBlock,
                            onClick = { onSelected(epochDay) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    hasCheckIn: Boolean,
    hasCheckOut: Boolean,
    hasStay: Boolean,
    hasBlock: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasBlock -> MaterialTheme.colorScheme.surfaceVariant
        hasStay -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
        else -> MaterialTheme.colorScheme.surface
    }
    val border = if (isToday) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Surface(
        modifier = modifier
            .height(40.dp)
            .padding(1.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday || isSelected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = if (enabled || isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                },
            )

            if (hasCheckIn || hasCheckOut) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (hasCheckIn) CalendarMarkerDot(CalendarCheckInBlue)
                    if (hasCheckOut) CalendarMarkerDot(CalendarCheckOutRed)
                }
            }
        }
    }
}

@Composable
internal fun AvailabilityLegend(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvailabilityLegendItem(
            color = CalendarCheckInBlue,
            label = stringResource(R.string.check_in),
        )
        AvailabilityLegendItem(
            color = CalendarCheckOutRed,
            label = stringResource(R.string.check_out),
        )
        AvailabilityLegendItem(
            color = MaterialTheme.colorScheme.secondary,
            label = stringResource(R.string.legend_booked),
        )
        AvailabilityLegendItem(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            label = stringResource(R.string.legend_blocked),
        )
    }
}

@Composable
private fun AvailabilityLegendItem(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        CalendarMarkerDot(color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CalendarMarkerDot(color: Color) {
    Surface(
        modifier = Modifier.size(5.dp),
        shape = CircleShape,
        color = color,
    ) {}
}

internal fun isCheckInAvailable(
    epochDay: Long,
    bookings: List<BookingEntity>,
    blocks: List<BlockedDateEntity>,
): Boolean {
    val occupied = bookings.any {
        epochDay >= it.checkInEpochDay && epochDay < it.checkOutEpochDay
    }
    val blocked = blocks.any {
        epochDay >= it.startEpochDay && epochDay < it.endEpochDay
    }
    return !occupied && !blocked
}

internal fun isCheckoutAvailable(
    checkInEpochDay: Long,
    candidateCheckoutEpochDay: Long,
    bookings: List<BookingEntity>,
    blocks: List<BlockedDateEntity>,
): Boolean {
    if (candidateCheckoutEpochDay <= checkInEpochDay) return false

    val bookingOverlap = bookings.any {
        it.checkInEpochDay < candidateCheckoutEpochDay &&
            it.checkOutEpochDay > checkInEpochDay
    }
    if (bookingOverlap) return false

    val blockOverlap = blocks.any {
        it.startEpochDay < candidateCheckoutEpochDay &&
            it.endEpochDay > checkInEpochDay
    }
    return !blockOverlap
}

internal fun isStayRangeAvailable(
    checkInEpochDay: Long,
    checkOutEpochDay: Long,
    propertyId: String,
    bookings: List<BookingEntity>,
    blockedDates: List<BlockedDateEntity>,
    excludeBookingId: String? = null,
): Boolean {
    if (propertyId.isBlank() || checkOutEpochDay <= checkInEpochDay) return false

    val activeBookings = bookings.filter {
        !it.isDeleted &&
            it.status != BookingStatus.CANCELLED &&
            it.propertyId == propertyId &&
            it.id != excludeBookingId
    }
    val blocks = blockedDates.filter {
        !it.isDeleted && it.propertyId == propertyId
    }

    return isCheckoutAvailable(
        checkInEpochDay = checkInEpochDay,
        candidateCheckoutEpochDay = checkOutEpochDay,
        bookings = activeBookings,
        blocks = blocks,
    )
}
