# Homika Private APK Updater

Homika uses GitHub Releases as its zero-cost update source.

## Release contract

- Repository: `IzokaGM/HOMIQ`
- Latest release API: `https://api.github.com/repos/IzokaGM/HOMIQ/releases/latest`
- Release tag: `vX.Y.Z`
- APK asset: `Homika-vX.Y.Z.apk`
- APK application ID: `com.homiq.app`
- All upgrade-compatible APKs must use the same stable Homika release certificate.

The dedicated private-release workflow enforces this contract automatically.

## Runtime flow

1. Homika silently checks GitHub Releases at most once every six hours after first-run setup.
2. More -> Homika version -> Check update can force a check.
3. If the latest release version is newer, Homika shows the release notes.
4. The APK downloads to private cache.
5. Homika validates package name, newer versionCode and signing-certificate continuity.
6. Android PackageInstaller handles installation and always keeps the user in control.
7. Android may request one-time permission to install unknown apps from Homika.

## End-to-end updater proof

After the first release-signed `1.0.0` is installed:

1. Run **Build Homika Private Release** again with `1.0.1` / `10001`.
2. Confirm GitHub Releases contains `Homika-v1.0.1.apk`.
3. On the phone still running release `1.0.0`, use **Check update**.
4. Download and install `1.0.1` through Homika.
5. Confirm Room data, account/sync settings and Homika preferences remain intact.

Once that succeeds, the updater is proven end-to-end.

## Repository visibility

The app intentionally uses GitHub's unauthenticated latest-release endpoint and embeds no GitHub token. Release metadata/assets therefore need to remain publicly readable. If source code is ever made private, use a separate public release-only repository instead of embedding a personal access token in Homika.
