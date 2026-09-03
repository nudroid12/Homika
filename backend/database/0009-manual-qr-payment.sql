-- Homika Pro Patch 13C
-- QR payment + manual admin approval.
-- Run ONCE on D1 app-license-prod before Worker v14.

CREATE TABLE IF NOT EXISTS manual_payment_submissions (
    id TEXT PRIMARY KEY,
    checkout_id TEXT NOT NULL UNIQUE,
    payer_name TEXT NOT NULL,
    payment_reference TEXT,
    proof_object_key TEXT NOT NULL,
    proof_content_type TEXT NOT NULL,
    proof_size INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'submitted',
    admin_note TEXT,
    submitted_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TEXT,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (checkout_id) REFERENCES checkout_intents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_manual_payment_status
ON manual_payment_submissions(status, submitted_at);

CREATE INDEX IF NOT EXISTS idx_manual_payment_checkout
ON manual_payment_submissions(checkout_id);
