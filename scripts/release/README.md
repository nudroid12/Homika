# Release tooling

Homika private releases are built by `.github/workflows/private-release.yml` using a stable signing key stored only in GitHub Actions secrets.

Required repository secrets:

- `HOMIKA_RELEASE_KEYSTORE_BASE64`
- `HOMIKA_RELEASE_KEYSTORE_PASSWORD`

Release certificate SHA-1: `9E:83:5A:83:A6:8E:2B:53:04:F9:CE:27:51:CD:A4:72:D0:11:2A:D1`

Do not copy the release keystore into this directory or commit it anywhere in the repository.
