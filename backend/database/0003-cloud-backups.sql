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

CREATE INDEX IF NOT EXISTS idx_cloud_backups_license_created
ON cloud_backups(license_id, created_at_epoch_millis DESC);

CREATE INDEX IF NOT EXISTS idx_cloud_backups_status
ON cloud_backups(license_id, status);
