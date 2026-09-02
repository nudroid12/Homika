package com.homiq.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homiq.app.HomiqApplication
import com.homiq.app.BuildConfig
import com.homiq.app.R
import com.homiq.app.data.preferences.AppearanceMode
import com.homiq.app.data.preferences.AppearancePreferences
import com.homiq.app.data.preferences.TextSizeMode
import com.homiq.app.ui.components.HomikaBrandMark
import com.homiq.app.ui.theme.LocalHomikaTextSize
import com.homiq.app.ui.viewmodel.AccountViewModel
import com.homiq.app.ui.viewmodel.HomiqViewModelFactory

@Composable
fun MoreScreen(
    onPropertiesClick: () -> Unit,
    onBackupClick: () -> Unit,
    onLicenseClick: () -> Unit,
    appLockEnabled: Boolean,
    onSecurityClick: () -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application =
        context.applicationContext as HomiqApplication
    val factory = remember(application) {
        HomiqViewModelFactory(application.container)
    }
    val accountViewModel: AccountViewModel =
        viewModel(factory = factory)
    val accountState by
        accountViewModel.state
            .collectAsStateWithLifecycle()
    var showAccount by rememberSaveable {
        mutableStateOf(false)
    }

    BackHandler(enabled = showAccount) {
        showAccount = false
    }

    if (showAccount) {
        AccountScreen(
            viewModel = accountViewModel,
            onBack = { showAccount = false },
            modifier = modifier,
        )
        return
    }

    val configuration = LocalConfiguration.current
    val explicitLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val currentLanguage = if (explicitLocales.isBlank()) {
        configuration.locales[0].language
    } else {
        explicitLocales.substringBefore(",").substringBefore("-")
    }
    val isMalay = currentLanguage == "ms"

    val appearancePreferences = remember(context) {
        AppearancePreferences(context.applicationContext)
    }
    var appearanceMode by remember {
        mutableStateOf(appearancePreferences.mode)
    }

    val textSizeController = LocalHomikaTextSize.current
    val textSizeMode = textSizeController.mode

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactHeader()

        CompactSection(stringResource(R.string.settings_account)) {
            CompactSettingRow(
                icon = Icons.Outlined.Person,
                title = stringResource(R.string.account),
                supporting =
                    accountState.account
                        .localProfileName
                        .takeIf { it.isNotBlank() }
                        ?: stringResource(
                            R.string.account_local_supporting,
                        ),
                trailing = stringResource(
                    R.string.account_local_short,
                ),
                onClick = { showAccount = true },
            )
            CompactDivider()
            CompactSettingRow(
                icon = Icons.Outlined.VerifiedUser,
                title = stringResource(R.string.license_settings_title),
                supporting = stringResource(R.string.license_settings_supporting),
                trailing = stringResource(R.string.license_settings_active),
                onClick = onLicenseClick,
            )
        }

        CompactSection(stringResource(R.string.settings_workspace)) {
            CompactSettingRow(
                icon = Icons.Outlined.HomeWork,
                title = stringResource(R.string.properties),
                supporting = stringResource(R.string.properties_compact_supporting),
                onClick = onPropertiesClick,
            )
        }

        CompactSection(stringResource(R.string.settings_data)) {
            CompactSettingRow(
                icon = Icons.Outlined.Backup,
                title = stringResource(R.string.backup_restore),
                onClick = onBackupClick,
            )
            CompactDivider()
            CompactSettingRow(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.security),
                trailing = stringResource(
                    if (appLockEnabled) R.string.on else R.string.off,
                ),
                onClick = onSecurityClick,
            )
        }

        CompactSection(stringResource(R.string.settings_preferences)) {
            SelectorSettingRow(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.language),
            ) {
                TinyChoice(
                    label = "MY",
                    selected = currentLanguage == "ms",
                    onClick = { setHomikaLanguage("ms") },
                )
                TinyChoice(
                    label = "EN",
                    selected = currentLanguage != "ms",
                    onClick = { setHomikaLanguage("en") },
                )
            }
            CompactDivider()
            SelectorSettingRow(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.appearance),
            ) {
                TinyChoice(
                    label = stringResource(R.string.theme_system_short),
                    selected = appearanceMode == AppearanceMode.SYSTEM,
                    onClick = {
                        appearanceMode = AppearanceMode.SYSTEM
                        appearancePreferences.set(AppearanceMode.SYSTEM)
                    },
                )
                TinyChoice(
                    label = stringResource(R.string.theme_light_short),
                    selected = appearanceMode == AppearanceMode.LIGHT,
                    onClick = {
                        appearanceMode = AppearanceMode.LIGHT
                        appearancePreferences.set(AppearanceMode.LIGHT)
                    },
                )
                TinyChoice(
                    label = stringResource(R.string.theme_dark_short),
                    selected = appearanceMode == AppearanceMode.DARK,
                    onClick = {
                        appearanceMode = AppearanceMode.DARK
                        appearancePreferences.set(AppearanceMode.DARK)
                    },
                )
            }
            CompactDivider()
            SelectorSettingRow(
                icon = Icons.Outlined.FormatSize,
                title = if (isMalay) "Saiz teks" else "Text size",
            ) {
                TinyChoice(
                    label = "A−",
                    selected = textSizeMode == TextSizeMode.SMALL,
                    onClick = {
                        textSizeController.setMode(TextSizeMode.SMALL)
                    },
                )
                TinyChoice(
                    label = "A",
                    selected = textSizeMode == TextSizeMode.STANDARD,
                    onClick = {
                        textSizeController.setMode(TextSizeMode.STANDARD)
                    },
                )
                TinyChoice(
                    label = "A+",
                    selected = textSizeMode == TextSizeMode.LARGE,
                    onClick = {
                        textSizeController.setMode(TextSizeMode.LARGE)
                    },
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCheckUpdates),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(
                        R.string.updater_more_label,
                        BuildConfig.VERSION_NAME,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.updater_check_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun CompactHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomikaBrandMark(modifier = Modifier.size(38.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.more_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.more_compact_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            ),
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun CompactSettingRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    supporting: String? = null,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = if (supporting == null) 8.dp else 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(7.dp).size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SelectorSettingRow(
    icon: ImageVector,
    title: String,
    choices: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(7.dp).size(18.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            choices()
        }
    }
}

@Composable
private fun TinyChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (selected) {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            )
        } else {
            null
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    )
}

private fun setHomikaLanguage(languageTag: String) {
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(languageTag),
    )
}
