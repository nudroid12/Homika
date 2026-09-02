package com.homiq.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object HomiqMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE properties ADD COLUMN bookingCode TEXT NOT NULL DEFAULT ''",
            )
            db.execSQL(
                "ALTER TABLE bookings ADD COLUMN bookingReference TEXT NOT NULL DEFAULT ''",
            )

            // Give existing properties a useful stable code during migration.
            // "Mamat Homestay" -> "MH"; a single-word name uses its first 2 characters.
            db.execSQL(
                """
                UPDATE properties
                SET bookingCode = UPPER(
                    CASE
                        WHEN instr(trim(name), ' ') > 0 THEN
                            substr(trim(name), 1, 1) ||
                            substr(
                                ltrim(substr(trim(name), instr(trim(name), ' ') + 1)),
                                1,
                                1
                            )
                        ELSE substr(trim(name), 1, 2)
                    END
                )
                WHERE bookingCode = ''
                """.trimIndent(),
            )

            // Existing bookings are backfilled once so their references are stable
            // immediately after upgrade, even if the property name/code changes later.
            db.execSQL(
                """
                UPDATE bookings
                SET bookingReference = (
                    SELECT p.bookingCode
                    FROM properties p
                    WHERE p.id = bookings.propertyId
                ) || '-' ||
                strftime(
                    '%d%m',
                    checkInEpochDay * 86400,
                    'unixepoch'
                ) || substr(
                    strftime(
                        '%Y',
                        checkInEpochDay * 86400,
                        'unixepoch'
                    ),
                    3,
                    2
                )
                WHERE bookingReference = ''
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
