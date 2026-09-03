Homika Pro Patch 13A.3
Trial Backend Final Fix + Homika Theme Restore

IMPORTANT ORDER
1. Run backend/database/0007-trial-claims-v2.sql on D1 app-license-prod.
2. Apply this patch through the normal Homika patch workflow.
3. Wait for Android workflow and Cloudflare Worker deployment to be green.
4. Verify /health reports version 12 and trial_ledger=hashed_device_email_v2.
5. Test Start 7-day free trial once.

Backend:
- New trial_claims_v2 ledger stores email/device hashes only.
- Trial creation no longer inserts or depends on customers rows.
- One trial per device and one trial per email remain enforced server-side.
- Legacy trial_redemptions are still checked so successful older claims cannot reset.
- Trial license/device/ledger writes are sequential with cleanup on partial failure.
- Trial remains 7 days and maximum 1 device.
- No change to paid licences, Cloud Sync, Cloud Backup, updater, or signing.

UI:
- Removes grey/dull trial and plan cards introduced in 13A.2.
- Trial card uses Homika teal.
- Trial field is high-contrast white with Homika ink.
- Trial CTA uses Homika mint.
- Paid plan card uses Homika Mint Soft with dark Homika text.
- Root screen still respects the app light/dark theme.
- Existing text-contrast and navigation-bar fixes remain.
