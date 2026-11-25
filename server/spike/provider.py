from __future__ import annotations

import hashlib
import os
import socket
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Protocol
from urllib.parse import quote, urlparse

import httpx

ZIP_MAGIC = b"PK\x03\x04"
PDF_MAGIC = b"%PDF-"
ELF_MAGIC = b"\x7fELF"
PE_EXE_MAGIC = b"MZ"
SHEBANG_MAGIC = b"#!"


@dataclass(frozen=True)
class ProviderCapabilities:
    search: bool
    server_download: bool
    continue_in_browser: bool


@dataclass(frozen=True)
class EditionCandidate:
    source: str
    title: str
    authors: list[str]
    language: str
    year: str
    format: str
    byte_size: int | None
    download_url: str
    needs_browser: bool = False


@dataclass(frozen=True)
class MagicByteValidation:
    valid: bool
    detected_type: str
    reason: str


@dataclass(frozen=True)
class DownloadVerification:
    file_path: Path
    sha256: str
    byte_size: int
    validation: MagicByteValidation
    needs_browser: bool


class ProviderConnector(Protocol):
    name: str
    capabilities: ProviderCapabilities

    def search(self, query: str) -> list[EditionCandidate]: ...
    def download(self, candidate: EditionCandidate, dest_dir: Path) -> DownloadVerification: ...


def resolve_via_doh(hostname: str, doh_url: str = "https://cloudflare-dns.com/dns-query") -> str | None:
    """Resolve an A record via DNS-over-HTTPS when ISP DNS blocks a provider domain."""
    try:
        resp = httpx.get(
            doh_url,
            params={"name": hostname, "type": "A"},
            headers={"Accept": "application/dns-json"},
            timeout=8.0,
        )
        resp.raise_for_status()
        answers = resp.json().get("Answer", [])
        for ans in answers:
            if ans.get("type") == 1 and ans.get("data"):
                return str(ans["data"])
    except Exception:
        return None
    return None


def validate_magic_bytes_and_container(file_path: Path, expected_format: str) -> MagicByteValidation:
    """Inspect file header magic bytes and ZIP container structure; reject disguised executables."""
    fmt = expected_format.lower().strip().lstrip(".")
    with file_path.open("rb") as f:
        header = f.read(64)

    if not header:
        return MagicByteValidation(valid=False, detected_type="empty", reason="File is empty")

    if header.startswith(PE_EXE_MAGIC) or header.startswith(ELF_MAGIC) or header.startswith(SHEBANG_MAGIC):
        return MagicByteValidation(
            valid=False,
            detected_type="executable_or_script",
            reason="Rejected executable/script signature",
        )

    if fmt == "pdf":
        if header.startswith(PDF_MAGIC):
            return MagicByteValidation(valid=True, detected_type="pdf", reason="Valid %PDF- header")
        if header.startswith(ZIP_MAGIC):
            return MagicByteValidation(
                valid=False,
                detected_type="zip_or_apk",
                reason="Expected PDF but found ZIP/APK container magic bytes",
            )
        return MagicByteValidation(valid=False, detected_type="unknown", reason="Missing %PDF- header")

    if fmt == "epub":
        if not header.startswith(ZIP_MAGIC):
            return MagicByteValidation(valid=False, detected_type="not_zip", reason="Missing PK ZIP header")
        try:
            with zipfile.ZipFile(file_path, "r") as zf:
                names = set(zf.namelist())
                if "AndroidManifest.xml" in names or "classes.dex" in names:
                    return MagicByteValidation(
                        valid=False,
                        detected_type="android_apk",
                        reason="Rejected disguised Android APK inside ZIP container",
                    )
                if "mimetype" not in names:
                    return MagicByteValidation(
                        valid=False,
                        detected_type="zip_non_epub",
                        reason="EPUB container missing mimetype entry",
                    )
                mimetype_bytes = zf.read("mimetype").strip()
                if mimetype_bytes != b"application/epub+zip":
                    return MagicByteValidation(
                        valid=False,
                        detected_type=mimetype_bytes.decode("utf-8", errors="replace"),
                        reason="EPUB mimetype is not application/epub+zip",
                    )
                if "META-INF/container.xml" not in names:
                    return MagicByteValidation(
                        valid=False,
                        detected_type="epub_incomplete",
                        reason="EPUB missing META-INF/container.xml",
                    )
                return MagicByteValidation(
                    valid=True,
                    detected_type="epub",
                    reason="Valid PK ZIP + application/epub+zip + META-INF/container.xml",
                )
        except zipfile.BadZipFile as exc:
            return MagicByteValidation(valid=False, detected_type="corrupt_zip", reason=str(exc))

    return MagicByteValidation(valid=False, detected_type="unsupported", reason=f"Unsupported format: {fmt}")


