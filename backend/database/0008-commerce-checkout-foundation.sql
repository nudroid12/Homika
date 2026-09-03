-- Homika Pro Patch 13B
-- Store checkout + same-licence renewal foundation.
-- Run ONCE on D1 app-license-prod before Worker v13.

CREATE TABLE IF NOT EXISTS checkout_intents (
    id TEXT PRIMARY KEY,
    public_token TEXT NOT NULL UNIQUE,
    product_id TEXT NOT NULL,
    action TEXT NOT NULL,
    license_id TEXT,
    customer_email TEXT,
    plan_key TEXT,
    amount_cents INTEGER,
    currency TEXT NOT NULL DEFAULT 'MYR',
    status TEXT NOT NULL DEFAULT 'pending',
    provider TEXT,
    provider_reference TEXT,
    target_expires_at TEXT,
    resulting_license_id TEXT,
    resulting_license_key TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TEXT NOT NULL,
    paid_at TEXT,
    completed_at TEXT,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (license_id) REFERENCES licenses(id),
    FOREIGN KEY (plan_key) REFERENCES license_plans(plan_key),
    FOREIGN KEY (resulting_license_id) REFERENCES licenses(id)
);

CREATE INDEX IF NOT EXISTS idx_checkout_intents_public_token
ON checkout_intents(public_token);

CREATE INDEX IF NOT EXISTS idx_checkout_intents_status
ON checkout_intents(status, created_at);

CREATE INDEX IF NOT EXISTS idx_checkout_intents_license
ON checkout_intents(license_id, created_at);

CREATE INDEX IF NOT EXISTS idx_checkout_intents_provider_reference
ON checkout_intents(provider, provider_reference);
