package com.homiq.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.homiq.app.data.local.dao.BackupDao
import com.homiq.app.data.local.dao.BlockedDateDao
import com.homiq.app.data.local.dao.BookingDao
import com.homiq.app.data.local.dao.DepositDao
import com.homiq.app.data.local.dao.ExpenseDao
import com.homiq.app.data.local.dao.PaymentDao
import com.homiq.app.data.local.dao.PropertyDao
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity

@Database(
    entities = [
        PropertyEntity::class,
        BookingEntity::class,
        PaymentEntity::class,
        DepositEntity::class,
        ExpenseEntity::class,
        BlockedDateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(HomiqTypeConverters::class)
abstract class HomiqDatabase : RoomDatabase() {
    abstract fun backupDao(): BackupDao
    abstract fun propertyDao(): PropertyDao
    abstract fun bookingDao(): BookingDao
    abstract fun paymentDao(): PaymentDao
    abstract fun depositDao(): DepositDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun blockedDateDao(): BlockedDateDao

    companion object {
        const val DATABASE_NAME = "homiq.db"

        fun create(context: Context): HomiqDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                HomiqDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(*HomiqMigrations.ALL)
                .build()
    }
}
