package com.homiq.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.viewmodel.AccountUiMessage
import com.homiq.app.ui.viewmodel.AccountViewModel

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by
        viewModel.state.collectAsStateWithLifecycle()
    var profileName by rememberSaveable {
        mutableStateOf(
            state.account.localProfileName,
        )
    }

    LaunchedEffect(
        state.account.localProfileName,
    ) {
        if (
            profileName.isBlank() ||
            profileName ==
                state.account.localProfileName
        ) {
            profileName =
                state.account.localProfileName
        }
    }

    val authorizationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartIntentSenderForResult(),
        ) { result ->
            viewModel.completeGoogleSignIn(
                result.data,
            )
        }

    state.pendingResolution?.let { pending ->
        LaunchedEffect(pending) {
            viewModel.resolutionLaunched()
            authorizationLauncher.launch(
                IntentSenderRequest.Builder(
                    pending.intentSender,
                ).build(),
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 28.dp,
        ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription =
                            stringResource(
                                R.string.back,
                            ),
                    )
                }
                Column {
                    Text(
                        stringResource(
                            R.string.account,
                        ),
                        style =
                            MaterialTheme.typography
                                .headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            R.string
                                .account_page_subtitle,
                        ),
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Text(
                stringResource(
                    R.string.account_local_profile,
                ),
                style =
                    MaterialTheme.typography
                        .titleMedium,
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape =
                    MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier =
                        Modifier.padding(14.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape =
                                MaterialTheme.shapes
                                    .extraLarge,
                            color =
                                MaterialTheme.colorScheme
                                    .primaryContainer,
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .padding(10.dp)
                                        .size(22.dp),
                                tint =
                                    MaterialTheme.colorScheme
                                        .onPrimaryContainer,
                            )
                        }
                        Column {
                            Text(
                                text =
                                    state.account
                                        .localProfileName
                                        .ifBlank {
                                            stringResource(
                                                R.string
                                                    .account_profile_not_set,
                                            )
                                        },
                                style =
                                    MaterialTheme.typography
                                        .titleSmall,
                                fontWeight =
                                    FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(
                                    R.string
                                        .account_local_profile_body,
                                ),
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = {
                            profileName = it
                        },
                        label = {
                            Text(
                                stringResource(
                                    R.string
                                        .account_profile_name,
                                ),
                            )
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = {
                            viewModel.saveLocalProfile(
                                profileName,
                            )
                        },
                        enabled =
                            profileName.trim() !=
                                state.account
                                    .localProfileName,
                        modifier =
                            Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .account_save_profile,
                            ),
                        )
                    }
                }
            }
        }

        item {
            Text(
                stringResource(
                    R.string.account_google_account,
                ),
                style =
                    MaterialTheme.typography
                        .titleMedium,
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape =
                    MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
            ) {
                Column {
                    if (
                        state.account.googleConnected
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(
                                    Icons.Outlined
                                        .CheckCircle,
                                    contentDescription =
                                        null,
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                )
                            },
                            headlineContent = {
                                Text(
                                    state.account
                                        .googleDisplayName
                                        ?: stringResource(
                                            R.string
                                                .account_google_connected,
                                        ),
                                    fontWeight =
                                        FontWeight.SemiBold,
                                )
                            },
                            supportingContent = {
                                Column {
                                    state.account.googleEmail
                                        ?.let {
                                            Text(it)
                                        }
                                    Text(
                                        stringResource(
                                            R.string
                                                .account_drive_backup_ready,
                                        ),
                                    )
                                }
                            },
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(
                                        R.string
                                            .account_sync_status,
                                    ),
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        if (
                                            state.syncEnabled
                                        ) {
                                            R.string
                                                .account_sync_on
                                        } else {
                                            R.string
                                                .account_sync_off
                                        },
                                    ),
                                )
                            },
                        )
                    } else {
                        ListItem(
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.Login,
                                    contentDescription =
                                        null,
                                )
                            },
                            headlineContent = {
                                Text(
                                    stringResource(
                                        R.string
                                            .account_google_not_signed_in,
                                    ),
                                    fontWeight =
                                        FontWeight.SemiBold,
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        R.string
                                            .account_google_signin_body,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }

        if (state.isBusy) {
            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        } else if (
            state.account.googleConnected
        ) {
            item {
                OutlinedButton(
                    onClick =
                        viewModel::signOutGoogle,
                    modifier =
                        Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = null,
                    )
                    Text(
                        stringResource(
                            R.string
                                .account_google_sign_out,
                        ),
                        modifier =
                            Modifier.padding(
                                start = 8.dp,
                            ),
                    )
                }
            }
            item {
                Text(
                    stringResource(
                        R.string
                            .account_signout_warning,
                    ),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                )
            }
        } else {
            item {
                Button(
                    onClick =
                        viewModel::signInGoogle,
                    modifier =
                        Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.Login,
                        contentDescription = null,
                    )
                    Text(
                        stringResource(
                            R.string
                                .account_google_sign_in,
                        ),
                        modifier =
                            Modifier.padding(
                                start = 8.dp,
                            ),
                    )
                }
            }
        }
    }

    state.message?.let { message ->
        val title: String
        val body: String
        when (message) {
            AccountUiMessage.ProfileSaved -> {
                title = stringResource(
                    R.string.account_profile_saved,
                )
                body = stringResource(
                    R.string
                        .account_profile_saved_body,
                )
            }
            AccountUiMessage.GoogleConnected -> {
                title = stringResource(
                    R.string
                        .account_google_connected,
                )
                body = stringResource(
                    R.string
                        .account_google_connected_body,
                )
            }
            AccountUiMessage.GoogleSignedOut -> {
                title = stringResource(
                    R.string
                        .account_google_signed_out,
                )
                body = stringResource(
                    R.string
                        .account_google_signed_out_body,
                )
            }
            is AccountUiMessage.Failure -> {
                title = stringResource(
                    R.string.sync_error_title,
                )
                body = stringResource(
                    message.reason.messageRes(),
                )
            }
        }
        AlertDialog(
            onDismissRequest =
                viewModel::clearMessage,
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(
                    onClick =
                        viewModel::clearMessage,
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}
