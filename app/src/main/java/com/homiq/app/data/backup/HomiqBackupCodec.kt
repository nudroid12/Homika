package com.homiq.app.data.backup

import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity
import com.homiq.app.data.model.BookingSource
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.data.model.DepositStatus
import com.homiq.app.data.model.ExpenseCategory
import com.homiq.app.data.model.PaymentMethod
import org.json.JSONArray
import org.json.JSONObject

object HomiqBackupCodec {
    const val MAGIC = "HOMIQ_BACKUP"
    const val FORMAT_VERSION = 1
    const val DATABASE_SCHEMA_VERSION = 2

    fun encode(
        snapshot: HomiqBackupSnapshot,
    ): String {
        val data = JSONObject()
            .put(
                "properties",
                JSONArray().apply {
                    snapshot.properties.forEach {
                        put(propertyToJson(it))
                    }
                },
            )
            .put(
                "bookings",
                JSONArray().apply {
                    snapshot.bookings.forEach {
                        put(bookingToJson(it))
                    }
                },
            )
            .put(
                "payments",
                JSONArray().apply {
                    snapshot.payments.forEach {
                        put(paymentToJson(it))
                    }
                },
            )
            .put(
                "deposits",
                JSONArray().apply {
                    snapshot.deposits.forEach {
                        put(depositToJson(it))
                    }
                },
            )
            .put(
                "expenses",
                JSONArray().apply {
                    snapshot.expenses.forEach {
                        put(expenseToJson(it))
                    }
                },
            )
            .put(
                "blockedDates",
                JSONArray().apply {
                    snapshot.blockedDates.forEach {
                        put(blockedDateToJson(it))
                    }
                },
            )

        return JSONObject()
            .put("magic", MAGIC)
            .put("formatVersion", FORMAT_VERSION)
            .put(
                "databaseSchemaVersion",
                DATABASE_SCHEMA_VERSION,
            )
            .put(
                "createdAtEpochMillis",
                snapshot.createdAtEpochMillis,
            )
            .put("data", data)
            .toString(2)
    }

    fun decode(
        raw: String,
    ): HomiqBackupSnapshot {
        val root = JSONObject(raw)

        require(root.getString("magic") == MAGIC) {
            "Not a HOMIQ backup."
        }

        val formatVersion =
            root.getInt("formatVersion")
        require(
            formatVersion <= FORMAT_VERSION,
        ) {
            "Unsupported backup format."
        }

        val databaseVersion =
            root.getInt("databaseSchemaVersion")
        require(
            databaseVersion <= DATABASE_SCHEMA_VERSION,
        ) {
            "Backup database version is newer."
        }

        val data = root.getJSONObject("data")

        return HomiqBackupSnapshot(
            createdAtEpochMillis =
                root.getLong("createdAtEpochMillis"),
            properties = data
                .getJSONArray("properties")
                .mapObjects(::propertyFromJson),
            bookings = data
                .getJSONArray("bookings")
                .mapObjects(::bookingFromJson),
            payments = data
                .getJSONArray("payments")
                .mapObjects(::paymentFromJson),
            deposits = data
                .getJSONArray("deposits")
                .mapObjects(::depositFromJson),
            expenses = data
                .getJSONArray("expenses")
                .mapObjects(::expenseFromJson),
            blockedDates = data
                .getJSONArray("blockedDates")
                .mapObjects(::blockedDateFromJson),
        )
    }

    fun preview(
        snapshot: HomiqBackupSnapshot,
    ): BackupPreview =
        BackupPreview(
            createdAtEpochMillis =
                snapshot.createdAtEpochMillis,
            propertyCount =
                snapshot.properties.size,
            bookingCount =
                snapshot.bookings.size,
            paymentCount =
                snapshot.payments.size,
            depositCount =
                snapshot.deposits.size,
            expenseCount =
                snapshot.expenses.size,
            blockedDateCount =
                snapshot.blockedDates.size,
        )

    private fun propertyToJson(
        value: PropertyEntity,
    ): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("name", value.name)
            .put("bookingCode", value.bookingCode)
            .putNullable("address", value.address)
            .putNullable("notes", value.notes)
            .put(
                "defaultNightlyRateSen",
                value.defaultNightlyRateSen,
            )
            .put("isActive", value.isActive)
            .put(
                "createdAtEpochMillis",
                value.createdAtEpochMillis,
            )
            .put(
                "updatedAtEpochMillis",
                value.updatedAtEpochMillis,
            )
            .put("revision", value.revision)
            .put("isDeleted", value.isDeleted)

