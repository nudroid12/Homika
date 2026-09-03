-- Homika Pro Patch 13A
-- Store pricing + 7-day one-trial-per-device/customer foundation.
-- Apply ONCE to the production D1 database before deploying Worker v9.

ALTER TABLE license_plans ADD COLUMN price_cents INTEGER;
ALTER TABLE license_plans ADD COLUMN compare_at_price_cents INTEGER;
ALTER TABLE license_plans ADD COLUMN currency TEXT NOT NULL DEFAULT 'MYR';
ALTER TABLE license_plans ADD COLUMN is_featured INTEGER NOT NULL DEFAULT 0;

ALTER TABLE licenses ADD COLUMN plan_key TEXT;
ALTER TABLE payments ADD COLUMN plan_key TEXT;

UPDATE licenses
SET plan_key = CASE lower(plan_type)
    WHEN 'trial' THEN 'trial_7d'
    WHEN 'monthly' THEN '1_month'
    WHEN 'lifetime' THEN 'lifetime'
    ELSE '1_year'
END
WHERE plan_key IS NULL OR trim(plan_key) = '';

CREATE INDEX IF NOT EXISTS idx_licenses_plan_key
ON licenses(plan_key);

CREATE INDEX IF NOT EXISTS idx_payments_plan_key
ON payments(plan_key);

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

CREATE INDEX IF NOT EXISTS idx_trial_redemptions_license
ON trial_redemptions(license_id);

CREATE INDEX IF NOT EXISTS idx_trial_redemptions_customer
ON trial_redemptions(customer_id);

-- Retire the old public catalogue rows. Existing licences remain valid.
UPDATE license_plans
SET sale_enabled = 0
WHERE product_id = 'homika_pro'
  AND plan_key IN ('trial', 'monthly', 'annual');

INSERT OR REPLACE INTO license_plans
(plan_key, product_id, name, duration_unit, duration_value, max_devices,
 sale_enabled, sort_order, price_cents, compare_at_price_cents, currency, is_featured)
VALUES
('trial_7d', 'homika_pro', '7-Day Free Trial', 'day', 7, 1, 0, 5, 0, 0, 'MYR', 0),
('1_month',  'homika_pro', '1 Month',  'month', 1, 3, 1, 10, 700, 1000, 'MYR', 0),
('3_month',  'homika_pro', '3 Months', 'month', 3, 3, 1, 20, 1900, 3000, 'MYR', 0),
('6_month',  'homika_pro', '6 Months', 'month', 6, 3, 1, 30, 3500, 6000, 'MYR', 0),
('1_year',   'homika_pro', '1 Year',   'year',  1, 3, 1, 40, 5900, 12000, 'MYR', 1),
('lifetime', 'homika_pro', 'Lifetime', 'lifetime', NULL, 3, 0, 90, NULL, NULL, 'MYR', 0);
