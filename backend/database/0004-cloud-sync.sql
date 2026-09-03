-- Homika Cloud Sync protocol v1 foundation
-- Safe to run against the existing production D1 database.

PRAGMA foreign_keys = ON;

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
