-- Homika Backend canonical schema
-- Reference/new-environment schema. Existing production D1 must NOT be wiped.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS products (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    default_max_devices INTEGER NOT NULL DEFAULT 3,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customers (
    id TEXT PRIMARY KEY,
    name TEXT,
    email TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS licenses (
    id TEXT PRIMARY KEY,
    license_key TEXT NOT NULL UNIQUE,
    product_id TEXT NOT NULL,
    customer_id TEXT,
    status TEXT NOT NULL DEFAULT 'active',
    expires_at TEXT NOT NULL,
    max_devices INTEGER NOT NULL DEFAULT 3,
    plan_type TEXT NOT NULL DEFAULT 'annual',
    plan_key TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS devices (
    id TEXT PRIMARY KEY,
    license_id TEXT NOT NULL,
    device_hash TEXT NOT NULL,
    device_name TEXT,
    status TEXT NOT NULL DEFAULT 'active',
    activated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TEXT,
    FOREIGN KEY (license_id) REFERENCES licenses(id),
    UNIQUE (license_id, device_hash)
);

CREATE TABLE IF NOT EXISTS payments (
    id TEXT PRIMARY KEY,
    license_id TEXT,
    provider TEXT,
    provider_payment_id TEXT UNIQUE,
    amount_cents INTEGER NOT NULL,
    currency TEXT NOT NULL DEFAULT 'MYR',
    status TEXT NOT NULL,
    plan_key TEXT,
    paid_at TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (license_id) REFERENCES licenses(id)
);

CREATE TABLE IF NOT EXISTS license_plans (
    plan_key TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    name TEXT NOT NULL,
    duration_unit TEXT NOT NULL,
    duration_value INTEGER,
    max_devices INTEGER NOT NULL DEFAULT 3,
    sale_enabled INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    price_cents INTEGER,
    compare_at_price_cents INTEGER,
    currency TEXT NOT NULL DEFAULT 'MYR',
    is_featured INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS cloud_backups (
    id TEXT PRIMARY KEY,
    license_id TEXT NOT NULL,
    device_hash TEXT NOT NULL,
    object_key TEXT NOT NULL UNIQUE,
    created_at_epoch_millis INTEGER NOT NULL,
    record_count INTEGER NOT NULL DEFAULT 0,
    format_version INTEGER NOT NULL DEFAULT 1,
    database_schema_version INTEGER NOT NULL DEFAULT 1,
    byte_size INTEGER NOT NULL,
    sha256 TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ready',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (license_id) REFERENCES licenses(id)
);

CREATE INDEX IF NOT EXISTS idx_licenses_key ON licenses(license_key);
CREATE INDEX IF NOT EXISTS idx_licenses_customer ON licenses(customer_id);
CREATE INDEX IF NOT EXISTS idx_licenses_plan_type ON licenses(plan_type);
CREATE INDEX IF NOT EXISTS idx_licenses_plan_key ON licenses(plan_key);
CREATE INDEX IF NOT EXISTS idx_devices_license ON devices(license_id);
CREATE INDEX IF NOT EXISTS idx_devices_hash ON devices(device_hash);
CREATE INDEX IF NOT EXISTS idx_payments_license ON payments(license_id);
CREATE INDEX IF NOT EXISTS idx_payments_plan_key ON payments(plan_key);
CREATE INDEX IF NOT EXISTS idx_license_plans_product ON license_plans(product_id);
CREATE INDEX IF NOT EXISTS idx_license_plans_sale_enabled ON license_plans(product_id, sale_enabled);
CREATE INDEX IF NOT EXISTS idx_cloud_backups_license_created
ON cloud_backups(license_id, created_at_epoch_millis DESC);
CREATE INDEX IF NOT EXISTS idx_cloud_backups_status
ON cloud_backups(license_id, status);

INSERT OR IGNORE INTO products (id, name, default_max_devices)
VALUES ('homika_pro', 'Homika Pro', 3);

INSERT OR REPLACE INTO license_plans
(plan_key, product_id, name, duration_unit, duration_value, max_devices,
 sale_enabled, sort_order, price_cents, compare_at_price_cents, currency, is_featured)
VALUES
('trial_7d', 'homika_pro', '7-Day Free Trial', 'day', 7, 1, 0, 5, 0, 0, 'MYR', 0),
('1_month',  'homika_pro', '1 Month',  'month', 1, 3, 1, 10, 700, 1000, 'MYR', 0),
('3_month',  'homika_pro', '3 Months', 'month', 3, 3, 1, 20, 1900, 3000, 'MYR', 0),
('6_month',  'homika_pro', '6 Months', 'month', 6, 3, 1, 30, 3500, 6000, 'MYR', 0),
('1_year',   'homika_pro', '1 Year',   'year', 1, 3, 1, 40, 5900, 12000, 'MYR', 1),
('lifetime', 'homika_pro', 'Lifetime', 'lifetime', NULL, 3, 0, 90, NULL, NULL, 'MYR', 0);

CREATE TABLE IF NOT EXISTS trial_redemptions (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    license_id TEXT NOT NULL UNIQUE,
    customer_id TEXT NOT NULL,
    customer_hash TEXT NOT NULL,
    device_hash TEXT NOT NULL,
    redeemed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (license_id) REFERENCES licenses(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    UNIQUE (product_id, customer_hash),
    UNIQUE (product_id, device_hash)
);

CREATE INDEX IF NOT EXISTS idx_trial_redemptions_license ON trial_redemptions(license_id);
CREATE INDEX IF NOT EXISTS idx_trial_redemptions_customer ON trial_redemptions(customer_id);

-- Cloud Sync protocol v1
CREATE TABLE IF NOT EXISTS cloud_sync_events (
    sequence INTEGER PRIMARY KEY AUTOINCREMENT,
    id TEXT NOT NULL UNIQUE,
    license_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    revision INTEGER NOT NULL,
    base_revision INTEGER NOT NULL,
    updated_at_epoch_millis INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    payload_b64 TEXT NOT NULL,
    payload_sha256 TEXT NOT NULL,
    content_sha256 TEXT NOT NULL,
    source_device_hash TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (license_id) REFERENCES licenses(id)
);

CREATE INDEX IF NOT EXISTS idx_cloud_sync_events_license_sequence
ON cloud_sync_events(license_id, sequence);
CREATE INDEX IF NOT EXISTS idx_cloud_sync_events_license_status_sequence
ON cloud_sync_events(license_id, status, sequence);

CREATE TABLE IF NOT EXISTS cloud_sync_items (
    license_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    revision INTEGER NOT NULL,
    updated_at_epoch_millis INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    payload_b64 TEXT NOT NULL,
    payload_sha256 TEXT NOT NULL,
    content_sha256 TEXT NOT NULL,
    source_device_hash TEXT NOT NULL,
    server_sequence INTEGER NOT NULL,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (license_id, entity_type, entity_id),
    FOREIGN KEY (license_id) REFERENCES licenses(id)
);

CREATE INDEX IF NOT EXISTS idx_cloud_sync_items_license_sequence
ON cloud_sync_items(license_id, server_sequence);


-- Homika Cloud Sync v2: encrypted per-device snapshots

CREATE TABLE IF NOT EXISTS cloud_sync_snapshots (
    license_id TEXT NOT NULL,
    device_hash TEXT NOT NULL,
    object_key TEXT NOT NULL UNIQUE,
    updated_at_epoch_millis INTEGER NOT NULL,
    record_count INTEGER NOT NULL DEFAULT 0,
    format_version INTEGER NOT NULL DEFAULT 1,
    database_schema_version INTEGER NOT NULL DEFAULT 1,
    byte_size INTEGER NOT NULL,
    sha256 TEXT NOT NULL,
    content_sha256 TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ready',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (license_id, device_hash),
    FOREIGN KEY (license_id) REFERENCES licenses(id)
);

CREATE INDEX IF NOT EXISTS idx_cloud_sync_snapshots_license_updated
ON cloud_sync_snapshots(license_id, updated_at_epoch_millis DESC);


-- Trial eligibility ledger v2

CREATE TABLE IF NOT EXISTS trial_claims_v2 (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    license_id TEXT NOT NULL UNIQUE,
    email_hash TEXT NOT NULL,
    device_hash TEXT NOT NULL,
    redeemed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (license_id) REFERENCES licenses(id),
    UNIQUE (product_id, email_hash),
    UNIQUE (product_id, device_hash)
);

CREATE INDEX IF NOT EXISTS idx_trial_claims_v2_license
ON trial_claims_v2(license_id);

CREATE INDEX IF NOT EXISTS idx_trial_claims_v2_device
ON trial_claims_v2(product_id, device_hash);

CREATE INDEX IF NOT EXISTS idx_trial_claims_v2_email
ON trial_claims_v2(product_id, email_hash);
