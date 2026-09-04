# Homika Pro - Canonical Project Context

Last updated: 2026-09-04
Repository: `nudroid12/Homika`

This file is the repository checkpoint and source of truth for continuing the Homika Pro project in a new chat/session. Read this before changing architecture or preparing a patch.

## 1. Product identity

- Personal app remains private and unchanged.
- Personal package/application ID: `com.homiq.app`.
- Commercial Pro app package/application ID: `com.homika.app`.
- Pro repository: `nudroid12/Homika`.
- Avoid Google dependency where practical.
- Local-first Android app with encrypted cloud backup/sync.

## 2. Commercial plans

Public plans:

| Plan key | Duration | Devices | Launch price | Regular/compare price |
|---|---:|---:|---:|---:|
| `1_month` | 1 month | 3 | RM7 | RM10 |
| `3_month` | 3 months | 3 | RM19 | RM30 |
| `6_month` | 6 months | 3 | RM35 | RM60 |
| `1_year` | 1 year | 3 | RM59 | RM120 |

Internal plans:

- `trial_7d`: 7 days, 1 device, not sold.
- `lifetime`: internal/admin only, never sold publicly.

Rules:

- Paid licence maximum: 3 devices.
- Trial maximum: 1 device.
- Trial once per device and once per email/customer.
- Same device + same email can resume remaining trial after reinstall/clear data, never receives a fresh 7 days.
- Same device + different email rejects.
- Same email + different device rejects.
- Renewal extends the same licence code.
- Renewal preserves unused remaining time.
- Trial to paid upgrade keeps the same licence identity/code where applicable.

## 3. Cloud architecture

Cloudflare stack:

- Worker: `app-license-api`
- Worker production URL: `https://app-license-api.nudroids.workers.dev/`
- D1 database: `app-license-prod`
- D1 binding: `DB`
- R2 bucket: `homika-cloud-prod`
- Store: Cloudflare Pages at `https://homika-store.pages.dev/`

Do not commit secret values into the repository.

Important Worker environment variables/secrets currently used:

- `CLOUD_MASTER_KEY`
- `HOMIKA_STORE_URL` (optional override; canonical Store URL is also compiled into Worker from Patch 13C.2)
- `HOMIKA_ADMIN_SECRET`
- `HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN`
- `HOMIKA_ADMIN_TELEGRAM_CHAT_ID`

`HOMIKA_PAYMENT_WEBHOOK_SECRET` belongs to the generic payment-webhook foundation from Phase 13B. Do not rely on it as a replacement for a future real payment provider's official signature verification.

## 4. Licensing backend status

Completed and stable:

- Licence activation.
- Signed activation tokens.
- Device registration/deactivation.
- 3-device paid limit.
- 7-day trial lifecycle.
- Verify Now.
- Expiry handling.
- Exact `plan_key` in licence/token paths.
- Same-code renewal logic.
- Trial to paid conversion foundation.

Known test licence previously used:

- `HMK-TEST-2026-0001`
- Product: `homika_pro`
- Max devices: 3

Do not redesign licensing core unless a regression requires it.

## 5. Cloud Backup and Sync

Cloud backup/sync is settled and should not be casually modified.

Current architecture:

- Encrypted client-side cloud backup using R2.
- Server can derive vault key using `CLOUD_MASTER_KEY`; do not market this as zero-knowledge/E2EE.
- Snapshot sync architecture replaced the failed record-event sync experiments.
- D1 `cloud_sync_snapshots` is active.
- Merge behavior uses Personal-style `HomiqSyncMerger`:
  1. higher revision
  2. newer `updatedAt`
  3. deterministic fingerprint tie-break

Foreground sync behavior:

- App background/closed: no live polling.
- App foreground: immediate cloud check.
- Local edit: debounce about 1.5 seconds then sync.
- App open: lightweight metadata check about every 30 seconds.

User has tested live sync successfully.

## 6. Updater and signing

Updater and production signing are complete.

Production package uses its own signing key.

Signing alias:

- `homika`

GitHub secrets:

- `HOMIKA_RELEASE_KEYSTORE_BASE64`
- `HOMIKA_RELEASE_KEYSTORE_PASSWORD`

