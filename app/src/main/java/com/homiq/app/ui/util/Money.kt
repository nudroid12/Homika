package com.homiq.app.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun parseRinggitToSen(input: String): Long? {
    val cleaned = input
        .trim()
        .replace(",", "")
        .removePrefix("RM")
        .trim()

    if (cleaned.isBlank()) return 0L

    return runCatching {
        BigDecimal(cleaned)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()
}

fun formatSenAsRinggit(
    amountSen: Long,
    locale: Locale,
): String {
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance("MYR")
    }
    return formatter.format(BigDecimal.valueOf(amountSen, 2))
}

fun formatSenForInput(amountSen: Long): String =
    BigDecimal.valueOf(amountSen, 2)
        .stripTrailingZeros()
        .toPlainString()


fun formatPercent(
    value: Double,
    locale: Locale,
): String =
    String.format(
        locale,
        "%.1f%%",
        value,
    )
