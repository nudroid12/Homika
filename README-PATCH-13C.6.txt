Homika Pro Patch 13C.6 - Brevo Free Customer Email Delivery

Purpose
- Replace the Resend integration from 13C.5 with Brevo transactional email.
- Keep the existing Approve/Reject behavior and customer email UX.
- No domain purchase is required for the current setup as long as the sender address is verified in Brevo.

Worker configuration after deploy
1. BREVO_API_KEY
   - Cloudflare Secret
   - Use the API key generated in Brevo.
   - Never commit this value to GitHub.

2. HOMIKA_EMAIL_FROM
   - Cloudflare Variable or Secret
   - Must be the exact sender email address that is verified in Brevo.
   - Example: yourverifiedaddress@gmail.com

3. HOMIKA_EMAIL_FROM_NAME (optional)
   - Cloudflare Variable
   - Default is Homika when omitted.

Old Resend settings
- RESEND_API_KEY is no longer read by Worker v18.
- It may be deleted after Brevo testing is successful.

Health check
GET /health
Expected:
- version: 18
- customer_email_delivery: true
- customer_email_provider: brevo
- customer_email_configured: true

Behavior
Approve fresh purchase:
- Payment/licence completion happens first.
- Customer receives Licence Key by email.

Approve Trial upgrade / renewal:
- Existing licence is retained.
- Customer receives an email telling them to use Verify Now.

Reject:
- Admin reason is mandatory.
- Customer receives the exact rejection reason and instructions to create a new order with the correct receipt.

Recovery:
- Hantar semula email remains available in Approved/Rejected history.

No D1 migration is required.
Android, Money 14A, Cloud Sync, Backup, updater, signing and licence core are untouched.