Production signing certificate SHA256:

`C5:28:87:7F:E3:97:93:EE:25:A8:A8:7C:4C:DA:44:0B:D5:AA:FB:82:3B:5E:1A:F9:BF:66:29:FC:0E:CE:60:99`

Do not regenerate or replace the signing key.

Workflow builds a signed release APK and GitHub Release with automatic versioning.

## 7. Database migrations

Applied through migration `0009`.

Key migrations/status:

- Base licensing schema complete.
- `license_plans`, pricing, plan keys complete.
- Trial redemption/claim migrations complete.
- `0007`: `trial_claims_v2` final trial ledger.
- `0008`: `checkout_intents` purchase/renewal foundation.
- `0009`: manual QR payment submissions + indexes.

Migration 0009 is already applied to production D1. Do not ask the user to rerun it.

Future D1 console migration format preference:

- Give ONE downloadable file.
- Inside that single file clearly label `PART 01`, `PART 02`, etc.
- Each PART contains exactly one SQL statement/query.
- User runs one PART at a time in Cloudflare D1 Console.

## 8. Phase history

### Patch 13A - Store + Trial

Completed:

- Store pricing/catalog.
- Trial email claim.
- Trial activation.
- 7-day/1-device enforcement.
- Final trial ledger.
- Homika theme restored.

Trial is settled. Do not touch unless regression.

### Patch 13B - Purchase/Renewal Foundation

Completed:

- `checkout_intents`.
- Public purchase intent foundation.
- Authenticated renewal/upgrade intent.
- Same-licence renewal.
- Preserve unused remaining time.
- Exact `plan_key` propagation.
- Store checkout context.
- Worker v13 foundation.

End-to-end path verified:

Homika app -> Licence -> Upgrade -> Worker checkout intent -> Homika Store Pages.

### Patch 13C - Personal QR Payment + Manual Admin Approval

Current commercial payment solution because the owner does not currently use an SSM merchant payment gateway.

Completed design:

- Personal Touch 'n Go / Malaysia National QR bundled into Store.
- Customer selects plan.
- Customer pays via QR.
- Same-phone flow supports opening/saving QR for banking/eWallet gallery scan where supported.
- Customer presses `Saya Dah Bayar`.
- Payment proof screenshot upload is required.
- Payer name and transaction/reference fields may be optional in UX.
- Payment becomes `submitted`/pending.
- Customer is clearly told approval is manual and may take several hours or until the next day if admin is unavailable.
- Customer is told not to pay again while pending.
- Private proof storage in R2.
- Admin dashboard at Store `admin.html`.
- Admin can review proof and Approve/Reject.
- Approve invokes existing same-licence completion logic automatically.
- Telegram notification can alert admin on new submission.
- Worker v14 health exposes manual payment/admin feature flags.
- Android app was not changed by 13C.

Required production secrets/configuration already set by user:

- `HOMIKA_ADMIN_SECRET`
- `HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN`
- `HOMIKA_ADMIN_TELEGRAM_CHAT_ID`

Telegram notification has been confirmed received.

## 9. Patch 13C.1 - Admin Approve/Reject auth fix

Reason:

The Store admin dashboard could log in and list `submitted` payments, but Approve/Reject returned `unauthorized`.

Root cause in `store/admin.js`:

The shared `api()` helper built headers first and then spread `...options` afterward. POST review passes `options.headers = {'Content-Type':'application/json'}`. This replaced the complete header object and removed `x-homika-admin-secret`.

GET list worked because it did not pass its own `options.headers`.

Fix:

- Merge default/admin headers with request-specific headers first.
- Spread `options` before assigning the final merged `headers` object.
- Approve/Reject now retain `x-homika-admin-secret` and `Content-Type` together.
- If admin auth actually expires/fails, clear session and return to admin login with an appropriate message.

This patch changes Store admin JS only plus this context document. It does not touch Android, licence core, sync, D1 schema, Worker logic, signing, or updater.

## 10. Admin payment flow

Expected final flow:

