package com.homiq.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.ui.components.EmptyStateCard
import com.homiq.app.ui.components.MetricCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.components.SectionHeader
import com.homiq.app.ui.util.formatEpochDay
import com.homiq.app.ui.util.formatSenAsRinggit
import com.homiq.app.ui.util.labelRes
import com.homiq.app.ui.viewmodel.MoneyViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    viewModel: MoneyViewModel,
    onAddExpense: () -> Unit,
    onReportsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val propertyNames = viewModel.activeProperties.collectAsStateWithLifecycle().value.associate { it.id to it.name }
    var actionExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var deleteExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    val monthTitle = state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
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
                title = stringResource(R.string.money_title),
                subtitle = stringResource(R.string.homika_money_live_subtitle_v2),
                compact = true,
            )
        }
        item {
            MonthSelector(
                title = monthTitle,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onCurrent = viewModel::currentMonth,
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        stringResource(R.string.money_month_summary),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        formatSenAsRinggit(state.netIncomeSen, locale),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(R.string.money_net_support),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MetricCard(
                            label = stringResource(R.string.revenue),
                            value = formatSenAsRinggit(state.revenueSen, locale),
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                        MetricCard(
                            label = stringResource(R.string.expenses),
                            value = formatSenAsRinggit(state.expensesSen, locale),
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.add_expense),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 5.dp),
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onReportsClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.QueryStats, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.view_reports),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 5.dp),
                        maxLines = 1,
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.property_breakdown),
                compact = true,
            )
        }
        if (state.breakdown.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.no_money_activity),
                    body = stringResource(R.string.homika_no_money_activity_body_v2),
                    icon = Icons.Outlined.ReceiptLong,
                    compact = true,
                )
            }
        } else {
            items(state.breakdown, key = { it.propertyId ?: "general" }) { row ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = if (row.propertyId == null) {
                                stringResource(R.string.general_expense)
                            } else {
                                row.propertyName.ifBlank { stringResource(R.string.unknown_property) }
                            },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        MoneyLine(stringResource(R.string.revenue), formatSenAsRinggit(row.revenueSen, locale))
                        MoneyLine(stringResource(R.string.expenses), formatSenAsRinggit(row.expensesSen, locale))
                        MoneyLine(
                            stringResource(R.string.net_income),
                            formatSenAsRinggit(row.netIncomeSen, locale),
                            true,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.expense_history),
                compact = true,
            )
        }
        if (state.expenses.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.no_expenses_this_month),
                    body = stringResource(R.string.no_expenses_this_month_body),
                    icon = Icons.Outlined.ReceiptLong,
                    compact = true,
                )
            }
        } else {
            items(state.expenses, key = { it.id }) { expense ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { actionExpense = expense },
                        ),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                stringResource(expense.category.labelRes()),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                expense.propertyId?.let { propertyNames[it] }
                                    ?: stringResource(R.string.general_expense),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                formatEpochDay(expense.expenseDateEpochDay, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            formatSenAsRinggit(expense.amountSen, locale),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    val selectedActionExpense = actionExpense
    if (selectedActionExpense != null) {
        ModalBottomSheet(onDismissRequest = { actionExpense = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(selectedActionExpense.category.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                androidx.compose.material3.ListItem(
                    modifier = Modifier.clickable {
                        actionExpense = null
                        deleteExpense = selectedActionExpense
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.list_expense_action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
    }

    val selectedDeleteExpense = deleteExpense
    if (selectedDeleteExpense != null) {
        AlertDialog(
            onDismissRequest = { deleteExpense = null },
            title = { Text(stringResource(R.string.list_expense_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.list_expense_delete_message,
                        formatSenAsRinggit(selectedDeleteExpense.amountSen, locale),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(selectedDeleteExpense.id)
                        deleteExpense = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.list_expense_action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteExpense = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MonthSelector(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            androidx.compose.material3.TextButton(
                onClick = onCurrent,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) {
                Text(
                    stringResource(R.string.current_month),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MoneyLine(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
