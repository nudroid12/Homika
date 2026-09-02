package com.homiq.app.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homiq.app.BuildConfig
import com.homiq.app.HomiqApplication
import com.homiq.app.R
import com.homiq.app.data.license.LicenseAccess
import com.homiq.app.data.preferences.OnboardingPreferences
import com.homiq.app.ui.components.HomikaUpdateDialog
import com.homiq.app.ui.screens.AppLockScreen
import com.homiq.app.ui.screens.BackupScreen
import com.homiq.app.ui.screens.BlockDateFormScreen
import com.homiq.app.ui.screens.BookingDetailScreen
import com.homiq.app.ui.screens.BookingFormScreen
import com.homiq.app.ui.screens.BookingsScreen
import com.homiq.app.ui.screens.CalendarScreen
import com.homiq.app.ui.screens.DepositScreen
import com.homiq.app.ui.screens.ExpenseFormScreen
import com.homiq.app.ui.screens.HomeScreen
import com.homiq.app.ui.screens.LicenseActivationScreen
import com.homiq.app.ui.screens.LicenseManagementScreen
import com.homiq.app.ui.screens.MoneyScreen
import com.homiq.app.ui.screens.MoreScreen
import com.homiq.app.ui.screens.OnboardingScreen
import com.homiq.app.ui.screens.PropertiesScreen
import com.homiq.app.ui.screens.PropertyFormScreen
import com.homiq.app.ui.screens.ReportsScreen
import com.homiq.app.ui.screens.SecurityScreen
import com.homiq.app.ui.viewmodel.AppLockViewModel
import com.homiq.app.ui.viewmodel.BackupViewModel
import com.homiq.app.ui.viewmodel.BlockedDateViewModel
import com.homiq.app.ui.viewmodel.BookingViewModel
import com.homiq.app.ui.viewmodel.CalendarViewModel
import com.homiq.app.ui.viewmodel.DashboardViewModel
import com.homiq.app.ui.viewmodel.FinanceViewModel
import com.homiq.app.ui.viewmodel.HomiqViewModelFactory
import com.homiq.app.ui.viewmodel.LicenseViewModel
import com.homiq.app.ui.viewmodel.MoneyViewModel
import com.homiq.app.ui.viewmodel.PropertyViewModel
import com.homiq.app.ui.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch

private enum class HomiqDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(R.string.nav_home, Icons.Outlined.Home),
    Calendar(R.string.nav_calendar, Icons.Outlined.CalendarMonth),
    Bookings(R.string.nav_bookings, Icons.Outlined.ListAlt),
    Money(R.string.nav_money, Icons.Outlined.AccountBalanceWallet),
    More(R.string.nav_more, Icons.Outlined.MoreHoriz),
}

private enum class HomiqRoute {
    MAIN,
    PROPERTIES,
    PROPERTY_FORM,
    BOOKING_FORM,
    BOOKING_DETAIL,
    BLOCK_DATE_FORM,
    DEPOSIT,
    EXPENSE_FORM,
    REPORTS,
    BACKUP,
    SECURITY,
    LICENSE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomiqApp() {
    val application = LocalContext.current.applicationContext as HomiqApplication
    val factory = remember(application) { HomiqViewModelFactory(application.container) }

    val licenseViewModel: LicenseViewModel = viewModel(factory = factory)
    val licenseState by licenseViewModel.state.collectAsStateWithLifecycle()
    if (licenseState.access != LicenseAccess.ACTIVE) {
        LicenseActivationScreen(
            state = licenseState,
            onActivate = licenseViewModel::activate,
            onRetry = licenseViewModel::retry,
        )
        return
    }

    val appLockViewModel: AppLockViewModel = viewModel(factory = factory)
    val appLockState by appLockViewModel.state.collectAsStateWithLifecycle()
    if (appLockState.locked) {
        AppLockScreen(viewModel = appLockViewModel)
        return
    }

    val propertyViewModel: PropertyViewModel = viewModel(factory = factory)
    val bookingViewModel: BookingViewModel = viewModel(factory = factory)
    val calendarViewModel: CalendarViewModel = viewModel(factory = factory)
    val blockedDateViewModel: BlockedDateViewModel = viewModel(factory = factory)
    val financeViewModel: FinanceViewModel = viewModel(factory = factory)
    val moneyViewModel: MoneyViewModel = viewModel(factory = factory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val reportsViewModel: ReportsViewModel = viewModel(factory = factory)
    val backupViewModel: BackupViewModel = viewModel(factory = factory)
    val updateManager = remember(application) { application.container.updateManager }
    val updateState by updateManager.state.collectAsStateWithLifecycle()

    val onboardingPreferences = remember(application) { OnboardingPreferences(application) }
    var onboardingComplete by rememberSaveable { mutableStateOf(onboardingPreferences.isComplete) }

    var destinationName by rememberSaveable { mutableStateOf(HomiqDestination.Home.name) }
    var routeName by rememberSaveable { mutableStateOf(HomiqRoute.MAIN.name) }
    var routeId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var routePropertyId by rememberSaveable { mutableStateOf<String?>(null) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }

    if (!onboardingComplete) {
        OnboardingScreen(
            appLockViewModel = appLockViewModel,
            onFinished = {
                onboardingPreferences.complete()
                onboardingComplete = true
                destinationName = HomiqDestination.Home.name
                routeName = HomiqRoute.MAIN.name
                routeId = null
            },
        )
        return
    }

    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete) {
            updateManager.checkForUpdates(manual = false)
        }
    }