1. Customer starts purchase/upgrade/renewal.
2. Store receives checkout context from Worker.
3. Customer selects plan if necessary.
4. Store displays the bundled Malaysia National QR.
5. Customer pays.
6. Customer uploads receipt/proof.
7. Worker stores proof privately in R2 and D1 row becomes `submitted`.
8. Telegram notifies admin.
9. Admin opens `admin.html` and signs in with `HOMIKA_ADMIN_SECRET`.
10. Admin views proof.
11. Admin Approves or Rejects.
12. Approve completes the existing checkout idempotently.
13. Renewal/upgrade keeps same licence and remaining time.
14. New purchase generates the paid licence.
15. Customer returns to Homika and verifies licence/status.

## 11. Payment gateway direction

CHIP was investigated but normal production onboarding expects merchant/business details. Current direction is therefore manual personal QR approval.

If the business later gets a suitable payment gateway:

- Keep `checkout_intents` as merchant order source of truth.
- Add provider integration server-to-server in Worker only.
- Never expose provider API secrets in Pages/Android.
- Verify official provider webhook signatures.
- Map provider transaction ID to `checkout_intents.provider_reference`.
- Reuse the existing idempotent checkout completion/same-licence renewal logic.
- Replace manual approval trigger with verified provider payment success without redesigning licensing.

## 12. Patch/repository conventions

User prefers actual downloadable ZIP patches rather than long instructions.

Patch rules:

- ZIP preserves repository-relative paths.
- Include only files that actually need replacing/adding.
- Do not include `.github` workflow files unless the workflow itself is being fixed or user explicitly asks.
- Do not include secrets.
- Do not overwrite `backend/wrangler.jsonc` unnecessarily.
- Do not touch stable Sync/Backup/licensing/updater systems for unrelated fixes.

Existing workflow has historically selected patch ZIP using filename sorting. When old patches remain, use a sufficiently high `ZZZ...` prefix so the intended latest patch is selected until workflow selection is deliberately fixed.

## 13. Validation expectations

Before shipping a backend/store patch where applicable:

- `node --check backend/src/index.js`
- `node --check store/app.js`
- `node --check store/admin.js`
- XML parse for changed Android XML.
- Inspect Kotlin structure for changed Kotlin files.
- GitHub Actions remains the final Android compilation gate if local Gradle cannot download its distribution.

For payment/admin fixes, verify separately:

- dashboard login/list
- proof view
- Approve
- Reject
- unauthorized/session-expired handling
- Telegram notification
- idempotent completion

## 14. Patch 13C.2 - Persistent Store URL

Reason:

`HOMIKA_STORE_URL` added manually in the Cloudflare Dashboard could disappear after Wrangler/Workers Builds deployment because non-secret dashboard variables are not guaranteed to survive config-driven deploys.

Fix:

- Canonical production Store URL `https://homika-store.pages.dev/` is compiled into Worker as a safe default.
- `HOMIKA_STORE_URL` remains supported as an optional environment override.
- Checkout redirect, renewal checkout URLs, bundled QR asset URL, and Admin Dashboard notification links all use the same resolver.
- Store functionality therefore survives future Worker deployments even if the dashboard text variable disappears.
- No D1 migration.
- No Android changes.
- No secret values are committed.
- `backend/wrangler.jsonc` is deliberately not replaced, preserving the production D1/R2 binding configuration.

## 15. Current status and next test

Canonical status after Patch 13C.2 is deployed:

- D1 migration 0009: done.
- Worker v14: 13C backend deployed.
- Store 13C: deployed.
- QR: bundled.
- Admin secret: configured.
- Telegram bot/chat secrets: configured.
- Telegram new-payment notification: confirmed working.
- Patch 13C.1 Admin Approve/Reject auth fix: user confirmed working.
- Store URL persistence: fixed by Patch 13C.2 canonical Worker fallback.

Next test after deploying Patch 13C.2:

1. Allow Worker deployment to complete.
2. `HOMIKA_STORE_URL` may remain set or may be absent in Cloudflare Dashboard; checkout must work either way.
3. Open Homika -> Licence -> Upgrade/Renew.
4. Confirm browser opens `https://homika-store.pages.dev/`.
5. Confirm QR and Admin Dashboard links still work.

No D1 migration is required for Patch 13C.2.

