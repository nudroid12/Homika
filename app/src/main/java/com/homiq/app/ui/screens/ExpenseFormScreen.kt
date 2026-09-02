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
import com.homiq.app.data.model.ExpenseCategory
import com.homiq.app.domain.ExpenseDraft
import com.homiq.app.domain.ExpenseSaveResult
import com.homiq.app.ui.components.DateField
import com.homiq.app.ui.components.InfoCard
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.components.SelectionField
import com.homiq.app.ui.util.labelRes
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.util.parseRinggitToSen
import com.homiq.app.ui.viewmodel.MoneyViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

private data class ExpensePropertyOption(
    val id: String?,
    val name: String,
)

@Composable
fun ExpenseFormScreen(
    viewModel: MoneyViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val properties by viewModel.activeProperties
        .collectAsStateWithLifecycle()

    val propertyOptions = remember(properties) {
        buildList {
            add(
                ExpensePropertyOption(
                    id = null,
                    name = "",
                ),
            )
            properties.forEach {
                add(
                    ExpensePropertyOption(
                        id = it.id,
                        name = it.name,
                    ),
                )
            }
        }
    }

    var selectedPropertyId by remember {
        mutableStateOf<String?>(null)
    }
    var amount by remember { mutableStateOf("") }
    var expenseDate by remember {
        mutableLongStateOf(LocalDate.now().toEpochDay())
    }
    var category by remember {
        mutableStateOf(ExpenseCategory.CLEANING)
    }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember {
        mutableStateOf<Int?>(null)
    }

    val scope = rememberCoroutineScope()

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
                title = stringResource(R.string.add_expense),
                subtitle = stringResource(
                    R.string.expense_form_subtitle,
                ),
            )
        }

        item {
            InfoCard(
                title = stringResource(
                    R.string.expense_general_title,
                ),
                body = stringResource(
                    R.string.expense_general_body,
                ),
            )
        }

        item {
            val selected = propertyOptions.first {
                it.id == selectedPropertyId
            }

            SelectionField(
                label = stringResource(
                    R.string.expense_property,
                ),
                selectedText = if (selected.id == null) {
                    stringResource(R.string.general_expense)
                } else {
                    selected.name
                },
                options = propertyOptions,
                optionText = {
                    if (it.id == null) {
                        stringResource(
                            R.string.general_expense,
                        )
                    } else {
                        it.name
                    }
                },
                onSelected = {
                    selectedPropertyId = it.id
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it.filter { char ->
                        char.isDigit() || char == '.'
                    }
                    errorMessage = null
                },
                label = {
                    Text(
                        stringResource(
                            R.string.expense_amount,
                        ),
                    )
                },
                prefix = { Text("RM ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            DateField(
                label = stringResource(R.string.expense_date),
                epochDay = expenseDate,
                onDateSelected = {
                    expenseDate = it
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            SelectionField(
                label = stringResource(
                    R.string.expense_category,
                ),
                selectedText = stringResource(
                    category.labelRes(),
                ),
                options = ExpenseCategory.entries,
                optionText = {
                    stringResource(it.labelRes())
                },
                onSelected = {
                    category = it
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = {
                    Text(
                        stringResource(
                            R.string.expense_description,
                        ),
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = {
                    Text(stringResource(R.string.notes))
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        errorMessage?.let { message ->
            item {
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            Button(
                onClick = {
                    val amountSen =
                        parseRinggitToSen(amount)

                    if (amountSen == null) {
                        errorMessage =
                            R.string.error_invalid_expense_amount
                        return@Button
                    }

                    scope.launch {
                        when (
                            val result =
                                viewModel.saveExpense(
                                    ExpenseDraft(
                                        propertyId =
                                            selectedPropertyId,
                                        amountSen = amountSen,
                                        expenseDateEpochDay =
                                            expenseDate,
                                        category = category,
                                        description = description,
                                        notes = notes,
                                    ),
                                )
                        ) {
                            is ExpenseSaveResult.Success ->
                                onSaved()

                            is ExpenseSaveResult.Failure ->
                                errorMessage =
                                    result.issue.messageRes()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        R.string.save_expense,
                    ),
                )
            }
        }
    }
}
