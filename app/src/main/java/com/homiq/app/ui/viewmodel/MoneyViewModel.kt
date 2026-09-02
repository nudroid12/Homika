package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.local.model.PropertyAmountRow
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.ExpenseRepository
import com.homiq.app.data.repository.PropertyRepository
import com.homiq.app.domain.BookingRevenueRules
import com.homiq.app.domain.ExpenseDraft
import com.homiq.app.domain.ExpenseManager
import com.homiq.app.domain.ExpenseSaveResult
import com.homiq.app.domain.MoneyRules
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PropertyMoneySummary(
    val propertyId: String?,
    val propertyName: String,
    val revenueSen: Long,
    val expensesSen: Long,
) {
    val netIncomeSen: Long
        get() = MoneyRules.netIncomeSen(
            revenueSen = revenueSen,
            expensesSen = expensesSen,
        )
}

data class MoneyUiState(
    val month: YearMonth = YearMonth.now(),
    val revenueSen: Long = 0L,
    val expensesSen: Long = 0L,
    val netIncomeSen: Long = 0L,
    val breakdown: List<PropertyMoneySummary> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
)

private data class MonthRange(
    val month: YearMonth,
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyViewModel(
    propertyRepository: PropertyRepository,
    bookingRepository: BookingRepository,
    private val expenseRepository: ExpenseRepository,
    private val expenseManager: ExpenseManager,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())

    private val range: Flow<MonthRange> = month.map { value ->
        MonthRange(
            month = value,
            startEpochDay = value.atDay(1).toEpochDay(),
            endEpochDayExclusive = value.plusMonths(1).atDay(1).toEpochDay(),
        )
    }

    private val bookings = bookingRepository.observeAll()

    private val revenue: Flow<Long> = combine(range, bookings) { currentRange, bookingList ->
        BookingRevenueRules.revenueInRangeSen(
            bookings = bookingList,
            startEpochDay = currentRange.startEpochDay,
            endEpochDayExclusive = currentRange.endEpochDayExclusive,
        )
    }

    private val expensesTotal: Flow<Long> = range.flatMapLatest {
        expenseRepository.observeTotalInRangeSen(
            startEpochDay = it.startEpochDay,
            endEpochDayExclusive = it.endEpochDayExclusive,
        )
    }

    private val expenseList: Flow<List<ExpenseEntity>> = range.flatMapLatest {
        expenseRepository.observeInRange(
            startEpochDay = it.startEpochDay,
            endEpochDayExclusive = it.endEpochDayExclusive,
        )
    }

    private val revenueByProperty: Flow<List<PropertyAmountRow>> =
        combine(range, bookings) { currentRange, bookingList ->
            BookingRevenueRules.revenueByPropertyInRange(
                bookings = bookingList,
                startEpochDay = currentRange.startEpochDay,
                endEpochDayExclusive = currentRange.endEpochDayExclusive,
            )
        }

    private val expensesByProperty: Flow<List<PropertyAmountRow>> = range.flatMapLatest {
        expenseRepository.observeByPropertyInRange(
            startEpochDay = it.startEpochDay,
            endEpochDayExclusive = it.endEpochDayExclusive,
        )
    }

    private val properties: Flow<List<PropertyEntity>> = propertyRepository.observeAll()

    val activeProperties: StateFlow<List<PropertyEntity>> =
        properties
            .map { list -> list.filter { !it.isDeleted } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val state: StateFlow<MoneyUiState> =
        combine(
            month,
            revenue,
            expensesTotal,
            expenseList,
            combine(
                properties,
                revenueByProperty,
                expensesByProperty,
            ) { propertyList, revenueRows, expenseRows ->
                buildBreakdown(
                    properties = propertyList,
                    revenueRows = revenueRows,
                    expenseRows = expenseRows,
                )
            },
        ) {
            currentMonth,
            revenueSen,
            expenseSen,
            currentExpenses,
            propertyBreakdown,
        ->
            MoneyUiState(
                month = currentMonth,
                revenueSen = revenueSen,
                expensesSen = expenseSen,
                netIncomeSen = MoneyRules.netIncomeSen(revenueSen, expenseSen),
                breakdown = propertyBreakdown,
                expenses = currentExpenses,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoneyUiState(),
        )

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun currentMonth() { month.value = YearMonth.now() }

    suspend fun saveExpense(draft: ExpenseDraft): ExpenseSaveResult =
        expenseManager.save(draft)

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.delete(expenseId)
        }
    }

    private fun buildBreakdown(
        properties: List<PropertyEntity>,
        revenueRows: List<PropertyAmountRow>,
        expenseRows: List<PropertyAmountRow>,
    ): List<PropertyMoneySummary> {
        val names = properties.associate { it.id to it.name }
        val revenue = revenueRows.associate { it.propertyId to it.amountSen }
        val expenses = expenseRows.associate { it.propertyId to it.amountSen }

        val ids = buildSet<String?> {
            addAll(revenue.keys)
            addAll(expenses.keys)
        }

        return ids.map { id ->
            PropertyMoneySummary(
                propertyId = id,
                propertyName = if (id == null) "" else names[id].orEmpty(),
                revenueSen = revenue[id] ?: 0L,
                expensesSen = expenses[id] ?: 0L,
            )
        }.sortedWith(
            compareBy<PropertyMoneySummary> { it.propertyId == null }
                .thenBy { it.propertyName.lowercase() },
        )
    }
}