## 16. Patch 14A - Year-to-Date Money Summary

Goal:

Expose the owner's running yearly financial position directly on the Money screen without making the main screen chart-heavy.

Behavior:

- Money keeps the existing selected-month summary unchanged.
- A new compact `{year} so far` card appears directly below Cash movement.
- The card shows:
  - Revenue from January through the selected month.
  - Expenses from January through the selected month.
  - Net income = Revenue - Expenses.
- The period label follows the selected month, for example `January - September 2026`.
- Moving the Money month backward/forward recalculates YTD for that selected year/month. This also makes historical year review intuitive.
- Revenue uses the existing `BookingRevenueRules` and therefore remains consistent with the rest of Homika's Money/Reports calculations.
- Expenses use the existing `ExpenseRepository.observeTotalInRangeSen` range query.
- Existing Reports already has `This month`, `3 months`, `6 months`, `YTD`, and `Custom` filters, so no separate duplicate yearly report engine is added.
- No database/schema migration.
- No Worker/Store/licensing/cloud-sync/updater changes.

Canonical product status after 14A:

- Most production acceptance flow is green, but manual Admin Approve exposed one backend FK bug during paid checkout completion on 2026-09-04.
- The failure was `D1_ERROR: FOREIGN KEY constraint failed` inside `completePaidCheckout()` while reviewing a manual QR payment.
- Patch 13C.3 fixes this before v1.0 is finally locked.


## 17. Patch 13C.3 - Payment Completion Foreign-Key Fix

Observed production failure:

- Admin Dashboard authentication works.
- Telegram payment submission notification works.
- Manual payment submission reaches the Admin Dashboard.
- Pressing Approve reached Worker `/v1/admin/payments/review` but Worker threw:
  `D1_ERROR: FOREIGN KEY constraint failed: SQLITE_CONSTRAINT_FOREIGNKEY`.
- Stack pointed into `completePaidCheckout()`.

Root cause fixed:

- `checkout_intents.resulting_license_id` is a foreign key to `licenses(id)`.
- The previous fresh-purchase completion path could persist a newly generated `resulting_license_id` into `checkout_intents` before the corresponding `licenses` row existed.
- D1 correctly rejected that write.

Patch 13C.3 behavior:

- Fresh purchases persist only `resulting_license_key` before licence creation.
- The paid licence row is created first.
- Only after the licence exists is `checkout_intents.resulting_license_id` written.
- Retry recovery uses the persisted resulting licence key to find an already-created licence, preventing duplicate licences after partial failures.
- Renewal/Trial-upgrade validates the target licence before update.
- A legacy orphan `customer_id` is repaired from the checkout email rather than causing another FK failure.
- The payment row is inserted only after the target licence is re-validated as existing.
- Existing provider payment rows are checked against licence, amount and currency for idempotency/conflict protection.
- Admin review returns stable payment-completion error codes while full error details remain in Worker logs.
- Worker health version is 15 and exposes `payment_completion_fk_fix: true`.
- No D1 migration is required.
- Android, Money 14A, Cloud Sync, Backup, updater and signing are untouched.

Required production retest after green deploy:

1. Open the existing `submitted` payment that previously failed.
2. Press Approve again. Do not create a new payment first.
3. Confirm Admin status becomes approved/completed.
4. For an upgrade/renewal, return to Homika and Verify Now. Confirm the same licence is retained and expiry/plan updates correctly.
5. For a fresh purchase, confirm exactly one paid licence is produced.
6. Confirm `/health` reports Worker version 15 and `payment_completion_fk_fix: true`.

Only after this retest passes should Homika Pro v1.0 be considered fully production-locked.

## 18. Patch 13C.4 - Approval Completion UX

Reason:

After payment approval became technically successful, the Admin Dashboard immediately refreshed the pending list. This made the approved card disappear with no positive confirmation, and the customer handoff for a newly generated Licence Key was not obvious enough.

Behavior:

