-- Homika Pro Patch 13C.8
-- Purchase account activation with email + 6-digit PIN.
-- IMPORTANT: Cloudflare D1 Console should run ONE statement at a time.

-- PART 01
ALTER TABLE checkout_intents ADD COLUMN purchase_pin_salt TEXT;

-- PART 02
ALTER TABLE checkout_intents ADD COLUMN purchase_pin_hash TEXT;

-- PART 03
ALTER TABLE checkout_intents ADD COLUMN purchase_pin_version INTEGER;

-- PART 04
CREATE TABLE IF NOT EXISTS purchase_accounts (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    email_hash TEXT NOT NULL,
    pin_salt TEXT NOT NULL,
    pin_hash TEXT NOT NULL,
    pin_version INTEGER NOT NULL DEFAULT 1,
    license_id TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TEXT,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (license_id) REFERENCES licenses(id),
    UNIQUE (product_id, email_hash)
);

-- PART 05
CREATE INDEX IF NOT EXISTS idx_purchase_accounts_license
ON purchase_accounts(license_id);

-- PART 06
CREATE TABLE IF NOT EXISTS purchase_pin_security (
    product_id TEXT NOT NULL,
    email_hash TEXT NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TEXT,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, email_hash),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
