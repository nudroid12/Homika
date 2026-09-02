package com.homiq.app.data

import android.content.Context
import com.homiq.app.data.account.AccountPreferences
import com.homiq.app.data.account.GoogleAccountService
import com.homiq.app.data.backup.AutoBackupService
import com.homiq.app.data.backup.BackupPreferences
import com.homiq.app.data.backup.DriveBackupService
import com.homiq.app.data.backup.GoogleDriveBackupClient
import com.homiq.app.data.backup.HomiqBackupService
import com.homiq.app.data.local.HomiqDatabase
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
import com.homiq.app.data.sync.GoogleDriveAuthorization
import com.homiq.app.data.sync.GoogleDriveRestClient
import com.homiq.app.data.sync.HomiqSyncEngine
import com.homiq.app.data.sync.HomiqSyncService
import com.homiq.app.data.sync.SyncChangeSignal
import com.homiq.app.data.sync.SyncPreferences
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

    val appLockService: AppLockService by lazy {
        AppLockService(appLockPreferences)
    }

    val syncPreferences: SyncPreferences by lazy {
        SyncPreferences(context)
    }

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

    private val driveAuthorization:
        GoogleDriveAuthorization by lazy {
        GoogleDriveAuthorization(context)
    }

    val accountService: GoogleAccountService by lazy {
        GoogleAccountService(
            authorization = driveAuthorization,
            preferences = accountPreferences,
            syncPreferences = syncPreferences,
            backupPreferences = backupPreferences,
        )
    }

    val driveBackupService: DriveBackupService by lazy {
        DriveBackupService(
            database = database,
            authorization = driveAuthorization,
            drive = GoogleDriveBackupClient(),
            accountPreferences = accountPreferences,
            backupPreferences = backupPreferences,
        )
    }

    val autoBackupService: AutoBackupService by lazy {
        AutoBackupService(
            driveBackup = driveBackupService,
            preferences = backupPreferences,
            accountPreferences = accountPreferences,
            changes = syncChanges,
        )
    }

    val updateManager: HomikaUpdateManager by lazy {
        HomikaUpdateManager(context)
    }

    val syncService: HomiqSyncService by lazy {
        HomiqSyncService(
            authorization = driveAuthorization,
            engine =
                HomiqSyncEngine(
                    database = database,
                    drive =
                        GoogleDriveRestClient(),
                    preferences =
                        syncPreferences,
                ),
            preferences = syncPreferences,
            changes = syncChanges,
            accountService = accountService,
        )
    }
}