- Worker health version is 16 and exposes `approval_completion_ux: true`.
- No D1 migration.
- Admin Approve now opens a clear success dialog instead of silently removing the pending card.
- Fresh purchase approval shows the generated Licence Key to admin, with `Salin Licence Key` and `Salin link customer`.
- Upgrade/renewal approval explicitly states that the existing licence is retained and the customer should use Verify Now.
- Reject also gets a visible completion dialog and customer status-link copy action.
- Approved/Rejected history remains available through the existing Admin filter rather than disappearing permanently.
- Approved fresh-purchase rows expose their Licence Key only through the authenticated Admin API so admin can recover/copy it when needed.
- Customer checkout status remains the primary self-service delivery channel. If the customer leaves the page open, the 8-second polling detects approval automatically. If the customer closes it, the same checkout/status URL can be reopened later; reopened pending status pages now resume polling automatically.
- Fresh purchase completion page clearly displays the Licence Key and provides one-tap copy.
- Upgrade/renewal completion page clearly says no new key is created and instructs the customer to return to Homika and Verify Now.
- Customer completion page also provides a copy-status-link action for recovery.
- No email delivery is claimed or added because no transactional email provider is configured.
- Android, Money 14A, Cloud Sync, Backup, updater and signing remain untouched.

Production acceptance test for 13C.4:

1. Submit a QR proof for a fresh purchase.
2. Admin Approve. Confirm success dialog appears and includes the Licence Key plus customer status link.
3. Confirm customer page changes from Pending to Approved automatically and shows the same Licence Key.
4. Close and reopen the customer status link. Confirm the key is still available.
5. Repeat with Trial upgrade/renewal. Confirm no new key is created and customer is instructed to Verify Now.
6. Check Admin filter `Diluluskan` and confirm approved history remains visible.

## 19. Patch 13C.5 - Automatic Customer Email Delivery

Goal:

Make manual QR approval practical even when the customer has already closed the checkout page before admin reviews the payment.

Architecture/security:

- Licence Key + device activation + signed token remain the security boundary.
- Email is only the delivery/notification channel. Email address alone never activates a licence and never grants Homika Cloud access.
- Customer email is taken from the existing checkout intent.
- Transactional delivery is server-to-server from the Cloudflare Worker through Resend. No API key is exposed to Store JavaScript or Android.
- Required production Worker configuration:
  - `RESEND_API_KEY` as a Cloudflare Secret.
  - `HOMIKA_EMAIL_FROM` as a Cloudflare Secret or persistent variable, e.g. `Homika <no-reply@mail.example.com>` from a verified Resend domain.
- `resend.dev` is suitable only for restricted testing. Production delivery to arbitrary customer addresses requires a domain owned/controlled by the Homika owner and verified with the email provider.

Approve behavior:

- Fresh purchase:
  - Payment completion runs first.
  - Paid licence is generated using the existing licensing core.
  - Customer receives an approval email containing order reference, plan, amount and the generated Licence Key.
  - Email instructs customer to open Homika -> Activate Licence and enter the key.
- Trial upgrade / renewal:
  - Same licence remains in use.
  - Customer receives an approval email confirming the plan/payment update.
  - No new Licence Key is issued.
  - Email instructs customer to open Homika -> Licence -> Verify Now.
- Approval remains successful even if the email provider is temporarily unavailable. Admin sees a clear warning instead of the licence transaction being rolled back.

Reject behavior:

- Reject reason is now mandatory in both Admin UI and Worker validation.
- The exact admin reason is stored in the existing `admin_note` field.
- Customer automatically receives a rejection email containing that reason.
- Email states that no activation/renewal was performed and asks the customer to create a NEW order and upload the correct receipt/payment proof.
- For a fresh purchase the email can link back to Homika Store.
- For Upgrade/Renew the email tells the customer to restart checkout from Homika -> Licence -> Upgrade / Renew so an authenticated renewal intent is created again.

Recovery:

- Approved and Rejected history rows include `Hantar semula email` in Admin Dashboard.
- This calls the authenticated admin review endpoint with `resend_email` and can be used after fixing email configuration or on customer request.
- Automatic sends use a stable Resend idempotency key to reduce accidental duplicate sends; manual resend intentionally gets a fresh idempotency key.

Worker/UX status:

- Worker health version: 17.
- Health flags:
  - `customer_email_delivery: true`
  - `customer_email_provider: "resend"`
  - `customer_email_configured: true/false`
  - `rejection_reason_required: true`
