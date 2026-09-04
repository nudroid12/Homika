# Homika Pro - Canonical Project Context

Last updated: 2026-09-03
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
