HOMIKA PRO PATCH 13C.5
Automatic Customer Email Delivery

FILES REPLACED
- backend/src/index.js
- store/admin.js
- PROJECT-CONTEXT.md

WHAT CHANGED
- Worker v17.
- Approve fresh purchase -> customer email includes Licence Key.
- Approve Trial upgrade / renewal -> customer email says same licence is retained and to use Verify Now.
- Reject reason is mandatory.
- Reject -> customer email includes exact rejection reason and asks customer to make a NEW order with the correct receipt.
- Approved/Rejected Admin history has "Hantar semula email" recovery action.
- Email failure never rolls back a successful payment/licence decision. Admin is warned so the email can be resent.
- No D1 migration.
- No Android changes.

EMAIL PROVIDER
This patch uses Resend server-to-server from the Cloudflare Worker.

Required Worker configuration:
1. RESEND_API_KEY
   - store as Cloudflare Secret
2. HOMIKA_EMAIL_FROM
   - recommended to store as Cloudflare Secret too
   - example only: Homika <no-reply@mail.yourdomain.com>

Production customer delivery requires a domain you control and verify with Resend.
Do not put RESEND_API_KEY in GitHub, Store JS or Android.

HEALTH CHECK
GET https://app-license-api.nudroids.workers.dev/health
Expected after deploy:
- version: 17
- customer_email_delivery: true
- customer_email_provider: resend
- customer_email_configured: true (only after both email settings exist)
- rejection_reason_required: true

TEST
1. Fresh purchase -> submit proof -> close page -> Approve -> email must contain Licence Key.
2. Renewal / Trial upgrade -> Approve -> email must say Verify Now, no new key.
3. Reject without reason -> blocked.
4. Reject with reason -> email must show exact reason and ask for a new order + correct receipt.
5. Approved/Rejected history -> Hantar semula email.