    val selectedDestination = remember(destinationName) {
        HomiqDestination.entries.firstOrNull { it.name == destinationName } ?: HomiqDestination.Home
    }
    val route = remember(routeName) {
        HomiqRoute.entries.firstOrNull { it.name == routeName } ?: HomiqRoute.MAIN
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalContext.current as? android.app.Activity
    var lastBackPressMillis by rememberSaveable { mutableStateOf(0L) }

    fun navigate(
        newRoute: HomiqRoute,
        id: String? = null,
        epochDay: Long? = null,
        propertyId: String? = null,
    ) {
        routeName = newRoute.name
        routeId = id
        routeEpochDay = epochDay
        routePropertyId = propertyId
    }

    fun goMain(destination: HomiqDestination? = null) {
        if (destination != null) destinationName = destination.name
        navigate(HomiqRoute.MAIN)
    }

    BackHandler {
        if (route != HomiqRoute.MAIN) {
            when (route) {
                HomiqRoute.PROPERTY_FORM -> navigate(HomiqRoute.PROPERTIES)
                HomiqRoute.BOOKING_FORM -> {
                    if (routeId != null) navigate(HomiqRoute.BOOKING_DETAIL, id = routeId)
                    else goMain(HomiqDestination.Calendar)
                }
                HomiqRoute.BOOKING_DETAIL -> goMain(HomiqDestination.Bookings)
                HomiqRoute.BLOCK_DATE_FORM -> goMain(HomiqDestination.Calendar)
                HomiqRoute.DEPOSIT -> navigate(HomiqRoute.BOOKING_DETAIL, id = routeId)
                HomiqRoute.EXPENSE_FORM -> goMain(HomiqDestination.Money)
                HomiqRoute.REPORTS -> goMain(HomiqDestination.Home)
                HomiqRoute.BACKUP,
                HomiqRoute.SECURITY,
                HomiqRoute.LICENSE,
                HomiqRoute.PROPERTIES -> goMain(HomiqDestination.More)
                HomiqRoute.MAIN -> Unit
            }
        } else if (selectedDestination != HomiqDestination.Home) {
            destinationName = HomiqDestination.Home.name
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressMillis <= 2000L) {
                activity?.finish()
            } else {
                lastBackPressMillis = now
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Press back again to exit",
                    )
                }
            }
        }
    }


    if (route == HomiqRoute.MAIN) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                HomikaBottomBar(
                    selected = selectedDestination,
                    onSelected = { destinationName = it.name },
                )
            },
            floatingActionButton = {
                if (selectedDestination != HomiqDestination.More) {
                    SmallFloatingActionButton(
                        onClick = { showQuickAdd = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.quick_add))
                    }
                }
            },
        ) { innerPadding ->
            when (selectedDestination) {
                HomiqDestination.Home -> HomeScreen(
                    viewModel = dashboardViewModel,
                    onBookingClick = { navigate(HomiqRoute.BOOKING_DETAIL, id = it) },
                    onReportsClick = { navigate(HomiqRoute.REPORTS) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqDestination.Calendar -> CalendarScreen(
                    viewModel = calendarViewModel,
                    onBookingClick = { navigate(HomiqRoute.BOOKING_DETAIL, id = it) },
                    onNewBooking = { day, propertyId -> navigate(HomiqRoute.BOOKING_FORM, epochDay = day, propertyId = propertyId) },
                    onBlockDate = { day, propertyId -> navigate(HomiqRoute.BLOCK_DATE_FORM, epochDay = day, propertyId = propertyId) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqDestination.Bookings -> BookingsScreen(
                    viewModel = bookingViewModel,
                    onBookingClick = { navigate(HomiqRoute.BOOKING_DETAIL, id = it) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqDestination.Money -> MoneyScreen(
                    viewModel = moneyViewModel,
                    onAddExpense = { navigate(HomiqRoute.EXPENSE_FORM) },
                    onReportsClick = { navigate(HomiqRoute.REPORTS) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqDestination.More -> MoreScreen(
                    onPropertiesClick = { navigate(HomiqRoute.PROPERTIES) },
                    onBackupClick = { navigate(HomiqRoute.BACKUP) },
                    onLicenseClick = { navigate(HomiqRoute.LICENSE) },
                    appLockEnabled = appLockState.hasPin,
                    onSecurityClick = { navigate(HomiqRoute.SECURITY) },
                    onCheckUpdates = { updateManager.checkForUpdates(manual = true) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    } else {
        Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
            when (route) {
                HomiqRoute.PROPERTIES -> PropertiesScreen(
                    viewModel = propertyViewModel,
                    onAddProperty = { navigate(HomiqRoute.PROPERTY_FORM) },
                    onPropertyClick = { navigate(HomiqRoute.PROPERTY_FORM, id = it) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqRoute.PROPERTY_FORM -> PropertyFormScreen(
                    propertyId = routeId,
                    viewModel = propertyViewModel,
                    onSaved = { navigate(HomiqRoute.PROPERTIES) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqRoute.BOOKING_FORM -> BookingFormScreen(
                    bookingId = routeId,
                    viewModel = bookingViewModel,
                    onSaved = { goMain(HomiqDestination.Calendar) },
                    onNeedProperty = { navigate(HomiqRoute.PROPERTY_FORM) },
                    modifier = Modifier.padding(innerPadding),
                    initialCheckInEpochDay = routeEpochDay,
                    initialPropertyId = routePropertyId,
                )
                HomiqRoute.BOOKING_DETAIL -> {
                    val id = routeId
                    if (id != null) {
                        BookingDetailScreen(
                            bookingId = id,
                            viewModel = bookingViewModel,
                            financeViewModel = financeViewModel,
                            onEdit = { navigate(HomiqRoute.BOOKING_FORM, id = id) },
                            onCancelled = { goMain(HomiqDestination.Calendar) },
                            onManageDeposit = { navigate(HomiqRoute.DEPOSIT, id = id) },
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
                HomiqRoute.BLOCK_DATE_FORM -> BlockDateFormScreen(
                    viewModel = blockedDateViewModel,
                    onSaved = { goMain(HomiqDestination.Calendar) },
                    onNeedProperty = { navigate(HomiqRoute.PROPERTY_FORM) },
                    modifier = Modifier.padding(innerPadding),
                    initialStartEpochDay = routeEpochDay,
                    initialPropertyId = routePropertyId,
                )
                HomiqRoute.DEPOSIT -> {
                    val id = routeId
                    if (id != null) {
                        DepositScreen(
                            bookingId = id,
                            bookingViewModel = bookingViewModel,
                            financeViewModel = financeViewModel,
                            onDone = { navigate(HomiqRoute.BOOKING_DETAIL, id = id) },
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
                HomiqRoute.EXPENSE_FORM -> ExpenseFormScreen(
                    viewModel = moneyViewModel,
                    onSaved = { goMain(HomiqDestination.Money) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqRoute.REPORTS -> ReportsScreen(viewModel = reportsViewModel, modifier = Modifier.padding(innerPadding))
                HomiqRoute.BACKUP -> BackupScreen(viewModel = backupViewModel, modifier = Modifier.padding(innerPadding))
                HomiqRoute.SECURITY -> SecurityScreen(viewModel = appLockViewModel, modifier = Modifier.padding(innerPadding))
                HomiqRoute.LICENSE -> LicenseManagementScreen(
                    state = licenseState,
                    onRefresh = licenseViewModel::refreshNow,
                    onDeactivate = licenseViewModel::deactivate,
                    onBack = { goMain(HomiqDestination.More) },
                    modifier = Modifier.padding(innerPadding),
                )
                HomiqRoute.MAIN -> Unit
            }
        }
    }

    if (showQuickAdd) {
        ModalBottomSheet(onDismissRequest = { showQuickAdd = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.quick_add), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.quick_add_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QuickActionRow(Icons.Outlined.EventNote, stringResource(R.string.new_booking)) {
                    showQuickAdd = false
                    navigate(HomiqRoute.BOOKING_FORM)
                }
                QuickActionRow(Icons.Outlined.ReceiptLong, stringResource(R.string.add_expense)) {
                    showQuickAdd = false
                    navigate(HomiqRoute.EXPENSE_FORM)
                }
                QuickActionRow(Icons.Outlined.Block, stringResource(R.string.block_date)) {
                    showQuickAdd = false
                    navigate(HomiqRoute.BLOCK_DATE_FORM)
                }
            }
        }
    }

    HomikaUpdateDialog(
        state = updateState,
        currentVersion = BuildConfig.VERSION_NAME,
        onDismiss = updateManager::dismiss,
        onDownload = {
            val available = updateState as? com.homiq.app.data.update.UpdateState.Available
            if (available != null) {
                updateManager.download(available.release)
            }
        },
        onInstall = updateManager::install,
        onOpenInstallSettings = updateManager::openInstallPermissionSettings,
    )
}

@Composable
private fun HomikaBottomBar(
    selected: HomiqDestination,
    onSelected: (HomiqDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        HomiqDestination.entries.forEach { destination ->
            val active = destination == selected
            NavigationBarItem(
                selected = active,
                onClick = { onSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        leadingContent = {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(9.dp),
                )
            }
        },
    )
}
