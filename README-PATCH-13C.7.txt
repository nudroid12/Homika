HOMIKA PRO PATCH 13C.7
Customer Telegram Notifications

WHAT THIS PATCH DOES
- Adds optional Telegram notification for each customer checkout.
- Reuses the existing Homika admin Telegram bot.
- Customer explicitly links their own Telegram by opening a unique checkout deep-link and pressing START.
- Approve fresh purchase -> Telegram sends Licence Key.
- Approve Trial/renewal -> Telegram tells customer to Verify Now.
- Reject -> Telegram sends exact rejection reason and asks customer to create a new order with the correct receipt.
- Brevo email remains as backup.
- Admin completion popup reports Telegram delivery state.
- Admin "Hantar semula notifikasi" retries email + Telegram when linked.

SECURITY
- Email and Telegram are notification channels only.
- They do not unlock Homika data.
- Licence Key + device activation + signed token remain required.
- Checkout Telegram tokens are unique and cannot be rebound to a different Telegram account after linking.
- Telegram webhook secret is generated server-side and stored in D1.

D1 MIGRATION REQUIRED
Run backend/database/0010-customer-telegram-notifications.sql ONE PART AT A TIME:
PART 01 -> Success
PART 02 -> Success
PART 03 -> Success

NO NEW CLOUDFLARE SECRET REQUIRED
Existing HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN is reused.
The Worker automatically configures the Telegram webhook on the first valid customer Telegram link.

WORKER
v19
/health flags:
- customer_telegram_notifications: true
- customer_telegram_bot_shared_with_admin: true
- customer_telegram_auto_webhook_setup: true

NO ANDROID CHANGE
No Money/Sync/Backup/updater/licensing-engine changes.
