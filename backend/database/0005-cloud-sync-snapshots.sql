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
