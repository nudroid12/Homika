package com.homiq.app.data

import android.content.Context
import com.homiq.app.data.account.AccountPreferences
import com.homiq.app.data.backup.BackupPreferences
import com.homiq.app.data.backup.HomiqBackupService
import com.homiq.app.data.cloud.CloudAutoBackupCoordinator
import com.homiq.app.data.cloud.HomikaCloudBackupService
import com.homiq.app.data.local.HomiqDatabase
import com.homiq.app.data.license.LicenseRepository
import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.DepositRepository
import com.homiq.app.data.repository.ExpenseRepository
import com.homiq.app.data.repository.PaymentRepository
import com.homiq.app.data.repository.PropertyRepository
import com.homiq.app.data.repository.RoomBlockedDateRepository
import com.homiq.app.data.repository.RoomBookingRepository
import com.homiq.app.data.repository.RoomDepositRepository
import com.homiq.app.data.repository.RoomExpenseRepository
import com.homiq.app.data.repository.RoomPaymentRepository
import com.homiq.app.data.repository.RoomPropertyRepository
import com.homiq.app.data.security.AppLockPreferences
import com.homiq.app.data.security.AppLockService
import com.homiq.app.data.sync.SyncChangeSignal
import com.homiq.app.data.update.HomikaUpdateManager

class HomiqAppContainer(
    context: Context,
) {
    val database: HomiqDatabase by lazy {
        HomiqDatabase.create(context)
    }

    val appLockPreferences: AppLockPreferences by lazy {
        AppLockPreferences(context)
    }

    val licenseRepository: LicenseRepository by lazy {
        LicenseRepository(context)
    }

    val appLockService: AppLockService by lazy {
        AppLockService(appLockPreferences)
    }

    // Kept as a generic local change signal for the future Homika Cloud Sync layer.
    val syncChanges: SyncChangeSignal by lazy {
        SyncChangeSignal()
    }

    val properties: PropertyRepository by lazy {
        RoomPropertyRepository(
            dao = database.propertyDao(),
            onChanged = syncChanges::notifyChanged,
        )
    }

    val bookings: BookingRepository by lazy {
        RoomBookingRepository(
            dao = database.bookingDao(),
            onChanged = syncChanges::notifyChanged,
        )
    }

    val payments: PaymentRepository by lazy {
        RoomPaymentRepository(
            dao = database.paymentDao(),
            onChanged = syncChanges::notifyChanged,
        )
    }

    val deposits: DepositRepository by lazy {
        RoomDepositRepository(
            dao = database.depositDao(),
            onChanged = syncChanges::notifyChanged,
        )
    }

    val expenses: ExpenseRepository by lazy {
        RoomExpenseRepository(
            dao = database.expenseDao(),
            onChanged = syncChanges::notifyChanged,
        )
    }

    val blockedDates: BlockedDateRepository by lazy {
        RoomBlockedDateRepository(
            dao = database.blockedDateDao(),
            onChanged = syncChanges::notifyChanged,
        )
    }

    val accountPreferences: AccountPreferences by lazy {
        AccountPreferences(context)
    }

    val backupPreferences: BackupPreferences by lazy {
        BackupPreferences(context)
    }

    val backupService: HomiqBackupService by lazy {
        HomiqBackupService(
            context = context,
            database = database,
        )
    }

    val cloudBackupService: HomikaCloudBackupService by lazy {
        HomikaCloudBackupService(
            backupService = backupService,
            licenseRepository = licenseRepository,
        )
    }

    val cloudAutoBackupCoordinator: CloudAutoBackupCoordinator by lazy {
        CloudAutoBackupCoordinator(
            changes = syncChanges,
            cloudService = cloudBackupService,
            preferences = backupPreferences,
        )
    }

    fun startBackgroundServices() {
        cloudAutoBackupCoordinator.start()
    }

    val updateManager: HomikaUpdateManager by lazy {
        HomikaUpdateManager(context)
    }
}