class MemoryOfTheWorldConnector:
    """Connector for Memory of the World (library.memoryoftheworld.org), listed UP on open-slum.org."""

    name = "Memory of the World"
    base_url = "https://library.memoryoftheworld.org"
    capabilities = ProviderCapabilities(search=True, server_download=True, continue_in_browser=False)

    def __init__(
        self,
        proxy: str | None = None,
        use_doh: bool = False,
        timeout_s: float = 30.0,
        client: httpx.Client | None = None,
    ) -> None:
        self.proxy = proxy
        self.use_doh = use_doh
        self.timeout_s = timeout_s
        self._client = client

    def _get_client(self) -> tuple[httpx.Client, bool]:
        if self._client is not None:
            return self._client, False
        # Optional DoH reachability check when ISP DNS fails locally
        if self.use_doh:
            host = urlparse(self.base_url).hostname
            if host:
                try:
                    socket.getaddrinfo(host, 443)
                except OSError:
                    _ = resolve_via_doh(host)
        client = httpx.Client(
            follow_redirects=True,
            timeout=self.timeout_s,
            proxy=self.proxy,
            headers={"User-Agent": "ZeusReader-Phase0-Spike/0.1"},
        )
        return client, True

    @staticmethod
    def parse_search_payload(payload: dict, base_url: str = "https://library.memoryoftheworld.org") -> list[EditionCandidate]:
        candidates: list[EditionCandidate] = []
        for item in payload.get("_items", []):
            title = str(item.get("title") or "").strip()
            authors = [str(a).strip() for a in item.get("authors", []) if str(a).strip()]
            raw_langs = item.get("languages") or []
            language = ", ".join(str(x) for x in raw_langs) if isinstance(raw_langs, list) else str(raw_langs)
            pubdate = str(item.get("pubdate") or "")
            year = pubdate[:4] if len(pubdate) >= 4 else ""
            item_baseurl = str(item.get("baseurl") or "").rstrip("/")

            for fmt_entry in item.get("formats", []):
                fmt = str(fmt_entry.get("format") or "").lower().strip()
                if fmt not in {"epub", "pdf"}:
                    continue
                dir_path = str(fmt_entry.get("dir_path") or "").lstrip("/")
                file_name = str(fmt_entry.get("file_name") or "").lstrip("/")
                if not dir_path or not file_name:
                    continue
                rel_path = quote(f"{dir_path}{file_name}", safe="/")
                dl_url = f"{base_url.rstrip('/')}{item_baseurl}/{rel_path}"
                candidates.append(
                    EditionCandidate(
                        source=MemoryOfTheWorldConnector.name,
                        title=title,
                        authors=authors,
                        language=language,
                        year=year,
                        format=fmt,
                        byte_size=int(fmt_entry["size"]) if fmt_entry.get("size") is not None else None,
                        download_url=dl_url,
                        needs_browser=False,
                    )
                )
        return candidates

    def search_raw(self, query: str) -> dict:
        client, should_close = self._get_client()
        try:
            encoded = quote(query.strip(), safe="")
            response = client.get(f"{self.base_url}/search/titles/{encoded}")
            response.raise_for_status()
            return response.json()
        finally:
            if should_close:
                client.close()

    def search(self, query: str) -> list[EditionCandidate]:
        return self.parse_search_payload(self.search_raw(query), base_url=self.base_url)

    def download(self, candidate: EditionCandidate, dest_dir: Path) -> DownloadVerification:
        dest_dir.mkdir(parents=True, exist_ok=True)
        safe_name = "".join(c if c.isalnum() or c in "._- " else "_" for c in candidate.title).strip() or "book"
        final_path = dest_dir / f"{safe_name}.{candidate.format}"
        part_path = dest_dir / f"{safe_name}.{candidate.format}.part"

        client, should_close = self._get_client()
        sha256_hash = hashlib.sha256()
        total_bytes = 0
        try:
            with client.stream("GET", candidate.download_url) as resp:
                resp.raise_for_status()
                with part_path.open("wb") as out:
                    for chunk in resp.iter_bytes(chunk_size=65536):
                        if chunk:
                            out.write(chunk)
                            sha256_hash.update(chunk)
                            total_bytes += len(chunk)

            validation = validate_magic_bytes_and_container(part_path, candidate.format)
            if not validation.valid:
                part_path.unlink(missing_ok=True)
                raise ValueError(f"Magic-byte/container validation failed: {validation.reason}")

            os.replace(part_path, final_path)
            return DownloadVerification(
                file_path=final_path,
                sha256=sha256_hash.hexdigest(),
                byte_size=total_bytes,
                validation=validation,
                needs_browser=candidate.needs_browser,
            )
        except Exception:
            part_path.unlink(missing_ok=True)
            raise
        finally:
            if should_close:
                client.close()


def verification_to_dict(ver: DownloadVerification) -> dict:
    data = asdict(ver)
    data["file_path"] = str(ver.file_path)
    return data
