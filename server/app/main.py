from __future__ import annotations

import base64
import hashlib
import json
import time
import uuid
from pathlib import Path
from typing import Any, Literal
from urllib.parse import urlparse

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Query, status
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

from app.ai_client import OllamaClient
from app.auth import verify_pairing_token
from app.config import ServerSettings
from app.converter import OfflineLegacyConverter
from app.db import ServerDatabase
from spike.provider import MemoryOfTheWorldConnector, validate_magic_bytes_and_container
from spike.slum_status import fetch_slum_status


class TransferCreateRequest(BaseModel):
    idempotency_key: str
    source: str
    title: str
    authors: list[str] = Field(default_factory=list)
    language: str = ""
    year: str = ""
    format: str
    download_url: str


class TransferJobResponse(BaseModel):
    job_id: str
    idempotency_key: str
    source: str
    title: str
    authors: list[str]
    language: str
    year: str
    format: str
    state: str
    progress_percent: int
    error_message: str | None = None
    browser_url: str | None = None
    file_sha256: str | None = None
    byte_size: int | None = None
    created_at_ms: int
    updated_at_ms: int


class SyncRecordIn(BaseModel):
    record_type: str
    record_id: str
    edition_id: str = ""
    payload_json: str
    base_version: int = 0
    updated_at_ms: int
    deleted_at_ms: int | None = None


class SyncRequest(BaseModel):
    since_ms: int = 0
    records: list[SyncRecordIn] = Field(default_factory=list)


class BackupCreateRequest(BaseModel):
    mode: Literal["data_only", "full"]
    manifest_json: str
    payload_b64: str


class AiPassageRequest(BaseModel):
    passage_text: str
    book_title: str = ""
    target_language: str = "en"


def _now_ms() -> int:
    return int(time.time() * 1000)


def _dir_size_bytes(path: Path) -> int:
    if not path.exists():
        return 0
    return sum(p.stat().st_size for p in path.rglob("*") if p.is_file())


