package com.homiq.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.domain.BookingReferenceRules
import com.homiq.app.domain.PropertyDraft
import com.homiq.app.domain.PropertySaveResult
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.util.formatSenForInput
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.util.parseRinggitToSen
import com.homiq.app.ui.viewmodel.PropertyViewModel
import kotlinx.coroutines.launch

@Composable
fun PropertyFormScreen(
    propertyId: String?,
    viewModel: PropertyViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val properties by viewModel.propertyList.collectAsStateWithLifecycle()
    val existing = properties.firstOrNull { it.id == propertyId }

    var name by remember(propertyId) { mutableStateOf("") }
    var bookingCode by remember(propertyId) { mutableStateOf("") }
    var bookingCodeManuallyEdited by remember(propertyId) { mutableStateOf(propertyId != null) }
    var address by remember(propertyId) { mutableStateOf("") }
    var rate by remember(propertyId) { mutableStateOf("") }
    var notes by remember(propertyId) { mutableStateOf("") }
    var isActive by remember(propertyId) { mutableStateOf(true) }
    var initialized by remember(propertyId) { mutableStateOf(false) }
    var errorMessage by remember(propertyId) {
        mutableStateOf<Int?>(null)
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            name = existing.name
            bookingCode = BookingReferenceRules.effectivePropertyCode(
                storedCode = existing.bookingCode,
                propertyName = existing.name,
            )
            address = existing.address.orEmpty()
            rate = formatSenForInput(existing.defaultNightlyRateSen)
            notes = existing.notes.orEmpty()
            isActive = existing.isActive
            initialized = true
        }
    }



    LaunchedEffect(name, propertyId, bookingCodeManuallyEdited) {
        if (propertyId == null && !bookingCodeManuallyEdited) {
            bookingCode = BookingReferenceRules.suggestPropertyCode(name)
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
                title = stringResource(
                    if (propertyId == null) {
                        R.string.add_property
                    } else {
                        R.string.edit_property
                    },
                ),
                subtitle = stringResource(
                    R.string.property_form_subtitle,
                ),
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.property_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = bookingCode,
                onValueChange = { raw ->
                    bookingCodeManuallyEdited = true
                    bookingCode = BookingReferenceRules.sanitizePropertyCode(raw)
                },
                label = {
                    Text(stringResource(R.string.homika_booking_code))
                },
                supportingText = {
                    Text(stringResource(R.string.homika_booking_code_hint))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = {
                    Text(stringResource(R.string.property_address))
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = rate,
                onValueChange = {
                    rate = it.filter { char ->
                        char.isDigit() || char == '.'
                    }
                    errorMessage = null
                },
                label = {
                    Text(
                        stringResource(
                            R.string.default_nightly_rate,
                        ),
                    )
                },
                prefix = { Text("RM ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (propertyId != null) {
            item {
                androidx.compose.material3.ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(
                                R.string.property_active,
                            ),
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(
                                R.string.property_active_body,
                            ),
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                        )
                    },
                )
            }
        }

        errorMessage?.let { message ->
            item {
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Button(
                onClick = {
                    val rateSen = parseRinggitToSen(rate)
                    if (rateSen == null) {
                        errorMessage = R.string.error_invalid_rate
                        return@Button
                    }

                    scope.launch {
                        when (
                            val result = viewModel.save(
                                PropertyDraft(
                                    id = propertyId,
                                    name = name,
                                    bookingCode = bookingCode,
                                    address = address,
                                    notes = notes,
                                    defaultNightlyRateSen = rateSen,
                                    isActive = isActive,
                                ),
                            )
                        ) {
                            is PropertySaveResult.Success ->
                                onSaved()

                            is PropertySaveResult.Failure ->
                                errorMessage = result.issue.messageRes()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_property))
            }
        }
    }
}
