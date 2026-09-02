# Homika Google Drive Sync Setup

Phase 9 contains the Android code for optional same-account Google Drive synchronization.

Google authorization still requires one-time developer configuration in Google Cloud.

## 1. Google Cloud project

Create or select the Google Cloud project used for Homika.

Enable:

- Google Drive API

## 2. OAuth consent screen

Configure the OAuth consent screen for the Homika application.

Declare the narrow Drive application-data scope:

`https://www.googleapis.com/auth/drive.appdata`

Homika does not request full Google Drive access.

The sync engine stores its current-state files only in Drive `appDataFolder`.

## 3. Android OAuth client

Create an OAuth Client ID of type Android.

Use:

- Package name: `com.homiq.app`
- SHA-1: the SHA-1 fingerprint of the certificate that signs the APK installed on the phone

No OAuth client secret belongs in the Android source tree.

No `google-services.json` is required by the Phase 9 implementation.

## 4. Signing certificate must be stable

Google identifies an Android OAuth application using package name plus signing certificate.

A newly generated debug keystore can have a different SHA-1.

If GitHub Actions creates a fresh debug keystore on every runner, Drive authorization will not be reliably testable by registering only one SHA-1.

Before runtime Drive testing, use one stable signing certificate.

Recommended later repository setup:

- Keep the keystore outside the repository.
- Store signing material in GitHub Secrets.
- Configure the Android build to use that stable signing certificate.
- Register its SHA-1 in the Google Cloud Android OAuth client.

This also aligns with Phase 10/11 release hardening.

## 5. Testing accounts

If the OAuth consent configuration is still in Testing, add the Google account used on the test phones as an allowed test user when Google Cloud requires it.

## 6. Two-phone test

1. Install a Homika build signed by the registered certificate on Phone A.
2. Open More -> Google Drive Sync.
3. Connect the intended Google account.
4. Create a property or booking and sync.
5. Install the same signed Homika build on Phone B.
6. Connect the same Google account.
7. Tap Sync Now.
8. Confirm Phone B receives Phone A data.
9. Edit a record on Phone B.
10. Sync Phone B, then Phone A.
11. Confirm both devices converge.

## Google Drive architecture

Homika uses the narrow `drive.appdata` OAuth scope.

Drive `appDataFolder` is per Google user and private to application data.

Each Homika installation has a stable local device UUID and writes only its own file:

`homiq-sync-device-<device-id>.json`

This avoids two phones overwriting the same Drive file concurrently.

Every sync:

1. Takes a local Room snapshot.
2. Lists all Homika device snapshot files in `appDataFolder`.
3. Downloads and decodes valid snapshots.
4. Merges records by stable UUID.
5. Keeps tombstones.
6. Applies the merged state to Room in one transaction.
7. Writes the merged state back to only this device file.

## Conflict rule

For the same record ID:

1. Higher `revision` wins.
2. If revisions are equal, newer `updatedAtEpochMillis` wins.
3. If both are equal, a stable payload comparison breaks the tie.

A same-revision payload difference is counted as a concurrent edit conflict and shown in Sync status.

Deposit is merged by `bookingId` because Room enforces one deposit per booking.

## Automatic sync triggers

When sync is connected:

- App enters foreground
- Successful local repository write/delete
- Manual Sync Now

Repository changes are debounced before network sync.

Homika does not require a webhook or always-on server.

## Backup remains separate

Phase 8 Backup & Restore is disaster recovery.

Phase 9 Drive Sync is current-state convergence between Homika installations.

They are deliberately separate features.


## Phase 10 stable debug OAuth identity

Phase 10 now ships a dedicated debug-only certificate so GitHub debug APK fingerprints stop changing.

For **debug testing**, create the Android OAuth client with:

- Package: `com.homiq.app`
- SHA-1: `5B:FC:0E:63:6E:F3:06:80:F3:BD:A1:5D:4B:B9:93:C4:22:B1:48:D9`

The Sync screen also displays the certificate SHA-1 detected from the installed APK so it can be copied directly.

Do not reuse the Phase 10 debug key as the production release key. Phase 11 will define release signing separately, and its release SHA-1 must also be registered in Google Cloud when production Drive authorization is tested.


## Homika V1 private release OAuth client

Private release signing uses a certificate separate from the Phase 10 debug build. Keep the existing debug Android OAuth client and create another Android client with:

- Package: `com.homiq.app`
- SHA-1: `9E:83:5A:83:A6:8E:2B:53:04:F9:CE:27:51:CD:A4:72:D0:11:2A:D1`

Both clients use the same Google Cloud project, Drive API and `drive.appdata` scope.