    private fun propertyFromJson(
        value: JSONObject,
    ): PropertyEntity =
        PropertyEntity(
            id = value.getString("id"),
            name = value.getString("name"),
            bookingCode = value.optString("bookingCode", ""),
            address =
                value.nullableString("address"),
            notes =
                value.nullableString("notes"),
            defaultNightlyRateSen =
                value.getLong(
                    "defaultNightlyRateSen",
                ),
            isActive =
                value.getBoolean("isActive"),
            createdAtEpochMillis =
                value.getLong(
                    "createdAtEpochMillis",
                ),
            updatedAtEpochMillis =
                value.getLong(
                    "updatedAtEpochMillis",
                ),
            revision =
                value.getLong("revision"),
            isDeleted =
                value.getBoolean("isDeleted"),
        )

    private fun bookingToJson(
        value: BookingEntity,
    ): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("propertyId", value.propertyId)
            .put("bookingReference", value.bookingReference)
            .put("guestName", value.guestName)
            .putNullable(
                "guestPhone",
                value.guestPhone,
            )
            .put(
                "checkInEpochDay",
                value.checkInEpochDay,
            )
            .put(
                "checkOutEpochDay",
                value.checkOutEpochDay,
            )
            .put("source", value.source.name)
            .put(
                "totalAmountSen",
                value.totalAmountSen,
            )
            .put("status", value.status.name)
            .putNullable("notes", value.notes)
            .put(
                "createdAtEpochMillis",
                value.createdAtEpochMillis,
            )
            .put(
                "updatedAtEpochMillis",
                value.updatedAtEpochMillis,
            )
            .put("revision", value.revision)
            .put("isDeleted", value.isDeleted)

    private fun bookingFromJson(
        value: JSONObject,
    ): BookingEntity =
        BookingEntity(
            id = value.getString("id"),
            propertyId =
                value.getString("propertyId"),
            bookingReference =
                value.optString("bookingReference", ""),
            guestName =
                value.getString("guestName"),
            guestPhone =
                value.nullableString("guestPhone"),
            checkInEpochDay =
                value.getLong("checkInEpochDay"),
            checkOutEpochDay =
                value.getLong("checkOutEpochDay"),
            source =
                enumValueOf<BookingSource>(
                    value.getString("source"),
                ),
            totalAmountSen =
                value.getLong("totalAmountSen"),
            status =
                enumValueOf<BookingStatus>(
                    value.getString("status"),
                ),
            notes =
                value.nullableString("notes"),
            createdAtEpochMillis =
                value.getLong(
                    "createdAtEpochMillis",
                ),
            updatedAtEpochMillis =
                value.getLong(
                    "updatedAtEpochMillis",
                ),
            revision =
                value.getLong("revision"),
            isDeleted =
                value.getBoolean("isDeleted"),
        )

    private fun paymentToJson(
        value: PaymentEntity,
    ): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("bookingId", value.bookingId)
            .put("amountSen", value.amountSen)
            .put(
                "paymentDateEpochDay",
                value.paymentDateEpochDay,
            )
            .put("method", value.method.name)
            .putNullable("notes", value.notes)
            .put(
                "createdAtEpochMillis",
                value.createdAtEpochMillis,
            )
            .put(
                "updatedAtEpochMillis",
                value.updatedAtEpochMillis,
            )
            .put("revision", value.revision)
            .put("isDeleted", value.isDeleted)

    private fun paymentFromJson(
        value: JSONObject,
    ): PaymentEntity =
        PaymentEntity(
            id = value.getString("id"),
            bookingId =
                value.getString("bookingId"),
            amountSen =
                value.getLong("amountSen"),
            paymentDateEpochDay =
                value.getLong(
                    "paymentDateEpochDay",
                ),
            method =
                enumValueOf<PaymentMethod>(
                    value.getString("method"),
                ),
            notes =
                value.nullableString("notes"),
            createdAtEpochMillis =
                value.getLong(
                    "createdAtEpochMillis",
                ),
            updatedAtEpochMillis =
                value.getLong(
                    "updatedAtEpochMillis",
                ),
            revision =
                value.getLong("revision"),
            isDeleted =
                value.getBoolean("isDeleted"),
        )

    private fun depositToJson(
        value: DepositEntity,
    ): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("bookingId", value.bookingId)
            .put("amountSen", value.amountSen)
            .put("status", value.status.name)
            .putNullable(
                "receivedAtEpochDay",
                value.receivedAtEpochDay,
            )
            .put(
                "returnedAmountSen",
                value.returnedAmountSen,
            )
            .putNullable(
                "returnedAtEpochDay",
                value.returnedAtEpochDay,
            )
            .putNullable("notes", value.notes)
            .put(
                "createdAtEpochMillis",
                value.createdAtEpochMillis,
            )
            .put(
                "updatedAtEpochMillis",
                value.updatedAtEpochMillis,
            )
            .put("revision", value.revision)
            .put("isDeleted", value.isDeleted)

