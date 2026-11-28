CREATE TABLE IF NOT EXISTS transfer_jobs (
    job_id TEXT PRIMARY KEY,
    idempotency_key TEXT UNIQUE NOT NULL,
    source TEXT NOT NULL,
    title TEXT NOT NULL,
    authors_json TEXT NOT NULL DEFAULT '[]',
    language TEXT NOT NULL DEFAULT '',
    year TEXT NOT NULL DEFAULT '',
    format TEXT NOT NULL,
    download_url TEXT NOT NULL,
    state TEXT NOT NULL,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    browser_url TEXT,
    file_sha256 TEXT,
    byte_size INTEGER,
    r2_key TEXT,
    created_at_ms INTEGER NOT NULL,
    updated_at_ms INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_records (
    record_type TEXT NOT NULL,
    record_id TEXT NOT NULL,
    edition_id TEXT NOT NULL DEFAULT '',
    payload_json TEXT NOT NULL,
    version INTEGER NOT NULL,
    updated_at_ms INTEGER NOT NULL,
    deleted_at_ms INTEGER,
    PRIMARY KEY (record_type, record_id)
);

CREATE TABLE IF NOT EXISTS backups (
    backup_id TEXT PRIMARY KEY,
    mode TEXT NOT NULL,
    manifest_json TEXT NOT NULL,
    sha256 TEXT NOT NULL,
    byte_size INTEGER NOT NULL,
    r2_key TEXT NOT NULL,
    created_at_ms INTEGER NOT NULL
);
