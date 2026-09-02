# Homika Private Release

Homika is distributed privately as a signed APK through GitHub Releases / Telegram. It is not a Play Store product.

## Locked Android identity

- Display name: `Homika`
- Android application ID: `com.homiq.app`
- Default source version: `1.0.0` / `10000`
- Stable debug SHA-1: `5B:FC:0E:63:6E:F3:06:80:F3:BD:A1:5D:4B:B9:93:C4:22:B1:48:D9`
- Stable private-release SHA-1: `9E:83:5A:83:A6:8E:2B:53:04:F9:CE:27:51:CD:A4:72:D0:11:2A:D1`

The package ID stays `com.homiq.app` so Room data, SharedPreferences, backup compatibility, Drive sync identity and updater package validation stay compatible.

## Stable release key

The actual private key must never be committed to the repository. The supplied offline key bundle contains the one stable release keystore generated for Homika. Keep at least two offline copies.

GitHub Actions only needs these two repository secrets:

- `HOMIKA_RELEASE_KEYSTORE_BASE64`
- `HOMIKA_RELEASE_KEYSTORE_PASSWORD`

The alias is fixed as `homika`; the key password intentionally uses the same strong value as the keystore password to reduce secret setup mistakes.

`app/build.gradle.kts` accepts release values from environment variables and only enables release signing when the key is present. Debug builds continue using the existing stable debug certificate.

## One-time GitHub setup

1. Upload `private-release.yml` to `.github/workflows/private-release.yml`.
2. Open GitHub repository **Settings -> Secrets and variables -> Actions**.
3. Create both secrets using the exact values from the offline release-key bundle.
4. Keep the offline keystore bundle outside the repository.

## One-time Google OAuth setup for release APK

Create another Android OAuth client in the same Google Cloud project:

- Package: `com.homiq.app`
- SHA-1: `9E:83:5A:83:A6:8E:2B:53:04:F9:CE:27:51:CD:A4:72:D0:11:2A:D1`

Use the same Drive API / `drive.appdata` configuration. The existing debug OAuth client remains useful for debug APKs. No `google-services.json` is required by Homika's current Drive authorization implementation.

## Publishing a release

Open **Actions -> Build Homika Private Release -> Run workflow**.

For the first private release use:

- version_name: `1.0.0`
- version_code: `10000`

The workflow injects those values into Gradle, builds the signed APK, validates package/version/signature, creates a SHA-256 checksum and publishes:

- `Homika-v1.0.0.apk`
- `Homika-v1.0.0.apk.sha256`

It also prints the release certificate SHA-1 in the Actions job summary.

For an updater test, publish the next build as `1.0.1` / `10001`. The source file does not need a version edit just to create a release; the workflow inputs override the default values at build time.

## First transition from debug to release

Debug and release certificates are different, so Android cannot install the first release APK directly over the current debug APK.

1. Sync Homika and create a local backup.
2. Register the release SHA-1 above in Google Cloud.
3. Uninstall the debug APK.
4. Install `Homika-v1.0.0.apk`.
5. Connect the same Google account and sync, or restore the local backup.

From that point onward, every APK made by the release workflow uses the same certificate and can update the installed release without clearing Homika data.
