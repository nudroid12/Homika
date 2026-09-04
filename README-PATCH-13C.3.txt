HOMIKA PRO PATCH 13C.3
Payment Completion Foreign-Key Fix

Observed production error:
D1_ERROR: FOREIGN KEY constraint failed during Admin Approve.

Fix:
- Fresh purchase no longer writes checkout_intents.resulting_license_id before the referenced licenses row exists.
- Licence is created first, then resulting_license_id is stored.
- Retry recovery uses resulting_license_key to avoid duplicate licences after partial completion.
- Renewal/Trial upgrade validates and repairs customer relation when necessary.
- Payment insert validates the licence FK target immediately before writing.
- Existing provider payment rows are validated for idempotency/conflicts.
- Admin completion errors now return stable error codes and keep full detail in Worker logs.
- Worker health version: 15
- health flag: payment_completion_fk_fix=true

NO D1 MIGRATION REQUIRED.
NO ANDROID CHANGES.
Cloud Sync, Backup, updater, signing, Store UI and Money 14A are untouched.

Retest after green deploy:
1. Open the SAME existing submitted payment that previously failed.
2. Press Approve again.
3. Confirm it becomes approved/completed.
4. Return to Homika and Verify Now if it is upgrade/renewal.
5. Confirm same licence + correct paid expiry/plan.