- No D1 migration is required for 13C.5.
- No Android changes.
- Money 14A, Cloud Sync, Backup, updater, signing and licence core remain untouched.

Production acceptance for 13C.5:

1. Deploy Worker/Store patch and confirm `/health` version 17.
2. Configure `RESEND_API_KEY` and `HOMIKA_EMAIL_FROM`.
3. Confirm `/health` reports `customer_email_configured: true`.
4. Fresh purchase test: submit QR proof, close checkout page, Approve later, confirm customer receives Licence Key email and can activate.
5. Renewal/Trial-upgrade test: Approve and confirm email says use existing licence + Verify Now.
6. Reject test: attempt Reject without reason and confirm it is blocked. Enter a reason, Reject, and confirm customer receives the exact reason plus instructions to make a new order with the correct receipt.
7. From Approved/Rejected history test `Hantar semula email`.


## 20. Patch 13C.6 - Brevo Free Customer Email Delivery

Reason:

13C.5 implemented customer decision emails through Resend, but production delivery to arbitrary customers would require a verified sender domain. The owner wants a no-cost launch path and has created a Brevo Free account, generated an API key, and verified a sender email address.

Current email provider:

- Worker v18 uses Brevo Transactional Email API server-to-server.
- Endpoint: `POST https://api.brevo.com/v3/smtp/email`.
- Authentication is sent in the `api-key` request header.
- No API credential is exposed to Store JavaScript or Android.
- Sender must match the sender address already verified in Brevo.

Required Cloudflare Worker configuration:

- `BREVO_API_KEY` as a Cloudflare Secret.
- `HOMIKA_EMAIL_FROM` as a Variable or Secret containing the exact Brevo-verified sender email.
- `HOMIKA_EMAIL_FROM_NAME` is optional; defaults to `Homika`.
- `RESEND_API_KEY` is no longer used by Worker v18 and may be removed after successful Brevo acceptance testing.

Health:

- Worker version: 18.
- `customer_email_delivery: true`.
- `customer_email_provider: "brevo"`.
- `customer_email_configured` is true only when both `BREVO_API_KEY` and a valid `HOMIKA_EMAIL_FROM` are present.

Payment decision behavior remains unchanged from 13C.5:

- Fresh purchase Approve sends the generated Licence Key to the checkout email.
- Trial upgrade / renewal Approve sends confirmation that the existing licence has been updated and tells the customer to use Verify Now.
- Reject requires an admin reason and emails the exact rejection reason plus instructions to create a new order and upload the correct receipt.
- Email is a notification/delivery channel only. Email address alone never activates a licence and never grants Homika Cloud access.
- Payment/licence completion is not rolled back if email delivery fails; Admin gets a warning and can use `Hantar semula email` later.

No D1 migration is required for 13C.6.
No Android changes. Money 14A, Cloud Sync, Backup, updater, signing and licence core remain untouched.

Production acceptance test for 13C.6:

1. Deploy patch and confirm `/health` reports version 18 and provider `brevo`.
2. Add `BREVO_API_KEY` and `HOMIKA_EMAIL_FROM` in Worker Variables and Secrets.
3. Confirm `/health` reports `customer_email_configured: true`.
4. Fresh purchase: Approve an order and confirm the customer receives the Licence Key email.
5. Trial upgrade / renewal: Approve and confirm the email says to use the existing licence and Verify Now.
6. Reject with a reason and confirm the customer receives the exact reason plus instructions for a new order.
7. Test `Hantar semula email` from Approved/Rejected history.

## 21. Patch 13C.7 - Customer Telegram Notifications

Canonical current payment notification architecture after 13C.7:

- Email remains the backup delivery path through Brevo Free.
- Telegram is the fast notification path for customers who explicitly opt in.
- The same Telegram bot already used for Homika admin payment alerts is reused. No second bot is required.
- Customer Telegram is never linked by email or phone number.
- A customer must open the unique Telegram deep-link from their own Homika checkout and press START.
- Backend creates a one-time checkout-specific Telegram link token and binds that checkout to the Telegram private chat that starts the bot.
- A linked checkout cannot be rebound to a different Telegram chat using the same token.
- Telegram is notification/delivery only. It is NOT an authentication method for Homika licence activation or Homika Cloud data.
- Licence Key + activated device + signed token remain the security boundary for paid activation/cloud access.

