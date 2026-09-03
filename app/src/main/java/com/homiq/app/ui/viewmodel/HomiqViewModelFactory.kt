package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.homiq.app.data.HomiqAppContainer
import com.homiq.app.domain.BlockedDateManager
import com.homiq.app.domain.BookingManager
import com.homiq.app.domain.DepositManager
import com.homiq.app.domain.ExpenseManager
import com.homiq.app.domain.PaymentManager

class HomiqViewModelFactory(
    private val container: HomiqAppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
    ): T {
        return when {
            modelClass.isAssignableFrom(
                LicenseViewModel::class.java,
            ) ->
                LicenseViewModel(
                    repository =
                        container.licenseRepository,
                ) as T

            modelClass.isAssignableFrom(
                AccountViewModel::class.java,
            ) ->
                AccountViewModel(
                    accountPreferences =
                        container.accountPreferences,
                ) as T

            modelClass.isAssignableFrom(
                AppLockViewModel::class.java,
            ) ->
                AppLockViewModel(
                    service =
                        container
                            .appLockService,
                ) as T

            modelClass.isAssignableFrom(
                PropertyViewModel::class.java,
            ) ->
                PropertyViewModel(
                    properties =
                        container.properties,
                    bookings =
                        container.bookings,
                    expenses =
                        container.expenses,
                    blockedDates =
                        container.blockedDates,
                ) as T

            modelClass.isAssignableFrom(
                BookingViewModel::class.java,
            ) ->
                BookingViewModel(
                    properties =
                        container.properties,
                    bookings =
                        container.bookings,
                    blockedDates =
                        container.blockedDates,
                    bookingManager =
                        BookingManager(
                            properties =
                                container.properties,
                            bookings =
                                container.bookings,
                            blockedDates =
                                container.blockedDates,
                        ),
                ) as T

            modelClass.isAssignableFrom(
                CalendarViewModel::class.java,
            ) ->
                CalendarViewModel(
                    properties =
                        container.properties,
                    bookings =
                        container.bookings,
                    blockedDates =
                        container.blockedDates,
                ) as T

            modelClass.isAssignableFrom(
                BlockedDateViewModel::class.java,
            ) ->
                BlockedDateViewModel(
                    properties =
                        container.properties,
                    manager =
                        BlockedDateManager(
                            properties =
                                container.properties,
                            bookings =
                                container.bookings,
                            blockedDates =
                                container.blockedDates,
                        ),
                ) as T

            modelClass.isAssignableFrom(
                FinanceViewModel::class.java,
            ) ->
                FinanceViewModel(
                    payments =
                        container.payments,
                    deposits =
                        container.deposits,
                    paymentManager =
                        PaymentManager(
                            bookings =
                                container.bookings,
                            payments =
                                container.payments,
                        ),
                    depositManager =
                        DepositManager(
                            bookings =
                                container.bookings,
                            deposits =
                                container.deposits,
                        ),
                ) as T

            modelClass.isAssignableFrom(
                MoneyViewModel::class.java,
            ) ->
                MoneyViewModel(
                    propertyRepository =
                        container.properties,
                    bookingRepository =
                        container.bookings,
                    expenseRepository =
                        container.expenses,
                    expenseManager =
                        ExpenseManager(
                            properties =
                                container.properties,
                            expenses =
                                container.expenses,
                        ),
                ) as T

            modelClass.isAssignableFrom(
                DashboardViewModel::class.java,
            ) ->
                DashboardViewModel(
                    properties =
                        container.properties,
                    bookings =
                        container.bookings,
                    blockedDates =
                        container.blockedDates,
                    expenses =
                        container.expenses,
                ) as T

            modelClass.isAssignableFrom(
                ReportsViewModel::class.java,
            ) ->
                ReportsViewModel(
                    properties =
                        container.properties,
                    bookings =
                        container.bookings,
                    blockedDates =
                        container.blockedDates,
                    expenses =
                        container.expenses,
                ) as T

            modelClass.isAssignableFrom(
                BackupViewModel::class.java,
            ) ->
                BackupViewModel(
                    service =
                        container.backupService,
                    backupPreferences =
                        container.backupPreferences,
                    cloudService =
                        container.cloudBackupService,
                    autoBackupCoordinator =
                        container.cloudAutoBackupCoordinator,
                    cloudSyncCoordinator =
                        container.cloudSnapshotSyncCoordinator,
                ) as T

            else -> error(
                "Unknown HOMIQ ViewModel: ${modelClass.name}",
            )
        }
    }
}