def create_app(
    settings: ServerSettings | None = None,
    http_client_factory: Any | None = None,
) -> FastAPI:
    cfg = settings or ServerSettings()
    cfg.books_dir.mkdir(parents=True, exist_ok=True)
    cfg.backups_dir.mkdir(parents=True, exist_ok=True)
    cfg.converted_dir.mkdir(parents=True, exist_ok=True)

    db = ServerDatabase(cfg.db_path)
    db.recover_interrupted_transfers()

    app = FastAPI(title="ZeusReader Private Server", version=cfg.api_version)

    def require_token(
        authorization: str | None = Header(default=None),
        x_zeus_token: str | None = Header(default=None),
    ) -> str:
        return verify_pairing_token(cfg, authorization=authorization, x_zeus_token=x_zeus_token)

    def _row_to_job(row: Any) -> TransferJobResponse:
        return TransferJobResponse(
            job_id=row["job_id"],
            idempotency_key=row["idempotency_key"],
            source=row["source"],
            title=row["title"],
            authors=json.loads(row["authors_json"]),
            language=row["language"],
            year=row["year"],
            format=row["format"],
            state=row["state"],
            progress_percent=row["progress_percent"],
            error_message=row["error_message"],
            browser_url=row["browser_url"],
            file_sha256=row["file_sha256"],
            byte_size=row["byte_size"],
            created_at_ms=row["created_at_ms"],
            updated_at_ms=row["updated_at_ms"],
        )

    @app.get("/health")
    def health(_: str = Depends(require_token)) -> dict[str, Any]:
        return {
            "status": "ok",
            "api_version": cfg.api_version,
            "min_supported_client_api": cfg.min_supported_client_api,
            "ai_enabled": cfg.ai_enabled,
            "storage_used_bytes": _dir_size_bytes(cfg.data_dir),
            "storage_quota_bytes": cfg.storage_quota_bytes,
        }

    @app.get("/v1/slum/status")
    def slum_status(_: str = Depends(require_token)) -> dict[str, Any]:
        cache_path = cfg.data_dir / "slum_cache.json"
        if http_client_factory is not None:
            with http_client_factory() as client:
                snap = fetch_slum_status(cache_path=cache_path, client=client)
        else:
            snap = fetch_slum_status(cache_path=cache_path)
        return {
            "fetched_at_utc": snap.fetched_at_utc,
            "stale": snap.stale,
            "providers": [
                {
                    "name": p.name,
                    "overall_status": p.overall_status,
                    "domains": [
                        {"host": d.host, "url": d.url, "role": d.role, "health": d.health}
                        for d in p.domains
                    ],
                }
                for p in snap.providers
            ],
        }

    @app.get("/v1/search")
    def search_books(
        q: str = Query(min_length=1),
        _: str = Depends(require_token),
    ) -> dict[str, Any]:
        connector = MemoryOfTheWorldConnector()
        if http_client_factory is not None:
            with http_client_factory() as client:
                results = connector.search(q, client=client)
        else:
            results = connector.search(q)
        return {
            "query": q,
            "results": [
                {
                    "source": r.source,
                    "title": r.title,
                    "authors": r.authors,
                    "language": r.language,
                    "year": r.year,
                    "format": r.format,
                    "byte_size": r.byte_size,
                    "download_url": r.download_url,
                }
                for r in results
            ],
        }

    def _validate_provider_url(url: str) -> None:
        parsed = urlparse(url)
        host = (parsed.hostname or "").lower()
        if parsed.scheme not in ("https", "http") or not host:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid download URL")
        if not any(host == allowed or host.endswith(f".{allowed}") for allowed in cfg.allowed_provider_domains):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Provider domain '{host}' is not in allowlist",
            )

    def _execute_transfer(job_id: str) -> None:
        with db.connect() as conn:
            row = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            if row is None or row["state"] == "Cancelled":
                return
            download_url = row["download_url"]
            expected_format = row["format"]

        now = _now_ms()
        with db.connect() as conn:
            conn.execute(
                "UPDATE transfer_jobs SET state = 'Downloading', progress_percent = 25, updated_at_ms = ? WHERE job_id = ?",
                (now, job_id),
            )

        temp_path = cfg.books_dir / f"{job_id}.part"
        try:
            if http_client_factory is not None:
                with http_client_factory() as client:
                    resp = client.get(download_url, follow_redirects=True)
            else:
                with httpx.Client(timeout=60.0) as client:
                    resp = client.get(download_url, follow_redirects=True)

            content_type = resp.headers.get("content-type", "").lower()
            if resp.status_code in (403, 429) or "text/html" in content_type:
                with db.connect() as conn:
                    conn.execute(
                        """
                        UPDATE transfer_jobs
                        SET state = 'NeedsBrowser', browser_url = ?, error_message = ?, updated_at_ms = ?
                        WHERE job_id = ?
                        """,
                        (download_url, "Provider requires browser verification", _now_ms(), job_id),
                    )
                return

            resp.raise_for_status()
            data = resp.content
            temp_path.write_bytes(data)

            with db.connect() as conn:
                conn.execute(
                    "UPDATE transfer_jobs SET state = 'Verifying', progress_percent = 85, updated_at_ms = ? WHERE job_id = ?",
                    (_now_ms(), job_id),
                )

            validation = validate_magic_bytes_and_container(temp_path, expected_format)
            if not validation.valid:
                temp_path.unlink(missing_ok=True)
                with db.connect() as conn:
                    conn.execute(
                        "UPDATE transfer_jobs SET state = 'Failed', error_message = ?, updated_at_ms = ? WHERE job_id = ?",
                        (validation.reason, _now_ms(), job_id),
                    )
                return

            sha256 = hashlib.sha256(data).hexdigest()
            final_path = cfg.books_dir / f"{sha256}.{expected_format.lower()}"
            temp_path.replace(final_path)

            with db.connect() as conn:
                conn.execute(
                    """
                    UPDATE transfer_jobs
                    SET state = 'ReadyOnServer', progress_percent = 100, file_sha256 = ?, byte_size = ?,
                        local_path = ?, error_message = NULL, updated_at_ms = ?
                    WHERE job_id = ?
                    """,
                    (sha256, len(data), str(final_path), _now_ms(), job_id),
                )
        except Exception as exc:
            temp_path.unlink(missing_ok=True)
            with db.connect() as conn:
                conn.execute(
                    "UPDATE transfer_jobs SET state = 'Failed', error_message = ?, updated_at_ms = ? WHERE job_id = ?",
                    (str(exc), _now_ms(), job_id),
                )

    @app.post("/v1/transfers", response_model=TransferJobResponse)
    def create_transfer(
        req: TransferCreateRequest,
        _: str = Depends(require_token),
    ) -> TransferJobResponse:
        _validate_provider_url(req.download_url)
        if _dir_size_bytes(cfg.data_dir) >= cfg.storage_quota_bytes:
            raise HTTPException(status_code=status.HTTP_507_INSUFFICIENT_STORAGE, detail="Server storage quota reached")

        with db.connect() as conn:
            existing = conn.execute(
                "SELECT * FROM transfer_jobs WHERE idempotency_key = ?",
                (req.idempotency_key,),
            ).fetchone()
            if existing is not None:
                return _row_to_job(existing)

            job_id = str(uuid.uuid4())
            now = _now_ms()
            conn.execute(
                """
                INSERT INTO transfer_jobs (
                    job_id, idempotency_key, source, title, authors_json, language, year,
                    format, download_url, state, progress_percent, created_at_ms, updated_at_ms
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Queued', 0, ?, ?)
                """,
                (
                    job_id,
                    req.idempotency_key,
                    req.source,
                    req.title,
                    json.dumps(req.authors),
                    req.language,
                    req.year,
                    req.format.upper(),
                    req.download_url,
                    now,
                    now,
                ),
            )

        _execute_transfer(job_id)
        with db.connect() as conn:
            row = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            return _row_to_job(row)

    @app.get("/v1/transfers", response_model=list[TransferJobResponse])
    def list_transfers(_: str = Depends(require_token)) -> list[TransferJobResponse]:
        with db.connect() as conn:
            rows = conn.execute("SELECT * FROM transfer_jobs ORDER BY created_at_ms DESC").fetchall()
            return [_row_to_job(r) for r in rows]

    @app.get("/v1/transfers/{job_id}", response_model=TransferJobResponse)
    def get_transfer(job_id: str, _: str = Depends(require_token)) -> TransferJobResponse:
        with db.connect() as conn:
            row = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            if row is None:
                raise HTTPException(status_code=404, detail="Transfer job not found")
            return _row_to_job(row)

    @app.post("/v1/transfers/{job_id}/cancel", response_model=TransferJobResponse)
    def cancel_transfer(job_id: str, _: str = Depends(require_token)) -> TransferJobResponse:
        with db.connect() as conn:
            row = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            if row is None:
                raise HTTPException(status_code=404, detail="Transfer job not found")
            conn.execute(
                "UPDATE transfer_jobs SET state = 'Cancelled', updated_at_ms = ? WHERE job_id = ?",
                (_now_ms(), job_id),
            )
            updated = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            return _row_to_job(updated)

    @app.post("/v1/transfers/{job_id}/retry", response_model=TransferJobResponse)
    def retry_transfer(job_id: str, _: str = Depends(require_token)) -> TransferJobResponse:
        with db.connect() as conn:
            row = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            if row is None:
                raise HTTPException(status_code=404, detail="Transfer job not found")
            conn.execute(
                "UPDATE transfer_jobs SET state = 'Queued', error_message = NULL, updated_at_ms = ? WHERE job_id = ?",
                (_now_ms(), job_id),
            )
        _execute_transfer(job_id)
        with db.connect() as conn:
            updated = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            return _row_to_job(updated)

    @app.get("/v1/transfers/{job_id}/file")
    def download_transfer_file(job_id: str, _: str = Depends(require_token)) -> FileResponse:
        with db.connect() as conn:
            row = conn.execute("SELECT * FROM transfer_jobs WHERE job_id = ?", (job_id,)).fetchone()
            if row is None or row["state"] != "ReadyOnServer" or not row["local_path"]:
                raise HTTPException(status_code=404, detail="Verified file not ready")
            file_path = Path(row["local_path"])
            if not file_path.exists():
                raise HTTPException(status_code=404, detail="File missing on server")
            return FileResponse(
                path=str(file_path),
                filename=f"{row['title']}.{row['format'].lower()}",
                headers={"X-File-Sha256": row["file_sha256"] or ""},
            )

    @app.post("/v1/sync")
    def sync_records(req: SyncRequest, _: str = Depends(require_token)) -> dict[str, Any]:
        """
        Versioned + tombstone sync covering library, highlights, vocab,
        and PassageFolder / Passage / PassageTag (SCOPE-ADDENDUM-v2 §B.6).
        """
        with db.connect() as conn:
            for item in req.records:
                existing = conn.execute(
                    "SELECT * FROM sync_records WHERE record_type = ? AND record_id = ?",
                    (item.record_type, item.record_id),
                ).fetchone()

                if existing is None:
                    conn.execute(
                        """
                        INSERT INTO sync_records (
                            record_type, record_id, edition_id, payload_json, version, updated_at_ms, deleted_at_ms
                        ) VALUES (?, ?, ?, ?, 1, ?, ?)
                        """,
                        (
                            item.record_type,
                            item.record_id,
                            item.edition_id,
                            item.payload_json,
                            item.updated_at_ms,
                            item.deleted_at_ms,
                        ),
                    )
                else:
                    server_ver = int(existing["version"])
                    if item.base_version == server_ver:
                        # Idempotent check: if payload & deleted_at match, keep version unchanged
                        if (
                            existing["payload_json"] == item.payload_json
                            and existing["deleted_at_ms"] == item.deleted_at_ms
                        ):
                            continue
                        conn.execute(
                            """
                            UPDATE sync_records
                            SET payload_json = ?, edition_id = ?, version = ?, updated_at_ms = ?, deleted_at_ms = ?
                            WHERE record_type = ? AND record_id = ?
                            """,
                            (
                                item.payload_json,
                                item.edition_id,
                                server_ver + 1,
                                item.updated_at_ms,
                                item.deleted_at_ms,
                                item.record_type,
                                item.record_id,
                            ),
                        )
                    else:
                        # Concurrent edit conflict: retain both versions for user review
                        conn.execute(
                            """
                            INSERT INTO sync_conflicts (
                                conflict_id, record_type, record_id, server_payload_json, client_payload_json,
                                server_version, client_base_version, resolved, created_at_ms
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)
                            """,
                            (
                                str(uuid.uuid4()),
                                item.record_type,
                                item.record_id,
                                existing["payload_json"],
                                item.payload_json,
                                server_ver,
                                item.base_version,
                                _now_ms(),
                            ),
                        )

            server_rows = conn.execute(
                "SELECT * FROM sync_records WHERE updated_at_ms >= ? ORDER BY updated_at_ms ASC",
                (req.since_ms,),
            ).fetchall()
            conflicts = conn.execute(
                "SELECT * FROM sync_conflicts WHERE resolved = 0 ORDER BY created_at_ms DESC"
            ).fetchall()

        return {
            "server_time_ms": _now_ms(),
            "records": [
                {
                    "record_type": r["record_type"],
                    "record_id": r["record_id"],
                    "edition_id": r["edition_id"],
                    "payload_json": r["payload_json"],
                    "version": r["version"],
                    "updated_at_ms": r["updated_at_ms"],
                    "deleted_at_ms": r["deleted_at_ms"],
                }
                for r in server_rows
            ],
            "conflicts": [
                {
                    "conflict_id": c["conflict_id"],
                    "record_type": c["record_type"],
                    "record_id": c["record_id"],
                    "server_payload_json": c["server_payload_json"],
                    "client_payload_json": c["client_payload_json"],
                    "server_version": c["server_version"],
                    "client_base_version": c["client_base_version"],
                }
                for c in conflicts
            ],
        }

    @app.post("/v1/backups")
    def create_backup(req: BackupCreateRequest, _: str = Depends(require_token)) -> dict[str, Any]:
        raw = base64.b64decode(req.payload_b64)
        sha256 = hashlib.sha256(raw).hexdigest()
        backup_id = f"{int(time.time())}_{uuid.uuid4().hex[:8]}"
        meta = {
            "backup_id": backup_id,
            "mode": req.mode,
            "sha256": sha256,
            "byte_size": len(raw),
            "created_at_ms": _now_ms(),
            "manifest": json.loads(req.manifest_json),
        }
        (cfg.backups_dir / f"{backup_id}.meta.json").write_text(json.dumps(meta), encoding="utf-8")
        (cfg.backups_dir / f"{backup_id}.bin").write_bytes(raw)
        return meta

    @app.get("/v1/backups")
    def list_backups(_: str = Depends(require_token)) -> list[dict[str, Any]]:
        items = []
        for meta_file in sorted(cfg.backups_dir.glob("*.meta.json"), reverse=True):
            items.append(json.loads(meta_file.read_text(encoding="utf-8")))
        return items

    @app.get("/v1/backups/{backup_id}")
    def get_backup(backup_id: str, _: str = Depends(require_token)) -> dict[str, Any]:
        meta_file = cfg.backups_dir / f"{backup_id}.meta.json"
        bin_file = cfg.backups_dir / f"{backup_id}.bin"
        if not meta_file.exists() or not bin_file.exists():
            raise HTTPException(status_code=404, detail="Backup not found")
        meta = json.loads(meta_file.read_text(encoding="utf-8"))
        meta["payload_b64"] = base64.b64encode(bin_file.read_bytes()).decode("ascii")
        return meta

    @app.post("/v1/ai/explain-passage")
    def explain_passage(req: AiPassageRequest, _: str = Depends(require_token)) -> dict[str, Any]:
        if not cfg.ai_enabled:
            raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Server AI is disabled")
        explanation, model = OllamaClient(cfg).explain_passage(req.book_title, req.passage_text)
        return {"explanation": explanation, "model": model}

    @app.post("/v1/ai/suggest-tags")
    def suggest_tags(req: AiPassageRequest, _: str = Depends(require_token)) -> dict[str, Any]:
        if not cfg.ai_enabled:
            raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Server AI is disabled")
        tags, model = OllamaClient(cfg).suggest_tags(req.book_title, req.passage_text)
        return {"tags": tags, "model": model}

    @app.post("/v1/convert")
    def convert_legacy_book(
        filename: str,
        source_format: str,
        _: str = Depends(require_token),
    ) -> dict[str, Any]:
        safe_name = Path(filename).name
        source_path = cfg.books_dir / safe_name
        result = OfflineLegacyConverter().convert(source_path, source_format, cfg.converted_dir)
        if not result.success:
            raise HTTPException(status_code=422, detail=result.error_message or "Conversion failed")
        return {
            "success": True,
            "target_format": result.target_format,
            "output_file": result.output_path.name if result.output_path else "",
        }

    return app


app = create_app()