Backend Worker:
- Worker version: v19.
- New endpoint: `POST /v1/store/telegram/link`
- New Telegram webhook endpoint: `POST /v1/store/telegram/webhook`
- The Worker automatically calls Telegram `getMe` + `setWebhook` on the first valid customer link request if the customer webhook has not been configured yet.
- Webhook secret is generated by the Worker and stored privately in D1. The user does not need to create another Cloudflare secret for the webhook.
- Existing `HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN` is reused.
- Existing admin Telegram notifications remain unchanged.

D1 migration 0010 adds:
- `homika_telegram_config`
- `checkout_telegram_links`
- status lookup index

Migration file:
- `backend/database/0010-customer-telegram-notifications.sql`
- It is deliberately split into clearly labelled PART 01 / PART 02 / PART 03 sections because Cloudflare D1 Console should be run one statement at a time for this project.

Customer flow:
1. Customer chooses a plan, pays personal DuitNow/TNG QR and uploads receipt.
2. Once proof is submitted, checkout shows `Aktifkan Notifikasi Telegram`.
3. Button opens the Homika Telegram bot with a unique checkout deep-link.
4. Customer presses START.
5. Bot confirms the order is linked and customer may close the checkout page.
6. On Approve:
   - fresh purchase: Telegram sends approved message + Licence Key + activation instruction.
   - Trial upgrade/renewal: Telegram says existing licence was updated and instructs `Verify Now`.
7. On Reject:
   - Telegram sends the exact admin rejection reason and asks customer to create a new order with the correct receipt.
8. Brevo email is still sent as backup for Approve/Reject.

Admin UX:
- Approval popup also reports whether customer Telegram was delivered or whether the customer had not linked Telegram.
- `Hantar semula email` is renamed to `Hantar semula notifikasi`; it retries email and Telegram when a Telegram link exists.

Files changed in 13C.7:
- `backend/src/index.js`
- `backend/database/0010-customer-telegram-notifications.sql`
- `store/app.js`
- `store/styles.css`
- `store/admin.js`
- `PROJECT-CONTEXT.md`

Required production sequence:
1. Apply D1 migration 0010, PART 01 then PART 02 then PART 03.
2. Deploy Patch 13C.7.
3. Submit one test QR payment proof.
4. Tap `Aktifkan Notifikasi Telegram` and press START in the Homika bot.
5. Confirm bot says the order is waiting for admin review.
6. Approve from Admin Dashboard.
7. Confirm Telegram receives approval and fresh-purchase Licence Key / renewal Verify Now message.
8. Test Reject separately and confirm exact rejection reason is delivered.

Do not weaken activation security by allowing email-only or Telegram-only licence access.

## 22. Patch 13C.8 - Purchase Account Activation (Email + 6-digit PIN)

Reason:

Email delivery can land in Promotions/spam and Telegram is optional. A paying customer must still be able to activate Homika without waiting for either channel or retaining a browser status link.

Canonical activation UX after 13C.8:

- Primary paid-purchase activation is now **Purchase Email + 6-digit Purchase PIN**.
- Customer sets the 6-digit PIN during checkout and confirms it before proceeding to QR payment.
- After admin approval, customer opens Homika and uses the same purchase email + PIN to activate the current device directly.
- Email and Telegram remain optional notification/backup delivery channels only.
- Licence Key remains supported as a backup/legacy activation method and remains available to admin/recovery flows.
- Trial activation remains unchanged.
- Signed activation tokens, device binding, 3-device paid limit, Cloud Sync authorization and existing licence verification remain unchanged.

Security boundary:

