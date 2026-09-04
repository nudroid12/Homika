-- HOMIKA PRO PATCH 13C.7
-- D1 MIGRATION 0010
-- Customer Telegram Notifications
--
-- IMPORTANT FOR CLOUDFLARE D1 CONSOLE:
-- Run ONE PART at a time. Wait for Success before continuing.

-- ============================================================
-- PART 01 - Telegram webhook configuration table
-- ============================================================
CREATE TABLE IF NOT EXISTS homika_telegram_config (
    id TEXT PRIMARY KEY,
    bot_username TEXT NOT NULL,
    webhook_secret TEXT NOT NULL,
    webhook_url TEXT NOT NULL,
    configured_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- PART 02 - Checkout to customer Telegram binding table
-- ============================================================
CREATE TABLE IF NOT EXISTS checkout_telegram_links (
    id TEXT PRIMARY KEY,
    checkout_id TEXT NOT NULL UNIQUE,
    link_token TEXT NOT NULL UNIQUE,
    chat_id TEXT,
    telegram_username TEXT,
    telegram_first_name TEXT,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    linked_at TEXT,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (checkout_id) REFERENCES checkout_intents(id) ON DELETE CASCADE
);

-- ============================================================
-- PART 03 - Admin/status lookup index
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_checkout_telegram_status
ON checkout_telegram_links(status, updated_at);

-- ============================================================
-- SELESAI
-- ============================================================
-- After PART 01, PART 02 and PART 03 all return Success,
-- migration 0010 is complete.
