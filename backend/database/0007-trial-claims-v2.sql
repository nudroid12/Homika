-- Homika Pro Patch 13A.3
-- Independent hashed trial eligibility ledger.
-- Run ONCE on D1 app-license-prod before Worker v12.

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
