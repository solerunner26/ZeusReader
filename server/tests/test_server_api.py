from __future__ import annotations

import base64
import hashlib
import io
import json
import zipfile
from pathlib import Path

import httpx
from fastapi.testclient import TestClient

from app.config import ServerSettings
from app.main import create_app


def _make_sample_epub_bytes() -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        zf.writestr("mimetype", "application/epub+zip")
        zf.writestr("META-INF/container.xml", "<container/>")
    return buf.getvalue()


def test_pairing_token_required_and_health_ok(tmp_path: Path) -> None:
    settings = ServerSettings(pairing_token="secret-token", data_dir=tmp_path)
    app = create_app(settings=settings)
    client = TestClient(app)

    assert client.get("/health").status_code == 401
    assert client.get("/health", headers={"Authorization": "Bearer wrong"}).status_code == 401

    res = client.get("/health", headers={"Authorization": "Bearer secret-token"})
    assert res.status_code == 200
    body = res.json()
    assert body["status"] == "ok"
    assert body["api_version"] == "1.0"
    assert body["ai_enabled"] is False


def test_transfer_idempotency_domain_guard_and_verified_download(tmp_path: Path) -> None:
    epub_bytes = _make_sample_epub_bytes()
    expected_sha = hashlib.sha256(epub_bytes).hexdigest()

    def mock_http() -> httpx.Client:
        return httpx.Client(
            transport=httpx.MockTransport(
                lambda req: httpx.Response(
                    200,
                    content=epub_bytes,
                    headers={"content-type": "application/epub+zip"},
                )
            )
        )

    settings = ServerSettings(pairing_token="secret-token", data_dir=tmp_path)
    app = create_app(settings=settings, http_client_factory=mock_http)
    client = TestClient(app)
    headers = {"X-Zeus-Token": "secret-token"}

    # Domain outside allowlist is rejected
    bad = client.post(
        "/v1/transfers",
        headers=headers,
        json={
            "idempotency_key": "k-bad",
            "source": "Evil",
            "title": "Bad",
            "format": "EPUB",
            "download_url": "https://evil.example.com/book.epub",
        },
    )
    assert bad.status_code == 403

    payload = {
        "idempotency_key": "k-flatland",
        "source": "MemoryOfTheWorld",
        "title": "Flatland",
        "authors": ["Edwin A. Abbott"],
        "format": "EPUB",
        "download_url": "https://library.memoryoftheworld.org/flatland.epub",
    }
    r1 = client.post("/v1/transfers", headers=headers, json=payload)
    r2 = client.post("/v1/transfers", headers=headers, json=payload)
    assert r1.status_code == 200
    assert r2.status_code == 200
    assert r1.json()["job_id"] == r2.json()["job_id"]
    assert r1.json()["state"] == "ReadyOnServer"
    assert r1.json()["file_sha256"] == expected_sha

    dl = client.get(f"/v1/transfers/{r1.json()['job_id']}/file", headers=headers)
    assert dl.status_code == 200
    assert dl.content == epub_bytes
    assert dl.headers["x-file-sha256"] == expected_sha


def test_sync_passages_tombstones_and_conflicts(tmp_path: Path) -> None:
    settings = ServerSettings(pairing_token="secret-token", data_dir=tmp_path)
    client = TestClient(create_app(settings=settings))
    headers = {"Authorization": "Bearer secret-token"}

    r1 = client.post(
        "/v1/sync",
        headers=headers,
        json={
            "since_ms": 0,
            "records": [
                {
                    "record_type": "Passage",
                    "record_id": "p-1",
                    "edition_id": "ed-1",
                    "payload_json": json.dumps({"text": "Quote v1"}),
                    "base_version": 0,
                    "updated_at_ms": 1000,
                }
            ],
        },
    )
    assert r1.status_code == 200
    assert len(r1.json()["records"]) == 1
    assert r1.json()["records"][0]["version"] == 1

    # Stale base_version triggers conflict retention
    r2 = client.post(
        "/v1/sync",
        headers=headers,
        json={
            "since_ms": 0,
            "records": [
                {
                    "record_type": "Passage",
                    "record_id": "p-1",
                    "edition_id": "ed-1",
                    "payload_json": json.dumps({"text": "Conflicting edit"}),
                    "base_version": 0,
                    "updated_at_ms": 2000,
                }
            ],
        },
    )
    assert r2.status_code == 200
    assert len(r2.json()["conflicts"]) == 1


def test_two_mode_backups_and_disabled_ai(tmp_path: Path) -> None:
    settings = ServerSettings(pairing_token="secret-token", data_dir=tmp_path, ai_enabled=False)
    client = TestClient(create_app(settings=settings))
    headers = {"Authorization": "Bearer secret-token"}

    payload = base64.b64encode(b"encrypted-backup-blob").decode("ascii")
    created = client.post(
        "/v1/backups",
        headers=headers,
        json={"mode": "data_only", "manifest_json": json.dumps({"passages": 3}), "payload_b64": payload},
    )
    assert created.status_code == 200
    backup_id = created.json()["backup_id"]
    fetched = client.get(f"/v1/backups/{backup_id}", headers=headers)
    assert fetched.status_code == 200
    assert fetched.json()["payload_b64"] == payload

    ai = client.post(
        "/v1/ai/explain-passage",
        headers=headers,
        json={"passage_text": "Test passage", "book_title": "Book"},
    )
    assert ai.status_code == 503