    private fun depositFromJson(
        value: JSONObject,
    ): DepositEntity =
        DepositEntity(
            id = value.getString("id"),
            bookingId =
                value.getString("bookingId"),
            amountSen =
                value.getLong("amountSen"),
            status =
                enumValueOf<DepositStatus>(
                    value.getString("status"),
                ),
            receivedAtEpochDay =
                value.nullableLong(
                    "receivedAtEpochDay",
                ),
            returnedAmountSen =
                value.getLong(
                    "returnedAmountSen",
                ),
            returnedAtEpochDay =
                value.nullableLong(
                    "returnedAtEpochDay",
                ),
            notes =
                value.nullableString("notes"),
            createdAtEpochMillis =
                value.getLong(
                    "createdAtEpochMillis",
                ),
            updatedAtEpochMillis =
                value.getLong(
                    "updatedAtEpochMillis",
                ),
            revision =
                value.getLong("revision"),
            isDeleted =
                value.getBoolean("isDeleted"),
        )

    private fun expenseToJson(
        value: ExpenseEntity,
    ): JSONObject =
        JSONObject()
            .put("id", value.id)
            .putNullable(
                "propertyId",
                value.propertyId,
            )
            .put("amountSen", value.amountSen)
            .put(
                "expenseDateEpochDay",
                value.expenseDateEpochDay,
            )
            .put("category", value.category.name)
            .putNullable(
                "description",
                value.description,
            )
            .putNullable("notes", value.notes)
            .put(
                "createdAtEpochMillis",
                value.createdAtEpochMillis,
            )
            .put(
                "updatedAtEpochMillis",
                value.updatedAtEpochMillis,
            )
            .put("revision", value.revision)
            .put("isDeleted", value.isDeleted)

    private fun expenseFromJson(
        value: JSONObject,
    ): ExpenseEntity =
        ExpenseEntity(
            id = value.getString("id"),
            propertyId =
                value.nullableString("propertyId"),
            amountSen =
                value.getLong("amountSen"),
            expenseDateEpochDay =
                value.getLong(
                    "expenseDateEpochDay",
                ),
            category =
                enumValueOf<ExpenseCategory>(
                    value.getString("category"),
                ),
            description =
                value.nullableString("description"),
            notes =
                value.nullableString("notes"),
            createdAtEpochMillis =
                value.getLong(
                    "createdAtEpochMillis",
                ),
            updatedAtEpochMillis =
                value.getLong(
                    "updatedAtEpochMillis",
                ),
            revision =
                value.getLong("revision"),
            isDeleted =
                value.getBoolean("isDeleted"),
        )

    private fun blockedDateToJson(
        value: BlockedDateEntity,
    ): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("propertyId", value.propertyId)
            .put(
                "startEpochDay",
                value.startEpochDay,
            )
            .put(
                "endEpochDay",
                value.endEpochDay,
            )
            .putNullable("reason", value.reason)
            .put(
                "createdAtEpochMillis",
                value.createdAtEpochMillis,
            )
            .put(
                "updatedAtEpochMillis",
                value.updatedAtEpochMillis,
            )
            .put("revision", value.revision)
            .put("isDeleted", value.isDeleted)

    private fun blockedDateFromJson(
        value: JSONObject,
    ): BlockedDateEntity =
        BlockedDateEntity(
            id = value.getString("id"),
            propertyId =
                value.getString("propertyId"),
            startEpochDay =
                value.getLong("startEpochDay"),
            endEpochDay =
                value.getLong("endEpochDay"),
            reason =
                value.nullableString("reason"),
            createdAtEpochMillis =
                value.getLong(
                    "createdAtEpochMillis",
                ),
            updatedAtEpochMillis =
                value.getLong(
                    "updatedAtEpochMillis",
                ),
            revision =
                value.getLong("revision"),
            isDeleted =
                value.getBoolean("isDeleted"),
        )

    private fun JSONObject.putNullable(
        key: String,
        value: Any?,
    ): JSONObject =
        put(
            key,
            value ?: JSONObject.NULL,
        )

    private fun JSONObject.nullableString(
        key: String,
    ): String? =
        if (isNull(key)) {
            null
        } else {
            getString(key)
        }

    private fun JSONObject.nullableLong(
        key: String,
    ): Long? =
        if (isNull(key)) {
            null
        } else {
            getLong(key)
        }

    private fun <T> JSONArray.mapObjects(
        mapper: (JSONObject) -> T,
    ): List<T> =
        List(length()) { index ->
            mapper(getJSONObject(index))
        }
}
