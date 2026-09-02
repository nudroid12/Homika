# Homika Phase 10 Security

## App lock

Homika can be protected with a local numeric PIN.

- PIN length: 4 to 8 digits.
- The plain PIN is never stored.
- A random 16-byte salt is generated on PIN creation/change.
- PBKDF2-HMAC-SHA256 derives a 256-bit hash.
- Hash comparison is constant-time.
- A fresh app process starts locked whenever a PIN exists.

## Biometric unlock

Biometric unlock is optional and becomes available only after a PIN exists.

Homika uses AndroidX Biometric and the device biometric enrollment. Homika does not store fingerprint or face data.

## Auto-lock

Available timeouts:

- Immediately
- 1 minute (default)
- 5 minutes
- 15 minutes

Configuration changes do not count as leaving the app.

## Phase 10 stable debug signing

Phase 10 introduces a dedicated **debug-only** signing key:

`app/homiq-debug.keystore`

Its sole purpose is to keep the certificate fingerprint stable for GitHub debug APKs and Google Drive OAuth testing.

Debug package:

`com.homiq.app`

Debug SHA-1:

`5B:FC:0E:63:6E:F3:06:80:F3:BD:A1:5D:4B:B9:93:C4:22:B1:48:D9`

The debug keystore is intentionally not a production credential and must never sign a release build.

Phase 11 will use a separate secure release signing process/key.

## Signature transition warning

Phase 1-9 debug APKs may have been signed with another debug certificate.

Android will reject an in-place update when the signing certificate changes.

Before installing the first Phase 10 APK:

1. Create a Homika backup using Backup & Restore.
2. Keep the backup somewhere safe.
3. Uninstall the older Homika app if Android reports a signature mismatch.
4. Install the Phase 10 APK.
5. Restore the backup.

After this one-time transition, Phase 10+ debug APKs use the same debug signing certificate.
