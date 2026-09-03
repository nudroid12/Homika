# Homika Pro Store - Patch 13B

Static Cloudflare Pages frontend for the Homika Pro catalogue and checkout-intent flow.

Required before public use:
1. Deploy this `store/` directory to Cloudflare Pages.
2. Set Worker environment variable `HOMIKA_STORE_URL` to the Pages HTTPS URL.
3. Apply D1 migration `0008-commerce-checkout-foundation.sql`.
4. Deploy Worker v13.

Patch 13B does NOT charge customers yet. It creates secure checkout intents and the same-licence renewal path.
The generic `/v1/store/payment-webhook` completion endpoint is included for the next payment-gateway patch.
Do not set or expose `HOMIKA_PAYMENT_WEBHOOK_SECRET` in this static site.
