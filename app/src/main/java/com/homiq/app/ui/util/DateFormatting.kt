package com.homiq.app.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatEpochDay(
    epochDay: Long,
    locale: Locale,
): String =
    LocalDate.ofEpochDay(epochDay)
        .format(
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale),
        )

fun nightsBetween(
    checkInEpochDay: Long,
    checkOutEpochDay: Long,
): Long = (checkOutEpochDay - checkInEpochDay).coerceAtLeast(0L)
