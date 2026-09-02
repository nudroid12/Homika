# Homika Backup and Restore

Phase 8 implements portable, local-first backup and restore.

## Backup format

Homika exports a versioned JSON document.

Default file name:

`Homika-backup-YYYY-MM-DD-HHmm.homiq.json`

The backup contains:

- Format metadata
- Database schema version
- Backup creation time
- Properties
- Bookings
- Payments
- Deposits
- Expenses
- Blocked dates

Stable IDs, timestamps, revisions and tombstone fields are preserved.

This is important for later sync readiness.

## Why JSON instead of copying SQLite

Homika deliberately does not copy the raw SQLite database file.

A logical backup is safer because:

- Restore does not depend on WAL/SHM file state.
- Restore does not require closing and restarting Room.
- The file can be validated before touching the live database.
- Future backup-format migration can be independent from SQLite internals.
- A human-readable format is easier to diagnose if recovery is ever needed.

## Creating a backup

Flow:

`More -> Backup & Restore -> Create Backup`

Android opens the system Storage Access Framework picker.

The owner chooses the destination.

Possible destinations depend on the device's installed DocumentsProvider apps and can include:

- Local storage
- SD/external storage
- Google Drive
- Other cloud document providers

No broad storage permission is requested.

For Google Drive, the owner selects Drive and the intended Google account in the Android picker.

Homika never receives the Google password.

## Snapshot consistency

Backup reads all six business tables inside one Room transaction.

This produces one consistent logical snapshot.

Deleted/tombstone rows are included because they are part of future sync state.

## Restore validation

Before the confirmation dialog appears, Homika:

1. Opens the selected document.
2. Verifies the Homika backup marker.
3. Verifies backup format compatibility.
4. Verifies database schema compatibility.
5. Parses every entity.
6. Verifies foreign-key relationships.
7. Verifies at most one deposit exists per booking.

Invalid files are rejected before the live database is changed.

## Restore transaction

After owner confirmation, restore runs inside one Room transaction.

Order:

1. Delete child tables.
2. Delete bookings and expenses.
3. Delete properties.
4. Insert properties.
5. Insert bookings.
6. Insert payments.
7. Insert deposits.
8. Insert expenses.
9. Insert blocked dates.

If any operation fails, Room rolls the transaction back.

The previous database therefore remains intact instead of becoming partially restored.

## Backup history

Homika stores local timestamps for:

- Last successful backup
- Last successful restore

These timestamps are convenience metadata only and are not required to recover data.

## Google Drive and zero recurring cost

Phase 8 uses Android's system document picker rather than a Homika cloud server.

When Google Drive is installed and available as a DocumentsProvider, a user can save the backup into their selected Drive account.

This keeps the owner in control and avoids:

- A Homika-hosted backend
- Server subscription costs
- Drive passwords inside Homika
- OAuth client configuration for manual backup/restore

## Phase boundary

Phase 8 implements backup and disaster recovery.

It does not implement live two-device sync.

Multi-device current-state synchronization remains Phase 9 because sync needs:

- Change tracking
- Conflict resolution
- Remote current-state semantics
- Tombstone propagation
