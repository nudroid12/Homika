package com.homiq.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object BookingReferenceRules {
    private const val MAX_PROPERTY_CODE_LENGTH = 5
    private val referenceDateFormatter = DateTimeFormatter.ofPattern("ddMMyy", Locale.ROOT)
    private val tokenRegex = Regex("[\\p{L}\\p{N}]+")

    fun sanitizePropertyCode(raw: String): String =
        raw.filter { it.isLetterOrDigit() }
            .uppercase(Locale.ROOT)
            .take(MAX_PROPERTY_CODE_LENGTH)

    fun suggestPropertyCode(propertyName: String): String {
        val tokens = tokenRegex.findAll(propertyName)
            .map { it.value }
            .filter { it.isNotBlank() }
            .toList()

        val suggested = when {
            tokens.isEmpty() -> "HM"
            tokens.size == 1 -> tokens.first().take(2)
            else -> tokens.take(MAX_PROPERTY_CODE_LENGTH).joinToString("") { token ->
                token.first().toString()
            }
        }

        return sanitizePropertyCode(suggested).ifBlank { "HM" }
    }

    fun effectivePropertyCode(
        storedCode: String,
        propertyName: String,
    ): String = sanitizePropertyCode(storedCode).ifBlank {
        suggestPropertyCode(propertyName)
    }

    fun create(
        propertyCode: String,
        propertyName: String,
        checkInEpochDay: Long,
    ): String {
        val code = effectivePropertyCode(propertyCode, propertyName)
        val date = LocalDate.ofEpochDay(checkInEpochDay).format(referenceDateFormatter)
        return "$code-$date"
    }

    fun display(
        storedReference: String,
        propertyCode: String,
        propertyName: String,
        checkInEpochDay: Long,
    ): String = storedReference.trim().ifBlank {
        create(
            propertyCode = propertyCode,
            propertyName = propertyName,
            checkInEpochDay = checkInEpochDay,
        )
    }
}
