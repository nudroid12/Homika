# Homika Phase 9 Sync Flow

## Principle

Room remains the primary local database.

Google Drive is a current-state exchange layer, not the runtime database.

The app continues working offline.

## Storage model

Drive space:

`appDataFolder`

OAuth scope:

`drive.appdata`

Per-device file:

`homiq-sync-device-<device-id>.json`

A device updates only its own Drive file.

## Sync algorithm

```text
Local Room snapshot
        |
        v
List all Homika device files
        |
        v
Download valid remote snapshots
        |
        v
Deterministic record merge
        |
        v
Room transaction upsert
        |
        v
Write merged snapshot to own Drive file
```

## Merge order

Data includes all current and tombstoned rows.

Merge keys:

- Property: id
- Booking: id
- Payment: id
- Deposit: bookingId
- Expense: id
- Blocked Date: id

Winner order:

- revision
- updatedAtEpochMillis
- stable payload tie-break

## Delete propagation

Deletes remain tombstones.

A tombstoned row with a higher revision wins over an older active row.

This prevents an old phone from resurrecting a deleted item.

## Offline behavior

When Drive is unavailable:

- Local saves still succeed.
- Room remains usable.
- Sync failure does not roll back the local business action.
- The next foreground, local-change trigger, or Sync Now can retry.

## No server

Phase 9 uses no Homika backend.

There is no push webhook.

Foreground/change-triggered synchronization is sufficient for the intended private two-phone owner workflow.
