CREATE INDEX IF NOT EXISTS idx_manual_payment_status
ON manual_payment_submissions(status, submitted_at);
