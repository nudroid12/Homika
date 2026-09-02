package com.homiq.app.domain

import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.repository.ExpenseRepository
import com.homiq.app.data.repository.PropertyRepository

class ExpenseManager(
    private val properties: PropertyRepository,
    private val expenses: ExpenseRepository,
) {
    suspend fun save(
        draft: ExpenseDraft,
    ): ExpenseSaveResult {
        if (!ExpenseRules.isAmountValid(draft.amountSen)) {
            return ExpenseSaveResult.Failure(
                ExpenseSaveIssue.INVALID_AMOUNT,
            )
        }

        if (draft.propertyId != null) {
            val property = properties.getById(draft.propertyId)
                ?: return ExpenseSaveResult.Failure(
                    ExpenseSaveIssue.PROPERTY_NOT_FOUND,
                )

            if (property.isDeleted) {
                return ExpenseSaveResult.Failure(
                    ExpenseSaveIssue.PROPERTY_NOT_FOUND,
                )
            }
        }

        val entity = ExpenseEntity(
            propertyId = draft.propertyId,
            amountSen = draft.amountSen,
            expenseDateEpochDay = draft.expenseDateEpochDay,
            category = draft.category,
            description = draft.description
                .trim()
                .ifBlank { null },
            notes = draft.notes
                .trim()
                .ifBlank { null },
        )

        expenses.save(entity)
        return ExpenseSaveResult.Success(entity.id)
    }
}
