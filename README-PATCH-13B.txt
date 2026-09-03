Homika Pro Patch 13B
Store + Purchase/Renewal Foundation

IMPORTANT ORDER
1. Apply D1 migration backend/database/0008-commerce-checkout-foundation.sql.
2. Apply this ZIP using the normal Homika patch workflow.
3. Confirm Worker /health reports version 13.
4. Deploy the store/ folder to Cloudflare Pages if it is not already deployed.
5. Set Worker HOMIKA_STORE_URL to the HTTPS Pages URL.

Included:
- Live catalogue RM7 / RM19 / RM35 / RM59.
- 1 Year marked Paling Berbaloi.
- Authenticated trial upgrade / paid renewal tied to the SAME licence.
- Public manual buy/renew checkout intents.
- Generic secure payment webhook foundation for the next gateway patch.
- Renewal extends from max(current expiry, payment time), preserving unused time.
- Paid plan max devices becomes 3.
- Activation token carries exact plan_key.
- Licence UI can show 1 Month / 3 Months / 6 Months / 1 Year.
- Expiry is shown in the phone local timezone.

Payment safety:
- This patch does not fake or simulate a payment.
- Payment completion requires HOMIKA_PAYMENT_WEBHOOK_SECRET.
- Leave that secret unset until the real gateway integration.

Not changed:
- Cloud Sync engine
- Cloud Backup merger/encryption
- Updater
- Android signing
