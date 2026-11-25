from __future__ import annotations

import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS slum_cache (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    snapshot_json TEXT NOT NULL,
    fetched_at_utc TEXT NOT NULL,
    stale INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transfer_jobs (
    job_id TEXT PRIMARY KEY,
    idempotency_key TEXT UNIQUE NOT NULL,
    source TEXT NOT NULL,
    title TEXT NOT NULL,
    authors_json TEXT NOT NULL,
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
    local_path TEXT,
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

CREATE TABLE IF NOT EXISTS sync_conflicts (
    conflict_id TEXT PRIMARY KEY,
    record_type TEXT NOT NULL,
    record_id TEXT NOT NULL,
    server_payload_json TEXT NOT NULL,
    client_payload_json TEXT NOT NULL,
    server_version INTEGER NOT NULL,
    client_base_version INTEGER NOT NULL,
    resolved INTEGER NOT NULL DEFAULT 0,
    created_at_ms INTEGER NOT NULL
);
"""


class ServerDatabase:
    def __init__(self, db_path: Path) -> None:
        self.db_path = db_path
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_schema()

    @contextmanager
    def connect(self) -> Iterator[sqlite3.Connection]:
        conn = sqlite3.connect(str(self.db_path), timeout=30.0, check_same_thread=False)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL;")
        conn.execute("PRAGMA synchronous=NORMAL;")
        conn.execute("PRAGMA foreign_keys=ON;")
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def _init_schema(self) -> None:
        with self.connect() as conn:
            conn.executescript(SCHEMA_SQL)
            existing = conn.execute("SELECT version FROM schema_version LIMIT 1").fetchone()
            if existing is None:
                conn.execute("INSERT INTO schema_version (version) VALUES (1)")

    def recover_interrupted_transfers(self) -> int:
        """Marks in-flight server transfers interrupted by restart back to Queued."""
        with self.connect() as conn:
            cur = conn.execute(
                """
                UPDATE transfer_jobs
                SET state = 'Queued',
                    error_message = 'Resumed after server restart'
                WHERE state IN ('Downloading', 'Verifying')
                """
            )
            return cur.rowcount
