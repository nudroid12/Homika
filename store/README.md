# Homika Pro Store

Static Cloudflare Pages storefront for Homika Pro.

Patch 13C Final uses QR payment with manual admin approval.

- Customer store: `/`
- Admin dashboard: `/admin.html`
- Store API: `https://app-license-api.nudroids.workers.dev`
- Real payment QR: `payment-qr.jpg`
- Worker normally resolves the bundled QR from `HOMIKA_STORE_URL`.
- `HOMIKA_PAYMENT_QR_URL` is optional and only overrides the bundled QR.
- Admin access requires Worker secret `HOMIKA_ADMIN_SECRET`.
- Payment proof is uploaded to the Worker and stored privately in existing R2 `BACKUPS` binding.
- Optional Telegram push requires Worker secrets `HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN` and `HOMIKA_ADMIN_TELEGRAM_CHAT_ID`.

No API key, admin secret or Telegram secret belongs in this Pages directory.
