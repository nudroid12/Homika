package com.homiq.app.domain

import com.homiq.app.data.model.ExpenseCategory

data class ExpenseDraft(
    val propertyId: String?,
    val amountSen: Long,
    val expenseDateEpochDay: Long,
    val category: ExpenseCategory,
    val description: String,
    val notes: String,
)

enum class ExpenseSaveIssue {
    INVALID_AMOUNT,
    PROPERTY_NOT_FOUND,
}

sealed interface ExpenseSaveResult {
    data class Success(val expenseId: String) : ExpenseSaveResult
    data class Failure(val issue: ExpenseSaveIssue) : ExpenseSaveResult
}
