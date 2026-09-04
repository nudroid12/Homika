# Homika Pro Final Production Audit - 15B

Status: feature freeze candidate. This audit intentionally does not add product features.

## Static checks completed

- Android applicationId remains `com.homika.app` and source namespace remains `com.homiq.app` by design.
- Android manifest keeps backup disabled and only the launcher activity exported; updater receiver is non-exported.
- Release updater verifies package/version and signing certificate before install.
- Stored activation token is protected with Android Keystore AES-GCM.
- Purchase PIN is not persisted on Android after activation.
- Signed activation token is device-bound and licence/device state is rechecked server-side for authenticated cloud operations.
- Cloud Backup and Cloud Sync uploads have 10 MB server limits and R2 access remains behind licence/device authentication.
- Cloud snapshots are encrypted before upload; sync content hashes are verified before merge/apply.
- Room uses explicit migration 1 -> 2; destructive migration fallback is not enabled.
- Local restore validates entity relationships before replacing the database inside a transaction.
- English and Malay Android string resources referenced by Kotlin are present.
- Backend JS, Worker deployment-hardening JS, Store JS and Android XML resources passed syntax/parse checks in the audit environment.
- No Google service dependency or WebView bridge was found in the production source reviewed.

## 15B hardening applied

- Worker health moves to v22.
- Checkout/store redirects are HTTPS-only on Worker and Android.
- Admin secret comparison uses the existing constant-time comparison helper.
- CORS advertises the existing PUT snapshot operation and its sync headers correctly.
- Store uses `Referrer-Policy: no-referrer` and a restrictive CSP while preserving HTTPS QR images and Worker API access.
- Repository now ignores the complete `LOCAL-ONLY-DO-NOT-UPLOAD/` directory.
- Patch safety cleanup removes that directory if it was accidentally copied into the repository tree. This does not delete any backup kept outside the repository.

## Manual release acceptance required

Complete these on the signed Release Candidate APK before calling v1.0 production-ready:

1. Fresh install -> Start 7-day Trial -> close/reopen -> Verify Now -> confirm trial remains the same trial.
2. Fresh paid order -> set Email + 6-digit PIN -> upload receipt -> confirm Pending before approval -> Approve -> activate from Android using Email + PIN.
3. Reject a separate order -> confirm exact rejection reason appears and customer must create a new order.
4. Existing paid licence -> renewal -> approve -> confirm same Licence Key and extended expiry after Verify Now.
5. Activate three paid devices -> fourth device must be blocked -> remove another device -> fourth device can then activate.
6. Create/edit data on device A -> foreground sync -> device B -> foreground sync -> confirm merged data and no duplicate booking/payment/deposit records.
7. Create Cloud Backup -> restore on a test device/account state -> verify properties, bookings, payments, deposits, expenses and blocked dates.
8. Enable App Lock -> background beyond configured timeout -> foreground -> confirm business data is not visible before PIN/biometric unlock.
9. Install one older production-signed APK -> use in-app updater -> confirm update installs without uninstall and preserves local data/licence state.
10. Test BM and English on Activation, Calendar, Booking, Money, Reports, Backup, Licence and Settings for clipping/overflow.
11. Open Store and Admin Dashboard after deployment and confirm browser console has no CSP/CORS errors; QR image and payment proof preview still load.
12. Confirm Worker `/health` reports version 22 and all required production configuration flags are healthy.

## Release gate

If all 12 manual acceptance checks pass on the same signed release candidate, freeze features and proceed to the v1.0 production release. Any failure should be fixed as a focused 15C release-polish/regression patch only.
