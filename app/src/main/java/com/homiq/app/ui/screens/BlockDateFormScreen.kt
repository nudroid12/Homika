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
import com.homiq.app.domain.BlockedDateDraft
import com.homiq.app.domain.BlockedDateSaveResult
import com.homiq.app.ui.components.DateField
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.components.SelectionField
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.viewmodel.BlockedDateViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun BlockDateFormScreen(
    viewModel: BlockedDateViewModel,
    onSaved: () -> Unit,
    onNeedProperty: () -> Unit,
    modifier: Modifier = Modifier,
    initialStartEpochDay: Long? = null,
    initialPropertyId: String? = null,
) {
    val properties by viewModel.activeProperties
        .collectAsStateWithLifecycle()

    val defaultStart = initialStartEpochDay
        ?: LocalDate.now().toEpochDay()

    var propertyId by remember(initialPropertyId) {
        mutableStateOf(initialPropertyId.orEmpty())
    }
    var start by remember(initialStartEpochDay) {
        mutableLongStateOf(defaultStart)
    }
    var endInclusive by remember(initialStartEpochDay) {
        mutableLongStateOf(defaultStart)
    }
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember {
        mutableStateOf<Int?>(null)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(properties) {
        if (
            propertyId.isBlank() &&
            properties.size == 1
        ) {
            propertyId = properties.first().id
        }
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
                title = stringResource(R.string.block_date),
                subtitle = stringResource(
                    R.string.block_date_subtitle,
                ),
            )
        }

        if (properties.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        R.string.block_needs_property,
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
                val selectedName = properties
                    .firstOrNull { it.id == propertyId }
                    ?.name
                    .orEmpty()

                SelectionField(
                    label = stringResource(R.string.property),
                    selectedText = selectedName,
                    options = properties,
                    optionText = { it.name },
                    onSelected = {
                        propertyId = it.id
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                DateField(
                    label = stringResource(R.string.block_from),
                    epochDay = start,
                    onDateSelected = {
                        start = it
                        if (endInclusive < start) {
                            endInclusive = start
                        }
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                DateField(
                    label = stringResource(R.string.block_until),
                    epochDay = endInclusive,
                    onDateSelected = {
                        endInclusive = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = {
                        Text(
                            stringResource(
                                R.string.block_reason,
                            ),
                        )
                    },
                    minLines = 2,
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
                        scope.launch {
                            when (
                                val result = viewModel.save(
                                    BlockedDateDraft(
                                        propertyId = propertyId,
                                        startEpochDay = start,
                                        endEpochDayExclusive =
                                            endInclusive + 1L,
                                        reason = reason,
                                    ),
                                )
                            ) {
                                is BlockedDateSaveResult.Success ->
                                    onSaved()

                                is BlockedDateSaveResult.Failure ->
                                    errorMessage =
                                        result.issue.messageRes()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.save_blocked_date,
                        ),
                    )
                }
            }
        }
    }
}
