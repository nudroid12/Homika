Homika Pro Patch 13C FINAL
QR Payment + Manual Admin Approval

What this final patch adds
- Real Touch n Go / Malaysia National QR is bundled in store/payment-qr.jpg.
- Customer does not need a second phone:
  * Buka QR penuh
  * Simpan QR
  * use bank/eWallet Scan/Import from Gallery when supported
- Customer is clearly told BEFORE payment that approval is manual.
- Customer message does not promise instant activation:
  * usually reviewed within a few hours
  * if admin is unavailable, review may happen the next day
  * do not pay twice
- Proof flow is reduced: screenshot is the only required customer proof field.
  Payer name and Transaction ID are optional.
- Checkout token is preserved in the browser URL.
- Customer can copy a status link and reopen it later.
- Submitted state shows Bayaran diterima and tells customer not to pay again.
- Admin dashboard auto refreshes every 30 seconds while open.
- Optional Telegram push notification is supported when a payment proof is submitted.
- Approve/Reject behavior remains the same and reuses Patch 13B licence completion logic.
- Worker health remains version 14 because this is the final 13C before first Worker v14 deployment.

D1
Migration 0009 is still included for repository completeness.
If migration 0009 has already been completed successfully, DO NOT run it again.
There is no additional D1 migration for this final patch.

Required Worker setting
1. Secret: HOMIKA_ADMIN_SECRET
   Use a long random secret. Never commit it to the repository.

Existing setting
2. Variable: HOMIKA_STORE_URL
   Example: https://homika-store.pages.dev
   The bundled QR is automatically resolved from this URL.

Optional settings
3. Variable: HOMIKA_PAYMENT_DISPLAY_NAME
   Optional text shown above the QR.

4. Variable: HOMIKA_PAYMENT_QR_URL
   Optional override only. Leave unset to use bundled store/payment-qr.jpg.

Optional instant admin notification via Telegram
5. Secret: HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN
6. Secret: HOMIKA_ADMIN_TELEGRAM_CHAT_ID

When both Telegram secrets are configured, every submitted/resubmitted payment proof sends a push message containing amount, plan, order reference, purchase type and the Admin Dashboard URL.
Telegram notification failure never blocks the customer payment submission.

Admin Dashboard
https://homika-store.pages.dev/admin.html
Enter HOMIKA_ADMIN_SECRET.
The secret is kept in sessionStorage for the current browser session only.

Customer payment proof
- JPG / PNG / WebP
- max 2 MB
- stored privately in existing R2 BACKUPS under manual-payments/

No Android app files are changed.
Patch 13B in-app Buy/Upgrade/Renew link continues to work with this 13C store flow.
