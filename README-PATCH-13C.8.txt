Homika Pro Patch 13C.8
Purchase Account Activation: Email + 6-digit PIN

Purpose
- Make paid licence activation independent from email inbox delivery, Telegram availability and saved browser links.
- Customer sets a 6-digit PIN during checkout.
- After admin approval the customer can activate directly inside Homika with purchase email + PIN.

Security
- Email alone is never enough.
- PIN is HMAC-SHA256 protected with a random salt + Worker-only pepper.
- Five wrong attempts cause a temporary ~15 minute lock.
- Existing device binding, 3-device paid limit and signed activation token remain unchanged.
- Licence Key stays available as backup/legacy activation.

Required before deploy
1. Apply backend/database/0011-purchase-account-pin.sql one PART at a time in D1 Console.
2. Add Worker Secret HOMIKA_PURCHASE_PIN_PEPPER with a long random value.
3. Deploy patch.
4. /health should report version 20 and purchase_pin_pepper_configured=true.

No change to Cloud Sync/Backup data model, updater, production signing or Money 14A.
