# Homika Local Data Model

Phase 2 establishes the first persistent business-data schema.

## Storage engine

Homika uses Room 2.8.4 over SQLite for the Android local database.

Database file:

`homiq.db`

Database version:

`1`

Room schema export is enabled through the Room Gradle plugin. Future database changes must add an explicit migration. Destructive migration is intentionally not enabled.

## Core rules

### Money

All money values are stored as integer sen using Kotlin `Long`.

Examples:

- RM1.00 = 100
- RM120.50 = 12050

Do not store business money as `Float` or `Double`.

### Stay dates

Booking stay ranges are half-open:

`[check-in, check-out)`

This means a guest checking out on 12 September does not block a new guest checking in on 12 September.

Dates are stored as `LocalDate.toEpochDay()` values in `Long` columns.

### Identity and sync readiness

Every business record has:

- Stable UUID string ID
- createdAtEpochMillis
- updatedAtEpochMillis
- revision
- isDeleted

`isDeleted` is a tombstone foundation for future sync. Repository deletion uses soft deletion.

### Security deposits

Deposit records are separate from payment/revenue records.

A booking can have at most one active deposit record in the database because `bookingId` is uniquely indexed in the deposit table.

## Tables

### properties

Owner-managed homestay units.

Important fields:

- id
- name
- address
- notes
- defaultNightlyRateSen
- isActive
- sync metadata

### bookings

Manual stay record.

Important fields:

- propertyId
- guestName
- guestPhone
- checkInEpochDay
- checkOutEpochDay
- source
- totalAmountSen
- status
- notes

### payments

Money received against a booking.

A booking can have multiple payments.

### deposits

Security deposit state for a booking.

Deposit is deliberately not merged into payment totals.

### expenses

Operating cost entry.

`propertyId` is optional, allowing a general expense that is not tied to one homestay.

### blocked_dates

Owner-created unavailable date ranges.

Ranges use the same half-open interval rule as bookings.

## Double-booking foundation

The booking DAO already exposes an overlap query using:

`existing.checkIn < new.checkOut AND existing.checkOut > new.checkIn`

Cancelled and soft-deleted bookings do not count as occupied.

Blocked dates expose equivalent overlap queries.

Phase 3 will combine these checks into booking validation before saving.

## Repository boundary

UI code must not talk directly to Room DAOs.

Target flow:

`UI -> ViewModel -> Repository -> DAO -> Room/SQLite`

Phase 2 provides repositories for:

- properties
- bookings
- payments
- deposits
- expenses
- blocked dates

## Migration policy

Version 1 is the initial schema, therefore no migration object exists yet.

When version 2 is introduced:

1. Add the migration to `HomiqMigrations`.
2. Keep schema export enabled.
3. Add a migration test.
4. Never use destructive fallback for production owner data.