- Email alone NEVER activates Homika.
- Purchase PIN must be exactly 6 digits.
- PIN is never stored plaintext in D1, Android, Store JavaScript or notification messages.
- Checkout/account PIN verifier is HMAC-SHA256 using a random per-checkout salt and a server-only pepper.
- Required Worker Secret: `HOMIKA_PURCHASE_PIN_PEPPER`.
- Do not expose or commit the pepper.
- Failed PIN attempts are persisted by email hash.
- Five failed attempts temporarily lock purchase sign-in for about 15 minutes.
- Successful verification clears the failed-attempt counter.
- Device limit remains enforced by the existing `devices` table and activation path.
- Purchase account login only produces the same signed activation token as existing licence activation. It does not directly expose cloud backups or cloud sync records.

Backend Worker v20:

- New endpoint: `POST /v1/purchases/activate`.
- `/health` reports:
  - `version: 20`
  - `purchase_account_activation: true`
  - `purchase_pin_digits: 6`
  - `purchase_pin_rate_limit: true`
  - `purchase_pin_pepper_configured` based on Worker Secret presence.
- Checkout create/select-plan now requires `purchase_pin`.
- Authenticated renewal checkout still starts from the existing signed licence token, but Store requires the customer to enter purchase email + PIN before QR payment.
- First Trial upgrade / legacy renewal can create the Purchase PIN during checkout.
- Once a purchase account already exists, renewal requires the existing PIN. A different PIN is rejected.
- A purchase account is bound to one Homika licence. Attempts to bind the same email/PIN account to a different licence are rejected.
- On payment approval, `completePaidCheckout` upserts the approved licence into the purchase account before checkout completion.
- Legacy orders without a Purchase PIN remain valid and continue using Licence Key delivery/activation.

Pending/rejected behavior from the Android activation screen:

- Correct email + PIN while receipt is still awaiting admin review returns `purchase_pending` and tells the customer not to pay again.
- Rejected order returns `purchase_rejected` and includes the admin rejection reason when available.
- Correct credentials but no receipt submission returns `purchase_not_submitted`.
- Wrong email/PIN returns a generic credential error and contributes to rate limiting.
- No Purchase PIN is saved locally after successful activation.

D1 migration 0011:

Run one statement at a time in Cloudflare D1 Console.

- PART 01: add `checkout_intents.purchase_pin_salt`.
- PART 02: add `checkout_intents.purchase_pin_hash`.
- PART 03: add `checkout_intents.purchase_pin_version`.
- PART 04: create `purchase_accounts`.
- PART 05: create purchase-account licence index.
- PART 06: create `purchase_pin_security` for persistent failed-attempt / lock state.

Store UX:

- Fresh purchase: Email -> PIN 6 digit -> Confirm PIN -> QR payment -> upload receipt.
- Renewal/manual legacy licence: Email -> PIN -> Confirm PIN -> Licence Key -> plan -> QR.
- Checkout explicitly tells the customer to remember the PIN and that Homika will not send the PIN by email/Telegram.
- Approved fresh purchase page tells the customer to activate using Email + PIN first.
- Licence Key is moved to a secondary backup section.
- Brevo and Telegram approval messages now describe Email + PIN as the primary activation method and Licence Key as backup.

Android files changed:

- `LicenseModels.kt`
- `LicenseApiClient.kt`
- `LicenseRepository.kt`
- `LicenseViewModel.kt`
- `HomiqApp.kt`
- `LicenseActivationScreen.kt`
- English and Malay licence string resources.

Required production sequence:

1. Apply D1 migration 0011 PART 01 through PART 06, one statement at a time.
2. Create Cloudflare Worker Secret `HOMIKA_PURCHASE_PIN_PEPPER` with a long random value. Never paste it into chat or commit it.
3. Deploy Patch 13C.8 / Worker v20.
4. Confirm `/health` shows `purchase_pin_pepper_configured: true`.
5. Fresh test: choose RM7, set email + PIN, submit proof, verify Android login says payment pending before approval.
6. Approve the same order.
7. Reopen/fresh-install Homika and activate using the same Email + PIN without using email, Telegram or Licence Key.
8. Verify device count is correct and Cloud Sync still uses the signed activation token.
9. Test five wrong PIN attempts and confirm temporary lock.
10. Test a rejected order and confirm the Android screen shows rejection state/reason and requests a new order.

Do not remove Licence Key compatibility. It is the recovery/legacy path if the customer forgets the Purchase PIN or for older licences created before 13C.8.
